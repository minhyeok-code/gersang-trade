package org.example.gersangtrade.crawler.parser;

import org.example.gersangtrade.crawler.config.ExclusiveEquipPolicy;
import org.example.gersangtrade.crawler.config.ExclusiveEquipmentPageConfig;
import org.example.gersangtrade.crawler.parser.GersangjjangExclusiveEquipmentParser.ParsedExclusiveRow;
import org.example.gersangtrade.domain.catalog.enums.Enhancement;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GersangjjangExclusiveEquipmentParser")
class GersangjjangExclusiveEquipmentParserTest {

    @Nested
    @DisplayName("resolveSectionMercenary")
    class ResolveSectionMercenary {

        @Test
        @DisplayName("각성 섹션 → 각성 용병명")
        void 각성_섹션() {
            ExclusiveEquipmentPageConfig config =
                    ExclusiveEquipmentPageConfig.dualSection("wang_zz.asp", "증장천왕", "각성 증장천왕");

            Optional<String> result = GersangjjangExclusiveEquipmentParser
                    .resolveSectionMercenary("각성 증장천왕 전용템", config);

            assertThat(result).contains("각성 증장천왕");
        }

        @Test
        @DisplayName("일반 섹션 → 기본 용병명")
        void 일반_섹션() {
            ExclusiveEquipmentPageConfig config =
                    ExclusiveEquipmentPageConfig.dualSection("wang_zz.asp", "증장천왕", "각성 증장천왕");

            Optional<String> result = GersangjjangExclusiveEquipmentParser
                    .resolveSectionMercenary("증장천왕 전용템", config);

            assertThat(result).contains("증장천왕");
        }
    }

    @Nested
    @DisplayName("resolveSlotAndKind")
    class ResolveSlotAndKind {

        @Test
        @DisplayName("무신 → DIVINE + APPEARANCE")
        void 무신() {
            var result = GersangjjangExclusiveEquipmentParser.resolveSlotAndKind(
                    "무신의 증장천왕", ExclusiveEquipPolicy.HEAVENLY_KING_AND_MYEONGWANG);
            assertThat(result).isPresent();
            assertThat(result.get().slot()).isEqualTo(EquipmentSlot.DIVINE);
            assertThat(result.get().kind()).isEqualTo(EquipmentKind.APPEARANCE);
        }

        @Test
        @DisplayName("인형 → WEAPON")
        void 인형() {
            var result = GersangjjangExclusiveEquipmentParser.resolveSlotAndKind(
                    "성훈의 인형", ExclusiveEquipPolicy.PROTAGONIST_DOLL);
            assertThat(result).isPresent();
            assertThat(result.get().slot()).isEqualTo(EquipmentSlot.WEAPON);
        }

