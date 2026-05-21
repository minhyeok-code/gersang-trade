# 몬스터 크롤러 설계 문서

> 거상 거래 플랫폼 — 몬스터 크롤링 설계 확정본

---

## 목차
1. [크롤링 대상](#1-크롤링-대상)
2. [HTML 패턴 분류](#2-html-패턴-분류)
3. [파싱 전략](#3-파싱-전략)
4. [엔티티 설계](#4-엔티티-설계)
5. [멱등성(Upsert) 전략](#5-멱등성upsert-전략)
6. [오류 처리 전략](#6-오류-처리-전략)

---

## 1. 크롤링 대상

### 1.1 인덱스 페이지

```
https://www.gersangjjang.com/monster/index.asp
```

이 페이지에서 몬스터 상세 페이지 URL을 동적으로 수집한다.
URL을 하드코딩하지 않고 인덱스에서 수집하므로, 거상짱에 페이지가 추가되면 자동으로 반영된다.

### 1.2 URL 수집 규칙

```
수집 대상:
  href가 /monster/ 로 시작하는 링크만 수집

제외 대상:
  href가 /quest/ 로 시작하는 링크 → 퀘스트 페이지이므로 제외
  /monster/index.asp 자기 자신 → 제외

중복 제거:
  수집된 URL을 Set으로 관리
  동일 URL이 여러 섹션에 중복 등장해도 한 번만 크롤링
  (예: /monster/longshan.asp, /monster/youmingjie.asp, /monster/star.asp 등)
```

### 1.3 크롤링 흐름

```
1. 인덱스 페이지 크롤링 → URL Set 수집
2. URL Set 순회 → 각 몬스터 페이지 크롤링
3. 페이지 내 monster-row 순회 → 몬스터별 파싱 및 저장
```

---

## 2. HTML 패턴 분류

몬스터 페이지의 HTML 구조가 구형/신형에 따라 다르다.
크롤러는 아래 3가지 패턴을 감지하고 분기 처리해야 한다.

### 패턴 요약

| 구분 | hp | 타저/마저 위치 | 속성값 위치 | element 위치 |
|---|---|---|---|---|
| 패턴 A (구형) | null | `info-section` 저항력 라인 | 저항력 라인 안에 혼재 | 저항력 라인 (`수속성 20` 형태) |
| 패턴 B (신형 보스) | `monster-sprite` 텍스트 | `monster-sprite` 텍스트 | `info-section` 속성값 태그 | 몬스터 이름 괄호 |
| 패턴 C (중간형) | null | `info-section` 저항/저항력 라인 | `info-section` 속성값 별도 태그 | 몬스터 이름 괄호 |

### 패턴 A 예시 (구형)

```html
<div class="monster-name">연못거북(水) (9급)</div>
<div class="monster-sprite"><img ...></div>

<div class="info-section">
  <strong>경험치</strong>: 12.5만,
  <strong>저항력</strong>: 타저 290%, 마저 290%, 수속성 20
</div>
```

특징:
- `monster-sprite`에 텍스트 없음 (이미지만)
- `저항력` 한 줄에 타저/마저/속성값/속성 전부 혼재
- hp 없음

### 패턴 B 예시 (신형 보스)

```html
<div class="monster-name">악령 전이 (雷) (boss급)</div>
<div class="monster-sprite">
  공격력 1700<br>
  체력 400만, 마력 8만<br>
  타저 410%, 마저 420%
</div>

<div class="info-section">
  <strong>경험치</strong>: 520만, <strong>속성값: </strong>170
</div>
```

특징:
- `monster-sprite`에 텍스트로 hp, 타저, 마저 존재
- `info-section`에 `속성값` 별도 태그
- element는 이름 괄호에서 파싱

### 패턴 C 예시 (중간형)

```html
<div class="monster-name">광려산멧돼지 (雷) (9급)</div>
<div class="monster-sprite"><img ...></div>

<div class="info-section">
  <strong>경험치</strong>: 15만, <strong>속성값:</strong> 20
  <strong>저항력</strong>: 타저 290%, 마저 290%
</div>
```

```html
<!-- 저항 라벨이 "저항"으로 축약된 케이스도 있음 -->
<strong>속성값</strong>: 250, <strong>저항</strong>: 타저 415%, 마저 405%
```

특징:
- `monster-sprite`에 텍스트 없음
- `info-section`에 `속성값` 별도 태그 + `저항력` 또는 `저항` 라인 분리
- hp 없음
- element는 이름 괄호에서 파싱

---

## 3. 파싱 전략

### 3.1 패턴 감지 로직

```
// 1단계: hp 존재 여부로 패턴 B 감지
hasHp = monster-sprite 텍스트에 "체력" 포함

// 2단계: 속성값 위치 감지
hasSeparateElementTag = info-section 안에 <strong>속성값</strong> (또는 속성값:) 태그 존재

// 3단계: 저항 위치 감지
resistanceInSprite = monster-sprite 텍스트에 "타저" 포함

// 패턴 분기
if (hasHp && resistanceInSprite):
    → 패턴 B
else if (hasSeparateElementTag):
    → 패턴 C
else:
    → 패턴 A
```

### 3.2 패턴별 파싱 규칙

#### 패턴 A

```
타저:
  info-section > 저항력 라인에서 "타저 N%" 정규식
  예: "타저 290%" → 290

마저:
  info-section > 저항력 라인에서 "마저 N%" 정규식
  예: "마저 290%" → 290

속성값 + element:
  저항력 라인에서 속성명+수치 파싱
  예: "수속성 20" → elementValue=20, element=WATER
  예: "화속성 15" → elementValue=15, element=FIRE

hp:
  null 저장
```

#### 패턴 B

```
hp:
  monster-sprite 텍스트에서 "체력 N만" 파싱
  예: "체력 400만" → 4_000_000
  예: "체력 1680만" → 16_800_000

타저:
  monster-sprite 텍스트에서 "타저 N%" 파싱

마저:
  monster-sprite 텍스트에서 "마저 N%" 파싱

속성값:
  info-section > <strong>속성값</strong> 태그 다음 텍스트
  예: "속성값: 170" → 170
  예: "속성값: 0" → 0

element:
  monster-name 괄호에서 파싱 (3.3 참고)
```

#### 패턴 C

```
hp:
  null 저장

타저:
  info-section > 저항력 또는 저항 라인에서 "타저 N%" 파싱
  ※ 라벨이 "저항력"과 "저항" 두 가지 모두 존재하므로 둘 다 매칭

마저:
  info-section > 저항력 또는 저항 라인에서 "마저 N%" 파싱

속성값:
  info-section > <strong>속성값</strong> 태그 다음 텍스트

element:
  monster-name 괄호에서 파싱 (3.3 참고)
```

### 3.3 element 파싱 규칙 (공통)

monster-name 텍스트에서 괄호를 추출하여 element를 결정한다.

```
(水) → WATER
(雷) → THUNDER
(火) → FIRE
(風) → WIND
(土) → EARTH
(明) → NONE        ← 명시적 무속성
괄호 없음 → null   ← 데이터 없음 (구형 몹 등)
```

패턴 A의 경우 저항력 라인에서도 속성을 파싱할 수 있으나,
monster-name 괄호 파싱을 우선 시도하고 괄호가 없을 때만 저항력 라인에서 파싱한다.

### 3.4 hp 단위 파싱

```
"체력 400만"   → 4_000_000
"체력 1680만"  → 16_800_000
"체력 500"     → 500 (만 단위 아닌 경우 그대로)

파싱 규칙:
  숫자 추출 후 "만" 포함이면 × 10_000
  "만" 없으면 그대로 저장
```

---

## 4. 엔티티 설계

### Monster

```java
@Entity
@Table(
    name = "monster",
    uniqueConstraints = @UniqueConstraint(columnNames = {"name", "page_url"})
)
public class Monster {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 몬스터 이름 (괄호 속성/등급 포함 원문 저장) */
    @Column(nullable = false)
    private String name;

    /** 출처 페이지 URL (중복 판단 기준) */
    @Column(name = "page_url", nullable = false)
    private String pageUrl;

    /** 생명력 — 구형 몹은 null */
    @Column(nullable = true)
    private Long hp;

    /** 타격저항력(%) — null 허용 */
    @Column(nullable = true)
    private Integer hittingResistance;

    /** 마법저항력(%) — null 허용 */
    @Column(nullable = true)
    private Integer magicResistance;

    /** 속성값 수치 — null 허용 */
    @Column(nullable = true)
    private Integer elementValue;

    /**
     * 속성 종류
     * WATER / THUNDER / FIRE / WIND / EARTH : 해당 속성
     * NONE  : 명시적 무속성 (明 등)
     * null  : 데이터 없음 (구형 몹)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Element element;
}
```

### Element enum 추가 항목

기존 Element enum에 아래를 추가한다.

```java
NONE,  // 명시적 무속성 (明속성 몹)
```

---

## 5. 멱등성(Upsert) 전략

### Upsert 기준 키

```
Monster: name + pageUrl
```

같은 페이지에 같은 이름의 몬스터가 두 번 등장하는 경우는 없다고 가정한다.
단, 다른 페이지에 동명의 몬스터가 있을 수 있으므로 pageUrl을 함께 기준 키로 사용한다.

### Upsert 처리

```java
monsterRepository.findByNameAndPageUrl(name, pageUrl)
    .ifPresentOrElse(
        existing -> existing.update(hp, hittingResistance, magicResistance, elementValue, element),
        () -> monsterRepository.save(new Monster(...))
    );
```

---

## 6. 오류 처리 전략

### 6.1 크롤러 경고 로그 조건

```
// elementValue가 있는데 element 파싱 실패
if (elementValue != null && element == null):
    [WARN] elementValue 존재하지만 element 파싱 실패
    monsterName: xxx, pageUrl: xxx
    → 수동 확인 필요

// 괄호가 없어서 element를 null로 저장한 케이스
if (element == null && elementValue == null):
    [INFO] element/elementValue 모두 null (구형 몹 또는 무속성)
    monsterName: xxx

// (明) 파싱 성공
if (element == NONE):
    [INFO] 명속성 몹, element=NONE 저장
    monsterName: xxx
```

### 6.2 URL 수집 실패

```
인덱스 페이지 접근 실패 → 크롤링 중단 + 에러 로그
개별 몬스터 페이지 접근 실패 → 해당 URL 스킵 + [WARN] 로그
```

### 6.3 패턴 감지 실패

```
세 가지 패턴 중 어디에도 해당하지 않는 경우:
  [WARN] 알 수 없는 HTML 패턴
  pageUrl: xxx, monsterName: xxx
  → 수동 확인 필요
  → 해당 몬스터 스킵 (크롤링 중단 없음)
```