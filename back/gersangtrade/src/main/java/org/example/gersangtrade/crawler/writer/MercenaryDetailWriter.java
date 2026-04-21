package org.example.gersangtrade.crawler.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.MercenaryMaterialRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.MercenaryStatRepository;
import org.example.gersangtrade.crawler.parser.GerniverseParser;
import org.example.gersangtrade.crawler.service.S3ImageService;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryMaterial;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.jsoup.nodes.Document;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Job 1 - Step 4: 용병 상세 정보 수집 및 저장 ItemWriter.
 *
 * <p>각 용병에 대해:
 * <ol>
 *   <li>gerniverse /mercenary/{용병명} 페이지 파싱</li>
 *   <li>category / nation / nature / natureValue / imageUrl 업데이트</li>
 *   <li>MercenaryStat 재적재 (기존 삭제 후 신규 삽입)</li>
 *   <li>MercenaryMaterial 재적재 (기존 삭제 후 신규 삽입)</li>
 *   <li>crawledAt 설정 (처리 완료 표시)</li>
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
    private final MercenaryStatRepository mercenaryStatRepository;
    private final MercenaryMaterialRepository mercenaryMaterialRepository;

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
        // 이미지 S3 업로드
        String s3Url = null;
        if (data.imageKey() != null) {
            s3Url = s3ImageService.uploadMercenaryImage(data.imageKey());
        }

        // 기본 정보 업데이트
        mercenary.updateSpec(
                data.key(), data.category(), data.nation(),
                data.nature(), data.natureValue(), data.comingSoon(),
                s3Url, LocalDateTime.now());

        log.info("용병 스펙 업데이트: {} (카테고리={}, 속성={}, 속성값={})",
                mercenary.getName(), data.category(), data.nature(), data.natureValue());

        // 스탯 재적재 (기존 삭제 후 신규 삽입) — 단일 트랜잭션 보장
        mercenaryStatRepository.deleteByMercenaryId(mercenary.getId());
        for (GerniverseParser.MercenaryStatEntry entry : data.stats()) {
            mercenaryStatRepository.save(MercenaryStat.builder()
                    .mercenary(mercenary)
                    .statKey(entry.statKey())
                    .statValue(entry.statValue())
                    .build());
            log.debug("용병 스탯 저장: {} → {} = {}",
                    mercenary.getName(), entry.statKey(), entry.statValue());
        }

        // 전직 재료 재적재 (기존 삭제 후 신규 삽입) — 단일 트랜잭션 보장
        if (!data.materials().isEmpty()) {
            mercenaryMaterialRepository.deleteByResultMercenaryId(mercenary.getId());

            for (GerniverseParser.MaterialEntry entry : data.materials()) {
                if (entry.materialItemKey() != null) {
                    // 아이템 재료: key 문자열로 저장 (Item FK 없이)
                    mercenaryMaterialRepository.save(MercenaryMaterial.builder()
                            .resultMercenary(mercenary)
                            .materialItemKey(entry.materialItemKey())
                            .quantity(entry.quantity())
                            .requiredLevel(entry.requiredLevel())
                            .requiredCredit(entry.requiredCredit())
                            .build());
                    log.debug("용병 아이템 재료 저장: {} → {} x{}",
                            mercenary.getName(), entry.materialItemKey(), entry.quantity());

                } else if (entry.materialMercenaryName() != null) {
                    // 용병 재료: 이름으로 DB 조회 후 FK 저장
                    mercenaryRepository.findByName(entry.materialMercenaryName())
                            .ifPresentOrElse(
                                    materialMercenary -> {
                                        mercenaryMaterialRepository.save(MercenaryMaterial.builder()
                                                .resultMercenary(mercenary)
                                                .materialMercenary(materialMercenary)
                                                .quantity(entry.quantity())
                                                .requiredLevel(entry.requiredLevel())
                                                .requiredCredit(entry.requiredCredit())
                                                .build());
                                        log.debug("용병 재료 저장: {} → {} x{}",
                                                mercenary.getName(),
                                                entry.materialMercenaryName(),
                                                entry.quantity());
                                    },
                                    // 재료 용병이 아직 DB에 미등록이면 skip
                                    // (처리 순서상 하위 용병이 먼저 등록되어야 함)
                                    () -> log.debug("재료 용병 미등록 (skip): {}",
                                            entry.materialMercenaryName())
                            );
                }
            }
        }
    }
}
