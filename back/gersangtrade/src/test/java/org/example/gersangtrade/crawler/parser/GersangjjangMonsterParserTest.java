package org.example.gersangtrade.crawler.parser;

import org.example.gersangtrade.crawler.parser.GersangjjangMonsterParser.MonsterRow;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GersangjjangMonsterParser 단위 테스트.
 * HTML 픽스처로 패턴 A/B/C 감지 및 파싱 결과를 검증한다.
 */
@DisplayName("GersangjjangMonsterParser")
class GersangjjangMonsterParserTest {

    private static final String PAGE_URL = "https://www.gersangjjang.com/monster/test.asp";

    // ── 인덱스 파싱 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("parseIndexUrls")
    class ParseIndexUrls {

        @Test
        @DisplayName("monster 링크만 수집하고 quest·index 자기자신 제외")
        void monster링크만_수집() {
            String html = """
                    <html><body>
                      <a href="/monster/longshan.asp">롱산</a>
                      <a href="/monster/youmingjie.asp">유명계</a>
                      <a href="/quest/main.asp">퀘스트</a>
                      <a href="/monster/index.asp">인덱스</a>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<String> urls = GersangjjangMonsterParser.parseIndexUrls(doc);

            assertThat(urls).hasSize(2);
            assertThat(urls).allMatch(u -> u.contains("/monster/"));
            assertThat(urls).noneMatch(u -> u.contains("/quest/"));
            assertThat(urls).noneMatch(u -> u.endsWith("index.asp"));
        }

        @Test
        @DisplayName("중복 URL은 한 번만 수집")
        void 중복URL_한번만() {
            String html = """
                    <html><body>
                      <a href="/monster/star.asp">별</a>
                      <a href="/monster/star.asp">별(중복)</a>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<String> urls = GersangjjangMonsterParser.parseIndexUrls(doc);

            assertThat(urls).hasSize(1);
        }

        @Test
        @DisplayName("monster 링크 없으면 빈 목록")
        void 링크없으면_빈목록() {
            Document doc = Jsoup.parse("<html><body><a href='/quest/q.asp'>퀘</a></body></html>");
            assertThat(GersangjjangMonsterParser.parseIndexUrls(doc)).isEmpty();
        }
    }

    // ── 패턴 A (구형) ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("패턴 A — 구형 (저항력 라인에 타저/마저/속성값 혼재)")
    class PatternA {

        @Test
        @DisplayName("타저·마저·수속성값 파싱")
        void 타저마저수속성_파싱() {
            String html = monsterHtml(
                    "연못거북(水) (9급)",
                    "<img src='x'>",   // sprite에 텍스트 없음
                    "<strong>경험치</strong>: 12.5만, <strong>저항력</strong>: 타저 290%, 마저 290%, 수속성 20"
            );
            MonsterRow row = parseFirst(html);

            assertThat(row.name()).isEqualTo("연못거북(水) (9급)");
            assertThat(row.hp()).isNull();
            assertThat(row.hittingResistance()).isEqualTo(290);
            assertThat(row.magicResistance()).isEqualTo(290);
            assertThat(row.elementValue()).isEqualTo(20);
            assertThat(row.element()).isEqualTo(Element.WATER);
        }

        @Test
        @DisplayName("화속성 파싱")
        void 화속성_파싱() {
            String html = monsterHtml(
                    "불꽃도깨비 (7급)",
                    "<img src='x'>",
                    "<strong>저항력</strong>: 타저 310%, 마저 280%, 화속성 15"
            );
            MonsterRow row = parseFirst(html);

            assertThat(row.element()).isEqualTo(Element.FIRE);
            assertThat(row.elementValue()).isEqualTo(15);
        }

        @Test
        @DisplayName("속성 없는 구형 몹 — element·elementValue 모두 null")
        void 속성없는구형몹() {
            String html = monsterHtml(
                    "산적 (10급)",
                    "<img src='x'>",
                    "<strong>저항력</strong>: 타저 200%, 마저 200%"
            );
            MonsterRow row = parseFirst(html);

            assertThat(row.element()).isNull();
            assertThat(row.elementValue()).isNull();
            assertThat(row.hittingResistance()).isEqualTo(200);
        }
    }

