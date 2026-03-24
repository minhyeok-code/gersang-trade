package org.example.gersangtrade.crawler.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.catalog.repository.MaterialItemRepository;
import org.example.gersangtrade.crawler.parser.GerniverseParser;
import org.example.gersangtrade.crawler.service.S3ImageService;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.example.gersangtrade.domain.catalog.*;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.jsoup.nodes.Document;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Job 1 - Step 2: 아이템 상세 정보 수집 및 저장 ItemWriter.
 *
 * <p>각 아이템에 대해:
 * <ol>
 *   <li>gerniverse /item/{아이템명} 페이지 파싱</li>
 *   <li>장비 여부 판단 → type 업데이트, EquipmentItem 또는 MaterialItem 생성</li>
 *   <li>능력치(ItemStat) 저장</li>
 *   <li>이미지 S3 업로드 후 item.imageUrl 업데이트</li>
 * </ol>
 *
 * <p>⚠ gerniverse 파싱 실패 시 해당 아이템은 skip하고 계속 진행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemDetailWriter implements ItemWriter<Item> {

    private static final String GERNIVERSE_ITEM_URL = "https://gerniverse.app/item/";

    private final JsoupFetcher jsoupFetcher;
    private final S3ImageService s3ImageService;
    private final EquipmentItemRepository equipmentItemRepository;
    private final MaterialItemRepository materialItemRepository;
    private final ItemStatRepository itemStatRepository;

    @Override
    @Transactional
    public void write(Chunk<? extends Item> chunk) throws Exception {
        for (Item item : chunk) {
            try {
                processItem(item);
            } catch (Exception e) {
                log.warn("아이템 상세 처리 실패 (skip): {} — {}", item.getName(), e.getMessage());
            }
        }
    }

    private void processItem(Item item) throws Exception {
        String encodedName = URLEncoder.encode(item.getName(), StandardCharsets.UTF_8);
        String url = GERNIVERSE_ITEM_URL + encodedName;

        Document doc = jsoupFetcher.fetch(url);
        GerniverseParser.parseItem(doc).ifPresentOrElse(
                data -> applyItemData(item, data),
                () -> log.debug("gerniverse 파싱 결과 없음 (skip): {}", item.getName())
        );
    }

    private void applyItemData(Item item, GerniverseParser.ItemData data) {
        // 아이템 타입 업데이트 (gerniverse 분류 기준)
        if (data.isEquipment() && item.getType() == ItemType.MATERIAL) {
            item.updateType(ItemType.EQUIPMENT);
        }

        // 장비 아이템 — EquipmentItem 서브타입 생성
        if (data.isEquipment() && data.slot() != null) {
            if (!equipmentItemRepository.existsByItemId(item.getId())) {
                equipmentItemRepository.save(EquipmentItem.builder()
                        .item(item)
                        .equipmentKind(EquipmentKind.NORMAL)   // 기본값; APPEARANCE는 추후 수정
                        .slot(data.slot())
                        .build());
                log.debug("EquipmentItem 생성: {} ({})", item.getName(), data.slot());
            }
        } else if (!data.isEquipment()) {
            // 재료 아이템 — MaterialItem 서브타입 생성
            if (!materialItemRepository.existsByItemId(item.getId())) {
                materialItemRepository.save(MaterialItem.builder()
                        .item(item)
                        .build());
                log.debug("MaterialItem 생성: {}", item.getName());
            }
        }

        // 능력치 저장 (기존 없는 경우만)
        for (GerniverseParser.StatEntry stat : data.stats()) {
            if (!itemStatRepository.existsByItemIdAndStatType(item.getId(), stat.statType())) {
                itemStatRepository.save(ItemStat.builder()
                        .item(item)
                        .statType(stat.statType())
                        .element(Element.NONE)
                        .value(stat.value())
                        .build());
                log.debug("ItemStat 저장: {} {} {}", item.getName(), stat.statType(), stat.value());
            }
        }

        // 이미지 S3 업로드
        if (data.imageKey() != null) {
            String s3Url = s3ImageService.uploadItemImage(data.imageKey());
            if (s3Url != null) {
                item.updateImageUrl(s3Url);
                log.info("아이템 이미지 저장 완료: {} → {}", item.getName(), s3Url);
            }
        }
    }
}
