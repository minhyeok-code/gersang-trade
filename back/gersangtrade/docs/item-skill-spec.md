# ItemSkill 엔티티 설계 및 무기 스킬 계산기 연동 문서

> 거상 거래 플랫폼 — 아이템 스킬 설계 확정본

---

## 목차

1. [개요 및 설계 원칙](#1-개요-및-설계-원칙)
2. [ItemSkill 엔티티 수정](#2-itemskill-엔티티-수정)
3. [데이터 예시](#3-데이터-예시)
4. [계산기 연동 흐름](#4-계산기-연동-흐름)
5. [제약 조건](#5-제약-조건)
6. [오픈 이슈](#6-오픈-이슈)

---

## 1. 개요 및 설계 원칙

### 1.1 배경

일부 무기 아이템은 장착 시 용병의 기본 스킬을 대체하는 고유 스킬을 가진다.
대표 사례: 각천극(무기) 장착 시 각성 광목천왕의 기본 스킬이 각천극 스킬로 대체됨.

추가로, 일부 무기는 대체 스킬 외에 **조건부 발동(트리거) 패시브 스킬**을 함께 가진다.
대표 사례: 각천극 스킬 3회 시전마다 별도 패시브 스킬이 발동됨.

### 1.2 설계 원칙

```
- 무기 ItemSkill이 기본 스킬을 대체하는지 여부 → replacesBaseSkill 플래그로 표현
- 무기 ItemSkill의 스킬 타입 → MercenarySkill과 동일한 SkillType enum 재사용
- 트리거 스킬의 발동 조건 → triggerEveryN + triggerBaseSkillKey로 표현
- 스킬 계수 → SkillCoefficient에서 skillKey로 조회 (기존 구조 유지)
- 주인공 기본 스킬은 MVP 제외 — 무기 ItemSkill로 대체 계산
```

### 1.3 기존 ItemSkill과의 차이

| 항목 | 기존 ItemSkill | 변경 후 ItemSkill |
|---|---|---|
| 역할 | 아이템이 가진 스킬명/key 저장 | 스킬 타입 + 대체 여부 + 트리거 조건 추가 |
| 계수 연결 | skillKey → SkillCoefficient | 동일 (유지) |
| 대체 관계 | 없음 | replacesBaseSkill 플래그 추가 |
| 트리거 조건 | 없음 | triggerEveryN + triggerBaseSkillKey 추가 |

---

## 2. ItemSkill 엔티티 수정

### 2.1 추가 컬럼 목록

기존 컬럼 (`id`, `item_id`, `skill_name`, `skill_key`) 유지.
아래 컬럼 신규 추가.

```java
@Entity
@Table(
        name = "item_skills",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_item_skills_item_skill",
                columnNames = {"item_id", "skill_name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    /** 거니버스 내부 스킬 식별 키. 거상짱 크롤링 시에는 null. */
    @Column(name = "skill_key", length = 100)
    private String skillKey;

    // ── 신규 추가 ─────────────────────────────────────────

    /**
     * 스킬 타입. MercenarySkill의 SkillType enum 재사용.
     *   ACTIVE  → 일반 발동 스킬
     *   TRIGGER → 조건부 발동 스킬 (매 n번째)
     *   PASSIVE → 상시 적용 패시브
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", length = 20)
    private SkillType skillType;

    /**
     * 기본 스킬 대체 여부.
     * true  → 이 아이템 장착 시 용병의 기본 MercenarySkill 대신 이 스킬로 DPS 계산
     * false → 기본 스킬에 추가로 발동되는 스킬
     *
     * 용병의 기본 스킬은 1개이며, 무기 아이템은 이를 항상 대체한다.
     * 동일 DeckMercenarySlot에서 replacesBaseSkill=true인 ItemSkill이
     * 존재하면 MercenarySkill은 DPS 계산에서 제외된다.
     */
    @Column(name = "replaces_base_skill", nullable = false)
    private boolean replacesBaseSkill = false;

    /**
     * 트리거 발동 주기 (TRIGGER 타입 전용).
     * 예: 3 → triggerBaseSkillKey 스킬 3회 시전마다 이 스킬 발동
     * null → 트리거 조건 없음
     */
    @Column(name = "trigger_every_n")
    private Integer triggerEveryN;

    /**
     * 트리거 카운트 기준 스킬 key (TRIGGER 타입 전용).
     * 예: 각천극 스킬의 skill_key → 각천극 스킬 n회마다 발동
     * null → 트리거 조건 없음
     */
    @Column(name = "trigger_base_skill_key", length = 100)
    private String triggerBaseSkillKey;

    /**
     * 스킬 특이사항 메모.
     * 공식으로 표현 불가한 특수 효과 기록용.
     */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    public void updateSkillKey(String skillKey) {
        this.skillKey = skillKey;
    }
}
```

---

## 3. 데이터 예시

### 3.1 각천극 — 대체 스킬 + 트리거 패시브

각천극 무기는 두 개의 ItemSkill을 가진다.

**① 대체 스킬 (각천극 메인 스킬)**

```
item       : 각천극
skill_name : 각천극 스킬 (실제 스킬명으로 교체)
skill_key  : (거니버스 적재 후 채워짐)
skill_type : ACTIVE
replaces_base_skill : true
trigger_every_n     : null
trigger_base_skill_key : null
```

SkillCoefficient (각천극 스킬):
```
공격력 계수: 10.75
생명력 계수: 24.3
레벨 계수  : 7.3
```

비교 — 각성 광목천왕 기본 스킬 SkillCoefficient:
```
공격력 계수: 10.75
생명력 계수: 20.2
레벨 계수  : 7.31
```

**② 트리거 패시브 (3회마다 발동)**

```
item       : 각천극
skill_name : 각천극 패시브 스킬 (실제 스킬명으로 교체)
skill_key  : (거니버스 적재 후 채워짐)
skill_type : TRIGGER
replaces_base_skill    : false
trigger_every_n        : 3
trigger_base_skill_key : 각천극 메인 스킬의 skill_key
```

### 3.2 각천비 — 대체 스킬만 있는 경우

```
item       : 각천비
skill_name : 각천비 스킬
skill_key  : (거니버스 적재 후 채워짐)
skill_type : ACTIVE
replaces_base_skill : true
trigger_every_n     : null
trigger_base_skill_key : null
```

SkillCoefficient (각천비 스킬):
```
공격력 계수: 5.7
지력 계수  : 12.8
레벨 계수  : 12.1
```

비교 — 각성 다문천왕 기본 스킬 SkillCoefficient:
```
공격력 계수: 5.7
지력 계수  : 10.7
레벨 계수  : 6.5
```

---

## 4. 계산기 연동 흐름

### 4.1 DPS 계산 시 스킬 선택 로직

```
DeckMercenary 조회
  ↓
해당 용병의 DeckMercenarySlot 전체 순회
  ↓
replacesBaseSkill=true인 ItemSkill 존재 여부 확인
  ↓
  [존재하는 경우]
    MercenarySkill → DPS 계산에서 제외
    ItemSkill(replacesBaseSkill=true)의 skillKey
      → SkillCoefficient 조회 → DPS 계산
    TRIGGER 타입 ItemSkill이 있으면 추가 계산 (4.2 참고)

  [존재하지 않는 경우]
    MercenarySkill의 skillKey
      → SkillCoefficient 조회 → DPS 계산
```

### 4.2 트리거 스킬 DPS 계산

```
트리거 스킬 DPS =
  triggerSkillDamage × (baseSkill.castsPerSecond / triggerEveryN)

예시 (각천극):
  각천극 스킬 casts_per_second = 2.0
  trigger_every_n = 3
  트리거 발동 횟수 = 2.0 / 3 = 0.667회/초

  트리거 DPS = triggerSkillDamage × 0.667
```

### 4.3 전체 DPS 합산

```
용병 총 DPS
  = 대체 스킬 DPS (or 기본 스킬 DPS)
  + 트리거 스킬 DPS (있는 경우)
  + 기타 슬롯 ItemSkill DPS (replacesBaseSkill=false인 ACTIVE 스킬)
```

> 모든 슬롯(일반 + 외변)의 ItemSkill이 DPS 계산에 포함된다.
> 단, replacesBaseSkill=true인 ItemSkill이 있으면 MercenarySkill은 제외한다.

---

## 5. 제약 조건

### 5.1 서비스 레이어 검증

```
DeckMercenary 단위:
  replacesBaseSkill=true인 ItemSkill은 최대 1개
  → 무기 슬롯(WEAPON)은 1개이므로 자연스럽게 보장되나,
    서비스 레이어에서 명시적 검증 추가 권장

ItemSkill 저장 시:
  skill_type = TRIGGER인 경우
    → trigger_every_n not null
    → trigger_base_skill_key not null
  skill_type != TRIGGER인 경우
    → trigger_every_n = null
    → trigger_base_skill_key = null
```

### 5.2 SkillCoefficient 연결

```
ItemSkill.skillKey → SkillCoefficient 조회
  → skillKey가 null이면 계산기에서 해당 스킬 스킵
  → 거니버스 데이터 적재 후 skillKey 채워짐
```

---

## 6. 오픈 이슈

| 항목 | 내용 | 우선순위 |
|---|---|---|
| 각천극/각천비 외 무기 스킬 목록 확인 | replacesBaseSkill=true 대상 무기 전수 조사 필요 | 높음 |
| 트리거 카운트 기준 스킬 key 적재 시점 | triggerBaseSkillKey는 거니버스 적재 완료 후 수동 입력 또는 별도 매핑 필요 | 중간 |
| 주인공 기본 스킬 계산 | MVP 제외. 무기 ItemSkill로 대체 계산. 추후 별도 스펙 추가 | 낮음 |