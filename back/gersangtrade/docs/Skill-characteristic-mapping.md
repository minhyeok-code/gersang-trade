# 사천왕/명왕 스킬 특성 매핑 스펙

## 0. 이 문서의 목적

기존 스펙 파일에서 `statType=null (본인 스킬 강화)`로 스킵 처리된 특성들을
DPS 계산기에 연결하기 위한 변경 사항을 정의한다.

Claude Code는 이 문서와 기존 스펙 파일(heavenly-king-spec.md, awakened-heavenly-king-spec.md,
myungwang-spec.md, awakened-myungwang-spec.md)을 함께 읽고 구현해야 한다.

---

## 1. 핵심 변경사항

### MercenaryCharacteristicLevel에 targetSkillName 추가

```java
@Entity
@Table(name = "mercenary_characteristic_level")
public class MercenaryCharacteristicLevel {

    // ... 기존 필드 유지 ...

    /**
     * 특정 스킬에만 적용되는 특성 효과일 경우 스킬명을 지정.
     * null이면 해당 용병의 모든 스킬에 적용 (일반 케이스).
     * 스킬명은 SkillCoefficient의 skill_name 컬럼과 일치해야 한다.
     *
     * 예시:
     *   "염룡살진" → 염룡살진 스킬 계수에만 적용
     *   "화상"     → 화상 도트 계수에만 적용
     */
    @Column(nullable = true)
    private String targetSkillName;
}
```

### statType 변경 규칙

```
기존: label: 스킬명 데미지 → statType=null (계산기 스킵)
변경: label: 스킬명 데미지 → statType=SKILL_DAMAGE_PERCENT + targetSkillName="스킬명"

SKILL_DAMAGE_PERCENT: 해당 스킬의 base_damage에 % 증가 적용
target: SELF (해당 용병 본인에게만)
valueType: PERCENT_ADD
```

### 계산기 적용 방식

```
// targetSkillName이 null인 경우 (일반 SKILL_DAMAGE_PERCENT)
→ 해당 용병의 모든 스킬 base_damage에 적용

// targetSkillName이 있는 경우
→ SkillCoefficient.skillName == targetSkillName 인 스킬에만 적용

dps 계산:
  buffed_damage = base_damage
                × (1 + Σ DAMAGE_PERCENT(해당 속성) / 100)
                × (1 + Σ SKILL_DAMAGE_PERCENT(targetSkillName 일치 or null) / 100)
```

---

## 2. 일반 사천왕 변경사항

### 2-1. 지국천왕 (FIRE)

```
특성1 / 겁화
  label: 염룡살진 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "염룡살진"
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
    value: 10/20/30/40/50/60/75/95/120/150%

특성2 / 화상
  label: 화상 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "화상"
    level:  1/ 2/ 3/  4/  5/  6/  7/  8/  9/  10
    value: 20/40/60/ 80/100/120/140/160/180/200%
```

### 2-2. 광목천왕 (WIND)

```
특성1 / 광풍
  label: 풍룡섬 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "풍룡섬"
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
    value: 10/20/30/40/50/60/75/95/120/150%
```

### 2-3. 증장천왕 (THUNDER)

```
특성1 / 감전
  label: 뇌룡격 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "뇌룡격"
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
    value: 10/20/30/40/50/60/75/95/120/150%

특성2 / 충격파
  label: 3배 피해 확률 → statType=null 유지 (확률값, 계산기 대상 아님)
```

### 2-4. 다문천왕 (WATER)

```
특성1 / 강화
  label: 흑귀 능력치 → statType=null 유지 (소환수 강화, 계산기 대상 아님)

특성2 / 집중
  label: 눈사태 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "눈사태"
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/  9/ 10
    value: 10/20/30/40/50/60/75/95/120/150%
```

---

## 3. 각성 사천왕 변경사항

### 3-1. 각성 지국천왕 (FIRE)

