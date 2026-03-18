# 테스트 실행 보고서

**실행일시**: 2026-03-17
**대상 클래스**: `ListingServiceTest`, `WantedListingServiceTest`
**테스트 프레임워크**: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)

---

## 전체 결과 요약

| 클래스 | 전체 | 통과 | 실패 | 스킵 |
|--------|------|------|------|------|
| `ListingServiceTest` | 17 | 17 | 0 | 0 |
| `WantedListingServiceTest` | 16 | 16 | 0 | 0 |
| **합계** | **33** | **33** | **0** | **0** |

모든 테스트 통과 ✅

---

## ListingServiceTest 테스트 케이스별 검증 내용

### createListing (등록)

| 테스트명 | 검증 내용 |
|---------|---------|
| `createListing_정상_재료아이템_등록글ID반환` | 재료 아이템(MATERIAL) 번들 등록 시 TradeListing, ListingBundle, BundleLine이 저장되고 ID가 반환되며 장비 상세 저장은 호출되지 않음을 검증 |
| `createListing_정상_장비아이템_주술없음_등록글ID반환` | 일반 장비(hasRitual=false)로 등록 시 BundleEquipmentDetail 저장까지 완료되고 주술 저장(saveAll)은 호출되지 않음을 검증 |
| `createListing_정상_장비아이템_주술있음_등록글ID반환` | 주술 포함 장비 등록 시 BundleEquipmentDetail과 BundleEquipmentRitual.saveAll()까지 모두 호출됨을 검증 |
| `createListing_외변장비_강화수치5아닌경우_예외발생` | EquipmentKind.APPEARANCE 장비에 enhanceLevel=3 입력 시 `IllegalArgumentException("외변 장비의 강화 수치는 5이어야 합니다")` 발생 검증 |
| `createListing_차단된사용자_예외발생` | UserStatus.BLOCKED 사용자 요청 시 `IllegalStateException("차단된 계정은 거래 등록글을 등록할 수 없습니다")` 발생 검증 |
| `createListing_존재하지않는사용자_예외발생` | DB에 없는 sellerId 입력 시 `IllegalArgumentException("존재하지 않는 사용자입니다")` 발생 검증 |
| `createListing_존재하지않는아이템_예외발생` | DB에 없는 itemId 입력 시 `IllegalArgumentException("존재하지 않는 아이템입니다")` 발생 검증 |
| `createListing_주술일관성위반_예외발생` | hasRitual=true인데 rituals 목록이 비어 있을 때 `IllegalArgumentException("주술 적용(hasRitual=true) 시 주술 목록은 1개 이상이어야 합니다")` 발생 검증 |
| `createListing_적용불가능한주술_예외발생` | 해당 장비에 RitualApplicability가 없는 ritualId 사용 시 `IllegalArgumentException("해당 장비에 적용 불가능한 주술입니다")` 발생 검증 |

### getListings (목록 조회)

| 테스트명 | 검증 내용 |
|---------|---------|
| `getListings_정상조회_요약목록반환` | 검색 조건으로 등록글이 조회되면 번들 IN 쿼리로 일괄 로드하고 ListingSummaryResponse 목록을 반환함을 검증 |
| `getListings_결과없음_빈목록반환` | 조건에 맞는 등록글이 없을 때 빈 목록을 반환하고 bundleRepository.findByListingIdIn()이 호출되지 않음을 검증 |

### getDetail (상세 조회)

| 테스트명 | 검증 내용 |
|---------|---------|
| `getDetail_정상조회_상세응답반환` | 존재하는 등록글 ID로 조회 시 번들→라인→장비→주술 계층이 조립된 ListingDetailResponse가 반환됨을 검증 |
| `getDetail_존재하지않는등록글_예외발생` | `findActiveById` 결과가 빈 Optional일 때 `IllegalArgumentException("존재하지 않는 등록글입니다")` 발생 검증 |

### cancelListing (취소)

| 테스트명 | 검증 내용 |
|---------|---------|
| `cancelListing_정상취소_상태변경` | 본인 소유의 ACTIVE 등록글 취소 시 `cancel()`과 `softDelete()` 메서드가 모두 호출됨을 검증 |
| `cancelListing_본인아닌경우_예외발생` | 요청자 ID와 등록글 seller.id가 다를 때 `IllegalArgumentException("본인의 등록글만 취소할 수 있습니다")` 발생 검증 |
| `cancelListing_이미취소된경우_예외발생` | CANCELLED 상태 등록글 재취소 시 `IllegalStateException("이미 완료되거나 취소된 거래 등록글입니다")` 발생 검증 |
| `cancelListing_SOLD상태_예외발생` | SOLD 상태 등록글 취소 시 `IllegalStateException("이미 완료되거나 취소된 거래 등록글입니다")` 발생 검증 |

---

## WantedListingServiceTest 테스트 케이스별 검증 내용

### createWantedListing (등록)

