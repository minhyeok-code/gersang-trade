# 각성 사천왕(AwakenedHeavenlyKing) 엔티티 및 계산 스펙

## 0. 이 문서의 목적

Claude Code가 이 문서만 읽고 아래를 구현할 수 있도록 작성되었다.

1. 각성 사천왕 특성 데이터 시딩 (기존 `MercenaryCharacteristic` + `MercenaryCharacteristicLevel` 재사용)
2. 각성 특성(속성값 +20) 처리
3. `MercenaryType.AWAKENED_HEAVENLY_KING` 적용 확인

### 데이터 적재 방식

**크롤링 없음.** 4마리 데이터를 `DataInitializer`로 직접 시딩.

---

## 1. 도메인 개요

```
각성 사천왕 4마리:
  각성 지국천왕 (화속성, FIRE)
  각성 광목천왕 (풍속성, WIND)
  각성 증장천왕 (뇌속성, THUNDER)
  각성 다문천왕 (수속성, WATER)

특성 구조:
  하위 특성 2개: point=1, 포인트 1개당 레벨 1, 최대 레벨 5
  상위 특성 2개: point=2, 포인트 2개당 레벨 1, 최대 레벨 5 (하위 선행 필요)
  각성 특성 1개: point=null, level=null → 별도 처리 (아래 참고)

특성 포인트:
  일반 사천왕과 동일: 총 17포인트
```

---

## 2. 각성 특성 처리

```
각성 특성 (awakening_characteristic):
  point=null, level=null → MercenaryCharacteristic 저장은 하되
  별도 컬럼 또는 플래그로 "각성 특성 장착 여부" 관리

각성 특성 효과:
  각성 지국천왕: 자신의 ELEMENT_VALUE(FIRE)   +20 영구 적용
  각성 광목천왕: 자신의 ELEMENT_VALUE(WIND)   +20 영구 적용
  각성 증장천왕: 자신의 ELEMENT_VALUE(THUNDER) +20 영구 적용
  각성 다문천왕: 자신의 ELEMENT_VALUE(WATER)   +20 영구 적용

적용 대상: 해당 용병 자신에게만 (덱 전체 아님)
계산기에서: 각성 사천왕 장착 시 해당 용병 ELEMENT_VALUE에 +20 가산
```

---

## 3. StatType 매핑

```
마법저항력 감소    → MAGIC_RESISTANCE   PERCENT_ADD ENEMY (음수)
타격저항력 감소    → HITTING_RESISTANCE PERCENT_ADD ENEMY (음수)
공중 몬스터 마법저항 감소 → MAGIC_RESISTANCE PERCENT_ADD ENEMY (음수, 항상 적용 가정)
본인 스킬 강화     → statType=null
소환수 강화        → statType=null
```

---

## 4. 전체 데이터

버프 컬럼 순서: `특성명 / point / required / label / statType / valueType / target / level별 value`

### 4-1. 각성 지국천왕 (FIRE)

```
각성 특성: ELEMENT_VALUE FIRE FLAT 자신 +20

하위 / 겁화 / point=1 / required=null
  label: 염화무극진 데미지 → statType=null
    level: 1/ 2/ 3/  4/  5
    value:20/40/70/100/150%

상위 / 경안 / point=2 / required=겁화
  label: 데미지 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 5/10/15/20/40%
  label: 기절   → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 4/ 6/ 8/10%

하위 / 화상 / point=1 / required=null
  label: 화상 데미지 → statType=null
    level:  1/ 2/  3/  4/  5
    value: 20/50/100/150/200%
  label: 마법저항력  → MAGIC_RESISTANCE PERCENT_ADD ENEMY (음수)
    level:  1/ 2/ 3/ 4/ 5
    value: -1/-3/-6/-10/-15%

상위 / 층화 / point=2 / required=화상
  label: 중첩 데미지 증가율 → statType=null
    level:  1/ 2/ 3/ 4/  5
    value:  2/ 5/20/50/100%
  label: 지속시간           → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 3/ 5/ 7/10초
```

