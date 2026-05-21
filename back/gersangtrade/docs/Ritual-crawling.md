# 주술 크롤러 설계 문서

> 거상 거래 플랫폼 — 주술 크롤링 설계 확정본  
> 작성일: 2026-04-29

---

## 목차
1. [크롤링 대상](#1-크롤링-대상)
2. [페이지별 HTML 구조 분석](#2-페이지별-html-구조-분석)
3. [저장 대상 엔티티](#3-저장-대상-엔티티)
4. [파싱 전략](#4-파싱-전략)
5. [예외 처리](#5-예외-처리)
6. [실행 순서 및 의존성](#6-실행-순서-및-의존성)

---

## 1. 크롤링 대상

### 1.1 대상 페이지

| 페이지 | URL | 크롤링 대상 |
|---|---|---|
| 4등급 황색 | `/qianghua/4.asp` | 고구려의, 담덕만 필터링 |
| 5등급 | `/qianghua/5.asp` | 전체 데이터 |

### 1.2 황색 페이지 필터링 기준

```
황색 페이지에는 여러 주술이 있지만 거래가치 있는 것은 고구려의/담덕만.
이름 기준으로 필터링:
  포함: "고구려의", "담덕"
  나머지: 스킵
```

---

## 2. 페이지별 HTML 구조 분석

### 2.1 황색 페이지 (고구려의 / 담덕)

```
구조: <tr> 하나 = 주술 1개

1열 (주술명):
  <td class="hang">
    <img ...>
    고구려의 (또는 담덕)
    주술비법 lv210 lv
  </td>

2열 (재료): 크롤링 불필요

3열 (성공 시 Stats):
  <td>
    힘+50, 민첩+50, 생명+25, 지력+25, 방어력+100, 데미지+25~35
  </td>

4열 (적용 가능 아이템):
  <td>
    <strong>아이템</strong>: 태황투구, 태황갑옷, 두이갑옷, 두이투구
    <strong>드랍</strong>: ... (무시)
    <strong>상점가</strong>: ... (무시)
  </td>
```

**황색 주술 특이사항:**
- Gem이 없는 주술이다 (5단계와 다름)
- Gem을 생성하지 않음 — Ritual 쪽은 아무것도 할 필요 없음
- 세트 효과(RitualSetEffect)가 없음 — RitualSetEffect 저장 안 함
- 재료 데이터는 크롤링하지 않는다

---

### 2.2 5단계 페이지

```
구조: <tr> 하나 = 주술 1개 (북두칠성 행 포함)

1열 (주술명):
  <td class="hang">
    <img ...>
    천권
    150lv
  </td>

2열 (Gem명):
  <td>강화된 사금석</td>

3열 (재료): 크롤링 불필요

4열 (성공 시 Stats):
  <td>민첩+120</td>

5열 (적용 가능 아이템 + 세트 효과):
  <td>
    <strong>보석추가 아이템</strong>: 수라, 바투, 증장천왕, 군다리명왕 장갑, 요대, 신발
    <strong>3세트</strong>: 민첩+150, 데미지+2%
    <strong>5세트</strong>: 민첩+150, 데미지+3%
    <strong>드랍</strong>: ... (무시)
    <strong>상점가</strong>: ... (무시)
  </td>

--- 북두칠성 행 (바로 다음 <tr>) ---

1열: "북두칠성" (텍스트로 판별)
2열: "천권 대성공" (직전 주술명 참조)
3열: 재료 없음 ("-")
4열: 민첩+180 (대성공 Stats)
5열:
  <strong>보석추가 아이템</strong>: 증장천왕, 군다리명왕 장갑, 요대, 신발
  <strong>3세트</strong>: 민첩+250, 뇌속성값+2
  <strong>5세트</strong>: 민첩+250, 뇌속성값+3
```

---

## 3. 저장 대상 엔티티

### 3.1 Ritual

```
저장 대상:
  displayName   (주술명 — 천추, 천권, 고구려의, 담덕 등)

저장 제외:
  재료
  드랍 몬스터
  상점가
```

### 3.2 Gem

```
5단계 주술에서만 저장 (황색 주술은 Gem 생성 안 함):
  name     = "사금석", "흑요석" 등 (기본 보석명만 추출)
  gemGrade = ENHANCED (강화됨)
  ritual   = 해당 주술 FK

파싱 규칙:
  "강화된 사금석" → name="사금석", gemGrade=ENHANCED
  "강화된 흑요석" → name="흑요석", gemGrade=ENHANCED

주의사항:
  강인한/신속한/끈기의/신비한의 Gem은 구데이터에 "+5" 표기가 있으나 무시
  현재는 그냥 "강화된 00석"으로 처리
```

### 3.3 RitualStat (주술 성공 시 Stats)

```
Ritual에 연결되는 스탯 엔티티 (ItemStat와 별개):
  ritual    : Ritual FK
  outcome   : RitualOutcome (SUCCESS | GREAT_SUCCESS)
  statType  : StatType (Enum)
  statValue : Integer
  statUnit  : StatUnit (Enum)
  element   : Element (NONE if not applicable)

outcome 구분:
  outcome = SUCCESS       → 일반 주술 성공 스탯
  outcome = GREAT_SUCCESS → 북두칠성 대성공 스탯

파싱 규칙 (crawler-design.md 3.1 참고):
  "힘+50"       → STRENGTH,          50, FLAT,    NONE
  "민첩+50"     → DEXTERITY,         50, FLAT,    NONE
  "생명+25"     → VITALITY,          25, FLAT,    NONE
  "지력+25"     → INTELLECT,         25, FLAT,    NONE
  "방어력+100"  → DEFENSE,          100, FLAT,    NONE
  "데미지+25~35" → 오픈 이슈 참고
```

### 3.4 RitualApplicability

```
"보석추가 아이템" 또는 "아이템" 태그 이후 텍스트 파싱:

파싱 규칙 (crawler-design.md 참고):
  DB 세트명과 매칭 → 해당 세트 전슬롯 RitualApplicability 저장
  슬롯명(장갑/요대/신발 등) → 무시

예외:
  강인한 → 고급천왕검  (DB 조회 후 연결)
  신속한 → 고급천왕주  (DB 조회 후 연결)
  끈기의 → 고급천왕극  (DB 조회 후 연결)
  신비한 → 고급천왕비  (DB 조회 후 연결)
  → 데이터에 명시 안 되어있음. RitualApplicability 저장하지 않음. 수동으로 나중에 INSERT.
```

### 3.5 RitualSetEffect

```
5단계 주술에서만 저장 (황색 주술은 세트 효과 없음 — RitualSetEffect 저장 안 함)

"3세트"/"5세트" 태그 파싱:
  outcome = SUCCESS       → 일반 주술의 세트 효과
  outcome = GREAT_SUCCESS → 북두칠성 행의 세트 효과

예시:
  "3세트: 민첩+150, 데미지+2%"
  → RitualSetEffect(ritual=천권, outcome=SUCCESS, requiredRitualPieces=3, DEXTERITY, 150, FLAT)
  → RitualSetEffect(ritual=천권, outcome=SUCCESS, requiredRitualPieces=3, DAMAGE_PERCENT, 2, PERCENT)

  북두칠성 "3세트: 민첩+250, 뇌속성값+2"
  → RitualSetEffect(ritual=천권, outcome=GREAT_SUCCESS, requiredRitualPieces=3, DEXTERITY, 250, FLAT)
  → RitualSetEffect(ritual=천권, outcome=GREAT_SUCCESS, requiredRitualPieces=3, ELEMENT_VALUE, 2, FLAT, element=THUNDER)
```

---

## 4. 파싱 전략

### 4.1 북두칠성 행 판별

```
<tr> 순회 중:
  1열 텍스트가 "북두칠성" → 대성공 행
  1열 텍스트가 "북두칠성" 아님 → 일반 주술 행

북두칠성 행 처리:
  별도 Ritual 저장 안 함
  직전에 저장한 Ritual의 ritual_id 참조
  outcome = GREAT_SUCCESS로 저장
```

### 4.2 Gem명 파싱

```
"강화된 사금석" → "강화된 " 제거 → "사금석"
"강화된 흑요석" → "강화된 " 제거 → "흑요석"

광개토 주술 예외:
  2열에 "태황반지"가 나옴
  → Gem이 아니라 RitualApplicability 대상 아이템
  → Gem 생성 안 함
  → DB에서 "태황반지" EquipmentItem 조회 후 RitualApplicability 저장
  → 못 찾으면 스킵 + 경고 로그
```

### 4.3 RitualApplicability 파싱

```
"보석추가 아이템:" 또는 "아이템:" 이후 텍스트 파싱

토큰 처리:
  DB EquipmentSet명과 매칭 → 해당 세트 전슬롯 저장
  슬롯명(장갑/요대/신발 등) → 무시
  DB EquipmentItem명과 매칭 → 단품 아이템 저장 (태황반지 등)
  매칭 안 됨 → 스킵 + 경고 로그
```

### 4.4 황색 주술 필터링

```
<tr> 순회 중:
  1열 텍스트에 "고구려의" 또는 "담덕" 포함 → 크롤링 대상
  나머지 → 스킵
```

---

## 5. 예외 처리

### 5.1 강인한/신속한/끈기의/신비한 RitualApplicability

```
문제: 데이터에 적용 가능 아이템이 명시되지 않음
처리:
  해당 주술의 RitualApplicability는 저장하지 않음
  수동으로 나중에 INSERT
  (RitualApplicability는 ritual_id + equipment_item_id 모두 nullable=false — null 행 INSERT 불가)

수동 입력 대상:
  강인한 → 고급천왕검
  신속한 → 고급천왕주
  끈기의 → 고급천왕극
  신비한 → 고급천왕비
```

### 5.2 광개토 태황반지

```
문제: 2열(Gem 위치)에 "태황반지"가 있지만 실제로는 적용 가능 아이템
처리:
  Gem 생성 안 함
  "태황반지"를 DB에서 EquipmentItem으로 조회
  RitualApplicability로 저장
  못 찾으면 스킵 + 경고 로그
```

### 5.3 데미지 범위값 처리

```
"데미지+25~35" 파싱 시 범위값이 나옴
→ 오픈 이슈 (아래 참고)
```

### 5.4 FK 조회 실패 시

```
세트명/아이템명 DB 조회 실패 → 스킵 + 경고 로그
로그 형식:
  [SKIP] RitualApplicability 연결 실패
  ritualName: 천권
  targetName: 수라
  이유: EquipmentSet 조회 실패
  → 수동 처리 필요
```

---

## 6. 실행 순서 및 의존성

```
주술 크롤러 실행 전 반드시 완료되어야 하는 것:

1. 아이템 크롤러 완료
   → EquipmentItem (고급천왕검/주/극/비, 태황반지 등) DB에 존재해야 함

2. 세트 크롤러 완료
   → EquipmentSet (수라, 바투, 증장천왕 등) DB에 존재해야 함

순서 어기면:
   RitualApplicability FK 조회 실패 → 스킵 로그 발생
```

---

## 오픈 이슈

| 항목 | 내용 | 우선순위 |
|---|---|---|
| 데미지 범위값 처리 | "데미지+25~35" 파싱 시 최솟값/최댓값 별도 저장 | 중간 |
| 강인한 등 4종 수동 입력 | RitualApplicability 미저장 후 관리자 페이지 또는 직접 DB 입력 | 낮음 |
