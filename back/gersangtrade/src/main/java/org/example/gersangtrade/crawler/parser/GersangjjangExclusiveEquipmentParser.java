package org.example.gersangtrade.crawler.parser;

import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.crawler.config.ExclusiveEquipmentPageConfig;
import org.example.gersangtrade.crawler.config.ExclusiveEquipPolicy;
import org.example.gersangtrade.crawler.parser.GersangjjangParser.ItemRow;
import org.example.gersangtrade.crawler.parser.GersangjjangParser.ParsedStat;
import org.example.gersangtrade.domain.catalog.enums.Enhancement;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 거상짱 전용장비 페이지 파서.
 * 섹션별 용병 분기·슬롯 감지·강화 단계 파싱을 담당한다.
 */
@Slf4j
public class GersangjjangExclusiveEquipmentParser {

    /** 행 내 용병 태그 — 예: {@code <항삼세명왕>}, {@code <각성 항삼세명왕>} */
    private static final Pattern MERCENARY_TAG_PATTERN = Pattern.compile("<([^>]+)>");

    /** 전설장수 강화 접미사 — 예: 최무선의화포(+5), 여포의 방천화극 (+5) */
    private static final Pattern ENHANCEMENT_SUFFIX_PATTERN = Pattern.compile("\\s*\\(\\+(\\d+)\\)$");

    /**
     * 명왕부 w-info 설명 속성값 — .w-stat에는 생명+150만 있고 속성값은 설명에만 기재된다.
     * 예: {@code * 명왕의 힘이전대상자가 화속성일 경우 속성값+5}
     */
    private static final Pattern MYEONGWANG_TALISMAN_ELEMENT_VALUE_PATTERN =
            Pattern.compile("([화수뇌풍]|불)속성일\\s*경우\\s*속성값([+\\-])(\\d+)");

    private GersangjjangExclusiveEquipmentParser() {}

    /**
     * 파싱된 전용장비 행.
     *
     * @param itemRow         아이템명·스탯·스킬
     * @param restrictionMercenaryNames restriction 대상 시더 canonical name 목록 (인형은 빈 리스트)
     * @param slot                      장비 슬롯
     * @param kind                      장비 종류
     * @param enhancement               전설장수 강화 단계 (해당 없으면 null)
     */
    public record ParsedExclusiveRow(
            ItemRow itemRow,
            List<String> restrictionMercenaryNames,
            EquipmentSlot slot,
            EquipmentKind kind,
            Enhancement enhancement
    ) {}

    /** 슬롯 + 장비 종류 쌍 */
    public record SlotKind(EquipmentSlot slot, EquipmentKind kind) {}

    /**
     * 전용장비 페이지를 파싱한다.
     *
     * @param doc    페이지 Document
     * @param config 페이지 설정
     * @return restriction 적용 대상 행 목록 (슬롯 미감지·용병 미확정 행은 제외)
     */
    public static List<ParsedExclusiveRow> parsePage(Document doc, ExclusiveEquipmentPageConfig config) {
        List<ParsedExclusiveRow> results = new ArrayList<>();
        String sectionMercenary = resolveInitialMercenary(config);

        // Type A: .data-row (사천왕·명왕·주인공) / Type B: .item-row .sub-row (전설장수 4j_*)
        for (Element el : doc.select(".main-title, .data-row, .item-row .sub-row")) {
            if (el.hasClass("main-title")) {
                sectionMercenary = resolveSectionMercenary(el.text().trim(), config)
                        .orElse(sectionMercenary);
                continue;
            }

            ItemRow itemRow = parseItemRowElement(el);
            if (itemRow == null) continue;

            if (config.policy() == ExclusiveEquipPolicy.PROTAGONIST_DOLL) {
                Optional<SlotKind> slotKind = resolveSlotAndKind(itemRow.name(), config.policy());
                if (slotKind.isEmpty()) {
                    log.warn("슬롯 감지 실패 — skip: [{}] ({})", itemRow.name(), config.href());
                    continue;
                }
                SlotKind sk = slotKind.get();
                results.add(new ParsedExclusiveRow(itemRow, List.of(), sk.slot(), sk.kind(), null));
                continue;
            }

            Optional<SlotKind> slotKind = resolveSlotAndKind(itemRow.name(), config.policy());
            if (slotKind.isEmpty()) {
                log.warn("슬롯 감지 실패 — skip: [{}] ({})", itemRow.name(), config.href());
                continue;
            }

            SlotKind sk = slotKind.get();
            if (!config.policy().shouldApplyRestriction(sk.slot(), sk.kind())) {
                log.debug("정책상 restriction 제외: [{}] slot={}", itemRow.name(), sk.slot());
                continue;
            }

            List<String> mercenaryNames = resolveRestrictionMercenaries(
                    el, itemRow.name(), sectionMercenary, sk, config);
            if (mercenaryNames.isEmpty()) {
                log.warn("용병명 미확정 — skip: [{}] ({})", itemRow.name(), config.href());
                continue;
            }

            Enhancement enhancement = config.policy() == ExclusiveEquipPolicy.LEGENDARY_GENERAL
                    ? parseEnhancement(itemRow.name()).orElse(null)
                    : null;

            if (config.policy() == ExclusiveEquipPolicy.HEAVENLY_KING_AND_MYEONGWANG
                    && sk.slot() == EquipmentSlot.TALISMAN) {
                itemRow = enrichMyeongwangTalismanFromInfo(el, itemRow);
            }

            results.add(new ParsedExclusiveRow(
                    itemRow, mercenaryNames, sk.slot(), sk.kind(), enhancement));
        }

        log.debug("전용장비 파싱 완료: {} → {}행", config.href(), results.size());
        return results;
    }

