# 명왕(Myungwang) 엔티티 및 계산 스펙

## 0. 이 문서의 목적

Claude Code가 이 문서만 읽고 아래를 구현할 수 있도록 작성되었다.

1. 명왕 특성 데이터 시딩
2. 스탯 이전 계산 서비스 (`MyungwangStatTransferCalculator`)
3. `CharacteristicApplyType` enum 추가

### 데이터 적재 방식

**크롤링 없음.** 5마리 데이터를 `DataInitializer`로 직접 시딩.

---

## 1. 도메인 개요

```
명왕 5마리:
  항삼세명왕  (화속성, FIRE)
  군다리명왕  (뇌속성, THUNDER)
  대위덕명왕  (풍속성, WIND)
  부동명왕    (토속성, EARTH)
  금강야차명왕 (수속성, WATER)

핵심 메커니즘: 스탯 이전
  명왕의 특정 스탯을 일정 비율로 다른 유닛에게 이전
  기본 이전율 + 특성 레벨 이전율 합산
```

---

## 2. CharacteristicApplyType enum (신규)

기존 `MercenaryCharacteristic`에 `applyType` 컬럼 추가.

```java
package org.example.gersangtrade.domain.catalog.enums;

public enum CharacteristicApplyType {
    NORMAL,     // 포인트 배분형 (일반 특성)
    SELF_AUTO,  // 자동 적용, 자신에게만 (각성 사천왕 각성 특성)
    ALLY_AUTO,  // 자동 적용, 조건부 아군에게 (주인공 국적 버프)
}
```

> **MercenaryCharacteristic 수정 사항:**
> - 기존 `isAwakening: boolean` 제거 (추가하지 않음)
> - `applyType: CharacteristicApplyType` 컬럼 추가
> - 기본값: `NORMAL`

---

## 3. 스탯 이전 메커니즘

### 3-1. 이전 대상 우선순위

명왕의 속성과 같은 속성을 가진 유닛을 아래 순서로 탐색:

```
1순위: 같은 속성 사천왕 (일반/각성 모두 포함)
2순위: 같은 속성 주인공 (2차전직 국적 속성 기준)
3순위: 같은 속성 전설장수
4순위: 없으면 이전 X
```

### 3-2. 이전량 계산 공식

```
이전량 = 명왕의 해당 스탯 × (기본 이전율% + 특성 레벨 이전율%)

예: 항삼세명왕 힘=900, 기본=10%, 특성 레벨10 추가=15%
  → 이전량 = 900 × (10 + 15)% = 225
  → 이전 대상의 힘에 +225 가산
```

### 3-3. 명왕별 기본 이전율

```
항삼세명왕  (FIRE):    힘(STRENGTH)  기본 이전율 10%
군다리명왕  (THUNDER): 민첩(DEXTERITY) 기본 이전율 10%
대위덕명왕  (WIND):    생명(VITALITY)  기본 이전율 5%
금강야차명왕 (WATER):  지력(INTELLECT) 기본 이전율 5%
부동명왕    (EARTH):   스탯 이전 없음
```

---

## 4. 계산 서비스

### MyungwangStatTransferCalculator

```java
package org.example.gersangtrade.domain.calculator.service;

/**
 * 명왕 스탯 이전을 계산한다.
 *
 * 이전 대상 우선순위:
 *   1. 같은 속성 사천왕
 *   2. 같은 속성 주인공
 *   3. 같은 속성 전설장수
 *   4. 없으면 이전 X (반환값 0)
 *
 * 부동명왕(EARTH)은 스탯 이전 없음 → 빈 Map 반환
 */
@Service
public class MyungwangStatTransferCalculator {

    /**
     * 명왕 스탯 이전량을 계산한다.
     *
     * @param myungwang        명왕 용병
     * @param characteristicLevel 특성 레벨 배분 포인트 (0~10)
     * @param myungwangStats   명왕의 현재 스탯 Map
     * @return 이전량 Map (StatType → 이전 수치). 이전 대상 없으면 빈 Map.
     */
    public Map<StatType, Float> calculate(
            Mercenary myungwang,
            int characteristicLevel,
            Map<StatType, Float> myungwangStats) {

        TransferConfig config = getConfig(myungwang.getNature());
        if (config == null) return Map.of(); // 부동명왕 등 이전 없음

        float baseRate = config.baseRate();
        float additionalRate = getAdditionalRate(myungwang, characteristicLevel);
        float totalRate = (baseRate + additionalRate) / 100f;

        float statValue = myungwangStats.getOrDefault(config.statType(), 0f);
        float transferAmount = statValue * totalRate;

        return Map.of(config.statType(), transferAmount);
    }

    private TransferConfig getConfig(Nature nature) {
        return switch (nature) {
            case FIRE    -> new TransferConfig(StatType.STRENGTH,  10f);
            case THUNDER -> new TransferConfig(StatType.DEXTERITY, 10f);
            case WIND    -> new TransferConfig(StatType.VITALITY,   5f);
            case WATER   -> new TransferConfig(StatType.INTELLECT,  5f);
            case EARTH   -> null; // 부동명왕 이전 없음
            default      -> null;
        };
    }

    private float getAdditionalRate(Mercenary myungwang, int level) {
        // MercenaryCharacteristicLevel에서 스탯이전 label 행 조회
        // level번째 누적 합산
        return myungwang.getCharacteristics().stream()
            .flatMap(c -> c.getLevels().stream())
            .filter(l -> isTransferLabel(l.getLabel()))
            .filter(l -> l.getLevel() <= level)
            .map(l -> l.getAmountValue())
            .reduce(0f, Float::sum);
    }

    private boolean isTransferLabel(String label) {
        return label != null && label.contains("이전");
    }

    public record TransferConfig(StatType statType, float baseRate) {}
}
```