### 4-2. 각성 광목천왕 (WIND)

```
각성 특성: ELEMENT_VALUE WIND FLAT 자신 +20

하위 / 광풍 / point=1 / required=null
  label: 풍극진멸 데미지 → statType=null
    level: 1/ 2/ 3/  4/  5
    value:20/40/70/100/150%

상위 / 연파 / point=2 / required=광풍
  label: 생명력 증가    → statType=null (자신 방어)
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 5/10/15/30%
  label: 고정 추가 피해 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 3/ 8/15/25/50%

하위 / 충돌 / point=1 / required=null
  label: 타격저항력 → HITTING_RESISTANCE PERCENT_ADD ENEMY (음수)
    level:  1/ 2/ 3/ 4/ 5
    value: -1/-3/-6/-10/-15%

상위 / 신보 / point=2 / required=충돌
  label: 기본 데미지 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 5/10/20/40%
  label: 추가 데미지 → statType=null
    level:  1/ 2/ 3/  4/  5
    value: 10/25/50/150/200%
```

### 4-3. 각성 증장천왕 (THUNDER)

```
각성 특성: ELEMENT_VALUE THUNDER FLAT 자신 +20

하위 / 감전 / point=1 / required=null
  label: 천궁뇌격 데미지 → statType=null
    level: 1/ 2/ 3/  4/  5
    value:20/50/70/100/150%

상위 / 전폭 / point=2 / required=감전
  label: 전격 확률   → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 3/ 5/15/50%
  label: 전폭 피해   → statType=null
    level:  1/  2/  3/  4/  5
    value:100/150/200/300/400%
  label: 주변 데미지 → statType=null
    level: 1/ 2/ 3/  4/  5
    value:50/75/100/150/200%

하위 / 충격파 / point=1 / required=null
  label: 3배 피해 확률         → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 4/10/20/40/70%
  label: 마법저항 (공중 몬스터) → MAGIC_RESISTANCE PERCENT_ADD ENEMY (음수, 항상 적용 가정)
    level:  1/ 2/ 3/ 4/ 5
    value: -1/-3/-6/-10/-15%

상위 / 원격 / point=2 / required=충격파
  label: 치명타 피해 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 3/ 5/20/50%
```

### 4-4. 각성 다문천왕 (WATER)

```
각성 특성: ELEMENT_VALUE WATER FLAT 자신 +20

하위 / 강화 / point=1 / required=null
  label: 소환수 스탯 → statType=null (소환수 강화)
    level:  1/ 2/ 3/  4/  5
    value: 20/40/70/100/150%
  label: 마법저항력  → MAGIC_RESISTANCE PERCENT_ADD ENEMY (음수)
    level:  1/ 2/ 3/ 4/ 5
    value: -1/-3/-6/-10/-15%

상위 / 안식 / point=2 / required=강화
  label: 청빙격류 데미지 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 3/ 5/30/80%
  label: 피해            → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 2/ 3/ 7/10%

하위 / 집중 / point=1 / required=null
  label: 청빙격류 데미지 → statType=null
    level:  1/ 2/ 3/  4/  5
    value: 20/50/70/100/150%

상위 / 전심 / point=2 / required=집중
  label: 데미지 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 3/ 5/10/25%
```

---

## 5. 주의사항

```
1. 각성 특성(awakening_characteristic)은 point/level이 null
   → MercenaryCharacteristic에 저장하되 별도 플래그(isAwakening=true)로 구분
   → 계산기에서 각성 사천왕 장착 시 해당 용병 ELEMENT_VALUE +20 자동 적용

2. 각성 증장천왕 충격파의 마법저항 감소는 "공중 몬스터 한정"이나
   항상 적용 가정으로 처리

3. 각성 다문천왕 강화 특성이 덱버프(마법저항 감소)와 소환수 강화 두 개를 동시에 가짐
   → label 컬럼으로 구분, MAGIC_RESISTANCE label만 statType 매핑

4. MercenaryType = AWAKENED_HEAVENLY_KING 으로 저장
```