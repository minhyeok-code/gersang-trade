package org.example.gersangtrade.watchlist.service;

import org.example.gersangtrade.domain.user.enums.SetComposition;

/**
 * watchKey 문자열 생성·파싱 유틸리티.
 *
 * 형식:
 *   ITEM (주술 없음)  : ITEM:{itemId}
 *   ITEM (주술 있음)  : ITEM:{itemId}:RITUAL:{mark}
 *   SET               : SET:{setId}:COMP:{composition}:RC:{ritualCount}:MARK:{mark|NONE}
 */
public final class WatchKeyBuilder {

    private WatchKeyBuilder() {}

    public static String itemKey(Long itemId) {
        return "ITEM:" + itemId;
    }

    public static String itemKey(Long itemId, String ritualMark) {
        if (ritualMark == null || ritualMark.isBlank()) {
            return itemKey(itemId);
        }
        return "ITEM:" + itemId + ":RITUAL:" + ritualMark;
    }

    public static String setKey(Long setId, SetComposition composition, int ritualCount, String mark) {
        // BANSSANG(반지쌍 단독)은 반지에 주술이 없으므로 RC:0 강제
        // FULL_BANSSANG(5피스+반지쌍)은 갑옷 주술이 제목에 반영되므로 강제하지 않는다
        if (composition == SetComposition.BANSSANG) {
            ritualCount = 0;
            mark = null;
        }
        String markPart = (ritualCount > 0 && mark != null) ? mark : "NONE";
        return "SET:" + setId + ":COMP:" + composition.name() + ":RC:" + ritualCount + ":MARK:" + markPart;
    }
}
