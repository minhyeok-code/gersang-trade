package org.example.gersangtrade.crawler.parser;

import org.example.gersangtrade.crawler.config.ExclusiveEquipmentPageConfig;
import org.example.gersangtrade.crawler.service.ItemStatScopeResolver;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 거상짱 ming_hang.asp 실제 HTML 구조 기준 명왕 무기 속성 스탯 파싱 검증.
 */
class MingHangWeaponStatParseTest {

    @Test
    @DisplayName("명왕무기_속성스탯_파싱_및_scope")
    void 명왕무기_속성스탯_파싱_및_scope() {
        String html = """
                <html><body>
                  <div class="main-title">항삼세명왕 전용장비</div>
                  <div class="data-row">
                    <div class="cell w-name"><div class="name-wrap"><strong>진각명왕장</strong><br>230lv<br>&lt;각성 항삼세명왕&gt;</div></div>
                    <div class="cell w-stat">공 300-300<br>힘+700<br>생명+50<br>지력+100<br>동속아군 속성+25%<br>천결</div>
                  </div>
                  <div class="data-row">
                    <div class="cell w-name"><div class="name-wrap"><strong>각성명왕장</strong><br>210lv<br>&lt;각성 항삼세명왕&gt;</div></div>
                    <div class="cell w-stat">공 300-300<br>힘+500<br>생명+50<br>지력+100<br>동속아군 속성+20%<br>천결</div>
                  </div>
                  <div class="data-row">
                    <div class="cell w-name"><div class="name-wrap"><strong>고급명왕검</strong><br>230lv<br>&lt;항삼세명왕&gt;</div></div>
                    <div class="cell w-stat">공 250-250<br>힘+450<br>생명+50<br>지력+50<br>불속성값+10<br>천개</div>
                  </div>
                  <div class="data-row">
                    <div class="cell w-name"><div class="name-wrap"><strong>명왕검</strong><br>200lv<br>&lt;항삼세명왕&gt;</div></div>
                    <div class="cell w-stat">공 250-250<br>힘+300<br>지력+50<br>불속성값+5<br>명계의보호막</div>
                  </div>
                </body></html>
                """;

        var config = ExclusiveEquipmentPageConfig.dualSection("ming_hang.asp", "항삼세명왕", "각성 항삼세명왕");
        var rows = GersangjjangExclusiveEquipmentParser.parsePage(Jsoup.parse(html), config);

        var mingwang = rows.stream().filter(r -> r.itemRow().name().contains("명왕장")
                || r.itemRow().name().contains("명왕검")).toList();
        assertThat(mingwang).hasSize(4);

        assertElementStat(mingwang, "명왕검", 5, BuffTarget.ALLY_HEAVENLY_KING);
        assertElementStat(mingwang, "고급명왕검", 10, BuffTarget.ALLY_HEAVENLY_KING);
        assertPercentShare(mingwang, "각성명왕장", 20);
        assertPercentShare(mingwang, "진각명왕장", 25);
    }

    @Test
    @DisplayName("명왕궁_뇌전속성값_명왕극_바람속성값_파싱")
    void 명왕궁_명왕극_복합속성접두어() {
        var gunConfig = ExclusiveEquipmentPageConfig.dualSection("ming_gun.asp", "군다리명왕", "각성 군다리명왕");
        var gunRows = GersangjjangExclusiveEquipmentParser.parsePage(Jsoup.parse("""
                <html><body>
                  <div class="main-title">군다리명왕 전용장비</div>
                  <div class="data-row">
                    <div class="cell w-name"><div class="name-wrap"><strong>명왕궁</strong><br>200lv<br>&lt;군다리명왕&gt;</div></div>
                    <div class="cell w-stat">공 250-250<br>민첩+350<br>뇌전속성값+5<br>명계의우뢰</div>
                  </div>
                  <div class="data-row">
                    <div class="cell w-name"><div class="name-wrap"><strong>고급명왕궁</strong><br>230lv<br>&lt;군다리명왕&gt;</div></div>
                    <div class="cell w-stat">민첩+500<br>뇌전속성값+10<br>천속</div>
                  </div>
                </body></html>
                """), gunConfig);
        assertElementStat(gunRows, "명왕궁", 5, BuffTarget.ALLY_HEAVENLY_KING, Element.THUNDER);
        assertElementStat(gunRows, "고급명왕궁", 10, BuffTarget.ALLY_HEAVENLY_KING, Element.THUNDER);

        var daConfig = ExclusiveEquipmentPageConfig.dualSection("ming_da.asp", "대위덕명왕", "각성 대위덕명왕");
        var daRows = GersangjjangExclusiveEquipmentParser.parsePage(
                Jsoup.parse("""
                        <html><body>
                          <div class="main-title">대위덕명왕 전용장비</div>
                          <div class="data-row">
                            <div class="cell w-name"><div class="name-wrap"><strong>명왕극</strong><br>200lv<br>&lt;대위덕명왕&gt;</div></div>
                            <div class="cell w-stat">생명+300<br>바람속성값+5<br>명계의돌풍</div>
                          </div>
                          <div class="data-row">
                            <div class="cell w-name"><div class="name-wrap"><strong>고급명왕극</strong><br>230lv<br>&lt;대위덕명왕&gt;</div></div>
                            <div class="cell w-stat">생명+450<br>바람속성값+10<br>천해</div>
                          </div>
                        </body></html>
                        """),
                daConfig);
        assertElementStat(daRows, "명왕극", 5, BuffTarget.ALLY_HEAVENLY_KING, Element.WIND);
        assertElementStat(daRows, "고급명왕극", 10, BuffTarget.ALLY_HEAVENLY_KING, Element.WIND);
    }

    private static void assertElementStat(
            java.util.List<GersangjjangExclusiveEquipmentParser.ParsedExclusiveRow> rows,
            String name, int value, BuffTarget scope, Element element) {
        var row = rows.stream().filter(r -> r.itemRow().name().equals(name)).findFirst().orElseThrow();
        var stat = row.itemRow().stats().stream()
                .filter(s -> s.statType() == StatType.ELEMENT_VALUE && s.statUnit() == StatUnit.FLAT)
                .findFirst().orElseThrow();
        assertThat(stat.element()).isEqualTo(element);
        assertThat(stat.value()).isEqualTo(value);
        assertThat(ItemStatScopeResolver.resolve(name, row.slot(), stat.statType(), stat.statUnit()))
                .isEqualTo(scope);
    }

    private static void assertElementStat(
            java.util.List<GersangjjangExclusiveEquipmentParser.ParsedExclusiveRow> rows,
            String name, int value, BuffTarget scope) {
        assertElementStat(rows, name, value, scope, Element.FIRE);
    }

    private static void assertPercentShare(
            java.util.List<GersangjjangExclusiveEquipmentParser.ParsedExclusiveRow> rows,
            String name, int percent) {
        var row = rows.stream().filter(r -> r.itemRow().name().equals(name)).findFirst().orElseThrow();
        var stat = row.itemRow().stats().stream()
                .filter(s -> s.statType() == StatType.ELEMENT_VALUE && s.statUnit() == StatUnit.PERCENT)
                .findFirst().orElseThrow();
        assertThat(stat.value()).isEqualTo(percent);
        assertThat(ItemStatScopeResolver.resolve(name, row.slot(), stat.statType(), stat.statUnit()))
                .isEqualTo(BuffTarget.ALLY_SAME_ELEMENT);
    }
}
