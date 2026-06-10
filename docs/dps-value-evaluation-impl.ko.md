# DPS 가성비 평가 기능 구현 작업 기록

이 문서는 `docs/dps-value-evaluation.ko.md` 기획을 기반으로 진행된 백엔드 구현 작업을 정리한다.

---

## 구현 범위 요약

| 단계 | 내용 | 상태 |
|------|------|------|
| Step 0 | 기반 도메인 설계 · 공통 계산 인프라 (상세 미기록) | ✅ 완료 |
| Step 1 | DPS 계산기 코어 구현 (상세 미기록) | ✅ 완료 |
| Step 1b | Overlay·ScenarioItemType 패턴 구현 (상세 미기록) | ✅ 완료 |
| Step 2 | 가성비 계산기 요청/응답 DTO 기반 구축 (상세 미기록) | ✅ 완료 |
| Step 2a | `TradeStatDaily` · `TradeStatMonthly`에 `server_id` 추가 | ✅ 완료 |
| Step 3 | `CatalogPriceResolverService` 구현 (§6.2 가격 우선순위) | ✅ 완료 |
| Step 4 | `DpsValueEvaluation` 엔티티 생성 | ✅ 완료 |
| Step 5 | `EvaluationSnapshotBuilder` 구현 | ✅ 완료 |
| Step 6 | `DpsValueEvaluationService` + `DpsValueEvaluationController` 구현 | ✅ 완료 |
| 테스트 | `DpsValueEvaluationServiceTest` + `DpsValueEvaluationResponseTest` | ✅ 완료 |
| Step 7 | 평가 목록·상세 조회 API (`GET /api/calculator/dps/evaluations`) | ✅ 완료 |

---

## Step 2a — TradeStatDaily · TradeStatMonthly server_id 추가

### 배경

가격 통계는 서버별로 분리되어야 하지만, 기존 `TradeStatDaily` / `TradeStatMonthly` 엔티티에 `server_id` 컬럼이 없었다.

### 변경 파일

**`domain/trade/TradeStatDaily.java`**
- `@ManyToOne Server server` 필드 추가 (`nullable = false`)
- unique constraint: `(stat_date, stat_key)` → `(stat_date, stat_key, server_id)`

**`domain/trade/TradeStatMonthly.java`**
- 동일 패턴 적용

**`trade/repository/TradeStatDailyRepository.java`**
- `findByStatKeyAndStatDateAndServer(String, LocalDate, Server)` 추가
- `upsertAccumulate` native SQL에 `server_id` 컬럼 및 `:serverId` 파라미터 추가
- `findByStatKeyAndDateRange`에 `serverId` 필터 추가

**`trade/repository/TradeStatMonthlyRepository.java`**
- 모든 조회 메서드에 `server_id` 필터 추가
- `findByStatKeysAndServerIdAndMonth(List<String>, Integer, String)` 추가 — 세트 피스 일괄 조회
- `findByStatKeyAndServerIdAndMonth(String, Integer, String)` 추가 — 단품 조회

**`catalog/repository/ServerRepository.java`**
- `Optional<Server> findByName(String name)` 추가

**`trade/service/TradeStatService.java`**
- `upsertDailyStat`에 `Server server` 파라미터 추가
- `getDailyHistory`에 `Integer serverId` 파라미터 추가

**`trade/controller/PriceHistoryController.java`**
- `getPriceHistory`에 `@RequestParam Integer serverId` 추가

**`chat/service/ChatService.java`**
- `serverRepository.findByName(serverSnapshot)`으로 서버 resolve 후 stat upsert 호출
- 서버명으로 조회 실패 시 warn 로그 + skip (NOT NULL 위반 방지)

**테스트 수정**
- `TradeStatServiceTest` — `SERVER` / `SERVER_ID` 파라미터 추가
- `ChatServiceTest` — `@Mock ServerRepository` 추가, `@BeforeEach`에 stub 추가

---

## Step 3 — CatalogPriceResolverService (§6.2 가격 우선순위)

### 신규 DTO

| 파일 | 설명 |
|------|------|
| `calculator/dto/response/PriceSource.java` | `USER_INPUT \| TRADE_STAT \| MIXED \| MISSING` |
| `calculator/dto/response/ItemPriceLine.java` | 세트 피스별 가격 라인 `(itemId, price, source)` |
| `calculator/dto/response/PriceResolution.java` | `(totalPrice, source, tradeCount, serverId, breakdown)` |

