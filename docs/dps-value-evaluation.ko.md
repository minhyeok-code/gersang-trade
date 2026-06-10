# DPS 가성비 평가 (스펙업 시뮬레이션) 기획

> 작성일: 2026-06-09  
> 최종 수정: 2026-06-12  
> 상태: 초안 (Draft)  
> 관련: [`calculator.md`](./calculator.md), [`value-for-money-feature.ko.md`](./value-for-money-feature.ko.md), [`recommend-plan.md`](./recommend-plan.md), [`clear-time-hunt-hub.ko.md`](./clear-time-hunt-hub.ko.md)

---

## 1. 개요

유저가 **현재 덱**을 기준으로 아이템 또는 용병을 추가·교체했을 때의 **DPS 변화**와 **가격 대비 효율(가성비)** 을 계산·저장하는 기능이다.

| 구분 | 설명 |
|------|------|
| **입력** | 덱 ID, 몬스터 ID, 아이템/용병 시나리오, 가격(정책별) |
| **계산** | DPS 3종(raw / adjust / final) before·after 비교 |
| **저장** | 평가 결과 수치 + 시나리오 덱 스냅샷 (최소 저장 원칙) |
| **비저장** | 유저가 수정한 가격은 세션·요청 단위 (DB 미저장) |

기존 `CalculatorService`(`POST /api/calculator`)는 **저항통과율×속성보정 배율** 기반의 단순 가성비이며, 본 기능은 **`DpsCalculatorService` 기반 풀 DPS**를 사용한다. 두 계산기는 목적이 다르므로 병행 유지한다.

---

## 2. 확정 정책

| 항목 | 정책 |
|------|------|
| **DPS 저장** | raw / adjust / final **3종 모두** before·after·delta·증가율·가성비 저장 (향후 확장 대비) |
| **가성비 기본 기준** | **finalDps** (저항 포함 최종 DPS) — UI 정렬·추천의 기본값 |
| **용병 가격** | **유저 직접 입력만** (시세 자동 조회 없음) |
| **아이템 가격** | **`User.server` 기준** `TradeStatMonthly` → 없으면 **유저 입력** (`priceOverrides`). `User.server` null → `ITEM_*`만 400 |
| **덱 변경** | 실제 `UserDeck` DB 수정 **금지** — 서버 내부 overlay 시뮬레이션 |
| **API 호출** | 프론트가 DPS API 2회 호출하지 않음 — **평가 API 1회**로 before/after 일괄 처리 |
| **시나리오 멤버** | 단일 필드 `affectedMemberId`로 통일 (§7.1) |
| **중복 평가** | 동일 `evaluation_hash` → **기존 row 반환** (멱등, §8.3) |
| **`persist: false`** | 계산·응답은 동일, DB·스냅샷 저장 생략 (`evaluationId = null`) |
| **인증** | **로그인 필수** — 덱 편집 권한과 동일 (Guest 불가) |
| **평가 목록 공개** | **본인만 조회** (프라이버시). 사냥 허브·클리어타임 연동 공개는 후속 검토 |
| **주술 입력** | 거래 등록 폼과 동일 — **피스별 유저 지정** (§3.4) |

---

## 3. 유저 플로우

### 3.1 공통 전제

- 기준 덱: 유저가 저장한 `UserDeck`
- 몬스터: `monsterId` 필수 (DPS 의미 없이 평가 불가)
- 멤버별 레벨·스탯분배: `MemberDpsInput` (기존 DPS API와 동일, DB 미저장)

### 3.2 아이템 선택

```
[아이템 / 용병 선택] → 아이템
        ↓
[거래 검색·등록과 동일한 폼으로 아이템 선택]
  - 단품(EQUIPMENT_SINGLE) 또는 세트(EQUIPMENT_SET)
  - 강화·주술 포함 (EquipmentDetailRequest)
        ↓
[현재 덱의 대상 용병 선택] → affectedMemberId
        ↓
[서버: 해당 용병에게 아이템 자동 장착/교체 시뮬레이션]
  - 단품: 슬롯 호환·용병 제한 검증 후 해당 슬롯 교체
  - 세트: DeckService.equipSet과 동일 (5피스 + 반지 양손)
        ↓
[DPS before/after 계산 → 증가분·가성비 표시]
        ↓
(선택) 평가 결과 DB 저장
```

### 3.3 용병 선택

```
[아이템 / 용병 선택] → 용병
        ↓
[용병 가격 — 유저 직접 입력 (필수)]
        ↓
[덱 12명 가득?]
  ├─ 예 → 교체 대상 용병 1명 선택 (mode=REPLACE, affectedMemberId)
  └─ 아니오 → 빈 슬롯에 자동 추가 (mode=APPEND, affectedMemberId=null)
        ↓
[특성·레벨·스탯분배 선택]
        ↓
[서버: 용병 추가/교체 overlay 시뮬레이션]
        ↓
[DPS before/after 계산 → 증가분·가성비 표시]
        ↓
(선택) 평가 결과 DB 저장
```

### 3.4 주술 입력 — 무엇을 의미하는가

가성비 평가에서 「주술」은 **장비 피스 1개에 붙는 주술**을 뜻한다. 거래 등록·덱 장착과 동일하다.

| 구분 | 데이터 | 유저 입력 | DPS 반영 |
|------|--------|-----------|----------|
| **피스 주술** | `ritualId` + `SUCCESS` / `GREAT_SUCCESS` | 거래 폼 `EquipmentDetailRequest` (피스마다) | `RitualStat` — 해당 슬롯 스탯 가산 |
| **장비 세트 효과** | 5피스 세트 보너스 | **입력 없음** — 피스 장착 수로 자동 | `EquipmentSetEffect` |
| **주술 세트 효과** | 동일 주술 N피스 보너스 | **입력 없음** — 피스별 주술 조합으로 자동 | `RitualSetEffect` |