```
하위 / 겁화
  label: 염화무극진 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "염화무극진"
    level: 1/ 2/ 3/  4/  5
    value:20/40/70/100/150%

상위 / 경안
  label: 데미지 → statType=null 유지 (기절 연계 데미지, 계산기 대상 아님)
  label: 기절   → statType=null 유지

하위 / 화상
  label: 화상 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "화상"
    level:  1/ 2/  3/  4/  5
    value: 20/50/100/150/200%

상위 / 층화
  label: 중첩 데미지 증가율 → statType=null 유지 (중첩 메커니즘, 계산기 대상 아님)
  label: 지속시간           → statType=null 유지
```

### 3-2. 각성 광목천왕 (WIND)

```
하위 / 광풍
  label: 풍극진멸 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "풍극진멸"
    level: 1/ 2/ 3/  4/  5
    value:20/40/70/100/150%

상위 / 연파
  label: 생명력 증가    → statType=null 유지 (자신 방어)
  label: 고정 추가 피해 → statType=null 유지 (고정값, 계산기 대상 아님)

하위 / 충돌
  label: 타격저항력 → 기존 HITTING_RESISTANCE ENEMY 유지 (변경 없음)

상위 / 신보
  label: 기본 데미지 → statType=null 유지
  label: 추가 데미지 → statType=null 유지
```

### 3-3. 각성 증장천왕 (THUNDER)

```
하위 / 감전
  label: 천궁뇌격 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "천궁뇌격"
    level: 1/ 2/ 3/  4/  5
    value:20/50/70/100/150%

상위 / 전폭
  label: 전격 확률   → statType=null 유지 (확률값)
  label: 전폭 피해   → statType=null 유지
  label: 주변 데미지 → statType=null 유지

하위 / 충격파
  label: 3배 피해 확률         → statType=null 유지 (확률값)
  label: 마법저항 (공중 몬스터) → 기존 MAGIC_RESISTANCE ENEMY 유지 (변경 없음)

상위 / 원격
  label: 치명타 피해 → statType=null 유지
```

### 3-4. 각성 다문천왕 (WATER)

```
하위 / 강화
  label: 소환수 스탯 → statType=null 유지 (소환수 강화)
  label: 마법저항력  → 기존 MAGIC_RESISTANCE ENEMY 유지 (변경 없음)

상위 / 안식
  label: 청빙격류 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "청빙격류"
    level: 1/ 2/ 3/ 4/ 5
    value: 2/ 3/ 5/30/80%
  label: 피해 → statType=null 유지

하위 / 집중
  label: 청빙격류 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "청빙격류"
    level:  1/ 2/ 3/  4/  5
    value: 20/50/70/100/150%

상위 / 전심
  label: 데미지 → statType=null 유지
```

---

## 4. 명왕 변경사항

### 4-1. 항삼세명왕 (FIRE)

```
특성2 / 화신
  label: 힘, 생명력 → statType=null 유지 (소환수 강화)
  label: 흡수 피해  → statType=null 유지 (흡수 메커니즘, 계산기 대상 아님)
```

### 4-2. 군다리명왕 (THUNDER)

```
특성2 / 우뢰
  label: 우뢰 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "우뢰"
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/ 10
    value: 10/20/30/40/50/60/70/80/90/100%
  label: 유지시간 → statType=null 유지
```

### 4-3. 대위덕명왕 (WIND)

```
특성2 / 돌풍
  label: 돌풍 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "돌풍"
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/ 10
    value: 10/20/30/40/50/60/70/80/90/100%
  label: 회오리 범위 → statType=null 유지
```

### 4-4. 부동명왕 (EARTH)

```
특성1 / 집중
  label: 지속시간 → statType=null 유지

특성2 / 압박
  label: 상아감옥 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "상아감옥"
    level:  1/ 2/ 3/ 4/ 5/ 6/ 7/ 8/ 9/ 10
    value: 10/20/30/40/50/60/70/80/90/100%
  label: 속박 확률 → statType=null 유지 (확률값)
```

