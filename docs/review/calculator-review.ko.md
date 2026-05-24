# 가성비 계산기(Resistance/Attribute) 기능 검증 리뷰

기준일: 2026-03-18  
대상 문서: `docs/calculator.md`  
대상 코드: `back/gersangtrade/src/main/java/org/example/gersangtrade/calculator/*`

---

## 1) 구현 현황 요약

- **API**: `POST /api/calculator`
  - `CalculatorController`: `back/gersangtrade/src/main/java/org/example/gersangtrade/calculator/controller/CalculatorController.java`
- **핵심 서비스**: `CalculatorService`
  - 공식 구현(저항 통과율/속성 보정/데미지 배율/상승률/가성비 점수)
  - 가격 기본값: `MaterialPriceHistory`의 “직전 달(avgPrice)”  
  - 유저 가격 수정값: `priceOverrides` 맵으로 요청 단위 적용(서버 저장 없음)

---

## 2) 스펙(`docs/calculator.md`)과의 일치 여부

### ✅ 일치(구현이 스펙과 동일)
- **저항 통과율**
  - $resistAfter = monsterResistance - currentResistPierce$
  - `resistAfter >= 260` → 1.4% 고정
  - 미만 → $100 - (resistAfter * 0.16 + 57)$, 최솟값 0
  - 구현: `CalculatorService.calcResistPassRate()`

- **속성 보정**
  - $clamp((3x - y)/2, -50, +50)$
  - 구현: `CalculatorService.calcElementBonus()`

- **종합 데미지 배율**
  - $(100 + elementBonus) * (passRate/100)$
  - 구현: `CalculatorService.calcDamageMultiplier()`

- **데미지 상승률**
  - $(after/before - 1) * 100$
  - 구현: `CalculatorService.calcIncreaseRate()`

- **가성비 점수**
  - `데미지 상승률(%) / 가격(만 골드)`
  - 구현: `CalculatorService.calcEfficiencyScore()` (골드→만골드 환산 포함)

- **정렬/추천**
  - 가성비 점수 내림차순 정렬 + 최고 점수 `recommended=true`
  - 구현: `sortAndMarkTopResist()`, `sortAndMarkTopElement()`

### ⚠️ 불일치/리스크(스펙 대비 기능이 “의도대로 동작하지 않을 수 있음”)

#### A) “비로그인(Guest) 허용”이 코드 주석/문서와 달리 **실제로는 막혀 있을 가능성**
- `CalculatorController` 주석은 Guest 허용을 전제로 하고,
  `SecurityConfig`에 `/api/calculator/** permitAll`이 필요하다고 적혀 있음.
- 하지만 현재 `SecurityConfig`는 **GET**에 대해서만 `/api/listings/**`, `/api/wanted/**`, `/api/items/**` 등을 permitAll하고,
  **`/api/calculator`(POST)** 에 대한 permitAll이 보이지 않음.
- 결과적으로 지금 상태에서는 `POST /api/calculator`가 `anyRequest().authenticated()`에 걸려 **401**일 가능성이 큼.

> 결론: “스펙상 Guest 허용”이 목표라면, 현재 코드는 **설정 미반영으로 기능이 막혀 있을 확률이 높음**.

#### B) 서버 입력 방식 차이: 스펙은 “서버명”, 구현은 “serverId”
- 스펙(`docs/calculator.md`) 유저 입력 항목에는 서버 개념이 없거나 “서버명” 중심으로 읽히는데,
  구현은 `CalculatorRequest.serverId`(1~13)를 필수로 받음.
- 가격 기본값 로딩도 `serverId + yearMonth`로 조회.

> 결론: 프론트/문서 관점에서 “서버명 vs 서버ID” 매핑이 명확히 정리되지 않으면 혼선 가능.

---

## 3) 계산 로직 관점에서의 잠재 버그/정책 미확정 포인트