즉 「주술 기본값」 논의는 **세트 보너스가 아니라**, 시뮬레이션에 넣는 **새 장비 각 피스에 어떤 주술(`ritualId`)·결과(`SUCCESS`/`GREAT_SUCCESS`)를 적용할지** 를 말한다. 이 값이 `RitualStat` 테이블의 스탯을 읽어 DPS에 반영한다.

**확정 정책 (UI · API):**

| UX | API · 시뮬레이션 |
|----|------------------|
| 유저가 **주술을 선택하지 않음** | 피스별 주술 입력 UI(라인) **비노출**. `lines` 생략·`[]`·`hasRitual=false` → 해당 피스 **미주술** |
| 유저가 **주술을 선택함** | 피스별 라인 UI **노출**. 피스마다 `hasRitual` / `rituals` 지정 |
| 주술 선택 후 **피스 주술을 비워 둠** | `hasRitual=true`인데 `rituals` 비어 있음 → **400** (플로우 미완료) |
| 라인에 주술 필드 없음 / `hasRitual=false` | 해당 피스 **미주술** (오류 아님) |

- 서버가 덱의 기존 주술을 복사하거나 SUCCESS를 임의로 채우지 **않는다**.
- 시나리오에서 **바꾸지 않는 슬롯**은 현재 덱에 장착된 아이템·주술을 그대로 overlay baseline으로 유지한다.

**`ITEM_SET`와 `lines`:**

- `setId`로 장착할 피스 목록은 서버가 `EquipmentSetPiece`에서 resolve한다.
- **주술 미사용**이면 `lines` 생략·`[]` 허용 → 전 피스 **미주술**로 장착.
- **주술 사용**이면 피스 수만큼 `lines`를 보내고, 각 라인에 `equipmentDetail`로 주술·강화를 지정한다.
- `lines`가 있을 때 피스 수·`itemId`가 세트 정의와 불일치 → **400**.

---

## 4. 아키텍처

### 4.1 핵심 원칙: 실제 덱 수정 금지

현재 `DpsCalculatorService`는 `deckId`로 **DB의 슬롯·멤버**를 읽는다.  
`DeckSnapshot`은 클리어타임용 JSON 저장소이며, DPS API 입력으로는 사용되지 않는다.

따라서 아래 방식은 **사용하지 않는다**.

- 덱 장착 API 호출 → DPS 계산 → 덱 원복

**권장 서버 흐름:**

```
[유저 시나리오 요청]
        ↓
baseline 덱 상태 로드 (DB 읽기만)
        ↓
메모리 overlay로 시나리오 적용 (장착/교체 시뮬레이션)
        ↓
DPS before / after 각 1회 계산
        ↓
(persist=true) 시나리오 덱 → `EvaluationSnapshotBuilder`로 DeckSnapshot 저장 (content_hash dedup)
        ↓
(persist=true) DpsValueEvaluation row 저장
```

### 4.2 신규 컴포넌트 (구현 예정)

| 컴포넌트 | 종류 | 역할 |
|----------|------|------|
| `DpsScenarioOverlay` (+ 하위 record) | **데이터** | API 시나리오를 담는 **불변 명세** (merge 로직 없음) |
| `DpsScenarioOverlayFactory` | 서비스 | API `scenario` JSON → `DpsScenarioOverlay` 변환 |
| `LoadedDeckState` | **데이터** | DB 배치 로드 직후 스냅샷 (overlay **적용 전**) |
| `DeckStateMerger` | 서비스 | `LoadedDeckState` + overlay → **`DeckCalculationState`** |
| `DeckCalculationState` | **데이터** | merge 결과 (또는 overlay 없을 때 loaded 복사본). DPS 파이프라인 입력 |
| `DpsCalculatorService.calculateWithOverlay(...)` | 기존 확장 | 로드 → (merge) → DPS 파이프라인 실행 |
| `DpsValueEvaluationService` | 서비스 | before/after, delta, 가성비, 저장 오케스트레이션 |
| `CatalogPriceResolverService` | 서비스 | 장비 아이템·세트 시세 조회 (`TradeStatMonthly`, 용병 제외) |

**역할 분리:**

```
API scenario
    → DpsScenarioOverlayFactory → DpsScenarioOverlay (명세만)
    → DpsCalculatorService.calculateWithOverlay(req, overlay)
          → DB 로드 → LoadedDeckState
          → (overlay 있으면) DeckStateMerger.merge → DeckCalculationState
          → DPS 파이프라인(DeckCalculationState)
```

`DeckStateMerger`는 `DpsScenarioOverlay`의 **내부 메서드가 아니다.** overlay는 record(데이터)이고, merge는 **`DeckEquipmentValidator`를 호출하는 별도 `@Service`** 로 둔다 (`DpsCalculatorService` 또는 `DeckStateMerger`가 주입).

### 4.2.1 `DpsScenarioOverlay` · `calculateWithOverlay` 시그니처

**`DpsScenarioOverlay`** — merge할 변경 **명세만** 담는다. 동작(merge)은 `DeckStateMerger`가 담당한다.

