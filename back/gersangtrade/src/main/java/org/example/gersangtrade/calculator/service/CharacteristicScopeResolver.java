package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.domain.catalog.CharacteristicEffect;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.BuffValueType;
import org.example.gersangtrade.domain.catalog.enums.StatType;

/**
 * 특성·스킬 효과의 적용 범위(scope)와 적용 방식을 판별한다.
 *
 * <p>MercenaryCharacteristicLevel은 명시적 target 컬럼이 없으므로 statType·수치·label로 추론한다.
 * CharacteristicEffect(전설장수)는 DB에 저장된 {@link BuffTarget}을 그대로 사용한다.
 */
public final class CharacteristicScopeResolver {

    private CharacteristicScopeResolver() {}

    /** 스탯/데미지 적용 방식 */
    public enum ApplicationMode {
        /** flat/percent 스탯 맵에 가산 */
        STAT,
        /** 스킬 DPS 계산 시 damage_percent_sum에 가산 */
        SKILL_DAMAGE,
        /** DPS·스탯 합산에서 제외 */
        SKIP
    }

    /** scope + 적용 방식 + 대상 스킬명(null이면 전체 스킬) */
    public record ScopedEffect(
            BuffTarget target,
            ApplicationMode mode,
            boolean percent,
            String targetSkillName
    ) {
        public ScopedEffect(BuffTarget target, ApplicationMode mode, boolean percent) {
            this(target, mode, percent, null);
        }
    }

    /**
     * 사천왕·명왕·주인공 특성 레벨 행의 scope를 추론한다.
     * statType=null이면 label이 "{스킬명} 데미지" 패턴일 때 SELF 스킬 데미지로 처리한다.
     */
    public static ScopedEffect resolve(MercenaryCharacteristicLevel level) {
        if (level == null || level.getAmountValue() == null) return null;

        StatType statType = level.getStatType();
        float value = level.getAmountValue();
        boolean percent = level.getAmount() != null && level.getAmount().endsWith("%");
        String label = level.getLabel();

        if (statType == null) {
            String skillName = extractSkillNameFromDamageLabel(label);
            if (skillName != null) {
                return new ScopedEffect(BuffTarget.SELF, ApplicationMode.SKILL_DAMAGE, true, skillName);
            }
            return null;
        }

        // 명왕 스탯 이전율 — % 수치이며 MyungwangStatTransferCalculator에서만 적용 (flat 스탯 가산 금지)
        if (isStatTransferRateLabel(label)) {
            return new ScopedEffect(BuffTarget.ALLY, ApplicationMode.SKIP, true);
        }

        return switch (statType) {
            case SKILL_DAMAGE_PERCENT ->
                    new ScopedEffect(BuffTarget.SELF, ApplicationMode.SKILL_DAMAGE, true);
            case DAMAGE_PERCENT, DAMAGE_PERCENT_GROUND, DAMAGE_PERCENT_AIR ->
                    new ScopedEffect(BuffTarget.ALLY, ApplicationMode.SKILL_DAMAGE, true);
            case RESIST_PIERCE ->
                    new ScopedEffect(BuffTarget.ENEMY, ApplicationMode.STAT, false);
            case MAGIC_RESISTANCE, HITTING_RESISTANCE ->
                    value < 0
                            ? new ScopedEffect(BuffTarget.ENEMY, ApplicationMode.STAT, percent)
                            : new ScopedEffect(BuffTarget.SELF, ApplicationMode.STAT, percent);
            case ELEMENT_VALUE ->
                    value < 0
                            ? new ScopedEffect(BuffTarget.ENEMY, ApplicationMode.STAT, false)
                            : new ScopedEffect(BuffTarget.SELF, ApplicationMode.STAT, false);
            case MIN_POWER, MAX_POWER ->
                    value < 0
                            ? new ScopedEffect(BuffTarget.ENEMY, ApplicationMode.STAT, false)
                            : new ScopedEffect(BuffTarget.ALLY, ApplicationMode.STAT, false);
            case MOVE_SPEED, SKILL_RANGE ->
                    value < 0
                            ? new ScopedEffect(BuffTarget.ENEMY, ApplicationMode.STAT, percent)
                            : new ScopedEffect(BuffTarget.SELF, ApplicationMode.STAT, percent);
            case ATTACK_SPEED, MP_RECOVERY, HP_RECOVERY, FIELD_MOVE_SPEED ->
                    new ScopedEffect(BuffTarget.ALLY, ApplicationMode.STAT, percent);
            // % 표기여도 flat으로 저장해 effectiveStats에서 직접 읽는다 (rawDmg 배율 적용용)
            case BASE_DAMAGE_MULTIPLIER ->
                    new ScopedEffect(BuffTarget.SELF, ApplicationMode.STAT, false);
            default ->
                    new ScopedEffect(BuffTarget.SELF, ApplicationMode.STAT, percent);
        };
    }

    /** 전설장수 CharacteristicEffect — DB target 컬럼 사용 */
    public static ScopedEffect resolve(CharacteristicEffect effect) {
        if (effect == null) return null;

        boolean percent = effect.getValueType() == BuffValueType.PERCENT_ADD;
        ApplicationMode mode = switch (effect.getStatType()) {
            case SKILL_DAMAGE_PERCENT, DAMAGE_PERCENT, DAMAGE_PERCENT_GROUND, DAMAGE_PERCENT_AIR ->
                    ApplicationMode.SKILL_DAMAGE;
            case ATTACK_SPEED, MP_RECOVERY, HP_RECOVERY, FIELD_MOVE_SPEED, CRITICAL_CHANCE ->
                    ApplicationMode.SKIP;
            default -> ApplicationMode.STAT;
        };

        return new ScopedEffect(effect.getTarget(), mode, percent);
    }

    /** "{스킬명} 데미지" label에서 스킬명 추출. 해당 패턴이 아니면 null. */
    static String extractSkillNameFromDamageLabel(String label) {
        if (label == null) return null;
        String suffix = " 데미지";
        if (!label.endsWith(suffix)) return null;
        String skillName = label.substring(0, label.length() - suffix.length()).trim();
        return skillName.isEmpty() ? null : skillName;
    }

    /**
     * 명왕 스탯 이전율 특성 label 여부.
     * 예: "힘 이전율"(각성), "이전되는 힘"(일반) — flat 스탯이 아니라 이전 비율(%)이다.
     */
    public static boolean isStatTransferRateLabel(String label) {
        if (label == null) return false;
        return label.contains("이전율") || label.contains("이전되는");
    }
}