| 테스트명 | 검증 내용 |
|---------|---------|
| `createWantedListing_정상_재료아이템_등록글ID반환` | 재료 아이템 구매 희망 등록 시 WantedListing, WantedItem이 저장되고 ID가 반환되며 장비 조건 저장은 호출되지 않음을 검증 |
| `createWantedListing_정상_장비아이템_주술없음_등록글ID반환` | hasRitual=false인 장비 조건 등록 시 WantedEquipmentCondition 저장까지 완료되고 주술 조건 저장(saveAll)은 호출되지 않음을 검증 |
| `createWantedListing_정상_장비아이템_주술있음_등록글ID반환` | 주술 조건 포함 장비 희망 등록 시 WantedEquipmentCondition과 WantedRitualCondition.saveAll()까지 모두 호출됨을 검증 |
| `createWantedListing_차단된사용자_예외발생` | UserStatus.BLOCKED 구매자 요청 시 `IllegalStateException("차단된 계정은 구매 희망 등록글을 등록할 수 없습니다")` 발생 검증 |
| `createWantedListing_존재하지않는사용자_예외발생` | DB에 없는 buyerId 입력 시 `IllegalArgumentException("존재하지 않는 사용자입니다")` 발생 검증 |
| `createWantedListing_존재하지않는아이템_예외발생` | DB에 없는 itemId 입력 시 `IllegalArgumentException("존재하지 않는 아이템입니다")` 발생 검증 |
| `createWantedListing_주술일관성위반_예외발생` | hasRitual=true인데 ritualConditions 목록이 비어 있을 때 `IllegalArgumentException("주술 조건(hasRitual=true) 시 주술 조건 목록은 1개 이상이어야 합니다")` 발생 검증 |
| `createWantedListing_적용불가능한주술_예외발생` | 해당 장비에 적용 불가능한 ritualId 사용 시 `IllegalArgumentException("해당 장비에 적용 불가능한 주술입니다")` 발생 검증 |

### getWantedListings (목록 조회)

| 테스트명 | 검증 내용 |
|---------|---------|
| `getWantedListings_정상조회_요약목록반환` | 조건에 맞는 구매 희망 등록글이 있을 때 아이템 IN 쿼리로 일괄 로드하고 WantedListingSummaryResponse 목록을 반환함을 검증 |
| `getWantedListings_결과없음_빈목록반환` | 조건에 맞는 등록글이 없을 때 빈 목록을 반환하고 wantedItemRepository.findByWantedListingIdIn()이 호출되지 않음을 검증 |

### getDetail (상세 조회)

| 테스트명 | 검증 내용 |
|---------|---------|
| `getDetail_정상조회_상세응답반환` | 존재하는 등록글 ID로 조회 시 아이템→장비조건→주술조건 계층이 조립된 WantedListingDetailResponse가 반환됨을 검증 |
| `getDetail_존재하지않는등록글_예외발생` | `findActiveById` 결과가 빈 Optional일 때 `IllegalArgumentException("존재하지 않는 구매 희망 등록글입니다")` 발생 검증 |

### cancelWantedListing (취소)

| 테스트명 | 검증 내용 |
|---------|---------|
| `cancelWantedListing_정상취소_상태변경` | 본인 소유의 OPEN 등록글 취소 시 `cancel()`과 `softDelete()` 메서드가 모두 호출됨을 검증 |
| `cancelWantedListing_본인아닌경우_예외발생` | 요청자 ID와 등록글 buyer.id가 다를 때 `IllegalArgumentException("본인의 등록글만 취소할 수 있습니다")` 발생 검증 |
| `cancelWantedListing_이미취소된경우_예외발생` | CANCELLED 상태 등록글 재취소 시 `IllegalStateException("이미 완료되거나 취소된 구매 희망 등록글입니다")` 발생 검증 |
| `cancelWantedListing_PURCHASED상태_예외발생` | PURCHASED 상태 등록글 취소 시 `IllegalStateException("이미 완료되거나 취소된 구매 희망 등록글입니다")` 발생 검증 |

---

## 발견된 버그 및 이슈

버그 없음. 서비스 로직이 명세대로 구현되어 있음을 확인.

---

## 기술 메모 (테스트 작성 중 발견한 설계 특이사항)

### EquipmentItem @MapsId itemId 주의사항

`EquipmentItem`은 `@MapsId` 패턴으로 `itemId`를 `Item.id`에서 주입받는다.
빌더로 `EquipmentItem`을 생성하면 JPA 컨텍스트 없이는 `itemId` 필드가 `null`이 된다.
따라서 테스트에서 `EquipmentItem` 객체는 `mock(EquipmentItem.class)`로 생성하고
`when(equipmentItem.getItemId()).thenReturn(2L)` 방식으로 ID를 명시적으로 설정해야 한다.
동일한 이유로 `Item` 역시 `mock(Item.class)`를 사용하고 `getId()`, `getType()`, `getName()`을 stub 처리했다.

### Mockito strict stubbing 설정

`@ExtendWith(MockitoExtension.class)`는 기본적으로 strict stubbing(`STRICT_STUBS`)을 사용한다.
`@BeforeEach`에서 공통 Mock stub을 설정하면 일부 테스트에서 해당 stub이 사용되지 않아
`UnnecessaryStubbingException`이 발생할 수 있다.
이를 방지하기 위해 `@MockitoSettings(strictness = Strictness.LENIENT)`를 클래스 레벨에 적용했다.
