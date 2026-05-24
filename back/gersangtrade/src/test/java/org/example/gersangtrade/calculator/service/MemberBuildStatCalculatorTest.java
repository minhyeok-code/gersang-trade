package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.calculator.dto.request.BonusStatTarget;
import org.example.gersangtrade.catalog.repository.SkillCoefficientRepository;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.ItemSkill;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.SkillCoefficient;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberBuildStatCalculator")
class MemberBuildStatCalculatorTest {

    @Mock
    private SkillCoefficientRepository skillCoefficientRepository;

    @InjectMocks
    private MemberBuildStatCalculator calculator;

    @Test
    @DisplayName("resolveMainStat — 공격력 제외, STR/DEX/VIT/INT 중 최대 계수")
    void resolveMainStat_최대계수() {
        SkillCoefficient akbar = mockCoefValues(0f, 0f, 20f, 17f);
        assertThat(MemberBuildStatCalculator.resolveMainStat(akbar)).isEqualTo(StatType.VITALITY);

        SkillCoefficient strMain = mockCoefValues(20f, 10f, 5f, 5f);
        assertThat(MemberBuildStatCalculator.resolveMainStat(strMain)).isEqualTo(StatType.STRENGTH);
    }

    @Test
    @DisplayName("악바르 — 주스탯 투자 시 레벨 260 스탯은 생명력(주스탯)에 부여")
    void 악바르_주스탯_생명력() {
        mockMercenaryCoef(0f, 0f, 20f, 17f);
        UserDeckMember member = member(BonusStatTarget.MAIN_STAT, 260, 0);

        MemberBuildStatCalculator.BuildStatBonus result =
                calculator.compute(member, List.of());

        assertThat(result.resolvedMainStat()).isEqualTo(StatType.VITALITY);
        assertThat(result.levelBonusStats()).containsEntry(StatType.VITALITY, 2466);
    }

    @Test
    @DisplayName("장비·용병 계수 모두 없으면 생명력 기본값")
    void 계수없음_생명력_기본() {
        given(skillCoefficientRepository.findByMercenaryIdIn(anyList()))
                .willReturn(List.of());

        MemberBuildStatCalculator.BuildStatBonus result =
                calculator.compute(member(BonusStatTarget.MAIN_STAT, 260, 0), List.of());

        assertThat(result.resolvedMainStat()).isEqualTo(StatType.VITALITY);
        assertThat(result.levelBonusStats()).containsEntry(StatType.VITALITY, 2466);
    }

    @Test
    @DisplayName("장비 스킬 계수가 있으면 장비 기준 주스탯 판별")
    void 장비스킬_우선() {
        long itemId = 99L;
        SkillCoefficient itemCoef = mockItemCoef(itemId, 30f, 0f, 0f, 0f);
        given(skillCoefficientRepository.findByItemIdIn(anyList()))
                .willReturn(List.of(itemCoef));

        UserDeckMember member = member(BonusStatTarget.MAIN_STAT, 260, 0);
        UserDeckMemberSlot slot = mockEquippedSlot(itemId);

        MemberBuildStatCalculator.BuildStatBonus result =
                calculator.compute(member, List.of(slot));

        assertThat(result.resolvedMainStat()).isEqualTo(StatType.STRENGTH);
        assertThat(result.levelBonusStats()).containsEntry(StatType.STRENGTH, 2466);
    }

    @Test
    @DisplayName("생명력 투자 — 레벨 스탯은 주스탯, 보너스만 생명력")
    void 생명력_보너스만_생명력() {
        mockMercenaryCoef(20f, 0f, 10f, 0f);
        UserDeckMember member = member(BonusStatTarget.VITALITY, 260, 500);

        MemberBuildStatCalculator.BuildStatBonus result =
                calculator.compute(member, List.of());

        assertThat(result.levelBonusStats()).containsEntry(StatType.STRENGTH, 2466);
        assertThat(result.bonusStats()).containsEntry(StatType.VITALITY, 500);
    }

    private void mockMercenaryCoef(float str, float dex, float vit, float intel) {
        SkillCoefficient coef = mockCoefValues(str, dex, vit, intel);
        given(skillCoefficientRepository.findByMercenaryIdIn(anyList()))
                .willReturn(List.of(coef));
    }

    private static SkillCoefficient mockCoefValues(float str, float dex, float vit, float intel) {
        SkillCoefficient coef = mock(SkillCoefficient.class);
        when(coef.getCoefStr()).thenReturn(str);
        when(coef.getCoefDex()).thenReturn(dex);
        when(coef.getCoefVit()).thenReturn(vit);
        when(coef.getCoefInt()).thenReturn(intel);
        return coef;
    }

    private static SkillCoefficient mockItemCoef(long itemId, float str, float dex, float vit, float intel) {
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(itemId);
        ItemSkill itemSkill = mock(ItemSkill.class);
        when(itemSkill.getItem()).thenReturn(item);
        SkillCoefficient coef = mockCoefValues(str, dex, vit, intel);
        when(coef.getItemSkill()).thenReturn(itemSkill);
        return coef;
    }

    private static UserDeckMemberSlot mockEquippedSlot(long itemId) {
        EquipmentItem equipmentItem = mock(EquipmentItem.class);
        when(equipmentItem.getItemId()).thenReturn(itemId);

        UserDeckMemberSlot slot = mock(UserDeckMemberSlot.class);
        when(slot.getEquipmentItem()).thenReturn(equipmentItem);
        return slot;
    }

    private static UserDeckMember member(BonusStatTarget target, int level, int bonusAmount) {
        Mercenary mercenary = Mercenary.builder()
                .name("악바르")
                .key("akbar")
                .category(MercenaryCategory.LEGENDARY_GENERAL)
                .nature(Nature.FIRE)
                .build();
        setMercenaryId(mercenary, 1L);

        UserDeckMember member = UserDeckMember.builder()
                .deck(null)
                .mercenary(mercenary)
                .build();
        setMemberId(member, 10L);
        member.updateBuild(level, target, bonusAmount);
        return member;
    }

    private static void setMemberId(UserDeckMember member, Long id) {
        try {
            var field = UserDeckMember.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(member, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setMercenaryId(Mercenary mercenary, Long id) {
        try {
            var field = Mercenary.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(mercenary, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
