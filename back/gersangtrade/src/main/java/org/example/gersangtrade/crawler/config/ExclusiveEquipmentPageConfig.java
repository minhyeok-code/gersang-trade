package org.example.gersangtrade.crawler.config;

import org.example.gersangtrade.crawler.parser.GersangjjangParser;

import java.util.List;

/**
 * 전용장비 크롤링 대상 페이지 정적 설정.
 * URL·정책·시더 canonical Mercenary.name 매핑을 보관한다.
 *
 * @param href                  상대 경로 (예: wang_zz.asp)
 * @param policy                restriction 적용 정책
 * @param defaultMercenaryName  단일 용병 페이지 기본값 (전설장수·주인공 국가별). null 가능
 * @param baseMercenaryName     사천왕·명왕 일반 섹션 용병명. null 가능
 * @param awakenedMercenaryName 사천왕·명왕 각성 섹션 용병명. null이면 각성 섹션 없음
 */
public record ExclusiveEquipmentPageConfig(
        String href,
        ExclusiveEquipPolicy policy,
        String defaultMercenaryName,
        String baseMercenaryName,
        String awakenedMercenaryName
) {
    public String url() {
        return GersangjjangParser.BASE_URL + href;
    }

    /** 단일 용병 페이지용 팩토리 */
    public static ExclusiveEquipmentPageConfig single(
            String href, ExclusiveEquipPolicy policy, String mercenaryName) {
        return new ExclusiveEquipmentPageConfig(href, policy, mercenaryName, null, null);
    }

    /** 사천왕·명왕 2섹션 페이지용 팩토리 */
    public static ExclusiveEquipmentPageConfig dualSection(
            String href, String baseName, String awakenedName) {
        return new ExclusiveEquipmentPageConfig(
                href, ExclusiveEquipPolicy.HEAVENLY_KING_AND_MYEONGWANG,
                null, baseName, awakenedName);
    }

    /** 주인공 인형 페이지용 팩토리 */
    public static ExclusiveEquipmentPageConfig protagonistDoll(String href) {
        return new ExclusiveEquipmentPageConfig(
                href, ExclusiveEquipPolicy.PROTAGONIST_DOLL, null, null, null);
    }

    /** 크롤링 대상 전체 페이지 목록 */
    public static List<ExclusiveEquipmentPageConfig> all() {
        return List.of(
                // ── 사천왕 ────────────────────────────────────────────────────
                dualSection("wang_cg.asp", "지국천왕", "각성 지국천왕"),
                dualSection("wang_gm.asp", "광목천왕", "각성 광목천왕"),
                dualSection("wang_zz.asp", "증장천왕", "각성 증장천왕"),
                dualSection("wang_dm.asp", "다문천왕", "각성 다문천왕"),
                // ── 명왕 ──────────────────────────────────────────────────────
                dualSection("ming_hang.asp", "항삼세명왕", "각성 항삼세명왕"),
                dualSection("ming_gun.asp", "군다리명왕", "각성 군다리명왕"),
                dualSection("ming_da.asp", "대위덕명왕", "각성 대위덕명왕"),
                dualSection("ming_kum.asp", "금강야차명왕", "각성 금강야차명왕"),
                new ExclusiveEquipmentPageConfig(
                        "ming_bu.asp", ExclusiveEquipPolicy.HEAVENLY_KING_AND_MYEONGWANG,
                        null, "부동명왕", null),
                // ── 전설장수 ────────────────────────────────────────────────────
                single("4j_lvbu.asp",    ExclusiveEquipPolicy.LEGENDARY_GENERAL, "여포"),
                single("4j_nobu.asp",    ExclusiveEquipPolicy.LEGENDARY_GENERAL, "노부츠나"),
                single("4j_choi.asp",     ExclusiveEquipPolicy.LEGENDARY_GENERAL, "최무선"),
                single("4j_chiyome.asp",  ExclusiveEquipPolicy.LEGENDARY_GENERAL, "치요메"),
                single("4j_chosen.asp",   ExclusiveEquipPolicy.LEGENDARY_GENERAL, "초선"),
                single("4j_mazo.asp",     ExclusiveEquipPolicy.LEGENDARY_GENERAL, "마조"),
                single("4j_meng.asp",     ExclusiveEquipPolicy.LEGENDARY_GENERAL, "맹획"),
                single("4j_boku.asp",     ExclusiveEquipPolicy.LEGENDARY_GENERAL, "보쿠텐"),
                single("4j_hong.asp",     ExclusiveEquipPolicy.LEGENDARY_GENERAL, "홍길동"),
                single("4j_zhumeng.asp",  ExclusiveEquipPolicy.LEGENDARY_GENERAL, "주몽"),
                single("4j_hua.asp",      ExclusiveEquipPolicy.LEGENDARY_GENERAL, "화목란"),
                single("4j_baji.asp",     ExclusiveEquipPolicy.LEGENDARY_GENERAL, "바지라오"),
                single("4j_akbar.asp",    ExclusiveEquipPolicy.LEGENDARY_GENERAL, "악바르"),
                single("4j_manxian.asp",  ExclusiveEquipPolicy.LEGENDARY_GENERAL, "만선야"), // 6종, 반지 없음
                single("4j_rejina.asp",   ExclusiveEquipPolicy.LEGENDARY_GENERAL, "레지나"),
                // ── 주인공 ──────────────────────────────────────────────────────
                protagonistDoll("zhu_bian.asp"),
                single("z_kr1.asp", ExclusiveEquipPolicy.PROTAGONIST_NATIONAL, "신궁"),
                single("z_kr2.asp", ExclusiveEquipPolicy.PROTAGONIST_NATIONAL, "포수"),
                single("z_jp1.asp", ExclusiveEquipPolicy.PROTAGONIST_NATIONAL, "검호"),
                single("z_jp2.asp", ExclusiveEquipPolicy.PROTAGONIST_NATIONAL, "이타코"),
                single("z_cn1.asp", ExclusiveEquipPolicy.PROTAGONIST_NATIONAL, "일대종사"),
                single("z_cn2.asp", ExclusiveEquipPolicy.PROTAGONIST_NATIONAL, "강신"),
                single("z_tw1.asp", ExclusiveEquipPolicy.PROTAGONIST_NATIONAL, "도사"),    // 대만男
                single("z_tw2.asp", ExclusiveEquipPolicy.PROTAGONIST_NATIONAL, "백수왕"), // 대만女
                single("z_in1.asp", ExclusiveEquipPolicy.PROTAGONIST_NATIONAL, "투신"),
                single("z_in2.asp", ExclusiveEquipPolicy.PROTAGONIST_NATIONAL, "무희")
        );
    }
}
