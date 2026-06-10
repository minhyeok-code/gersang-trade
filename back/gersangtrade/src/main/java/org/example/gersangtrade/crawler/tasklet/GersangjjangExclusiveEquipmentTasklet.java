package org.example.gersangtrade.crawler.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.ItemMercenaryRestrictionRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.crawler.config.ExclusiveEquipPolicy;
import org.example.gersangtrade.crawler.config.ExclusiveEquipmentPageConfig;
import org.example.gersangtrade.crawler.parser.GersangjjangExclusiveEquipmentParser;
import org.example.gersangtrade.crawler.parser.GersangjjangExclusiveEquipmentParser.ParsedExclusiveRow;
import org.example.gersangtrade.crawler.service.GersangjjangCatalogUpsertService;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.ItemMercenaryRestriction;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.jsoup.nodes.Document;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 전용장비 크롤링 Tasklet.
 *
 * <p>사천왕·명왕·전설장수·주인공 전용장비 페이지를 순회하며
 * Item UPSERT + ItemMercenaryRestriction UPSERT를 수행한다.
 *
 * <p>선행 조건: ApplicationRunner 시더가 Mercenary 행을 생성한 뒤 실행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GersangjjangExclusiveEquipmentTasklet implements Tasklet {

    private final JsoupFetcher jsoupFetcher;
    private final GersangjjangCatalogUpsertService catalogUpsertService;
    private final MercenaryRepository mercenaryRepository;
    private final ItemMercenaryRestrictionRepository restrictionRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {

        log.info("=== GersangjjangExclusiveEquipmentTasklet 시작: 전용장비 수집 ===");

        int itemCount = 0;
        int restrictionCount = 0;
        int skipCount = 0;

        for (ExclusiveEquipmentPageConfig config : ExclusiveEquipmentPageConfig.all()) {
            try {
                Document doc = jsoupFetcher.fetch(config.url());
                List<ParsedExclusiveRow> rows = GersangjjangExclusiveEquipmentParser.parsePage(doc, config);
                Set<String> processedNames = new HashSet<>();

                for (ParsedExclusiveRow row : rows) {
                    String name = row.itemRow().name();
                    if (!processedNames.add(name)) continue;

                    Mercenary exclusiveMercenary = resolveExclusiveMercenary(row, config.policy());
                    Item item = catalogUpsertService.upsertEquipmentItem(
                            name, row.slot(), row.kind(), row.enhancement(), exclusiveMercenary);
                    catalogUpsertService.upsertAllStats(item, row.slot(), row.itemRow().stats());
                    catalogUpsertService.upsertSkills(item, row.itemRow().skills());
                    itemCount++;

                    int saved = upsertRestrictions(item, row, config.policy());
                    restrictionCount += saved;
                    if (saved == 0) {
                        skipCount++;
                    }
                }

                log.debug("페이지 완료: {} → {}행", config.href(), rows.size());

            } catch (Exception e) {
                log.warn("전용장비 페이지 파싱 실패 (skip): {} — {}", config.url(), e.getMessage());
            }
        }

        log.info("=== GersangjjangExclusiveEquipmentTasklet 완료: "
                + "아이템 {}개, restriction {}개, skip {}개 ===",
                itemCount, restrictionCount, skipCount);
        return RepeatStatus.FINISHED;
    }

    /**
     * 단일 전용 용병일 때만 EquipmentItem.mercenary_id FK를 설정한다.
     * 명왕부(2명)·인형(category)은 null.
     */
    private Mercenary resolveExclusiveMercenary(ParsedExclusiveRow row, ExclusiveEquipPolicy policy) {
        if (policy == ExclusiveEquipPolicy.PROTAGONIST_DOLL
                || row.restrictionMercenaryNames().size() != 1) {
            return null;
        }
        return mercenaryRepository.findByName(row.restrictionMercenaryNames().getFirst())
                .orElse(null);
    }

    /**
     * restriction UPSERT.
     *
     * @return 신규 저장된 restriction 행 수
     */
    private int upsertRestrictions(Item item, ParsedExclusiveRow row, ExclusiveEquipPolicy policy) {
        if (policy == ExclusiveEquipPolicy.PROTAGONIST_DOLL) {
            if (restrictionRepository.existsByItemIdAndCategory(item.getId(), MercenaryCategory.PROTAGONIST)) {
                return 0;
            }
            restrictionRepository.save(ItemMercenaryRestriction.builder()
                    .item(item)
                    .category(MercenaryCategory.PROTAGONIST)
                    .build());
            log.debug("restriction 저장 (PROTAGONIST): {}", item.getName());
            return 1;
        }

        int saved = 0;
        for (String mercenaryName : row.restrictionMercenaryNames()) {
            Mercenary mercenary = mercenaryRepository.findByName(mercenaryName).orElse(null);
            if (mercenary == null) {
                log.warn("시더에 없는 용병명 — restriction skip: [{}] → {}",
                        item.getName(), mercenaryName);
                continue;
            }
            if (restrictionRepository.existsByItemIdAndMercenaryId(item.getId(), mercenary.getId())) {
                continue;
            }
            restrictionRepository.save(ItemMercenaryRestriction.builder()
                    .item(item)
                    .mercenary(mercenary)
                    .build());
            log.debug("restriction 저장: {} → {}", item.getName(), mercenaryName);
            saved++;
        }
        return saved;
    }
}