    // ── 패턴 B (신형 보스) ──────────────────────────────────────────────────

    @Nested
    @DisplayName("패턴 B — 신형 보스 (sprite에 hp+타저+마저, info-section에 속성값 태그)")
    class PatternB {

        @Test
        @DisplayName("hp·타저·마저·속성값·element 모두 파싱")
        void 전체필드_파싱() {
            String html = monsterHtml(
                    "악령 전이 (雷) (boss급)",
                    "공격력 1700<br>체력 400만, 마력 8만<br>타저 410%, 마저 420%",
                    "<strong>경험치</strong>: 520만, <strong>속성값: </strong>170"
            );
            MonsterRow row = parseFirst(html);

            assertThat(row.hp()).isEqualTo(4_000_000L);
            assertThat(row.hittingResistance()).isEqualTo(410);
            assertThat(row.magicResistance()).isEqualTo(420);
            assertThat(row.elementValue()).isEqualTo(170);
            assertThat(row.element()).isEqualTo(Element.THUNDER);
        }

        @Test
        @DisplayName("체력 1680만 — 단위 만 파싱")
        void 체력_만단위_파싱() {
            String html = monsterHtml(
                    "대형 보스 (火) (boss급)",
                    "체력 1680만<br>타저 500%, 마저 500%",
                    "<strong>속성값: </strong>200"
            );
            MonsterRow row = parseFirst(html);

            assertThat(row.hp()).isEqualTo(16_800_000L);
            assertThat(row.element()).isEqualTo(Element.FIRE);
        }

        @Test
        @DisplayName("속성값 0 — 정상 저장")
        void 속성값_0_저장() {
            String html = monsterHtml(
                    "무속성보스 (明) (boss급)",
                    "체력 200만<br>타저 350%, 마저 360%",
                    "<strong>속성값: </strong>0"
            );
            MonsterRow row = parseFirst(html);

            assertThat(row.elementValue()).isEqualTo(0);
            assertThat(row.element()).isEqualTo(Element.NONE);
        }
    }

    // ── 패턴 C (중간형) ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("패턴 C — 중간형 (sprite 텍스트 없음, info-section에 속성값 태그 분리)")
    class PatternC {

        @Test
        @DisplayName("저항력 라벨 — 타저·마저·속성값·element 파싱")
        void 저항력라벨_파싱() {
            String html = monsterHtml(
                    "광려산멧돼지 (雷) (9급)",
                    "<img src='x'>",
                    "<strong>경험치</strong>: 15만, <strong>속성값:</strong> 20 " +
                    "<strong>저항력</strong>: 타저 290%, 마저 290%"
            );
            MonsterRow row = parseFirst(html);

            assertThat(row.hp()).isNull();
            assertThat(row.hittingResistance()).isEqualTo(290);
            assertThat(row.magicResistance()).isEqualTo(290);
            assertThat(row.elementValue()).isEqualTo(20);
            assertThat(row.element()).isEqualTo(Element.THUNDER);
        }

        @Test
        @DisplayName("저항 라벨(축약형) — 타저·마저 파싱")
        void 저항라벨축약형_파싱() {
            String html = monsterHtml(
                    "풍속몹 (風) (8급)",
                    "<img src='x'>",
                    "<strong>속성값</strong>: 250, <strong>저항</strong>: 타저 415%, 마저 405%"
            );
            MonsterRow row = parseFirst(html);

            assertThat(row.hittingResistance()).isEqualTo(415);
            assertThat(row.magicResistance()).isEqualTo(405);
            assertThat(row.elementValue()).isEqualTo(250);
            assertThat(row.element()).isEqualTo(Element.WIND);
        }
    }

    // ── element 파싱 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("element 파싱")
    class ElementParsing {

