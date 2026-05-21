# 주인공(PlayerCharacter) 엔티티 및 계산 스펙

## 0. 이 문서의 목적

Claude Code가 이 문서만 읽고 아래를 구현할 수 있도록 작성되었다.

1. `MercenaryType` enum 추가
2. `PlayerCharacterDetail` JPA 엔티티 (Mercenary 1:1)
3. 주인공 특성 데이터 시딩 (기존 `MercenaryCharacteristic` + `MercenaryCharacteristicLevel` 재사용)
4. 계산 서비스 (`PlayerCharacterBuffCalculator`)

---

## 1. 도메인 개요

### 전직 구조

```
노말 → 1차전직 → 2차전직
조선/일본/중국/대만: 노말/1차/2차 모두 존재
인도: 노말 없음. 귀화(아이템)로 1차 이후 진입
```

### MVP 범위

```
2차전직만 구현. 노말/1차전직 데이터는 저장하되 계산기는 2차전직만.
```

### 특성 포인트

```
총 16포인트 (레벨 200~260 구간에서만 부여)
  200~250: 10단위로 1개씩 (200/210/.../250 = 6개)
  251~260: 1단위로 1개씩 (251/252/.../260 = 10개)
  총 16개

특성 구조:
  하위 특성: point=1, 포인트 1개당 레벨 1, 최대 레벨 5
  상위 특성: point=2, 포인트 2개당 레벨 1, 최대 레벨 5 (하위 1개 이상 선행 필요)
  총 필요 포인트: 30 → 16포인트로 전부 찍기 불가. 의도된 설계.
```

### 국가별 속성 버프 (2차전직 시 자동 적용)

```
조선 → 화속성(FIRE)    용병 ELEMENT_VALUE +5
일본 → 수속성(WATER)   용병 ELEMENT_VALUE +5
중국 → 풍속성(WIND)    용병 ELEMENT_VALUE +5
대만 → 뇌속성(THUNDER) 용병 ELEMENT_VALUE +5
인도 → 토속성(EARTH)   용병 ELEMENT_VALUE +3  ← 예외 (땅속성 정책)
```

---

## 2. Enum 정의

### 2-1. MercenaryType

```java
package org.example.gersangtrade.domain.catalog.enums;

public enum MercenaryType {
    MAIN_CHARACTER,          // 주인공
    NORMAL_MERCENARY,        // 일반 용병
    HEAVENLY_KING,           // 일반 사천왕
    MYUNGWANG,               // 일반 명왕
    AWAKENED_HEAVENLY_KING,  // 각성 사천왕
    AWAKENED_MYUNGWANG,      // 각성 명왕
    LEGEND_GENERAL,          // 전설장수
}
```

> `Mercenary` 엔티티에 `MercenaryType type` 컬럼을 추가한다.

### 2-2. Nation

```java
package org.example.gersangtrade.domain.catalog.enums;

public enum Nation {
    JOSEON("조선"),
    JAPAN("일본"),
    CHINA("중국"),
    TAIWAN("대만"),
    INDIA("인도"),
    MONGOL("몽골"),   // 크롤링 용병 데이터에서 추가됨
    NONE("-");        // 국가 구분 없는 용병(정령·마수류) 대체값

    private final String displayName;
    Nation(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
```

> `MONGOL`과 `NONE`은 주인공 계산기 대상이 아니나, gerniverse 크롤링 용병 데이터를 수용하기 위해 추가됨.
> 이로 인해 `getNationBuff`에 `default` 케이스가 필요하다 (아래 4절 참고).

### 2-3. JobType

```java
package org.example.gersangtrade.domain.catalog.enums;

public enum JobType {
    NORMAL,   // 노말 (인도는 불가)
    FIRST,    // 1차전직
    SECOND,   // 2차전직 — MVP 계산 대상
}
```

### 2-4. Gender

```java
package org.example.gersangtrade.domain.catalog.enums;

public enum Gender {
    MALE, FEMALE
}
```

---

## 3. 엔티티 정의

### 3-1. Mercenary 수정

```java
// Mercenary 엔티티에 아래 컬럼 추가
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private MercenaryType mercenaryType;  // 기존 Mercenary에 추가
```

### 3-2. PlayerCharacterDetail (신규)

```java
package org.example.gersangtrade.domain.catalog.entity;

@Entity
@Table(name = "player_character_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerCharacterDetail {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id", nullable = false, unique = true)
    private Mercenary mercenary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Nation nation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;
}
```

---

## 4. 계산 서비스

### PlayerCharacterBuffCalculator

