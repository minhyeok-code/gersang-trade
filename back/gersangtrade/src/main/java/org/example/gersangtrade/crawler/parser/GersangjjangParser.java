package org.example.gersangtrade.crawler.parser;

import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 거상짱(gersangjjang.com) 아이템 페이지 파서.
 *
 * <p>거상짱은 전통적인 SSR ASP 사이트로 Jsoup 정적 파싱이 정상 동작한다.
 *
 * <p>페이지 구조:
 * <ul>
 *   <li>인덱스 ({@code /item/index.asp}) — {@code .classification-container .data a}에 카테고리별 ASP 링크 목록</li>
 *   <li>카테고리 페이지 (예: {@code dao.asp}) — {@code .data-row}에 아이템명(.w-name strong)과 스탯(.w-stat)</li>
 * </ul>
 */
@Slf4j
public class GersangjjangParser {

    public static final String BASE_URL = "https://www.gersangjjang.com/item/";

    /**
     * 아이템이 아닌 카테고리 페이지 — 수집 제외 대상.
     * 칭호(chenghao.asp): 게임 내 칭호 시스템, 거래 아이템 아님.
     */
    private static final Set<String> EXCLUDED_HREFS = Set.of(
            "chenghao.asp"
    );

    /** 공격력 행 파싱 패턴 — "공 220-250", "공 220~250", "공 220" */
    private static final Pattern ATTACK_PATTERN =
            Pattern.compile("^공\\s+(\\d+)(?:[-~](\\d+))?$");

    /** 일반 스탯 행 파싱 패턴 — "힘+300", "지력-50", "마저+75%", "타저+80%" 등 (말미 % 허용) */
    private static final Pattern STAT_PATTERN =
            Pattern.compile("^([가-힣]+)([+\\-])(\\d+)%?$");

    /**
     * 거상짱 한국어 스탯 접두어 → StatType 매핑.
     * 공격력(공)은 별도 ATTACK_PATTERN으로 처리하므로 여기서 제외.
     * 속성값/속성깎은 속성 접두어가 없는 범용 형태("속성값+10")만 여기서 처리.
     * 속성별("불속성값+5" 등)은 ELEMENT_STAT_PREFIX_MAP으로 별도 처리.
     */
    private static final Map<String, StatType> STAT_PREFIX_MAP = Map.ofEntries(
            Map.entry("힘", StatType.STRENGTH),
            Map.entry("생명", StatType.VITALITY),
            Map.entry("생명력", StatType.VITALITY),
            Map.entry("민첩", StatType.DEXTERITY),
            Map.entry("지력", StatType.INTELLECT),
            Map.entry("방어", StatType.DEFENSE),
            Map.entry("방어력", StatType.DEFENSE),   // "방어력+50" 표기 대응
            Map.entry("시야", StatType.SIGHT),
            Map.entry("명중률", StatType.HIT_RATE),
            Map.entry("크리티컬확률", StatType.CRITICAL_CHANCE),
            Map.entry("마법저항", StatType.MAGIC_RESISTANCE),
            Map.entry("마저", StatType.MAGIC_RESISTANCE),
            Map.entry("타격저항", StatType.HITTING_RESISTANCE),
            Map.entry("타저", StatType.HITTING_RESISTANCE),
            Map.entry("속성값", StatType.ELEMENT_VALUE),
            Map.entry("속성깎", StatType.ELEMENT_PIERCE),
            Map.entry("저항깎", StatType.RESIST_PIERCE)
    );

    /** 속성 접두어("불", "뇌", "물", "풍", "땅") → Element 매핑 */
    private static final Map<String, org.example.gersangtrade.domain.catalog.enums.Element> ELEMENT_PREFIX_MAP = Map.of(
            "불", org.example.gersangtrade.domain.catalog.enums.Element.FIRE,
            "뇌", org.example.gersangtrade.domain.catalog.enums.Element.LIGHTNING,
            "물", org.example.gersangtrade.domain.catalog.enums.Element.WATER,
            "풍", org.example.gersangtrade.domain.catalog.enums.Element.WIND,
            "땅", org.example.gersangtrade.domain.catalog.enums.Element.EARTH
    );