        @Test
        @DisplayName("한자 괄호별 Element 매핑")
        void 한자괄호_Element매핑() {
            assertElement("몹 (水)", Element.WATER);
            assertElement("몹 (雷)", Element.THUNDER);
            assertElement("몹 (火)", Element.FIRE);
            assertElement("몹 (風)", Element.WIND);
            assertElement("몹 (土)", Element.EARTH);
            assertElement("몹 (明)", Element.NONE);
        }

        @Test
        @DisplayName("괄호 없는 이름 — element null")
        void 괄호없는이름_null() {
            String html = monsterHtml("그냥몹 (9급)", "<img src='x'>",
                    "<strong>저항력</strong>: 타저 200%, 마저 200%");
            MonsterRow row = parseFirst(html);
            assertThat(row.element()).isNull();
        }

        private void assertElement(String name, Element expected) {
            String html = monsterHtml(name, "<img src='x'>",
                    "<strong>저항력</strong>: 타저 200%, 마저 200%");
            MonsterRow row = parseFirst(html);
            assertThat(row.element()).isEqualTo(expected);
        }
    }

    // ── 예외·경계값 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("예외·경계값")
    class EdgeCases {

        @Test
        @DisplayName("monster-row 없으면 빈 목록")
        void monsterRow없으면_빈목록() {
            Document doc = Jsoup.parse("<html><body><p>내용 없음</p></body></html>");
            List<MonsterRow> rows = GersangjjangMonsterParser.parseMonsterRows(doc, PAGE_URL);
            assertThat(rows).isEmpty();
        }

        @Test
        @DisplayName("monster-name 비어있으면 해당 행 스킵")
        void 이름비면_스킵() {
            String html = """
                    <html><body>
                      <div class="monster-row">
                        <div class="monster-name"></div>
                        <div class="monster-sprite"><img src="x"></div>
                        <div class="info-section"><strong>저항력</strong>: 타저 200%, 마저 200%</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<MonsterRow> rows = GersangjjangMonsterParser.parseMonsterRows(doc, PAGE_URL);
            assertThat(rows).isEmpty();
        }

        @Test
        @DisplayName("여러 monster-row — 전부 파싱")
        void 여러row_전부파싱() {
            String html = """
                    <html><body>
                      <div class="monster-row">
                        <div class="monster-name">몹A (水) (9급)</div>
                        <div class="monster-sprite"><img src="x"></div>
                        <div class="info-section"><strong>저항력</strong>: 타저 290%, 마저 290%, 수속성 20</div>
                      </div>
                      <div class="monster-row">
                        <div class="monster-name">몹B (火) (8급)</div>
                        <div class="monster-sprite"><img src="x"></div>
                        <div class="info-section"><strong>저항력</strong>: 타저 310%, 마저 310%, 화속성 15</div>
                      </div>
                    </body></html>
                    """;
            Document doc = Jsoup.parse(html);
            List<MonsterRow> rows = GersangjjangMonsterParser.parseMonsterRows(doc, PAGE_URL);

            assertThat(rows).hasSize(2);
            assertThat(rows.get(0).element()).isEqualTo(Element.WATER);
            assertThat(rows.get(1).element()).isEqualTo(Element.FIRE);
        }
    }

    // ── 픽스처 헬퍼 ─────────────────────────────────────────────────────────

    private static String monsterHtml(String name, String spriteContent, String infoContent) {
        return """
                <html><body>
                  <div class="monster-row">
                    <div class="monster-name">%s</div>
                    <div class="monster-sprite">%s</div>
                    <div class="info-section">%s</div>
                  </div>
                </body></html>
                """.formatted(name, spriteContent, infoContent);
    }

    private static MonsterRow parseFirst(String html) {
        Document doc = Jsoup.parse(html);
        List<MonsterRow> rows = GersangjjangMonsterParser.parseMonsterRows(doc, PAGE_URL);
        assertThat(rows).isNotEmpty();
        return rows.get(0);
    }
}
