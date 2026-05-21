# 일반 사천왕(HeavenlyKing) 엔티티 및 계산 스펙

## 0. 이 문서의 목적

Claude Code가 이 문서만 읽고 아래를 구현할 수 있도록 작성되었다.

1. 일반 사천왕 특성 데이터 시딩 (기존 `MercenaryCharacteristic` + `MercenaryCharacteristicLevel` 재사용)
2. `MercenaryType.HEAVENLY_KING` 적용 확인

### 데이터 적재 방식

**크롤링 없음.** 4마리 데이터를 `DataInitializer`로 직접 시딩.

---

## 1. 도메인 개요

```
일반 사천왕 4마리:
  지국천왕 (화속성, FIRE)
  광목천왕 (풍속성, WIND)
  증장천왕 (뇌속성, THUNDER)
  다문천왕 (수속성, WATER)

특성 구조:
  특성 2개, 모두 point=1, required=null (선행 없음)
  레벨 1~10, 포인트 1개당 레벨 1
  전설장수와 동일한 MercenaryCharacteristic 구조 재사용

특성 포인트:
  전설장수와 동일: 총 17포인트
  레벨 1에 1개, 20레벨마다 1개(~200), 200~260은 10레벨마다 1개
```

---

## 2. 덱버프 StatType 매핑

```
마법저항력 감소 → MAGIC_RESISTANCE   PERCENT_ADD ENEMY (음수)
타격저항력 감소 → HITTING_RESISTANCE PERCENT_ADD ENEMY (음수)
본인 스킬 강화  → statType=null (계산기 스킵)
소환수 강화     → statType=null (계산기 스킵)
```

---

## 3. 전체 데이터

버프 컬럼 순서: `특성명 / point / label / statType / element / valueType / target / level별 value`

### 3-1. 지국천왕 (FIRE)

```
특성1 / 겁화 / point=1 / required=null
  label: 염룡살진 데미지 → statType=null
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
    value: 10/20/30/40/50/60/75/95/120/150%

특성2 / 화상 / point=1 / required=null
  label: 화상 데미지 → statType=null
    level:  1/ 2/ 3/  4/  5/  6/  7/  8/  9/  10
    value: 20/40/60/ 80/100/120/140/160/180/ 200%

  label: 마법 저항력 → MAGIC_RESISTANCE PERCENT_ADD ENEMY (음수)
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/ 10
    value: -1/-2/-3/-4/-5/-6/-7/-9/-12/-15%
```

### 3-2. 광목천왕 (WIND)

```
특성1 / 광풍 / point=1 / required=null
  label: 풍룡섬 데미지 → statType=null
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
    value: 10/20/30/40/50/60/75/95/120/150%

특성2 / 충돌 / point=1 / required=null
  label: 타격 저항력 → HITTING_RESISTANCE PERCENT_ADD ENEMY (음수)
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/ 10
    value: -1/-2/-3/-4/-5/-6/-7/-9/-12/-15%
```

### 3-3. 증장천왕 (THUNDER)

```
특성1 / 감전 / point=1 / required=null
  label: 뇌룡격 데미지 → statType=null
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
    value: 10/20/30/40/50/60/75/95/120/150%

특성2 / 충격파 / point=1 / required=null
  label: 3배 피해 확률 → statType=null
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/ 10
    value:  4/ 8/12/16/20/24/30/40/55/70%
```

### 3-4. 다문천왕 (WATER)

```
특성1 / 강화 / point=1 / required=null
  label: 흑귀 능력치 → statType=null (소환수 강화)
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
    value: 10/20/30/40/50/60/75/95/120/150%

특성2 / 집중 / point=1 / required=null
  label: 눈사태 데미지 → statType=null
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
    value: 10/20/30/40/50/60/75/95/120/150%
```

---

## 4. 주의사항

```
1. 지국천왕 화상 특성은 label이 2개 (화상 데미지 + 마법저항력)
   → MercenaryCharacteristicLevel 행이 레벨당 2개 생성됨
   → label 컬럼으로 구분

2. 증장천왕/다문천왕은 덱버프 없음
   → 특성 저장은 하되 계산기에서 전부 스킵

3. MercenaryType = HEAVENLY_KING 으로 저장
```