### CatalogPriceResolverService 구현

**`calculator/service/CatalogPriceResolverService.java`**

```
resolveItem(itemId, serverId, overrides)
  1순위: priceOverrides["ITEM:{itemId}"] → USER_INPUT
  2순위: TradeStatMonthly 직전 달 avgPrice → TRADE_STAT
  3순위: 없으면 totalPrice=null → MISSING

resolveSet(setId, serverId, pieceItemIds, overrides)
  1순위: priceOverrides["SET:{setId}"] → USER_INPUT (전체 세트 단가)
  2순위: TradeStatMonthly 세트 기준 시세 조회 → TRADE_STAT
  3순위: 개별 피스 합산 (피스별 priceOverrides 우선, 없으면 TradeStatMonthly) → USER_INPUT / MIXED / TRADE_STAT
  4순위: 미조회 피스 존재 → totalPrice=null → MISSING
```

**`test/calculator/service/CatalogPriceResolverServiceTest.java`** — 8개 테스트

---

## Step 4 — DpsValueEvaluation 엔티티

**`domain/calculator/DpsValueEvaluation.java`**

- 테이블: `dps_value_evaluations`
- unique constraint: `(user_id, evaluation_hash)` → 중복 저장 방지
- before/after DPS 3종(raw/adjust/final) × delta · increaseRate 저장
- `efficiencyPerEokRaw/Adjust/Final` — 가성비(증가율 ÷ 가격 억)
- `evaluationHash` SHA-256 64자 — 멱등 처리 키
- `createdAt` — `LocalDateTime.now()` 생성자에서 자동 설정
- Flyway 마이그레이션 파일은 생성하지 않음 (ddl-auto로 관리)

**`calculator/repository/DpsValueEvaluationRepository.java`**

```java
Optional<DpsValueEvaluation> findByUserIdAndEvaluationHash(Long userId, String evaluationHash);
```

---

## Step 5 — EvaluationSnapshotBuilder

**`calculator/service/EvaluationSnapshotBuilder.java`**

- `deck_snapshots` 테이블과 `DeckSnapshotHashUtil`을 `DeckSnapshotBuilderService`와 공유
- `buildOrReuse(DeckCalculationState calcState, DpsResponse afterDps, ResistanceType resistanceType)`
  - `LoadedMember` → `DeckMemberResponse` 변환 (`memberInputs` 맵 기반)
  - `afterDps.totalResistPierce()` → `totalResDown`
  - `null` effects (평가 스냅샷은 덱 효과 메타 생략)
  - canonical JSON → SHA-256 → `findByContentHash` 재사용 or 신규 저장
- `DeckSnapshotBuilderService`와 달리 DB 재조회 없음 — `DeckCalculationState`에서 직접 변환

---

## Step 6 — DpsValueEvaluationService + Controller

### DpsCalculatorService 변경

**`calculator/service/DpsCalculatorService.java`**

```java
public DeckCalculationState prepareState(DpsRequest req, @Nullable DpsScenarioOverlay overlay)
```

기존 private `loadDeckState` + `deckStateMerger.merge` 패턴을 외부에서 접근 가능하도록 공개. 가성비 평가 서비스에서 저항 타입 자동 판별 및 스냅샷 생성에 사용.

### DpsValueEvaluationService

**`calculator/service/DpsValueEvaluationService.java`**

```
evaluate(userId, req):
  1. 유저 로드 + 서버 요구사항 검증
  2. overlay 생성 (DpsScenarioOverlayFactory)
  3. before/after DPS 계산 (calculateWithOverlay)
  4. 가격 조회 (CatalogPriceResolverService)
  5. persist=false → ofTransient 반환 (DB 저장 없음)
  6. evalHash 계산 → 중복 여부 조회 → 기존 있으면 ofPersisted 반환
  7. resistanceType 결정 (요청값 or prepareState 기반 자동 판별)
  8. EvaluationSnapshotBuilder.buildOrReuse
  9. DpsValueEvaluation 빌드 → save → ofPersisted 반환
```

**중복 처리 방식**: `AlreadyEvaluatedException` 내부 예외 대신 단순 `if (existing.isPresent()) return` 패턴으로 처리 (컨트롤러 패키지에서 접근 불가 문제 해결).

### DpsValueEvaluationResponse