```java
package org.example.gersangtrade.domain.calculator.service;

/**
 * 주인공 2차전직 버프를 계산한다.
 *
 * 버프 종류:
 *   1. 국가 속성 버프 — 해당 속성 용병에게 ELEMENT_VALUE +n (자동 적용)
 *   2. 특성 버프 — statType 매핑된 특성만 계산, 항상 적용 가정
 *
 * jobType != SECOND 이면 계산 스킵.
 */
@Service
public class PlayerCharacterBuffCalculator {

    public NationBuff getNationBuff(Nation nation) {
        return switch (nation) {
            case JOSEON -> new NationBuff(Element.FIRE,    5f);
            case JAPAN  -> new NationBuff(Element.WATER,   5f);
            case CHINA  -> new NationBuff(Element.WIND,    5f);
            case TAIWAN -> new NationBuff(Element.THUNDER, 5f);
            case INDIA  -> new NationBuff(Element.EARTH,   3f);
            // MONGOL·NONE은 주인공 국가가 아니므로 버프 없음
            default     -> new NationBuff(Element.NONE,    0f);
        };
    }

    public record NationBuff(Element element, float value) {}

    /**
     * 특성 버프 계산.
     * statType null인 특성(상재 등 전투 무관)은 스킵.
     * 상위 특성(point=2): 레벨 = floor(포인트 / 2)
     * 하위 특성(point=1): 레벨 = 포인트
     *
     * amountValue는 각 레벨의 누적 총합값이므로 정확히 해당 레벨(== level)만 사용한다.
     * (<= level 로 합산하면 중복 계산됨. 예: 저항깎 레벨5=10 → 2+4+6+8+10=30 이 되어 잘못됨)
     */
    public Map<StatType, Float> calculateCharacteristicBuffs(
            List<MercenaryCharacteristic> characteristics,
            Map<Long, Integer> pointsAllocated) {

        Map<StatType, Float> result = new HashMap<>();

        for (MercenaryCharacteristic characteristic : characteristics) {
            int points = pointsAllocated.getOrDefault(characteristic.getId(), 0);
            if (points == 0) continue;

            Integer point = characteristic.getPoint();
            if (point == null) continue;

            int level = point == 2 ? points / 2 : points;
            if (level == 0) continue;

            // levels는 repository로 조회 (MercenaryCharacteristic에 getLevels() 없음)
            List<MercenaryCharacteristicLevel> levels =
                    levelRepository.findByCharacteristicId(characteristic.getId());

            for (MercenaryCharacteristicLevel l : levels) {
                if (l.getStatType() == null) continue;
                if (l.getLevel() != level) continue;  // 정확히 해당 레벨만 (누적값이므로)
                if (l.getAmountValue() == null) continue;
                result.merge(l.getStatType(), l.getAmountValue(), Float::sum);
            }
        }

        return result;
    }
}
```

---

## 5. 특성 전체 데이터

### 5-1. StatType 매핑 규칙

```
"저항력 감소"         → RESIST_PIERCE,  target=ENEMY
"아군 공격력 증가"    → MIN_POWER + MAX_POWER, target=ALLY (각각 저장)
"적군 공격력 감소"    → MIN_POWER + MAX_POWER, target=ENEMY (음수값)
"이속 감소"           → MOVE_SPEED,     target=ENEMY (음수값)
"사거리 감소"         → SKILL_RANGE,    target=ENEMY (음수값)
"아군 피해 증가"      → DAMAGE_PERCENT, target=ALLY
전투 무관 특성        → statType = null (상재 등)
```

### 5-2. 조선

**국가 특성**: 화속성(FIRE) 용병 ELEMENT_VALUE +5

#### 조선 남 (신궁)

```
하위 / 확산 / point=1 / required=null
  label: 피해      → DAMAGE_PERCENT PERCENT_ADD ALLY
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%
  label: 지속시간  → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 1/ 2/ 2/ 3초

상위 / 충격 / point=2 / required=확산
  label: 확률      → statType=null (이동불가 확률)
    level: 1/ 2/ 3/ 4/ 5
    value:20/30/40/50/60%
  label: 지속시간  → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 1/ 2/ 2/ 3초

하위 / 폭발 / point=1 / required=null
  label: 연사 피해량 → statType=null (본인 스킬)
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%

상위 / 정밀 / point=2 / required=폭발
  label: 저항력 감소 → RESIST_PIERCE FLAT ENEMY
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 4/ 6/ 8/10
```

#### 조선 여 (포수)