```java
/**
 * 가성비 시나리오 overlay. 아이템 시나리오와 용병 시나리오는 상호 배타.
 */
public record DpsScenarioOverlay(
        ItemScenarioOverlay item,       // ITEM_SINGLE | ITEM_SET — null이면 용병 시나리오
        MercenaryScenarioOverlay mercenary  // MERCENARY — null이면 아이템 시나리오
) {}

/** affectedMemberId 슬롯에 적용할 장비 변경 */
public record ItemScenarioOverlay(
        ScenarioItemType type,          // ITEM_SINGLE | ITEM_SET
        Long setId,                     // ITEM_SET일 때
        Long affectedMemberId,
        List<ScenarioLine> lines        // 거래 등록 BundleLine과 동일 구조 (§9.1)
) {}

public record MercenaryScenarioOverlay(
        MercenaryMode mode,             // REPLACE | APPEND
        Long mercenaryId,
        Long affectedMemberId,          // REPLACE 시 필수, APPEND 시 null
        int level,
        BonusStatTarget bonusTarget,
        int bonusAmount,
        List<CharacteristicSelection> characteristics
) {}
```

**`DpsCalculatorService`**

```java
/** 기존 API — overlay 없음과 동일 */
public DpsResponse calculate(DpsRequest req) {
    return calculateWithOverlay(req, null);
}

/**
 * @param overlay null이면 DB 덱 그대로 계산 (before DPS에 사용).
 *                non-null이면 DB 로드 후 in-memory merge 뒤 계산 (after DPS).
 */
public DpsResponse calculateWithOverlay(DpsRequest req, @Nullable DpsScenarioOverlay overlay)
```

**`LoadedDeckState` · `DeckCalculationState` (별도 record)**

```java
/** DB 배치 로드 결과 — overlay 적용 전 (불변 스냅샷) */
public record LoadedDeckState(
        Long deckId,
        List<LoadedMember> members,      // mercenary, slots, characteristics, …
        Map<Long, MemberDpsInput> memberInputs
) {}

/** DPS 계산 파이프라인 입력 — overlay merge 후 (또는 loaded와 동일 내용) */
public record DeckCalculationState(
        Long deckId,
        List<LoadedMember> members,
        Map<Long, MemberDpsInput> memberInputs
) {}
```

`LoadedDeckState`와 `DeckCalculationState`는 필드 구성이 같을 수 있으나 **역할을 분리**한다.  
overlay가 null이면 `loaded.toCalculationState()`로 변환만 하고 merge는 생략한다.

**`DeckStateMerger` (별도 컴포넌트)**

```java
@Service
public class DeckStateMerger {
    private final DeckEquipmentValidator validator;

    /** DB에서 로드한 덱 + overlay 명세 → DPS 계산용 in-memory 상태 */
    public DeckCalculationState merge(LoadedDeckState loaded, DpsScenarioOverlay overlay) {
        // 용병 REPLACE / APPEND, 아이템·세트 슬롯 갱신 (validator 호출)
    }
}
```

**`DpsCalculatorService` 내부 흐름 (리팩터 방향):**

```
1. deckId로 members · slots · 특성 등 DB 배치 로드 → LoadedDeckState
2. overlay == null → loaded를 DeckCalculationState로 그대로 변환
   overlay != null → deckStateMerger.merge(loaded, overlay) → DeckCalculationState
      - 용병 REPLACE: 해당 memberId의 mercenary·특성·MemberDpsInput 교체
      - 용병 APPEND: 가상 멤버(음수 memberId) 추가 (§7.4)
      - 아이템: affectedMemberId 슬롯에 장비·주술 반영 (§4.4 validator)
3. DeckCalculationState로 기존 DPS 파이프라인 실행 → DpsResponse 반환
```

**before / after 호출 (채택):**

`DpsValueEvaluationService`에서 **동일 메서드**를 overlay 유무만 바꿔 **2회** 호출한다. 기존 `calculate()`를 after 전용으로 따로 부르지 않는다.

```java
DpsResponse before = dpsCalculator.calculateWithOverlay(req, null);
DpsScenarioOverlay overlay = overlayFactory.from(request.scenario());
DpsResponse after  = dpsCalculator.calculateWithOverlay(req, overlay);
```

| 호출 | overlay | 의미 |
|------|---------|------|
| before | `null` | 현재 DB 덱 그대로 |
| after | 시나리오 적용 | overlay merge 후 |

### 4.3 기존 코드 재사용

| 기존 자산 | 활용 |
|-----------|------|
| `BundleCreateRequest` / `EquipmentDetailRequest` | 아이템·주술·강화 입력 폼 |
| `MemberDpsInput` | 레벨·스탯분배 |
| `DeckSnapshotContent.CharacteristicSelection` | 용병 특성 선택 |
| `UserClearTime` (raw/adjust/final DPS 컬럼) | 저장 필드 패턴 |
| `DeckSnapshotHashUtil` | canonical JSON · SHA-256 |
| `TradeStatMonthlyRepository` | `statKey` 시세 조회 |
| `DeckService.equipSet` | **장착 규칙 참고용** — DB 쓰기 메서드이므로 overlay에서는 로직만 이식 |

### 4.4 덱 검증 로직 분리 (구현 선행 작업)

`DeckService`의 아래 메서드는 **`private`** 이라 `DpsScenarioOverlay`·`DpsValueEvaluationService`에서 **직접 호출할 수 없다**.

| 메서드 | 용도 |
|--------|------|
| `validateSlotCompatibility` | 슬롯·아이템 호환 |
| `validateMercenaryRestriction` | 용병별 착용 제한 |
| `validateMyeongwangComposition` | 명왕 구성 제약 |
| `equipSetPiece` / `resolveSetEquipSlot` | 세트 피스 슬롯 매핑 |

**채택 방안:** `deck` 패키지에 **`DeckEquipmentValidator`** (또는 `DeckCompositionValidator`) 컴포넌트를 신설하고, 위 검증·슬롯 매핑 로직을 **추출**한다. `DeckService`와 `DpsScenarioOverlay`가 동일 컴포넌트를 주입받아 사용한다.