### 3.1 깎은 뒤 저항이 “큰 음수”일 때 통과율이 100%를 초과할 수 있음
- 현재 구현은 최솟값(0)만 clamp하고 **최댓값 clamp가 없음**.
- 예: resistAfterDebuff = -1000이면 통과율이 200%대가 될 수 있음.
- 스펙 문서도 이 부분은 “상한 별도 정의 필요(추후 확인)”로 남아있음.

> 결론: 현재 구현은 “정책 미확정” 상태를 그대로 코드로 가져간 형태이며,  
> 운영 전에는 **상한(예: 100% cap 여부)** 정책 확정이 필요.

### 3.2 “상성 관계 없는 속성” 처리 불가(입력 모델이 없음)
- 스펙 문서에는 “상성 관계 없는 속성은 보정 0 처리”가 있으나,
  구현 입력은 `currentElementValue`, `monsterElementValue` (숫자 2개)뿐이라
  “상성 없음”을 판정할 속성 타입/관계 입력이 없음.

> 결론: 현재 모델에서는 유저가 직접 `monsterElementValue`를 조정하는 방식으로만 우회 가능.

### 3.3 가격 데이터가 없으면 대부분 항목이 점수(null) 처리되어 추천이 사라짐
- `calcEfficiencyScore()`는 price가 null/0이면 null 반환(계산 제외) → 정렬 시 뒤로 밀림
- `MaterialPriceHistory`가 비어 있거나(직전 달 데이터 미수집),
  재료 가격이 없는 항목(특히 용병 재료 일부)은 비용이 null로 남아 점수가 null이 될 수 있음.
- 용병 비용은 “알려진 재료만 합산”이라 실제보다 낮아질 수 있다는 경고가 코드에 존재.

> 결론: 데이터 수집/집계가 안정화되기 전에는 결과 품질이 크게 흔들릴 수 있음(특히 용병 쪽).

### 3.4 “데미지 상승률이 0 이하” 처리(비추천) 규칙 미반영 가능
- 스펙은 “상승률 0 이하이면 0 또는 음수 표시(비추천)” 언급이 있으나,
  구현은 음수 상승률/음수 점수를 그대로 산출하고,
  리스트에서 최고 점수(설령 음수라도)면 recommended가 붙을 수 있음.

> 결론: 추천 배지 로직이 스펙의 “비추천” 정책과 충돌할 수 있음.

---

## 4) 기능 검증 체크리스트(간단)

- **권한**: 비로그인 상태에서 `POST /api/calculator`가 200인지, 401인지 확인 필요(현재 설정상 401 가능성 높음)
- **데이터**: 직전 달 `material_price_history` 데이터가 없을 때 응답이 어떻게 나오는지(가격 null → 점수 null)
- **예시 검증**: 스펙의 예시(저항 365, 저항깎 136 → 저항 229 → 통과율 6.36%)가 코드와 일치하는지(일치)
- **엣지**: resistAfterDebuff가 매우 작은 음수일 때 passRate가 100% 초과하는지(정책 미확정)

---

## 5) 재검증 보고 (2026-03-18)

### 2-A) SecurityConfig permitAll — **리뷰 내용 오류**
- **수정 여부: 수정 불필요 — 리뷰 기술이 틀렸음**
- 실제 `SecurityConfig.java`를 확인하면 이미 아래 설정이 존재한다:
  ```java
  .requestMatchers(HttpMethod.POST, "/api/calculator").permitAll()
  ```
- 기존 리뷰에서 "현재 SecurityConfig에 `/api/calculator`(POST) permitAll이 보이지 않음 → 401 가능성 높음"이라고 했으나, 코드에 명확히 있음
- `CalculatorController` 주석 `"permitAll 처리 필요"` → `"permitAll 적용됨"` 으로 수정 완료