        @ParameterizedTest
        @CsvSource({
                "증장천왕의 투구, HELMET",
                "천왕주,           WEAPON",
                "증장천왕부,       TALISMAN"
        })
        @DisplayName("suffix 슬롯 감지")
        void suffix_슬롯(String name, EquipmentSlot expected) {
            var result = GersangjjangExclusiveEquipmentParser.resolveSlotAndKind(
                    name, ExclusiveEquipPolicy.HEAVENLY_KING_AND_MYEONGWANG);
            assertThat(result).isPresent();
            assertThat(result.get().slot()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("parseEnhancement")
    class ParseEnhancement {

        @Test
        @DisplayName("(+5) → FIVE")
        void plus5() {
            assertThat(GersangjjangExclusiveEquipmentParser.parseEnhancement("최무선의화포(+5)"))
                    .contains(Enhancement.FIVE);
        }

        @Test
        @DisplayName("(+10) → TEN")
        void plus10() {
            assertThat(GersangjjangExclusiveEquipmentParser.parseEnhancement("최무선의화포(+10)"))
                    .contains(Enhancement.TEN);
        }

        @Test
        @DisplayName("공백 포함 (+5) → FIVE")
        void 공백_포함_plus5() {
            assertThat(GersangjjangExclusiveEquipmentParser.parseEnhancement("여포의 방천화극 (+5)"))
                    .contains(Enhancement.FIVE);
        }
    }

    @Nested
    @DisplayName("parsePage")
    class ParsePage {

        @Test
        @DisplayName("사천왕 — 6피스 제외, 무기·수호부·무신만 restriction 대상")
        void 사천왕_슬롯_필터() {
            String html = """
                    <html><body>
                      <div class="main-title">증장천왕 전용템</div>
                      <div class="data-row">
                        <div class="w-name"><strong>천왕주</strong></div>
                        <div class="w-stat">공150-150</div>
                      </div>
                      <div class="data-row">
                        <div class="w-name"><strong>증장천왕의 투구</strong></div>
                        <div class="w-stat">방어300</div>
                      </div>
                      <div class="data-row">
                        <div class="w-name"><strong>무신의 증장천왕</strong></div>
                        <div class="w-stat">민+300</div>
                      </div>
                      <div class="data-row">
                        <div class="w-name"><strong>증장천왕부</strong></div>
                        <div class="w-stat">민+150</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            ExclusiveEquipmentPageConfig config =
                    ExclusiveEquipmentPageConfig.dualSection("wang_zz.asp", "증장천왕", "각성 증장천왕");

            List<ParsedExclusiveRow> rows = GersangjjangExclusiveEquipmentParser.parsePage(doc, config);

            assertThat(rows).hasSize(3);
            assertThat(rows).extracting(r -> r.itemRow().name())
                    .containsExactly("천왕주", "무신의 증장천왕", "증장천왕부");
            assertThat(rows).allMatch(r -> r.restrictionMercenaryNames().equals(List.of("증장천왕")));
        }

        @Test
        @DisplayName("각성 섹션 — 각성 용병명 매핑")
        void 각성_섹션_용병() {
            String html = """
                    <html><body>
                      <div class="main-title">각성 증장천왕 전용템</div>
                      <div class="data-row">
                        <div class="w-name"><strong>각성천왕궁</strong></div>
                        <div class="w-stat">공250-250</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            ExclusiveEquipmentPageConfig config =
                    ExclusiveEquipmentPageConfig.dualSection("wang_zz.asp", "증장천왕", "각성 증장천왕");

            List<ParsedExclusiveRow> rows = GersangjjangExclusiveEquipmentParser.parsePage(doc, config);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).restrictionMercenaryNames()).containsExactly("각성 증장천왕");
        }

        @Test
        @DisplayName("인형 — mercenaryName null, 전 행 파싱")
        void 인형_PROTAGONIST() {
            String html = """
                    <html><body>
                      <div class="main-title">변신무기 - 본캐 전용</div>
                      <div class="data-row">
                        <div class="w-name"><strong>성훈의 인형</strong></div>
                        <div class="w-stat">공120-140</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            ExclusiveEquipmentPageConfig config = ExclusiveEquipmentPageConfig.protagonistDoll("zhu_bian.asp");

            List<ParsedExclusiveRow> rows = GersangjjangExclusiveEquipmentParser.parsePage(doc, config);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).restrictionMercenaryNames()).isEmpty();
            assertThat(rows.get(0).slot()).isEqualTo(EquipmentSlot.WEAPON);
        }

        @Test
        @DisplayName("전설장수 — 노부츠나 7종×2강 14행 파싱")
        void 전설장수_노부츠나_14행() {
            String html = """
                    <html><body>
                      <div class="main-title">일본 전설장수 - 군신 노부츠나 전용템</div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">노부츠나의창<br>(+5)</div><div class="cell w-stat">공300</div></div>
                        <div class="sub-row"><div class="cell w-name">노부츠나의창<br>(+10)</div><div class="cell w-stat">공350</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">노부츠나의투구<br>(+5)</div><div class="cell w-stat">방200</div></div>
                        <div class="sub-row"><div class="cell w-name">노부츠나의투구<br>(+10)</div><div class="cell w-stat">방250</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">노부츠나의갑주<br>(+5)</div><div class="cell w-stat">방300</div></div>
                        <div class="sub-row"><div class="cell w-name">노부츠나의갑주<br>(+10)</div><div class="cell w-stat">방350</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">노부츠나의팔보호구<br>(+5)</div><div class="cell w-stat">힘100</div></div>
                        <div class="sub-row"><div class="cell w-name">노부츠나의팔보호구<br>(+10)</div><div class="cell w-stat">힘150</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">노부츠나의요대<br>(+5)</div><div class="cell w-stat">힘100</div></div>
                        <div class="sub-row"><div class="cell w-name">노부츠나의요대<br>(+10)</div><div class="cell w-stat">힘150</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">노부츠나의전투화<br>(+5)</div><div class="cell w-stat">힘100</div></div>
                        <div class="sub-row"><div class="cell w-name">노부츠나의전투화<br>(+10)</div><div class="cell w-stat">힘150</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">노부츠나의반지<br>(+5)</div><div class="cell w-stat">힘100</div></div>
                        <div class="sub-row"><div class="cell w-name">노부츠나의반지<br>(+10)</div><div class="cell w-stat">힘150</div></div>
                      </div></div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            ExclusiveEquipmentPageConfig config =
                    ExclusiveEquipmentPageConfig.single("4j_nobu.asp",
                            ExclusiveEquipPolicy.LEGENDARY_GENERAL, "노부츠나");

            List<ParsedExclusiveRow> rows = GersangjjangExclusiveEquipmentParser.parsePage(doc, config);

            assertThat(rows).hasSize(14);
            assertThat(rows).extracting(r -> r.itemRow().name())
                    .contains("노부츠나의창(+5)", "노부츠나의창(+10)", "노부츠나의갑주(+10)");
            assertThat(rows).filteredOn(r -> r.itemRow().name().equals("노부츠나의갑주(+5)"))
                    .extracting(ParsedExclusiveRow::slot)
                    .containsExactly(EquipmentSlot.ARMOR);
            assertThat(rows).allMatch(r -> r.restrictionMercenaryNames().equals(List.of("노부츠나")));
        }

        @Test
        @DisplayName("전설장수 Type B(item-row) — 6피스+무기 전부 파싱")
        void 전설장수_item_row_파싱() {
            String html = """
                    <html><body>
                      <div class="main-title">조선 전설장수 - 도령 최무선 전용템</div>
                      <div class="item-row">
                        <div class="info-container">
                          <div class="sub-row">
                            <div class="cell w-name">최무선의화포(+5)<br>210 lv</div>
                            <div class="cell w-stat">공 300-350힘+250</div>
                          </div>
                          <div class="sub-row">
                            <div class="cell w-name">최무선의화포(+10)<br>210 lv</div>
                            <div class="cell w-stat">공 350-400힘+300</div>
                          </div>
                        </div>
                      </div>
                      <div class="item-row">
                        <div class="info-container">
                          <div class="sub-row">
                            <div class="cell w-name">최무선의투구(+5)<br>210lv</div>
                            <div class="cell w-stat">방어 200힘+300</div>
                          </div>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            ExclusiveEquipmentPageConfig config =
                    ExclusiveEquipmentPageConfig.single("4j_choi.asp",
                            ExclusiveEquipPolicy.LEGENDARY_GENERAL, "최무선");

            List<ParsedExclusiveRow> rows = GersangjjangExclusiveEquipmentParser.parsePage(doc, config);

            assertThat(rows).hasSize(3);
            assertThat(rows).extracting(r -> r.itemRow().name())
                    .containsExactly("최무선의화포(+5)", "최무선의화포(+10)", "최무선의투구(+5)");
            assertThat(rows).allMatch(r -> r.restrictionMercenaryNames().equals(List.of("최무선")));
            assertThat(rows.get(0).enhancement()).isEqualTo(Enhancement.FIVE);
            assertThat(rows.get(1).enhancement()).isEqualTo(Enhancement.TEN);
        }

        @Test
        @DisplayName("전설장수 — 홍길동 7종×2강 14행 (실제 HTML 구조)")
        void 전설장수_홍길동_14행() {
            String html = """
                    <html><body>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">홍길동의 봉<br>(+5)</div><div class="cell w-stat">공250</div></div>
                        <div class="sub-row rank-10"><div class="cell w-name">홍길동의 봉<br>(+10)</div><div class="cell w-stat">공340</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">홍길동의 패랭이<br>(+5)</div><div class="cell w-stat">방200</div></div>
                        <div class="sub-row rank-10"><div class="cell w-name">홍길동의 패랭이<br>(+10)</div><div class="cell w-stat">방100</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">홍길동 의복<br>(+5)</div><div class="cell w-stat">방350</div></div>
                        <div class="sub-row rank-10"><div class="cell w-name">홍길동 의복<br>(+10)</div><div class="cell w-stat">방350</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">홍길동의 팔보호구<br>(+5)</div><div class="cell w-stat">방100</div></div>
                        <div class="sub-row rank-10"><div class="cell w-name">홍길동의 팔보호구<br>(+10)</div><div class="cell w-stat">방100</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">홍길동의 요대<br>(+5)</div><div class="cell w-stat">방100</div></div>
                        <div class="sub-row rank-10"><div class="cell w-name">홍길동의 요대<br>(+10)</div><div class="cell w-stat">방100</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">홍길동의 짚신<br>(+5)</div><div class="cell w-stat">힘75</div></div>
                        <div class="sub-row rank-10"><div class="cell w-name">홍길동의 짚신<br>(+10)</div><div class="cell w-stat">힘100</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">홍길동의 반지<br>
                          (+5)</div><div class="cell w-stat">힘45</div></div>
                        <div class="sub-row rank-10"><div class="cell w-name">홍길동의 반지<br>
                          (+10)</div><div class="cell w-stat">힘60</div></div>
                      </div></div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            ExclusiveEquipmentPageConfig config =
                    ExclusiveEquipmentPageConfig.single("4j_hong.asp",
                            ExclusiveEquipPolicy.LEGENDARY_GENERAL, "홍길동");

            List<ParsedExclusiveRow> rows = GersangjjangExclusiveEquipmentParser.parsePage(doc, config);

            assertThat(rows).hasSize(14);
            assertThat(rows).extracting(r -> r.itemRow().name())
                    .contains("홍길동의 반지(+5)", "홍길동의 반지(+10)", "홍길동 의복(+10)");
            assertThat(rows).filteredOn(r -> r.itemRow().name().equals("홍길동의 반지(+10)"))
                    .extracting(ParsedExclusiveRow::enhancement)
                    .containsExactly(Enhancement.TEN);
        }

        @Test
        @DisplayName("전설장수 — 만선야 6종×2강 12행 (4j_manxian)")
        void 전설장수_만선야_12행() {
            String html = """
                    <html><body>
                      <div class="main-title">대만 전설장수 - 선인 만선야 전용템</div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">만선야의지팡이<br>(+5)</div><div class="cell w-stat">공250</div></div>
                        <div class="sub-row rank-10"><div class="cell w-name">만선야의지팡이<br>(+10)</div><div class="cell w-stat">공340</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">만선야의머리장식<br>(+5)</div><div class="cell w-stat">방200</div></div>
                        <div class="sub-row rank-10"><div class="cell w-name">만선야의머리장식<br>(+10)</div><div class="cell w-stat">방250</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">만선야의의복<br>(+5)</div><div class="cell w-stat">방300</div></div>
                        <div class="sub-row rank-10"><div class="cell w-name">만선야의의복<br>(+10)</div><div class="cell w-stat">방350</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">만선야의팔보호구<br>(+5)</div><div class="cell w-stat">힘100</div></div>
                        <div class="sub-row rank-10"><div class="cell w-name">만선야의팔보호구<br>(+10)</div><div class="cell w-stat">힘150</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">만선야의요대<br>(+5)</div><div class="cell w-stat">힘100</div></div>
                        <div class="sub-row rank-10"><div class="cell w-name">만선야의요대<br>(+10)</div><div class="cell w-stat">힘150</div></div>
                      </div></div>
                      <div class="item-row"><div class="info-container">
                        <div class="sub-row"><div class="cell w-name">만선야의신발<br>(+5)</div><div class="cell w-stat">힘75</div></div>
                        <div class="sub-row rank-10"><div class="cell w-name">만선야의신발<br>(+10)</div><div class="cell w-stat">힘100</div></div>
                      </div></div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            ExclusiveEquipmentPageConfig config =
                    ExclusiveEquipmentPageConfig.single("4j_manxian.asp",
                            ExclusiveEquipPolicy.LEGENDARY_GENERAL, "만선야");

            List<ParsedExclusiveRow> rows = GersangjjangExclusiveEquipmentParser.parsePage(doc, config);

            assertThat(rows).hasSize(12);
            assertThat(rows).allMatch(r -> r.restrictionMercenaryNames().equals(List.of("만선야")));
            assertThat(rows).extracting(r -> r.itemRow().name())
                    .contains("만선야의지팡이(+5)", "만선야의신발(+10)");
        }

        @Test
        @DisplayName("전설장수 — w-info 재료 <광개토>를 용병명으로 오인하지 않음")
        void 전설장수_재료태그_무시() {
            String html = """
                    <html><body>
                      <div class="item-row">
                        <div class="info-container">
                          <div class="sub-row">
                            <div class="cell w-name">홍길동의 반지(+5)<br>210 lv</div>
                            <div class="cell w-stat">힘+100</div>
                            <div class="cell w-info">재료: &lt;광개토&gt;태황의반지, 봉인된힘의조각30</div>
                          </div>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            ExclusiveEquipmentPageConfig config =
                    ExclusiveEquipmentPageConfig.single("4j_hong.asp",
                            ExclusiveEquipPolicy.LEGENDARY_GENERAL, "홍길동");

            List<ParsedExclusiveRow> rows = GersangjjangExclusiveEquipmentParser.parsePage(doc, config);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).restrictionMercenaryNames()).containsExactly("홍길동");
        }