    /** Type A(.data-row) 또는 Type B(.sub-row) 요소에서 ItemRow 추출 */
    private static ItemRow parseItemRowElement(Element el) {
        if (el.hasClass("data-row")) {
            return GersangjjangParser.parseSingleDataRow(el);
        }
        if (el.hasClass("sub-row")) {
            return GersangjjangParser.parseSingleSubRow(el);
        }
        return null;
    }

    // ── 섹션·용병 분기 ────────────────────────────────────────────────────────

    private static String resolveInitialMercenary(ExclusiveEquipmentPageConfig config) {
        if (config.defaultMercenaryName() != null) {
            return config.defaultMercenaryName();
        }
        return config.baseMercenaryName();
    }

    /**
     * 섹션 제목에서 용병명을 추출한다.
     * 예: "증장천왕 전용템" → 증장천왕, "각성 증장천왕 전용템" → 각성 증장천왕
     */
    static Optional<String> resolveSectionMercenary(String sectionTitle,
                                                    ExclusiveEquipmentPageConfig config) {
        if (sectionTitle.isBlank()) return Optional.empty();

        if (config.awakenedMercenaryName() != null && sectionTitle.contains("각성")) {
            return Optional.of(config.awakenedMercenaryName());
        }
        if (config.baseMercenaryName() != null) {
            return Optional.of(config.baseMercenaryName());
        }
        return Optional.empty();
    }

    /**
     * restriction 대상 용병명 목록을 결정한다.
     *
     * <ul>
     *   <li>명왕부(TALISMAN) — 일반·각성 명왕 둘 다 착용 가능 → 2행</li>
     *   <li>명왕 무기 — 행 태그 또는 이름 패턴으로 일반/각성 1명</li>
     *   <li>그 외 — 섹션 기본 용병 1명</li>
     * </ul>
     */
    static List<String> resolveRestrictionMercenaries(Element row,
                                                      String itemName,
                                                      String sectionMercenary,
                                                      SlotKind slotKind,
                                                      ExclusiveEquipmentPageConfig config) {
        // 명왕부: 일반·각성 명왕 공유 착용
        if (slotKind.slot() == EquipmentSlot.TALISMAN
                && config.policy() == ExclusiveEquipPolicy.HEAVENLY_KING_AND_MYEONGWANG
                && isMyeongwangTalisman(itemName)
                && config.baseMercenaryName() != null
                && config.awakenedMercenaryName() != null) {
            return List.of(config.baseMercenaryName(), config.awakenedMercenaryName());
        }

        // 명왕 전용 무기만 .w-stat의 <용병명> 태그 사용 (w-info 재료 <광개토> 등과 구분)
        if (config.policy() == ExclusiveEquipPolicy.HEAVENLY_KING_AND_MYEONGWANG) {
            Element statEl = row.selectFirst(".w-stat, .cell.w-stat");
            if (statEl != null) {
                Optional<String> fromTag = extractMercenaryTag(statEl.text());
                if (fromTag.isPresent()) {
                    return List.of(fromTag.get());
                }
            }
        }

        Optional<String> fromWeaponName = resolveMyeongwangWeaponMercenary(itemName, config);
        if (fromWeaponName.isPresent()) {
            return List.of(fromWeaponName.get());
        }

        if (sectionMercenary != null) {
            return List.of(sectionMercenary);
        }
        if (config.defaultMercenaryName() != null) {
            return List.of(config.defaultMercenaryName());
        }
        return List.of();
    }

    /** 명왕부 여부 — *명왕부 접미사만 (사천왕 *천왕부와 구분) */
    static boolean isMyeongwangTalisman(String itemName) {
        return itemName.endsWith("명왕부");
    }

    /**
     * 명왕부 행의 w-info에서 동속성 사천왕 속성값 버프를 추출해 스탯 목록에 병합한다.
     * 부동명왕부는 제외한다.
     */
    static ItemRow enrichMyeongwangTalismanFromInfo(Element row, ItemRow itemRow) {
        if (!isMyeongwangTalisman(itemRow.name()) || itemRow.name().contains("부동")) {
            return itemRow;
        }
        Optional<ParsedStat> elementValue = parseMyeongwangTalismanElementValue(row);
        if (elementValue.isEmpty()) {
            return itemRow;
        }
        boolean alreadyHas = itemRow.stats().stream()
                .anyMatch(s -> s.statType() == StatType.ELEMENT_VALUE);
        if (alreadyHas) {
            return itemRow;
        }
        List<ParsedStat> merged = new ArrayList<>(itemRow.stats());
        merged.add(elementValue.get());
        return new ItemRow(itemRow.name(), merged, itemRow.skills());
    }

