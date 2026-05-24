package org.example.gersangtrade.crawler.parser;

import org.example.gersangtrade.crawler.parser.GersangjjangSetParser.ParsedSetRow;
import org.example.gersangtrade.crawler.parser.GersangjjangSetParser.ParsedSetEffect;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
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

/**
 * GersangjjangSetParser 단위 테스트.
 * 아이템명 suffix 슬롯 감지 및 세트 행 파싱을 검증한다.
 */
@DisplayName("GersangjjangSetParser")
class GersangjjangSetParserTest {

    // ── detectSlotByName ────────────────────────────────────────────────────

    @Nested
    @DisplayName("detectSlotByName")
    class DetectSlotByName {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "공명관,    HELMET",
                "공명극,    WEAPON",
                "공명갑옷,  ARMOR",
                "공명장갑,  GLOVES",
                "공명요대,  BELT",
                "공명화,    SHOES",
                "공명반지,  RING"
        })
        @DisplayName("suffix 매핑 정확도 검증")
        void suffix_슬롯_매핑(String itemName, String expectedSlot) {
            Optional<EquipmentSlot> result = GersangjjangSetParser.detectSlotByName(itemName.trim());
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(EquipmentSlot.valueOf(expectedSlot.trim()));
        }

        @Test
        @DisplayName("매핑 없는 이름은 empty 반환")
        void 미매핑_이름_empty() {
            Optional<EquipmentSlot> result = GersangjjangSetParser.detectSlotByName("알수없는아이템");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("전투화는 '화'보다 우선 매핑 (SHOES)")
        void 전투화_우선_매핑() {
            // "전투화" suffix가 "화" suffix보다 먼저 매핑되어야 SHOES로 결정
            Optional<EquipmentSlot> result = GersangjjangSetParser.detectSlotByName("명왕전투화");
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(EquipmentSlot.SHOES);
        }
    }

    // ── parseSetRows ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("parseSetRows")
    class ParseSetRows {

        @Test
        @DisplayName("세트명과 레벨 파싱")
        void 세트명_레벨_파싱() {
            String html = """
                    <html><body>
                      <div class="g-table">
                        <div class="g-row">
                          <div class="lv-col">공명<br>(215lv)</div>
                          <div class="item-col">관 (방어300, 힘50)<br>의 (방어300, 힘50)</div>
                          <div class="effect-col"><strong>2종 힘50</strong></div>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ParsedSetRow> rows = GersangjjangSetParser.parseSetRows(doc);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).setName()).isEqualTo("공명");
            assertThat(rows.get(0).level()).isEqualTo(215);
        }

        @Test
        @DisplayName("세트 효과 'N종 스탯' 복합 형식 파싱")
        void 세트효과_복합형식_파싱() {
            String html = """
                    <html><body>
                      <div class="g-table">
                        <div class="g-row">
                          <div class="lv-col">테스트<br>(200lv)</div>
                          <div class="item-col">관 (방어300)</div>
                          <div class="effect-col"><strong>2종 힘50<br>4종 타저25%</strong></div>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ParsedSetRow> rows = GersangjjangSetParser.parseSetRows(doc);

            assertThat(rows).hasSize(1);
            List<ParsedSetEffect> effects = rows.get(0).effects();
            assertThat(effects).anyMatch(e ->
                    e.requiredPieces() == 2 && e.statType() == StatType.STRENGTH
                            && e.statValue() == 50 && e.statUnit() == StatUnit.FLAT);
            assertThat(effects).anyMatch(e ->
                    e.requiredPieces() == 4 && e.statType() == StatType.HITTING_RESISTANCE
                            && e.statValue() == 25 && e.statUnit() == StatUnit.PERCENT);
        }

        @Test
        @DisplayName("세트 효과 'N종' 단독 + 다음 줄 스탯 형식 파싱")
        void 세트효과_종수단독_파싱() {
            String html = """
                    <html><body>
                      <div class="g-table">
                        <div class="g-row">
                          <div class="lv-col">영웅<br>(210lv)</div>
                          <div class="item-col">관 (방어200)</div>
                          <div class="effect-col"><strong>3종<br>방어150</strong></div>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ParsedSetRow> rows = GersangjjangSetParser.parseSetRows(doc);

            assertThat(rows).hasSize(1);
            List<ParsedSetEffect> effects = rows.get(0).effects();
            assertThat(effects).anyMatch(e ->
                    e.requiredPieces() == 3 && e.statType() == StatType.DEFENSE && e.statValue() == 150);
        }

        @Test
        @DisplayName("속성별 세트 효과 '(땅속성3)' → ELEMENT_VALUE + EARTH + FLAT")
        void 속성_세트효과_파싱() {
            String html = """
                    <html><body>
                      <div class="g-table">
                        <div class="g-row">
                          <div class="lv-col">대지<br>(200lv)</div>
                          <div class="item-col">관 (방어200)</div>
                          <div class="effect-col"><strong>5종 (땅속성3)</strong></div>
                        </div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<ParsedSetRow> rows = GersangjjangSetParser.parseSetRows(doc);

            assertThat(rows).hasSize(1);
            List<ParsedSetEffect> effects = rows.get(0).effects();
            assertThat(effects).anyMatch(e ->
                    e.requiredPieces() == 5
                            && e.statType() == StatType.ELEMENT_VALUE
                            && e.element() == Element.EARTH
                            && e.statUnit() == StatUnit.FLAT);
        }

        @Test
        @DisplayName("빈 페이지는 빈 리스트 반환")
        void 빈_페이지() {
            Document doc = Jsoup.parse("<html><body><div class=\"g-table\"></div></body></html>");
            List<ParsedSetRow> rows = GersangjjangSetParser.parseSetRows(doc);
            assertThat(rows).isEmpty();
        }
    }
}