- `DeckService` public API 시그니처는 유지 (내부에서 validator 위임).
- overlay는 DB write 없이 validator만 호출 후 in-memory 슬롯 맵을 갱신.

### 4.5 시나리오 스냅샷 빌더 (클리어타임 빌더와 분리)

기존 `DeckSnapshotBuilderService.buildOrReuse`는 **클리어타임 전용**이다.

```java
BuildResult buildOrReuse(
    Long userId, Long deckId, ResistanceType resistanceType,
    List<MemberDpsInput> memberInputs,
    List<MemberDpsResult> memberDpsResults  // 클리어타임 DPS 결과
)
```

생성 JSON(`DeckSnapshotContent`)에는 `attrXValue`, `totalResDown`, `DpsContext` 등 클리어타임 맥락 필드가 포함된다. 가성비 **after** 시나리오를 이 메서드에 그대로 넣으면 의미가 섞인다.

**채택 방안:**

| 항목 | 방침 |
|------|------|
| 저장 테이블 | `deck_snapshots` **공유** (`content_hash` dedup 유지) |
| hash 유틸 | `DeckSnapshotHashUtil` **재사용** |
| 빌더 | **`EvaluationSnapshotBuilder`** (신규) — overlay 적용 후 덱 상태를 `DeckSnapshotContent`로 직렬화 |
| 클리어타임 빌더 | `DeckSnapshotBuilderService` **변경 없음** |

`EvaluationSnapshotBuilder`는 after DPS의 `memberResults`로 `DpsContext.memberElementValues`만 채우고, 평가 시점 `monsterId`·시나리오 메타는 `DpsValueEvaluation` row에 둔다 (스냅샷 JSON에 중복 저장하지 않음).

### 4.6 `DeckSnapshot` 재사용과 결합도

**재사용 의도:** `DeckSnapshot`·`DeckSnapshotContent`는 클리어타임과 **동일 JSON 스키마**를 공유하여 직렬화·`content_hash` dedup 비용을 줄인다.

**허용하는 결합:** hash 유틸·JSON 구조 공유.

**리스크:** 클리어타임 전용 필드가 스냅샷 스키마에 추가되면 가성비 평가 저장에도 영향을 받을 수 있다.

**완화 (후속 검토):** `snapshotPurpose: CLEAR_TIME | VALUE_EVALUATION` 메타 필드 추가, 또는 평가 전용 `ScenarioSnapshot` 분리. MVP에서는 재사용을 유지한다.

---

## 5. DPS 지표 정의

### 5.1 명칭 통일 — finalDps

본 기능·API·DB·UI에서 **최종 DPS는 `finalDps`로 통일**한다.  
기존 `DpsResponse.totalDps`는 구현 시 **`finalDps`로 매핑**한다 (`UserClearTime.final_dps`와 동일 도메인 용어).

| 레이어 | raw | adjust | final (최종) |
|--------|-----|--------|----------------|
| `DpsResponse` (기존 코드) | `rawTotalDps` | `adjustTotalDps` | `totalDps` → **매핑** |
| 평가 API JSON | `raw` | `adjust` | **`final`** |
| DB (`DpsValueEvaluation`, `UserClearTime`) | `raw_dps_*` | `adjust_dps_*` | **`final_dps_*`** |

**의미:**

| 필드 | 의미 |
|------|------|
| `rawDps` | 스킬 계수 합산, 보정 없음 |
| `adjustDps` | 속성 보정만 적용 (저항 미적용) |
| `finalDps` | 속성 + 저항 통과율 모두 적용 |

### 5.2 역할 분리 (비중 vs 가성비)

| 용도 | 기준 DPS | 비고 |
|------|----------|------|
| 멤버별 `damageShare` (덱 내 딜 비중) | **adjustDps** | `DpsCalculatorService`·`MemberDpsResult` 기존 동작 |
| 증가분·가성비·정렬·추천 | **finalDps** | 본 기능의 기본 지표 |
| raw 계열 | **rawDps** | 저장·확장·디버그용 |

### 5.3 증가분·증가율

```
{dpsKind}Delta        = after - before
{dpsKind}IncreaseRate = (after / before - 1) × 100   (before ≤ 0이면 0)
```

`dpsKind` ∈ `raw`, `adjust`, `final`

### 5.4 가성비 점수

```
efficiencyPerEok_{dpsKind} = {dpsKind}IncreaseRate / 가격(억)
```

- 가격 단위: 전(錢), 억 환산 = `price / 100_000_000`
- 가격이 null 또는 0이면 해당 가성비 전부 **null** (계산 제외)
- **UI 기본 정렬·추천**: `efficiencyPerEok_final`
- raw/adjust 가성비도 저장하여 향후 뷰 전환 가능

### 5.5 UI 표시 예시

> Final DPS **+6.25%** (+50,000) · **15억** · **1억당 +0.42%**

보조 탭에서 Raw / Adjust 증가분·가성비 노출.

---

## 6. 가격 정책

### 6.1 용병 — 유저 입력만

| 항목 | 정책 |
|------|------|
| 기본값 | 없음 |
| `price` | **필수** (null/0 → 가성비 계산 불가, API 400) |
| `priceSource` | 항상 `USER_INPUT` |
| 재료 시세 합산 | MVP 범위 **제외** |

DPS 증가분은 가격 없이도 표시 가능. 가성비만 null 처리.

### 6.2 아이템 — 시세 자동 → 없으면 유저 입력

