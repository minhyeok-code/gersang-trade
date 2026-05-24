# 크롤링/배치 작업 기능 검증 리뷰 (코드 수정 없음)

기준일: 2026-03-19  
대상 범위(패키지): `back/gersangtrade/src/main/java/org/example/gersangtrade/crawler/*`  
Job 구성:

- **Job 1**: 마스터 데이터 수집 (`MasterDataJobConfig`)
- **Job 2**: 가격 데이터 수집 (`PriceCrawlJobConfig`)

---

## 1) 전체 구조/운영 방식 확인

### Job 1 — 마스터 데이터 수집

- 설정: `crawler/job/MasterDataJobConfig.java`
- Step 구성:
  - Step1 `ItemListTasklet`: geota 아이템 목록 → `items`, `gems` UPSERT
  - Step2 `ItemDetailReader/Writer`: gerniverse 아이템 상세 + 이미지 S3 업로드 + `equipment_items`/`material_items`/`item_stats` 적재
  - Step3 `MercenaryListTasklet`: geota 용병 목록 → `mercenaries` UPSERT
  - Step4 `MercenaryDetailReader/Writer`: gerniverse 용병 상세 + 이미지 + 재료(`mercenary_materials`) 재적재
- 실행 방식: **관리자 수동 트리거**
  - `POST /admin/crawler/master` (`admin/controller/CrawlerAdminController.java`)

### Job 2 — 월간 가격 집계

- 설정: `crawler/job/PriceCrawlJobConfig.java`
- Step 구성:
  - Step1 `PriceCrawlTasklet`: 서버별 육의전 가격 수집 → IQR 이상치 제거 → `material_price_history` UPSERT
- 실행 방식:
  - 스케줄: 매월 1일 03:00 `@Scheduled(cron = "0 0 3 1 * *")` (`crawler/scheduler/CrawlerScheduler.java`)
  - 관리자 수동 트리거: `POST /admin/crawler/price`

### 공통

- 중복 실행 제한 우회: JobParameter에 `timestamp`를 넣어 매번 새로운 JobInstance로 실행
- 스케줄링 활성화: `@EnableScheduling` (`GersangtradeApplication.java`)
- 관리자 API 권한: `@PreAuthorize("hasRole('ADMIN')")` + `SecurityConfig`의 `/admin/`** role 제한이 전제

---

## 2) 잘 된 점(유지 추천)

- **Job 분리**: 마스터데이터(저빈도)와 가격 집계(월간)를 Job으로 분리한 것은 운영에 유리함
- **Reader reset 리스너**: 재실행 시 `ItemDetailReader.reset()` / `MercenaryDetailReader.reset()`로 큐 상태를 초기화하려는 의도가 명확함
- **IQR 이상치 제거 + 최소 샘플 제한(5건)**: 집계 품질 기준을 코드로 고정(`IqrCalculator.MIN_SAMPLE`)
- **HTTP fetch 재시도/딜레이**: `JsoupFetcher`에 기본적인 재시도/딜레이가 있어 외부 사이트 일시 오류에 완충이 있음

---

## 3) 중요 이슈(데이터 누락/오동작 가능성 높음)

### 이슈 1) 보석(GEM) 적재에서 “주술별 변형”이 누락될 수 있음 (중복 제거 키 버그)

- 위치: `crawler/tasklet/ItemListTasklet.java`
- 문제:
  - `processedNames`의 dedupe 키가 `cleanName:type:gemGrade` 형태라서,
  **같은 보석명 + 같은 등급(주술됨)** 인데 **주술명(ritualName)이 다른 보석**을 동일 항목으로 취급하고 skip할 수 있음.
  - 결과: `gems` 테이블에 “주술됨 보석”이 **일부만 저장되는 데이터 누락** 위험.

### 이슈 2) 장비 플래그(주술가능/홈버전 존재) 집계가 실제 저장에 반영되지 않음

