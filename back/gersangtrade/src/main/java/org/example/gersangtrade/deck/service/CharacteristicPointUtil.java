package org.example.gersangtrade.deck.service;

import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;

/**
 * 용병 특성 포인트 소모 계산.
 *
 * <p>일반(사천왕·명왕·주인공): {@code point × selectedLevel}
 * <p>전설장수: 스텁 {@code point=null} — 배분 레벨 자체가 소모 포인트 ({@code selectedLevel} 합산)
 */
public final class CharacteristicPointUtil {

    private CharacteristicPointUtil() {}

    /** 단일 특성 선택의 포인트 소모량 */
    public static int selectionCost(MercenaryCharacteristic characteristic, int selectedLevel) {
        Integer point = characteristic.getPoint();
        if (point == null || point == 0) {
            return selectedLevel;
        }
        return point * selectedLevel;
    }

    /** 선택 목록의 총 포인트 소모량 */
    public static int totalUsedPoints(
            java.util.Map<Long, MercenaryCharacteristic> characteristicMap,
            java.util.List<? extends CharacteristicSelection> selections) {
        int used = 0;
        for (CharacteristicSelection entry : selections) {
            MercenaryCharacteristic ch = characteristicMap.get(entry.characteristicId());
            if (ch == null) continue;
            used += selectionCost(ch, entry.selectedLevel());
        }
        return used;
    }

    /** 특성 저장 요청 엔트리 공통 인터페이스 */
    public interface CharacteristicSelection {
        Long characteristicId();
        Integer selectedLevel();
    }
}