단품·세트는 조회 함수별 우선순위가 다르다. 상세는 아래 **`resolveItem` / `resolveSet`** 표 참고.

> `MaterialPriceHistory`는 **재료 전용** 크롤링 테이블이다. 장비 `itemId`로 조회해도 레코드가 없으므로 장비 가성비 경로의 fallback으로 사용하지 않는다.

**`statKey` 형식 (DB 조회 — `TradeStatMonthly` / `TradeStatDaily`):**

```
"ITEM:{itemId}"   예: "ITEM:101"
"SET:{setId}"     예: "SET:5"   (세트 통째 거래 통계가 있을 때)
```

> ⚠️ 콜론(`:`) 구분자. `ITEM_101`처럼 언더스코어로 조회하면 레코드를 찾지 못한다 (`TradeStatService`, `TradeConfirmed`와 동일 규칙).

**`priceOverrides` (API 요청 — 유저 가격 수정):**

```
"ITEM:{itemId}" → 골드 단위   예: "ITEM:101"
"SET:{setId}"   → 세트 총액 한 번에 override (선택)
```

본 API는 시세 `statKey`와 override 키를 **동일한 콜론 형식**으로 통일한다.  
(참고: 구 `CalculatorRequest`는 `ITEM_{id}` 언더스코어 형식 — 본 기능과 별개.)

**서버별 시세 (필수):**

시세는 **유저가 속한 서버** 기준으로 조회한다. 요청 body에 `serverId`를 넣지 않고, **로그인 유저의 `User.server`** 를 사용한다.

```
로그인 유저 → User.server.id (serverId)
           → CatalogPriceResolverService.resolveItem(itemId, serverId, overrides)
           → TradeStatMonthly WHERE stat_key = 'ITEM:{itemId}' AND server_id = :serverId
```

**현재 엔티티 보완 (선행 작업):**

지금 `TradeStatDaily`·`TradeStatMonthly`에는 **`server_id`가 없다** — 설계 오류로 간주하고, 가성비 평가 구현 전에 스키마·집계 배치를 수정한다.

| 엔티티 | 추가 | unique 변경 (예) |
|--------|------|------------------|
| `trade_stat_daily` | `server_id` FK NOT NULL | `(stat_date, stat_key, server_id)` |
| `trade_stat_monthly` | `server_id` FK NOT NULL | `(stat_month, stat_key, server_id)` |

- `TradeConfirmed` 집계 시 listing의 서버를 함께 롤업한다.
- `User.server`가 null이면 **아이템 시세 조회 불가** → `ITEM_*` 시나리오만 **400** (프로필에서 서버 설정 필요). `MERCENARY`는 유저 입력 가격만 쓰므로 **서버 null이어도 평가 가능**.

**단품 가격 조회 우선순위 (`resolveItem`):**

| 순위 | 소스 |
|------|------|
| 1 | `priceOverrides["ITEM:{itemId}"]` |
| 2 | `TradeStatMonthly` — `statKey = "ITEM:{itemId}"` + `server_id` |
| 3 | `MISSING` (`price = null`) |

**세트 가격 조회 우선순위 (`resolveSet`):**

| 순위 | 소스 | 설명 |
|------|------|------|
| 1 | `priceOverrides["SET:{setId}"]` | 세트 총액 override |
| 2 | `TradeStatMonthly` — `statKey = "SET:{setId}"` + `server_id` | **세트 단위** 통계 (통째 거래 집계) |
| 3 | `Σ TradeStatMonthly["ITEM:{pieceItemId}"]` | ② 없을 때 **피스별 합산** fallback |
| 4 | `MISSING` | ③에서도 총액 산출 불가 |

③ 피스 합산 시 일부 피스만 시세 없으면:

- 없는 피스만 `priceOverrides["ITEM:{pieceItemId}"]`로 보완 → `priceSource = MIXED`
- 보완 후에도 총액 산출 불가 → `price = null`, `priceSource = MISSING`

**`price_source` enum (장비 경로):** `USER_INPUT` | `TRADE_STAT` | `MIXED` | `MISSING`

### 6.3 가격 응답 메타데이터

```json
{
  "price": 1500000000,
  "formattedPrice": "15억",
  "priceSource": "TRADE_STAT",
  "tradeCount": 12,
  "priceBreakdown": [
    { "itemId": 1, "name": "투구", "price": 300000000, "source": "TRADE_STAT" }
  ]
}
```

`tradeCount`가 낮으면 UI에 **「표본 적음」** 배지 (`value-for-money-feature.ko.md` 정책과 동일).

---

## 7. 시나리오 멤버 · 용병 시뮬레이션

### 7.1 `affectedMemberId` (단일 필드)

시나리오마다 “변경이 일어나는 덱 멤버”는 **하나의 필드**로 표현한다. `targetMemberId` / `replacedMemberId` 이중 필드는 사용하지 않는다.

| `candidate_type` | `mode` | `affectedMemberId` | 의미 |
|------------------|--------|-------------------|------|
| `ITEM_SINGLE` / `ITEM_SET` | — | **필수** | 아이템·세트를 장착할 덱 멤버 |
| `MERCENARY` | `REPLACE` | **필수** | 교체되는 기존 멤버 슬롯 |
| `MERCENARY` | `APPEND` | **null** | 가상 멤버 추가 (overlay 전용) |

### 7.2 덱 12명 가득 찬 경우 (교체)

새 `UserDeckMember`를 DB에 만들지 않는다.

```
affectedMemberId=42, mode=REPLACE
  → overlay: member 42의 mercenaryId를 신규 용병으로 교체
  → level / 특성 / 스탯분배는 요청값을 MemberDpsInput에 적용
```