        @Test
        @DisplayName("명왕 무기 — 행 태그로 각성 용병 분기")
        void 명왕_행_태그() {
            String html = """
                    <html><body>
                      <div class="main-title">항삼세명왕 전용장비</div>
                      <div class="data-row">
                        <div class="w-name"><strong>명왕검</strong></div>
                        <div class="w-stat">공250-250 &lt;항삼세명왕&gt;</div>
                      </div>
                      <div class="data-row">
                        <div class="w-name"><strong>각성명왕장</strong></div>
                        <div class="w-stat">공300-300 &lt;각성 항삼세명왕&gt;</div>
                      </div>
                      <div class="data-row">
                        <div class="w-name"><strong>항삼세명왕의투구</strong></div>
                        <div class="w-stat">방어300</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            ExclusiveEquipmentPageConfig config =
                    ExclusiveEquipmentPageConfig.dualSection("ming_hang.asp", "항삼세명왕", "각성 항삼세명왕");

            List<ParsedExclusiveRow> rows = GersangjjangExclusiveEquipmentParser.parsePage(doc, config);

            assertThat(rows).hasSize(2);
            assertThat(rows).extracting(ParsedExclusiveRow::restrictionMercenaryNames)
                    .containsExactly(List.of("항삼세명왕"), List.of("각성 항삼세명왕"));
        }

        @Test
        @DisplayName("명왕부 — 일반·각성 명왕 둘 다 restriction")
        void 명왕부_이중_restriction() {
            String html = """
                    <html><body>
                      <div class="main-title">항삼세명왕 전용장비</div>
                      <div class="data-row">
                        <div class="w-name"><strong>항삼세명왕부</strong></div>
                        <div class="w-stat">생명+150</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            ExclusiveEquipmentPageConfig config =
                    ExclusiveEquipmentPageConfig.dualSection("ming_hang.asp", "항삼세명왕", "각성 항삼세명왕");

            List<ParsedExclusiveRow> rows = GersangjjangExclusiveEquipmentParser.parsePage(doc, config);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).restrictionMercenaryNames())
                    .containsExactly("항삼세명왕", "각성 항삼세명왕");
        }

        @Test
        @DisplayName("명왕부 — w-info 설명에서 속성값+5 파싱")
        void 명왕부_w_info_속성값_파싱() {
            String html = """
                    <html><body>
                      <div class="main-title">항삼세명왕 전용장비</div>
                      <div class="data-row">
                        <div class="w-name"><strong>항삼세명왕부</strong></div>
                        <div class="w-stat">생명+150</div>
                        <div class="w-info">
                          <div class="info-text">
                            * 명왕의 힘이전대상자가 화속성일 경우 속성값+5
                          </div>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            ExclusiveEquipmentPageConfig config =
                    ExclusiveEquipmentPageConfig.dualSection("ming_hang.asp", "항삼세명왕", "각성 항삼세명왕");

            List<ParsedExclusiveRow> rows = GersangjjangExclusiveEquipmentParser.parsePage(doc, config);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).itemRow().stats())
                    .anyMatch(s -> s.statType() == StatType.ELEMENT_VALUE && s.value() == 5);
            assertThat(rows.get(0).itemRow().stats())
                    .anyMatch(s -> s.statType() == StatType.VITALITY && s.value() == 150);
        }

        @Test
        @DisplayName("부동명왕부 — w-info 속성값 미추출")
        void 부동명왕부_속성값_제외() {
            String html = """
                    <html><body>
                      <div class="main-title">부동명왕 전용장비</div>
                      <div class="data-row">
                        <div class="w-name"><strong>부동명왕부</strong></div>
                        <div class="w-stat">생명+150</div>
                        <div class="w-info">
                          <div class="info-text">* 명왕의 힘이전대상자가 화속성일 경우 속성값+5</div>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            ExclusiveEquipmentPageConfig config =
                    ExclusiveEquipmentPageConfig.dualSection("ming_bu.asp", "부동명왕", "각성 부동명왕");

            List<ParsedExclusiveRow> rows = GersangjjangExclusiveEquipmentParser.parsePage(doc, config);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).itemRow().stats())
                    .noneMatch(s -> s.statType() == StatType.ELEMENT_VALUE);
        }

        @Test
        @DisplayName("사천왕부 — 해당 섹션 용병 1명만 restriction")
        void 사천왕부_단일_restriction() {
            String html = """
                    <html><body>
                      <div class="main-title">증장천왕 전용템</div>
                      <div class="data-row">
                        <div class="w-name"><strong>증장천왕부</strong></div>
                        <div class="w-stat">민+150</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            ExclusiveEquipmentPageConfig config =
                    ExclusiveEquipmentPageConfig.dualSection("wang_zz.asp", "증장천왕", "각성 증장천왕");

            List<ParsedExclusiveRow> rows = GersangjjangExclusiveEquipmentParser.parsePage(doc, config);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).restrictionMercenaryNames()).containsExactly("증장천왕");
        }
    }
}
