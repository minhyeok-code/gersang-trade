package org.example.gersangtrade.crawler.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nation;
import org.example.gersangtrade.domain.catalog.enums.Nature;
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

    // ── 용병 목록 파싱 ─────────────────────────────────────────────────────────

    /**
     * gerniverse 용병 목록 페이지 파싱.
     * {@code <script type="application/ld+json">} 태그 중 {@code @type: "ItemList"}인 블록에서
     * 용병명과 상세 URL을 추출한다.
     *
     * @param doc Jsoup Document (gerniverse /mercenary)
     * @return 용병 목록 (name + url). 파싱 실패 시 빈 리스트
     */
    public static List<MercenaryListItem> parseMercenaryList(Document doc) {
        List<MercenaryListItem> result = new ArrayList<>();
        Elements scripts = doc.select("script[type=application/ld+json]");

        for (Element script : scripts) {
            try {
                JsonNode node = MAPPER.readTree(script.html());
                if (node == null || node.isNull()) continue;
                if (!"ItemList".equals(node.path("@type").asText(""))) continue;

                JsonNode items = node.get("itemListElement");
                if (items == null || !items.isArray()) continue;

                for (JsonNode item : items) {
                    String name = item.path("name").asText("").trim();
                    String url = item.path("url").asText("").trim();
                    if (!name.isBlank() && !url.isBlank()) {
                        result.add(new MercenaryListItem(name, url));
                    }
                }
                log.debug("gerniverse 용병 목록 {}개 파싱 완료", result.size());
                return result;

            } catch (JsonProcessingException ignored) {
                // 다음 스크립트 시도
            }
        }

        log.warn("gerniverse 용병 목록 ItemList JSON-LD를 찾을 수 없음");
        return result;
    }

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

            // 카테고리 배지에서 MercenaryCategory 파싱 (예: "각성명왕" → MYEONG_KING_AWAKENING)
            String categoryBadge = parseCategoryBadge(doc);
            MercenaryCategory category = mapBadgeToCategory(categoryBadge);

            // 속성(nature) 파싱 — JSON-LD additionalProperty에서 추출
            Nature nature = null;
            Integer natureValue = null;

            // 스탯 목록 (MercenaryStat으로 저장될 항목들)
            List<MercenaryStatEntry> stats = new ArrayList<>();

            if (jsonLd != null) {
                JsonNode props = jsonLd.get("additionalProperty");
                if (props != null && props.isArray()) {
                    for (JsonNode prop : props) {
                        String propName = prop.path("name").asText("");
                        String propValue = prop.path("value").asText("");

                        // 저항깎 (resist pierce) — 몬스터 저항 감소 디버프
                        if (propName.contains("저항깎") || propName.contains("저항 깎")) {
                            Integer val = parseIntFromPercent(propValue);
                            if (val != null) stats.add(new MercenaryStatEntry(StatType.RESIST_PIERCE, val));
                        }
                        // 속성값 (element value) — 속성 추가 데미지 계산 수치
                        else if (propName.contains("속성값")) {
                            Integer val = parseIntFromPercent(propValue);
                            if (val != null) {
                                natureValue = val;
                                stats.add(new MercenaryStatEntry(StatType.ELEMENT_VALUE, val));
                            }
                        }
                        // 속성깎 (element pierce)
                        else if (propName.contains("속성깎") || propName.contains("속성 깎")) {
                            Integer val = parseIntFromPercent(propValue);
                            if (val != null) stats.add(new MercenaryStatEntry(StatType.ELEMENT_PIERCE, val));
                        }
                        // 마법저항 (magic resistance) — 용병 자신의 방어 스탯
                        else if (propName.equals("마법 저항") || propName.equals("마법저항")) {
                            Integer val = parseIntFromPercent(propValue);
                            if (val != null) stats.add(new MercenaryStatEntry(StatType.MAGIC_RESISTANCE, val));
                        }
                        // 타격저항 (hitting resistance) — 용병 자신의 방어 스탯
                        else if (propName.equals("타격 저항") || propName.equals("타격저항")) {
                            Integer val = parseIntFromPercent(propValue);
                            if (val != null) stats.add(new MercenaryStatEntry(StatType.HITTING_RESISTANCE, val));
                        }
                        // 속성 종류 (fire/water/thunder/air/earth)
                        else if (propName.equals("속성") || propName.equals("nature")) {
                            nature = mapNature(propValue);
                        }
                    }
                }
            }

            // 이미지 키 추출
            String imageKey = extractImageKey(jsonLd);
            String imageUrl = imageKey != null ? GERNIVERSE_IMAGE_BASE + imageKey + ".webp" : null;

            // 고용 재료 파싱 (a[href^=/item/] 및 a[href^=/mercenary/] 링크에서)
            List<MaterialEntry> materials = parseMaterials(doc);

            return Optional.of(new MercenaryData(
                    name, null, category, null, nature, natureValue,
                    false, imageKey, imageUrl, stats, materials));

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
     * 검증 결과: 첫 번째 "div.inline-flex.items-center.gap-1\.5" 내부 첫 번째 span = 대분류.
     * 재료 아이템은 카테고리 배지 없음 → 빈 문자열 반환.
     */
    private static String parseCategoryBadge(Document doc) {
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
     * 카테고리 배지 텍스트 → MercenaryCategory 매핑.
     * 알 수 없는 배지는 null 반환.
     */
    private static MercenaryCategory mapBadgeToCategory(String badge) {
        if (badge == null || badge.isBlank()) return null;
        return switch (badge) {
            case "주인공" -> MercenaryCategory.PROTAGONIST;
            case "사천왕" -> MercenaryCategory.FOUR_HEAVENLY_KINGS;
            case "각성사천왕" -> MercenaryCategory.FOUR_HEAVENLY_KINGS_AWAKENING;
            case "명왕" -> MercenaryCategory.MYEONG_KING;
            case "각성명왕" -> MercenaryCategory.MYEONG_KING_AWAKENING;
            case "전설장수" -> MercenaryCategory.LEGENDARY_GENERAL;
            case "신수" -> MercenaryCategory.DIVINE_BEAST;
            case "흉수" -> MercenaryCategory.EVIL_BEAST;
            case "각성흉수" -> MercenaryCategory.EVIL_BEAST_AWAKENING;
            case "고용몬스터" -> MercenaryCategory.HIRED_MONSTER;
            case "전직몬스터" -> MercenaryCategory.EVOLVE_MONSTER;
            case "정령몬스터" -> MercenaryCategory.SPIRIT_MONSTER;
            case "각성장수" -> MercenaryCategory.GENERAL_AWAKENING;
            case "개조장수" -> MercenaryCategory.MODIFIED_GENERAL;
            case "2차장수" -> MercenaryCategory.SECOND_GRADE_GENERAL;
            case "1차장수" -> MercenaryCategory.FIRST_GRADE_GENERAL;
            case "용병" -> MercenaryCategory.MERCENARY;
            default -> null;
        };
    }

    /**
     * 속성 문자열 → Nature 매핑.
     * gerniverse JSON-LD의 "nature" 값(영문 소문자) 또는 한국어 표기 모두 처리한다.
     */
    private static Nature mapNature(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value.toLowerCase().trim()) {
            case "fire", "화" -> Nature.FIRE;
            case "water", "수" -> Nature.WATER;
            case "thunder", "뇌" -> Nature.THUNDER;
            case "air", "풍" -> Nature.AIR;
            case "earth", "토" -> Nature.EARTH;
            case "-", "none" -> Nature.NONE;
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
     * a[href^=/item/] → 아이템 재료 (materialItemKey 설정)
     * a[href^=/mercenary/] → 용병 재료 (materialMercenaryName 설정)
     *
     * <p>검증 결과:
     * - 아이템명: href를 URL 디코딩 후 경로 prefix 제거.
     *   예: href="/item/%EB%AC%BC%EC%9D%98%EC%86%8D%EC%84%B1%EC%84%9D" → "물의속성석"
     * - 수량: 링크 텍스트 내 "x15" 형식. x 제거 후 정수 변환.
     */
    private static List<MaterialEntry> parseMaterials(Document doc) {
        List<MaterialEntry> materials = new ArrayList<>();

        // 아이템 재료 파싱
        Elements itemLinks = doc.select("a[href^=/item/]");
        for (Element link : itemLinks) {
            String href = link.attr("href");
            String itemKey = URLDecoder.decode(
                    href.replaceFirst("^/item/", ""), StandardCharsets.UTF_8).trim();
            if (itemKey.isBlank()) continue;

            int quantity = extractQuantity(link.text());
            materials.add(new MaterialEntry(itemKey, null, quantity, null, null));
        }

        // 용병 재료 파싱
        Elements mercenaryLinks = doc.select("a[href^=/mercenary/]");
        for (Element link : mercenaryLinks) {
            String href = link.attr("href");
            String mercenaryName = URLDecoder.decode(
                    href.replaceFirst("^/mercenary/", ""), StandardCharsets.UTF_8).trim();
            if (mercenaryName.isBlank()) continue;

            int quantity = extractQuantity(link.text());
            materials.add(new MaterialEntry(null, mercenaryName, quantity, null, null));
        }

        return materials;
    }

    /** 링크 텍스트에서 "x{숫자}" 패턴으로 수량 추출. 없으면 1 반환. */
    private static int extractQuantity(String text) {
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("[xX×](\\d+)").matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return 1;
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

    /** gerniverse 용병 목록 항목 */
    public record MercenaryListItem(String name, String url) {}

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
            String key,                          // gerniverse 내부 키 (파싱 성공 시 설정, 아니면 null)
            MercenaryCategory category,
            Nation nation,                       // 파싱 성공 시 설정, 아니면 null
            Nature nature,
            Integer natureValue,                 // 속성값 (null이면 무속성)
            boolean comingSoon,
            String imageKey,
            String imageUrl,
            List<MercenaryStatEntry> stats,      // RESIST_PIERCE, ELEMENT_VALUE 등
            List<MaterialEntry> materials
    ) {}

    /** 아이템 능력치 엔트리 */
    public record StatEntry(StatType statType, int value) {}

    /** 용병 스탯 엔트리 */
    public record MercenaryStatEntry(StatType statKey, int statValue) {}

    /**
     * 재료 엔트리.
     * materialItemKey와 materialMercenaryName 중 하나만 설정된다.
     */
    public record MaterialEntry(
            String materialItemKey,         // 아이템 재료인 경우. null if 용병 재료
            String materialMercenaryName,   // 용병 재료인 경우. null if 아이템 재료
            int quantity,
            Integer requiredLevel,
            Integer requiredCredit
    ) {}
}