### 7.3 덱 12명 미만 (추가)

```
mode=APPEND, affectedMemberId=null
  → overlay에 가상 멤버 추가 (파티 버프·저항깎 합산 포함)
```

유저는 `affectedMemberId`를 보내지 않고, 레벨·특성·스탯분배만 지정한다.

### 7.4 가상 멤버 ID (`APPEND` — 서버 내부 전용)

`APPEND`는 DB에 아직 없는 용병을 덱에 넣는 시뮬레이션이므로, DPS 계산 루프에서 멤버를 구분할 **임시 식별자**가 서버 내부에 필요하다.  
이 값은 **API 요청·응답·DB에 노출하지 않는다.**

| 시나리오 | memberId |
|----------|----------|
| `REPLACE` | 기존 `affectedMemberId` (예: `42`) — DB와 동일 |
| `APPEND` | DB row 없음 → overlay가 **임시 ID** 부여 |

**MVP 구현 (채택): 음수 ID**

```
기존 덱: memberId 1, 2, … 8   (DB 실존)
추가 용병: memberId = -1       (시뮬레이션 전용, DB·API 비노출)
```

- `-1`, `-2` … 처럼 **음수**를 쓰는 이유: 실제 `UserDeckMember.id`(양수)와 절대 겹치지 않게 하기 위함.
- `MemberDpsInput.memberId`에도 서버가 `-1`을 채워 레벨·스탯분배를 묶는다.
- 유저가 요청 body에 `-1`을 넣지 않는다.

**대안 (미채택):** `scenario key` 맵(`"append-0"` → 가상 멤버 스펙)으로 `memberId` 숫자에 의존하지 않는 방식. overlay 구조는 더 명시적이나 `DpsCalculatorService` 리팩터 범위가 커져 MVP에서는 음수 ID를 사용한다.

### 7.5 입력 필드

| 입력 | DTO/형식 |
|------|----------|
| 레벨, 스탯분배 | `MemberDpsInput` (`level`, `bonusTarget`, `bonusAmount`) |
| 특성 | `CharacteristicSelection` (`characteristicId`, `selectedLevel`) |
| 구성 제약 | `DeckEquipmentValidator` (§4.4) 동일 규칙 적용 |

---

## 8. 엔티티 설계 — `DpsValueEvaluation`

### 8.1 필드

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long PK | NO | |
| `user_id` | Long FK | NO | |
| `deck_id` | Long | NO | 참고용 (FK 아님 — 덱 삭제 후에도 기록 유지) |
| `monster_id` | Long FK | NO | 평가 기준 몬스터 |
| `scenario_deck_snapshot_id` | Long FK | YES | 변경 후 덱 스냅샷 (`persist=false` 요청은 row 자체가 없음) |
| `candidate_type` | Enum | NO | `ITEM_SINGLE` \| `ITEM_SET` \| `MERCENARY` |
| `candidate_ref` | Long | NO | itemId / setId / mercenaryId |
| `mercenary_mode` | Enum | YES | `REPLACE` \| `APPEND` (용병 시나리오만) |
| `affected_member_id` | Long | YES | §7.1 — APPEND 시 null |
| `server_id` | Long FK | YES | `ITEM_*` 시 시세 조회에 쓴 `User.server.id`. `MERCENARY`는 null |
| `price` | Long | YES | 평가에 사용된 가격 |
| `price_source` | Enum | NO | `USER_INPUT` \| `TRADE_STAT` \| `MIXED` \| `MISSING` |
| `price_json` | TEXT | YES | 세트 breakdown 등 (선택) |
| `raw_dps_before` / `raw_dps_after` | Long | NO | |
| `adjust_dps_before` / `adjust_dps_after` | Long | NO | |
| `final_dps_before` / `final_dps_after` | Long | NO | `DpsResponse.totalDps` 매핑 |
| `raw_dps_delta` / `raw_dps_increase_rate` | Long / Double | NO | |
| `adjust_dps_delta` / `adjust_dps_increase_rate` | Long / Double | NO | |
| `final_dps_delta` / `final_dps_increase_rate` | Long / Double | NO | |
| `efficiency_per_eok_raw` | Double | YES | price 없으면 null |
| `efficiency_per_eok_adjust` | Double | YES | |
| `efficiency_per_eok_final` | Double | YES | 기본 정렬·추천 |
| `evaluation_hash` | String(64) | NO | 요청 canonical hash (중복 저장 방지) |
| `created_at` | LocalDateTime | NO | |

### 8.2 스냅샷 저장 전략

| 방식 | 채택 |
|------|------|
| 시나리오 스냅샷 1개 + before/after 수치 row에 denormalize | **채택** |
| baseline + scenario 스냅샷 2개 | 보류 (저장량 증가) |

`DeckSnapshot`은 `content_hash` UNIQUE로 동일 구성 자동 dedup (`EvaluationSnapshotBuilder` + `DeckSnapshotHashUtil`).  
`persist: false`이면 스냅샷·evaluation row **모두 저장하지 않는다**.

### 8.3 Unique 제약 · 중복 요청

```
UNIQUE (user_id, evaluation_hash)
```

`evaluation_hash` = canonical JSON(덱 시나리오 + 몬스터 + 후보 + 가격 + memberInputs)의 SHA-256.

**중복 요청 동작 (멱등):**

- 동일 `(user_id, evaluation_hash)`로 `persist: true` 요청이 다시 오면 **새 row를 만들지 않고 기존 row를 조회해 200 반환**한다.
- 409 Conflict를 반환하지 않는다 (더블클릭·재시도·`DeckSnapshot` dedup 패턴과 일관).
- `created_at`은 최초 저장 시점을 유지한다.

