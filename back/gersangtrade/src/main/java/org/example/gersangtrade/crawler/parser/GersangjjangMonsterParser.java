package org.example.gersangtrade.crawler.parser;

import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 거상짱 몬스터 페이지 파서.
 *
 * <p>몬스터 HTML은 구형/신형에 따라 3가지 패턴으로 구분된다.
 * <ul>
 *   <li>패턴 A (구형): monster-sprite에 텍스트 없음, 저항력 라인에 타저/마저/속성값 혼재</li>
 *   <li>패턴 B (신형 보스): monster-sprite에 hp/타저/마저 텍스트, info-section에 속성값 태그</li>
 *   <li>패턴 C (중간형): monster-sprite에 텍스트 없음, info-section에 속성값 태그 + 저항력 분리</li>
 * </ul>
 */
@Slf4j
public final class GersangjjangMonsterParser {

    /** 인덱스 페이지 URL */
    public static final String INDEX_URL = "https://www.gersangjjang.com/monster/index.asp";
    /** 거상짱 베이스 URL */
    private static final String BASE_URL = "https://www.gersangjjang.com";

    private static final Pattern PATTERN_RESIST = Pattern.compile("타저\\s*(\\d+)%?");
    private static final Pattern PATTERN_MAGIC_RESIST = Pattern.compile("마저\\s*(\\d+)%?");
    private static final Pattern PATTERN_HP = Pattern.compile("체력\\s*(\\d+)(만)?");
    private static final Pattern PATTERN_ELEMENT_VALUE = Pattern.compile("속성값[:\\s]*(\\d+)");
    private static final Pattern PATTERN_OLD_ELEMENT = Pattern.compile("([화수뇌풍토])속성\\s*(\\d+)");
    // monster-name 괄호에서 속성 한자 추출: (水), (雷), (火), (風), (土), (明)
    private static final Pattern PATTERN_NAME_ELEMENT = Pattern.compile("[（(]([火水雷風土明])[）)]");

    private GersangjjangMonsterParser() {}

    // ── 인덱스 파싱 ──────────────────────────────────────────────────────────

    /**
     * 인덱스 페이지에서 몬스터 상세 페이지 URL Set을 수집한다.
     * /monster/ 로 시작하는 링크만 수집하고, 인덱스 자기 자신은 제외한다.
     */
    public static List<String> parseIndexUrls(Document doc) {
        List<String> urls = new ArrayList<>();
        doc.select("a[href]").forEach(a -> {
            String href = a.attr("href");
            if (href.startsWith("/monster/") && !href.equals("/monster/index.asp")) {
                String fullUrl = BASE_URL + href;
                if (!urls.contains(fullUrl)) {
                    urls.add(fullUrl);
                }
            }
        });
        return urls;
    }

    // ── 몬스터 페이지 파싱 ──────────────────────────────────────────────────

    /**
     * 몬스터 상세 페이지에서 MonsterRow 목록을 파싱한다.
     *
     * @param doc     파싱할 Jsoup Document
     * @param pageUrl 출처 URL (UNIQUE 키 구성에 사용)
     * @return 파싱된 몬스터 행 목록
     */
    public static List<MonsterRow> parseMonsterRows(Document doc, String pageUrl) {
        List<MonsterRow> results = new ArrayList<>();
        Elements rows = doc.select(".monster-row");
        if (rows.isEmpty()) {
            log.warn("monster-row 없음: {}", pageUrl);
            return results;
        }

        for (org.jsoup.nodes.Element row : rows) {
            try {
                MonsterRow parsed = parseRow(row, pageUrl);
                if (parsed != null) results.add(parsed);
            } catch (Exception e) {
                String name = row.select(".monster-name").text();
                log.warn("몬스터 파싱 실패 [name={}, url={}]: {}", name, pageUrl, e.getMessage());
            }
        }
        return results;
    }

