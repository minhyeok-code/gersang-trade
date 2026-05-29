# 속성값(ELEMENT_VALUE) 백엔드 구현 정리

> 프론트 덱·DPS UI에서 속성값이 “이상하게” 보일 때 참고하는 백엔드 동작 문서.  
> **최종 업데이트**: 2026-05-24

---

## 1. 개념 정리

| 개념 | 저장 위치 | DPS 계산에 사용 |
|------|-----------|-----------------|
| **용병 속성** | `Mercenary.nature` (화/수/뇌/풍/토/NONE) | 속성 필터, 국가 버프, 명왕 이전 대상 판별 |
| **기본 속성값** | `MercenaryStat` (`StatType.ELEMENT_VALUE`) | ✅ base 스탯 |
| **속성값 컬럼** | `Mercenary.natureValue` | ❌ 계산 미사용 (API 표시·편의 필드) |
| **속성깎** | `MercenaryStat` / 장비 `ELEMENT_PIERCE` | 몬스터 `effectiveMonsterElement` 감소 |

- `Nature`(용병)와 `Element`(장비·버프)는 **enum 이름이 같으면** 매칭 (`FIRE` ↔ `FIRE`).
- DPS 응답 `memberResults[].elementValue` = 멤버별 최종 `ELEMENT_VALUE` 유효 스탯.
- 프론트 덱 페이지는 이 DPS 값을 용병 카드 “속성값”으로 표시한다.

---

## 2. DPS에서 멤버별 속성값 산출 흐름

**진입점**: `DpsCalculatorService.calculate()`

```
MercenaryStat(ELEMENT_VALUE)  ─┐
SELF 특성 / 장비 / 세트       ─┼─► (base + selfFlat + partyFlat) × (1 + percent%)
ALLY 특성 / 장비 / 정령 / 진법 ─┘
         │
         ▼
주인공 국가 버프 (+n, 동속성만)
         │
         ▼
각성 명왕 공통 버프 (전원 +5, 토 +2 추가)
         │
         ▼
명왕 스탯 이전 (STR/DEX/VIT/INT — 속성값 아님)
         │
         ▼
memberElementValue → ElementBonusCalculator → Adjust DPS
```

**핵심 코드**

```java
// back/.../calculator/service/DpsCalculatorService.java
// 최종 유효 스탯 = (기본 + selfFlat + partyFlat) × (1 + percent/100)
applyProtagonistNationBuff(members, member, effectiveStats);
applyAwakenedMyeongwangAllyBuff(members, member, effectiveStats);
// 이후 명왕 이전 merge → effectiveStatsByMemberId
int memberElementValue = stats.getOrDefault(StatType.ELEMENT_VALUE, 0);
```

---

## 3. 소스별 상세

### 3-1. 용병 기본 속성값

| 항목 | 내용 |
|------|------|
| 로드 | `mercenaryStatRepository.findByMercenaryIdIn()` → `StatType.ELEMENT_VALUE`만 base로 사용 |
| 미사용 | `Mercenary.natureValue` 컬럼은 DPS·스탯 합산에 **포함되지 않음** |
| 적재 | 크롤링(gerniverse/거상짱) 또는 시더 → `mercenary_stats` |

**주의**: 목록 API의 `elementValue`(= `natureValue`)와 DPS base가 DB 불일치 시 숫자가 다르게 보일 수 있음.

---

### 3-2. 특성 (Characteristic)

**판별**: `CharacteristicScopeResolver.resolve()`

| `ELEMENT_VALUE` 부호 | scope | DPS 처리 |
|---------------------|-------|----------|
| 양수 | SELF | 본인 `selfFlat` 가산 |
| 음수 | ENEMY | `enemyElementDebuff` (몬스터 속성값 감소) |
| 이전율 label (`이전율`, `이전되는`) | SKIP | flat 가산 안 함 → `MyungwangStatTransferCalculator`에서만 사용 |

---

### 3-3. 각성 특성 `SELF_AUTO` (+15 / +20) — ❌ 미구현

각성 명왕·각성 사천왕의 “각성” 특성:

- `CharacteristicApplyType.SELF_AUTO`
- `MercenaryCharacteristicLevel` 행 **없음** (시더 주석만 존재)
  - 각성 명왕: 자신 `ELEMENT_VALUE` +15
  - 각성 사천왕: 자신 `ELEMENT_VALUE` +20

