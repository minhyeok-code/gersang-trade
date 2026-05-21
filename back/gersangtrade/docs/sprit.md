# 정령(Spirit) 엔티티 및 크롤링 스펙

## 0. 이 문서의 목적

Claude Code가 이 문서만 읽고 아래 두 가지를 구현할 수 있도록 작성되었다.

1. `Spirit`, `SpiritBuff` JPA 엔티티
2. 정령 데이터 시딩(Seeding) — 크롤링 대상 없음, 데이터 직접 하드코딩

---

## 1. 도메인 개요

거상의 **정령**은 유저 덱 전체에 버프/디버프를 적용하는 특수 유닛이다.

- 속성 5종 × 등급 5단계 = **총 25종** (현재 데이터 기준)
- 유저 덱에 **최대 2개 장착**, 반드시 **서로 다른 정령**이어야 함
- 버프 적용 대상: **아군 전체(ALLY)** 또는 **적(ENEMY)**
- 등급이 오를수록 버프 수치와 종류가 모두 늘어남

### 특수 케이스 (계산 로직 주의)

| 정령 | 특수 효과 | 처리 방침 |
|------|----------|----------|
| 번개 전설 (어린 전비) | 2초당 전체스텟+100, 최대 1500 | `ALL_STAT FLAT 800` 으로 근사. `specialEffectNote`에 원본 명시 |
| 땅 전설 (어린 토석동) | 타 정령 효과 ×2 (공속·이속 제외) | `SpiritBuffCalculator`에서 별도 처리. 엔티티 수치는 본인 버프만 저장 |

---

## 2. Enum 정의

### 2-1. Nature (기존 재사용 — 수정 금지)

`org.example.gersangtrade.domain.catalog.enums.Nature` 기존 enum을 그대로 사용한다.

```java
// 기존 값 (변경 금지)
FIRE("화"), WATER("수"), THUNDER("뇌"), WIND("풍"), EARTH("토"), NONE("-")
```

- `NONE` 은 무속성 용병에서 사용 중이므로 삭제 금지
- Spirit 엔티티의 `nature` 필드에 FIRE / WATER / THUNDER / WIND / EARTH 중 하나를 저장

### 2-2. SpiritGrade (신규)

```java
package org.example.gersangtrade.domain.catalog.enums;

public enum SpiritGrade {
    LOWER("하급"),
    MIDDLE("중급"),
    UPPER("상급"),
    HIGHEST("최상급"),
    LEGEND("전설");

    private final String displayName;

    SpiritGrade(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
```

### 2-3. StatUnit (기존 재사용)

`org.example.gersangtrade.domain.catalog.enums.StatUnit` 기존 enum을 그대로 사용한다.

```java
// 기존 값
FLAT,    // 고정값  예: 공격력 +100
PERCENT, // 퍼센트  예: 마저 +10%
LEVEL    // 단계
```

`BuffValueType` 을 별도로 만들지 않는다.

### 2-4. BuffTarget (신규)

```java
package org.example.gersangtrade.domain.catalog.enums;

public enum BuffTarget {
    ALLY,   // 아군 버프
    ENEMY   // 적 디버프
}
```

### 2-5. StatType 추가 항목

기존 `StatType` enum에 아래 4개 항목만 추가한다.  
`CRITICAL_CHANCE`, `SKILL_DAMAGE_PERCENT` 는 이미 존재하므로 추가 불필요.

```java
// 정령 버프용 추가
ATTACK_SPEED("공격속도"),
MOVE_SPEED("이동속도"),
HP_RECOVERY("체력회복"),
MP_RECOVERY("마력회복"),
```

### 2-6. Element enum (기존 재사용)

`SpiritBuff.element`는 기존 `Element` enum을 그대로 사용한다.  
속성 무관 버프에는 `NONE`을 사용한다.

```
FIRE, WATER, WIND, EARTH, THUNDER  — 해당 속성 용병에게만 적용
ADAPTIVE                            — 착용 용병 속성 추종
NONE                                — 속성 무관 (ELEMENT_VALUE 외 대부분)
```

> **Spirit vs SpiritBuff 속성 구분 이유**  
> - `Spirit.nature` (`Nature` 타입): 정령 자체의 속성 (화/수/뇌/풍/토 고정)  
> - `SpiritBuff.element` (`Element` 타입): 버프가 적용될 대상 속성 조건.  
>   `ADAPTIVE`가 필요하기 때문에 `Element`를 사용한다.  
>   예) 물 전설의 `ELEMENT_VALUE ADAPTIVE +5` = 착용 용병 속성에 따라 속성값 +5