```
하위 / 집중 / point=1 / required=null
  label: 피해 → statType=null (본인 스킬)
    level: 1/  2/  3/  4/   5
    value:10/ 25/ 50/ 75/ 100%

상위 / 산탄 / point=2 / required=집중
  label: 추가피해 확률 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value:50/55/60/65/70%
  label: 산탄피해량   → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value:10/15/20/25/30%

하위 / 과녁 / point=1 / required=null
  label: 피해량 → statType=null (본인 스킬)
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%

상위 / 관통 / point=2 / required=과녁
  label: 방패 무효화 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%

독립 / 상재 / point=1 / required=null
  label: 상점판매액 → statType=null (전투 무관)
    level: 1/ 2/ 3/ 4/ 5
    value:10/15/20/25/30%
```

---

### 5-3. 일본

**국가 특성**: 수속성(WATER) 용병 ELEMENT_VALUE +5

#### 일본 남 (사무라이)

```
하위 / 집중 / point=1 / required=null
  label: 발도 피해량 → statType=null (본인 스킬)
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%

상위 / 치명상 / point=2 / required=집중
  label: 3배 피해 확률 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%
  label: 기절 확률     → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value:20/25/30/35/40%

하위 / 난도 / point=1 / required=null
  label: 피해 증가         → statType=null (본인 스킬)
    level: 1/ 2/ 3/ 4/ 5
    value:10/25/40/55/70%
  label: 표식 필요 데미지  → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%

상위 / 교활 / point=2 / required=난도
  label: 저항력 감소 → RESIST_PIERCE FLAT ENEMY
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 4/ 6/ 8/10
```

#### 일본 여 (무녀)

```
하위 / 분노 / point=1 / required=null
  label: 빙룡출사 피해량 → statType=null (본인 스킬)
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%

상위 / 혹한 / point=2 / required=분노
  label: 빙룡 강림 확률 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value:30/35/40/45/50%
  label: 빙결 확률      → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 4/ 6/ 8/10%

하위 / 마력주입 / point=1 / required=null
  label: 피해흡수율 → statType=null (본인 스킬)
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/50/70%

상위 / 마력사출 / point=2 / required=마력주입
  label: 마법력 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%
```

---

### 5-4. 중국

**국가 특성**: 풍속성(WIND) 용병 ELEMENT_VALUE +5

#### 중국 남 (무술가)

```
하위 / 충격 / point=1 / required=null
  label: 공진각 피해량 → statType=null (본인 스킬)
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%

상위 / 공진 / point=2 / required=충격
  label: 공진각 피해 범위 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 1/ 2/ 2/ 3칸
  label: 기절 확률        → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 5/ 5/10/10/15%

하위 / 저항 / point=1 / required=null
  label: 피해 감소량 → statType=null (본인 방어)
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 1/ 2/ 2/ 3%

상위 / 기합 / point=2 / required=저항
  label: 저항력 감소 → RESIST_PIERCE FLAT ENEMY
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 4/ 6/ 8/10
```

#### 중국 여 (강신사)

```
하위 / 축복 / point=1 / required=null
  label: 회복량    → statType=null (본인 스킬)
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%
  label: 유지시간  → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 5/10/15/20/25초

상위 / 가호 / point=2 / required=축복
  label: 마법력 추가 회복량 → statType=null
    level:  1/   2/   3/   4/   5
    value:0.1/ 0.2/ 0.3/ 0.4/ 0.5%

하위 / 의지 / point=1 / required=null
  label: 보호막 체력 증가 → statType=null (본인 스킬)
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%

상위 / 확산 / point=2 / required=의지
  label: 보호막 적용 범위 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 2/ 3/ 4/ 5칸
```

---

### 5-5. 대만

**국가 특성**: 뇌속성(THUNDER) 용병 ELEMENT_VALUE +5

#### 대만 여 (야수소환사)

```
하위 / 강인함 / point=1 / required=null
  label: 야수 능력치 → statType=null (소환수 강화)
    level: 1/ 2/ 3/ 4/ 5
    value:20/40/60/80/100%
  label: 저항력     → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%

상위 / 독수리 훈련 / point=2 / required=강인함
  label: 공격력 추가 감소 → statType=null (소환수 스킬)
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%
  label: 기절 확률        → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 4/ 6/ 8/10%

하위 / 칼날비 / point=1 / required=null
  label: 회전칼날 데미지 → statType=null (본인 스킬)
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%
  label: 감전 확률       → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 5/10/15/20/25%

상위 / 비검 / point=2 / required=칼날비
  label: 저항력 → RESIST_PIERCE FLAT ENEMY
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 4/ 6/ 8/10
```

#### 대만 남 (도사)

