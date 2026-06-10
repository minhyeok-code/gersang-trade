package org.example.gersangtrade.crawler.parser;

import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 거상짱 장비 세트 페이지 파서.
 *
 * <p>페이지 구조: {@code .g-table > .g-row}에 세트 1개씩.
 * 각 행은 lv-col(세트명·레벨), item-col(피스 목록), effect-col(세트효과), max-col(최대치)로 구성.
 *
 * <p>피스명 + 세트명 조합으로 아이템명을 구성한다. 예: "공명" + "관" → "공명관"
 */
@Slf4j
public class GersangjjangSetParser {

    /** 피스명 → EquipmentSlot 매핑 (docs/piece.md 기준) */
    private static final Map<String, EquipmentSlot> PIECE_SLOT_MAP = Map.ofEntries(
            // 무기
            Map.entry("극",    EquipmentSlot.WEAPON),
            Map.entry("혼겸",  EquipmentSlot.WEAPON),
            Map.entry("검",    EquipmentSlot.WEAPON),
            Map.entry("주",    EquipmentSlot.WEAPON),
            Map.entry("궁",    EquipmentSlot.WEAPON),
            Map.entry("비",    EquipmentSlot.WEAPON),
            Map.entry("갑",    EquipmentSlot.WEAPON),
            Map.entry("장",    EquipmentSlot.WEAPON),
            Map.entry("월",    EquipmentSlot.WEAPON),
            Map.entry("창",    EquipmentSlot.WEAPON),
            Map.entry("활",    EquipmentSlot.WEAPON),
            Map.entry("화포",  EquipmentSlot.WEAPON),   // 최무선 등 화포 무기
            // 전설장수 전용 무기 타입
            Map.entry("방천화극", EquipmentSlot.WEAPON), // 여포
            Map.entry("지팡이", EquipmentSlot.WEAPON),  // 치요메
            Map.entry("대도",  EquipmentSlot.WEAPON),   // 보쿠텐
            Map.entry("도끼",  EquipmentSlot.WEAPON),   // 맹획
            Map.entry("부채",  EquipmentSlot.WEAPON),   // 초선
            Map.entry("홀판",  EquipmentSlot.WEAPON),   // 마조
            Map.entry("채찍",  EquipmentSlot.WEAPON),   // 레지나
            // 전용 장수 추가 무기 타입
            Map.entry("봉",    EquipmentSlot.WEAPON),   // 봉술 (도사·요가 계열)
            Map.entry("부메랑", EquipmentSlot.WEAPON),  // 부메랑 (인도女 요가)
            Map.entry("령",    EquipmentSlot.WEAPON),   // 방울·령 (대만女 선녀)
            Map.entry("비파",  EquipmentSlot.WEAPON),   // 비파 악기 무기 (무당·도사·선녀)
            Map.entry("갈퀴",  EquipmentSlot.WEAPON),   // 갈퀴 계열
            Map.entry("발톱",  EquipmentSlot.WEAPON),   // 발톱 (일본女 쿠노이치)
            Map.entry("권갑",  EquipmentSlot.WEAPON),   // 권갑 주먹 무기
            // 투구
            Map.entry("머리띠", EquipmentSlot.HELMET),
            Map.entry("관",    EquipmentSlot.HELMET),
            Map.entry("투구",  EquipmentSlot.HELMET),
            Map.entry("깃",    EquipmentSlot.HELMET),
            Map.entry("모자",  EquipmentSlot.HELMET),
            // 전용 장수 추가 투구 타입
            Map.entry("두건",  EquipmentSlot.HELMET),   // 두건 (무당·쿠노이치·궁수)
            Map.entry("삿갓",  EquipmentSlot.HELMET),   // 삿갓 (중국女 도사)
            Map.entry("가발",  EquipmentSlot.HELMET),   // 가발 (대만女 선녀)
            Map.entry("가면",  EquipmentSlot.HELMET),   // 가면 (광목천왕 계열)
            Map.entry("머리장식", EquipmentSlot.HELMET), // 치요메
            Map.entry("패랭이", EquipmentSlot.HELMET),  // 홍길동
            // 갑옷
            Map.entry("갑주",  EquipmentSlot.ARMOR),    // 노부츠나 (주와 구분)
            Map.entry("견갑",  EquipmentSlot.ARMOR),    // 맹획
            Map.entry("도복",  EquipmentSlot.ARMOR),    // 보쿠텐
            Map.entry("예복",  EquipmentSlot.ARMOR),
            Map.entry("갑옷",  EquipmentSlot.ARMOR),
            Map.entry("의",    EquipmentSlot.ARMOR),
            Map.entry("복장",  EquipmentSlot.ARMOR),
            Map.entry("복",    EquipmentSlot.ARMOR),    // 복/한복류 (선녀·도사 계열 의상)
            // 장갑
            Map.entry("보호구", EquipmentSlot.GLOVES),
            Map.entry("보호대", EquipmentSlot.GLOVES),
            Map.entry("장갑",  EquipmentSlot.GLOVES),
            Map.entry("손목",  EquipmentSlot.GLOVES),
            Map.entry("손모",  EquipmentSlot.GLOVES),
            Map.entry("수대",  EquipmentSlot.GLOVES),   // 치요메
            Map.entry("팔찌",  EquipmentSlot.GLOVES),   // 마조
            // 요대
            Map.entry("요대",  EquipmentSlot.BELT),
            Map.entry("허리띠", EquipmentSlot.BELT),
            // 신발
            Map.entry("화",    EquipmentSlot.SHOES),
            Map.entry("실발",  EquipmentSlot.SHOES),
            Map.entry("신발",  EquipmentSlot.SHOES),
            Map.entry("전투화", EquipmentSlot.SHOES),
            Map.entry("짚신",  EquipmentSlot.SHOES),    // 홍길동
            // 반지
            Map.entry("반지",  EquipmentSlot.RING),
            Map.entry("반지2", EquipmentSlot.RING),
            Map.entry("가락지", EquipmentSlot.RING),
            Map.entry("지환",  EquipmentSlot.RING),     // 지환 (광목천왕지환 계열)
            // 수호부
            Map.entry("부",    EquipmentSlot.TALISMAN)
    );