- 위치:
  - 집계: `ItemListTasklet`의 `equipmentFlags`(ritualApplicable, hasSlotOption)
  - 저장: `crawler/writer/ItemDetailWriter.java`의 `EquipmentItem` 생성
- 문제:
  - `EquipmentItem` 엔티티에는 `ritualApplicable`, `hasSlotOption` 필드가 있는데,
  Writer 생성 시 기본값(false)로만 생성되고, Tasklet에서 집계한 플래그가 **어디에도 적용되지 않음**.
  - 결과: “홈이있는/주술” 버전 존재 여부가 DB에 반영되지 않아 기능(필터링/표시)이 틀어질 수 있음.

### 이슈 3) 용병 상세 적재 주석/의도와 실제 구현이 불일치

- 위치: `crawler/writer/MercenaryDetailWriter.java`
- 발견:
  - 주석에는 “저항깎·속성값·용병종류 업데이트”라고 되어 있으나,
  실제 호출은 `mercenary.updateSpec(resistPierce, elementValue, imageUrl)` 뿐이라 **mercenaryType은 갱신되지 않음**.
  - 주석에는 “재료 아이템이 DB에 없으면 신규 생성 후 저장”이라고 되어 있으나,
  실제 코드는 `log.debug("재료 아이템 미등록 (skip)")`만 하고 **생성하지 않음**.

### 이슈 4) 용병 이미지 URL이 재실행 시 null로 덮여써질 수 있음

- 위치: `MercenaryDetailWriter.applyMercenaryData()`
- 문제:
  - `data.imageKey()`가 없거나 S3 업로드 실패 시 `s3Url=null`인데,
  `updateSpec(..., s3Url)`가 **기존 imageUrl을 null로 바꿀 수 있음**(재실행/부분 실패 시 데이터 손실 위험).

---

## 4) 성능/트랜잭션/멱등성 관점 이슈

### 4.1 ItemDetailWriter의 ItemStat 저장 로직이 매우 비효율적 (대량 데이터에서 병목)

- 위치: `crawler/writer/ItemDetailWriter.java`
- 문제:
  - stat 하나 처리할 때마다 `itemStatRepository.findAllByStatTypeWithItem(...)`로 **해당 statType 전체를 매번 로드**하고,
  그중 itemId로 필터링해 존재 여부를 판정함.
  - 아이템 수×스탯 수에 비례해 DB 부하가 폭발(N×M 형태)할 수 있음.
- 추가로:
  - `element`를 항상 `Element.NONE`으로 저장 → “속성별 능력치”를 실제로는 표현 못함(문서/엔티티 주석과 불일치 가능)
  - 기존 stat이 이미 존재할 때 “값 갱신(update)”이 아니라 “스킵”만 수행 → 소스 변경 시 반영 불가

### 4.2 PriceCrawlTasklet의 집계 연월(yearMonth)과 계산기 소비 방식이 엇갈릴 수 있음

- 위치:
  - `PriceCrawlTasklet`: `yearMonth = YearMonth.now()` (현재 월)
  - `CalculatorService`: 가격 기본값은 “직전 달”(`LocalDate.now().minusMonths(1)`)
- 결과:
  - 1일 03:00에 Job이 “이번 달”로 저장하면,
  계산기는 “지난 달”을 조회하므로 **항상 한 달치 공백**이 생길 수 있음(운영 시점에 따라).
- 문서(`docs/calculator.md`)는 “직전 달 평균가”를 기본값으로 명시하고 있어, 이 정합성은 특히 중요함.

### 4.3 PriceCrawlTasklet 트랜잭션 범위/부분 커밋 정책이 불명확

- 위치: `PriceCrawlTasklet.execute()`가 `@Transactional`
- 특징:
  - 서버 단위 예외를 catch하고 skip → “부분 성공”을 허용하는 형태
  - 하지만 트랜잭션은 메서드 전체 범위라,
  “어디까지를 원자적으로 묶을지(서버 단위 vs 전체 서버)” 정책이 코드만으로는 모호함.

