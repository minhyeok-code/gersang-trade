package org.example.gersangtrade.crawler.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.MercenarySkillRepository;
import org.example.gersangtrade.catalog.repository.MercenaryStatRepository;
import org.example.gersangtrade.crawler.parser.GersangjjangMercenaryParser;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.MercenarySkill;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.enums.Nation;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.jsoup.nodes.Document;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Job 1 - Step 3: 거상짱 용병 목록 + 기본 스탯 수집 Tasklet.
 *
 * <p>URL: https://www.gersangjjang.com/yongbing/index.asp
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>인덱스 페이지에서 카테고리 URL 목록 수집 (없으면 인덱스 페이지 직접 파싱)</li>
 *   <li>페이지별 parsePage() → 용병명·카테고리·스탯·스킬·특성명 추출</li>
 *   <li>Mercenary UPSERT</li>
 *   <li>MercenaryStat 재적재 (기존 삭제 후 신규 삽입)</li>
 *   <li>MercenarySkill UPSERT (중복 skip)</li>
 *   <li>MercenaryCharacteristic UPSERT (특성명만 저장 — 수치는 관리자 수동 입력)</li>
 * </ol>
 *
 * <p>특성 레벨 수치(MercenaryCharacteristicLevel)는 거상짱에서 제공하지 않으므로
 * 관리자 페이지(/admin/mercenaries)에서 수동 입력한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GersangjjangMercenaryTasklet implements Tasklet {

    private static final String INDEX_URL = "https://www.gersangjjang.com/yongbing/index.asp";

    private final JsoupFetcher jsoupFetcher;
    private final MercenaryRepository mercenaryRepository;
    private final MercenaryStatRepository mercenaryStatRepository;
    private final MercenarySkillRepository mercenarySkillRepository;
    private final MercenaryCharacteristicRepository mercenaryCharacteristicRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {

        log.info("=== GersangjjangMercenaryTasklet 시작: 거상짱 용병 목록 수집 ===");

        Document indexDoc = jsoupFetcher.fetch(INDEX_URL);
        List<String> categoryUrls = GersangjjangMercenaryParser.parseCategoryLinks(indexDoc);

        Set<String> processedNames = new HashSet<>();
        int savedCount = 0, updatedCount = 0;

        if (categoryUrls.isEmpty()) {
            // 단일 대형 페이지 구조 — 인덱스 페이지 직접 파싱
            log.info("카테고리 URL 없음 → 인덱스 페이지 직접 파싱");
            processRows(GersangjjangMercenaryParser.parsePage(indexDoc),
                    processedNames, savedCount, updatedCount);
        } else {
            log.info("용병 카테고리 {}개 수집 시작", categoryUrls.size());
            for (String url : categoryUrls) {
                try {
                    Document categoryDoc = jsoupFetcher.fetch(url);
                    List<GersangjjangMercenaryParser.MercenaryRow> rows =
                            GersangjjangMercenaryParser.parsePage(categoryDoc);

                    if (rows.isEmpty()) {
                        log.warn("카테고리 용병 0개 (HTML 구조 확인 필요): {}", url);
                    }

                    for (GersangjjangMercenaryParser.MercenaryRow row : rows) {
                        if (!processedNames.add(row.name())) continue;
                        processRow(row);
                    }

                    log.debug("카테고리 완료: {} → {}개", url, rows.size());
                } catch (Exception e) {
                    log.warn("카테고리 파싱 실패 (skip): {} — {}", url, e.getMessage());
                }
            }
        }

        log.info("=== GersangjjangMercenaryTasklet 완료: 처리 {}개 ===", processedNames.size());
        return RepeatStatus.FINISHED;
    }

    private void processRows(List<GersangjjangMercenaryParser.MercenaryRow> rows,
                             Set<String> processedNames, int savedCount, int updatedCount) {
        for (GersangjjangMercenaryParser.MercenaryRow row : rows) {
            if (!processedNames.add(row.name())) continue;
            processRow(row);
        }
    }

    private void processRow(GersangjjangMercenaryParser.MercenaryRow row) {
        Mercenary mercenary = mercenaryRepository.findByName(row.name()).orElse(null);

        if (mercenary == null) {
            mercenary = mercenaryRepository.save(Mercenary.builder()
                    .name(row.name())
                    .category(row.category())
                    .nation(row.nation() != null ? row.nation() : Nation.NONE)
                    .nature(row.nature() != null ? row.nature() : Nature.NONE)
                    .build());
            log.debug("용병 신규 저장: {}", row.name());
        } else {
            mercenary.updateSpec(
                    mercenary.getKey(),
                    row.category() != null ? row.category() : mercenary.getCategory(),
                    row.nation() != null && row.nation() != Nation.NONE ? row.nation() : mercenary.getNation(),
                    row.nature() != null && row.nature() != Nature.NONE ? row.nature() : mercenary.getNature(),
                    mercenary.getNatureValue(),
                    mercenary.isComingSoon(),
                    mercenary.getImageUrl(),
                    LocalDateTime.now());
            log.debug("용병 업데이트: {}", row.name());
        }

        upsertStats(mercenary, row.stats());
        upsertSkills(mercenary, row.skills());
        upsertCharacteristics(mercenary, row.characteristicNames());
    }

    /** 용병 스탯 재적재 — 기존 전체 삭제 후 신규 삽입 */
    private void upsertStats(Mercenary mercenary,
                             List<GersangjjangMercenaryParser.ParsedStat> parsedStats) {
        mercenaryStatRepository.deleteByMercenaryId(mercenary.getId());
        for (GersangjjangMercenaryParser.ParsedStat ps : parsedStats) {
            mercenaryStatRepository.save(MercenaryStat.builder()
                    .mercenary(mercenary)
                    .statKey(ps.statType())
                    .statValue(ps.value())
                    .build());
        }
    }

    /** 용병 스킬 UPSERT — 이미 존재하는 스킬은 건너뜀 */
    private void upsertSkills(Mercenary mercenary, List<String> skills) {
        for (String skillName : skills) {
            if (!mercenarySkillRepository.existsByMercenaryIdAndSkillName(
                    mercenary.getId(), skillName)) {
                mercenarySkillRepository.save(MercenarySkill.builder()
                        .mercenary(mercenary)
                        .skillName(skillName)
                        .build());
                log.debug("용병 스킬 저장: {} → {}", mercenary.getName(), skillName);
            }
        }
    }

    /**
     * 용병 특성명 UPSERT — 이름 기준 키를 생성하여 존재하지 않으면 저장.
     * 기존 특성을 삭제하지 않아 관리자가 입력한 레벨 수치를 보존한다.
     * 특성 포인트·레벨 수치·선행특성은 관리자 페이지에서 수동 입력한다.
     */
    private void upsertCharacteristics(Mercenary mercenary, List<String> characteristicNames) {
        for (String charName : characteristicNames) {
            String key = buildCharacteristicKey(mercenary.getId(), charName);
            if (mercenaryCharacteristicRepository.findByKey(key).isEmpty()) {
                mercenaryCharacteristicRepository.save(MercenaryCharacteristic.builder()
                        .mercenary(mercenary)
                        .key(key)
                        .name(charName)
                        .build());
                log.debug("용병 특성 저장: {} → {}", mercenary.getName(), charName);
            }
        }
    }

    /** 특성 키 생성 — "merc-{mercenaryId}-{nameSlug}" */
    private String buildCharacteristicKey(Long mercenaryId, String name) {
        String slug = name.replaceAll("[^가-힣a-zA-Z0-9]", "");
        return "merc-" + mercenaryId + "-" + slug;
    }
}