    /**
     * 속성별 스탯 행 파싱 패턴.
     * "불속성값+5", "뇌속성깎-3", "물속성값+10" 등.
     * group(1)=속성접두어, group(2)=스탯종류(속성값|속성깎), group(3)=부호, group(4)=수치
     */
    private static final Pattern ELEMENT_STAT_PATTERN =
            Pattern.compile("^([불뇌물풍땅])(속성값|속성깎)([+\\-])(\\d+)$");

    /**
     * 파싱된 단일 스탯 (StatType + element + 수치).
     * 속성 구분 없는 스탯은 element = NONE.
     */
    public record ParsedStat(StatType statType,
                             org.example.gersangtrade.domain.catalog.enums.Element element,
                             int value) {
        /** element = NONE 단축 생성자 */
        public ParsedStat(StatType statType, int value) {
            this(statType, org.example.gersangtrade.domain.catalog.enums.Element.NONE, value);
        }
    }

    /**
     * 카테고리 페이지 아이템 1행 파싱 결과.
     *
     * @param name   아이템명
     * @param stats  파싱된 스탯 목록 (없으면 빈 리스트)
     * @param skills 파싱된 고유 스킬명 목록 (없으면 빈 리스트)
     */
    public record ItemRow(String name, List<ParsedStat> stats, List<String> skills) {}

    private GersangjjangParser() {}

    /**
     * 인덱스 페이지에서 아이템 카테고리 URL 목록을 추출한다.
     *
     * <p>모든 섹션(장수 포함)의 링크를 수집하되, {@link #EXCLUDED_HREFS}에 포함된 href와
     * {@code [}로 시작하는 특수 페이지(세트효과, 장날장부 등)는 제외한다.
     *
     * @param doc 인덱스 페이지 Document
     * @return 아이템 카테고리 페이지 절대 URL 목록
     */
    public static List<String> parseCategoryLinks(Document doc) {
        List<String> urls = new ArrayList<>();

        for (Element link : doc.select(".classification-container .data a")) {
            String href = link.attr("href").trim();
            String text = link.text().trim();
            if (href.isBlank() || text.startsWith("[")) continue;
            if (EXCLUDED_HREFS.contains(href)) {
                log.debug("href 제외: {}", href);
                continue;
            }
            urls.add(BASE_URL + href);
        }

        log.debug("거상짱 카테고리 링크 {}개 파싱", urls.size());
        return urls;
    }

    /**
     * 카테고리 페이지에서 아이템 행 목록을 추출한다.
     *
     * <p>거상짱 페이지는 두 가지 HTML 구조를 사용한다:
     * <ul>
     *   <li><b>Type A</b> (baozhu.asp, zhu_bian.asp 등): {@code .data-row} → {@code .w-name strong} + {@code .w-stat}</li>
     *   <li><b>Type B</b> (she.asp, dao.asp 등 무기 페이지): {@code .item-row .sub-row} → {@code .w-name} 첫 텍스트 노드 + {@code .w-stat}</li>
     * </ul>
     *
     * @param doc 카테고리 페이지 Document
     * @return 아이템 행 목록 (빈 페이지이면 빈 리스트)
     */
    public static List<ItemRow> parseItemRows(Document doc) {
        List<ItemRow> rows = new ArrayList<>();

        // Type A: .data-row 구조
        for (Element row : doc.select(".data-row")) {
            Element nameEl = row.selectFirst(".w-name strong");
            if (nameEl == null) continue;
            String name = nameEl.text().trim();
            if (name.isBlank()) continue;

            List<ParsedStat> stats = new ArrayList<>();
            List<String> skills = new ArrayList<>();
            parseStatCell(row.selectFirst(".w-stat"), stats, skills);

            rows.add(new ItemRow(name, stats, skills));
        }

        // Type B: .item-row .sub-row 구조 (무기 페이지 — strong 없이 첫 텍스트 노드가 이름)
        if (rows.isEmpty()) {
            for (Element subRow : doc.select(".item-row .sub-row")) {
                Element nameEl = subRow.selectFirst(".w-name");
                if (nameEl == null) continue;
                // .w-name 안 첫 번째 텍스트 노드 = 아이템명 (이후 <br>+레벨 제외)
                String name = nameEl.childNodes().stream()
                        .filter(n -> n instanceof TextNode)
                        .map(n -> ((TextNode) n).text().trim())
                        .filter(t -> !t.isBlank())
                        .findFirst()
                        .orElse("").trim();
                if (name.isBlank()) continue;

                List<ParsedStat> stats = new ArrayList<>();
                List<String> skills = new ArrayList<>();
                parseStatCell(subRow.selectFirst(".w-stat"), stats, skills);

                rows.add(new ItemRow(name, stats, skills));
            }
        }

        return rows;
    }