---

## 3. 엔티티 정의

패키지: `org.example.gersangtrade.domain.catalog` (기존 엔티티와 동일)

### 3-1. Spirit

```java
package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.*;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.SpiritGrade;
import org.example.gersangtrade.domain.common.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "spirits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Spirit extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;                            // 예: "어린 심아리"

    @Enumerated(EnumType.STRING)
    @Column(name = "nature", nullable = false, length = 20)
    private Nature nature;                          // FIRE, WATER, THUNDER, WIND, EARTH

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, length = 20)
    private SpiritGrade grade;                      // LOWER, MIDDLE, UPPER, HIGHEST, LEGEND

    @Column(name = "acquire_condition", columnDefinition = "TEXT")
    private String acquireCondition;                // 고용/친밀도/재료 조건 텍스트 (nullable)

    @Column(name = "special_effect_note", columnDefinition = "TEXT")
    private String specialEffectNote;               // 구조화 불가 특수 효과 원문 (nullable)

    @Builder.Default
    @OneToMany(mappedBy = "spirit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpiritBuff> buffs = new ArrayList<>();

    /** 표시용: "풍 전설 어린 각웅" 형태 */
    public String getDisplayLabel() {
        return nature.getDisplayName() + " " + grade.getDisplayName() + " " + name;
    }
}
```

### 3-2. SpiritBuff

```java
package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.*;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.example.gersangtrade.domain.common.BaseEntity;

@Entity
@Table(name = "spirit_buffs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class SpiritBuff extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spirit_id", nullable = false)
    private Spirit spirit;

    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", nullable = false, length = 30)
    private StatType statType;

    /**
     * 속성 구분. ELEMENT_VALUE 버프에만 의미 있음.
     * ADAPTIVE : 착용 용병 속성 추종 (착용 용병이 불이면 화속성값 +n)
     * EARTH 등 : 해당 속성 용병에게만 적용 (예: 물 전설 땅속성 예외 +2)
     * NONE     : 속성 무관 (ELEMENT_VALUE 외 대부분의 버프)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "element", nullable = false, length = 20)
    private Element element;                        // 기본값 NONE

    @Enumerated(EnumType.STRING)
    @Column(name = "stat_unit", nullable = false, length = 20)
    private StatUnit statUnit;                      // FLAT | PERCENT

    @Column(nullable = false)
    private float value;                            // 양수 = 버프, 음수 = 디버프

    @Enumerated(EnumType.STRING)
    @Column(name = "target", nullable = false, length = 10)
    private BuffTarget target;                      // ALLY or ENEMY
}
```

---

## 4. 계산 서비스

패키지: `org.example.gersangtrade.calculator.service` (기존 CalculatorService와 동일 패키지)

### SpiritBuffCalculator

```java
package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.domain.catalog.Spirit;
import org.example.gersangtrade.domain.catalog.SpiritBuff;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.SpiritGrade;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 장착된 정령 최대 2개를 받아 최종 합산 버프 Map을 반환한다.
 *
 * 땅 전설(어린 토석동) 특수 처리:
 *   - 타 정령의 버프에 ×2 적용 (ATTACK_SPEED, MOVE_SPEED 제외)
 *   - 땅 전설 본인 버프는 ×2 대상이 아님
 *
 * 입력 전제조건 (호출 전 검증 필요):
 *   - equippedSpirits.size() <= 2
 *   - 동일 정령 중복 장착 불가
 */
@Service
public class SpiritBuffCalculator {

    private static final Set<StatType> SPEED_STATS = Set.of(
        StatType.ATTACK_SPEED,
        StatType.MOVE_SPEED
    );

    public Map<StatType, Float> calculate(List<Spirit> equippedSpirits) {

        boolean hasEarthLegend = equippedSpirits.stream()
            .anyMatch(s -> s.getNature() == Nature.EARTH
                       && s.getGrade() == SpiritGrade.LEGEND);

        Map<StatType, Float> result = new HashMap<>();

        for (Spirit spirit : equippedSpirits) {

            boolean isEarthLegend = spirit.getNature() == Nature.EARTH
                                 && spirit.getGrade() == SpiritGrade.LEGEND;

            for (SpiritBuff buff : spirit.getBuffs()) {

                float value = buff.getValue();

                // 땅 전설 효과: 다른 정령 버프에 ×2 (공속·이속 제외)
                if (hasEarthLegend && !isEarthLegend && !SPEED_STATS.contains(buff.getStatType())) {
                    value *= 2.0f;
                }

                result.merge(buff.getStatType(), value, Float::sum);
            }
        }

        return result;
    }
}
```