    private static MonsterRow parseRow(org.jsoup.nodes.Element row, String pageUrl) {
        String rawName = row.select(".monster-name").text().trim();
        if (rawName.isEmpty()) return null;

        String spriteText = row.select(".monster-sprite").text().trim();
        String infoText = row.select(".info-section").text().trim();

        // 패턴 감지
        boolean hasHp = spriteText.contains("체력");
        boolean resistanceInSprite = spriteText.contains("타저");
        boolean hasSeparateElementTag = row.select(".info-section strong").stream()
                .anyMatch(el -> el.text().contains("속성값"));

        PatternType pattern;
        if (hasHp && resistanceInSprite) {
            pattern = PatternType.B;
        } else if (hasSeparateElementTag) {
            pattern = PatternType.C;
        } else {
            pattern = PatternType.A;
        }

        Long hp = null;
        Integer hittingResistance = null;
        Integer magicResistance = null;
        Integer elementValue = null;
        Element element = null;

        switch (pattern) {
            case A -> {
                // 저항력 라인에서 타저/마저/속성값 파싱
                String resistLine = extractResistLine(row);
                hittingResistance = extractInt(PATTERN_RESIST, resistLine);
                magicResistance = extractInt(PATTERN_MAGIC_RESIST, resistLine);
                // 속성값 + element: "수속성 20" 형태
                Matcher oldElem = PATTERN_OLD_ELEMENT.matcher(resistLine);
                if (oldElem.find()) {
                    element = parseKoreanElement(oldElem.group(1));
                    elementValue = Integer.parseInt(oldElem.group(2));
                }
                // 이름 괄호에서도 element 파싱 시도 (더 신뢰성 높음)
                Element nameElement = parseNameElement(rawName);
                if (nameElement != null) element = nameElement;
            }
            case B -> {
                hp = parseHp(spriteText);
                hittingResistance = extractInt(PATTERN_RESIST, spriteText);
                magicResistance = extractInt(PATTERN_MAGIC_RESIST, spriteText);
                elementValue = extractElementValueFromTag(row);
                element = parseNameElement(rawName);
            }
            case C -> {
                String resistLine = extractResistLine(row);
                hittingResistance = extractInt(PATTERN_RESIST, resistLine);
                magicResistance = extractInt(PATTERN_MAGIC_RESIST, resistLine);
                elementValue = extractElementValueFromTag(row);
                element = parseNameElement(rawName);
            }
        }

        // 경고 로그 조건
        if (elementValue != null && element == null) {
            log.warn("elementValue 존재하지만 element 파싱 실패 [name={}, url={}]", rawName, pageUrl);
        }

        return new MonsterRow(rawName, pageUrl, hp, hittingResistance, magicResistance,
                elementValue, element);
    }

    // ── 내부 파싱 헬퍼 ──────────────────────────────────────────────────────

    private static String extractResistLine(org.jsoup.nodes.Element row) {
        // "저항력" 또는 "저항" 라벨이 있는 <strong> 다음 텍스트 추출
        for (org.jsoup.nodes.Element strong : row.select(".info-section strong")) {
            String label = strong.text();
            if (label.contains("저항력") || label.equals("저항")) {
                // strong 다음 형제 텍스트 노드들을 포함한 부모 텍스트
                String parentText = strong.parent() != null ? strong.parent().text() : "";
                return parentText;
            }
        }
        return row.select(".info-section").text();
    }

    private static Integer extractElementValueFromTag(org.jsoup.nodes.Element row) {
        for (org.jsoup.nodes.Element strong : row.select(".info-section strong")) {
            if (strong.text().contains("속성값")) {
                // <strong>속성값</strong>: 170 or <strong>속성값: </strong>170
                String afterText = strong.nextSibling() != null
                        ? strong.nextSibling().toString().trim()
                        : "";
                // 콜론과 공백 제거
                afterText = afterText.replaceAll("^[:\\s]+", "").trim();
                if (afterText.isEmpty()) {
                    // 같은 strong 내에 수치가 포함된 경우 (예: "속성값: 170")
                    Matcher m = PATTERN_ELEMENT_VALUE.matcher(strong.parent().text());
                    if (m.find()) return Integer.parseInt(m.group(1));
                } else {
                    try {
                        return Integer.parseInt(afterText.replaceAll("[^0-9].*", ""));
                    } catch (NumberFormatException ignored) {
                        Matcher m = PATTERN_ELEMENT_VALUE.matcher(strong.parent().text());
                        if (m.find()) return Integer.parseInt(m.group(1));
                    }
                }
            }
        }
        return null;
    }

    private static Long parseHp(String spriteText) {
        Matcher m = PATTERN_HP.matcher(spriteText);
        if (!m.find()) return null;
        long val = Long.parseLong(m.group(1));
        if ("만".equals(m.group(2))) val *= 10_000L;
        return val;
    }

    private static Integer extractInt(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private static Element parseNameElement(String name) {
        Matcher m = PATTERN_NAME_ELEMENT.matcher(name);
        if (!m.find()) return null;
        return switch (m.group(1)) {
            case "火" -> Element.FIRE;
            case "水" -> Element.WATER;
            case "雷" -> Element.THUNDER;
            case "風" -> Element.WIND;
            case "土" -> Element.EARTH;
            case "明" -> Element.NONE;
            default -> null;
        };
    }

    /** 패턴 A 전용: 한국어 속성명("화","수" 등) → Element */
    private static Element parseKoreanElement(String kor) {
        return switch (kor) {
            case "화" -> Element.FIRE;
            case "수" -> Element.WATER;
            case "뇌" -> Element.THUNDER;
            case "풍" -> Element.WIND;
            case "토" -> Element.EARTH;
            default -> null;
        };
    }

    private enum PatternType { A, B, C }

    // ── 결과 DTO ────────────────────────────────────────────────────────────

    public record MonsterRow(
            String name,
            String pageUrl,
            Long hp,
            Integer hittingResistance,
            Integer magicResistance,
            Integer elementValue,
            org.example.gersangtrade.domain.catalog.enums.Element element
    ) {}
}