    /** 인라인 스탯 약어 → StatType 매핑 (수치형 스탯용) */
    private static final Map<String, StatType> INLINE_STAT_MAP = Map.ofEntries(
            Map.entry("방어",   StatType.DEFENSE),
            Map.entry("방어력", StatType.DEFENSE),
            Map.entry("힘",    StatType.STRENGTH),
            Map.entry("민",    StatType.DEXTERITY),
            Map.entry("민첩",  StatType.DEXTERITY),
            Map.entry("생",    StatType.VITALITY),
            Map.entry("생명",  StatType.VITALITY),
            Map.entry("생명력", StatType.VITALITY),
            Map.entry("지",    StatType.INTELLECT),
            Map.entry("지력",  StatType.INTELLECT),
            Map.entry("마저",  StatType.MAGIC_RESISTANCE),
            Map.entry("마법저항", StatType.MAGIC_RESISTANCE),
            Map.entry("타저",  StatType.HITTING_RESISTANCE),
            Map.entry("타격저항", StatType.HITTING_RESISTANCE),
            Map.entry("속성값", StatType.ELEMENT_VALUE)
    );

    /** 인라인 스탯 패턴 — "방어300", "힘50", "마저30%", "방어력 150", "공격210-240" */
    private static final Pattern INLINE_STAT_PATTERN =
            Pattern.compile("([가-힣]+)\\s*(\\d+)(?:[-~](\\d+))?(%)?");

    /** 세트 효과 "N종" 단독 패턴 — "6종" */
    private static final Pattern SET_COUNT_PATTERN = Pattern.compile("^(\\d+)종$");

    /** 세트 효과 "N종 스탯" 복합 패턴 — "2종 힘50", "5종 타저25%" */
    private static final Pattern COMBINED_EFFECT_PATTERN = Pattern.compile("^(\\d+)종\\s+(.+)$");

    /** 세트 효과 속성별 스탯 패턴 — "(땅속성3)", "불속성5" (괄호 제거 후 적용) */
    private static final Pattern ELEMENT_VALUE_PATTERN =
            Pattern.compile("^([불뇌물풍땅])속성(\\d+)%?$");

    /** 세트 효과 일반 스탯 패턴 — "힘400", "방어150", "속성값5" */
    private static final Pattern EFFECT_STAT_PATTERN =
            Pattern.compile("^([가-힣]+)(\\d+)(%)?$");

