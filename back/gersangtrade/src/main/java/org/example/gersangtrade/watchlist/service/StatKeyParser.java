package org.example.gersangtrade.watchlist.service;

import org.example.gersangtrade.domain.user.enums.SetComposition;

import java.util.Arrays;
import java.util.Optional;

/**
 * trade_confirmed.stat_key_snapshot 문자열 파싱.
 * 거래완료 시세의 유연 매칭에 사용한다.
 */
public final class StatKeyParser {

    private StatKeyParser() {}

    public record ParsedSetKey(Long setId, SetComposition composition, int ritualCount, String mark) {}

    public record ParsedItemKey(Long itemId, String ritualMark) {}

    public static Optional<ParsedSetKey> parseSet(String statKey) {
        if (statKey == null || !statKey.startsWith("SET:")) {
            return Optional.empty();
        }
        String[] parts = statKey.split(":", -1);
        if (parts.length < 8 || !"COMP".equals(parts[2]) || !"RC".equals(parts[4]) || !"MARK".equals(parts[6])) {
            return Optional.empty();
        }
        try {
            Long setId = Long.parseLong(parts[1]);
            SetComposition composition = SetComposition.valueOf(parts[3]);
            int ritualCount = Integer.parseInt(parts[5]);
            String markPart = parts.length == 8
                    ? parts[7]
                    : String.join(":", Arrays.copyOfRange(parts, 7, parts.length));
            String mark = "NONE".equals(markPart) ? null : markPart;
            return Optional.of(new ParsedSetKey(setId, composition, ritualCount, mark));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public static Optional<ParsedItemKey> parseItem(String statKey) {
        if (statKey == null || !statKey.startsWith("ITEM:")) {
            return Optional.empty();
        }
        String[] parts = statKey.split(":", -1);
        try {
            Long itemId = Long.parseLong(parts[1]);
            if (parts.length >= 4 && "RITUAL".equals(parts[2])) {
                String mark = parts.length == 4
                        ? parts[3]
                        : String.join(":", Arrays.copyOfRange(parts, 3, parts.length));
                return Optional.of(new ParsedItemKey(itemId, mark));
            }
            return Optional.of(new ParsedItemKey(itemId, null));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }
}
