# 전설장비 세트 설계 문서

> 거상 거래 플랫폼 — 전설 전용장비 세트 엔티티 설계 확정본

---

## 목차

1. [개요 및 설계 원칙](#1-개요-및-설계-원칙)
2. [Enum 정의](#2-enum-정의)
3. [엔티티 변경사항 총정리](#3-엔티티-변경사항-총정리)
4. [엔티티 상세 설계](#4-엔티티-상세-설계)
5. [데이터 예시](#5-데이터-예시)
6. [계산기 연동 흐름](#6-계산기-연동-흐름)
7. [제약 조건](#7-제약-조건)
8. [오픈 이슈](#8-오픈-이슈)

---

## 1. 개요 및 설계 원칙

### 1.1 배경

거상의 일부 용병(전설장수 등)은 해당 용병 전용 장비 세트가 존재한다.
전용 장비는 강화 단계(0강 / 5강 / 10강)에 따라 세트효과가 달라지며,
10강 7종 세트는 스킬 부여 효과(예: 천자총통:개량)를 포함한다.

인연(Affinity) 개념은 MVP 이후 구현 예정이나, 확장을 위한 구조는 지금 잡아둔다.

### 1.2 설계 원칙

```
- 전용장비 여부: EquipmentItem.mercenary FK (nullable)
  → null이면 누구나 착용 가능, not null이면 해당 용병 전용

- 강화 단계: EquipmentItem.enhancement (0 / 5 / 10)
  → 강화 단계별로 별도 EquipmentSet 생성
  → 5강 세트와 10강 세트는 대체 관계 (누적 아님)
  → 동일 슬롯의 착용 아이템 enhancement로 어느 세트 효과를 적용할지 결정

- 스탯 효과: 기존 EquipmentSetEffect 구조 재사용
  → 달성한 종 수 이하 모든 효과 누적 적용

- 스킬 부여 효과: EquipmentSetSkillEffect + SetGrantedSkill 신규 엔티티
  → EquipmentSetEffect와 완전 분리

- 전용장비는 거래 불가 (isTradeable=false)
  → 거래 플랫폼과 연동 불필요

- 인연(Affinity): MVP 이후 구현
  → statSource / triggerSource 컬럼만 지금 추가
```

---

## 2. Enum 정의

### 2.1 StatSource

스킬 데미지 계산 시 어느 용병의 스탯을 참조할지 결정한다.

```java
public enum StatSource {
    SELF,       // 스킬 보유 용병(전설장수) 본인의 스탯 사용 (기본값)
    AFFINITY    // 인연에 연결된 사천왕의 스탯 사용 (인연 구현 시 활성화)
}
```

### 2.2 TriggerSource

트리거 카운트를 누가 발생시키는지 결정한다.

```java
public enum TriggerSource {
    SELF,       // 스킬 보유 용병 본인의 스킬 시전 횟수 카운트 (기본값)
    MERCENARY   // 덱 내 연결된 사천왕의 스킬 시전 횟수 카운트 (인연 구현 시 활성화)
}
```

---

## 3. 엔티티 변경사항 총정리

### 수정 대상

| 엔티티 | 변경 내용 |
|---|---|
| `EquipmentItem` | `mercenary` FK (nullable) + `enhancement` 컬럼 추가 |

### 신규 추가 대상

| 엔티티 | 역할 |
|---|---|
| `EquipmentSetSkillEffect` | 세트 종 수 달성 시 스킬 부여 효과 |
| `SetGrantedSkill` | 세트가 부여하는 스킬 정의 |

---

## 4. 엔티티 상세 설계

### 4.1 EquipmentItem (수정)

기존 컬럼 유지. 아래 두 컬럼 신규 추가.

```java
/**
 * 전용장비 대상 용병.
 * null이면 누구나 착용 가능한 일반 장비.
 * not null이면 해당 용병만 착용 가능한 전용장비.
 *
 * 전설장수, 사천왕, 명왕, 주인공(성별 포함) 모두 Mercenary 테이블로 관리.
 * 주인공 성별 구분이 필요한 경우 Mercenary에 gender 컬럼 추가 필요 (오픈 이슈 참고).
 */
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "mercenary_id")
private Mercenary mercenary;

/**
 * 강화 단계.
 * 0  → 미강화 (0강)
 * 5  → 5강
 * 10 → 10강
 * null → 강화 단계 없는 일반 아이템
 *
 * 전용장비(mercenary != null)인 경우에만 의미 있는 값.
 * 강화 단계별로 별도 EquipmentSet이 존재하며, 대체 관계임 (누적 아님).
 */
@Column(name = "enhancement")
private Integer enhancement;
```

**서비스 레이어 검증:**
```
mercenary == null이면 enhancement도 null이어야 함
mercenary != null이면 enhancement는 0 / 5 / 10 중 하나여야 함
```

---

### 4.2 SetGrantedSkill (신규)

세트 효과로 부여되는 스킬 정의 엔티티.
`MercenarySkill`, `ItemSkill`과 독립된 별도 엔티티.

```java
@Entity
@Table(name = "set_granted_skills")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SetGrantedSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 스킬명.
     * 예: "천자총통:개량"
     * MercenarySkill의 "천자총통"과 별개의 스킬임.
     */
    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    /**
     * 거니버스 내부 스킬 식별 키.
     * 거니버스 데이터 적재 후 채워짐. 적재 전 null.
     * SkillCoefficient 연결 키로 사용.
     */
    @Column(name = "skill_key", length = 100)
    private String skillKey;

    /**
     * 스킬 타입. MercenarySkill의 SkillType enum 재사용.
     *   ACTIVE  → 일반 발동 스킬
     *   TRIGGER → 조건부 발동 스킬 (매 n번째)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", length = 20)
    private SkillType skillType;

    /**
     * 트리거 발동 주기 (skillType=TRIGGER일 때만 사용).
     * 예: 5 → triggerSource 기준 스킬 5회마다 이 스킬 발동
     * null → 트리거 조건 없음
     */
    @Column(name = "trigger_every_n")
    private Integer triggerEveryN;

    /**
     * 트리거 카운트 주체.
     * SELF      → 스킬 보유 용병(전설장수) 본인의 스킬 시전 횟수 카운트
     * MERCENARY → 인연에 연결된 사천왕의 스킬 시전 횟수 카운트
     *
     * 인연 미구현 MVP에서는 MERCENARY 타입 스킬은 계산에서 제외하고 note로 표시.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_source", nullable = false, length = 20)
    private TriggerSource triggerSource = TriggerSource.SELF;

    /**
     * 스탯 참조 대상.
     * SELF     → 전설장수 본인의 스탯으로 데미지 계산 (기본값)
     * AFFINITY → 인연에 연결된 사천왕의 스탯으로 데미지 계산
     *
     * 인연 미구현 MVP에서는 AFFINITY 타입 스킬은 계산에서 제외하고 note로 표시.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_source", nullable = false, length = 20)
    private StatSource statSource = StatSource.SELF;

    /**
     * 스킬 특이사항 메모.
     * 공식으로 표현 불가한 특수 효과 기록용.
     * 예: "인연 구현 시 statSource=AFFINITY로 변경 필요"
     */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    public void updateSkillKey(String skillKey) {
        this.skillKey = skillKey;
    }
}
```

---

### 4.3 EquipmentSetSkillEffect (신규)

세트 종 수 달성 시 스킬을 부여하는 효과 엔티티.
스탯 효과(`EquipmentSetEffect`)와 완전 분리.

```java
@Entity
@Table(
    name = "equipment_set_skill_effects",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_equipment_set_skill_effect",
        columnNames = {"equipment_set_id", "required_pieces", "set_granted_skill_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipmentSetSkillEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 어떤 세트의 효과인지.
     * 5강 세트 / 10강 세트가 별도 EquipmentSet으로 분리되어 있으므로
     * enhancement 정보는 equipmentSet을 통해 간접 조회.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_set_id", nullable = false)
    private EquipmentSet equipmentSet;

    /**
     * 몇 종 달성 시 발동하는 효과인지.
     * 예: 7 → 7종 달성 시 스킬 부여
     */
    @Column(name = "required_pieces", nullable = false)
    private Integer requiredPieces;

    /**
     * 부여되는 스킬.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_granted_skill_id", nullable = false)
    private SetGrantedSkill setGrantedSkill;
}
```

---

## 5. 데이터 예시

### 5.1 도령 최무선 전용장비 세트

**5강 세트 — EquipmentSetEffect**

| equipment_set_id | required_pieces | stat_type | stat_value | stat_unit |
|---|---|---|---|---|
| 최무선5강세트 | 3 | STUN_DURATION | 1 | FLAT |
| 최무선5강세트 | 4 | STRENGTH | 200 | FLAT |
| 최무선5강세트 | 5 | HITTING_RESISTANCE | 25 | PERCENT |
| 최무선5강세트 | 6 | STRENGTH | 350 | FLAT |

> `STUN_DURATION` (기절 시간) StatType 신규 추가 필요. StatType.java 수정 대상.

**10강 세트 — EquipmentSetEffect**

| equipment_set_id | required_pieces | stat_type | stat_value | stat_unit |
|---|---|---|---|---|
| 최무선10강세트 | 3 | STUN_DURATION | 2 | FLAT |
| 최무선10강세트 | 4 | STRENGTH | 200 | FLAT |
| 최무선10강세트 | 5 | HITTING_RESISTANCE | 30 | PERCENT |
| 최무선10강세트 | 6 | STRENGTH | 350 | FLAT |
| 최무선10강세트 | 7 | STRENGTH | 350 | FLAT |

**10강 세트 — EquipmentSetSkillEffect**

| equipment_set_id | required_pieces | set_granted_skill_id |
|---|---|---|
| 최무선10강세트 | 7 | 천자총통:개량 |

**SetGrantedSkill — 천자총통:개량**

```
skill_name    : 천자총통:개량
skill_key     : (거니버스 적재 후 채워짐)
skill_type    : TRIGGER
trigger_every_n      : n  (실제 값 확인 후 입력)
trigger_source       : MERCENARY  ← 사천왕의 스킬 시전 횟수 카운트
stat_source          : SELF       ← 전설장수(최무선) 스탯 기준 계산
                                     인연 등록 시 AFFINITY로 변경
note          : "MVP에서는 계산 제외. triggerSource=MERCENARY 인연 구현 시 활성화 필요."
```

### 5.2 EquipmentItem 강화 단계별 저장 예시

| name | slot | mercenary | enhancement |
|---|---|---|---|
| 최무선 전용 갑옷 0강 | ARMOR | 도령 최무선 | 0 |
| 최무선 전용 갑옷 5강 | ARMOR | 도령 최무선 | 5 |
| 최무선 전용 갑옷 10강 | ARMOR | 도령 최무선 | 10 |
| 지국천왕 갑옷 | ARMOR | null | null |

---

## 6. 계산기 연동 흐름

### 6.1 전용장비 세트효과 적용 로직

```
DeckMercenary 조회
  ↓
DeckMercenarySlot에서 착용 아이템 목록 조회
  ↓
equipmentItem.mercenary != null인 슬롯 필터링 (전용장비)
  ↓
전용장비 enhancement 값 확인
  → 모든 전용장비의 enhancement가 동일하다고 가정
  → 5강이면 5강 EquipmentSet 조회
  → 10강이면 10강 EquipmentSet 조회
  ↓
착용 전용장비 종 수 집계
  ↓
EquipmentSetEffect에서 requiredPieces 이하 효과 전부 누적 적용
  ↓
EquipmentSetSkillEffect에서 requiredPieces 달성 스킬 효과 확인
  → setGrantedSkill.triggerSource == MERCENARY → MVP 제외, note 표시
  → setGrantedSkill.triggerSource == SELF → DPS 계산 포함 (추후)
```

### 6.2 MVP에서 제외되는 계산

```
triggerSource = MERCENARY인 SetGrantedSkill
  → 계산기에서 스킵
  → UI에 "인연 효과 미포함" 표시

statSource = AFFINITY인 SetGrantedSkill
  → 계산기에서 스킵
  → UI에 "인연 효과 미포함" 표시
```

---

## 7. 제약 조건

### 7.1 EquipmentItem

```
mercenary == null → enhancement == null
mercenary != null → enhancement ∈ {0, 5, 10}
→ 서비스 레이어 검증
```

### 7.2 EquipmentSetSkillEffect

```
UNIQUE (equipment_set_id, required_pieces, set_granted_skill_id)
```

### 7.3 SetGrantedSkill

```
skillType = TRIGGER → triggerEveryN not null
skillType != TRIGGER → triggerEveryN null
→ 서비스 레이어 검증
```

---

## 8. 오픈 이슈

| 항목 | 내용 | 우선순위 |
|---|---|---|
| StatType.STUN_DURATION 추가 | 기절+n초 효과 표현을 위해 StatType.java에 신규 추가 필요 | 높음 |
| Mercenary.gender 컬럼 추가 | 주인공 성별 구분 필요 시 Mercenary 테이블에 gender 컬럼 추가 | 중간 |
| trigger_every_n 실제 값 확인 | 천자총통:개량의 실제 발동 주기 게임에서 확인 후 입력 필요 | 중간 |
| 인연(Affinity) 구현 | triggerSource=MERCENARY / statSource=AFFINITY 스킬 계산 활성화 | 낮음 (MVP 이후) |
| 전용장비 enhancement 혼합 착용 처리 | 5강 3개 + 10강 4개 혼합 시 어느 세트 효과를 적용할지 정책 결정 필요 | 중간 |