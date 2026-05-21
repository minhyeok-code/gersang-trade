package org.example.gersangtrade.crawler.parser;

import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 거상짱 주술 페이지 파서.
 *
 * <p>대상 페이지:
 * <ul>
 *   <li>4등급 황색 ({@code /qianghua/4.asp}) — 고구려의/담덕만 필터링</li>
 *   <li>5단계 ({@code /qianghua/5.asp}) — 전체 + 북두칠성(대성공) 행 포함</li>
 * </ul>
 *
 * <p>황색 주술: Gem 없음, RitualSetEffect 없음. SUCCESS 스탯과 적용 아이템만 저장.
 * <p>5단계 주술: Gem + SUCCESS 스탯 + GREAT_SUCCESS(북두칠성) 스탯 + 세트 효과.
 */
@Slf4j
public class GersangjjangRitualParser {

    public static final String BASE_URL = "https://www.gersangjjang.com";
    public static final String YELLOW_URL = BASE_URL + "/qianghua/4.asp";
    public static final String FIVE_STAGE_URL = BASE_URL + "/qianghua/5.asp";

    /** 황색 페이지 필터 키워드 */
    private static final Set<String> YELLOW_FILTER = Set.of("고구려의", "담덕");

    /** 무시할 슬롯명 단독 토큰 */
    private static final Set<String> SLOT_TOKENS = Set.of(
            "장갑", "요대", "신발", "투구", "갑옷", "무기", "반지", "수호부"
    );

    /** 보석추가/아이템 섹션 강태그 텍스트 */
    private static final Set<String> APPLICABLE_SECTIONS = Set.of("보석추가 아이템", "아이템");

    /** 세트효과 섹션 패턴 — "3세트", "5세트" */
    private static final Pattern SET_EFFECT_SECTION = Pattern.compile("^(\\d+)세트$");

    /** 수집 중단 섹션 */
    private static final Set<String> STOP_SECTIONS = Set.of("드랍", "상점가");

    /** 주술 스탯 명칭 → StatType 매핑 */
    private static final Map<String, StatType> RITUAL_STAT_MAP = Map.ofEntries(
            Map.entry("힘", StatType.STRENGTH),
            Map.entry("민첩", StatType.DEXTERITY),
            Map.entry("생명", StatType.VITALITY),
            Map.entry("생명력", StatType.VITALITY),
            Map.entry("지력", StatType.INTELLECT),
            Map.entry("방어", StatType.DEFENSE),
            Map.entry("방어력", StatType.DEFENSE),
            Map.entry("데미지", StatType.DAMAGE_PERCENT),
            Map.entry("피해량", StatType.DAMAGE_PERCENT),
            Map.entry("치명타확율", StatType.CRITICAL_RATE),
            Map.entry("치명타피해", StatType.CRITICAL_DAMAGE),
            Map.entry("스킬데미지", StatType.SKILL_DAMAGE_PERCENT),
            Map.entry("필드이속", StatType.FIELD_MOVE_SPEED),
            Map.entry("능력치", StatType.ALL_STAT)
    );

    /** 속성 접두어 → Element 매핑 (화=불 동의어) */
    private static final Map<String, Element> ELEMENT_MAP = Map.of(
            "불", Element.FIRE,
            "화", Element.FIRE,
            "뇌", Element.THUNDER,
            "물", Element.WATER,
            "풍", Element.WIND,
            "땅", Element.EARTH
    );

    /** "뇌속성값+2", "화속성값+5%" 형식 (화=불 포함) */
    private static final Pattern ELEMENT_STAT_PATTERN =
            Pattern.compile("^([불화뇌물풍땅])속성값\\+(\\d+)(%)?$");

    /** "속성값+5" — 원소 접두어 없는 형식 → Element.ADAPTIVE */
    private static final Pattern ADAPTIVE_STAT_PATTERN =
            Pattern.compile("^속성값\\+(\\d+)(%)?$");