**DPS 파이프라인에 `SELF_AUTO` 처리 코드 없음** → 각성 유닛 속성값이 게임 대비 낮게 나올 수 있음.

관련 파일:

- `catalog/seeder/AwakenedMyeongwangSeeder.java`
- `catalog/seeder/AwakenedHeavenlyKingSeeder.java`
- `domain/catalog/MercenaryCharacteristic.java`

---

### 3-4. 각성 명왕 덱 공통 속성값 버프

**구현**: `DpsCalculatorService.applyAwakenedMyeongwangAllyBuff()`  
**수치**: `AwakenedMyungwangBuffCalculator.getCommonAllyBuff()` → 전원 +5, `Nature.EARTH` +2 추가

| 게임 규칙(기획) | 현재 구현 |
|----------------|-----------|
| 각성 명왕 1명 이상 → 전원 +5 | ✅ `anyMatch` (1명이든 4명이든 **동일**, 중첩 없음) |
| 토 속성 추가 +2 | ✅ |
| 각성 명왕 본인 +15 | ❌ (3-3 미구현) |

---

### 3-5. 명왕 스탯 이전 (속성값과 무관)

**구현**: `MyungwangStatTransferCalculator`

| 명왕 속성 | 이전 스탯 | 기본 이전율 |
|-----------|-----------|-------------|
| 화(FIRE) | STR | 10% |
| 뇌(THUNDER) | DEX | 10% |
| 풍(WIND) | VIT | 일반 5% / 각성 10% |
| 수(WATER) | INT | 일반 5% / 각성 10% |
| 토(EARTH, 부동) | — | 이전 없음 |

- **이전 대상 우선순위** (동속성 1명): 사천왕 → 주인공 → 전설장수 (명왕 본인 제외)
- **추가 이전율**: “이전율/이전되는” 특성 레벨의 `amountValue`
- **결과**: `STR/DEX/VIT/INT`만 가산 — **`ELEMENT_VALUE` 아님**

`AwakenedMyungwangBuffCalculator.calculateTransfer()`는 DPS에서 **호출되지 않음** (dead code). 실제 이전은 `MyungwangStatTransferCalculator`만 사용.

---

### 3-6. 주인공 국가 속성 버프

**구현**: `PlayerCharacterBuffCalculator.getNationBuff()`

| 국가 | 속성 | 가산 |
|------|------|------|
| 조선 | FIRE | +5 |
| 일본 | WATER | +5 |
| 중국 | WIND | +5 |
| 대만 | THUNDER | +5 |
| 인도 | EARTH | +3 |

- 덱에 **주인공**이 있을 때, **같은 Nature** 용병에게만 `ELEMENT_VALUE` 가산
- DPS에만 적용 — `DeckService.getMemberStats`에는 **미반영**

---

### 3-7. 정령 / 진법 / 층진

#### 정령 (`applyDeckEffects`)

- ALLY 버프 → **전역 `partyFlatBonus`**에 합산
- **멤버별 속성 필터 없음**

예: 물 전설 정령 “어린 심아리” — `ADAPTIVE +5`, `EARTH +2`  
→ 덱 **전원**이 partyFlat로 +7 받는 구조 (토만 +2 추가가 아님)

#### 진법·층진

| Element | 적용 |
|---------|------|
| NONE, ADAPTIVE | 전원 파티 버프 |
| FIRE, WATER 등 | `applyDeckBuffSourcePerMember` — Nature 일치 멤버만 |

정령과 진법·층진의 **속성 필터 규칙이 일관되지 않음**.

---

### 3-8. 장비·세트

| scope | DPS 처리 |
|-------|----------|
| SELF | `isElementApplicable(element, member.nature)` 후 본인 flat |
| ALLY | 착용자 Nature로 필터 후 **전역 partyFlat** 합산 → **수혜자는 덱 전원** |

`DeckService` 장비 합산(`equipStatMap`)은 **속성 필터 없이** SELF scope만 합산 → 스탯 API와 DPS 수치 불일치 가능.

---

### 3-9. 속성 보정 (Adjust DPS)

**구현**: `ElementBonusCalculator`