    /** 속성 접두어 → Element 매핑 (세트 효과 파싱용) */
    private static final Map<String, org.example.gersangtrade.domain.catalog.enums.Element> ELEMENT_PREFIX_MAP = Map.of(
            "불", org.example.gersangtrade.domain.catalog.enums.Element.FIRE,
            "뇌", org.example.gersangtrade.domain.catalog.enums.Element.THUNDER,
            "물", org.example.gersangtrade.domain.catalog.enums.Element.WATER,
            "풍", org.example.gersangtrade.domain.catalog.enums.Element.WIND,
            "땅", org.example.gersangtrade.domain.catalog.enums.Element.EARTH
    );

    /** 피스 1행 패턴 — "관 (방어300, 힘50 ...)" or "반지 (...) 2개" */
    private static final Pattern PIECE_LINE_PATTERN =
            Pattern.compile("^(.+?)\\s*\\((.+?)\\)(?:\\s*(\\d+)개)?$");

    /** lv-col 레벨 추출 패턴 — "(215lv)" */
    private static final Pattern LEVEL_PATTERN = Pattern.compile("\\((\\d+)lv\\)");

    /** suffix 길이 내림차순 정렬 — 긴 suffix 우선 매칭 ("전투화" > "화") */
    private static final List<Map.Entry<String, EquipmentSlot>> SUFFIX_SLOT_LIST =
            PIECE_SLOT_MAP.entrySet().stream()
                    .sorted(Comparator.comparingInt((Map.Entry<String, EquipmentSlot> e) -> e.getKey().length()).reversed())
                    .toList();

    private GersangjjangSetParser() {}

    // ───────────────────────────────────────────────────────────────────────
    // Public API
    // ───────────────────────────────────────────────────────────────────────

