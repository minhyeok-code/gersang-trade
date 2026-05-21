# 각성 명왕(AwakenedMyungwang) 엔티티 및 계산 스펙

## 0. 이 문서의 목적

Claude Code가 이 문서만 읽고 아래를 구현할 수 있도록 작성되었다.

1. 각성 명왕 특성 데이터 시딩
2. 스탯 이전 계산 서비스 (`AwakenedMyungwangBuffCalculator`)
3. `MercenaryType.AWAKENED_MYUNGWANG` 적용 확인

### 데이터 적재 방식

**크롤링 없음.** 4마리 데이터를 `DataInitializer`로 직접 시딩.

---

## 1. 도메인 개요

```
각성 명왕 4마리 (현재 출시):
  각성 항삼세명왕  (화속성, FIRE)
  각성 군다리명왕  (뇌속성, THUNDER)
  각성 대위덕명왕  (풍속성, WIND)
  각성 금강야차명왕 (수속성, WATER)

※ 각성 부동명왕: 미출시 (추후 데이터 추가 필요)
```

---

## 2. 각성 명왕 공통 고정 버프

각성 명왕 4마리 전원에게 동일하게 적용되는 버프.
**엔티티로 저장하지 않고 계산기에서 하드코딩으로 처리한다.**

```
1. 자신 속성값 +15 (SELF_AUTO)
   → 해당 명왕 본인의 ELEMENT_VALUE +15

2. 아군 전체 속성값 +5 (ALLY_AUTO, 계산기 하드코딩)
   → ADAPTIVE: 모든 아군 속성값 +5
   → 단, EARTH 속성 아군은 +2 (땅속성 정책)
```

---

## 3. 스탯 이전 메커니즘 (일반 명왕과 차이점)

### 3-1. 이전 대상 기준 변경

```
일반 명왕: 같은 속성 아군 우선
각성 명왕: 해당 스탯이 가장 높은 아군 우선 (속성 무관)

이전 대상 우선순위 (동일):
  1순위: 해당 스탯이 가장 높은 사천왕
  2순위: 없으면 해당 스탯이 가장 높은 주인공
  3순위: 없으면 해당 스탯이 가장 높은 전설장수
  4순위: 없으면 이전 X
```

### 3-2. 이전량 계산 공식

```
이전량 = 명왕의 해당 스탯 × (기본 이전율 10% + 특성 레벨 이전율%)
→ 이전 대상의 해당 스탯에 가산

예: 각성 항삼세명왕 힘=1100, 특성 레벨5(+30%)
  → 이전량 = 1100 × (10 + 30)% = 440
  → 힘이 가장 높은 사천왕의 힘에 +440 가산
```

### 3-3. 각성 명왕별 이전 스탯 및 기본 이전율

```
각성 항삼세명왕  (FIRE):    STRENGTH  기본 10%
각성 군다리명왕  (THUNDER): DEXTERITY 기본 10%
각성 대위덕명왕  (WIND):    VITALITY  기본 10%
각성 금강야차명왕 (WATER):  INTELLECT 기본 10%
```

> 일반 명왕 대위덕/금강야차의 기본 이전율 5%에서 10%로 상향됨.

---

## 4. 계산 서비스

### AwakenedMyungwangBuffCalculator

