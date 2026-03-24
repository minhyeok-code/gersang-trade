package org.example.gersangtrade.crawler.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * gerniverse.app 상세 페이지 파서.
 *
 * <p>아이템 및 용병 상세 페이지에서 JSON-LD 스키마와 HTML을 파싱한다.
 *
 * <p>⚠ HTML 선택자는 gerniverse.app(Next.js SSR)의 실제 HTML 구조 확인 후 조정이 필요하다.
 * 현재 선택자는 spec 기반 추정값이다.
 *
 * <p>이미지 URL 패턴:
 * <pre>
 * 원본: https://images.gerniverse.app/tr:cm-pad_resize,w-120,h-120,f-auto,q-80/{imageKey}.webp
 * JSON-LD image 필드 예: "item/weapon/doll/gkstkdwkdmldlsgud"
 * </pre>
 */
@Slf4j
public final class GerniverseParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String GERNIVERSE_IMAGE_BASE =
            "https://images.gerniverse.app/tr:cm-pad_resize,w-120,h-120,f-auto,q-80/";

    private GerniverseParser() {}

    // ── 아이템 파싱 ────────────────────────────────────────────────────────────

    /**
     * gerniverse 아이템 상세 페이지 파싱.
     *
     * @param doc Jsoup Document (gerniverse /item/{아이템명})
     * @return 파싱 결과. 페이지 오류 시 Optional.empty()
     */
    public static Optional<ItemData> parseItem(Document doc) {
        try {
            String name = parseH1(doc);
            if (name.isBlank()) return Optional.empty();

            JsonNode jsonLd = parseJsonLd(doc);

            String imageKey = extractImageKey(jsonLd);
            String imageUrl = imageKey != null ? GERNIVERSE_IMAGE_BASE + imageKey + ".webp" : null;

            // 카테고리 badge에서 슬롯 정보 추출 (예: "갑옷", "투구", "무기" 등)
            String categoryBadge = parseCategoryBadge(doc);
            EquipmentSlot slot = mapCategoryToSlot(categoryBadge);

            // stats 파싱 (additionalProperty 또는 별도 섹션)
            List<StatEntry> stats = parseItemStats(jsonLd, doc);

            boolean isEquipment = slot != null;

            return Optional.of(new ItemData(name, isEquipment, slot, imageKey, imageUrl, stats));

        } catch (Exception e) {
            log.warn("아이템 페이지 파싱 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ── 용병 파싱 ──────────────────────────────────────────────────────────────

    /**
     * gerniverse 용병 상세 페이지 파싱.
     *
     * @param doc Jsoup Document (gerniverse /mercenary/{용병명})
     * @return 파싱 결과. 페이지 오류 시 Optional.empty()
     */
    public static Optional<MercenaryData> parseMercenary(Document doc) {
        try {
            String name = parseH1(doc);
            if (name.isBlank()) return Optional.empty();

            JsonNode jsonLd = parseJsonLd(doc);

            // 용병 종류 (badge 태그에서 파싱 — 예: "각성명왕", "사천왕")
            String mercenaryType = parseCategoryBadge(doc);

            // 저항깎, 속성값 파싱 (JSON-LD additionalProperty)
            Integer resistPierce = null;
            Integer elementValue = null;

            if (jsonLd != null) {
                JsonNode props = jsonLd.get("additionalProperty");
                if (props != null && props.isArray()) {
                    // 검증 결과: "마법 저항", "타격 저항" 두 종류가 분리됨.
                    // 도메인 모델은 resistPierce 단일 필드 → 두 값 중 큰 값을 저장.
                    Integer magicResist = null;
                    Integer physicalResist = null;

                    for (JsonNode prop : props) {
                        String propName = prop.path("name").asText("");
                        String propValue = prop.path("value").asText("");  // "100%" 형식

                        if (propName.equals("마법 저항")) {
                            magicResist = parseIntFromPercent(propValue);
                        } else if (propName.equals("타격 저항")) {
                            physicalResist = parseIntFromPercent(propValue);
                        } else if (propName.contains("속성")) {
                            elementValue = parseIntFromPercent(propValue);
                        }
                    }

                    // 두 저항깎 중 큰 값 사용 (단일 필드 저장)
                    if (magicResist != null && physicalResist != null) {
                        resistPierce = Math.max(magicResist, physicalResist);
                    } else if (magicResist != null) {
                        resistPierce = magicResist;
                    } else {
                        resistPierce = physicalResist;
                    }
                }
            }

            // 이미지 키 추출
            String imageKey = extractImageKey(jsonLd);
            String imageUrl = imageKey != null ? GERNIVERSE_IMAGE_BASE + imageKey + ".webp" : null;

            // 고용 재료 파싱 (a[href^=/item/] 링크에서)
            List<MaterialEntry> materials = parseMaterials(doc);

            return Optional.of(new MercenaryData(
                    name, mercenaryType, resistPierce, elementValue, imageKey, imageUrl, materials));

        } catch (Exception e) {
            log.warn("용병 페이지 파싱 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ── 공통 파싱 헬퍼 ────────────────────────────────────────────────────────

    private static String parseH1(Document doc) {
        Element h1 = doc.selectFirst("h1");
        return h1 != null ? h1.text().trim() : "";
    }

    /**
     * JSON-LD 스키마 파싱.
     * {@code <script type="application/ld+json">} 태그에서 첫 번째 JSON 객체를 반환한다.
     */
    private static JsonNode parseJsonLd(Document doc) {
        Elements scripts = doc.select("script[type=application/ld+json]");
        for (Element script : scripts) {
            try {
                JsonNode node = MAPPER.readTree(script.html());
                if (node != null && !node.isNull()) return node;
            } catch (JsonProcessingException ignored) {
                // 파싱 실패 시 다음 스크립트 시도
            }
        }
        return null;
    }

    /**
     * JSON-LD image 필드에서 imageKey 추출.
     * 검증 결과: image 필드는 항상 배열 형태로 존재함.
     * 예: ["item/weapon/doll/cidndlsgud"], ["thumbnail/myeong-kings/awakening/gakGoondari"]
     * 이미지 없는 아이템의 null/빈배열 케이스 방어 처리 포함.
     */
    private static String extractImageKey(JsonNode jsonLd) {
        if (jsonLd == null) return null;
        JsonNode imageNode = jsonLd.get("image");
        if (imageNode == null || imageNode.isNull()) return null;

        // 검증 결과: 항상 배열. 빈 배열([]) 방어 처리.
        if (imageNode.isArray()) {
            if (imageNode.isEmpty()) return null;
            String key = imageNode.get(0).asText(null);
            return (key == null || key.isBlank()) ? null : key;
        }
        // 만일 문자열로 오는 경우 방어
        return imageNode.asText(null);
    }

    /**
     * 카테고리 배지 대분류 텍스트 파싱.
     * 검증 결과: 첫 번째 "div.inline-flex.items-center.gap-1\\.5" 내부 첫 번째 span = 대분류(예: "무기").
     * 재료 아이템은 카테고리 배지 없음 → 빈 문자열 반환.
     */
    private static String parseCategoryBadge(Document doc) {
        // 검증된 선택자: Tailwind inline-flex 배지 div의 첫 번째 span이 대분류
        Elements badgeDivs = doc.select("div.inline-flex.items-center.gap-1\\.5");
        if (!badgeDivs.isEmpty()) {
            Elements spans = badgeDivs.get(0).select("span");
            if (!spans.isEmpty()) {
                return spans.get(0).text().trim();
            }
        }
        return "";
    }

    /**
     * 카테고리 텍스트 → EquipmentSlot 매핑.
     * null 반환 시 재료 아이템으로 처리한다.
     */
    private static EquipmentSlot mapCategoryToSlot(String category) {
        if (category == null || category.isBlank()) return null;
        return switch (category) {
            case "무기", "활", "지팡이", "채찍", "너클" -> EquipmentSlot.WEAPON;
            case "투구", "모자", "헬멧" -> EquipmentSlot.HELMET;
            case "갑옷", "상의", "옷" -> EquipmentSlot.ARMOR;
            case "장갑" -> EquipmentSlot.GLOVES;
            case "바지", "허리띠", "벨트" -> EquipmentSlot.BELT;
            case "신발", "부츠" -> EquipmentSlot.SHOES;
            default -> null;
        };
    }

    /**
     * 아이템 능력치(ItemStat) 파싱.
     * JSON-LD additionalProperty 또는 HTML 테이블에서 파싱한다.
     * ⚠ 실제 구조 확인 후 파싱 로직 보강 필요.
     */
    private static List<StatEntry> parseItemStats(JsonNode jsonLd, Document doc) {
        List<StatEntry> stats = new ArrayList<>();
        if (jsonLd == null) return stats;

        JsonNode props = jsonLd.get("additionalProperty");
        if (props == null || !props.isArray()) return stats;

        for (JsonNode prop : props) {
            String propName = prop.path("name").asText("");
            int value = prop.path("value").asInt(0);

            // 저항깎 (resist pierce) — ⚠ 실제 JSON-LD 키명 확인 필요
            if (propName.contains("저항깎") || propName.contains("마법저항감소")) {
                stats.add(new StatEntry(StatType.RESIST_PIERCE, value));
            }
            // 속성값 (element value)
            else if (propName.contains("속성값")) {
                stats.add(new StatEntry(StatType.ELEMENT_VALUE, value));
            }
            // 속성깎 (element pierce)
            else if (propName.contains("속성깎")) {
                stats.add(new StatEntry(StatType.ELEMENT_PIERCE, value));
            }
        }

        return stats;
    }

    /**
     * 고용 재료 목록 파싱.
     * 검증 결과:
     * - 아이템명: {@code a[href^=/item/]} href 경로를 URL 디코딩해서 추출.
     *   예: href="/item/%EB%AC%BC%EC%9D%98%EC%86%8D%EC%84%B1%EC%84%9D" → "물의속성석"
     * - 수량: 링크 텍스트 내 "x15" 형식. x 제거 후 정수 변환.
     * - 재료 없는 아이템(NPC 구매만 가능)은 빈 리스트 반환.
     */
    private static List<MaterialEntry> parseMaterials(Document doc) {
        List<MaterialEntry> materials = new ArrayList<>();

        Elements materialLinks = doc.select("a[href^=/item/]");
        for (Element link : materialLinks) {
            // 아이템명: href에서 URL 디코딩 (한글 인코딩 처리)
            String href = link.attr("href");
            String itemName = URLDecoder.decode(
                    href.replaceFirst("^/item/", ""), StandardCharsets.UTF_8).trim();

            if (itemName.isBlank()) continue;

            // 수량: 링크 텍스트에서 "x{숫자}" 패턴 추출 (대소문자 무관)
            String text = link.text();
            int quantity = 1;
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("[xX×](\\d+)").matcher(text);
            if (m.find()) {
                try {
                    quantity = Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignored) {
                    quantity = 1;
                }
            }

            materials.add(new MaterialEntry(itemName, quantity));
        }

        return materials;
    }

    /** "100%" 형식의 문자열에서 정수 추출 */
    private static Integer parseIntFromPercent(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.replace("%", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── 결과 레코드 ────────────────────────────────────────────────────────────

    /** gerniverse 아이템 파싱 결과 */
    public record ItemData(
            String name,
            boolean isEquipment,
            EquipmentSlot slot,          // null if material
            String imageKey,             // S3 키로 사용
            String imageUrl,             // gerniverse CDN URL (S3 업로드 원본)
            List<StatEntry> stats
    ) {}

    /** gerniverse 용병 파싱 결과 */
    public record MercenaryData(
            String name,
            String mercenaryType,
            Integer resistPierce,        // null if no resist pierce
            Integer elementValue,        // null if no element value
            String imageKey,
            String imageUrl,             // gerniverse CDN URL
            List<MaterialEntry> materials
    ) {}

    /** 능력치 엔트리 */
    public record StatEntry(StatType statType, int value) {}

    /** 재료 엔트리 */
    public record MaterialEntry(String itemName, int quantity) {}
}