    /** w-info 텍스트에서 명왕부 속성값+수치를 파싱한다. */
    static Optional<ParsedStat> parseMyeongwangTalismanElementValue(Element row) {
        Element infoEl = row.selectFirst(".w-info, .cell.w-info");
        if (infoEl == null) {
            return Optional.empty();
        }
        Matcher matcher = MYEONGWANG_TALISMAN_ELEMENT_VALUE_PATTERN.matcher(infoEl.text());
        if (!matcher.find()) {
            return Optional.empty();
        }
        int sign = "-".equals(matcher.group(2)) ? -1 : 1;
        int value = Integer.parseInt(matcher.group(3)) * sign;
        // 계산기는 ALLY_HEAVENLY_KING 행의 element 대신 착용 명왕 속성을 사용한다.
        return Optional.of(new ParsedStat(
                StatType.ELEMENT_VALUE,
                org.example.gersangtrade.domain.catalog.enums.Element.NONE,
                value,
                StatUnit.FLAT));
    }

    /** 행 텍스트의 {@code <용병명>} 태그 추출 */
    static Optional<String> extractMercenaryTag(String rowText) {
        Matcher matcher = MERCENARY_TAG_PATTERN.matcher(rowText);
        if (matcher.find()) {
            String tag = matcher.group(1).trim();
            if (!tag.isBlank()) {
                return Optional.of(tag);
            }
        }
        return Optional.empty();
    }

    /**
     * 명왕 전용 무기 이름 패턴으로 용병을 추정한다.
     * 고급명왕검·명왕검 → 일반 명왕, 각성명왕장·진각명왕장 → 각성 명왕.
     */
    static Optional<String> resolveMyeongwangWeaponMercenary(String itemName,
                                                             ExclusiveEquipmentPageConfig config) {
        if (config.baseMercenaryName() == null) return Optional.empty();
        if (itemName.contains("각성명왕") || itemName.contains("진각명왕")) {
            return config.awakenedMercenaryName() != null
                    ? Optional.of(config.awakenedMercenaryName())
                    : Optional.empty();
        }
        if (itemName.contains("고급명왕")
                || itemName.contains("명왕검") || itemName.contains("명왕장")
                || itemName.contains("명왕궁") || itemName.contains("명왕극")
                || itemName.contains("명왕갑") || itemName.contains("명왕혼겸")
                || itemName.contains("명왕비") || itemName.contains("명왕주")) {
            return Optional.of(config.baseMercenaryName());
        }
        return Optional.empty();
    }

    // ── 슬롯·강화 감지 ────────────────────────────────────────────────────────

    /**
     * 아이템명으로 슬롯·종류를 결정한다.
     * 무신·인형 등 suffix 매핑에 없는 패턴은 별도 규칙을 적용한다.
     */
    static Optional<SlotKind> resolveSlotAndKind(String itemName, ExclusiveEquipPolicy policy) {
        // 전설장수 (+5)/(+10) 접미사·공백 제거 후 슬롯 감지
        String normalized = ENHANCEMENT_SUFFIX_PATTERN.matcher(itemName).replaceAll("").replace(" ", "");

        if (normalized.startsWith("무신의")) {
            return Optional.of(new SlotKind(EquipmentSlot.DIVINE, EquipmentKind.APPEARANCE));
        }
        if (policy == ExclusiveEquipPolicy.PROTAGONIST_DOLL || normalized.contains("인형")) {
            return Optional.of(new SlotKind(EquipmentSlot.WEAPON, EquipmentKind.NORMAL));
        }
        if (normalized.endsWith("활") || normalized.endsWith("궁")) {
            return Optional.of(new SlotKind(EquipmentSlot.WEAPON, EquipmentKind.NORMAL));
        }

        return GersangjjangSetParser.detectSlotByName(normalized)
                .map(slot -> new SlotKind(slot, EquipmentKind.NORMAL));
    }

    /** 아이템명 접미사에서 강화 단계를 추출한다. (+5)→FIVE, (+10)→TEN */
    static Optional<Enhancement> parseEnhancement(String itemName) {
        Matcher matcher = ENHANCEMENT_SUFFIX_PATTERN.matcher(itemName);
        if (!matcher.find()) {
            return Optional.of(Enhancement.NONE);
        }
        int value = Integer.parseInt(matcher.group(1));
        if (value >= 10) return Optional.of(Enhancement.TEN);
        if (value >= 5) return Optional.of(Enhancement.FIVE);
        return Optional.of(Enhancement.NONE);
    }
}