```java
@Service
public class AwakenedMyungwangBuffCalculator {

    /**
     * 각성 명왕 공통 아군 버프를 반환한다.
     * MercenaryType == AWAKENED_MYUNGWANG이면 자동 적용.
     *
     * ADAPTIVE: 모든 아군 속성값 +5
     * EARTH 속성 아군: +2 (땅속성 정책)
     */
    public SpiritBuff getCommonAllyBuff() {
        return new AllyElementBuff(5f, 2f); // 일반 +5, EARTH +2
    }

    public record AllyElementBuff(float defaultValue, float earthValue) {}

    /**
     * 스탯 이전량을 계산한다.
     *
     * @param myungwang          각성 명왕 용병
     * @param characteristicLevel 특성 레벨 (0~5)
     * @param myungwangStats     명왕의 현재 스탯 Map
     * @return 이전량 Map (StatType → 이전 수치). 이전 대상 없으면 빈 Map.
     */
    public Map<StatType, Float> calculateTransfer(
            Mercenary myungwang,
            int characteristicLevel,
            Map<StatType, Float> myungwangStats) {

        TransferConfig config = getConfig(myungwang.getNature());
        if (config == null) return Map.of();

        float additionalRate = getAdditionalRate(myungwang, characteristicLevel);
        float totalRate = (10f + additionalRate) / 100f;

        float statValue = myungwangStats.getOrDefault(config.statType(), 0f);
        float transferAmount = statValue * totalRate;

        return Map.of(config.statType(), transferAmount);
    }

    private TransferConfig getConfig(Nature nature) {
        return switch (nature) {
            case FIRE    -> new TransferConfig(StatType.STRENGTH);
            case THUNDER -> new TransferConfig(StatType.DEXTERITY);
            case WIND    -> new TransferConfig(StatType.VITALITY);
            case WATER   -> new TransferConfig(StatType.INTELLECT);
            default      -> null;
        };
    }

    private float getAdditionalRate(Mercenary myungwang, int level) {
        return myungwang.getCharacteristics().stream()
            .flatMap(c -> c.getLevels().stream())
            .filter(l -> l.getLabel() != null && l.getLabel().contains("이전율"))
            .filter(l -> l.getLevel() <= level)
            .map(MercenaryCharacteristicLevel::getAmountValue)
            .reduce(0f, Float::sum);
    }

    public record TransferConfig(StatType statType) {}
}
```

---

## 5. 전체 데이터

버프 컬럼 순서: `특성명 / point / applyType / required / label / statType / level별 value`

### 5-1. 각성 항삼세명왕 (FIRE)

```
각성 특성 (applyType=SELF_AUTO): 자신 ELEMENT_VALUE FIRE +15
공통 아군 버프: 계산기 하드코딩 (모든 아군 속성값 +5, EARTH +2)

하위 / 파괴 / point=1 / applyType=NORMAL / required=null
  label: 힘 이전율 → STRENGTH 이전 특성
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 3/ 7/10/30%

상위 / 잠식 / point=2 / applyType=NORMAL / required=파괴
  label: 받는 데미지 증가 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 1/ 1/ 2/ 2%
  label: 중첩            → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 0/ 2/ 3/ 4/ 5

하위 / 화신 / point=1 / applyType=NORMAL / required=null
  label: 화무신 생명력 증가율  → statType=null (소환수)
    level:  1/ 2/ 3/ 4/  5
    value:  5/15/30/50/100%
  label: 화무신 피해감소 증가율 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 2/ 5/10/20%

상위 / 어령 / point=2 / applyType=NORMAL / required=화신
  label: 화무신 염랑 데미지 증가 → statType=null
    level:  1/ 2/ 3/  4/  5
    value: 10/30/50/150/300%
```

### 5-2. 각성 군다리명왕 (THUNDER)

```
각성 특성 (applyType=SELF_AUTO): 자신 ELEMENT_VALUE THUNDER +15
공통 아군 버프: 계산기 하드코딩

하위 / 신속 / point=1 / applyType=NORMAL / required=null
  label: 민첩성 이전율 → DEXTERITY 이전 특성
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 3/ 7/15/30%

상위 / 쇄도 / point=2 / applyType=NORMAL / required=신속
  label: 아군 데미지 증가율 → DAMAGE_PERCENT PERCENT_ADD ALLY (항상 적용 가정)
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 2/ 3/ 5/ 8%

하위 / 우뢰 / point=1 / applyType=NORMAL / required=null
  label: 데미지 증가         → statType=null (본인 스킬)
    level:  1/ 2/  3/  4/  5
    value: 50/70/100/150/200%
  label: 저항 감소 지속시간  → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 2/ 3/ 4/ 5초

상위 / 낙인 / point=2 / applyType=NORMAL / required=우뢰
  label: 거리비례 데미지 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 3/ 5/ 7/10%
  label: 치명타 피해량  → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 3/ 5/ 7/10%
```

### 5-3. 각성 대위덕명왕 (WIND)

