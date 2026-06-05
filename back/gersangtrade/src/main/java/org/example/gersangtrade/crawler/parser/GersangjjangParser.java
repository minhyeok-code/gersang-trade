package org.example.gersangtrade.crawler.parser;

import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
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

    /** 수집 제외 href 목록 — 유물/전설장수/각성개조는 크롤링 대상 외.
     *  잡화/소모품/재료 카테고리는 GersangjjangMaterialTasklet이 별도 처리하므로 여기서 제외. */
    private static final Set<String> EXCLUDED_HREFS = Set.of(
            "yiwu.asp",                                          // 유물
            "4j_lvbu.asp", "4j_nobu.asp", "4j_choi.asp",        // 전설장수 火
            "4j_chiyome.asp", "4j_chosen.asp", "4j_mazo.asp",   // 전설장수 水
            "4j_meng.asp", "4j_boku.asp", "4j_hong.asp",        // 전설장수 風
            "4j_zhumeng.asp", "4j_hua.asp",                      // 전설장수 雷
            "4j_baji.asp", "4j_akbar.asp",                       // 전설장수 土
            "3jiang_k.asp", "3jiang_j.asp", "3jiang_c.asp",     // 각성/개조
            "3jiang_t.asp", "3jiang_i.asp",                      // 각성/개조
            // ── 잡화 (GersangjjangMaterialTasklet 처리) ──────────────────────
            "ti.asp", "mo.asp", "fuhuo.asp", "shiwu.asp",
            "jiaoyipin.asp", "gongju.asp",
            // ── 소모품 ────────────────────────────────────────────────────────
            "cash1.asp", "cash2.asp", "cash3.asp",
            "bianshenshu.asp", "xinyong.asp",
            // ── 재료 (보석 baoshi.asp 포함 — Gem 엔티티가 별도 처리) ──────────
            "kuangshi.asp", "baoshi.asp", "yinji.asp"
    );

    /** 카테고리 메타 — 슬롯과 장비 종류(NORMAL/APPEARANCE), 슬롯 혼재 여부 */
    private record CategoryMeta(EquipmentSlot slot, EquipmentKind kind, boolean mixed) {
        static CategoryMeta normal(EquipmentSlot slot) {
            return new CategoryMeta(slot, EquipmentKind.NORMAL, false);
        }
        static CategoryMeta appearance(EquipmentSlot slot) {
            return new CategoryMeta(slot, EquipmentKind.APPEARANCE, false);
        }
        /** 슬롯 혼재 페이지 — 아이템명 suffix로 슬롯을 감지한다 */
        static CategoryMeta mixedSlots() {
            return new CategoryMeta(null, EquipmentKind.NORMAL, true);
        }
    }

    /**
     * 카테고리 링크 텍스트 → (slot, kind) 매핑.
     * 목록에 없는 링크 텍스트는 MATERIAL로 처리하고 DEBUG 로그.
     */
    private static final Map<String, CategoryMeta> CATEGORY_SLOT_MAP = Map.ofEntries(
            // ── 무기 근거리 ──────────────────────────────────────────────────────
            Map.entry("도검",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("도끼",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("창극",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("조도",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("차크람",     CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("쌍검",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("곤봉",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("보호대",     CategoryMeta.normal(EquipmentSlot.WEAPON)),
            // ── 무기 중거리 ──────────────────────────────────────────────────────
            Map.entry("인형",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("지팡이",     CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("쿠크리",     CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("사냥추",     CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("표창",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("염주",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("목탁",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("부채",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("구슬",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("길들인뱀",   CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("방울",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("거울",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("부적",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("침.바늘",    CategoryMeta.normal(EquipmentSlot.WEAPON)),
            // ── 무기 원거리 ──────────────────────────────────────────────────────
            Map.entry("활.궁",      CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("조총",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("석궁",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("화포",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            // ── 무기 기타 ────────────────────────────────────────────────────────
            Map.entry("변신무기",   CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("장인",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("기타무기",   CategoryMeta.normal(EquipmentSlot.WEAPON)),
            // ── 몹용병 (장착 무기 슬롯) ──────────────────────────────────────────
            Map.entry("당나귀",     CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("정령몹",     CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("환수",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("신수",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            Map.entry("흉수",       CategoryMeta.normal(EquipmentSlot.WEAPON)),
            // ── 방어구 ────────────────────────────────────────────────────────────
            Map.entry("갑옷",   CategoryMeta.normal(EquipmentSlot.ARMOR)),
            Map.entry("투구",   CategoryMeta.normal(EquipmentSlot.HELMET)),
            Map.entry("장갑",   CategoryMeta.normal(EquipmentSlot.GLOVES)),
            Map.entry("요대",   CategoryMeta.normal(EquipmentSlot.BELT)),
            Map.entry("신발",   CategoryMeta.normal(EquipmentSlot.SHOES)),
            Map.entry("반지",   CategoryMeta.normal(EquipmentSlot.RING)),
            // ── 의복 ─────────────────────────────────────────────────────────────
            Map.entry("의복 (cash,event)", CategoryMeta.normal(EquipmentSlot.ARMOR)),
            Map.entry("의복 (생산,드랍)",   CategoryMeta.normal(EquipmentSlot.ARMOR)),
            Map.entry("모자 (cash,event)", CategoryMeta.normal(EquipmentSlot.HELMET)),
            Map.entry("모자 (생산,보상)",   CategoryMeta.normal(EquipmentSlot.HELMET)),
            // ── 속성장비 (외변) ───────────────────────────────────────────────────
            Map.entry("장신구", CategoryMeta.appearance(EquipmentSlot.ACCESSORY)),
            Map.entry("무신",   CategoryMeta.appearance(EquipmentSlot.DIVINE)),
            Map.entry("팔찌",   CategoryMeta.appearance(EquipmentSlot.BRACELET)),
            Map.entry("각반",   CategoryMeta.appearance(EquipmentSlot.LEGGING)),
            Map.entry("귀걸이", CategoryMeta.appearance(EquipmentSlot.EARRING)),
            Map.entry("목걸이", CategoryMeta.appearance(EquipmentSlot.NECKLACE)),
            Map.entry("보주",   CategoryMeta.appearance(EquipmentSlot.ORB)),
            Map.entry("날개",   CategoryMeta.appearance(EquipmentSlot.WING)),
            Map.entry("칭호",   CategoryMeta.appearance(EquipmentSlot.TITLE)),
            // ── 보조 슬롯 ─────────────────────────────────────────────────────────
            Map.entry("수호부(신수,천왕,명왕)", CategoryMeta.normal(EquipmentSlot.TALISMAN)),
            // ── 슬롯 혼재 페이지 (아이템명 suffix로 감지) ─────────────────────────
            Map.entry("지국천왕(각성)", CategoryMeta.mixedSlots()),
            Map.entry("다문천왕(각성)", CategoryMeta.mixedSlots()),
            Map.entry("광목천왕(각성)", CategoryMeta.mixedSlots()),
            Map.entry("증장천왕(각성)", CategoryMeta.mixedSlots()),
            Map.entry("항삼세명왕(火)", CategoryMeta.mixedSlots()),
            Map.entry("금강야차명왕(水)", CategoryMeta.mixedSlots()),
            Map.entry("대위덕명왕(風)", CategoryMeta.mixedSlots()),
            Map.entry("군다리명왕(雷)", CategoryMeta.mixedSlots()),
            Map.entry("부동명왕(地)",  CategoryMeta.mixedSlots()),
            // ── 전용 장수 장비 (z_kr1.asp ~ z_in2.asp) ──────────────────────────
            Map.entry("조선男", CategoryMeta.mixedSlots()),
            Map.entry("조선女", CategoryMeta.mixedSlots()),
            Map.entry("일본男", CategoryMeta.mixedSlots()),
            Map.entry("일본女", CategoryMeta.mixedSlots()),
            Map.entry("중국男", CategoryMeta.mixedSlots()),
            Map.entry("중국女", CategoryMeta.mixedSlots()),
            Map.entry("대만男", CategoryMeta.mixedSlots()),
            Map.entry("대만女", CategoryMeta.mixedSlots()),
            Map.entry("인도男", CategoryMeta.mixedSlots()),
            Map.entry("인도女", CategoryMeta.mixedSlots())
    );

    /**
     * 카테고리 링크 정보.
     *
     * @param url  카테고리 페이지 절대 URL
     * @param text 링크 텍스트 (카테고리명)
     * @param slot 장비 슬롯 (null이면 MATERIAL 카테고리)
     * @param kind 장비 종류 (null이면 MATERIAL 카테고리)
     */
    public record CategoryInfo(String url, String text, EquipmentSlot slot, EquipmentKind kind, boolean mixed) {
        public boolean isEquipment() { return slot != null || mixed; }
        public boolean isMixed()     { return mixed; }
    }

    /** 공격력 행 파싱 패턴 — "공 220-250", "공 220~250", "공 220～250"(전각 물결 포함) */
    private static final Pattern ATTACK_PATTERN =
            Pattern.compile("^공\\s+(\\d+)(?:[-~～](\\d+))?$");

    /**
     * 일반 스탯 행 파싱 패턴.
     * 부호 선택적: "힘+300", "방어 19", "모든 능력치+600" 모두 허용.
     * 한글 사이 공백 허용: "모든 능력치".
     */
    private static final Pattern STAT_PATTERN =
            Pattern.compile("^([가-힣]+(?:\\s[가-힣]+)*)\\s*([+\\-]?)(\\d+)%?$");

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
            Map.entry("생", StatType.VITALITY),           // 약어
            Map.entry("민첩", StatType.DEXTERITY),
            Map.entry("민", StatType.DEXTERITY),           // 약어
            Map.entry("지력", StatType.INTELLECT),
            Map.entry("지", StatType.INTELLECT),           // 약어
            Map.entry("방어", StatType.DEFENSE),
            Map.entry("방어력", StatType.DEFENSE),
            Map.entry("시야", StatType.SIGHT),
            Map.entry("명중률", StatType.HIT_RATE),
            Map.entry("크리티컬확률", StatType.CRITICAL_CHANCE),
            Map.entry("마법저항", StatType.MAGIC_RESISTANCE),
            Map.entry("마저", StatType.MAGIC_RESISTANCE),
            Map.entry("타격저항", StatType.HITTING_RESISTANCE),
            Map.entry("타저", StatType.HITTING_RESISTANCE),
            Map.entry("속성값", StatType.ELEMENT_VALUE),
            Map.entry("속성깎", StatType.ELEMENT_PIERCE),
            Map.entry("저항깎", StatType.RESIST_PIERCE),
            Map.entry("모든능력치", StatType.ALL_STAT),
            Map.entry("모든 능력치", StatType.ALL_STAT)   // 공백 표기 대응
    );

    /** 속성 접두어("불", "뇌", "물", "풍", "땅") → Element 매핑 */
    private static final Map<String, org.example.gersangtrade.domain.catalog.enums.Element> ELEMENT_PREFIX_MAP = Map.of(
            "불", org.example.gersangtrade.domain.catalog.enums.Element.FIRE,
            "뇌", org.example.gersangtrade.domain.catalog.enums.Element.THUNDER,
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
     * 속성값 단축 표기 패턴 — "풍속성+20", "불속성+5" 등.
     * '값'/'깎' 없이 속성명+속성으로만 표기된 경우로, 항상 ELEMENT_VALUE로 처리한다.
     * group(1)=속성접두어, group(2)=부호, group(3)=수치
     */
    private static final Pattern ELEMENT_VALUE_SHORT_PATTERN =
            Pattern.compile("^([불뇌물풍땅])속성([+\\-])(\\d+)$");

    /**
     * 파싱된 단일 스탯 (StatType + element + 수치 + StatUnit).
     * 속성 구분 없는 스탯은 element=NONE. 속성값(범용)은 element=ADAPTIVE.
     */
    public record ParsedStat(StatType statType,
                             org.example.gersangtrade.domain.catalog.enums.Element element,
                             int value,
                             StatUnit statUnit) {
        /** element=NONE, statUnit=FLAT 단축 생성자 */
        public ParsedStat(StatType statType, int value) {
            this(statType, org.example.gersangtrade.domain.catalog.enums.Element.NONE, value, StatUnit.FLAT);
        }
        /** element=NONE 단축 생성자 (statUnit 지정) */
        public ParsedStat(StatType statType, int value, StatUnit statUnit) {
            this(statType, org.example.gersangtrade.domain.catalog.enums.Element.NONE, value, statUnit);
        }
    }

    public record ItemRow(String name, List<ParsedStat> stats, List<String> skills) {}

    public record MaterialRow(String name) {}

    /**
     * 재료 카테고리 페이지에서 아이템 목록을 추출한다.
     *
     * <p>재료 페이지 HTML 구조:
     * <pre>{@code
     * <div class="data-row">
     *   <div class="cell w-img"><img src="img3/guizhong/satong.gif"></div>
     *   <div class="cell w-name"><div class="name-wrap">사통팔달<br>(상단전용)</div></div>
     *   <div class="cell w-info">...</div>
     * </div>
     * }</pre>
     * 장비 페이지와 달리 {@code .w-name strong}이 없고 {@code .name-wrap} 내부에 이름이 있다.
     * {@code <br>}은 공백으로 치환하여 다중 줄 이름을 하나의 문자열로 합친다.
     *
     * @param doc 카테고리 페이지 Document
     * @return 아이템 행 목록 (빈 페이지이면 빈 리스트)
     */
    public static List<MaterialRow> parseMaterialRows(Document doc) {
        List<MaterialRow> rows = new ArrayList<>();

        for (Element row : doc.select(".data-row")) {
            Element nameWrap = row.selectFirst(".w-name .name-wrap");
            if (nameWrap == null) continue;

            // br 태그를 공백으로 치환 후 HTML 태그 제거하여 순수 텍스트 추출
            // 말미의 수량 표기("1 근", "2근" 등)는 아이템명이 아니므로 제거
            String name = nameWrap.html()
                    .replaceAll("(?i)<br[^>]*>", " ")
                    .replaceAll("<[^>]+>", "")
                    .replaceAll("\\s+", " ")
                    .trim()
                    .replaceAll("\\s*\\d+\\s*근$", "")
                    .trim();
            if (name.isBlank()) continue;

            rows.add(new MaterialRow(name));
        }

        return rows;
    }

    private GersangjjangParser() {}

    /**
     * 인덱스 페이지에서 아이템 카테고리 정보 목록을 추출한다.
     *
     * <p>모든 섹션(장수 포함)의 링크를 수집하되, {@link #EXCLUDED_HREFS}에 포함된 href와
     * {@code [}로 시작하는 특수 페이지(세트효과, 장날장부 등)는 제외한다.
     * 링크 텍스트를 {@link #CATEGORY_SLOT_MAP}에서 조회해 장비 슬롯을 결정한다.
     *
     * @param doc 인덱스 페이지 Document
     * @return 카테고리 정보 목록 (URL + 링크텍스트 + 슬롯)
     */
    public static List<CategoryInfo> parseCategoryLinks(Document doc) {
        List<CategoryInfo> result = new ArrayList<>();

        for (Element link : doc.select(".classification-container .data a")) {
            String href = link.attr("href").trim();
            String text = link.text().trim();
            if (href.isBlank() || text.startsWith("[")) continue;
            if (EXCLUDED_HREFS.contains(href)) {
                log.debug("href 제외: {}", href);
                continue;
            }
            CategoryMeta meta = CATEGORY_SLOT_MAP.get(text);
            if (meta == null && !text.isBlank()) {
                log.debug("카테고리 슬롯 미매핑 (MATERIAL로 처리): [{}] ({})", text, href);
            }
            EquipmentSlot slot = meta != null ? meta.slot() : null;
            EquipmentKind kind  = meta != null ? meta.kind()  : null;
            boolean mixed = meta != null && meta.mixed();
            result.add(new CategoryInfo(BASE_URL + href, text, slot, kind, mixed));
        }

        log.debug("거상짱 카테고리 링크 {}개 파싱", result.size());
        return result;
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

    /** 값만 있는 행 패턴 — "+75", "-30", "+200%" 등 (접두어 없이 부호+숫자만) */
    private static final Pattern VALUE_ONLY_PATTERN = Pattern.compile("^[+\\-]\\d+%?$");

    /**
     * .w-stat 셀의 TextNode들을 br 단위로 순회하며 stats/skills에 추가한다.
     * "모든능력치" / "+75" 처럼 접두어와 값이 두 줄로 분리된 경우 병합 후 파싱한다.
     */
    private static void parseStatCell(Element statEl, List<ParsedStat> stats, List<String> skills) {
        if (statEl == null) return;

        List<String> lines = new ArrayList<>();
        for (org.jsoup.nodes.Node child : statEl.childNodes()) {
            if (!(child instanceof TextNode textNode)) continue;
            String line = textNode.text().trim();
            if (!line.isBlank()) lines.add(line);
        }

        // 접두어만 있는 줄 + 값만 있는 다음 줄 → 병합 ("모든능력치" + "+75" → "모든능력치+75")
        List<String> merged = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String cur = lines.get(i);
            if (i + 1 < lines.size()
                    && STAT_PREFIX_MAP.containsKey(cur)
                    && VALUE_ONLY_PATTERN.matcher(lines.get(i + 1)).matches()) {
                merged.add(cur + lines.get(i + 1));
                i++;
            } else {
                merged.add(cur);
            }
        }

        for (String line : merged) {
            parseStatLine(line, stats, skills);
        }
    }

    /**
     * .w-stat 셀의 텍스트 1행을 파싱하여 stats 또는 skills에 추가한다.
     */
    private static void parseStatLine(String line, List<ParsedStat> stats, List<String> skills) {
        // "-" 단독 표기 = 해당 스탯 없음 표시 → 무시
        if ("-".equals(line)) return;

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

        // 속성별 스탯 (완전 표기): "불속성값+5", "뇌속성깎-3", "물속성값+10" 등 — 항상 FLAT
        Matcher elementStatMatcher = ELEMENT_STAT_PATTERN.matcher(line);
        if (elementStatMatcher.find()) {
            org.example.gersangtrade.domain.catalog.enums.Element element =
                    ELEMENT_PREFIX_MAP.get(elementStatMatcher.group(1));
            StatType statType = elementStatMatcher.group(2).equals("속성값")
                    ? StatType.ELEMENT_VALUE : StatType.ELEMENT_PIERCE;
            int sign = elementStatMatcher.group(3).equals("-") ? -1 : 1;
            int value = Integer.parseInt(elementStatMatcher.group(4)) * sign;
            stats.add(new ParsedStat(statType, element, value, StatUnit.FLAT));
            return;
        }

        // 속성값 단축 표기: "풍속성+20", "불속성+5" 등 → ELEMENT_VALUE, 특정 속성, FLAT
        Matcher elementValueShortMatcher = ELEMENT_VALUE_SHORT_PATTERN.matcher(line);
        if (elementValueShortMatcher.find()) {
            org.example.gersangtrade.domain.catalog.enums.Element element =
                    ELEMENT_PREFIX_MAP.get(elementValueShortMatcher.group(1));
            int sign = elementValueShortMatcher.group(2).equals("-") ? -1 : 1;
            int value = Integer.parseInt(elementValueShortMatcher.group(3)) * sign;
            stats.add(new ParsedStat(StatType.ELEMENT_VALUE, element, value, StatUnit.FLAT));
            return;
        }

        // 일반 수치 스탯: "힘+300", "방어 19", "모든 능력치+600" 등
        Matcher statMatcher = STAT_PATTERN.matcher(line);
        if (statMatcher.find()) {
            String prefix = statMatcher.group(1);
            int sign = "-".equals(statMatcher.group(2)) ? -1 : 1;
            int value = Integer.parseInt(statMatcher.group(3)) * sign;
            StatType type = STAT_PREFIX_MAP.get(prefix);
            if (type != null) {
                StatUnit statUnit = line.endsWith("%") ? StatUnit.PERCENT : StatUnit.FLAT;
                // "속성값+n" 단독 표기는 용병 속성 추종 (ADAPTIVE)
                org.example.gersangtrade.domain.catalog.enums.Element element =
                        (type == StatType.ELEMENT_VALUE)
                                ? org.example.gersangtrade.domain.catalog.enums.Element.ADAPTIVE
                                : org.example.gersangtrade.domain.catalog.enums.Element.NONE;
                stats.add(new ParsedStat(type, element, value, statUnit));
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