### 2-B) serverId vs 서버명 차이 — **수정 불필요**
- `CalculatorRequest.serverId`(Integer, 1~13) 방식은 `Server` 엔티티 설계와 일치함
- CLAUDE.md에서 서버는 1~13 고정 목록이며 `server_id`로 관리된다고 명시됨
- 프론트가 "서버 선택 드롭다운 → serverId 전송" 방식으로 구현하면 충분하며 API 레벨 변경 불필요

### 3.1) 저항 통과율 상한(100%) clamp 없음 — **수정 보류**
- `calcResistPassRate()`에 최솟값(0) clamp만 있고 최댓값(100%) clamp가 없음
- `resistAfterDebuff`가 음수 큰 값이면 통과율이 100%를 초과 가능 (예: -800 → 통과율 171%)
- **보류 근거**: CLAUDE.md 데미지 공식에 상한 cap에 대한 정의가 없음. 게임 메커닉에서 저항깎이 몬스터 저항을 넘어서는 경우의 처리 방식이 확정되지 않은 정책 미확정 상태. 무단으로 `Math.min(100.0, ...)` 추가 시 게임 실제 공식과 달라질 위험이 있음

### 3.2) "상성 없는 속성" 처리 — **수정 불필요**
- 현재 입력 모델(`currentElementValue`, `monsterElementValue` 숫자 2개)에서 속성 타입 없이 상성 판정 불가
- 스펙 구현 의도대로 "유저가 상성 없을 때 monsterElementValue를 0으로 입력"하는 우회 방식이 실용적이며 입력 단순성을 유지함
- 상성 타입 필드 추가는 UX 복잡도 대비 실익이 불분명하므로 현 설계 유지

### 3.3) 가격 없을 때 점수 null 처리 — **수정 불필요**
- `calcEfficiencyScore()`에서 `price == null || price == 0L`이면 null 반환 후 정렬 시 맨 뒤로 밀리는 것은 의도된 동작
- 가격 정보가 없는 항목을 추천 제외하는 것이 올바른 UX
- 크롤링 배치 데이터가 축적될수록 자연히 해소되는 문제

### 3.4) 음수 점수에 recommended=true 마킹 — ✅ **수정 완료**
- **수정 파일**: `CalculatorService.java` — `sortAndMarkTopResist()`, `sortAndMarkTopElement()`
- **문제**: 모든 항목의 가성비 점수가 음수(데미지가 오히려 감소)인 경우에도 목록 1위 항목에 `recommended=true`가 붙을 수 있었음
- **수정 내용**: 두 정렬 메서드 모두 `efficiencyScore > 0` 조건 추가
  ```java
  // 변경 전
  if (!list.isEmpty() && list.get(0).efficiencyScore() != null) { ... }
  // 변경 후
  if (!list.isEmpty()
          && list.get(0).efficiencyScore() != null
          && list.get(0).efficiencyScore() > 0) { ... }
  ```
- **근거**: 가성비 점수가 0 이하라는 것은 해당 아이템/용병을 추가해도 데미지가 늘지 않거나 오히려 줄어든다는 의미. 이를 "추천"으로 표시하면 유저에게 잘못된 정보를 제공함

---

## 6) 수정 사항 요약

| 번호 | 항목 | 수정 여부 | 파일 |
|------|------|----------|------|
| 2-A | SecurityConfig permitAll 기술 오류 | ✅ 주석 수정 | `calculator/controller/CalculatorController.java` |
| 3.4 | 음수 점수에 recommended 마킹 버그 | ✅ 수정 완료 | `calculator/service/CalculatorService.java` |
| 2-B | serverId vs 서버명 차이 | ⏸ 수정 불필요 | — |
| 3.1 | 통과율 100% 상한 clamp 없음 | ⏸ 정책 미확정, 보류 | — |
| 3.2 | 상성 없는 속성 처리 불가 | ⏸ 수정 불필요 | — |
| 3.3 | 가격 없으면 점수 null | ⏸ 의도된 동작 | — |

