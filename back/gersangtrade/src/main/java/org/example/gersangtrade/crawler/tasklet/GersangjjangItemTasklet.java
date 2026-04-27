package org.example.gersangtrade.crawler.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.GemRepository;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.ItemSkillRepository;
import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.catalog.repository.RitualRepository;
import org.example.gersangtrade.crawler.dto.ParsedItemDto;
import org.example.gersangtrade.crawler.parser.GersangjjangParser;
import org.example.gersangtrade.crawler.parser.GersangjjangParser.ItemRow;
import org.example.gersangtrade.crawler.parser.ItemNameParser;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.example.gersangtrade.domain.catalog.Gem;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.ItemSkill;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.GemGrade;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
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
 * Job 1 - Step 1: 거상짱 아이템 목록 수집 및 UPSERT Tasklet.
 *
 * <p>URL: https://www.gersangjjang.com/item/index.asp
 * 거상짱은 SSR ASP 사이트로 Jsoup 정적 파싱이 정상 동작한다.
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>인덱스 페이지에서 카테고리 URL 목록 수집</li>
 *   <li>카테고리별 페이지 파싱 → 아이템명 추출</li>
 *   <li>보석 이름 패턴(11종) → gems 테이블 UPSERT</li>
 *   <li>그 외 → items 테이블에 MATERIAL 타입으로 UPSERT</li>
 * </ol>
 *
 * <p>EQUIPMENT/MATERIAL 세분화는 후순위. 비보석 아이템은 전부 MATERIAL로 적재한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GersangjjangItemTasklet implements Tasklet {

    private static final String INDEX_URL = "https://www.gersangjjang.com/item/index.asp";

    private final JsoupFetcher jsoupFetcher;
    private final ItemRepository itemRepository;
    private final ItemStatRepository itemStatRepository;
    private final ItemSkillRepository itemSkillRepository;
    private final GemRepository gemRepository;
    private final RitualRepository ritualRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {

        log.info("=== GersangjjangItemTasklet 시작: 거상짱 아이템 목록 수집 ===");

        // 인덱스 페이지에서 카테고리 링크 목록 수집
        Document indexDoc = jsoupFetcher.fetch(INDEX_URL);
        List<String> categoryUrls = GersangjjangParser.parseCategoryLinks(indexDoc);
        log.info("카테고리 {}개 수집 시작", categoryUrls.size());

        Set<String> processedNames = new HashSet<>();
        int gemCount = 0, itemCount = 0, skipCount = 0;

        for (String categoryUrl : categoryUrls) {
            try {
                Document categoryDoc = jsoupFetcher.fetch(categoryUrl);
                List<ItemRow> rows = GersangjjangParser.parseItemRows(categoryDoc);

                if (rows.isEmpty()) {
                    // 실제 HTML 앞부분을 로그에 출력 — 파서가 읽는 구조인지 확인용
                    String bodySnippet = categoryDoc.body() != null
                            ? categoryDoc.body().html().replaceAll("\\s+", " ").substring(
                                    0, Math.min(300, categoryDoc.body().html().length()))
                            : "(body 없음)";
                    log.warn("카테고리 아이템 0개 (HTML 구조 확인 필요): {}\n  HTML snippet: {}",
                            categoryUrl, bodySnippet);
                }

                for (ItemRow row : rows) {
                    if (!processedNames.add(row.name())) continue;  // 전체 중복 제거

                    ParsedItemDto parsed = ItemNameParser.parse(row.name()).orElse(null);
                    if (parsed == null) {
                        skipCount++;
                        continue;
                    }

                    if (parsed.type() == ParsedItemDto.ParsedType.GEM) {
                        upsertGem(parsed);
                        gemCount++;
                    } else {
                        Item item = upsertItem(parsed.cleanName());
                        upsertAllStats(item, row.stats());
                        upsertSkills(item, row.skills());
                        itemCount++;
                    }
                }

                log.debug("카테고리 완료: {} → {}개", categoryUrl, rows.size());

            } catch (Exception e) {
                log.warn("카테고리 파싱 실패 (skip): {} — {}", categoryUrl, e.getMessage());
            }
        }

        log.info("=== GersangjjangItemTasklet 완료: 보석 {}개, 아이템 {}개, skip {}개 ===",
                gemCount, itemCount, skipCount);
        return RepeatStatus.FINISHED;
    }

    /** 보석 UPSERT — 이름+등급+주술 조합이 없으면 신규 저장 */
    private void upsertGem(ParsedItemDto dto) {
        Long ritualId = null;
        if (dto.gemGrade() == GemGrade.ENHANCED && dto.ritualName() != null) {
            ritualId = ritualRepository.findByDisplayName(dto.ritualName())
                    .map(Ritual::getId)
                    .orElse(null);
            if (ritualId == null) {
                log.debug("주술 미등록으로 보석 skip: {} - {}", dto.cleanName(), dto.ritualName());
                return;
            }
        }

        Long finalRitualId = ritualId;
        gemRepository.findByNameAndGemGradeAndRitualId(dto.cleanName(), dto.gemGrade(), ritualId)
                .ifPresentOrElse(
                        existing -> log.debug("보석 이미 존재: {} {}", dto.gemGrade(), dto.cleanName()),
                        () -> {
                            Ritual ritual = finalRitualId != null
                                    ? ritualRepository.findById(finalRitualId).orElse(null)
                                    : null;
                            gemRepository.save(Gem.builder()
                                    .name(dto.cleanName())
                                    .gemGrade(dto.gemGrade())
                                    .ritual(ritual)
                                    .build());
                            log.debug("보석 신규 저장: {} {}", dto.gemGrade(), dto.cleanName());
                        }
                );
    }

    /** 아이템 UPSERT — 이름이 없으면 MATERIAL 타입으로 신규 저장. 저장 또는 조회된 Item 반환 */
    private Item upsertItem(String name) {
        return itemRepository.findByName(name)
                .orElseGet(() -> {
                    Item saved = itemRepository.save(Item.builder()
                            .name(name)
                            .type(ItemType.MATERIAL)
                            .build());
                    log.debug("아이템 신규 저장: {}", name);
                    return saved;
                });
    }

    /** 파싱된 스탯 전체 UPSERT — (statType, element) 조합이 이미 존재하면 건너뜀 */
    private void upsertAllStats(Item item, List<GersangjjangParser.ParsedStat> parsedStats) {
        for (GersangjjangParser.ParsedStat ps : parsedStats) {
            if (!itemStatRepository.existsByItemIdAndStatTypeAndElement(
                    item.getId(), ps.statType(), ps.element())) {
                itemStatRepository.save(ItemStat.builder()
                        .item(item)
                        .statType(ps.statType())
                        .element(ps.element())
                        .value(ps.value())
                        .build());
                log.debug("스탯 저장: {} {} {} → {}", item.getName(), ps.element(), ps.statType(), ps.value());
            }
        }
    }

    /** 파싱된 스킬명 전체 UPSERT — 이미 존재하는 skillName은 건너뜀 */
    private void upsertSkills(Item item, List<String> skills) {
        for (String skillName : skills) {
            if (!itemSkillRepository.existsByItemIdAndSkillName(item.getId(), skillName)) {
                itemSkillRepository.save(ItemSkill.builder()
                        .item(item)
                        .skillName(skillName)
                        .build());
                log.debug("스킬 저장: {} → {}", item.getName(), skillName);
            }
        }
    }
}
