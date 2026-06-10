package org.example.gersangtrade.crawler.service;

import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;

/**
 * 크롤링·UPSERT 시 아이템 스탯의 적용 범위(scope)를 결정한다.
 */
public final class ItemStatScopeResolver {

    private ItemStatScopeResolver() {}

    /**
     * 파싱된 스탯 한 줄의 scope를 반환한다.
     *
     * <p>명왕부(TALISMAN) 속성값 → 동속성 사천왕({@link BuffTarget#ALLY_HEAVENLY_KING}).
     * <p>일반/고급 명왕 무기 속성값 → 동속성 사천왕({@link BuffTarget#ALLY_HEAVENLY_KING}).
     * <p>각성 명왕 무기 동속아군 속성+n% → 동속성 아군 전원({@link BuffTarget#ALLY_SAME_ELEMENT}).
     */
    public static BuffTarget resolve(String itemName, EquipmentSlot slot,
                                     StatType statType, StatUnit statUnit) {
        if (isMyeongwangHeavenlyKingElementValue(itemName, slot, statType)) {
            return BuffTarget.ALLY_HEAVENLY_KING;
        }
        if (isAwakenedMyeongwangWeaponShare(itemName, slot, statType, statUnit)) {
            return BuffTarget.ALLY_SAME_ELEMENT;
        }
        return BuffTarget.SELF;
    }

    /** 하위 호환 — statUnit 미지정 시 FLAT로 간주 */
    public static BuffTarget resolve(String itemName, EquipmentSlot slot, StatType statType) {
        return resolve(itemName, slot, statType, StatUnit.FLAT);
    }

    /** 명왕부 속성값 → 사천왕 버프 (부동명왕 제외) */
    private static boolean isMyeongwangHeavenlyKingElementValue(
            String itemName, EquipmentSlot slot, StatType statType) {
        if (statType != StatType.ELEMENT_VALUE) {
            return false;
        }
        if (slot == EquipmentSlot.TALISMAN) {
            if (itemName == null || !itemName.endsWith("명왕부")) {
                return false;
            }
            return !itemName.contains("부동");
        }
        if (slot == EquipmentSlot.WEAPON) {
            return MyungwangWeaponPolicy.isNormalOrAdvancedWeapon(itemName);
        }
        return false;
    }

    /** 각성 명왕 무기 — 동속아군 속성+n% */
    private static boolean isAwakenedMyeongwangWeaponShare(
            String itemName, EquipmentSlot slot, StatType statType, StatUnit statUnit) {
        if (slot != EquipmentSlot.WEAPON || statType != StatType.ELEMENT_VALUE) {
            return false;
        }
        if (statUnit != StatUnit.PERCENT) {
            return false;
        }
        return MyungwangWeaponPolicy.isAwakenedWeapon(itemName);
    }
}
