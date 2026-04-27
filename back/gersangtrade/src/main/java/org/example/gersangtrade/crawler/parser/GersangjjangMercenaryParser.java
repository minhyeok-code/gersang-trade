package org.example.gersangtrade.crawler.parser;

import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nation;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 거상짱(gersangjjang.com) 용병 페이지 파서.
 *
 * <p>거상짱 용병 페이지는 h2 섹션 + .card-grid 구조로 이루어진다.
 * h2 텍스트로 카테고리를 결정하고, .card 단위로 용병 정보를 파싱한다.
 *
 * <p>카드 구조는 2가지 타입이 존재한다:
 * <ul>
 *   <li><b>타입A (전설장수)</b>: .card-header 내 .nat-info(국가) + .stat-grid(상세 스탯).
 *       특성명이 스킬 행에 포함됨. 예: {@code / <b>특성:</b> 속사, 지주}</li>
 *   <li><b>타입B (1/2차 장수)</b>: .name-section strong에 국가 포함 (예: 조승훈(중국)).
 *       .stat-line 텍스트로 스탯 제공. 특성 없음.</li>
 * </ul>
 */
@Slf4j
public final class GersangjjangMercenaryParser {

    public static final String INDEX_URL = "https://www.gersangjjang.com/yongbing/index.asp";

    /** stat-grid/stat-line의 "key: value%" 패턴 */
    private static final Pattern STAT_VALUE_PATTERN =
            Pattern.compile("([가-힣a-zA-Z]+):\\s*(\\d+)%?");

    /** 이름에서 속성 한자 제거 패턴 — 예: "도사 홍길동(風)" → "도사 홍길동" */
    private static final Pattern NAME_NATURE_SUFFIX =
            Pattern.compile("[（(][火水雷風土地][）)]$");

    /** 이름에서 국가명 제거 패턴 — 예: "조승훈(중국)" → "조승훈" */
    private static final Pattern NAME_NATION_SUFFIX =
            Pattern.compile("[（(](조선|중국|일본|대만|인도)[）)]$");

    /** h2 텍스트 → MercenaryCategory 매핑 */
    private static final Map<String, MercenaryCategory> H2_CATEGORY_MAP = Map.ofEntries(
            Map.entry("거상 전설장수", MercenaryCategory.LEGENDARY_GENERAL),
            Map.entry("전설장수", MercenaryCategory.LEGENDARY_GENERAL),
            Map.entry("각성사천왕", MercenaryCategory.FOUR_HEAVENLY_KINGS_AWAKENING),
            Map.entry("사천왕", MercenaryCategory.FOUR_HEAVENLY_KINGS),
            Map.entry("각성명왕", MercenaryCategory.MYEONG_KING_AWAKENING),
            Map.entry("명왕", MercenaryCategory.MYEONG_KING),
            Map.entry("각성흉수", MercenaryCategory.EVIL_BEAST_AWAKENING),
            Map.entry("흉수", MercenaryCategory.EVIL_BEAST),
            Map.entry("신수", MercenaryCategory.DIVINE_BEAST),
            Map.entry("각성장수", MercenaryCategory.GENERAL_AWAKENING),
            Map.entry("개조장수", MercenaryCategory.MODIFIED_GENERAL),
            Map.entry("2차장수", MercenaryCategory.SECOND_GRADE_GENERAL),
            Map.entry("1차장수", MercenaryCategory.FIRST_GRADE_GENERAL),
            Map.entry("고용몬스터", MercenaryCategory.HIRED_MONSTER),
            Map.entry("전직몬스터", MercenaryCategory.EVOLVE_MONSTER),
            Map.entry("정령몬스터", MercenaryCategory.SPIRIT_MONSTER),
            Map.entry("주인공", MercenaryCategory.PROTAGONIST)
    );

    /** .nat-info / 이름 괄호 안 국가 텍스트 → Nation 매핑 */
    private static final Map<String, Nation> NATION_MAP = Map.of(
            "조선", Nation.JOSEON,
            "중국", Nation.CHINA,
            "일본", Nation.JAPAN,
            "대만", Nation.TAIWAN,
            "인도", Nation.INDIA,
            "몽골", Nation.MONGOL
    );

    /** stat-grid 스탯 접두어 → StatType 매핑 (속성 스탯 제외 — 별도 처리) */
    private static final Map<String, StatType> STAT_KEY_MAP = Map.ofEntries(
            Map.entry("힘", StatType.STRENGTH),
            Map.entry("민첩", StatType.DEXTERITY),
            Map.entry("생명", StatType.VITALITY),
            Map.entry("지력", StatType.INTELLECT),
            Map.entry("타저", StatType.HITTING_RESISTANCE),
            Map.entry("마저", StatType.MAGIC_RESISTANCE),
            Map.entry("시야", StatType.SIGHT)
    );

