package org.example.gersangtrade.domain.catalog.enums;

/**
 * 전용장비 강화 단계.
 * 거상의 유효한 강화 단계는 0강 / 5강 / 10강 세 가지다.
 * DB에는 value 값(0 / 5 / 10)으로 저장된다 — EnhancementConverter 참고.
 */
public enum Enhancement {
    NONE(0),
    FIVE(5),
    TEN(10);

    private final int value;

    Enhancement(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Enhancement fromValue(int value) {
        for (Enhancement e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("유효하지 않은 강화 단계: " + value);
    }
}