---

## 5) 크롤링 안정성(외부 사이트 변경 내성) 이슈

- 여러 곳에 `⚠ 실제 선택자 확인 필요` 주석이 있으며, 실제로 선택자가 바뀌면 “조용히 0건”이 될 수 있음
  - 예: `PriceCrawlTasklet`의 `doc.select("table tbody tr")`
  - 예: `ItemListTasklet`/`MercenaryListTasklet`의 `li.cursor-pointer`
- 운영 관점 권장:
  - “0건 수집” 같은 비정상 상태를 **에러/알람으로 승격**하거나, 최소 기대 건수 기반 fail-fast가 필요할 수 있음

---

## 6) 권장 검증 시나리오(테스트/운영 점검용)

- **Job 1**
  - 실행 후 `gems`에 “주술됨 보석”이 주술 종류별로 다수 존재하는지(누락 여부)
  - `equipment_items.has_slot_option`, `equipment_items.ritual_applicable` 값이 실제로 반영되는지(현재 코드상 반영되지 않을 가능성)
  - 용병 재료 적재 시 “재료 미등록” 로그가 다량 발생하는지(아이템 마스터 누락 여부)
  - 재실행 시 용병 `image_url`이 null로 돌아가는 케이스가 있는지
- **Job 2**
  - `material_price_history`의 `year_month`가 계산기에서 조회하는 연월과 맞는지(직전 달 vs 현재 달)
  - 페이지 수가 50을 초과하는 서버가 있는지(MAX_PAGES 컷오프)
  - 아이템명이 DB `items.name`과 1:1 매칭되는지(표기 차이로 skip되는 비율 확인)

---

## 7) 코드 검증 결과 및 수정 내역 (2026-03-19)

아래 항목은 실제 코드와 리뷰를 비교 검증하여 판정한 결과다.

---

### ✅ 수정 완료

#### 이슈 1 — GEM dedup 키 버그 (`ItemListTasklet.java`)

- **판정**: 이슈 확인됨. 수정 완료.
- **근거**: `processedNames`의 dedup 키가 `cleanName:type:gemGrade`였으므로, 동일 보석명+`주술됨` 등급인데 ritualName이 다른 항목(예: `<태산북두> 흑요석`과 `<화운신장> 흑요석`)이 같은 키로 처리되어 두 번째 이후 주술 변형이 skip되었다. `upsertGem()`은 `name + gemGrade + ritualId` 조합으로 별도 저장하는 올바른 로직을 갖고 있었으나 dedup 단계에서 막혔다.
- **수정**: 키를 `cleanName:type:gemGrade:ritualName`으로 변경.

#### 이슈 3-A — `mercenaryType` 미갱신 (`MercenaryDetailWriter.java`, `Mercenary.java`)

- **판정**: 이슈 확인됨. 수정 완료.
- **근거**: `GerniverseParser.parseMercenary()`는 `MercenaryData.mercenaryType()`을 올바르게 파싱하지만, `MercenaryDetailWriter.applyMercenaryData()`에서 `mercenary.updateSpec(resistPierce, elementValue, s3Url)` 호출 시 `mercenaryType`을 전달하지 않았다. `Mercenary.updateSpec()`도 해당 파라미터를 받지 않았으므로 mercenaryType은 초기 저장값에서 갱신되지 않았다.
- **수정**: `Mercenary.updateSpec()`에 `mercenaryType` 파라미터 추가. null/공백이면 기존 값 유지. `MercenaryDetailWriter`에서 `data.mercenaryType()`을 전달하도록 변경.

#### 이슈 3-B — 주석/의도 불일치 (`MercenaryDetailWriter.java`)