    /** 속성 접두어 → Nature 매핑 — "화속성", "회속성"(타이핑 오류) 등 포함 */
    private static final Map<String, Nature> ELEMENT_PREFIX_MAP = Map.of(
            "화속성", Nature.FIRE,
            "회속성", Nature.FIRE,  // 거상짱 타이핑 오류 대응
            "수속성", Nature.WATER,
            "뇌속성", Nature.THUNDER,
            "풍속성", Nature.AIR,
            "땅속성", Nature.EARTH
    );

    private GersangjjangMercenaryParser() {}

    /**
     * 거상짱 용병 페이지(인덱스 또는 카테고리)를 파싱한다.
     *
     * <p>h2 섹션을 순회하며 해당 섹션 아래 .card-grid 내 카드들을 파싱한다.
     * h2 텍스트가 카테고리를 결정한다.
     *
     * @param doc 거상짱 용병 페이지 Document
     * @return 파싱된 용병 행 목록
     */
    public static List<MercenaryRow> parsePage(Document doc) {
        List<MercenaryRow> results = new ArrayList<>();

        // h2 → 다음 .card-grid 형태로 섹션 순회
        for (Element h2 : doc.select("h2")) {
            String sectionText = h2.text().trim();
            MercenaryCategory category = resolveCategory(sectionText);

            Element cardGrid = h2.nextElementSibling();
            while (cardGrid != null && !cardGrid.tagName().equals("h2")) {
                if (cardGrid.hasClass("card-grid")) {
                    for (Element card : cardGrid.select(".card")) {
                        MercenaryRow row = parseCard(card, category);
                        if (row != null) results.add(row);
                    }
                }
                cardGrid = cardGrid.nextElementSibling();
            }
        }

        // h2 없이 바로 .card-grid만 있는 경우 (단일 카테고리 페이지)
        if (results.isEmpty()) {
            for (Element card : doc.select(".card-grid .card")) {
                MercenaryRow row = parseCard(card, null);
                if (row != null) results.add(row);
            }
        }

        log.debug("거상짱 용병 페이지 파싱: {}개", results.size());
        return results;
    }

    /**
     * 인덱스 페이지에서 카테고리 링크 목록을 추출한다.
     *
     * <p>href는 "/yongbing/korea.asp" 형태의 절대 경로이므로 도메인만 붙인다.
     * "/item/" 경로(수호부 등 아이템 페이지)는 제외한다.
     */
    public static List<String> parseCategoryLinks(Document doc) {
        List<String> links = new ArrayList<>();
        String base = "https://www.gersangjjang.com";

        for (Element link : doc.select(".classification-container .data a")) {
            String href = link.attr("href").trim();
            if (href.isBlank()) continue;
            if (href.startsWith("/item/")) continue;  // 수호부 등 아이템 페이지 제외
            links.add(base + href);
        }

        log.debug("거상짱 용병 카테고리 링크 {}개 파싱", links.size());
        return links;
    }

    // ── 카드 파싱 ─────────────────────────────────────────────────────────────

    private static MercenaryRow parseCard(Element card, MercenaryCategory defaultCategory) {
        Element nameEl = card.selectFirst(".name-section strong");
        if (nameEl == null) return null;

        String rawName = nameEl.text().trim();
        if (rawName.isBlank()) return null;

        // 국가 추출 (타입B: 이름에 포함)
        Nation nation = parseNationFromName(rawName);

        // 속성 한자 / 국가명 접미사 제거 → 순수 이름
        String cleanName = NAME_NATURE_SUFFIX.matcher(rawName).replaceAll("").trim();
        cleanName = NAME_NATION_SUFFIX.matcher(cleanName).replaceAll("").trim();

        // 국가 보완: .nat-info (타입A)
        if (nation == Nation.NONE) {
            Element natEl = card.selectFirst(".nat-info");
            if (natEl != null) {
                nation = NATION_MAP.getOrDefault(natEl.text().trim(), Nation.NONE);
            }
        }

        List<ParsedStat> stats = new ArrayList<>();
        Nature nature = null;

        // 타입A: .stat-grid span — "key: value%"
        Elements statSpans = card.select(".stat-grid span");
        if (!statSpans.isEmpty()) {
            for (Element span : statSpans) {
                String text = span.text().trim();
                StatNaturePair pair = parseStatGridEntry(text);
                if (pair != null) {
                    stats.add(pair.stat());
                    if (pair.nature() != null) nature = pair.nature();
                }
            }
        } else {
            // 타입B: .stat-line 텍스트 — "힘: 60 민첩: 30 ..."
            Element statLine = card.selectFirst(".stat-line");
            if (statLine != null) {
                Matcher m = STAT_VALUE_PATTERN.matcher(statLine.text());
                while (m.find()) {
                    String key = m.group(1);
                    int value = Integer.parseInt(m.group(2));
                    StatType type = STAT_KEY_MAP.get(key);
                    if (type != null) stats.add(new ParsedStat(type, value));
                }
            }
        }

        // 스킬 / 특성 파싱 (label=스킬 info-row)
        List<String> skills = new ArrayList<>();
        List<String> characteristicNames = new ArrayList<>();

        for (Element infoRow : card.select(".info-row")) {
            Element labelEl = infoRow.selectFirst(".label");
            Element contentEl = infoRow.selectFirst(".content");
            if (labelEl == null || contentEl == null) continue;

            if ("스킬".equals(labelEl.text().trim())) {
                parseSkillContent(contentEl.html(), skills, characteristicNames);
                break;
            }
        }

        return new MercenaryRow(cleanName, defaultCategory, nation, nature, stats,
                skills, characteristicNames);
    }