### 4-5. 금강야차명왕 (WATER)

```
특성1 / 신비
  label: 이전되는 지력 → 기존 INTELLECT 이전 특성 유지 (변경 없음)
  label: 회복 마법력   → statType=null 유지 (회복, 계산기 대상 아님)

특성2 / 가호
  label: 냉기 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "냉기"
    level:  1/ 2/ 3/  4/  5/  6/  7/  8/  9/  10
    value: 20/40/60/ 80/100/120/140/160/180/200%
  label: 데미지 흡수율 → statType=null 유지 (방어, 계산기 대상 아님)
```

---

## 5. 각성 명왕 변경사항

### 5-1. 각성 항삼세명왕 (FIRE)

```
상위 / 잠식
  label: 받는 데미지 증가 → statType=null 유지 (디버프 메커니즘)
  label: 중첩            → statType=null 유지

하위 / 화신
  label: 화무신 생명력 증가율  → statType=null 유지 (소환수)
  label: 화무신 피해감소 증가율 → statType=null 유지

상위 / 어령
  label: 화무신 염랑 데미지 증가 → statType=null 유지 (소환수)
```

### 5-2. 각성 군다리명왕 (THUNDER)

```
하위 / 우뢰
  label: 데미지 증가
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "우뢰"
    level:  1/ 2/  3/  4/  5
    value: 50/70/100/150/200%
  label: 저항 감소 지속시간 → statType=null 유지

상위 / 낙인
  label: 거리비례 데미지 → statType=null 유지
  label: 치명타 피해량  → statType=null 유지
```

### 5-3. 각성 대위덕명왕 (WIND)

```
하위 / 돌풍
  label: 흡풍멸진 데미지
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "흡풍멸진"
    level:  1/ 2/  3/  4/  5
    value: 50/70/100/150/200%
  label: 범위 증가 → statType=null 유지

상위 / 분천
  label: 시전속도    → statType=null 유지
  label: 폭발 데미지 → statType=null 유지
```

### 5-4. 각성 금강야차명왕 (WATER)

```
하위 / 가호
  label: 데미지 증가
    statType=null → SKILL_DAMAGE_PERCENT / PERCENT_ADD / SELF
    targetSkillName: "가호"
    level:  1/ 2/  3/  4/  5
    value: 50/70/100/150/200%
  label: 피해감소율 증가 → statType=null 유지 (방어)

상위 / 격노
  label: 중첩 당 데미지 → statType=null 유지 (중첩 메커니즘)
  label: 최대 중첩 횟수 → statType=null 유지
  label: 피격 횟수      → statType=null 유지
  label: 사거리 증가    → statType=null 유지
```

---

## 6. statType=null 유지 기준

아래 케이스는 DPS 계산기에서 처리 불가하므로 `statType=null` 유지:

```
- 소환수 강화 (흑귀, 화무신 등)
- 확률값 (3배 피해 확률, 기절 확률 등)
- 지속시간 / 범위 / 사거리 등 수치
- 회복 관련 (마법력 회복 등)
- 중첩 메커니즘
- 고정 추가 피해 (계수 역산 불가)
- 흡수 메커니즘
```

---

## 7. 주의사항

```
1. targetSkillName은 SkillCoefficient.skillName과 정확히 일치해야 함
   시딩 전 SkillCoefficient 테이블에서 스킬명 확인 필요

2. 각성 다문천왕 청빙격류는 하위(집중)와 상위(안식) 두 특성이
   동일한 targetSkillName="청빙격류"를 가짐
   → 계산기에서 두 특성 SKILL_DAMAGE_PERCENT 가산 합산 후 1회 적용

3. targetSkillName이 null이면서 statType=SKILL_DAMAGE_PERCENT인 경우
   → 해당 용병 모든 스킬에 적용 (현재 사천왕/명왕 케이스에는 없음)

4. 기존 스펙 파일의 statType=null 항목 중 이 문서에서 명시된 것만 변경
   나머지는 그대로 유지
```