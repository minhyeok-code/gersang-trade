package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.calculator.dto.request.DpsRequest;
import org.example.gersangtrade.calculator.dto.response.DpsResponse;
import org.example.gersangtrade.catalog.repository.EquipmentSetEffectRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetSkillEffectRepository;
import org.example.gersangtrade.catalog.repository.ItemSkillEffectRepository;
import org.example.gersangtrade.catalog.repository.ItemSkillMappingRepository;
import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.catalog.repository.MercenarySkillEffectRepository;
import org.example.gersangtrade.catalog.service.LegendGeneralLoadService;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.catalog.repository.MercenaryStatRepository;
import org.example.gersangtrade.catalog.repository.MonsterRepository;
import org.example.gersangtrade.catalog.repository.RitualSetEffectRepository;
import org.example.gersangtrade.catalog.repository.RitualStatRepository;
import org.example.gersangtrade.catalog.repository.SkillCoefficientRepository;
import org.example.gersangtrade.deck.repository.UserDeckMemberCharacteristicRepository;
import org.example.gersangtrade.deck.repository.UserDeckMemberRepository;
import org.example.gersangtrade.deck.repository.UserDeckMemberSlotRepository;
import org.example.gersangtrade.deck.repository.UserDeckRepository;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.EquipmentSetSkillEffect;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.ItemSkill;
import org.example.gersangtrade.domain.catalog.ItemSkillEffect;
import org.example.gersangtrade.domain.catalog.ItemSkillMapping;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.MercenarySkillEffect;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.MercenarySkill;
import org.example.gersangtrade.domain.catalog.Monster;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.catalog.RitualSetEffect;
import org.example.gersangtrade.domain.catalog.SetGrantedSkill;
import org.example.gersangtrade.domain.catalog.SkillCoefficient;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.Enhancement;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.SkillType;
import org.example.gersangtrade.domain.catalog.enums.StatSource;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.example.gersangtrade.domain.catalog.enums.TriggerSource;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.ValueType;
import org.example.gersangtrade.domain.deck.UserDeck;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.example.gersangtrade.domain.deck.UserDeckMemberCharacteristic;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlot;
import org.example.gersangtrade.domain.deck.UserDeckMemberSlotRitual;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DpsCalculatorService 단위 테스트.
 *
 * <p>기본 계산 1케이스 + 특성 레벨 보너스 4케이스 + 주술 세트효과 2케이스 + 세트 부여 스킬 4케이스 = 총 11케이스.
 *
 * <p>기본 시나리오:
 * <ul>
 *   <li>용병 1명, STR=1000, coefStr=1.0, castsPerSecond=2.0, hitCount=1
 *   <li>레벨 250 → levelStat=2256 → rawDps = (1000+2256)×1×2.0 = 6512.0
 *   <li>몬스터 저항 0 → resistPassRate = 100-(0×0.16+57) = 43.0%
 *   <li>속성값 0 → elementBonus = 0 → totalDps = 6512.0×0.43 = 2800.16
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DpsCalculatorServiceTest {

    @Mock private UserDeckRepository deckRepository;
    @Mock private UserDeckMemberRepository memberRepository;
    @Mock private UserDeckMemberSlotRepository slotRepository;
    @Mock private UserDeckMemberCharacteristicRepository characteristicRepository;
    @Mock private MercenaryStatRepository mercenaryStatRepository;
    @Mock private MercenaryCharacteristicLevelRepository characteristicLevelRepository;
    @Mock private ItemStatRepository itemStatRepository;
    @Mock private EquipmentSetEffectRepository setEffectRepository;
    @Mock private EquipmentSetSkillEffectRepository setSkillEffectRepository;
    @Mock private RitualSetEffectRepository ritualSetEffectRepository;
    @Mock private RitualStatRepository ritualStatRepository;
    @Mock private SkillCoefficientRepository skillCoefficientRepository;
    @Mock private ItemSkillMappingRepository itemSkillMappingRepository;
    @Mock private MercenarySkillEffectRepository mercenarySkillEffectRepository;
    @Mock private ItemSkillEffectRepository itemSkillEffectRepository;
    @Mock private MonsterRepository monsterRepository;
    @Mock private LegendGeneralLoadService legendGeneralLoadService;
    @Mock private MercenaryCharacteristicRepository mercenaryCharacteristicRepository;
    @Mock private LegendGeneralBuffCalculator legendGeneralBuffCalculator;
    @Mock private PlayerCharacterBuffCalculator playerCharacterBuffCalculator;
    @Mock private AwakenedMyungwangBuffCalculator awakenedMyungwangBuffCalculator;
    @Mock private MyungwangStatTransferCalculator myungwangStatTransferCalculator;
    @Mock private BudongMyungwangWeaponTransferCalculator budongMyungwangWeaponTransferCalculator;
    @Mock private DeckStateMerger deckStateMerger;

    @InjectMocks
    private DpsCalculatorService service;

    private static final Long DECK_ID    = 1L;
    private static final Long MEMBER_ID  = 10L;
    private static final Long MERC_ID    = 100L;
    private static final Long ITEM_ID    = 200L;
    private static final Long MONSTER_ID = 300L;

    private Mercenary mercenary;
    private UserDeckMember member;
    private org.example.gersangtrade.domain.catalog.EquipmentItem item;
    private UserDeckMemberSlot slot;

    @BeforeEach
    void setUp() {
        mercenary = mock(Mercenary.class);
        when(mercenary.getId()).thenReturn(MERC_ID);
        when(mercenary.getName()).thenReturn("테스트용병");
        when(mercenary.getNature()).thenReturn(Nature.FIRE);
        when(mercenary.getCategory()).thenReturn(MercenaryCategory.FOUR_HEAVENLY_KINGS);

        member = mock(UserDeckMember.class);
        when(member.getId()).thenReturn(MEMBER_ID);
        when(member.getMercenary()).thenReturn(mercenary);
        when(member.getLevel()).thenReturn(250);

        item = mock(org.example.gersangtrade.domain.catalog.EquipmentItem.class);
        when(item.getItemId()).thenReturn(ITEM_ID);
        when(item.getEquipmentSet()).thenReturn(null);

        slot = mock(UserDeckMemberSlot.class);
        when(slot.getDeckMember()).thenReturn(member);
        when(slot.getEquipmentItem()).thenReturn(item);
        when(slot.getRitual()).thenReturn(null);

        Monster monster = mock(Monster.class);
        when(monster.getId()).thenReturn(MONSTER_ID);
        when(monster.getName()).thenReturn("테스트몬스터");
        when(monster.getHittingResistance()).thenReturn(0);
        when(monster.getElementValue()).thenReturn(0);

        MercenarySkill mercSkill = mock(MercenarySkill.class);
        when(mercSkill.getMercenary()).thenReturn(mercenary);
        when(mercSkill.getSkillName()).thenReturn("기본스킬");

        MercenaryStat strStat = mock(MercenaryStat.class);
        when(strStat.getMercenary()).thenReturn(mercenary);
        when(strStat.getStatKey()).thenReturn(StatType.STRENGTH);
        when(strStat.getStatValue()).thenReturn(1000);

        SkillCoefficient baseCoef = mock(SkillCoefficient.class);
        when(baseCoef.isMercenarySkill()).thenReturn(true);
        when(baseCoef.getMercenarySkill()).thenReturn(mercSkill);
        when(baseCoef.getCoefStr()).thenReturn(1.0f);
        when(baseCoef.getCoefDex()).thenReturn(0.0f);
        when(baseCoef.getCoefVit()).thenReturn(0.0f);
        when(baseCoef.getCoefInt()).thenReturn(0.0f);
        when(baseCoef.getCoefAtk()).thenReturn(0.0f);
        when(baseCoef.getCoefLvl()).thenReturn(0.0f);
        when(baseCoef.getSkillType()).thenReturn(SkillType.INSTANT);
        when(baseCoef.getCastsPerSecond()).thenReturn(2.0f);
        when(baseCoef.getHitCount()).thenReturn(1);

        UserDeck deck = mock(UserDeck.class);
        when(deck.getSpirit1()).thenReturn(null);
        when(deck.getSpirit2()).thenReturn(null);
        when(deck.getJinbeopSource()).thenReturn(null);
        when(deck.getCheungjinSource()).thenReturn(null);
        when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));
        when(deckRepository.existsById(DECK_ID)).thenReturn(true);
        when(memberRepository.findByDeckIdWithMercenary(DECK_ID)).thenReturn(List.of(member));
        when(slotRepository.findByDeckMemberIdIn(anyList())).thenReturn(List.of(slot));
        when(mercenaryStatRepository.findByMercenaryIdIn(anyList())).thenReturn(List.of(strStat));
        when(itemStatRepository.findByItemIdIn(anyList())).thenReturn(List.of());
        when(characteristicRepository.findByDeckMemberIdIn(anyList())).thenReturn(List.of());
        when(mercenaryCharacteristicRepository.findByMercenaryId(MERC_ID)).thenReturn(List.of());
        when(skillCoefficientRepository.findByMercenaryIdIn(anyList())).thenReturn(List.of(baseCoef));
        when(itemSkillMappingRepository.findByItemIdIn(anyList())).thenReturn(List.of());
        when(mercenarySkillEffectRepository.findBySkill_MercenaryIdIn(anyList())).thenReturn(List.of());
        when(itemSkillEffectRepository.findBySkillIdIn(anyList())).thenReturn(List.of());
        when(monsterRepository.findById(MONSTER_ID)).thenReturn(Optional.of(monster));

        when(myungwangStatTransferCalculator.computeReceivedTransfers(anyList(), any(), any()))
                .thenReturn(new MyungwangStatTransferCalculator.ComputedTransfers(Map.of(), Map.of()));
        when(ritualStatRepository.findByRitualIdIn(anyList())).thenReturn(List.of());
    }

    private DpsRequest req() {
        return new DpsRequest(DECK_ID, MONSTER_ID, null, null);
    }

    // ── 기본 계산 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("기본_DPS_계산_정상")
    void 기본_DPS_계산_정상() {
        DpsResponse res = service.calculate(req());

        assertThat(res.totalResistPierce()).isZero();
        assertThat(res.resistPassRate()).isEqualTo(43.0);
        assertThat(res.memberResults()).hasSize(1);
        assertThat(res.memberResults().get(0).rawDps()).isEqualTo(1628L);
        assertThat(res.totalDps()).isEqualTo(700L);
    }

    @Test
    @DisplayName("무속성_몬스터_용병_속성값_있어도_속성보정_0")
    void 무속성_몬스터_용병_속성값_있어도_속성보정_0() {
        org.example.gersangtrade.domain.catalog.Item statItem = mock(org.example.gersangtrade.domain.catalog.Item.class);
        when(statItem.getId()).thenReturn(ITEM_ID);

        ItemStat elementStat = mock(ItemStat.class);
        when(elementStat.getItem()).thenReturn(statItem);
        when(elementStat.getStatType()).thenReturn(StatType.ELEMENT_VALUE);
        when(elementStat.getValue()).thenReturn(20);
        when(elementStat.getElement()).thenReturn(Element.NONE);
        when(elementStat.getScope()).thenReturn(BuffTarget.SELF);
        when(elementStat.getStatUnit()).thenReturn(StatUnit.FLAT);
        when(itemStatRepository.findByItemIdIn(anyList())).thenReturn(List.of(elementStat));

        Monster monster = monsterRepository.findById(MONSTER_ID).orElseThrow();
        when(monster.getElement()).thenReturn(null);
        when(monster.getElementValue()).thenReturn(null);

        DpsResponse res = service.calculate(req());

        assertThat(res.memberResults().get(0).elementBonus()).isZero();
        assertThat(res.memberResults().get(0).elementValue()).isEqualTo(20);
        assertThat(res.rawTotalDps()).isEqualTo(res.adjustTotalDps());
    }

    @Test
    @DisplayName("속성_몬스터_속성보정_적용")
    void 속성_몬스터_속성보정_적용() {
        org.example.gersangtrade.domain.catalog.Item statItem = mock(org.example.gersangtrade.domain.catalog.Item.class);
        when(statItem.getId()).thenReturn(ITEM_ID);

        ItemStat elementStat = mock(ItemStat.class);
        when(elementStat.getItem()).thenReturn(statItem);
        when(elementStat.getStatType()).thenReturn(StatType.ELEMENT_VALUE);
        when(elementStat.getValue()).thenReturn(20);
        when(elementStat.getElement()).thenReturn(Element.NONE);
        when(elementStat.getScope()).thenReturn(BuffTarget.SELF);
        when(elementStat.getStatUnit()).thenReturn(StatUnit.FLAT);
        when(itemStatRepository.findByItemIdIn(anyList())).thenReturn(List.of(elementStat));

        Monster monster = monsterRepository.findById(MONSTER_ID).orElseThrow();
        when(monster.getElement()).thenReturn(Element.FIRE);
        when(monster.getElementValue()).thenReturn(10);

        DpsResponse res = service.calculate(req());

        assertThat(res.memberResults().get(0).elementBonus()).isCloseTo(25.0, offset(0.01));
        assertThat(res.adjustTotalDps()).isEqualTo(Math.round(res.rawTotalDps() * 1.25));
    }

    // ── 특성 레벨 보너스 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("특성_FLAT_RESIST_PIERCE_totalResistPierce에_반영")
    void 특성_FLAT_RESIST_PIERCE_totalResistPierce에_반영() {
        MercenaryCharacteristic characteristic = characteristicMock(400L);

        UserDeckMemberCharacteristic memberChar = mock(UserDeckMemberCharacteristic.class);
        when(memberChar.getDeckMember()).thenReturn(member);
        when(memberChar.getCharacteristic()).thenReturn(characteristic);
        when(memberChar.getSelectedLevel()).thenReturn(1);

        MercenaryCharacteristicLevel level = charLevelMock(characteristic, 1, StatType.RESIST_PIERCE, 100.0f, "100");

        when(characteristicRepository.findByDeckMemberIdIn(anyList())).thenReturn(List.of(memberChar));
        when(characteristicLevelRepository.findByCharacteristicIdIn(anyList())).thenReturn(List.of(level));

        DpsResponse res = service.calculate(req());

        assertThat(res.totalResistPierce()).isEqualTo(100);
    }

    @Test
    @DisplayName("특성_PERCENT_STRENGTH_rawDps_증가")
    void 특성_PERCENT_STRENGTH_rawDps_증가() {
        // STR 50% → effectiveSTR=1500 → rawDps=(1500+2256)/2.0=1878.0
        MercenaryCharacteristic characteristic = characteristicMock(400L);

        UserDeckMemberCharacteristic memberChar = mock(UserDeckMemberCharacteristic.class);
        when(memberChar.getDeckMember()).thenReturn(member);
        when(memberChar.getCharacteristic()).thenReturn(characteristic);
        when(memberChar.getSelectedLevel()).thenReturn(1);

        MercenaryCharacteristicLevel level = charLevelMock(characteristic, 1, StatType.STRENGTH, 50.0f, "50%");

        when(characteristicRepository.findByDeckMemberIdIn(anyList())).thenReturn(List.of(memberChar));
        when(characteristicLevelRepository.findByCharacteristicIdIn(anyList())).thenReturn(List.of(level));

        DpsResponse res = service.calculate(req());

        assertThat(res.memberResults().get(0).rawDps()).isEqualTo(1878L);
    }

    @Test
    @DisplayName("특성_selectedLevel_불일치_보너스_미반영")
    void 특성_selectedLevel_불일치_보너스_미반영() {
        MercenaryCharacteristic characteristic = characteristicMock(400L);

        UserDeckMemberCharacteristic memberChar = mock(UserDeckMemberCharacteristic.class);
        when(memberChar.getDeckMember()).thenReturn(member);
        when(memberChar.getCharacteristic()).thenReturn(characteristic);
        when(memberChar.getSelectedLevel()).thenReturn(2); // 선택 레벨=2

        // DB에는 level=1 데이터만 존재 → 불일치 → skip
        MercenaryCharacteristicLevel level = charLevelMock(characteristic, 1, StatType.STRENGTH, 500.0f, "500");

        when(characteristicRepository.findByDeckMemberIdIn(anyList())).thenReturn(List.of(memberChar));
        when(characteristicLevelRepository.findByCharacteristicIdIn(anyList())).thenReturn(List.of(level));

        DpsResponse res = service.calculate(req());

        assertThat(res.memberResults().get(0).rawDps()).isEqualTo(1628L);
    }

    @Test
    @DisplayName("특성_statType_null_보너스_미반영")
    void 특성_statType_null_보너스_미반영() {
        MercenaryCharacteristic characteristic = characteristicMock(400L);

        UserDeckMemberCharacteristic memberChar = mock(UserDeckMemberCharacteristic.class);
        when(memberChar.getDeckMember()).thenReturn(member);
        when(memberChar.getCharacteristic()).thenReturn(characteristic);
        when(memberChar.getSelectedLevel()).thenReturn(1);

        // statType=null → 계산기에서 skip
        MercenaryCharacteristicLevel level = charLevelMock(characteristic, 1, null, 500.0f, "500");

        when(characteristicRepository.findByDeckMemberIdIn(anyList())).thenReturn(List.of(memberChar));
        when(characteristicLevelRepository.findByCharacteristicIdIn(anyList())).thenReturn(List.of(level));

        DpsResponse res = service.calculate(req());

        assertThat(res.memberResults().get(0).rawDps()).isEqualTo(1628L);
    }

    // ── 주술 세트효과 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("주술세트효과_피스수달성_RESIST_PIERCE_반영")
    void 주술세트효과_피스수달성_RESIST_PIERCE_반영() {
        givenSlotWithRitual(500L, 600L, RitualOutcome.SUCCESS);

        RitualSetEffect effect = ritualSetEffectMock(600L, RitualOutcome.SUCCESS, 500L, 1, StatType.RESIST_PIERCE, 50);
        when(ritualSetEffectRepository.findByRitualIdInAndEquipmentSetIdIn(anyList(), anyList()))
                .thenReturn(List.of(effect));
        when(setSkillEffectRepository.findByEquipmentSetIdIn(anyList())).thenReturn(List.of());

        DpsResponse res = service.calculate(req());

        assertThat(res.totalResistPierce()).isEqualTo(50);
    }

    @Test
    @DisplayName("주술세트효과_피스수미달_RESIST_PIERCE_미반영")
    void 주술세트효과_피스수미달_RESIST_PIERCE_미반영() {
        givenSlotWithRitual(500L, 600L, RitualOutcome.SUCCESS);

        // requiredRitualPieces=2인데 착용은 1피스 → 미달
        RitualSetEffect effect = ritualSetEffectMock(600L, RitualOutcome.SUCCESS, 500L, 2, StatType.RESIST_PIERCE, 50);
        when(ritualSetEffectRepository.findByRitualIdInAndEquipmentSetIdIn(anyList(), anyList()))
                .thenReturn(List.of(effect));
        when(setSkillEffectRepository.findByEquipmentSetIdIn(anyList())).thenReturn(List.of());

        DpsResponse res = service.calculate(req());

        assertThat(res.totalResistPierce()).isZero();
    }

    // ── 세트 부여 스킬 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("세트부여스킬_조건달성_skillResults에_추가")
    void 세트부여스킬_조건달성_skillResults에_추가() {
        SetGrantedSkill sgSkill = sgSkillMock(700L, StatSource.SELF, TriggerSource.SELF);

        // requiredPieces=1, NONE 강화 이상 → FIVE 착용 피스 1개로 달성
        givenSetWithSkillEffect(500L, sgSkill, 1, Enhancement.NONE);

        SkillCoefficient sgCoef = sgCoefMock(sgSkill);
        when(skillCoefficientRepository.findBySetGrantedSkillIdIn(anyList())).thenReturn(List.of(sgCoef));

        DpsResponse res = service.calculate(req());

        // 기본 용병 스킬 1개 + 세트 부여 스킬 1개
        assertThat(res.memberResults().get(0).skillResults()).hasSize(2);
    }

    @Test
    @DisplayName("세트부여스킬_피스수미달_skillResults_변화없음")
    void 세트부여스킬_피스수미달_skillResults_변화없음() {
        SetGrantedSkill sgSkill = sgSkillMock(700L, StatSource.SELF, TriggerSource.SELF);

        // requiredPieces=2인데 착용은 1피스 → 미달
        givenSetWithSkillEffect(500L, sgSkill, 2, Enhancement.NONE);

        SkillCoefficient sgCoef = sgCoefMock(sgSkill);
        when(skillCoefficientRepository.findBySetGrantedSkillIdIn(anyList())).thenReturn(List.of(sgCoef));

        DpsResponse res = service.calculate(req());

        assertThat(res.memberResults().get(0).skillResults()).hasSize(1);
    }

    @Test
    @DisplayName("세트부여스킬_StatSource_AFFINITY_skip")
    void 세트부여스킬_StatSource_AFFINITY_skip() {
        SetGrantedSkill sgSkill = sgSkillMock(700L, StatSource.AFFINITY, TriggerSource.SELF);

        givenSetWithSkillEffect(500L, sgSkill, 1, Enhancement.NONE);

        SkillCoefficient sgCoef = sgCoefMock(sgSkill);
        when(skillCoefficientRepository.findBySetGrantedSkillIdIn(anyList())).thenReturn(List.of(sgCoef));

        DpsResponse res = service.calculate(req());

        assertThat(res.memberResults().get(0).skillResults()).hasSize(1);
    }

    @Test
    @DisplayName("세트부여스킬_TriggerSource_MERCENARY_skip")
    void 세트부여스킬_TriggerSource_MERCENARY_skip() {
        SetGrantedSkill sgSkill = sgSkillMock(700L, StatSource.SELF, TriggerSource.MERCENARY);

        givenSetWithSkillEffect(500L, sgSkill, 1, Enhancement.NONE);

        SkillCoefficient sgCoef = sgCoefMock(sgSkill);
        when(skillCoefficientRepository.findBySetGrantedSkillIdIn(anyList())).thenReturn(List.of(sgCoef));

        DpsResponse res = service.calculate(req());

        assertThat(res.memberResults().get(0).skillResults()).hasSize(1);
    }

    // ── 스킬 효과(저항깎) ─────────────────────────────────────────────────────

    @Test
    @DisplayName("아이템스킬매핑_있고_아이템스킬효과_미등록이면_용병스킬효과_fallback")
    void 아이템스킬매핑_있고_아이템스킬효과_미등록이면_용병스킬효과_fallback() {
        givenItemSkillMappingWithoutEffects();
        givenMercenarySkillEffect(StatType.RESIST_PIERCE, 80, ValueType.FLAT);

        DpsResponse res = service.calculate(req());

        assertThat(res.totalResistPierce()).isEqualTo(80);
    }

    @Test
    @DisplayName("아이템스킬효과_등록되면_용병스킬효과_미적용")
    void 아이템스킬효과_등록되면_용병스킬효과_미적용() {
        givenItemSkillMappingWithEffect(StatType.RESIST_PIERCE, 40, ValueType.FLAT);
        givenMercenarySkillEffect(StatType.RESIST_PIERCE, 80, ValueType.FLAT);

        DpsResponse res = service.calculate(req());

        assertThat(res.totalResistPierce()).isEqualTo(40);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private MercenaryCharacteristic characteristicMock(Long id) {
        MercenaryCharacteristic c = mock(MercenaryCharacteristic.class);
        when(c.getId()).thenReturn(id);
        return c;
    }

    private MercenaryCharacteristicLevel charLevelMock(MercenaryCharacteristic characteristic,
                                                        int level, StatType statType,
                                                        float amountValue, String amount) {
        MercenaryCharacteristicLevel l = mock(MercenaryCharacteristicLevel.class);
        when(l.getCharacteristic()).thenReturn(characteristic);
        when(l.getLevel()).thenReturn(level);
        when(l.getStatType()).thenReturn(statType);
        when(l.getAmountValue()).thenReturn(amountValue);
        when(l.getAmount()).thenReturn(amount);
        return l;
    }

    /** 슬롯에 세트+주술을 설정하고, 세트 일반 효과는 빈 목록으로 스텁한다. */
    private void givenSlotWithRitual(Long setId, Long ritualId, RitualOutcome outcome) {
        EquipmentSet set = mock(EquipmentSet.class);
        when(set.getId()).thenReturn(setId);
        when(item.getEquipmentSet()).thenReturn(set);

        Ritual ritual = mock(Ritual.class);
        when(ritual.getId()).thenReturn(ritualId);

        UserDeckMemberSlotRitual slotRitual = mock(UserDeckMemberSlotRitual.class);
        when(slotRitual.getRitual()).thenReturn(ritual);
        when(slotRitual.getOutcome()).thenReturn(outcome);
        when(slot.getRitual()).thenReturn(slotRitual);

        when(setEffectRepository.findBySetIdIn(anyList())).thenReturn(List.of());
    }

    private RitualSetEffect ritualSetEffectMock(Long ritualId, RitualOutcome outcome,
                                                Long setId, int requiredPieces,
                                                StatType statType, int statValue) {
        Ritual ritual = mock(Ritual.class);
        when(ritual.getId()).thenReturn(ritualId);
        EquipmentSet set = mock(EquipmentSet.class);
        when(set.getId()).thenReturn(setId);

        RitualSetEffect e = mock(RitualSetEffect.class);
        when(e.getRitual()).thenReturn(ritual);
        when(e.getOutcome()).thenReturn(outcome);
        when(e.getEquipmentSet()).thenReturn(set);
        when(e.getRequiredRitualPieces()).thenReturn(requiredPieces);
        when(e.getElement()).thenReturn(Element.NONE);
        when(e.getStatType()).thenReturn(statType);
        when(e.getStatValue()).thenReturn(statValue);
        when(e.getStatUnit()).thenReturn(StatUnit.FLAT);
        return e;
    }

    /** 슬롯 아이템에 세트+강화 설정. 세트 일반 효과는 빈 목록으로 스텁한다. */
    private void givenSetWithSkillEffect(Long setId, SetGrantedSkill sgSkill,
                                         int requiredPieces, Enhancement requiredEnh) {
        EquipmentSet set = mock(EquipmentSet.class);
        when(set.getId()).thenReturn(setId);
        when(item.getEquipmentSet()).thenReturn(set);
        when(item.getEnhancement()).thenReturn(Enhancement.FIVE);

        EquipmentSetSkillEffect skillEffect = mock(EquipmentSetSkillEffect.class);
        when(skillEffect.getEquipmentSet()).thenReturn(set);
        when(skillEffect.getRequiredPieces()).thenReturn(requiredPieces);
        when(skillEffect.getEnhancement()).thenReturn(requiredEnh);
        when(skillEffect.getSetGrantedSkill()).thenReturn(sgSkill);

        when(setEffectRepository.findBySetIdIn(anyList())).thenReturn(List.of());
        when(setSkillEffectRepository.findByEquipmentSetIdIn(anyList())).thenReturn(List.of(skillEffect));
    }

    private SetGrantedSkill sgSkillMock(Long id, StatSource statSource, TriggerSource triggerSource) {
        SetGrantedSkill sg = mock(SetGrantedSkill.class);
        when(sg.getId()).thenReturn(id);
        when(sg.getSkillName()).thenReturn("세트부여스킬");
        when(sg.getStatSource()).thenReturn(statSource);
        when(sg.getTriggerSource()).thenReturn(triggerSource);
        return sg;
    }

    /** 아이템 스킬 매핑만 있고 item_skill_effects는 비어 있는 명왕형 시나리오 */
    private void givenItemSkillMappingWithoutEffects() {
        Item catalogItem = mock(Item.class);
        when(catalogItem.getId()).thenReturn(ITEM_ID);

        ItemSkill itemSkill = mock(ItemSkill.class);
        when(itemSkill.getId()).thenReturn(900L);

        ItemSkillMapping mapping = mock(ItemSkillMapping.class);
        when(mapping.getItem()).thenReturn(catalogItem);
        when(mapping.getSkill()).thenReturn(itemSkill);

        SkillCoefficient itemCoef = mock(SkillCoefficient.class);
        when(itemCoef.isMercenarySkill()).thenReturn(false);
        when(itemCoef.isItemSkill()).thenReturn(true);
        when(itemCoef.getItemSkill()).thenReturn(itemSkill);
        when(itemCoef.getCoefStr()).thenReturn(1.0f);
        when(itemCoef.getCoefDex()).thenReturn(0.0f);
        when(itemCoef.getCoefVit()).thenReturn(0.0f);
        when(itemCoef.getCoefInt()).thenReturn(0.0f);
        when(itemCoef.getCoefAtk()).thenReturn(0.0f);
        when(itemCoef.getCoefLvl()).thenReturn(0.0f);
        when(itemCoef.getSkillType()).thenReturn(SkillType.INSTANT);
        when(itemCoef.getCastsPerSecond()).thenReturn(2.0f);
        when(itemCoef.getHitCount()).thenReturn(1);

        when(itemSkillMappingRepository.findByItemIdIn(anyList())).thenReturn(List.of(mapping));
        when(skillCoefficientRepository.findByItemSkillIdIn(anyList())).thenReturn(List.of(itemCoef));
        when(itemSkillEffectRepository.findBySkillIdIn(anyList())).thenReturn(List.of());
    }

    private void givenItemSkillMappingWithEffect(StatType statType, int value, ValueType valueType) {
        givenItemSkillMappingWithoutEffects();

        ItemSkill itemSkill = mock(ItemSkill.class);
        when(itemSkill.getId()).thenReturn(900L);

        ItemSkillEffect effect = mock(ItemSkillEffect.class);
        when(effect.getSkill()).thenReturn(itemSkill);
        when(effect.getStatKey()).thenReturn(statType);
        when(effect.getStatValue()).thenReturn(value);
        when(effect.getValueType()).thenReturn(valueType);

        when(itemSkillEffectRepository.findBySkillIdIn(anyList())).thenReturn(List.of(effect));
    }

    private void givenMercenarySkillEffect(StatType statType, int value, ValueType valueType) {
        MercenarySkill mercSkill = mock(MercenarySkill.class);
        when(mercSkill.getMercenary()).thenReturn(mercenary);

        MercenarySkillEffect effect = mock(MercenarySkillEffect.class);
        when(effect.getSkill()).thenReturn(mercSkill);
        when(effect.getStatKey()).thenReturn(statType);
        when(effect.getStatValue()).thenReturn(value);
        when(effect.getValueType()).thenReturn(valueType);

        when(mercenarySkillEffectRepository.findBySkill_MercenaryIdIn(anyList())).thenReturn(List.of(effect));
    }

    @Test
    @DisplayName("민첩_1000당_크리티컬확률_2%p_DPS_반영")
    void 민첩_크리티컬확률_DPS_반영() {
        MercenaryStat dexStat = mock(MercenaryStat.class);
        when(dexStat.getMercenary()).thenReturn(mercenary);
        when(dexStat.getStatKey()).thenReturn(StatType.DEXTERITY);
        when(dexStat.getStatValue()).thenReturn(3000);
        when(mercenaryStatRepository.findByMercenaryIdIn(anyList())).thenReturn(List.of(dexStat));

        SkillCoefficient dexCoef = mock(SkillCoefficient.class);
        MercenarySkill mercSkill = mock(MercenarySkill.class);
        when(mercSkill.getMercenary()).thenReturn(mercenary);
        when(mercSkill.getSkillName()).thenReturn("민첩스킬");
        when(dexCoef.isMercenarySkill()).thenReturn(true);
        when(dexCoef.getMercenarySkill()).thenReturn(mercSkill);
        when(dexCoef.getCoefStr()).thenReturn(0.0f);
        when(dexCoef.getCoefDex()).thenReturn(1.0f);
        when(dexCoef.getCoefVit()).thenReturn(0.0f);
        when(dexCoef.getCoefInt()).thenReturn(0.0f);
        when(dexCoef.getCoefAtk()).thenReturn(0.0f);
        when(dexCoef.getCoefLvl()).thenReturn(0.0f);
        when(dexCoef.getSkillType()).thenReturn(SkillType.INSTANT);
        when(dexCoef.getCastsPerSecond()).thenReturn(1.0f);
        when(dexCoef.getHitCount()).thenReturn(1);
        when(skillCoefficientRepository.findByMercenaryIdIn(anyList())).thenReturn(List.of(dexCoef));

        DpsResponse res = service.calculate(req());

        // 민첩 3000 + 레벨분배 2256 → 크리 10% → E[dps] = 5256 × (1 + 0.10 × 2)
        long baseWithoutCrit = 5256L;
        long expectedRaw = Math.round(baseWithoutCrit * 1.20);
        assertThat(res.memberResults().get(0).rawDps()).isEqualTo(expectedRaw);
    }

    private SkillCoefficient sgCoefMock(SetGrantedSkill sgSkill) {
        SkillCoefficient coef = mock(SkillCoefficient.class);
        when(coef.isMercenarySkill()).thenReturn(false);
        when(coef.isItemSkill()).thenReturn(false);
        when(coef.isSetGrantedSkill()).thenReturn(true);
        when(coef.getSetGrantedSkill()).thenReturn(sgSkill);
        when(coef.getCoefStr()).thenReturn(0.5f);
        when(coef.getCoefDex()).thenReturn(0.0f);
        when(coef.getCoefVit()).thenReturn(0.0f);
        when(coef.getCoefInt()).thenReturn(0.0f);
        when(coef.getCoefAtk()).thenReturn(0.0f);
        when(coef.getCoefLvl()).thenReturn(0.0f);
        when(coef.getSkillType()).thenReturn(SkillType.INSTANT);
        when(coef.getCastsPerSecond()).thenReturn(1.0f);
        when(coef.getHitCount()).thenReturn(1);
        return coef;
    }
}
