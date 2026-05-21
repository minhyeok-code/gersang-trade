package org.example.gersangtrade.admin.dto.request;

/**
 * 직접 측정값 업데이트 요청.
 * skill_type은 JSON 적재 시 이미 저장되므로 여기서는 측정값만 받는다.
 *
 * <p>INSTANT   → castsPerSecond 필수.
 * <p>PERSISTENT → tickIntervalMs 필수.
 */
public record SkillCoefficientMeasurementRequest(
        Float castsPerSecond,
        Integer tickIntervalMs,
        String measurementNote
) {}
