package org.example.gersangtrade.calculator.dto.response;

/**
 * 스킬 1개의 DPS 계산 결과.
 * calculated가 false이면 casts_per_second / tick_interval_ms 미측정 상태.
 */
public record SkillDpsResult(
        String skillName,
        double dps,
        boolean calculated
) {}
