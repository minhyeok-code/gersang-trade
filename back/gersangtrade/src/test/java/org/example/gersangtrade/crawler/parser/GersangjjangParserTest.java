package org.example.gersangtrade.crawler.parser;

import org.example.gersangtrade.crawler.parser.GersangjjangParser.CategoryInfo;
import org.example.gersangtrade.crawler.parser.GersangjjangParser.ItemRow;
import org.example.gersangtrade.crawler.parser.GersangjjangParser.ParsedStat;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GersangjjangParser 단위 테스트.
 * HTML 픽스처로 카테고리 링크 파싱, 아이템 행 파싱, 스탯·스킬 파싱을 검증한다.
 */
@DisplayName("GersangjjangParser")
class GersangjjangParserTest {

    // ── parseCategoryLinks ──────────────────────────────────────────────────

    @Nested
    @DisplayName("parseCategoryLinks")
    class ParseCategoryLinks {

        @Test
        @DisplayName("장비 카테고리는 slot·kind가 결정됨")
        void 장비_카테고리_슬롯_결정() {
            String html = """
                    <html><body>
                      <div class="classification-container">
                        <div class="data">
                          <a href="dao.asp">도검</a>
                          <a href="jia.asp">갑옷</a>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<CategoryInfo> result = GersangjjangParser.parseCategoryLinks(doc);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(CategoryInfo::isEquipment);
            CategoryInfo sword = result.stream().filter(c -> c.text().equals("도검")).findFirst().orElseThrow();
            assertThat(sword.slot()).isEqualTo(EquipmentSlot.WEAPON);
            assertThat(sword.kind()).isEqualTo(EquipmentKind.NORMAL);
        }

        @Test
        @DisplayName("EXCLUDED_HREFS에 포함된 링크는 제외")
        void 제외_href_필터링() {
            String html = """
                    <html><body>
                      <div class="classification-container">
                        <div class="data">
                          <a href="dao.asp">도검</a>
                          <a href="yiwu.asp">유물</a>
                          <a href="baoshi.asp">보석</a>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<CategoryInfo> result = GersangjjangParser.parseCategoryLinks(doc);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).text()).isEqualTo("도검");
        }

        @Test
        @DisplayName("전용장비 페이지(wang_/ming_/4j_/z_kr/zhu_bian)는 EXCLUDED_HREFS로 제외")
        void 전용장비_href_필터링() {
            String html = """
                    <html><body>
                      <div class="classification-container">
                        <div class="data">
                          <a href="dao.asp">도검</a>
                          <a href="wang_zz.asp">증장천왕(각성)</a>
                          <a href="ming_gun.asp">군다리명왕(雷)</a>
                          <a href="4j_choi.asp">최무선(火)</a>
                          <a href="z_kr1.asp">조선男</a>
                          <a href="zhu_bian.asp">변신무기</a>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<CategoryInfo> result = GersangjjangParser.parseCategoryLinks(doc);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).text()).isEqualTo("도검");
        }

        @Test
        @DisplayName("장인·기타무기(zhu_jiangren/zhu_qita)는 EXCLUDED_HREFS로 영구 제외")
        void 장인_기타무기_영구_제외() {
            String html = """
                    <html><body>
                      <div class="classification-container">
                        <div class="data">
                          <a href="dao.asp">도검</a>
                          <a href="zhu_jiangren.asp">장인</a>
                          <a href="zhu_qita.asp">기타무기</a>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<CategoryInfo> result = GersangjjangParser.parseCategoryLinks(doc);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).text()).isEqualTo("도검");
        }

        @Test
        @DisplayName("[으로 시작하는 특수 링크는 제외")
        void 대괄호_특수링크_제외() {
            String html = """
                    <html><body>
                      <div class="classification-container">
                        <div class="data">
                          <a href="dao.asp">도검</a>
                          <a href="set.asp">[세트효과]</a>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<CategoryInfo> result = GersangjjangParser.parseCategoryLinks(doc);

            assertThat(result).hasSize(1);
        }
    }

    // ── parseItemRows (Type A: .data-row) ───────────────────────────────────

    @Nested
    @DisplayName("parseItemRows — Type A (.data-row)")
    class ParseItemRowsTypeA {

        @Test
        @DisplayName("스탯과 스킬을 가진 아이템 행 파싱")
        void 스탯과_스킬_파싱() {
            String html = """
                    <html><body>
                      <div class="data-row">
                        <div class="w-name"><strong>전설의 검</strong></div>
                        <div class="w-stat">힘+300<br>타저30%<br>광기</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ItemRow> rows = GersangjjangParser.parseItemRows(doc);

            assertThat(rows).hasSize(1);
            ItemRow row = rows.get(0);
            assertThat(row.name()).isEqualTo("전설의 검");

            // 힘+300 → FLAT
            assertThat(row.stats()).anyMatch(s ->
                    s.statType() == StatType.STRENGTH && s.value() == 300 && s.statUnit() == StatUnit.FLAT);
            // 타저30% → PERCENT
            assertThat(row.stats()).anyMatch(s ->
                    s.statType() == StatType.HITTING_RESISTANCE && s.value() == 30 && s.statUnit() == StatUnit.PERCENT);
            // 광기 → skill
            assertThat(row.skills()).contains("광기");
        }

        @Test
        @DisplayName("공격력 범위 '공 220-250' → MIN_POWER + MAX_POWER FLAT")
        void 공격력_범위_파싱() {
            String html = """
                    <html><body>
                      <div class="data-row">
                        <div class="w-name"><strong>공명극</strong></div>
                        <div class="w-stat">공 220-250</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ItemRow> rows = GersangjjangParser.parseItemRows(doc);

            assertThat(rows).hasSize(1);
            List<ParsedStat> stats = rows.get(0).stats();
            assertThat(stats).anyMatch(s -> s.statType() == StatType.MIN_POWER && s.value() == 220 && s.statUnit() == StatUnit.FLAT);
            assertThat(stats).anyMatch(s -> s.statType() == StatType.MAX_POWER && s.value() == 250 && s.statUnit() == StatUnit.FLAT);
        }

        @Test
        @DisplayName("화속성 단축 '화속성+5' → ELEMENT_VALUE + FIRE + FLAT")
        void 화속성_단축_파싱() {
            String html = """
                    <html><body>
                      <div class="data-row">
                        <div class="w-name"><strong>지국천왕부</strong></div>
                        <div class="w-stat">화속성+5</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ItemRow> rows = GersangjjangParser.parseItemRows(doc);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).stats()).anyMatch(s ->
                    s.statType() == StatType.ELEMENT_VALUE
                            && s.element() == Element.FIRE
                            && s.value() == 5
                            && s.statUnit() == StatUnit.FLAT);
            assertThat(rows.get(0).skills()).isEmpty();
        }

        @Test
        @DisplayName("속성별 스탯 '불속성값+5' → ELEMENT_VALUE + FIRE + FLAT")
        void 속성별_스탯_파싱() {
            String html = """
                    <html><body>
                      <div class="data-row">
                        <div class="w-name"><strong>화염의 갑옷</strong></div>
                        <div class="w-stat">불속성값+5</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ItemRow> rows = GersangjjangParser.parseItemRows(doc);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).stats()).anyMatch(s ->
                    s.statType() == StatType.ELEMENT_VALUE
                            && s.element() == Element.FIRE
                            && s.value() == 5
                            && s.statUnit() == StatUnit.FLAT);
        }

        @Test
        @DisplayName("각성명왕무기 '동속아군 속성+20%' → ELEMENT_VALUE PERCENT ADAPTIVE")
        void 동속아군_속성_퍼센트_파싱() {
            String html = """
                    <html><body>
                      <div class="data-row">
                        <div class="w-name"><strong>각성명왕장</strong></div>
                        <div class="w-stat">공 300-300<br>동속아군 속성+20%<br>천결</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ItemRow> rows = GersangjjangParser.parseItemRows(doc);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).stats()).anyMatch(s ->
                    s.statType() == StatType.ELEMENT_VALUE
                            && s.element() == Element.ADAPTIVE
                            && s.value() == 20
                            && s.statUnit() == StatUnit.PERCENT);
            assertThat(rows.get(0).skills()).contains("천결");
        }

        @Test
        @DisplayName("속성값 단독 '속성값+10' → ELEMENT_VALUE + ADAPTIVE")
        void 범용_속성값_파싱() {
            String html = """
                    <html><body>
                      <div class="data-row">
                        <div class="w-name"><strong>장신구</strong></div>
                        <div class="w-stat">속성값+10</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ItemRow> rows = GersangjjangParser.parseItemRows(doc);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).stats()).anyMatch(s ->
                    s.statType() == StatType.ELEMENT_VALUE && s.element() == Element.ADAPTIVE);
        }

        @Test
        @DisplayName("스탯 셀 없는 행은 빈 stats·skills")
        void 스탯없는_행() {
            String html = """
                    <html><body>
                      <div class="data-row">
                        <div class="w-name"><strong>빈 아이템</strong></div>
                        <div class="w-stat">-</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ItemRow> rows = GersangjjangParser.parseItemRows(doc);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).stats()).isEmpty();
            assertThat(rows.get(0).skills()).isEmpty();
        }

        @Test
        @DisplayName("아이템 없는 페이지는 빈 리스트 반환")
        void 빈_페이지() {
            Document doc = Jsoup.parse("<html><body></body></html>");
            List<ItemRow> rows = GersangjjangParser.parseItemRows(doc);
            assertThat(rows).isEmpty();
        }

        @Test
        @DisplayName("모든 능력치가 두 줄로 분리된 경우 병합 파싱")
        void 두줄_분리_스탯_병합() {
            // "모든 능력치" + "+600" 이 두 TextNode로 분리된 경우
            String html = """
                    <html><body>
                      <div class="data-row">
                        <div class="w-name"><strong>천하제일</strong></div>
                        <div class="w-stat">모든 능력치<br>+600</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ItemRow> rows = GersangjjangParser.parseItemRows(doc);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).stats()).anyMatch(s ->
                    s.statType() == StatType.ALL_STAT && s.value() == 600 && s.statUnit() == StatUnit.FLAT);
        }
    }

    // ── parseItemRows (Type B: .item-row .sub-row) ──────────────────────────

    @Nested
    @DisplayName("parseItemRows — Type B (.item-row .sub-row)")
    class ParseItemRowsTypeB {

        @Test
        @DisplayName("Type B 구조에서 첫 텍스트 노드를 이름으로 파싱")
        void TypeB_아이템명_파싱() {
            String html = """
                    <html><body>
                      <div class="item-row">
                        <div class="sub-row">
                          <div class="w-name">고급천왕검<br>(215lv)</div>
                          <div class="w-stat">힘+100</div>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ItemRow> rows = GersangjjangParser.parseItemRows(doc);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).name()).isEqualTo("고급천왕검");
            assertThat(rows.get(0).stats()).anyMatch(s ->
                    s.statType() == StatType.STRENGTH && s.value() == 100);
        }

        @Test
        @DisplayName("Type B — br 다음 줄 (+5) 강화 접미사 병합")
        void TypeB_강화_접미사_병합() {
            String html = """
                    <html><body>
                      <div class="item-row">
                        <div class="sub-row">
                          <div class="w-name">노부츠나의창<br>(+5)</div>
                          <div class="w-stat">공 300-350</div>
                        </div>
                        <div class="sub-row">
                          <div class="w-name">노부츠나의창<br>(+10)</div>
                          <div class="w-stat">공 350-400</div>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ItemRow> rows = GersangjjangParser.parseItemRows(doc);

            assertThat(rows).hasSize(2);
            assertThat(rows).extracting(ItemRow::name)
                    .containsExactly("노부츠나의창(+5)", "노부츠나의창(+10)");
        }

        @Test
        @DisplayName("Type B — br+줄바꿈 (+5) 표기 (홍길동 반지)")
        void TypeB_줄바꿈_강화_표기() {
            String html = """
                    <html><body>
                      <div class="item-row">
                        <div class="sub-row">
                          <div class="cell w-name">홍길동의 반지<br>
                            (+5)</div>
                          <div class="cell w-stat">힘 45</div>
                        </div>
                        <div class="sub-row rank-10">
                          <div class="cell w-name">홍길동의 반지<br>
                            (+10)</div>
                          <div class="cell w-stat">힘+60</div>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ItemRow> rows = GersangjjangParser.parseItemRows(doc);

            assertThat(rows).hasSize(2);
            assertThat(rows).extracting(ItemRow::name)
                    .containsExactly("홍길동의 반지(+5)", "홍길동의 반지(+10)");
        }

        @Test
        @DisplayName("Type B — 인라인 공백 (+5) 표기 정리")
        void TypeB_인라인_강화_표기() {
            String html = """
                    <html><body>
                      <div class="item-row">
                        <div class="sub-row">
                          <div class="w-name">여포의 방천화극 (+5)</div>
                          <div class="w-stat">공 300-350</div>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ItemRow> rows = GersangjjangParser.parseItemRows(doc);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).name()).isEqualTo("여포의 방천화극(+5)");
        }
    }
}
