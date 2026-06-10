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
        // BANSSANG·FULL_BANSSANG은 주술 접두가 제목에 반영되지 않으므로 ritual 필드 강제 초기화
        if (composition == SetComposition.BANSSANG || composition == SetComposition.FULL_BANSSANG) {
            ritualCount = 0;
            mark = null;
        }
        String markPart = (ritualCount > 0 && mark != null) ? mark : "NONE";
        return "SET:" + setId + ":COMP:" + composition.name() + ":RC:" + ritualCount + ":MARK:" + markPart;
    }
}