---

## 5. 데이터 시딩

크롤링 대상 없음. 아래 데이터를 `DataInitializer` 또는 Flyway seed SQL로 직접 삽입한다.

### 5-1. 공통 규칙

- **공격력 버프**: "아군 공격력 +N" → `MIN_POWER FLAT N`, `MAX_POWER FLAT N` **두 행**으로 저장
- **버프 컬럼 순서**: `target / element / statType / statUnit / value`
- `element = NONE`: 속성 무관 버프 (ELEMENT_VALUE 외 대부분)
- `statUnit`: FLAT 또는 PERCENT (`StatUnit` enum 사용)

### 5-2. 전체 데이터

#### 물 정령

| name | grade | acquireCondition | buffs |
|------|-------|-----------------|-------|
| 말랑이 | LOWER | 푸른색 알(유료) | ENEMY NONE MOVE_SPEED PERCENT -2 / ENEMY NONE ATTACK_SPEED PERCENT -2 |
| 얼음덩이 | MIDDLE | 친밀도3000, 물의결정1 | ENEMY NONE MOVE_SPEED PERCENT -4 / ENEMY NONE ATTACK_SPEED PERCENT -4 |
| 발로 | UPPER | 친밀도10000, 물의결정3, 봉인의서1 | ENEMY NONE MOVE_SPEED PERCENT -6 / ENEMY NONE ATTACK_SPEED PERCENT -6 |
| 어린수룡 | HIGHEST | 친밀도30000, 물의정령석30, 푸른색알, 봉인의서 | ENEMY NONE MOVE_SPEED PERCENT -7 / ENEMY NONE ATTACK_SPEED PERCENT -7 / ALLY NONE MIN_POWER FLAT 100 / ALLY NONE MAX_POWER FLAT 100 |
| 어린 심아리 | LEGEND | 친밀도50000, 안정된푸른시약, 물의정령옥30, 봉인의서3, 푸른색알 | ENEMY NONE MOVE_SPEED PERCENT -8 / ENEMY NONE ATTACK_SPEED PERCENT -8 / ALLY ADAPTIVE ELEMENT_VALUE FLAT 5 / ALLY EARTH ELEMENT_VALUE FLAT 2 / ALLY NONE MIN_POWER FLAT 200 / ALLY NONE MAX_POWER FLAT 200 |

> 물 전설 ELEMENT_VALUE 주의:
> - `ADAPTIVE FLAT 5`: 불·뇌·바람·물 속성 용병에게 +5
> - `EARTH FLAT 2`: 땅 속성 용병에게만 +2 (게임 정책상 땅속성 우위로 인한 예외)

#### 바람 정령

| name | grade | acquireCondition | buffs |
|------|-------|-----------------|-------|
| 씽씽이 | LOWER | 백은색 알(유료) | ALLY NONE MOVE_SPEED PERCENT 2 / ALLY NONE ATTACK_SPEED PERCENT 2 |
| 바람돌이 | MIDDLE | 친밀도3000, 바람의결정1 | ALLY NONE MOVE_SPEED PERCENT 4 / ALLY NONE ATTACK_SPEED PERCENT 4 |
| 풍백 | UPPER | 친밀도10000, 바람의결정3, 봉인의서1 | ALLY NONE MOVE_SPEED PERCENT 6 / ALLY NONE ATTACK_SPEED PERCENT 6 |
| 어린비호 | HIGHEST | 친밀도30000, 바람의정령석30, 백은색알, 봉인의서 | ALLY NONE MOVE_SPEED PERCENT 8 / ALLY NONE ATTACK_SPEED PERCENT 8 / ENEMY NONE MOVE_SPEED PERCENT -2 / ENEMY NONE ATTACK_SPEED PERCENT -2 |
| 어린 각웅 | LEGEND | 친밀도50000, 안정된초록시약, 바람의정령옥30, 봉인의서3, 백은색알 | ALLY NONE MOVE_SPEED PERCENT 10 / ALLY NONE ATTACK_SPEED PERCENT 10 / ALLY NONE CRITICAL_CHANCE PERCENT 2 / ENEMY NONE MOVE_SPEED PERCENT -3 / ENEMY NONE ATTACK_SPEED PERCENT -3 |

#### 불 정령