    /** "힘+50", "데미지+2%", "데미지+25~35" 형식 */
    private static final Pattern STAT_PATTERN =
            Pattern.compile("^([가-힣]+)\\+(\\d+)(?:~(\\d+))?(%)?$");

    private GersangjjangRitualParser() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 4등급 황색 페이지를 파싱한다. 고구려의/담덕만 반환.
     *
     * @param doc 4.asp 페이지 Document
     * @return 파싱된 주술 행 목록
     */
    public static List<ParsedRitualRow> parseYellowPage(Document doc) {
        List<ParsedRitualRow> result = new ArrayList<>();

        for (org.jsoup.nodes.Element tr : doc.select("tr")) {
            Elements tds = tr.select("> td");
            if (tds.size() < 4) continue;

            String displayName = extractRitualName(tds.get(0));
            if (displayName == null) continue;

            // 고구려의/담덕 필터
            boolean matched = YELLOW_FILTER.stream().anyMatch(displayName::contains);
            if (!matched) continue;

            // td[2]: 성공 스탯
            List<ParsedRitualStat> successStats = parseStats(normalizeStatText(tds.get(2)));

            // td[3]: 적용 아이템 (아이템: 섹션)
            Map<String, String> sections = extractSections(tds.get(3));
            List<String> applicableTokens = resolveApplicableTokens(sections, APPLICABLE_SECTIONS);

            result.add(new ParsedRitualRow(
                    displayName, true, null, null,
                    successStats, applicableTokens, List.of(),
                    null, null, null
            ));

            log.debug("황색 주술 파싱: {} → 스탯 {}개, 적용아이템 {}개",
                    displayName, successStats.size(), applicableTokens.size());
        }

        log.info("황색 주술 {}개 파싱 완료 (고구려의/담덕 필터)", result.size());
        return result;
    }