---

## 9. API 설계 (초안)

### 9.1 평가 실행

```
POST /api/calculator/dps/evaluations
```

**아이템 세트 — 주술 미사용 (lines 생략):**

```json
{
  "deckId": 10,
  "monsterId": 300,
  "resistanceType": "HITTING",
  "scenario": {
    "type": "ITEM_SET",
    "setId": 5,
    "affectedMemberId": 42
  },
  "persist": true
}
```

`lines` 생략 또는 `[]` → `setId` 피스 전부 **미주술**로 장착 (§3.4). 시세는 `User.server` 기준 (§6.2).

**아이템 세트 — 주술 사용 (lines 필수):**

주술 UI를 연 뒤에는 피스 수만큼 `lines`를 보낸다. `hasRitual=true`인데 `rituals` 비어 있으면 **400**.

```json
{
  "scenario": {
    "type": "ITEM_SET",
    "setId": 5,
    "affectedMemberId": 42,
    "lines": [
      {
        "itemId": 101,
        "quantity": 1,
        "sortOrder": 0,
        "equipmentDetail": {
          "enhanceLevel": 0,
          "hasRitual": true,
          "rituals": [{ "ritualId": 1, "outcome": "SUCCESS" }]
        }
      }
    ]
  }
}
```

> 예시는 1피스만 표기. 주술 사용 시 세트 전 피스(보통 5종)만큼 `lines`를 채운다.

**아이템 단품 예시:**

```json
{
  "scenario": {
    "type": "ITEM_SINGLE",
    "affectedMemberId": 42,
    "lines": [
      {
        "itemId": 201,
        "quantity": 1,
        "sortOrder": 0,
        "equipmentDetail": {
          "enhanceLevel": 5,
          "hasRitual": false
        }
      }
    ]
  }
}
```

`ITEM_SINGLE`도 `lines` **1개 필수**.

**용병 교체 예시:**

```json
{
  "deckId": 10,
  "monsterId": 300,
  "scenario": {
    "type": "MERCENARY",
    "mode": "REPLACE",
    "mercenaryId": 88,
    "affectedMemberId": 42,
    "level": 260,
    "bonusTarget": "STRENGTH",
    "bonusAmount": 700,
    "characteristics": [
      { "characteristicId": 1, "selectedLevel": 3 }
    ]
  },
  "price": 3000000000,
  "persist": true
}
```

**용병 추가 예시:**

```json
{
  "deckId": 10,
  "monsterId": 300,
  "scenario": {
    "type": "MERCENARY",
    "mode": "APPEND",
    "mercenaryId": 88,
    "level": 260,
    "bonusTarget": "STRENGTH",
    "bonusAmount": 700,
    "characteristics": []
  },
  "price": 3000000000,
  "persist": false
}
```

### 9.2 응답

**`persist: true`:**

```json
{
  "persisted": true,
  "evaluationId": 123,
  "scenarioDeckSnapshotId": 456,
  "before": { "raw": 1000000, "adjust": 1200000, "final": 800000 },
  "after":  { "raw": 1050000, "adjust": 1260000, "final": 850000 },
  "delta":  { "raw": 50000, "adjust": 60000, "final": 50000 },
  "increaseRate": { "raw": 5.0, "adjust": 5.0, "final": 6.25 },
  "efficiencyPerEok": { "raw": 0.17, "adjust": 0.20, "final": 0.21 },
  "price": 1500000000,
  "formattedPrice": "15억",
  "priceSource": "TRADE_STAT",
  "tradeCount": 12
}
```

**`persist: false`:**

```json
{
  "persisted": false,
  "evaluationId": null,
  "scenarioDeckSnapshotId": null,
  "before": { "raw": 1000000, "adjust": 1200000, "final": 800000 },
  "after":  { "raw": 1050000, "adjust": 1260000, "final": 850000 },
  "delta":  { "raw": 50000, "adjust": 60000, "final": 50000 },
  "increaseRate": { "raw": 5.0, "adjust": 5.0, "final": 6.25 },
  "efficiencyPerEok": { "raw": 0.17, "adjust": 0.20, "final": 0.21 },
  "price": 3000000000,
  "formattedPrice": "30억",
  "priceSource": "USER_INPUT",
  "tradeCount": null
}
```

- `evaluationId`·`scenarioDeckSnapshotId`는 **필드는 항상 포함**, 미저장 시 `null`.
- DPS·가성비 계산 결과는 `persist` 값과 무관하게 동일하게 반환한다.

### 9.3 검증 규칙

| 조건 | 처리 |
|------|------|
| `scenario.type == MERCENARY` && price null/0 | 400 Bad Request |
| `MERCENARY` + `REPLACE` && `affectedMemberId` null | 400 |
| `ITEM_*` && `affectedMemberId` null | 400 |
| `ITEM_SET` && `lines` 있음 && 피스 수·`itemId` 세트 정의와 불일치 | 400 |
| `ITEM_SINGLE` && `lines` 개수 ≠ 1 | 400 |
| `hasRitual=true` && `rituals` null 또는 빈 배열 | 400 (주술 선택 후 피스 주술 미완료) |
| `hasRitual=false` 또는 `equipmentDetail` 생략 | 해당 피스 미주술 (정상) |
| `ITEM_*` && 로그인 유저 `User.server` null | 400 (아이템 시세 조회 불가) |
| `MERCENARY` && `User.server` null | **허용** (시세 미사용, `price` 유저 입력만) |
| 용병 구성 제약 위반 | 400 |
| 아이템 착용 불가 (`ItemMercenaryRestriction`) | 400 |
| 덱·몬스터 없음 | 404 |
| `efficiencyPerEok_final ≤ 0` | `recommended = false` (추천 배지 미부여) |