    /** .w-stat 셀의 TextNode들을 br 단위로 순회하며 stats/skills에 추가한다 */
    private static void parseStatCell(Element statEl, List<ParsedStat> stats, List<String> skills) {
        if (statEl == null) return;
        for (org.jsoup.nodes.Node child : statEl.childNodes()) {
            if (!(child instanceof TextNode textNode)) continue;
            String line = textNode.text().trim();
            if (line.isBlank()) continue;
            parseStatLine(line, stats, skills);
        }
    }

    /**
     * .w-stat 셀의 텍스트 1행을 파싱하여 stats 또는 skills에 추가한다.
     */
    private static void parseStatLine(String line, List<ParsedStat> stats, List<String> skills) {
        // 공격력 범위: "공 220-250" 또는 "공 220"
        Matcher attackMatcher = ATTACK_PATTERN.matcher(line);
        if (attackMatcher.find()) {
            int min = Integer.parseInt(attackMatcher.group(1));
            int max = attackMatcher.group(2) != null
                    ? Integer.parseInt(attackMatcher.group(2))
                    : min;
            stats.add(new ParsedStat(StatType.MIN_POWER, min));
            stats.add(new ParsedStat(StatType.MAX_POWER, max));
            return;
        }

        // 속성별 스탯: "불속성값+5", "뇌속성깎-3", "물속성값+10" 등
        Matcher elementStatMatcher = ELEMENT_STAT_PATTERN.matcher(line);
        if (elementStatMatcher.find()) {
            org.example.gersangtrade.domain.catalog.enums.Element element =
                    ELEMENT_PREFIX_MAP.get(elementStatMatcher.group(1));
            StatType statType = elementStatMatcher.group(2).equals("속성값")
                    ? StatType.ELEMENT_VALUE : StatType.ELEMENT_PIERCE;
            int sign = elementStatMatcher.group(3).equals("-") ? -1 : 1;
            int value = Integer.parseInt(elementStatMatcher.group(4)) * sign;
            stats.add(new ParsedStat(statType, element, value));
            return;
        }

        // 일반 수치 스탯: "힘+300", "지력+50", "저항깎+20" 등
        Matcher statMatcher = STAT_PATTERN.matcher(line);
        if (statMatcher.find()) {
            String prefix = statMatcher.group(1);
            int sign = statMatcher.group(2).equals("-") ? -1 : 1;
            int value = Integer.parseInt(statMatcher.group(3)) * sign;
            StatType type = STAT_PREFIX_MAP.get(prefix);
            if (type != null) {
                stats.add(new ParsedStat(type, value));
            } else {
                log.debug("미매핑 스탯 접두어 (skill로 처리): {}", line);
                skills.add(line);
            }
            return;
        }

        // 수치 없는 텍스트 — 고유 스킬
        skills.add(line);
    }
}