    /**
     * stat-grid span 텍스트("힘: 50", "타저: 75%", "화속성: 20") 파싱.
     * 속성 스탯이면 Nature 정보도 함께 반환한다.
     */
    private static StatNaturePair parseStatGridEntry(String text) {
        Matcher m = STAT_VALUE_PATTERN.matcher(text);
        if (!m.find()) return null;

        String key = m.group(1);
        int value = Integer.parseInt(m.group(2));

        // 속성값 스탯 — "화속성", "수속성" 등
        Nature elementNature = ELEMENT_PREFIX_MAP.get(key);
        if (elementNature != null) {
            return new StatNaturePair(new ParsedStat(StatType.ELEMENT_VALUE, value), elementNature);
        }

        // 일반 스탯
        StatType type = STAT_KEY_MAP.get(key);
        if (type != null) return new StatNaturePair(new ParsedStat(type, value), null);

        return null;
    }

    /**
     * 스킬 info-row의 HTML 내용을 파싱하여 스킬명과 특성명을 분리한다.
     *
     * <p>형식: {@code 스킬A, 스킬B / <b>특성:</b> 특성A, 특성B}
     * " / " 구분자 앞은 스킬, {@code <b>특성:</b>} 뒤는 특성명 목록.
     */
    private static void parseSkillContent(String html, List<String> skills,
                                          List<String> characteristicNames) {
        // b 태그를 기준으로 분리: 앞부분(스킬), 뒷부분(특성)
        int bIdx = html.indexOf("<b>특성:</b>");
        String skillPart;
        String charPart = null;

        if (bIdx >= 0) {
            skillPart = html.substring(0, bIdx);
            charPart = html.substring(bIdx + "<b>특성:</b>".length());
        } else {
            skillPart = html;
        }

        // 스킬 파싱: " / " 구분자 앞 텍스트, HTML 태그 제거 후 쉼표 분리
        String skillText = skillPart.replaceAll("<[^>]+>", "")
                .replaceAll("\\s*/\\s*$", "").trim();
        for (String s : skillText.split(",")) {
            String skill = s.trim();
            if (!skill.isBlank()) skills.add(skill);
        }

        // 특성 파싱: 쉼표 분리
        if (charPart != null) {
            String charText = charPart.replaceAll("<[^>]+>", "").trim();
            for (String c : charText.split("[,，]")) {
                String char_ = c.trim();
                if (!char_.isBlank()) characteristicNames.add(char_);
            }
        }
    }

    /**
     * 이름 문자열에서 괄호 안 국가명을 추출한다.
     * 예: "조승훈(중국)" → Nation.CHINA
     */
    private static Nation parseNationFromName(String name) {
        Matcher m = NAME_NATION_SUFFIX.matcher(name);
        if (m.find()) {
            String nation = m.group(1);
            return NATION_MAP.getOrDefault(nation, Nation.NONE);
        }
        return Nation.NONE;
    }

    /** h2 텍스트 → MercenaryCategory. 매핑 실패 시 null. */
    private static MercenaryCategory resolveCategory(String h2Text) {
        for (Map.Entry<String, MercenaryCategory> entry : H2_CATEGORY_MAP.entrySet()) {
            if (h2Text.contains(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    // ── 결과 레코드 ───────────────────────────────────────────────────────────

    public record ParsedStat(StatType statType, int value) {}

    private record StatNaturePair(ParsedStat stat, Nature nature) {}

    /**
     * 카드 1행 파싱 결과.
     *
     * @param characteristicNames 특성명 목록 (레벨 수치 없음 — 관리자 수동 입력)
     */
    public record MercenaryRow(
            String name,
            MercenaryCategory category,
            Nation nation,
            Nature nature,
            List<ParsedStat> stats,
            List<String> skills,
            List<String> characteristicNames
    ) {}
}
