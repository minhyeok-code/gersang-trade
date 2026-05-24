package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.domain.catalog.enums.Element;

/**
 * 속성 보정 계산 유틸.
 * 무속성 몬스터(element null/NONE)는 속성 우위·추가 데미지가 없으므로 보정 0.
 */
public final class ElementBonusCalculator {

    private static final double ELEMENT_BONUS_MIN = -50.0;
    private static final double ELEMENT_BONUS_MAX = 50.0;

    private ElementBonusCalculator() {}

    /** 몬스터가 속성을 보유하는지 여부 (무속성·미상 제외). */
    public static boolean hasElementAttribute(Element monsterElement) {
        return monsterElement != null && monsterElement != Element.NONE;
    }

    /** 속성깎·디버프 적용 후 몬스터 속성값. */
    public static int effectiveMonsterElementValue(Integer monsterElementValue,
                                                   int elementPierce, int elementDebuffAbs) {
        int base = monsterElementValue != null ? monsterElementValue : 0;
        return Math.max(0, base - elementPierce - elementDebuffAbs);
    }

    /**
     * 속성 보정(%).
     * 무속성 몬스터(element null/NONE)면 0.
     * = clamp((3 × 용병 속성값 - 몬스터 속성값) / 2, -50, +50)
     */
    public static double calcElementBonus(int myElementValue, int monsterElementValue,
                                          Element monsterElement) {
        if (!hasElementAttribute(monsterElement)) {
            return 0.0;
        }
        double raw = (3.0 * myElementValue - monsterElementValue) / 2.0;
        return Math.max(ELEMENT_BONUS_MIN, Math.min(ELEMENT_BONUS_MAX, raw));
    }
}
