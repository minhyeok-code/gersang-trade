package org.example.gersangtrade.crawler.parser;

import org.example.gersangtrade.crawler.dto.ParsedItemDto;
import org.example.gersangtrade.domain.catalog.enums.GemGrade;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * geota 아이템명 파서.
 *
 * <p>geota 아이템 목록의 raw 아이템명을 아래 순서로 분류한다:
 * <ol>
 *   <li>구 데이터 skip — 괄호+숫자 패턴 {@code \(\+\d+\)} 포함 시 제외</li>
 *   <li>보석 판별 — GEM_NAMES 11종 중 하나가 포함되면 Gem 분류</li>
 *   <li>장비 판별 — {@code <홈이있는>} 또는 {@code <주술명>} 접두사가 있으면 Equipment</li>
 *   <li>나머지 — UNKNOWN (gerniverse 상세에서 최종 분류)</li>
 * </ol>
 *
 * <p>⚠ HTML 선택자와 아이템명 패턴은 geota 사이트 구조 변경 시 검증 필요.
 */
public final class ItemNameParser {

    /** 보석 기본명 11종 고정 목록 */
    public static final List<String> GEM_NAMES = List.of(
            "흑요석", "적혈석", "사금석", "백수정", "월장석",
            "적마노", "남옥", "석웅황", "벽옥", "청수석", "녹림석"
    );

    /** 구 데이터 패턴 — (강화된 흑요석(+1) 등). 이 패턴이 포함되면 skip */
    private static final Pattern OLD_FORMAT = Pattern.compile("\\(\\+\\d+\\)");

    /** {@code <마커내용> 나머지} 패턴 */
    private static final Pattern BRACKET_PREFIX = Pattern.compile("^<([^>]+)>\\s*(.+)$");

    /**
     * 홈이있는 접두사(괄호 없는 형태) — 예: "홈이있는 <태산북두> 챠우인형"
     * 이 경우 hasSlotOption=true, 뒤따르는 {@code <ritual> itemName}을 추가 파싱한다.
     */
    private static final String SLOT_PREFIX = "홈이있는 ";

    /** 세공됨 prefix (geota 표시명은 "세공된") */
    private static final String PREFIX_SEONG = "세공된 ";
    /** 강화됨 prefix (geota 표시명은 "강화된") */
    private static final String PREFIX_GANG = "강화된 ";
    /** 빛나는 prefix */
    private static final String PREFIX_SHINING = "빛나는 ";

    private ItemNameParser() {}

    /**
     * raw 아이템명을 파싱하여 분류 결과를 반환한다.
     *
     * @param rawName geota에서 수집한 raw 아이템명
     * @return 분류 결과. 구 데이터(skip 대상)이면 Optional.empty()
     */
    public static Optional<ParsedItemDto> parse(String rawName) {
        if (rawName == null || rawName.isBlank()) return Optional.empty();

        // 구 데이터 skip: (+1) ~ (+5) 패턴
        if (OLD_FORMAT.matcher(rawName).find()) return Optional.empty();

        // 보석 여부 판별
        for (String gemName : GEM_NAMES) {
            if (rawName.contains(gemName)) {
                return Optional.of(parseGem(rawName, gemName));
            }
        }

        // 나머지 아이템 분류
        return Optional.of(parseEquipmentOrUnknown(rawName));
    }

    // ── 보석 파싱 ──────────────────────────────────────────────────────────────

    private static ParsedItemDto parseGem(String raw, String gemName) {
        // <주술명> {gemName} 형식
        Matcher bracketMatcher = BRACKET_PREFIX.matcher(raw);
        if (bracketMatcher.matches()) {
            String marker = bracketMatcher.group(1);
            // <홈이있는>은 보석에 적용되지 않으므로 주술명으로 처리
            return ParsedItemDto.gem(gemName, GemGrade.주술됨, marker);
        }

        // 빛나는 {gemName}
        if (raw.startsWith(PREFIX_SHINING)) {
            return ParsedItemDto.gem(gemName, GemGrade.빛나는, null);
        }

        // 강화된 {gemName}
        if (raw.startsWith(PREFIX_GANG)) {
            return ParsedItemDto.gem(gemName, GemGrade.강화됨, null);
        }

        // 세공된 {gemName}
        if (raw.startsWith(PREFIX_SEONG)) {
            return ParsedItemDto.gem(gemName, GemGrade.세공됨, null);
        }

        // {gemName} (기본)
        return ParsedItemDto.gem(gemName, GemGrade.기본, null);
    }

    // ── 장비/미분류 파싱 ───────────────────────────────────────────────────────

    private static ParsedItemDto parseEquipmentOrUnknown(String raw) {
        // "홈이있는 <주술명> 아이템명" 패턴 — hasSlotOption=true, ritual 있음
        if (raw.startsWith(SLOT_PREFIX)) {
            String rest = raw.substring(SLOT_PREFIX.length());
            Matcher m = BRACKET_PREFIX.matcher(rest);
            if (m.matches()) {
                String ritualName = m.group(1);
                String itemName = m.group(2).trim();
                return ParsedItemDto.equipment(itemName, ritualName, true);
            }
            // "홈이있는 아이템명" (ritual 없는 경우, 드문 케이스)
            return ParsedItemDto.equipment(rest.trim(), null, true);
        }

        // "<마커> 아이템명" 패턴
        Matcher bracketMatcher = BRACKET_PREFIX.matcher(raw);
        if (bracketMatcher.matches()) {
            String marker = bracketMatcher.group(1);
            String itemName = bracketMatcher.group(2).trim();

            if ("홈이있는".equals(marker)) {
                // <홈이있는> 아이템명 — ritual 없음, hasSlotOption=true
                return ParsedItemDto.equipment(itemName, null, true);
            } else {
                // <주술명> 아이템명 — ritual 있음, hasSlotOption=false
                return ParsedItemDto.equipment(itemName, marker, false);
            }
        }

        // 마커 없는 일반 이름 — 분류 불명 (Material or Equipment, gerniverse에서 확정)
        return ParsedItemDto.unknown(raw.trim());
    }
}
