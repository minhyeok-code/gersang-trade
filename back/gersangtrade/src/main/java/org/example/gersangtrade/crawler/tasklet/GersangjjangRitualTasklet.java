package org.example.gersangtrade.crawler.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetPieceRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.example.gersangtrade.catalog.repository.GemRepository;
import org.example.gersangtrade.catalog.repository.RitualApplicabilityRepository;
import org.example.gersangtrade.catalog.repository.RitualRepository;
import org.example.gersangtrade.catalog.repository.RitualSetEffectRepository;
import org.example.gersangtrade.catalog.repository.RitualStatRepository;
import org.example.gersangtrade.crawler.parser.GersangjjangRitualParser;
import org.example.gersangtrade.crawler.parser.GersangjjangRitualParser.ParsedRitualRow;
import org.example.gersangtrade.crawler.parser.GersangjjangRitualParser.ParsedRitualStat;
import org.example.gersangtrade.crawler.parser.GersangjjangRitualParser.ParsedRitualSetEffect;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.EquipmentSetPiece;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.Gem;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.catalog.RitualApplicability;
import org.example.gersangtrade.domain.catalog.RitualSetEffect;
import org.example.gersangtrade.domain.catalog.RitualStat;
import org.example.gersangtrade.domain.catalog.enums.GemGrade;
import org.example.gersangtrade.domain.catalog.enums.RitualType;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;
import org.jsoup.nodes.Document;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Job - Step: 거상짱 주술 데이터 수집 및 UPSERT Tasklet.
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>4등급 황색 페이지 크롤링 — 고구려의/담덕 필터링</li>
 *   <li>5단계 페이지 크롤링 — 전체 + 북두칠성(대성공) 행 병합</li>
 *   <li>각 주술에 대해 Ritual, Gem, RitualStat, RitualApplicability, RitualSetEffect UPSERT</li>
 * </ol>
 *
 * <p>전제 조건: 아이템 크롤러, 세트 크롤러가 먼저 실행되어야 RitualApplicability FK 조회가 가능하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GersangjjangRitualTasklet implements Tasklet {

    /** 슬롯명 단독 토큰 — 적용 아이템 파싱 시 무시한다 */
    private static final Set<String> SLOT_TOKENS = Set.of(
            "장갑", "요대", "신발", "투구", "갑옷", "무기", "반지", "수호부"
    );

    /**
     * 현자/풍운/나한/천신 — 적용 가능 아이템이 HTML에 명시되지 않음.
     * 실제 적용 아이템: 항아의 목걸이, 항아의 귀걸이 (외변 아이템).
     */
    private static final Set<String> HYANGAH_RITUALS = Set.of("현자", "풍운", "나한", "천신");
    private static final List<String> HYANGAH_ITEM_NAMES = List.of("항아의 목걸이", "항아의 귀걸이");

    /**
     * 강인한/신속한/끈기의/신비한 — 적용 가능 아이템이 HTML에 명시되지 않음.
     * 주술명 → 단일 무기 아이템명 고정 매핑.
     */
    private static final Map<String, String> WEAPON_RITUAL_ITEM_MAP = Map.of(
            "강인한", "고급천왕검",
            "신속한", "고급천왕주",
            "끈기의", "고급천왕극",
            "신비한", "고급천왕비"
    );

    /**
     * 세트명 별칭 확장 — "각성사천왕"은 4개 실제 세트명으로 확장.
     * HTML에 "각성사천왕 장갑, 요대, 신발"로 표기되어 있으나,
     * 실제로는 4개 각성사천왕 세트의 모든 방어구에 적용된다.
     */
    private static final Map<String, List<String>> SET_EXPANSION_MAP = Map.of(
            "각성사천왕", List.of("각성지국천왕", "각성광목천왕", "각성다문천왕", "각성증장천왕")
    );

    private final JsoupFetcher jsoupFetcher;
    private final RitualRepository ritualRepository;
    private final GemRepository gemRepository;
    private final RitualStatRepository ritualStatRepository;
    private final RitualApplicabilityRepository ritualApplicabilityRepository;
    private final RitualSetEffectRepository ritualSetEffectRepository;
    private final EquipmentSetRepository equipmentSetRepository;
    private final EquipmentSetPieceRepository equipmentSetPieceRepository;
    private final EquipmentItemRepository equipmentItemRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("=== GersangjjangRitualTasklet 시작: 거상짱 주술 수집 ===");

        int totalRituals = 0, totalStats = 0, totalApplicability = 0, totalSetEffects = 0;

        // 황색 주술 처리
        Document yellowDoc = jsoupFetcher.fetch(GersangjjangRitualParser.YELLOW_URL);
        List<ParsedRitualRow> yellowRows = GersangjjangRitualParser.parseYellowPage(yellowDoc);
        for (ParsedRitualRow row : yellowRows) {
            try {
                int[] counts = processRow(row);
                totalRituals += counts[0];
                totalStats += counts[1];
                totalApplicability += counts[2];
                totalSetEffects += counts[3];
            } catch (Exception e) {
                log.warn("황색 주술 처리 실패 (skip): {} — {}", row.displayName(), e.getMessage());
            }
        }

        // 5단계 주술 처리
        Document fiveDoc = jsoupFetcher.fetch(GersangjjangRitualParser.FIVE_STAGE_URL);
        List<ParsedRitualRow> fiveRows = GersangjjangRitualParser.parseFiveStagePage(fiveDoc);
        for (ParsedRitualRow row : fiveRows) {
            try {
                int[] counts = processRow(row);
                totalRituals += counts[0];
                totalStats += counts[1];
                totalApplicability += counts[2];
                totalSetEffects += counts[3];
            } catch (Exception e) {
                log.warn("5단계 주술 처리 실패 (skip): {} — {}", row.displayName(), e.getMessage());
            }
        }

        log.info("=== GersangjjangRitualTasklet 완료: 주술 {}개, 스탯 {}개, 적용아이템 {}개, 세트효과 {}개 ===",
                totalRituals, totalStats, totalApplicability, totalSetEffects);
        return RepeatStatus.FINISHED;
    }

    /**
     * 주술 1개 처리. 반환 배열: [ritualCount, statCount, applicabilityCount, setEffectCount]
     * SUCCESS 적용 아이템 파싱 시 찾은 EquipmentSet 목록을 세트효과 저장에 재사용한다.
     */
    private int[] processRow(ParsedRitualRow row) {
        // 1. Ritual UPSERT — 북두칠성 대성공이 있는 주술은 greatSuccessMark="<북두칠성>"
        boolean hasNorthernDipper = row.greatSuccessStats() != null;
        RitualType ritualType = WEAPON_RITUAL_ITEM_MAP.containsKey(row.displayName())
                ? RitualType.WEAPON : RitualType.ARMOR;
        Ritual ritual = upsertRitual(row.displayName(), ritualType, hasNorthernDipper);

        // 2. Gem UPSERT (5단계 주술만)
        if (!row.isYellow() && row.gemName() != null) {
            upsertGem(row.gemName(), ritual);
        }

        // 3. 광개토 특수 케이스: 2열 아이템을 적용 가능 아이템으로 저장
        ApplicabilityResult gemApplicableResult = ApplicabilityResult.empty();
        if (row.gemAsApplicable() != null) {
            gemApplicableResult = resolveApplicability(ritual, List.of(row.gemAsApplicable()));
        }

        // 4. SUCCESS 적용 가능 아이템 — 찾은 EquipmentSet을 세트효과에 재사용
        ApplicabilityResult successResult = resolveApplicability(ritual, row.successApplicableTokens());

        // 현자/풍운/나한/천신: HTML에 아이템 미표기 → 항아의 목걸이/귀걸이 고정 매핑
        if (HYANGAH_RITUALS.contains(row.displayName())) {
            ApplicabilityResult hyangahResult = resolveApplicability(ritual, HYANGAH_ITEM_NAMES);
            successResult = new ApplicabilityResult(
                    successResult.saved() + hyangahResult.saved(),
                    successResult.linkedSets());
        }

        // 강인한/신속한/끈기의/신비한: HTML에 아이템 미표기 → 무기 아이템 고정 매핑
        String weaponItemName = WEAPON_RITUAL_ITEM_MAP.get(row.displayName());
        if (weaponItemName != null) {
            ApplicabilityResult weaponResult = resolveApplicability(ritual, List.of(weaponItemName));
            successResult = new ApplicabilityResult(
                    successResult.saved() + weaponResult.saved(),
                    successResult.linkedSets());
        }

        int applicabilityCount = gemApplicableResult.saved() + successResult.saved();

        // 5. SUCCESS 스탯
        int statCount = saveRitualStats(ritual, RitualOutcome.SUCCESS, row.successStats());

        // 6. SUCCESS 세트 효과 (5단계만) — SUCCESS 적용 세트 목록 사용
        int setEffectCount = saveRitualSetEffects(
                ritual, RitualOutcome.SUCCESS, row.successSetEffects(), successResult.linkedSets());

        // 7. GREAT_SUCCESS 처리
        if (row.greatSuccessStats() != null) {
            statCount += saveRitualStats(ritual, RitualOutcome.GREAT_SUCCESS, row.greatSuccessStats());

            // 대성공 적용 아이템 (SUCCESS의 부분 집합일 수 있음) — 역시 중복 skip
            ApplicabilityResult greatResult = resolveApplicability(ritual, row.greatSuccessApplicableTokens());
            applicabilityCount += greatResult.saved();

            // 대성공 세트 효과 — 대성공 적용 세트 목록 사용
            setEffectCount += saveRitualSetEffects(
                    ritual, RitualOutcome.GREAT_SUCCESS, row.greatSuccessSetEffects(), greatResult.linkedSets());
        }

        return new int[]{1, statCount, applicabilityCount, setEffectCount};
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPSERT 헬퍼
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ritual UPSERT — displayName 기준.
     * 마크는 {@code <마크명>} 형식으로 저장한다.
     * 북두칠성 대성공이 있는 주술은 greatSuccessMark를 {@code <북두칠성>}으로 저장·보정한다.
     *
     * @param ritualType        WEAPON / ARMOR
     * @param hasNorthernDipper 북두칠성(GREAT_SUCCESS) 행이 있는 주술 여부
     */
    private Ritual upsertRitual(String displayName, RitualType ritualType, boolean hasNorthernDipper) {
        String successMark = "<" + displayName + ">";
        String greatMark   = hasNorthernDipper ? "<북두칠성>" : null;

        return ritualRepository.findByDisplayName(displayName)
                .map(existing -> {
                    boolean needsUpdate =
                            existing.getRitualType() != ritualType ||
                            !successMark.equals(existing.getSuccessMark()) ||
                            !java.util.Objects.equals(greatMark, existing.getGreatSuccessMark());
                    if (needsUpdate) {
                        existing.updateMarks(ritualType, successMark, greatMark);
                        log.debug("주술 보정: {} type={} successMark={} greatSuccessMark={}", displayName, ritualType, successMark, greatMark);
                        return ritualRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Ritual saved = ritualRepository.save(Ritual.builder()
                            .displayName(displayName)
                            .ritualType(ritualType)
                            .successMark(successMark)
                            .greatSuccessMark(greatMark)
                            .build());
                    log.debug("주술 신규 저장: {} type={} successMark={} greatSuccessMark={}", displayName, ritualType, successMark, greatMark);
                    return saved;
                });
    }

    /**
     * Gem UPSERT — ritual + ENHANCED 조합 기준 (주술당 ENHANCED 보석은 하나).
     * 이름이 다르면 보정한다 ("+5 (힘100)" 등 오염 텍스트 정제 목적).
     */
    private void upsertGem(String gemName, Ritual ritual) {
        gemRepository.findByGemGradeAndRitual_Id(GemGrade.ENHANCED, ritual.getId())
                .ifPresentOrElse(
                        existing -> {
                            if (!gemName.equals(existing.getName())) {
                                existing.updateName(gemName);
                                gemRepository.save(existing);
                                log.debug("보석 이름 보정: {} → {} (주술: {})",
                                        existing.getName(), gemName, ritual.getDisplayName());
                            }
                        },
                        () -> {
                            gemRepository.save(Gem.builder()
                                    .name(gemName)
                                    .gemGrade(GemGrade.ENHANCED)
                                    .ritual(ritual)
                                    .build());
                            log.debug("보석 신규 저장: {} ENHANCED (주술: {})", gemName, ritual.getDisplayName());
                        }
                );
    }

    /**
     * RitualStat 저장 — ritual + outcome + statType + element 중복 시 skip.
     *
     * @return 저장된 스탯 수
     */
    private int saveRitualStats(Ritual ritual, RitualOutcome outcome, List<ParsedRitualStat> stats) {
        if (stats == null) return 0;
        int saved = 0;

        // 기존 스탯 목록을 한 번만 조회 (N+1 방지)
        List<RitualStat> existing = ritualStatRepository.findByRitualIdAndOutcome(ritual.getId(), outcome);

        for (ParsedRitualStat s : stats) {
            boolean exists = existing.stream().anyMatch(rs ->
                    rs.getStatType() == s.statType() && rs.getElement() == s.element());
            if (!exists) {
                ritualStatRepository.save(RitualStat.builder()
                        .ritual(ritual)
                        .outcome(outcome)
                        .statType(s.statType())
                        .statValue(s.value())
                        .statUnit(s.statUnit())
                        .element(s.element())
                        .build());
                log.debug("주술스탯 저장: {} {} {} {}{}",
                        ritual.getDisplayName(), outcome, s.statType(), s.value(), s.statUnit());
                saved++;
            }
        }
        return saved;
    }

    /**
     * 적용 가능 아이템 토큰을 해석하여 RitualApplicability를 저장한다.
     * 토큰 처리 우선순위:
     * 1. EquipmentSet 이름 직접 매칭 → 세트 전 피스에 저장
     * 2. 슬롯 접미어 제거 후 재시도 — "명왕 장갑" → "명왕", SET_EXPANSION_MAP 적용
     * 3. EquipmentItem 단품 매칭
     * 4. 슬롯명 단독 토큰 → 무시
     * 5. 매칭 실패 → 경고 로그
     *
     * @return ApplicabilityResult (저장 수 + 찾은 EquipmentSet 목록)
     */
    private ApplicabilityResult resolveApplicability(Ritual ritual, List<String> tokens) {
        if (tokens == null) return ApplicabilityResult.empty();
        int savedCount = 0;
        List<EquipmentSet> linkedSets = new ArrayList<>();

        for (String token : tokens) {
            token = token.trim();
            if (token.isBlank()) continue;

            // 1. EquipmentSet 이름 직접 매칭
            Optional<EquipmentSet> setOpt = equipmentSetRepository.findByName(token);
            if (setOpt.isPresent()) {
                savedCount += linkSet(ritual, setOpt.get(), linkedSets);
                continue;
            }

            // 2. 슬롯 접미어 제거 후 재시도 ("항삼세명왕 장갑" → "항삼세명왕")
            String stripped = stripSlotSuffix(token);
            if (!stripped.equals(token)) {
                // SET_EXPANSION_MAP 적용 ("각성사천왕" → 4개 세트)
                List<String> expanded = SET_EXPANSION_MAP.getOrDefault(stripped, List.of(stripped));
                boolean anyFound = false;
                for (String setName : expanded) {
                    Optional<EquipmentSet> expandedSet = equipmentSetRepository.findByName(setName);
                    if (expandedSet.isPresent()) {
                        savedCount += linkSet(ritual, expandedSet.get(), linkedSets);
                        anyFound = true;
                    } else {
                        log.warn("[SKIP] 세트 조회 실패 — ritualName: {}, setName: {} → 수동 처리 필요",
                                ritual.getDisplayName(), setName);
                    }
                }
                if (anyFound) continue;
            }

            // 3. EquipmentItem 단품 매칭
            Optional<EquipmentItem> itemOpt = equipmentItemRepository.findByItemName(token);
            if (itemOpt.isPresent()) {
                savedCount += saveApplicability(ritual, itemOpt.get());
                continue;
            }

            // 4. 슬롯명 단독 토큰 → 무시
            if (SLOT_TOKENS.contains(token)) {
                log.debug("슬롯명 토큰 무시: {} (주술: {})", token, ritual.getDisplayName());
                continue;
            }

            // 5. 매칭 실패
            log.warn("[SKIP] RitualApplicability 연결 실패 — ritualName: {}, targetName: {} → 수동 처리 필요",
                    ritual.getDisplayName(), token);
        }

        return new ApplicabilityResult(savedCount, linkedSets);
    }

    /** 세트의 방어구 피스에 RitualApplicability 저장 (반지 슬롯 제외). linkedSets에 추가 */
    private int linkSet(Ritual ritual, EquipmentSet set, List<EquipmentSet> linkedSets) {
        List<EquipmentSetPiece> pieces = equipmentSetPieceRepository.findByEquipmentSetId(set.getId());
        if (pieces.isEmpty()) {
            log.debug("세트 피스 없음 (skip applicability): {} (주술: {})", set.getName(), ritual.getDisplayName());
            return 0;
        }
        int saved = 0;
        for (EquipmentSetPiece piece : pieces) {
            // 반지에는 주술 적용 불가 — 세트에 반지 피스가 포함되더라도 제외
            if (piece.getEquipmentItem().getSlot() == EquipmentSlot.RING) continue;
            saved += saveApplicability(ritual, piece.getEquipmentItem());
        }
        if (!linkedSets.contains(set)) {
            linkedSets.add(set);
        }
        return saved;
    }

    /**
     * 토큰 끝의 슬롯명 접미어를 제거한다.
     * 예: "항삼세명왕 장갑" → "항삼세명왕", "각성사천왕 요대" → "각성사천왕"
     */
    private String stripSlotSuffix(String token) {
        for (String slot : SLOT_TOKENS) {
            if (token.endsWith(" " + slot)) {
                return token.substring(0, token.length() - slot.length() - 1).trim();
            }
        }
        return token;
    }

    /** RitualApplicability 단건 저장 (이미 존재하면 skip). 저장 수 반환 */
    private int saveApplicability(Ritual ritual, EquipmentItem equipmentItem) {
        if (ritualApplicabilityRepository.existsByRitual_IdAndEquipmentItem_ItemId(
                ritual.getId(), equipmentItem.getItemId())) {
            return 0;
        }
        ritualApplicabilityRepository.save(RitualApplicability.builder()
                .ritual(ritual)
                .equipmentItem(equipmentItem)
                .build());
        log.debug("주술적용 저장: {} → {}",
                ritual.getDisplayName(), equipmentItem.getItem().getName());
        return 1;
    }

    /**
     * RitualSetEffect 저장.
     * linkedSets 목록에 있는 각 EquipmentSet에 대해 세트 효과를 저장한다.
     * 유니크 제약 조건 위반 시 skip.
     *
     * @param linkedSets 이미 resolveApplicability에서 확인된 세트 목록 — findAll() 없이 사용
     * @return 저장된 세트 효과 수
     */
    private int saveRitualSetEffects(Ritual ritual, RitualOutcome outcome,
                                     List<ParsedRitualSetEffect> effects,
                                     List<EquipmentSet> linkedSets) {
        if (effects == null || effects.isEmpty()) return 0;
        if (linkedSets.isEmpty()) {
            log.debug("세트효과 저장 대상 세트 없음 (skip): {} {}", ritual.getDisplayName(), outcome);
            return 0;
        }

        int saved = 0;
        for (ParsedRitualSetEffect effect : effects) {
            ParsedRitualStat s = effect.stat();
            for (EquipmentSet set : linkedSets) {
                boolean exists = ritualSetEffectRepository
                        .existsByRitual_IdAndOutcomeAndEquipmentSet_IdAndRequiredRitualPiecesAndStatType(
                                ritual.getId(), outcome, set.getId(),
                                effect.requiredPieces(), s.statType());
                if (!exists) {
                    ritualSetEffectRepository.save(RitualSetEffect.builder()
                            .ritual(ritual)
                            .outcome(outcome)
                            .equipmentSet(set)
                            .requiredRitualPieces(effect.requiredPieces())
                            .statType(s.statType())
                            .statValue(s.value())
                            .statUnit(s.statUnit())
                            .element(s.element())
                            .build());
                    log.debug("세트효과 저장: {} {} {} {}종 {} {}{}",
                            ritual.getDisplayName(), set.getName(), outcome,
                            effect.requiredPieces(), s.statType(), s.value(), s.statUnit());
                    saved++;
                }
            }
        }
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 내부 DTO
    // ─────────────────────────────────────────────────────────────────────────

    /** resolveApplicability 반환값 — 저장 수 + 세트효과 저장에 재사용할 EquipmentSet 목록 */
    private record ApplicabilityResult(int saved, List<EquipmentSet> linkedSets) {
        static ApplicabilityResult empty() {
            return new ApplicabilityResult(0, List.of());
        }
    }
}
