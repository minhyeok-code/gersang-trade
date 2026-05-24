package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CharacteristicScopeResolver")
class CharacteristicScopeResolverTest {

    @Test
    @DisplayName("스킬명 데미지 label은 SELF 스킬 데미지로 해석")
    void skillDamageLabel() {
        var level = MercenaryCharacteristicLevel.builder()
                .label("염룡살진 데미지")
                .level(5)
                .amount("50%")
                .amountValue(50f)
                .build();

        var scoped = CharacteristicScopeResolver.resolve(level);

        assertThat(scoped).isNotNull();
        assertThat(scoped.targetSkillName()).isEqualTo("염룡살진");
        assertThat(scoped.mode()).isEqualTo(CharacteristicScopeResolver.ApplicationMode.SKILL_DAMAGE);
    }

    @Test
    @DisplayName("음수 마법저항은 ENEMY 디버프")
    void negativeMagicResistIsEnemy() {
        var level = MercenaryCharacteristicLevel.builder()
                .label("마법 저항력")
                .level(5)
                .amount("-15")
                .amountValue(-15f)
                .statType(org.example.gersangtrade.domain.catalog.enums.StatType.MAGIC_RESISTANCE)
                .build();

        var scoped = CharacteristicScopeResolver.resolve(level);

        assertThat(scoped).isNotNull();
        assertThat(scoped.target()).isEqualTo(org.example.gersangtrade.domain.catalog.enums.BuffTarget.ENEMY);
    }

    @Test
    @DisplayName("각성 명왕 힘 이전율은 flat 스탯이 아니라 SKIP")
    void awakenedMyeongwangTransferRateLabelIsSkipped() {
        var level = MercenaryCharacteristicLevel.builder()
                .label("힘 이전율")
                .level(5)
                .amount("30%")
                .amountValue(30f)
                .statType(org.example.gersangtrade.domain.catalog.enums.StatType.STRENGTH)
                .build();

        var scoped = CharacteristicScopeResolver.resolve(level);

        assertThat(scoped).isNotNull();
        assertThat(scoped.mode()).isEqualTo(CharacteristicScopeResolver.ApplicationMode.SKIP);
        assertThat(scoped.target()).isEqualTo(org.example.gersangtrade.domain.catalog.enums.BuffTarget.ALLY);
        assertThat(scoped.percent()).isTrue();
    }

    @Test
    @DisplayName("일반 명왕 이전되는 힘도 SKIP")
    void regularMyeongwangTransferLabelIsSkipped() {
        var level = MercenaryCharacteristicLevel.builder()
                .label("이전되는 힘")
                .level(10)
                .amount("15%")
                .amountValue(15f)
                .statType(org.example.gersangtrade.domain.catalog.enums.StatType.STRENGTH)
                .build();

        var scoped = CharacteristicScopeResolver.resolve(level);

        assertThat(scoped).isNotNull();
        assertThat(scoped.mode()).isEqualTo(CharacteristicScopeResolver.ApplicationMode.SKIP);
    }
}