    /**
     * 5단계 페이지를 파싱한다. 북두칠성 행을 직전 주술의 GREAT_SUCCESS로 병합.
     *
     * @param doc 5.asp 페이지 Document
     * @return 파싱된 주술 행 목록 (북두칠성 행이 병합된 상태)
     */
    public static List<ParsedRitualRow> parseFiveStagePage(Document doc) {
        List<ParsedRitualRow> rows = new ArrayList<>();

        for (org.jsoup.nodes.Element tr : doc.select("tr")) {
            Elements tds = tr.select("> td");
            if (tds.size() < 5) continue;

            String firstText = tds.get(0).text().trim();
            if (firstText.isBlank()) continue;

            if (firstText.contains("북두칠성")) {
                // 대성공 행 — 직전 주술에 병합
                if (rows.isEmpty()) {
                    log.warn("북두칠성 행 처리할 직전 주술 없음 (skip)");
                    continue;
                }

                List<ParsedRitualStat> greatSuccessStats = parseStats(normalizeStatText(tds.get(3)));
                Map<String, String> sections = extractSections(tds.get(4));
                List<String> greatSuccessTokens = resolveApplicableTokens(sections, APPLICABLE_SECTIONS);
                List<ParsedRitualSetEffect> greatSuccessEffects = parseSetEffectsFromSections(sections);

                ParsedRitualRow prev = rows.get(rows.size() - 1);
                rows.set(rows.size() - 1,
                        prev.withGreatSuccess(greatSuccessStats, greatSuccessTokens, greatSuccessEffects));

                log.debug("북두칠성 대성공 병합: {} → 스탯 {}개", prev.displayName(), greatSuccessStats.size());

            } else {
                String displayName = extractRitualName(tds.get(0));
                if (displayName == null) continue;

                // td[1]: Gem 또는 광개토 특수 케이스
                String gemName = null;
                String gemAsApplicable = null;
                String td1 = tds.get(1).text().trim();
                if (!td1.isBlank() && !"-".equals(td1)) {
                    if (td1.startsWith("강화된 ")) {
                        gemName = td1.substring("강화된 ".length()).split("\\s")[0].trim();
                    } else if (td1.startsWith("세공된 ")) {
                        // 세공됨(POLISHED) 보석 — 저장 범위 외, 무시
                        log.debug("세공됨 보석 무시 (주술: {}): {}", displayName, td1);
                    } else {
                        // 광개토 주술: 2열에 "태황반지" 등 아이템명이 들어있음
                        gemAsApplicable = td1;
                        log.debug("2열 Gem 위치에 아이템명 발견: {} (주술: {})", td1, displayName);
                    }
                }

                // td[3]: 성공 스탯
                List<ParsedRitualStat> successStats = parseStats(normalizeStatText(tds.get(3)));

                // td[4]: 적용 아이템 + 세트 효과
                Map<String, String> sections = extractSections(tds.get(4));
                List<String> applicableTokens = resolveApplicableTokens(sections, APPLICABLE_SECTIONS);
                List<ParsedRitualSetEffect> setEffects = parseSetEffectsFromSections(sections);

                rows.add(new ParsedRitualRow(
                        displayName, false, gemName, gemAsApplicable,
                        successStats, applicableTokens, setEffects,
                        null, null, null
                ));

                log.debug("5단계 주술 파싱: {} gem={} → 스탯 {}개, 세트효과 {}개",
                        displayName, gemName, successStats.size(), setEffects.size());
            }
        }

        log.info("5단계 주술 {}개 파싱 완료", rows.size());
        return rows;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 내부 파싱 로직
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * `<td class="hang">` 에서 주술명을 추출한다.
     * img 이후 첫 번째 비어있지 않은 TextNode가 주술명이다.
     * "150lv", "주술비법 lv210 lv" 등 레벨 표기 줄은 건너뛴다.
     */
    private static String extractRitualName(org.jsoup.nodes.Element td) {
        for (Node node : td.childNodes()) {
            if (!(node instanceof TextNode tn)) continue;
            String text = tn.text().trim();
            if (text.isBlank()) continue;
            if (text.matches("\\d+.*lv.*") || text.contains("주술비법") || text.matches(".*lv\\d+.*")) continue;
            return text;
        }
        return null;
    }

    /**
     * td 내 strong 태그를 기준으로 섹션을 분리한다.
     * 반환값: { "보석추가 아이템" → ": 수라, 바투", "3세트" → ": 민첩+150, 데미지+2%", ... }
     *
     * <p>북두칠성 행처럼 콜론이 strong 태그 안에 있는 경우
     * ({@code <strong>보석추가 아이템: </strong>})도 동일하게 처리하기 위해
     * 섹션명 끝의 콜론을 제거한다.
     */
    private static Map<String, String> extractSections(org.jsoup.nodes.Element td) {
        Map<String, String> sections = new LinkedHashMap<>();
        String currentSection = null;
        StringBuilder currentContent = new StringBuilder();

        for (Node node : td.childNodes()) {
            if (node instanceof org.jsoup.nodes.Element el && "strong".equals(el.tagName())) {
                if (currentSection != null) {
                    sections.put(currentSection, currentContent.toString().trim());
                }
                // 콜론이 strong 내부에 있는 경우("보석추가 아이템:") 제거
                currentSection = el.text().trim().replaceAll(":$", "").trim();
                currentContent = new StringBuilder();
            } else if (node instanceof TextNode tn && currentSection != null) {
                currentContent.append(tn.text());
            }
        }
        if (currentSection != null) {
            sections.put(currentSection, currentContent.toString().trim());
        }
        return sections;
    }

    /**
     * sections 맵에서 지정된 섹션 이름(들)에 해당하는 토큰 목록을 반환한다.
     * 콜론 제거 후 쉼표로 분리하며, STOP_SECTIONS은 건너뛴다.
     */
    private static List<String> resolveApplicableTokens(Map<String, String> sections, Set<String> sectionNames) {
        for (String sectionName : sectionNames) {
            String raw = sections.get(sectionName);
            if (raw == null) continue;
            String content = raw.replaceAll("^\\s*:\\s*", "").trim();
            if (content.isBlank()) continue;
            return Arrays.stream(content.split("[,，]"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        return List.of();
    }

    /** sections 맵에서 "N세트" 섹션들을 ParsedRitualSetEffect 목록으로 변환한다 */
    private static List<ParsedRitualSetEffect> parseSetEffectsFromSections(Map<String, String> sections) {
        List<ParsedRitualSetEffect> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            Matcher m = SET_EFFECT_SECTION.matcher(entry.getKey());
            if (!m.matches()) continue;
            int pieces = Integer.parseInt(m.group(1));
            String content = entry.getValue().replaceAll("^\\s*:\\s*", "").trim();
            if (content.isBlank()) continue;
            parseStats(content).forEach(stat -> result.add(new ParsedRitualSetEffect(pieces, stat)));
        }
        return result;
    }

    /**
     * td 내 텍스트를 스탯 파싱에 적합한 쉼표 구분 문자열로 정규화한다.
     *
     * <p>{@code <br>} 뒤 텍스트가 {@code +}로 시작하면 직전 토큰에 이어붙인다.
     * (예: {@code 피해량<br>+5%} → {@code 피해량+5%})
     * <br> 뒤 텍스트가 일반 스탯이면 쉼표로 구분한다.
     * (예: {@code 힘+40<br>민첩+40} → {@code 힘+40,민첩+40})
     */
    private static String normalizeStatText(org.jsoup.nodes.Element td) {
        StringBuilder sb = new StringBuilder();
        boolean lastWasBr = false;
        for (org.jsoup.nodes.Node node : td.childNodes()) {
            if (node instanceof org.jsoup.nodes.Element el && "br".equals(el.tagName())) {
                lastWasBr = true;
            } else if (node instanceof TextNode tn) {
                String text = tn.text().trim();
                if (text.isBlank()) continue;
                if (lastWasBr) {
                    if (text.startsWith("+")) {
                        // 직전 토큰과 합침: "피해량" + "+5%" → "피해량+5%"
                        sb.append(text);
                    } else {
                        sb.append(",").append(text);
                    }
                } else {
                    if (!sb.isEmpty()) sb.append(",");
                    sb.append(text);
                }
                lastWasBr = false;
            }
        }
        return sb.toString();
    }

    /**
     * 주술 스탯 문자열을 파싱한다.
     * 입력 형식: "힘+50, 민첩+50, 생명+25, 지력+25, 방어력+100, 데미지+25~35"
     * 범위값(데미지+25~35)은 MIN_DAMAGE + MAX_DAMAGE 두 스탯으로 저장한다.
     */
    static List<ParsedRitualStat> parseStats(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<ParsedRitualStat> result = new ArrayList<>();

        for (String token : text.split("[,，]")) {
            token = token.trim();
            if (token.isBlank()) continue;

            // "속성값+5" — 원소 없는 형식 → ADAPTIVE (가장 먼저 체크)
            Matcher am = ADAPTIVE_STAT_PATTERN.matcher(token);
            if (am.matches()) {
                int value = Integer.parseInt(am.group(1));
                StatUnit unit = am.group(2) != null ? StatUnit.PERCENT : StatUnit.FLAT;
                result.add(new ParsedRitualStat(StatType.ELEMENT_VALUE, Element.ADAPTIVE, value, unit));
                continue;
            }

            // "뇌속성값+2", "화속성값+5%" 형식
            Matcher em = ELEMENT_STAT_PATTERN.matcher(token);
            if (em.matches()) {
                Element element = ELEMENT_MAP.get(em.group(1));
                int value = Integer.parseInt(em.group(2));
                StatUnit unit = em.group(3) != null ? StatUnit.PERCENT : StatUnit.FLAT;
                result.add(new ParsedRitualStat(StatType.ELEMENT_VALUE, element, value, unit));
                continue;
            }

            // "힘+50", "데미지+2%", "데미지+25~35" 형식
            Matcher sm = STAT_PATTERN.matcher(token);
            if (sm.matches()) {
                String statName = sm.group(1);
                int value = Integer.parseInt(sm.group(2));
                boolean isRange = sm.group(3) != null;
                boolean isPercent = sm.group(4) != null;
                StatUnit unit = isPercent ? StatUnit.PERCENT : StatUnit.FLAT;

                if (isRange) {
                    // "데미지+25~35" → MIN_DAMAGE(25) + MAX_DAMAGE(35)
                    int maxVal = Integer.parseInt(sm.group(3));
                    result.add(new ParsedRitualStat(StatType.MIN_DAMAGE, Element.NONE, value, unit));
                    result.add(new ParsedRitualStat(StatType.MAX_DAMAGE, Element.NONE, maxVal, unit));
                    continue;
                }

                StatType type = RITUAL_STAT_MAP.get(statName);
                if (type == null) {
                    log.debug("미매핑 스탯 스킵: [{}]", token);
                    continue;
                }

                result.add(new ParsedRitualStat(type, Element.NONE, value, unit));
                continue;
            }

            log.debug("스탯 토큰 파싱 실패 (skip): [{}]", token);
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 파싱 결과 레코드
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 주술 1개 파싱 결과.
     *
     * @param displayName              주술명 (예: "천권", "고구려의")
     * @param isYellow                 황색(4등급) 주술 여부
     * @param gemName                  보석 기본명 (예: "사금석"). 황색이나 Gem 없으면 null
     * @param gemAsApplicable          광개토 특수 케이스: 2열에 아이템명이 있는 경우 (예: "태황반지")
     * @param successStats             SUCCESS 스탯 목록
     * @param successApplicableTokens  SUCCESS 적용 가능 아이템 토큰 목록 (세트명 or 아이템명 or 슬롯명)
     * @param successSetEffects        SUCCESS 세트 효과 (황색은 빈 리스트)
     * @param greatSuccessStats        GREAT_SUCCESS 스탯. 5단계에서 북두칠성 행 병합 후 채워짐
     * @param greatSuccessApplicableTokens 대성공 적용 가능 아이템 토큰
     * @param greatSuccessSetEffects   대성공 세트 효과
     */
    public record ParsedRitualRow(
            String displayName,
            boolean isYellow,
            String gemName,
            String gemAsApplicable,
            List<ParsedRitualStat> successStats,
            List<String> successApplicableTokens,
            List<ParsedRitualSetEffect> successSetEffects,
            List<ParsedRitualStat> greatSuccessStats,
            List<String> greatSuccessApplicableTokens,
            List<ParsedRitualSetEffect> greatSuccessSetEffects
    ) {
        /** 북두칠성 행 데이터를 병합한 새 인스턴스를 반환한다 */
        public ParsedRitualRow withGreatSuccess(
                List<ParsedRitualStat> greatSuccessStats,
                List<String> greatSuccessApplicableTokens,
                List<ParsedRitualSetEffect> greatSuccessSetEffects) {
            return new ParsedRitualRow(
                    displayName, isYellow, gemName, gemAsApplicable,
                    successStats, successApplicableTokens, successSetEffects,
                    greatSuccessStats, greatSuccessApplicableTokens, greatSuccessSetEffects);
        }
    }

    /**
     * 주술 스탯 1개.
     *
     * @param statType  능력치 종류
     * @param element   속성 구분 (속성값 스탯만 non-NONE)
     * @param value     수치
     * @param statUnit  단위 (FLAT / PERCENT)
     */
    public record ParsedRitualStat(StatType statType, Element element, int value, StatUnit statUnit) {}

    /**
     * 주술 세트 효과 1줄.
     *
     * @param requiredPieces 몇 피스 착용 시 발동 (3 또는 5)
     * @param stat           발동 스탯
     */
    public record ParsedRitualSetEffect(int requiredPieces, ParsedRitualStat stat) {}
}