```
각성 특성 (applyType=SELF_AUTO): 자신 ELEMENT_VALUE WIND +15
공통 아군 버프: 계산기 하드코딩

하위 / 끈기 / point=1 / applyType=NORMAL / required=null
  label: 생명력 이전율 → VITALITY 이전 특성
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 3/ 7/10/30%

상위 / 영속 / point=2 / applyType=NORMAL / required=끈기
  label: 공격속도 감소  → ATTACK_SPEED PERCENT_ADD ENEMY (음수, 항상 적용 가정)
    level:  1/  2/  3/  4/  5
    value: -2/ -3/ -5/ -7/-10%
  label: 이동속도 감소  → MOVE_SPEED PERCENT_ADD ENEMY (음수, 항상 적용 가정)
    level:  1/  2/  3/  4/  5
    value: -2/ -3/ -5/-10/-20%
  label: 타격저항 감소  → HITTING_RESISTANCE PERCENT_ADD ENEMY (음수)
    level:  1/  2/  3/  4/  5
    value: -1/ -2/ -3/ -4/ -5%

하위 / 돌풍 / point=1 / applyType=NORMAL / required=null
  label: 흡풍멸진 데미지 → statType=null (본인 스킬)
    level:  1/ 2/  3/  4/  5
    value: 50/70/100/150/200%
  label: 범위 증가      → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 0/ 1/ 1/ 2/ 2칸

상위 / 분천 / point=2 / applyType=NORMAL / required=돌풍
  label: 시전속도   → statType=null
    level:  1/ 2/ 3/ 4/ 5
    value:  2/ 3/10/30/50%
  label: 폭발 데미지 → statType=null
    level:  1/ 2/ 3/ 4/  5
    value: 10/30/50/70/150%

※ 분천 5레벨: 적 속성값 -5 (오픈 이슈 — 아이템 효과와 동일 처리 필요)
```

### 5-4. 각성 금강야차명왕 (WATER)

```
각성 특성 (applyType=SELF_AUTO): 자신 ELEMENT_VALUE WATER +15
공통 아군 버프: 계산기 하드코딩

하위 / 신비 / point=1 / applyType=NORMAL / required=null
  label: 지력 이전율 → INTELLECT 이전 특성
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 3/ 7/15/30%

상위 / 영벽 / point=2 / applyType=NORMAL / required=신비
  label: 피해 감소   → statType=null (본인 방어)
    level:  1/ 2/ 3/ 4/ 5
    value:  1/ 2/ 5/ 7/10%
  label: 마법력 증가 → statType=null
    level:  1/ 2/ 3/ 4/  5
    value: 10/20/35/50/100%

하위 / 가호 / point=1 / applyType=NORMAL / required=null
  label: 데미지 증가    → statType=null (본인 스킬)
    level:  1/ 2/  3/  4/  5
    value: 50/70/100/150/200%
  label: 피해감소율 증가 → statType=null (본인 방어)
    level:  1/ 2/ 3/ 4/ 5
    value:  2/ 5/10/15/30%

상위 / 격노 / point=2 / applyType=NORMAL / required=가호
  label: 사거리 증가    → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 1/ 1/ 2/ 2칸
  label: 중첩 당 데미지 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 1/ 2/ 2/ 3%
  label: 최대 중첩 횟수 → statType=null
    level:  1/ 2/ 3/ 4/ 5
    value: 10/20/30/40/50
  label: 피격 횟수      → statType=null
    level:  1/ 2/ 3/ 4/ 5
    value: 20/15/10/10/ 5
```

---

## 6. 주의사항

```
1. 각성 명왕 공통 아군 버프 (속성값 +5/+2)는
   AwakenedMyungwangBuffCalculator.getCommonAllyBuff()에서 하드코딩 처리
   엔티티로 저장하지 않음

2. 스탯 이전 대상: 해당 스탯이 가장 높은 아군 (속성 무관)
   우선순위: 사천왕 → 주인공 → 전설장수

3. 각성 명왕 기본 이전율: 전원 10% (일반 대위덕/금강야차 5%에서 상향)

4. 대위덕 분천 5레벨 적 속성값 -5 효과는 MVP 제외
   → 오픈 이슈로 기록, 아이템 특수 효과와 함께 추후 처리

5. MercenaryType = AWAKENED_MYUNGWANG 으로 저장

6. CharacteristicApplyType:
   NORMAL    → 일반 포인트 배분 특성
   SELF_AUTO → 각성 특성 (자신 속성값 +15)
   ALLY_AUTO → 주인공 국적 버프 등
```