**`calculator/dto/response/DpsValueEvaluationResponse.java`**

| 중첩 레코드 | 필드 |
|-------------|------|
| `DpsTriple` | `Long raw, Long adjust, Long finalDps` — DPS 정수값 |
| `DpsRateTriple` | `Double raw, Double adjust, Double finalDps` — 증가율(%) |
| `EfficiencyTriple` | `Double raw, Double adjust, Double finalDps` — 가성비 |

`formatPrice(Long)` → `"N억 N천만"` 형식 변환

### DpsValueEvaluationController

**`calculator/controller/DpsValueEvaluationController.java`**

```
POST /api/calculator/dps/evaluations
  @PreAuthorize("isAuthenticated()")
  @AuthenticationPrincipal Long userId
  @RequestBody @Valid DpsEvaluationRequest
  → DpsValueEvaluationResponse
```

---

## 버그 픽스

### 1. NotificationServiceTest SSE→WebSocket 회귀

SSE emitter 기반 테스트가 WebSocket 방식으로 전환된 서비스를 검증하지 못하던 문제.

- `@Mock SimpMessagingTemplate messagingTemplate` 추가
- `convertAndSendToUser()` verify로 교체

### 2. ChatServiceTest NPE

`@InjectMocks`가 `ServerRepository`를 주입하지 못해 NPE 발생.

- `@Mock ServerRepository serverRepository` 추가
- `@BeforeEach`에 `when(serverRepository.findByName(any())).thenReturn(Optional.of(server))` 추가

### 3. evaluationRepository.save() 반환값 무시

```java
// 수정 전
evaluationRepository.save(evaluation);
return DpsValueEvaluationResponse.ofPersisted(evaluation, before, after); // ID=null

// 수정 후
DpsValueEvaluation saved = evaluationRepository.save(evaluation);
return DpsValueEvaluationResponse.ofPersisted(saved, before, after);
```

JPA `IDENTITY` 전략에서 save 후 반환된 엔티티에 ID가 채워진다. 원본 엔티티를 쓰면 ID가 null.

### 4. DpsTriple 타입 불일치 컴파일 오류

`increaseRate` 필드에 `DpsTriple(Long)` 타입을 사용했으나 double 값을 전달해 컴파일 실패.
→ `DpsRateTriple(Double)` 를 별도 레코드로 분리.

---

## 테스트 현황

### DpsValueEvaluationServiceTest (7개)

| 테스트명 | 검증 내용 |
|----------|-----------|
| `persist_false_응답만_반환_DB저장없음` | persist=false → save 미호출 |
| `persist_true_신규요청_저장후_persisted_반환` | 신규 → evaluationId·snapshotId 정상 반환 |
| `persist_true_중복hash_기존결과_반환_추가저장없음` | 중복 hash → snapshotBuilder·save 미호출 |
| `ITEM_SINGLE_서버미설정_400_반환` | user.server=null + ITEM 시나리오 → 400 |
| `MERCENARY_price_null_400_반환` | price=null → 400 |
| `MERCENARY_price_0_400_반환` | price=0 → 400 |
| `사용자없음_404_반환` | userRepository empty → 404 |

### DpsValueEvaluationResponseTest (15개)

| 테스트명 | 검증 내용 |
|----------|-----------|
| `formatPrice_단위별_포맷_검증` (파라미터화 6종) | 억/억천만/천만/전 단위, null |
| `delta_정상계산` | raw·adjust·final delta |
| `increaseRate_raw_20percent_정상계산` | 증가율 소수점 |
| `increaseRate_before_0이면_0반환` | 0 나누기 방어 |
| `efficiency_1억에_10percent상승_10점` | 가성비 수치 |
| `efficiency_price_null_null반환` | price null → null |
| `efficiency_price_0_null반환` | price 0 → null |
| `ofTransient_persisted_false_evaluationId_null` | 구조 검증 |

---

## Step 7 — 평가 목록·상세 조회 API

### Repository 쿼리 추가

**`calculator/repository/DpsValueEvaluationRepository.java`**

```java
@Query("SELECT e FROM DpsValueEvaluation e JOIN FETCH e.monster WHERE e.user.id = :userId ORDER BY e.createdAt DESC")
Page<DpsValueEvaluation> findByUserIdWithMonster(@Param("userId") Long userId, Pageable pageable);

@Query("SELECT e FROM DpsValueEvaluation e JOIN FETCH e.monster WHERE e.id = :id AND e.user.id = :userId")
Optional<DpsValueEvaluation> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
```

