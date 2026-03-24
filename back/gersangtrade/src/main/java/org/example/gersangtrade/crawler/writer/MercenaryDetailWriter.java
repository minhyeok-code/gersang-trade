package org.example.gersangtrade.crawler.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.MercenaryMaterialRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.crawler.parser.GerniverseParser;
import org.example.gersangtrade.crawler.service.S3ImageService;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryMaterial;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.jsoup.nodes.Document;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Job 1 - Step 4: 용병 상세 정보 수집 및 저장 ItemWriter.
 *
 * <p>각 용병에 대해:
 * <ol>
 *   <li>gerniverse /mercenary/{용병명} 페이지 파싱</li>
 *   <li>저항깎·속성값·용병종류 업데이트</li>
 *   <li>고용 재료 목록 저장 (기존 재료 삭제 후 재적재)</li>
 *   <li>이미지 S3 업로드 후 mercenary.imageUrl 업데이트</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MercenaryDetailWriter implements ItemWriter<Mercenary> {

    private static final String GERNIVERSE_MERCENARY_URL = "https://gerniverse.app/mercenary/";

    private final JsoupFetcher jsoupFetcher;
    private final S3ImageService s3ImageService;
    private final MercenaryRepository mercenaryRepository;
    private final MercenaryMaterialRepository mercenaryMaterialRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public void write(Chunk<? extends Mercenary> chunk) throws Exception {
        for (Mercenary mercenary : chunk) {
            try {
                processMercenary(mercenary);
            } catch (Exception e) {
                log.warn("용병 상세 처리 실패 (skip): {} — {}", mercenary.getName(), e.getMessage());
            }
        }
    }

    private void processMercenary(Mercenary mercenary) throws Exception {
        String encodedName = URLEncoder.encode(mercenary.getName(), StandardCharsets.UTF_8);
        String url = GERNIVERSE_MERCENARY_URL + encodedName;

        Document doc = jsoupFetcher.fetch(url);
        GerniverseParser.parseMercenary(doc).ifPresentOrElse(
                data -> applyMercenaryData(mercenary, data),
                () -> log.debug("gerniverse 용병 파싱 결과 없음 (skip): {}", mercenary.getName())
        );
    }

    private void applyMercenaryData(Mercenary mercenary, GerniverseParser.MercenaryData data) {
        // 스펙 업데이트 (저항깎, 속성값은 null이면 그대로)
        String s3Url = null;
        if (data.imageKey() != null) {
            s3Url = s3ImageService.uploadMercenaryImage(data.imageKey());
        }

        mercenary.updateSpec(data.mercenaryType(), data.resistPierce(), data.elementValue(), s3Url);
        log.info("용병 스펙 업데이트: {} (종류={}, 저항깎={}, 속성값={})",
                mercenary.getName(), data.mercenaryType(), data.resistPierce(), data.elementValue());

        // 고용 재료 재적재 (기존 삭제 후 신규 삽입)
        if (!data.materials().isEmpty()) {
            mercenaryMaterialRepository.deleteByMercenaryId(mercenary.getId());

            for (GerniverseParser.MaterialEntry entry : data.materials()) {
                itemRepository.findByName(entry.itemName()).ifPresentOrElse(
                        item -> {
                            mercenaryMaterialRepository.save(MercenaryMaterial.builder()
                                    .mercenary(mercenary)
                                    .item(item)
                                    .quantity(entry.quantity())
                                    .build());
                            log.debug("용병 재료 저장: {} → {} x{}",
                                    mercenary.getName(), entry.itemName(), entry.quantity());
                        },
                        () -> {
                            // 재료 아이템이 DB에 미등록 상태이면 skip
                            // ItemListTasklet(Step1)에서 미리 등록되어 있어야 함
                            log.debug("재료 아이템 미등록 (skip): {}", entry.itemName());
                        }
                );
            }
        }
    }
}
