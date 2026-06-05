package org.example.gersangtrade.crawler.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.MaterialItemRepository;
import org.example.gersangtrade.crawler.parser.GersangjjangParser;
import org.example.gersangtrade.crawler.parser.GersangjjangParser.MaterialRow;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.MaterialItem;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.jsoup.nodes.Document;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job 1 - Step 2: 거상짱 잡화/소모품/재료 카테고리 수집 및 UPSERT Tasklet.
 *
 * <p>처리 대상: 잡화(약, 음식, 교역품, 공구) + 소모품(캐시, 랜덤상자 등) + 재료(광물/정수, 판대기).
 * 보석(baoshi.asp)은 GersangjjangItemTasklet의 Gem 엔티티 처리 대상이므로 제외한다.
 *
 * <p>저장 내용: 아이템명(name) + 이미지 URL(imageUrl)만 수집. 스탯/스킬 없음.
 * DB에는 {@code items(MATERIAL)} + {@code material_items}로 UPSERT한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GersangjjangMaterialTasklet implements Tasklet {

    /** 재료 카테고리 URL 목록 — 거상짱 잡화/소모품/재료 섹션 (보석 제외) */
    private static final List<String> MATERIAL_URLS = List.of(
            // 잡화
            GersangjjangParser.BASE_URL + "ti.asp",         // 약-체력
            GersangjjangParser.BASE_URL + "mo.asp",         // 약-마법력
            GersangjjangParser.BASE_URL + "fuhuo.asp",      // 약-부활
            GersangjjangParser.BASE_URL + "shiwu.asp",      // 음식
            GersangjjangParser.BASE_URL + "jiaoyipin.asp",  // 교역품
            GersangjjangParser.BASE_URL + "gongju.asp",     // 공구
            // 소모품
            GersangjjangParser.BASE_URL + "cash1.asp",      // 캐시.특수품
            GersangjjangParser.BASE_URL + "cash2.asp",      // 일상소모품
            GersangjjangParser.BASE_URL + "cash3.asp",      // 랜덤상자
            GersangjjangParser.BASE_URL + "bianshenshu.asp",// 변신주문서
            GersangjjangParser.BASE_URL + "xinyong.asp",    // 신용도+
            // 재료
            GersangjjangParser.BASE_URL + "kuangshi.asp",   // 광물/정수
            GersangjjangParser.BASE_URL + "yinji.asp"       // 판대기
    );

    private final JsoupFetcher jsoupFetcher;
    private final ItemRepository itemRepository;
    private final MaterialItemRepository materialItemRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {

        log.info("=== GersangjjangMaterialTasklet 시작: 잡화/소모품/재료 수집 ===");

        int savedCount = 0, skippedCount = 0, errorCount = 0;

        for (String url : MATERIAL_URLS) {
            try {
                Document doc = jsoupFetcher.fetch(url);
                List<MaterialRow> rows = GersangjjangParser.parseMaterialRows(doc);

                if (rows.isEmpty()) {
                    log.warn("재료 카테고리 아이템 0개 (HTML 구조 확인 필요): {}", url);
                    continue;
                }

                for (MaterialRow row : rows) {
                    boolean isNew = upsertMaterialItem(row.name());
                    if (isNew) savedCount++; else skippedCount++;
                }

                log.debug("재료 카테고리 완료: {} → {}개", url, rows.size());

            } catch (Exception e) {
                errorCount++;
                log.warn("재료 카테고리 파싱 실패 (skip): {} — {}", url, e.getMessage());
            }
        }

        log.info("=== GersangjjangMaterialTasklet 완료: 신규 {}개, 기존 {}개, 오류 {}건 ===",
                savedCount, skippedCount, errorCount);
        return RepeatStatus.FINISHED;
    }

    /** @return true=신규 저장, false=기존 존재 */
    private boolean upsertMaterialItem(String name) {
        boolean isNew = false;

        Item item = itemRepository.findByName(name).orElse(null);
        if (item == null) {
            item = itemRepository.save(Item.builder()
                    .name(name)
                    .type(ItemType.MATERIAL)
                    .build());
            isNew = true;
            log.debug("재료 아이템 신규 저장: {}", name);
        }

        if (!materialItemRepository.existsByItemId(item.getId())) {
            materialItemRepository.save(MaterialItem.builder()
                    .item(item)
                    .build());
        }

        return isNew;
    }
}