- **판정**: 이슈 확인됨. 수정 완료.
- **근거**: "재료 아이템이 DB에 없으면 신규 생성 후 저장 (ItemListStep에서 누락된 재료 보완)"라는 주석과 달리 실제 코드는 skip만 수행했다. skip이 올바른 동작이며(아이템 등록 책임은 ItemListTasklet에 있음), 주석이 잘못되었다.
- **수정**: 주석을 실제 동작에 맞게 수정.

#### 이슈 4 — 용병 imageUrl null 덮어쓰기 (`Mercenary.java`)

- **판정**: 이슈 확인됨. 수정 완료.
- **근거**: `applyMercenaryData()`에서 `data.imageKey()`가 null이거나 S3 업로드가 null을 반환하면 `s3Url = null`이 되고, `Mercenary.updateSpec(..., null)` 호출로 기존 `imageUrl`이 null로 덮어써졌다.
- **수정**: `Mercenary.updateSpec()`에서 `imageUrl`이 null이면 기존 값을 유지하도록 수정. (이슈 3-A 수정과 함께 처리)

#### 이슈 4.1 — ItemStat 존재 확인 비효율 (`ItemDetailWriter.java`, `ItemStatRepository.java`)

- **판정**: 이슈 확인됨. 수정 완료.
- **근거**: 기존 코드는 `findAllByStatTypeWithItem(stat.statType())`으로 해당 statType의 **전체 아이템 목록**을 DB에서 로드한 후, 메모리에서 `item.getId()`로 필터링하는 방식이었다. 아이템 수 × statType 수에 비례해 DB 부하가 급증하는 구조.
- **수정**: `ItemStatRepository`에 `existsByItemIdAndStatType(Long itemId, StatType statType)` 쿼리 메서드 추가. `ItemDetailWriter`에서 이를 사용해 개별 확인으로 변경.
- **남은 이슈**: `element`가 항상 `Element.NONE`으로 저장되는 부분, 기존 stat 갱신 없이 skip만 하는 부분은 속성별 능력치 구분 정책 확정 후 별도 처리 필요.

#### 이슈 4.2 — yearMonth 현재 달 vs 직전 달 불일치 (`PriceCrawlTasklet.java`)

- **판정**: 이슈 확인됨. 수정 완료.
- **근거**: `PriceCrawlTasklet`은 `YearMonth.now()`로 **현재 달**을 저장하고, `CalculatorService`는 `LocalDate.now().minusMonths(1)`로 **직전 달**을 조회한다. 매월 1일 03:00에 Job이 실행되면 `2026-03`으로 저장되지만 계산기는 `2026-02`를 조회해 항상 한 달치 공백이 발생한다. 의미상으로도 1일에 실행되는 Job은 "지난 달의 거래 데이터"를 집계하는 것이므로 `minusMonths(1)`이 맞다.
- **수정**: `PriceCrawlTasklet`에서 `YearMonth.now().minusMonths(1)` 사용으로 변경.

---

### ❌ 수정 보류 (이유 포함)

#### 이슈 2 — 장비 플래그(`ritualApplicable`, `hasSlotOption`)가 저장에 반영되지 않음

- **판정**: 이슈 확인됨. 수정 보류.
- **근거**: `ItemListTasklet(Step1)`에서 수집한 `equipmentFlags` Map은 `ItemDetailWriter(Step2)`에서 전혀 접근할 수 없다. `GerniverseParser.ItemData`에도 이 두 필드가 없다. 수정하려면 (a) `JobExecutionContext`를 통해 flags Map을 직렬화/전달하거나, (b) `GerniverseParser.parseItem()`이 gerniverse 페이지에서 주술 가능 여부/홈 버전 존재 여부를 직접 파싱하도록 보강해야 한다. 두 방법 모두 구조적 변경이 필요하므로 이번 수정 범위에서 제외한다.
- **결론**: `EquipmentItem.ritualApplicable`과 `hasSlotOption`은 현재 항상 `false`로 저장됨. 이 필드에 의존하는 기능(필터링, UI 표시) 구현 전까지는 실질적 영향 없음.