### 9.4 인증

- `POST /api/calculator/dps/evaluations` 및 조회 API 모두 **인증 필수** (`authenticated`).
- 덱 API와 동일하게 본인 덱만 평가 가능. Guest·`permitAll` 미적용.

### 9.5 조회 API (확장)

```
GET /api/calculator/dps/evaluations          — 내 평가 목록 (본인만)
GET /api/calculator/dps/evaluations/{id}     — 상세 (본인만)
```

- 평가 기록은 **프라이버시** — 타 유저·사냥 허브에 노출하지 않는다.
- 클리어타임·사냥 허브와의 공개 연동은 데이터가 쌓인 뒤 별도 기획 (현재 범위 외).

---

## 10. `CatalogPriceResolverService` (개념)

```java
// 단품: §6.2 resolveItem 우선순위 ①→③
PriceResolution resolveItem(Long itemId, Long serverId, Map<String, Long> overrides);

// 세트: §6.2 resolveSet 우선순위 ①→④ (SET 통계 우선, 없으면 피스 합산)
PriceResolution resolveSet(Long setId, Long serverId, List<Long> pieceItemIds,
                           Map<String, Long> overrides);

record PriceResolution(
    Long totalPrice,
    PriceSource source,   // USER_INPUT | TRADE_STAT | MIXED | MISSING
    Integer tradeCount,
    Long serverId,        // 조회에 사용된 서버
    List<ItemPriceLine> breakdown
) {}
```

- 용병은 이 서비스를 **거치지 않음**.
- `serverId`는 **`ITEM_*` 시나리오에서만** `DpsValueEvaluationService`가 로그인 유저의 `User.server`에서 취득한다. API 요청 body에 넣지 않는다. `MERCENARY`는 `CatalogPriceResolverService`를 호출하지 않는다.
- 장비 시세는 **`TradeStatMonthly`** 를 `statKey = "ITEM:{itemId}"` **+ `server_id`** 로 조회 (§6.2 스키마 보완 후).
- override 키는 `"ITEM:{itemId}"` / `"SET:{setId}"` (§6.2).

---

## 11. 엣지 케이스

| 상황 | 처리 |
|------|------|
| 주술 UI 미사용 (`ITEM_SET`) | `lines` 생략·`[]` → 전 피스 미주술 (§3.4) |
| 주술 UI 사용 중 피스 미완료 | `hasRitual=true` && `rituals` 비어 있음 → 400 |
| `ITEM_*` + 유저 서버 미설정 | `User.server` null → 시세 조회 400 |
| `MERCENARY` + 유저 서버 미설정 | 평가 **허용** (시세 조회 없음) |
| 강화 미입력 | 거래 폼과 동일 검증 |
| 세트 피스별 주술 상이 | 라인별 `EquipmentDetailRequest` 입력 |
| 용병 교체 시 파티 버프 연쇄 | 전체 덱 full recalc (overlay 전체 반영) |
| 가격 0 / 시세 없음 | 가성비 null, DPS 증가분만 표시 |
| 덱 변경 후 과거 평가 | row에 before/after 수치 저장으로 재현 가능 |
| 증가율 ≤ 0 | 가성비 음수·0 가능, **추천 배지 미부여** |
| `finalDps` before = 0 | 증가율 0 반환 |
| 동일 시나리오 재요청 (`persist: true`) | 기존 evaluation row 반환 (§8.3) |

---

## 12. 구현 순서

| 단계 | 작업 |
|------|------|
| 0 | `DeckEquipmentValidator` — `DeckService` private 검증 로직 추출 (§4.4) |
| 1 | `DpsScenarioOverlay` record + `DpsScenarioOverlayFactory` (§4.2) |
| 1b | `LoadedDeckState` + `DeckCalculationState` + `DeckStateMerger` (§4.2.1) |
| 2 | `DpsCalculatorService.calculateWithOverlay(req, overlay)` — §4.2.1, `calculate()`는 overlay=null 위임 |
| 2a | `TradeStatDaily`·`TradeStatMonthly`에 `server_id` 추가 + 집계 배치 수정 (§6.2) |
| 3 | `CatalogPriceResolverService` — `ITEM:{id}` + `User.server.id` 로 서버별 시세 조회 |
| 4 | `DpsValueEvaluation` 엔티티 + Flyway migration |
| 5 | `EvaluationSnapshotBuilder` — 클리어타임 빌더와 분리 (§4.5) |
| 6 | `DpsValueEvaluationService` + `POST /api/calculator/dps/evaluations` |
| 7 | 단위 테스트 (고정 덱·몬스터 fixture, before/after·가격 조회 검증) |
| 8 | (후속) 평가 목록 API, 사냥 허브 추천 연동 (`recommend-plan.md` 3단계) |

---

## 13. 기존 기능과의 관계

| 기능 | 관계 |
|------|------|
| `POST /api/calculator` | 단순 배율 가성비 — 수동 스펙 입력용, 병행 유지 |
| `POST /api/calculator/dps` | 덱 기준 단일 DPS — 평가 API 내부에서 재사용 |
| `UserClearTime` | DPS 3종(`raw`/`adjust`/`final`) 저장 패턴·DeckSnapshot 연동 참고 |
| `recommend-plan.md` 3단계 | 평가 데이터 축적 후 맞춤 스펙업 추천 입력으로 활용 |
| `value-for-money-feature.ko.md` | 능력치÷가격 랭킹(카탈로그 단위) — 본 기능(덱 맞춤 평가)과 상호 보완 |
