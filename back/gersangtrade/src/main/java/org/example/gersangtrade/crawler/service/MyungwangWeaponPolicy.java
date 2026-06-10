package org.example.gersangtrade.crawler.service;

/**
 * 명왕 전용 무기 이름 규칙 — 크롤 scope·DPS 계산기에서 공통 사용.
 */
public final class MyungwangWeaponPolicy {

    private MyungwangWeaponPolicy() {}

    /** 부동명왕 무기(명왕월) — 올스텟 이전 전용, 속성값 공유 대상 아님 */
    public static boolean isBudongWeapon(String itemName) {
        return itemName != null && itemName.contains("명왕월");
    }

    /** 각성 명왕 무기 — 동속아군 속성+n% */
    public static boolean isAwakenedWeapon(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return false;
        }
        return itemName.contains("각성명왕") || itemName.contains("진각명왕");
    }

    /**
     * 일반/고급 명왕 무기 — 동속성 사천왕에게 고정 속성값 이전.
     * 고급명왕월·각성명왕장 등은 제외.
     */
    public static boolean isNormalOrAdvancedWeapon(String itemName) {
        if (itemName == null || itemName.isBlank() || isBudongWeapon(itemName)) {
            return false;
        }
        if (isAwakenedWeapon(itemName)) {
            return false;
        }
        if (itemName.contains("고급명왕")) {
            return true;
        }
        return itemName.contains("명왕검") || itemName.contains("명왕장")
                || itemName.contains("명왕궁") || itemName.contains("명왕극")
                || itemName.contains("명왕갑") || itemName.contains("명왕혼겸")
                || itemName.contains("명왕비") || itemName.contains("명왕주");
    }
}