`JOIN FETCH e.monster` — 목록/상세 모두 몬스터명이 필요하므로 N+1 방지를 위해 fetch join 사용.
`findByIdAndUserId` — 타인 평가 조회를 404로 처리(존재 여부 비노출).

### DpsEvaluationSummary DTO

**`calculator/dto/response/DpsEvaluationSummary.java`**

목록 응답 전용 경량 레코드. `DpsValueEvaluation` 엔티티에서 직접 변환한다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `evaluationId` | `Long` | 평가 PK |
| `candidateType` | `ScenarioItemType` | `ITEM_SINGLE \| SET \| MERCENARY` |
| `candidateRef` | `Long` | itemId / setId / mercenaryId |
| `mercenaryMode` | `MercenaryMode` | nullable |
| `monsterId` / `monsterName` | `Long` / `String` | 몬스터 정보 |
| `finalDpsIncreaseRate` | `double` | final DPS 증가율(%) |
| `efficiencyPerEokFinal` | `Double` | final 가성비 (price null이면 null) |
| `price` / `formattedPrice` | `Long` / `String` | 가격 + "N억 N천만" 형식 |
| `priceSource` | `PriceSource` | 가격 출처 |
| `createdAt` | `LocalDateTime` | 저장 시각 |

### DpsValueEvaluationResponse.ofStored 팩토리

**`calculator/dto/response/DpsValueEvaluationResponse.java`**

GET 상세 조회 전용 팩토리. DPS 재계산 없이 DB 저장 값에서 복원한다.

```java
public static DpsValueEvaluationResponse ofStored(DpsValueEvaluation eval) {
    // DB 컬럼 → nested record 매핑만 수행; 연산 없음
}
private static double round2(double v) { return Math.round(v * 100) / 100.0; }
```

`increaseRate` 필드는 `DpsRateTriple(Double)` 타입이므로 `round2()`로 소수점 2자리 반올림 후 세팅.

### DpsValueEvaluationQueryService

**`calculator/service/DpsValueEvaluationQueryService.java`**

```java
@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class DpsValueEvaluationQueryService {

    public Page<DpsEvaluationSummary> getMyEvaluations(Long userId, Pageable pageable) {
        return evaluationRepository.findByUserIdWithMonster(userId, pageable)
                .map(DpsEvaluationSummary::from);
    }

    public DpsValueEvaluationResponse getMyEvaluation(Long userId, Long evaluationId) {
        var eval = evaluationRepository.findByIdAndUserId(evaluationId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "평가 결과를 찾을 수 없습니다. id=..."));
        return DpsValueEvaluationResponse.ofStored(eval);
    }
}
```

DPS 재계산 없음 — DB 저장 값만 읽어 반환. 목록/상세 모두 본인 데이터만 접근 가능.

### DpsValueEvaluationController GET 엔드포인트 추가

**`calculator/controller/DpsValueEvaluationController.java`**

```
GET /api/calculator/dps/evaluations
  @RequestParam page (기본 0), size (기본 20, 최대 100 클램프)
  Sort: createdAt DESC
  → Page<DpsEvaluationSummary>

GET /api/calculator/dps/evaluations/{id}
  @PathVariable Long id
  → DpsValueEvaluationResponse (persisted=true)
  타인 평가: 404
```

---

## 테스트 현황 (Step 7 추가분)

### DpsValueEvaluationQueryServiceTest (5개)

`@MockitoSettings(strictness = Strictness.LENIENT)` — `buildEvaluation()` 헬퍼가 세팅하는 stub 중 일부 테스트에서 사용하지 않는 것이 있어 `LENIENT` 설정 필요.

| 테스트명 | 검증 내용 |
|----------|-----------|
| `getMyEvaluations_페이징결과_summary_필드_매핑` | Page 매핑, 전 필드 값 검증 |
| `getMyEvaluations_빈결과_빈페이지_반환` | empty page 반환 |
| `getMyEvaluation_DB저장값_response로_복원` | before/after/delta/rate/efficiency 전 필드 |
| `getMyEvaluation_타인평가_404` | `findByIdAndUserId` empty → `ResponseStatusException` 404 |
| `getMyEvaluation_스냅샷있으면_snapshotId_반환` | `scenarioDeckSnapshotId` 정상 매핑 |
