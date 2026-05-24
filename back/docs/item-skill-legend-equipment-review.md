# ItemSkill · 전설장비 스펙 검토 및 결정 사항

> 참고 스펙: `item-skill-spec.md`, `legend-equipment-spec.md`
> 작성일: 2026-05-21
> 상태: 논의 중

---

## 목차

1. [🔴 충돌 — 결정 필요](#1-충돌--결정-필요)
2. [🟡 수정 필요 — 코드에 없는 것](#2-수정-필요--코드에-없는-것)
3. [🟢 신규 추가 대상](#3-신규-추가-대상)
4. [🔵 추가 설계 결정 필요](#4-추가-설계-결정-필요)
5. [결정 로그](#5-결정-로그)

---

## 1. 🔴 충돌 — 결정 필요

### 1-1. `SkillType` enum 충돌 (핵심)

`item-skill-spec.md`는 `SkillType`을 `ACTIVE / TRIGGER / PASSIVE` 값으로 사용한다고 명시.
그러나 **기존 `SkillType.java`는 이미 다른 의미로 존재**한다.

| 항목 | 기존 `SkillType` | 스펙의 `SkillType` |
|---|---|---|
| 값 | `INSTANT`, `PERSISTENT` | `ACTIVE`, `TRIGGER`, `PASSIVE` |
| 의미 | DPS **계산 방식** (즉발 vs 지속 틱) | 스킬 **행동 유형** (일반 vs 조건부 vs 패시브) |
| 사용처 | `SkillCoefficient.skillType` | `ItemSkill.skillType`, `SetGrantedSkill.skillType` |

스펙은 "MercenarySkill의 SkillType enum 재사용"이라고 했으나,
`MercenarySkill`에는 `skillType` 필드 자체가 없고 `SkillType`은 `SkillCoefficient`에서만 쓰인다.

**선택지:**
- **A안**: 새 enum `SkillBehaviorType`(ACTIVE/TRIGGER/PASSIVE) 신규 생성. 기존 `SkillType`(INSTANT/PERSISTENT) 유지. → 관심사 분리, 혼용 없음
- **B안**: 기존 `SkillType`에 `ACTIVE`, `TRIGGER`, `PASSIVE` 값 추가. → 두 의미가 한 enum에 혼재

> **검토 의견**: A안 권장. B안은 SkillCoefficient와 ItemSkill이 같은 enum을 다른 의미로 쓰게 되어 혼란 유발.

**→ 결정:**

---

### 1-2. `SetGrantedSkill` ↔ `SkillCoefficient` 연결 방식

현재 `SkillCoefficient`는 DB CHECK 제약으로 `mercenarySkill` / `itemSkill` 중 하나만 not null.
`SetGrantedSkill`(전설장비 세트 부여 스킬)도 SkillCoefficient에서 계수를 조회해야 한다.

**선택지:**
- **A안**: `SkillCoefficient`에 `setGrantedSkill` FK 추가. CHECK 제약을 "셋 중 하나만 not null"로 확장.
- **B안**: `SetGrantedSkill.skillKey`로 skill_key 컬럼 직접 string 매칭. `SkillCoefficient` 구조 변경 없음.

> **검토 의견**: A안은 구조 일관성 높음. B안은 SkillCoefficient 변경 최소화. 데이터 정합성은 A안이 유리.

**→ 결정:**

---

## 2. 🟡 수정 필요 — 코드에 없는 것

### 2-1. `EquipmentItem` — 컬럼 2개 누락

`legend-equipment-spec.md` 4.1절이 추가를 요구하나 현재 코드에 없음.

```
mercenary   (FK → Mercenary, nullable)  — 전용장비 대상 용병. null이면 일반 장비
enhancement (Integer, nullable)         — 강화 단계: 0 / 5 / 10. 전용장비 외에는 null
```

서비스 레이어 검증:
- `mercenary == null → enhancement == null`
- `mercenary != null → enhancement ∈ {0, 5, 10}`

---

### 2-2. `ItemSkill` — 필드 5개 누락

현재 `ItemSkill`에는 `id`, `item`, `skillName`, `skillKey` 4개만 있음.
`item-skill-spec.md` 2.1절이 아래 필드 추가를 요구.

```
skillType           (SkillBehaviorType or SkillType) — 스킬 행동 유형
replacesBaseSkill   (boolean, default false)         — 용병 기본 스킬 대체 여부
triggerEveryN       (Integer, nullable)              — 트리거 발동 주기
triggerBaseSkillKey (String, nullable)               — 트리거 카운트 기준 스킬 key
note                (TEXT, nullable)                 — 특이사항 메모
```

---

### 2-3. `StatType` — `STUN_DURATION` 누락

`legend-equipment-spec.md` 5.1절에서 최무선 세트 효과에 `STUN_DURATION` 사용.
현재 `StatType.java`에 없음.

---

## 3. 🟢 신규 추가 대상

현재 코드에 존재하지 않아 새로 만들어야 하는 항목.

| 대상 | 종류 | 출처 |
|---|---|---|
| `SetGrantedSkill` | 엔티티 | legend-equipment-spec 4.2 |
| `EquipmentSetSkillEffect` | 엔티티 | legend-equipment-spec 4.3 |
| `StatSource` | enum (SELF / AFFINITY) | legend-equipment-spec 2.1 |
| `TriggerSource` | enum (SELF / MERCENARY) | legend-equipment-spec 2.2 |

---

## 4. 🔵 추가 설계 결정 필요

### 4-1. `EquipmentSet`에 `enhancement` 필드 추가 여부

스펙은 "5강 세트와 10강 세트는 별도 EquipmentSet으로 분리"라고 하는데,
현재 `EquipmentSet`에는 enhancement 정보가 없다.

DPS 계산 시 "착용 아이템의 enhancement → 어떤 EquipmentSet 조회?" 흐름이 필요.

**선택지:**
- **A안**: `EquipmentSet`에 `enhancement` 컬럼 추가 → 명시적 조회 가능
- **B안**: 이름 컨벤션("최무선5강세트", "최무선10강세트")으로만 구분 → 코드에서 name 파싱 필요, 취약

> **검토 의견**: A안이 맞으나 스펙에 명시되지 않음. 확인 필요.

**→ 결정:**

---

### 4-2. 전용장비 enhancement 혼합 착용 처리 정책

`legend-equipment-spec.md` 8절 오픈 이슈:
"5강 3개 + 10강 4개 혼합 착용 시 어느 세트 효과를 적용할지" 미결정.

DPS 계산기 구현 전에 정책 확정 필요.

**후보 정책:**
- **다수결**: 5강/10강 중 더 많이 착용한 단계로 결정 (동수면 낮은 단계)
- **최솟값**: 착용 중 가장 낮은 강화 단계로 결정 (안전)
- **최댓값**: 착용 중 가장 높은 강화 단계로 결정 (유리하게)
- **분리**: 5강 피스끼리, 10강 피스끼리 별도 집계 후 각각 세트효과 적용

**→ 결정:**

---

## 5. 결정 로그

| 항목 | 결정 내용 | 일자 |
|---|---|---|
| 1-1. SkillType enum | A안 — 새 enum `SkillBehaviorType`(ACTIVE/TRIGGER/PASSIVE) 신규 생성. `SkillType`은 DPS 계산 방식 전용으로 INSTANT/PERSISTENT/**TRIGGER** 3종으로 확장. `trigger_every_n`은 `ItemSkill`/`SetGrantedSkill`에 보관하며 `SkillCoefficient`에 중복 저장 없음 (Skill-coeff-entity.json에 해당 필드 없음 확인) | 2026-05-21 |
| 1-2. SetGrantedSkill 연결 | A안 — `SkillCoefficient`에 `setGrantedSkill` FK 추가. CHECK 제약을 "셋 중 하나만 not null"로 확장 | 2026-05-21 |
| 4-1. EquipmentSet.enhancement | A안 — `EquipmentSet`에 `enhancement` 컬럼 추가. 타입은 `Enhancement` enum(NONE=0, FIVE=5, TEN=10)으로 value 필드 보유. DB에는 실제 수치(0/5/10) 저장 | 2026-05-21 |
| 4-2. 혼합 착용 정책 | 최댓값 — 착용 전용장비 중 가장 높은 enhancement 단계의 EquipmentSet 효과 적용 | 2026-05-21 |