| name | grade | acquireCondition | buffs |
|------|-------|-----------------|-------|
| 불덩이 | LOWER | 붉은색 알(유료) | ALLY NONE MIN_POWER FLAT 100 / ALLY NONE MAX_POWER FLAT 100 |
| 불꽃돌이 | MIDDLE | 친밀도3000, 불의결정1 | ALLY NONE MIN_POWER FLAT 200 / ALLY NONE MAX_POWER FLAT 200 |
| 불여우 | UPPER | 친밀도10000, 불의결정3, 봉인의서1 | ALLY NONE MIN_POWER FLAT 300 / ALLY NONE MAX_POWER FLAT 300 |
| 어린화룡 | HIGHEST | 친밀도30000, 불의정령석30, 붉은색알, 봉인의서 | ALLY NONE MIN_POWER FLAT 500 / ALLY NONE MAX_POWER FLAT 500 / ALLY NONE MOVE_SPEED PERCENT 2 / ALLY NONE ATTACK_SPEED PERCENT 2 |
| 어린 불사모 | LEGEND | 친밀도50000, 안정된붉은시약, 불의정령옥30, 봉인의서3, 붉은색알 | ALLY NONE MIN_POWER FLAT 600 / ALLY NONE MAX_POWER FLAT 600 / ALLY NONE ATTACK_SPEED PERCENT 3 / ALLY NONE MOVE_SPEED PERCENT 3 / ALLY NONE SKILL_DAMAGE_PERCENT PERCENT 5 |

#### 번개 정령

| name | grade | acquireCondition | buffs |
|------|-------|-----------------|-------|
| 묘아 | LOWER | 황금색 알(유료) | ALLY NONE MAGIC_RESISTANCE PERCENT 10 |
| 말붕이 | MIDDLE | 친밀도3000, 뇌전의결정1 | ALLY NONE MAGIC_RESISTANCE PERCENT 15 |
| 천둥이(지상) | UPPER | 친밀도10000, 뇌전의결정3, 봉인의서1 | ALLY NONE MAGIC_RESISTANCE PERCENT 20 |
| 어린 록아 | HIGHEST | 친밀도30000, 뇌전의정령석30, 황금색알, 봉인의서 | ALLY NONE MAGIC_RESISTANCE PERCENT 25 / ALLY NONE HITTING_RESISTANCE PERCENT 10 |
| 어린 전비 | LEGEND | 친밀도50000, 안정된노란시약, 뇌전의정령옥30, 봉인의서3, 황금색알 | ALLY NONE MAGIC_RESISTANCE PERCENT 30 / ALLY NONE HITTING_RESISTANCE PERCENT 15 / ALLY NONE ALL_STAT FLAT 800 / ALLY NONE MP_RECOVERY FLAT 50 |

> 번개 전설 `specialEffectNote`: "ALL_STAT 800은 근사값. 원본: 2초당 전체스텟+100(최대 1500). UI에 ※근사값 표시 필요"

#### 땅 정령

| name | grade | acquireCondition | buffs |
|------|-------|-----------------|-------|
| 도치 | LOWER | 초록색 알(유료) | ALLY NONE HITTING_RESISTANCE PERCENT 10 |
| 아목 | MIDDLE | 친밀도3000, 땅의결정1 | ALLY NONE HITTING_RESISTANCE PERCENT 15 |
| 하루방 | UPPER | 친밀도10000, 땅의결정3, 봉인의서1 | ALLY NONE HITTING_RESISTANCE PERCENT 20 |
| 어린독룡 | HIGHEST | 친밀도30000, 땅의정령석30, 초록색알, 봉인의서 | ALLY NONE HITTING_RESISTANCE PERCENT 25 / ALLY NONE MAGIC_RESISTANCE PERCENT 10 |
| 어린 토석동 | LEGEND | 친밀도50000, 안정된갈색시약, 땅의정령옥30, 봉인의서3, 초록색알 | ALLY NONE MAGIC_RESISTANCE PERCENT 15 / ALLY NONE HITTING_RESISTANCE PERCENT 30 / ALLY NONE HP_RECOVERY FLAT 500 |

> 땅 전설 `specialEffectNote`: "타 정령 버프 ×2 적용(공속·이속 제외). SpiritBuffCalculator에서 처리. 이 정령 본인 버프는 ×2 대상 아님"

---

## 6. 유효성 검증 규칙 (서비스 레이어)

```
- 덱당 정령 장착 최대 2개
- 동일 정령(같은 id) 중복 장착 불가
- 위반 시 IllegalArgumentException
```

---

## 7. UI 표시 규칙

- 정령 레이블: `{nature.displayName} {grade.displayName} {name}` — 예: `풍 전설 어린 각웅`
- 번개 전설 ALL_STAT 버프 옆에 `※ 근사값` 표시
- 땅 전설 장착 시 타 정령 버프 수치 옆에 `(×2 적용됨)` 표시 권장