#### 이슈 4.3 — `PriceCrawlTasklet` 트랜잭션 범위 정책 모호

- **판정**: 이슈 확인됨. 수정 보류.
- **근거**: `@Transactional`이 `execute()` 전체에 걸려 있어 서버 단위 catch+skip "부분 성공" 패턴과 트랜잭션 원자성 범위가 불일치한다. 완전한 해결은 서버별로 별도 트랜잭션을 구성(프록시 경계 조정)해야 하는 구조적 변경이 필요하다. 현재 코드에서 서버 예외가 checked 예외 형태로 catch되므로 실제로 트랜잭션이 rollback-only로 마킹될 가능성은 낮으나, 서버별 `@Transactional` 분리를 추후 개선 사항으로 남긴다.

---

## 8) 코드-문서 대조 피드백 (2026-03-19)

아래는 본 문서(섹션 7)의 “수정 완료/보류” 주장에 대해 **실제 코드를 다시 열어 확인한 결과**다.

### ✅ 문서의 “수정 완료” 주장 = 코드에 반영됨 (확인 완료)

- **이슈 1 (GEM dedup 키)**: 실제로 `ItemListTasklet`의 중복 제거 키가 `...:ritualName`까지 포함하도록 변경되어 있음.
- **이슈 3-B (주석 정정)**: `MercenaryDetailWriter`의 “재료 미등록은 skip” 주석/로직이 일치함.
- **이슈 4 (imageUrl null 덮어쓰기 방지)**: `Mercenary.updateSpec()`에서 `imageUrl != null`일 때만 갱신하도록 변경되어 있음.
- **이슈 4.1 (ItemStat 존재 확인 최적화)**: `ItemStatRepository.existsByItemIdAndStatType(...)`가 추가되어 있고, `ItemDetailWriter`가 이를 사용함.
- **이슈 4.2 (집계 연월 정합성)**: `PriceCrawlTasklet`이 `YearMonth.now().minusMonths(1)`로 저장하도록 변경되어 있음.

### ⚠️ “수정 완료”로 표기됐지만, 코드/주석 기준으로 **부분해결**로 보이는 지점

#### 이슈 3-A — “저항깎/속성값 null이면 기존 유지”는 아직 보장되지 않음

- `Mercenary.updateSpec(...)`는 `mercenaryType`/`imageUrl`은 null/공백 방어가 있으나,
`resistPierce`, `elementValue`는 **그대로 대입**하고 있어 null이 들어오면 기존 값이 null로 덮일 수 있음.
- 즉, `MercenaryDetailWriter` 주석의 “저항깎, 속성값은 null이면 그대로”는 현재 코드와 완전히 일치하지 않음.

#### 이슈 4.1 — exists 체크가 `element` 차원을 무시함(향후 확장 시 충돌 가능)

- `ItemStat`은 UNIQUE가 `(item_id, stat_type, element)`인데,
새 exists 쿼리는 `(item_id, stat_type)`만 보므로
추후 “속성별 능력치(element != NONE)”를 저장하려 하면 **추가 저장이 막힐 수 있음**.
- 현 단계에서 `Element.NONE`만 쓴다면 당장은 문제 없지만, 확장 계획이 있다면 “부분해결”로 보는 게 안전함.

### ✅ 문서의 “수정 보류” = 코드에서도 여전히 미해결 (확인 완료)

- **이슈 2 (장비 플래그 반영)**: `EquipmentItem` 생성 시 `ritualApplicable/hasSlotOption`이 실제로 세팅되지 않고 기본값(false)로 남는 구조가 유지됨.
- **이슈 4.3 (PriceCrawlTasklet 트랜잭션 정책)**: `PriceCrawlTasklet.execute()`에 `@Transactional`이 유지되며, 서버 루프 전체가 한 트랜잭션에 묶이는 형태가 유지됨.