| 몬ster | 속성 보정 |
|--------|-----------|
| `element` null 또는 `NONE` (무속성) | **0%** (우위·추가 데미지 없음) |
| 속성 있음 | `clamp((3×용병속성값 - 몬스터속성값) / 2, -50, +50)` |

몬스터 속성값은 **덱 전원 `ELEMENT_PIERCE` 합 + 적 디버프**로 `effectiveMonsterElement` 계산 후 사용.

---

## 4. 덱 스탯 API vs DPS 불일치

**스탯 API**: `GET .../decks/{deckId}/members/{memberId}/stats` → `DeckService.getMemberStats()`

| 항목 | DPS | getMemberStats |
|------|-----|----------------|
| MercenaryStat base | ✅ | ✅ |
| 장비 속성 필터 | ✅ (SELF) | ❌ |
| ALLY 특성 → total | ✅ (partyFlat) | ❌ `partyCharacteristicStatMap` total **미합산** |
| 주인공 국가 버프 | ✅ | ❌ |
| 각성 명왕 +5/+2 | ✅ | ❌ |
| SELF_AUTO 각성 +15/+20 | ❌ | ❌ |
| 명왕 이전 | STR/DEX/VIT/INT | ✅ (별도 `transferStatMap`) |

프론트 “속성값” 표시(DPS 패널)와 스탯 breakdown API 숫자가 다르면 위 차이 때문일 수 있음.

---

## 5. “수치가 이상해 보이는” 대표 원인

1. **각성 +15/+20 미연동** — 시더 주석만 있고 계산 코드 없음
2. **각성 명왕 수와 무관 +5 1회** — `anyMatch`, N마리 중첩 없음
3. **정령 ADAPTIVE/EARTH 전원 적용** — 비토 용병도 +7 가능
4. **partyFlat 구조** — ALLY 속성 버프가 특정 1명이 아니라 덱 전체에 붙음
5. **`natureValue` vs MercenaryStat** — API 목록과 DPS base 불일치
6. **명왕 이전 ≠ 속성값** — 힘/민첩 등만 이전
7. **스탯 API ≠ DPS** — breakdown과 DPS 비교 시 어긋남

---

## 6. 관련 파일 (백엔드)

| 역할 | 경로 |
|------|------|
| DPS 속성값 집계 | `back/gersangtrade/.../calculator/service/DpsCalculatorService.java` |
| 속성 보정 공식 | `back/gersangtrade/.../calculator/service/ElementBonusCalculator.java` |
| 명왕 스탯 이전 | `back/gersangtrade/.../calculator/service/MyungwangStatTransferCalculator.java` |
| 각성 명왕 버프(일부) | `back/gersangtrade/.../calculator/service/AwakenedMyungwangBuffCalculator.java` |
| 특성 scope | `back/gersangtrade/.../calculator/service/CharacteristicScopeResolver.java` |
| 주인공 국가 버프 | `back/gersangtrade/.../calculator/service/PlayerCharacterBuffCalculator.java` |
| 덱 스탯 API | `back/gersangtrade/.../deck/service/DeckService.java` |
| 각성 시드 | `AwakenedMyeongwangSeeder.java`, `AwakenedHeavenlyKingSeeder.java` |

---

## 7. 프론트 참고

| UI | 데이터 소스 |
|----|-------------|
| DPS 패널 용병 “속성값” | `POST /api/calculator/dps` → `memberResults[].elementValue` |
| 용병 목록 속성값 | `GET /api/mercenaries` → `elementValue` (= `natureValue`) |
| 스탯 breakdown | `getMemberStats` — DPS와 1:1 대응 아님 (§4 참고) |

---

## 8. 수정 시 우선순위 제안

1. `SELF_AUTO` 각성 속성값 (+15/+20) DPS·스탯 API 반영
2. 정령 `ELEMENT_VALUE` 멤버별 속성 필터 (진법·층진과 동일 규칙)
3. `getMemberStats`에 주인공 국가·각성 명왕 버프·party 특성 합산 정렬
4. 각성 명왕 N명 중첩 여부 — 게임 규칙 확인 후 `anyMatch` vs count×버프
5. `natureValue`와 `MercenaryStat(ELEMENT_VALUE)` 동기화 정책