```
하위 / 영험 / point=1 / required=null
  label: 체력회복량 → statType=null (수호물 범위, 항상 적용 가정)
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%

상위 / 활성화 / point=2 / required=영험
  label: 공격력 → MIN_POWER PERCENT_ADD ALLY + MAX_POWER PERCENT_ADD ALLY
    level: 1/ 2/ 3/ 4/ 5
    value: 5/10/15/20/25%

하위 / 무기력 / point=1 / required=null
  label: 적군 공격력 감소 → MIN_POWER PERCENT_ADD ENEMY + MAX_POWER PERCENT_ADD ENEMY (음수)
    level:  1/  2/  3/  4/  5
    value: -5/-15/-25/-35/-45%

상위 / 혼란 / point=2 / required=무기력
  label: 적군 이동속도 감소 → MOVE_SPEED PERCENT_ADD ENEMY (음수)
    level:  1/  2/  3/  4/  5
    value: -1/ -5/-10/-15/-20%
  label: 적군 사거리 감소   → SKILL_RANGE FLAT ENEMY (음수)
    level: 1/ 2/ 3/ 4/ 5
    value: 0/ 0/-1/-1/-2
```

---

### 5-6. 인도

**국가 특성**: 토속성(EARTH) 용병 ELEMENT_VALUE +3 (※ 땅속성 정책으로 타국보다 낮음)

#### 인도 남 (무투가)

```
하위 / 무쇠주먹 / point=1 / required=null
  label: 팔방타 피해  → statType=null (본인 스킬)
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%
  label: 기절 확률   → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 4/ 6/ 8/10%

상위 / 무력화 / point=2 / required=무쇠주먹
  label: 3배 피해 확률 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 5/10/20/30/40%

하위 / 일점사 / point=1 / required=null
  label: 표식 피해량 증가 → DAMAGE_PERCENT PERCENT_ADD ALLY
    level: 1/ 2/ 3/ 4/ 5
    value:10/15/20/25/35%

상위 / 원한 / point=2 / required=일점사
  label: 표식 유지시간 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 2/ 3/ 4/ 5초
```

#### 인도 여 (악사)

```
하위 / 화음 / point=1 / required=null
  label: 아군 공격력 → MIN_POWER PERCENT_ADD ALLY + MAX_POWER PERCENT_ADD ALLY
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%
  label: 적군 공격력 → MIN_POWER PERCENT_ADD ENEMY + MAX_POWER PERCENT_ADD ENEMY (음수)
    level:  1/  2/  3/  4/  5
    value: -5/ -7/-10/-15/-20%

상위 / 불협화음 / point=2 / required=화음
  label: 찬가 데미지   → statType=null (본인 스킬)
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%
  label: 찬가 유지시간 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 2/ 3/ 4/ 5초

하위 / 리듬 / point=1 / required=null
  label: 정화 회복량 → statType=null (본인 스킬)
    level: 1/ 2/ 3/ 4/ 5
    value:10/20/30/40/50%

상위 / 박자 / point=2 / required=리듬
  label: 상태 면역 → statType=null
    level: 1/ 2/ 3/ 4/ 5
    value: 1/ 2/ 3/ 4/ 5초
```

---

## 6. 유효성 검증 규칙

```
- 인도: JobType.NORMAL 불가
- 2차전직 특성 포인트 총합 <= 16
- 상위 특성(point=2): 대응 하위 특성 1포인트 이상 선행 필요
- 상위 특성 포인트는 2의 배수로만 배분 가능
```

---

## 7. 데이터 적재 방식

크롤링 없음. 10개 주인공 데이터를 `PlayerCharacterSeeder`(`ApplicationRunner`, `@Order(5)`)로 직접 시딩.
특성은 기존 `MercenaryCharacteristic` + `MercenaryCharacteristicLevel` 구조 재사용.
`statType=null`인 레벨 행은 저장하되 계산기에서 스킵.

- 시딩 중복 방지: `mercenaryRepository.findByName("조선 신궁").isPresent()`로 체크 후 skip
- label × level 조합 중복 방지: `findByCharacteristicIdAndLabelAndLevel`로 각 행 개별 체크
- 아군/적군 공격력(MIN_POWER + MAX_POWER)은 label을 "아군 최소공격력" / "아군 최대공격력"으로 분리 저장 (unique 제약 회피)

---

## 8. UI 표시 규칙

```
국가 속성 버프:
  조선/일본/중국/대만: "2차전직 시 {속성} 용병 속성값 +5"
  인도: "2차전직 시 땅속성 용병 속성값 +3 (※ 땅속성 정책)"

특성 레벨 계산:
  하위(point=1): 레벨 = 배분 포인트
  상위(point=2): 레벨 = floor(배분 포인트 / 2)
```