    /**
     * 아이템명 suffix로 장비 슬롯을 감지한다.
     * 슬롯 혼재 페이지(사천왕 각성, 명왕 등) 처리에 사용.
     * 긴 suffix를 우선 검사하여 오매칭을 방지한다 (예: "전투화" > "화").
     */
    public static Optional<EquipmentSlot> detectSlotByName(String itemName) {
        // 전설장수 등 공백 포함 이름 대응 — "여포의 방천화극", "홍길동 의복"
        String normalized = itemName.replace(" ", "");
        for (Map.Entry<String, EquipmentSlot> entry : SUFFIX_SLOT_LIST) {
            if (normalized.endsWith(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    /**
     * 세트 목록 페이지 전체를 파싱한다.
     *
     * @param doc 세트 페이지 Document
     * @return 파싱된 세트 행 목록
     */
    public static List<ParsedSetRow> parseSetRows(Document doc) {
        List<ParsedSetRow> result = new ArrayList<>();
        for (Element row : doc.select(".g-table .g-row")) {
            try {
                ParsedSetRow parsed = parseRow(row);
                if (parsed != null) result.add(parsed);
            } catch (Exception e) {
                log.warn("세트 행 파싱 실패 (skip): {}", e.getMessage());
            }
        }
        log.debug("세트 {}개 파싱 완료", result.size());
        return result;
    }

    // ───────────────────────────────────────────────────────────────────────
    // 내부 파싱 로직
    // ───────────────────────────────────────────────────────────────────────

    private static ParsedSetRow parseRow(Element row) {
        Element lvEl      = row.selectFirst(".lv-col");
        Element itemEl    = row.selectFirst(".item-col");
        Element effectEl  = row.selectFirst(".effect-col");
        if (lvEl == null || itemEl == null) return null;

        // 세트명 + 레벨 파싱
        String lvText  = lvEl.text().trim();           // "공명\n(215lv)" → "공명(215lv)"
        String rawHtml = lvEl.html();
        String setName = rawHtml.contains("<br>")
                ? lvEl.childNodes().stream()
                      .filter(n -> n instanceof TextNode)
                      .map(n -> ((TextNode) n).text().trim())
                      .filter(t -> !t.isBlank() && !t.startsWith("("))
                      .findFirst()
                      .orElse(lvText.replaceAll("\\(.*\\)", "").trim())
                : lvText.replaceAll("\\(.*\\)", "").trim();

        Matcher lvMatcher = LEVEL_PATTERN.matcher(lvEl.text());
        int level = lvMatcher.find() ? Integer.parseInt(lvMatcher.group(1)) : 0;

        if (setName.isBlank()) return null;

        // 피스 파싱
        List<ParsedPiece> pieces = parsePieces(itemEl, setName);

        // 세트 효과 TextNode 수집 후 ParsedSetEffect 목록으로 파싱
        List<String> rawEffects = new ArrayList<>();
        if (effectEl != null) {
            Element strong = effectEl.selectFirst("strong");
            Element target = strong != null ? strong : effectEl;
            for (Node node : target.childNodes()) {
                if (node instanceof TextNode tn) {
                    String t = tn.text().trim();
                    if (!t.isBlank()) rawEffects.add(t);
                }
            }
        }
        List<ParsedSetEffect> effects = parseSetEffects(rawEffects);

        return new ParsedSetRow(setName, level, pieces, effects);
    }

    private static List<ParsedPiece> parsePieces(Element itemEl, String setName) {
        List<ParsedPiece> pieces = new ArrayList<>();

        // item-col 내용을 <br> 단위로 분리
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (Node node : itemEl.childNodes()) {
            if (node instanceof TextNode tn) {
                current.append(tn.text());
            } else if (node.nodeName().equals("br")) {
                String line = current.toString().trim();
                if (!line.isBlank()) lines.add(line);
                current = new StringBuilder();
            }
        }
        if (!current.toString().isBlank()) lines.add(current.toString().trim());

        for (String line : lines) {
            ParsedPiece piece = parsePieceLine(line, setName);
            if (piece != null) pieces.add(piece);
        }
        return pieces;
    }

    private static ParsedPiece parsePieceLine(String line, String setName) {
        Matcher m = PIECE_LINE_PATTERN.matcher(line);
        if (!m.matches()) {
            log.debug("피스 행 패턴 미매칭 (skip): [{}]", line);
            return null;
        }

        String rawPieceName = m.group(1).trim();
        String statsText    = m.group(2);
        int pieceCount      = m.group(3) != null ? Integer.parseInt(m.group(3)) : 1;

        // 슬롯 결정
        EquipmentSlot slot = PIECE_SLOT_MAP.get(rawPieceName);
        if (slot == null) {
            log.warn("알 수 없는 피스명 (slot 미결정, skip): [{}] in set [{}]", rawPieceName, setName);
            return null;
        }

        // 아이템명 = 세트명 + 피스명 (예: "공명" + "관" = "공명관")
        String itemName = setName + rawPieceName;

        // 스탯 파싱
        List<GersangjjangParser.ParsedStat> stats = parseInlineStats(statsText);

        return new ParsedPiece(rawPieceName, itemName, slot, stats, pieceCount);
    }

    /** 인라인 스탯 문자열에서 모든 스탯을 추출한다 (피스 스탯 파싱용) */
    static List<GersangjjangParser.ParsedStat> parseInlineStats(String statsText) {
        List<GersangjjangParser.ParsedStat> result = new ArrayList<>();
        Matcher m = INLINE_STAT_PATTERN.matcher(statsText);
        while (m.find()) {
            String prefix = m.group(1);
            int val1 = Integer.parseInt(m.group(2));
            String raw2 = m.group(3);
            boolean isPercent = m.group(4) != null;
            StatUnit statUnit = isPercent ? StatUnit.PERCENT : StatUnit.FLAT;

            if ("공격".equals(prefix)) {
                // 공격 범위: MIN + MAX (항상 FLAT)
                int max = raw2 != null ? Integer.parseInt(raw2) : val1;
                result.add(new GersangjjangParser.ParsedStat(StatType.MIN_POWER, val1));
                result.add(new GersangjjangParser.ParsedStat(StatType.MAX_POWER, max));
            } else {
                StatType type = INLINE_STAT_MAP.get(prefix);
                if (type != null) {
                    // "속성값" 단독 표기는 용병 속성 추종 (ADAPTIVE)
                    org.example.gersangtrade.domain.catalog.enums.Element element =
                            (type == StatType.ELEMENT_VALUE)
                                    ? org.example.gersangtrade.domain.catalog.enums.Element.ADAPTIVE
                                    : org.example.gersangtrade.domain.catalog.enums.Element.NONE;
                    result.add(new GersangjjangParser.ParsedStat(type, element, val1, statUnit));
                } else {
                    log.debug("인라인 스탯 미매핑 (skip): [{}]", prefix);
                }
            }
        }
        return result;
    }

    /**
     * raw 텍스트 줄 목록("N종", "힘400", "속성값5", "(땅속성3)" 등)을
     * ParsedSetEffect 목록으로 변환한다.
     */
    private static List<ParsedSetEffect> parseSetEffects(List<String> rawLines) {
        List<ParsedSetEffect> result = new ArrayList<>();
        int currentPieces = 0;

        for (String raw : rawLines) {
            // 괄호 제거 후 처리 ("(땅속성3)" → "땅속성3")
            String line = raw.replaceAll("[()]", "").trim();
            if (line.isBlank()) continue;

            // "N종 스탯" 복합 형식 — "2종 힘50", "5종 타저25%"
            Matcher combinedMatcher = COMBINED_EFFECT_PATTERN.matcher(line);
            if (combinedMatcher.matches()) {
                currentPieces = Integer.parseInt(combinedMatcher.group(1));
                line = combinedMatcher.group(2).trim();
                // 스탯 파싱은 아래 로직으로 fall-through
            } else {
                // "N종" 단독 — 이후 스탯의 requiredPieces 갱신
                Matcher countMatcher = SET_COUNT_PATTERN.matcher(line);
                if (countMatcher.matches()) {
                    currentPieces = Integer.parseInt(countMatcher.group(1));
                    continue;
                }
            }

            if (currentPieces == 0) {
                log.debug("세트 효과 종수 없이 스탯 발견 (skip): [{}]", line);
                continue;
            }

            // 속성별 스탯: "땅속성3", "불속성5" → ELEMENT_VALUE + 특정 속성, FLAT
            Matcher elemMatcher = ELEMENT_VALUE_PATTERN.matcher(line);
            if (elemMatcher.matches()) {
                org.example.gersangtrade.domain.catalog.enums.Element element =
                        ELEMENT_PREFIX_MAP.get(elemMatcher.group(1));
                int value = Integer.parseInt(elemMatcher.group(2));
                result.add(new ParsedSetEffect(currentPieces, StatType.ELEMENT_VALUE,
                        value, StatUnit.FLAT, element));
                continue;
            }

            // 일반 스탯: "힘400", "방어150", "속성값5", "타저25%"
            Matcher statMatcher = EFFECT_STAT_PATTERN.matcher(line);
            if (statMatcher.matches()) {
                String prefix = statMatcher.group(1);
                int value = Integer.parseInt(statMatcher.group(2));
                boolean isPercent = statMatcher.group(3) != null;
                StatUnit statUnit = isPercent ? StatUnit.PERCENT : StatUnit.FLAT;

                StatType type = INLINE_STAT_MAP.get(prefix);
                if (type != null) {
                    // "속성값" 단독 표기는 ADAPTIVE
                    org.example.gersangtrade.domain.catalog.enums.Element element =
                            (type == StatType.ELEMENT_VALUE)
                                    ? org.example.gersangtrade.domain.catalog.enums.Element.ADAPTIVE
                                    : org.example.gersangtrade.domain.catalog.enums.Element.NONE;
                    result.add(new ParsedSetEffect(currentPieces, type, value, statUnit, element));
                } else {
                    log.debug("세트 효과 스탯 미매핑 (skip): [{}]", line);
                }
                continue;
            }

            log.debug("세트 효과 행 파싱 실패 (skip): [{}]", line);
        }

        return result;
    }

    // ───────────────────────────────────────────────────────────────────────
    // 파싱 결과 레코드
    // ───────────────────────────────────────────────────────────────────────

    /**
     * 세트 1개 파싱 결과.
     *
     * @param setName   세트명 (예: "공명")
     * @param level     요구 레벨
     * @param pieces    피스 목록
     * @param effects   파싱된 세트 효과 목록
     */
    public record ParsedSetRow(
            String setName,
            int level,
            List<ParsedPiece> pieces,
            List<ParsedSetEffect> effects
    ) {}

    /**
     * 세트 착용 수별 보너스 효과 1줄.
     *
     * @param requiredPieces 몇 종 착용 시 발동 (2·3·4·5·6)
     * @param statType       능력치 종류
     * @param statValue      수치
     * @param statUnit       단위 (FLAT / PERCENT)
     * @param element        속성 구분 (NONE·ADAPTIVE·특정속성)
     */
    public record ParsedSetEffect(
            int requiredPieces,
            StatType statType,
            int statValue,
            StatUnit statUnit,
            org.example.gersangtrade.domain.catalog.enums.Element element
    ) {}

    /**
     * 세트 피스 1개 파싱 결과.
     *
     * @param pieceName 피스 원래 이름 (예: "관")
     * @param itemName  DB 저장 아이템명 (예: "공명관")
     * @param slot      장비 슬롯
     * @param stats     피스별 스탯 목록
     * @param pieceCount 착용 개수 (보통 1, 반지는 2)
     */
    public record ParsedPiece(
            String pieceName,
            String itemName,
            EquipmentSlot slot,
            List<GersangjjangParser.ParsedStat> stats,
            int pieceCount
    ) {}
}