---

## 5. 전체 데이터

### 5-1. StatType 매핑

```
"이전되는 힘"     → STRENGTH,   이전 특성
"이전되는 민첩"   → DEXTERITY,  이전 특성
"이전되는 생명력" → VITALITY,   이전 특성
"이전되는 지력"   → INTELLECT,  이전 특성
본인 스킬 강화    → statType=null
```

### 5-2. 항삼세명왕 (FIRE)

```
기본 이전: STRENGTH 10%

특성1 / 파괴 / point=1 / applyType=NORMAL / required=null
  label: 이전되는 힘 → STRENGTH 이전 특성
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
    value:  1/ 2/ 3/ 4/ 5/ 6/ 8/10/12/15%

특성2 / 화신 / point=1 / applyType=NORMAL / required=null
  label: 힘, 생명력 → statType=null (소환수 강화)
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/ 10
    value: 10/20/30/40/50/60/70/80/90/100%
  label: 흡수 피해  → statType=null
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
    value:  1/ 2/ 3/ 4/ 5/ 6/ 8/10/12/15%
```

### 5-3. 군다리명왕 (THUNDER)

```
기본 이전: DEXTERITY 10%

특성1 / 신속 / point=1 / applyType=NORMAL / required=null
  label: 이전되는 민첩 → DEXTERITY 이전 특성
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/ 10
    value:  2/ 4/ 6/ 8/10/12/15/18/21/ 25%

특성2 / 우뢰 / point=1 / applyType=NORMAL / required=null
  label: 우뢰 데미지  → statType=null
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/ 10
    value: 10/20/30/40/50/60/70/80/90/100%
  label: 유지시간     → statType=null
    level: 1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
    value: 1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10초
```

### 5-4. 대위덕명왕 (WIND)

```
기본 이전: VITALITY 5%

특성1 / 끈기 / point=1 / applyType=NORMAL / required=null
  label: 이전되는 생명력 → VITALITY 이전 특성
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
    value:  1/ 2/ 3/ 4/ 5/ 6/ 8/10/12/15%

특성2 / 돌풍 / point=1 / applyType=NORMAL / required=null
  label: 돌풍 데미지  → statType=null
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/ 10
    value: 10/20/30/40/50/60/70/80/90/100%
  label: 회오리 범위  → statType=null
    level: 1/2/3/4/5/6/7/8/9/10
    value: 1/1/1/2/2/3/3/4/4/ 5칸
```

### 5-5. 부동명왕 (EARTH)

```
기본 이전: 없음

특성1 / 집중 / point=1 / applyType=NORMAL / required=null
  label: 지속시간 → statType=null
    level: 1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
    value: 1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10초

특성2 / 압박 / point=1 / applyType=NORMAL / required=null
  label: 상아감옥 데미지 → statType=null
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/ 10
    value: 10/20/30/40/50/60/70/80/90/100%
  label: 속박 확률      → statType=null
    level: 1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
    value: 1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10%
```

### 5-6. 금강야차명왕 (WATER)

```
기본 이전: INTELLECT 5%

특성1 / 신비 / point=1 / applyType=NORMAL / required=null
  label: 이전되는 지력 → INTELLECT 이전 특성
    level: 1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
    value: 1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10%
  label: 회복 마법력   → statType=null
    level: 1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
    value: 1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10%

특성2 / 가호 / point=1 / applyType=NORMAL / required=null
  label: 냉기 데미지   → statType=null
    level:  1/ 2/ 3/ 4/ 5/ 6/  7/  8/  9/  10
    value: 20/40/60/80/100/120/140/160/180/200%
  label: 데미지 흡수율 → statType=null
    level: 1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/10
    value: 5/10/15/20/25/30/35/40/45/50%
```

---

## 6. 주의사항

```
1. 부동명왕은 스탯 이전 없음 → MyungwangStatTransferCalculator에서 빈 Map 반환

2. 스탯 이전 계산 순서:
   명왕 스탯 확정 → 이전율 계산 → 이전 대상 탐색 → 이전 대상 스탯에 가산

3. 이전 대상이 없으면 (4순위까지 없음) 이전량 = 0

4. MercenaryType = MYUNGWANG 으로 저장

5. CharacteristicApplyType 추가로 MercenaryCharacteristic 엔티티 수정 필요:
   applyType: CharacteristicApplyType (기본값 NORMAL)
   이 컬럼은 각성 사천왕 각성 특성(SELF_AUTO), 주인공 국적 버프(ALLY_AUTO)에도 사용됨
```