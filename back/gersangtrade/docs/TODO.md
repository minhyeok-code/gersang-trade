# GersangTrade TODO 리스트

> **완료 기준**: 코드 구현 + 테스트 작성·통과 = 100%
> **최종 업데이트**: 2026-04-27 (크롤러 거상짱 전환, 아이템·용병 관리자 수정 API 추가, 2-3·2-5 완료율 갱신)

---

## 진행 현황 요약

| 구분 | 항목 수 | 평균 완료율 |
|------|---------|------------|
| MVP 기능 | 10개 | ~97% |
| 추가 기능 (확장) | 6개 | ~53% |
| 인프라·배포 | 3개 | ~3% |

---

## 1. MVP 기능

### 1-1. 인증 (OAuth2 Google 로그인)

**완료율: 100%** ✅

| 항목 | 상태 | 비고 |
|------|------|------|
| Google OAuth2 로그인 흐름 | ✅ 구현 | `CustomOAuth2UserService`, `OAuth2LoginSuccessHandler` |
| JWT 발급 (AccessToken 15분 / RefreshToken 7일) | ✅ 구현 | `JwtTokenizer` |
| RefreshToken 재발급 엔드포인트 | ✅ 구현 | `AuthController` |
| 로그아웃 (RefreshToken 무효화) | ✅ 구현 | |
| SecurityConfig (역할별 접근 제어) | ✅ 구현 | |
| 단위 테스트 6개 (AuthService) | ✅ 통과 | refresh 정상·유효하지않음·탈취의심, issueRT, logout |
| 통합 테스트 5개 (SecurityConfig 접근 제어) | ✅ 통과 | refresh/logout 흐름, 비인증 401, USER 403, ADMIN 200 |

---

### 1-2. 아이템 카탈로그 조회

**완료율: 100%** ✅

| 항목 | 상태 | 비고 |
|------|------|------|
| 아이템 검색 API (`GET /api/items`) | ✅ 구현 | `ItemSearchController` |
| 아이템 검색 서비스 | ✅ 구현 | `ItemSearchService` |
| `ItemQueryRepository` 데드코드 제거 | ✅ 완료 | jOOQ 단일 구현으로 통일 |
| 빈 문자열 `@NotBlank` 검증 | ✅ 완료 | `@Validated` + `@NotBlank` → 빈 q 요청 시 400 반환 |
| 주술 목록 조회 (장비별 적용 가능 주술) | ✅ 구현 | `RitualApplicability` 연관 |
| 아이템 검색 단위 테스트 | ✅ 통과 | limit 경계값·타입·종류 필터·결과 없음 등 9케이스 |
| 주술 조회 단위 테스트 | ✅ 통과 | 있음·여러 개·없음 3케이스 |

---

### 1-3. 거래 등록글 (판매 등록)

**완료율: 100%** ✅

| 항목 | 상태 | 비고 |
|------|------|------|
| 판매 등록 (`POST /api/listings`) | ✅ 구현 | `ListingService` |
| 목록 조회 (`GET /api/listings`) | ✅ 구현 | QueryDSL |
| 상세 조회 (`GET /api/listings/{id}`) | ✅ 구현 | |
| 취소 (`DELETE /api/listings/{id}`) | ✅ 구현 | softDelete |
| 재료/장비/세트 번들 분기 처리 | ✅ 구현 | |
| 주술 적용 검증 | ✅ 구현 | |
| 단위 테스트 17개 | ✅ 통과 | `ListingServiceTest` |
| 세트 표기 자동 생성 (`풀 XX 00세트` 형식) | ✅ 완료 | `SetTitleGenerator` — 전체/부분/없음/혼재 4케이스 처리 |
| 세트 표기 테스트 10개 | ✅ 통과 | `SetTitleGeneratorTest` |

---

### 1-4. 구매 희망 등록글

**완료율: 100%** ✅

| 항목 | 상태 | 비고 |
|------|------|------|
| 구매 희망 등록 (`POST /api/wanted`) | ✅ 구현 | `WantedListingService` |
| 목록 조회 (`GET /api/wanted`) | ✅ 구현 | |
| 상세 조회 (`GET /api/wanted/{id}`) | ✅ 구현 | |
| 취소 (`DELETE /api/wanted/{id}`) | ✅ 구현 | |
| 장비 조건·주술 조건 분기 처리 | ✅ 구현 | |
| 단위 테스트 16개 | ✅ 통과 | `WantedListingServiceTest` (기존) |
| 강화 수치 경계값 테스트 | ✅ 통과 | 외변 null/5/3위반, 일반 0/20 경계값 5케이스 추가 |
| 복수 주술 조건 및 중복 검사 테스트 | ✅ 통과 | 2개 정상저장, 중복ritualId 예외 2케이스 추가 |
| 수량·sortOrder 경계값 테스트 | ✅ 통과 | 장비quantity>1, sortOrder중복 2케이스 추가 |

---

### 1-5. 채팅 기반 거래 신청·확정 흐름

**완료율: 100%** ✅

| 항목 | 상태 | 비고 |
|------|------|------|
| `ChatRoom` 엔티티 | ✅ 구현 | 흥정하기/거래신청 → 1:1 채팅방 |
| `ChatMessage` 엔티티 | ✅ 구현 | flagged/hidden/archivedAt 포함 |
| `TradeConfirmed` 엔티티 (chatRoom FK, listingType) | ✅ 구현 | application → chatRoom으로 전환 |
| `ChatRoomRepository` | ✅ 구현 | |
| `ChatMessageRepository` | ✅ 구현 | 사용자용/관리자용/배치용 쿼리 |
| `TradeConfirmedRepository` | ✅ 구현 | 어뷰징 탐지 쿼리 포함 |
| 채팅방 생성 (`POST /api/chat-rooms`) | ✅ 구현 | 흥정하기·거래신청 공통, 중복 방지 |
| 내 채팅방 목록 (`GET /api/chat-rooms`) | ✅ 구현 | poster + counterparty 합산 |
| 채팅방 상세 + 메시지 (`GET /api/chat-rooms/{id}`) | ✅ 구현 | 최신 50건 |
| 메시지 전송 (`POST /api/chat-rooms/{id}/messages`) | ✅ 구현 | 알림 저장 포함 |
| 게시자 거래완료 요청 (`POST /api/chat-rooms/{id}/poster-confirm`) | ✅ 구현 | OPEN → POSTER_CONFIRMED |
| 상대방 거래완료 확인 (`POST /api/chat-rooms/{id}/counterparty-confirm`) | ✅ 구현 | TradeConfirmed 생성, EXP·평가 처리 |
| 키워드 감지 연동 (`KeywordDetectionService`) | ✅ 구현 | sendMessage Soft 방식 — 메시지 허용 + 자동 신고 |
| 채팅 아카이브 배치 Job (6개월 경과) | ❌ 없음 | Phase 5 범위 |
| 채팅 영구 삭제 배치 Job (2년 경과) | ❌ 없음 | Phase 5 범위 |
| 단위 테스트 15개 (`ChatServiceTest`) | ✅ 통과 | createChatRoom·sendMessage·posterConfirm·counterpartyConfirm 4그룹. KeywordDetectionService mock 추가 수정 |

---

### 1-6. 신고 시스템 + 키워드 감지

**완료율: 90%** ✅

| 항목 | 상태 | 비고 |
|------|------|------|
| `Report` 엔티티 (reporterType, reporter nullable, chatRoomId, processedBy) | ✅ 구현 | |
| `KeywordBlacklist` 엔티티 | ✅ 구현 | isRegex 지원 |
| `KeywordBlacklistRepository` | ✅ 구현 | |
| `ReportRepository` | ✅ 구현 | 상태별 페이징 조회 |
| `KeywordDetectionService` | ✅ 구현 | Spring Cache + Soft 감지 → 자동 Report 생성 |
| `ReportService` | ✅ 구현 | 신고 접수·검토·처리·기각 + 차단/해제 + 메시지 숨김 |
| 신고 접수 API (`POST /api/reports`) | ✅ 구현 | USER 신고 (자기 자신 방지) |
| 관리자 신고 목록 조회 (`GET /admin/reports`) | ✅ 구현 | 상태 필터 + 페이징 |
| 신고 검토 시작 API (`PATCH /admin/reports/{id}/review`) | ✅ 구현 | PENDING→REVIEWING |
| 신고 처리·기각 API | ✅ 구현 | REVIEWING→PROCESSED/DISMISSED |
| 사용자 차단/해제 API | ✅ 구현 | `/admin/users/{id}/block`, `/admin/users/{id}/unblock` |
| 채팅 메시지 숨김/복원 API | ✅ 구현 | `/admin/messages/{id}/hide`, `/admin/messages/{id}/unhide` |
| `ChatService.sendMessage()` 키워드 감지 연동 | ✅ 구현 | Soft 방식, 메시지 전송 허용 + 자동 신고 |
| 어뷰징 모니터링 (7일/3거래 플래그) | ❌ 없음 | Phase 5 범위 |
| 단위 테스트 (`KeywordDetectionServiceTest`) | ✅ 통과 | 5케이스: 키워드·정규식·잘못된regex·복수패턴·빈패턴 |
| 단위 테스트 (`ReportServiceTest`) | ✅ 통과 | 12케이스: fileReport·startReview·process/dismiss·차단/해제·숨김/해제 |

**100% 조건**: 키워드 감지 자동 신고 + 신고 접수·처리·기각 + 사용자 차단/해제 서비스 테스트 통과

---

### 1-7. 시세 조회 (가격 히스토리)

**완료율: 100%** ✅

| 항목 | 상태 | 비고 |
|------|------|------|
| `TradeStatDaily` / `TradeStatMonthly` 엔티티 | ✅ 구현 | |
| `TradeStatMonthlyRepository` | ✅ 구현 | 월별 statKey + 기간 조회 |
| 거래 확정 시 통계 집계 트리거 | ✅ 구현 | `ChatService.counterpartyConfirm()` → `TradeStatService.upsertDailyStat()` |
| statKey 형식 수정 (`ITEM:{itemId}`) | ✅ 구현 | 기존 `SELL:{listingId}` → 첫 번째 아이템 ID 기반; 실패 시 fallback |
| 시세 조회 API (`GET /api/items/{id}/price-history`) | ✅ 구현 | `PriceHistoryController` · `TradeStatService` |
| 일별 집계 쿼리 (`TradeStatDailyRepository`) | ✅ 구현 | `findByStatKeyAndDateRange` |
| 단위 테스트 5개 (`TradeStatServiceTest`) | ✅ 통과 | upsert 신규/accumulate/priceMin갱신, 조회 기간지정/미지정/빈결과/avgPrice계산 |

---

### 1-8. 유저 프로필·등급·관리

**완료율: 95%** ✅

| 항목 | 상태 | 비고 |
|------|------|------|
| `User` 엔티티 (소프트딜리트, 차단) | ✅ 구현 | |
| `User` 등급 필드 (grade, gradeStep, totalExp, mannerScore, tradeCount) | ✅ 구현 | |
| `GradeLevel` enum (5등급, baseExp·expPerStep 포함) | ✅ 구현 | |
| `ExpGradeCalculator` 유틸 (totalExp → grade + step 계산) | ✅ 구현 | |
| 내 정보 조회 (`GET /api/users/me`) | ✅ 구현 | `UserController` + `UserService` |
| 내 등록글 목록 (`GET /api/users/me/listings`) | ✅ 구현 | |
| 회원 탈퇴 (`DELETE /api/users/me`) | ✅ 구현 | softDelete |
| `ExpGradeCalculator` 단위 테스트 22개 | ✅ 통과 | `ExpGradeCalculatorTest` |
| `UserService` 단위 테스트 9개 | ✅ 통과 | `UserServiceTest` — getUserProfile(활성/탈퇴/차단), getMyListings(빈/있음), withdrawal(정상/없음/이미탈퇴) |

**100% 조건**: 완료 (통합 테스트 커버리지 보강은 선택 사항)

---

### 1-9. 거래 평가 (블라인드 리뷰)

**완료율: 100%** ✅

| 항목 | 상태 | 비고 |
|------|------|------|
| `TradeReview` 엔티티 | ✅ 구현 | UNIQUE(trade_confirmed_id, reviewer_id) |
| `TradeRating` enum (GOOD/NEUTRAL/BAD, EXP·매너점수 delta 포함) | ✅ 구현 | |
| `TradeReviewRepository` | ✅ 구현 | 공개 배치용 쿼리 포함 |
| 거래 확정 시 TradeReview 2건 자동 생성 | ✅ 구현 | `ChatService.counterpartyConfirm()` 내 처리 |
| 평가 제출 API (`POST /api/reviews/{id}`) | ✅ 구현 | revealAt 이전·미제출·본인 검증 포함 |
| 평가 공개 배치 Job (revealAt 경과) | ✅ 구현 | `TradeReviewScheduler` 매일 02:00 실행 |
| 내가 받은 평가 조회 API (`GET /api/reviews/received`) | ✅ 구현 | published=true만 |
| 단위 테스트 10개 (`TradeReviewServiceTest`) | ✅ 통과 | submit 4케이스, publishPending 5케이스, getReceived 2케이스 |

---

### 1-10. 알림 시스템 (SSE)

**완료율: 100%** ✅

| 항목 | 상태 | 비고 |
|------|------|------|
| `Notification` 엔티티 | ✅ 구현 | |
| `NotificationType` enum (12종) | ✅ 구현 | |
| `NotificationRepository` | ✅ 구현 | 미읽음 조회·일괄 읽음 처리 |
| `NotificationService` (알림 저장 + SSE push) | ✅ 구현 | `ConcurrentHashMap` emitter 관리, 오프라인 fallback |
| SSE 구독 엔드포인트 (`GET /api/notifications/subscribe`) | ✅ 구현 | 연결 즉시 미읽음 전송, 30분 타임아웃 |
| 알림 목록 조회 (`GET /api/notifications`) | ✅ 구현 | 최신순 50건 |
| 일괄 읽음 처리 (`PATCH /api/notifications/read-all`) | ✅ 구현 | |
| `ChatService` SSE 연동 (`saveNotification` → `NotificationService.send`) | ✅ 완료 | |
| 단위 테스트 8개 (`NotificationServiceTest`) | ✅ 통과 | send(SSE없음/있음/IOException), getUnread, getAll, markAllRead, subscribe 2케이스 |

---

## 2. 추가 기능 (확장)

### 2-1. 능력치 가성비 비교

**완료율: 15%**

| 항목 | 상태 | 비고 |
|------|------|------|
| `ItemStat` 엔티티 | ✅ 구현 | ELEMENT_VALUE / ELEMENT_PIERCE / RESIST_PIERCE |
| `ValueMetricMonthly` 엔티티 | ✅ 구현 | avgPrice / statValue / valueForMoney |
| 가성비 집계 배치 (월간) | ❌ 없음 | `statValue ÷ avgPrice` |
| 가성비 조회 API (`GET /api/stats/value-for-money`) | ❌ 없음 | 스탯 타입·서버별 필터 + 내림차순 정렬 |
| 단위 테스트 | ❌ 없음 | |

**100% 조건**: 집계 배치 + 조회 API + 스탯 타입별·서버별 필터 테스트 통과

---

### 2-2. 데미지 가성비 계산기

**완료율: 55%**

| 항목 | 상태 | 비고 |
|------|------|------|
| 계산기 API (`POST /api/calculator`) | ✅ 구현 | `CalculatorController` |
| 저항 통과율 공식 | ✅ 구현 | 260 cap 처리 포함 |
| 속성 보정 공식 | ✅ 구현 | clamp(-50, +50) |
| 가성비 점수 공식 | ✅ 구현 | 상승률% ÷ 만골드 |
| 직전 달 기본 가격 조회 (`MaterialPriceHistory`) | ✅ 구현 | |
| recommended 배지 로직 (`> 0` 체크) | ✅ 구현 | review에서 수정됨 |
| 용병 스탯 조회 `MercenaryStatRepository` 연동 | ✅ 구현 | `findByStatKey(RESIST_PIERCE/ELEMENT_VALUE)` 기반으로 전환 |
| 용병 재료 비용 계산 `materialItemKey` 기반 | ✅ 구현 | `loadPriceMapByItemName` 추가, `findAllByResultMercenaryIdIn` 사용 |
| 단위 테스트 (공식 검증) | ❌ 없음 | |
| 경계값 테스트 (저항 260, 속성 cap) | ❌ 없음 | |

**100% 조건**: 공식 단위 테스트 (저항 365, 디버프 136 예시값 일치 검증 포함) + 경계값 케이스 통과

---

### 2-3. 크롤링 Job 1 — 마스터 데이터 수집

**완료율: 80%**

> **2026-04-27 재구성**: gerniverse + geota 다단계(List Tasklet → Detail Reader/Writer Chunk) 방식에서
> **거상짱(gersangjjang.com) 단일 Tasklet** 방식으로 전면 전환.
> 삭제: `GerniverseParser`, `ItemListTasklet`, `MercenaryListTasklet`, `ItemDetailReader/Writer`, `MercenaryDetailReader/Writer`
> 신규: `GersangjjangParser`, `GersangjjangMercenaryParser`, `GersangjjangItemTasklet`, `GersangjjangMercenaryTasklet`
> 실제 DB에 아이템·용병 데이터 적재 완료 확인.

#### 아이템 크롤링

| 항목 | 상태 | 비고 |
|------|------|------|
| `GersangjjangParser` (거상짱 아이템 HTML 파싱) | ✅ 구현 | `ItemRow` 파싱. `GerniverseParser` 대체 |
| `GersangjjangItemTasklet` (아이템 목록+스탯+스킬 일괄 수집) | ✅ 구현 | 기존 2단계(List+Detail) → 단일 Tasklet으로 단순화 |
| 실제 데이터 수집 완료 | ✅ 확인 | DB에 아이템 데이터 적재됨 |
| 장비 플래그 (`ritualApplicable`, `hasSlotOption`) | ⚠️ 미완 | 거상짱 파싱 기반으로 재검토 필요 |

#### 용병 크롤링

| 항목 | 상태 | 비고 |
|------|------|------|
| `MercenaryCategory` / `Nation` / `Nature` Enum | ✅ 구현 | |
| `StatType` 확장 (STRENGTH·VITALITY 등 12종 추가) | ✅ 구현 | ItemStat과 공유 |
| `MercenaryStat` 엔티티 + `MercenaryStatRepository` | ✅ 구현 | UNIQUE(mercenary_id, stat_key) |
| `MercenarySkill` 엔티티 + `MercenarySkillRepository` | ✅ 구현 | |
| `MercenaryCharacteristic` 엔티티 + Repository | ✅ 구현 | key UNIQUE, 레벨 수치 포함 |
| `MercenaryMaterial` 재구조화 | ✅ 구현 | `materialMercenary`(nullable) / `materialItemKey`(nullable) 분기 |
| `GersangjjangMercenaryParser` (거상짱 용병 HTML 파싱) | ✅ 구현 | `GerniverseParser` 대체 |
| `GersangjjangMercenaryTasklet` (용병 목록+스탯+스킬 일괄 수집) | ✅ 구현 | 기존 List+Detail 2단계 → 단일 Tasklet |
| 실제 데이터 수집 완료 | ✅ 확인 | DB에 용병 데이터 적재됨 |

#### 공통

| 항목 | 상태 | 비고 |
|------|------|------|
| S3 이미지 업로드 | ✅ 구현 | `S3ImageService` |
| 관리자 수동 트리거 (`POST /admin/crawler/master·items·mercenaries`) | ✅ 구현 | `CrawlerAdminController` |
| `GersangjjangParser` 파싱 단위 테스트 (HTML fixture) | ❌ 없음 | 아이템·용병 파싱 검증 |
| 통합 테스트 (Mock HTML 파싱 검증) | ❌ 없음 | |

**100% 조건**: `GersangjjangParser`·`GersangjjangMercenaryParser` 파싱 단위 테스트 (고정 HTML fixture) + 장비 플래그 반영 + 테스트 통과

---

### 2-4. 크롤링 Job 2 — 가격 수집

**완료율: 50%**

| 항목 | 상태 | 비고 |
|------|------|------|
| `PriceCrawlTasklet` (geota 육의전 가격) | ✅ 구현 | |
| IQR 이상치 제거 (`IqrCalculator`) | ✅ 구현 | |
| `MaterialPriceHistory` UPSERT | ✅ 구현 | |
| 월 스케줄러 (매월 1일 03:00) | ✅ 구현 | |
| 관리자 수동 트리거 (`POST /admin/crawler/price`) | ✅ 구현 | |
| `PriceCrawlTasklet` 트랜잭션 범위 검토 | ⚠️ 보류 | review 지적 |
| 단위 테스트 (`IqrCalculator`) | ❌ 없음 | |
| 단위 테스트 (`PriceCrawlTasklet` 집계 로직) | ❌ 없음 | |

**100% 조건**: IQR 계산 단위 테스트 (샘플 데이터 5개 미만 스킵, 이상치 제거 검증) + 트랜잭션 범위 수정 후 통과

---

### 2-5. 관리자 기능

**완료율: 95%** ✅

| 항목 | 상태 | 비고 |
|------|------|------|
| 크롤러 수동 트리거 API | ✅ 구현 | `CrawlerAdminController` |
| 신고 처리 API (검토·완료·기각) | ✅ 구현 | `ReportAdminController` |
| 사용자 차단/해제 API | ✅ 구현 | `/admin/users/{id}/block`, `/admin/users/{id}/unblock` |
| 채팅 메시지 숨김/복원 API | ✅ 구현 | `/admin/messages/{id}/hide`, `/admin/messages/{id}/unhide` |
| 등록글 숨김 처리 (`hidden=true`) | ✅ 구현 | `ListingAdminController` — PATCH `/admin/listings/{id}/hide·unhide` |
| 용병 단건 조회 API | ✅ 구현 | `GET /admin/mercenaries/{id}` — 기본정보+스탯+스킬 |
| 용병 기본정보 수정 API | ✅ 구현 | `PUT /admin/mercenaries/{id}` — 이름·카테고리·국가·속성·속성값·출시예정 |
| 용병 스탯 전체 교체 API | ✅ 구현 | `PUT /admin/mercenaries/{id}/stats` — PUT 의미론 |
| 용병 스킬 전체 교체 API | ✅ 구현 | `PUT /admin/mercenaries/{id}/skills` — PUT 의미론 |
| 아이템 목록 조회 API | ✅ 구현 | `GET /admin/items` — statCount 포함 |
| 아이템 단건 조회 API | ✅ 구현 | `GET /admin/items/{id}` — 기본정보+장비정보+스탯+스킬 |
| 아이템 기본정보 수정 API | ✅ 구현 | `PUT /admin/items/{id}` — 이름·타입·거래카테고리 |
| 아이템 스탯 전체 교체 API | ✅ 구현 | `PUT /admin/items/{id}/stats` — element null → NONE 처리 |
| 아이템 스킬 전체 교체 API | ✅ 구현 | `PUT /admin/items/{id}/skills` |
| 관리자 전용 접근 제어 검증 테스트 | ✅ 통과 | `AuthSecurityIntegrationTest` — 비인증 401, USER 403, ADMIN 200 |
| 등록글 숨김 서비스 단위 테스트 | ❌ 없음 | `ListingService.hideListing/unhideListing` 케이스 |

**100% 조건**: 등록글 숨김 서비스 단위 테스트 보강

---

### 2-6. 개인화 서비스 & 캐싱

**완료율: 45%**

> 기준 문서: `docs/personalization_cache_design.md`
> 선행 조건: 2-2 데미지 계산기, 2-3 크롤링 Job 1 (Mercenary 적재 완료) 필요

#### 엔티티

| 항목 | 상태 | 비고 |
|------|------|------|
| `User.mainAttribute` 필드 추가 | ❌ 없음 | `Nature` enum 재활용. 덱 변경 시 재계산 |
| `UserDeck` 엔티티 | ✅ 구현 | userId + isActive + attrXValue·totalResDown 캐싱 |
| `UserDeckMember` 엔티티 | ✅ 구현 | deckId + mercenaryId + slotIndex(0~11). 주인공도 Mercenary로 통일 |
| `UserDeckMemberEquip` 엔티티 | ✅ 구현 | LEGENDARY_GENERAL 세트·강화·인연, MYEONG_KING 개별 장비 |
| `UserDeckMemberCharacteristic` 엔티티 | ✅ 구현 | characteristicId + selectedLevel. 전설장수 패시브 포함 통합 |
| `UserRecommendation` 엔티티 | ❌ 없음 | userId + itemId + score + type + updatedAt |

#### 인프라·설정

| 항목 | 상태 | 비고 |
|------|------|------|
| `spring-boot-starter-cache` 의존성 추가 | ❌ 없음 | `build.gradle` 수정 |
| `@EnableCaching` 설정 클래스 | ❌ 없음 | MVP는 인메모리(Caffeine). 멀티 서버 전환 시 Redis로 교체 |

#### Repository

| 항목 | 상태 | 비고 |
|------|------|------|
| `UserDeckRepository` | ✅ 구현 | 활성 덱 조회 포함 |
| `UserDeckMemberRepository` | ✅ 구현 | N+1 방지 fetch join 쿼리 포함 |
| `UserRecommendationRepository` | ❌ 없음 | |

#### 서비스

| 항목 | 상태 | 비고 |
|------|------|------|
| `UserDeckService` | ✅ 구현 | `calculateTotalStats()` — 기본 스탯 + 특성 레벨 합산 |
| `RecommendationService` | ❌ 없음 | 추천 점수 계산 (`@Cacheable`) + `UserRecommendation` 저장 |
| 덱 CRUD API | ❌ 없음 | `POST /api/decks`, `GET /api/decks`, `DELETE /api/decks/{id}` |
| 가격 변동 시 추천 재계산 배치 연동 | ❌ 없음 | 크롤링 Job 2 완료 이벤트 후 실행 |

#### 테스트

| 항목 | 상태 | 비고 |
|------|------|------|
| `UserDeckServiceTest` | ❌ 없음 | calculateTotalStats — 기본 스탯 합산, 특성 레벨 합산, statType null skip |
| `RecommendationServiceTest` | ❌ 없음 | Fallback (덱 미설정), 속성 일치 추천, 가격 없는 아이템 처리 |

**100% 조건**: 덱 CRUD API + mainAttribute 자동 계산 + 추천 결과 조회 API + Fallback 케이스 테스트 통과

---

## 3. 인프라·배포

### 3-1. Flyway 마이그레이션

**완료율: 0%**

| 항목 | 상태 | 비고 |
|------|------|------|
| `V1__init.sql` (전체 스키마) | ❌ 없음 | 현재 `ddl-auto=create` |
| `V2__seed_servers.sql` (서버 13개) | ❌ 없음 | |
| `V3__seed_items.sql` (아이템 기초 데이터) | ❌ 없음 | |
| `V4__seed_rituals.sql` (주술 정의) | ❌ 없음 | |
| Production `ddl-auto=validate` 전환 | ❌ 없음 | |
| Flyway 활성화 (`spring.flyway.enabled=true`) | ❌ 없음 | |

**100% 조건**: 마이그레이션 스크립트 실행 후 `validate` 모드에서 스키마 오류 없이 기동 성공

---

### 3-2. 프론트엔드

**완료율: 0%**

| 항목 | 상태 | 비고 |
|------|------|------|
| 기술 스택 결정 (React/Next.js 등) | ❌ 없음 | `/front/` 디렉토리 비어 있음 |
| 아이템 검색·목록 UI | ❌ 없음 | |
| 판매·구매 희망 등록 폼 | ❌ 없음 | |
| 시세 차트 | ❌ 없음 | |
| 데미지 계산기 UI | ❌ 없음 | |
| OAuth2 Google 로그인 버튼 | ❌ 없음 | |

**100% 조건**: 핵심 UI 구현 + E2E 시나리오 (로그인→등록→조회) 동작 확인

---

### 3-3. 운영 환경 구성

**완료율: 10%**

| 항목 | 상태 | 비고 |
|------|------|------|
| 환경변수 템플릿 (`.env.example`) | ❌ 없음 | DB/OAuth/JWT/S3 키 목록화 |
| AWS EC2 배포 스크립트 | ❌ 없음 | |
| RDS 연결 설정 (`application-prod.yml`) | ❌ 없음 | |
| GitHub Actions CI/CD | ❌ 없음 | |
| Spring Batch 스케줄러 모니터링 | ❌ 없음 | |
| `application.yml` 환경변수 분리 | ⚠️ 부분 | 일부 env var 사용 중 |

**100% 조건**: prod 프로파일로 EC2 배포 성공 + 크롤링 스케줄러 정상 기동 확인

---

## 전체 진행률 한눈에 보기

```
[MVP 기능]
1-1.  인증 (OAuth2)                    ██████████████████████ 100%  ← 완료
1-2.  아이템 카탈로그 조회             ██████████████████████ 100%  ← 완료
1-3.  거래 등록글 (판매)               ██████████████████████ 100%  ← 완료
1-4.  구매 희망 등록글                 ██████████████████████ 100%  ← 완료
1-5.  채팅 기반 거래 신청·확정 흐름    ██████████████████████ 100%  ← 완료
1-6.  신고 시스템 + 키워드 감지        ████████████████████░░  90%  ← 어뷰징 모니터링만 남음
1-7.  시세 조회                        ██████████████████████ 100%  ← 완료
1-8.  유저 프로필·등급                 █████████████████████░  95%  ← 서비스+테스트 완료
1-9.  거래 평가 (블라인드 리뷰)        ██████████████████████ 100%  ← 완료
1-10. 알림 시스템 (SSE)               ██████████████████████ 100%  ← 완료

[추가 기능]
2-1. 능력치 가성비 비교                ███░░░░░░░░░░░░░░░░░░░  15%
2-2. 데미지 계산기                     ████████████░░░░░░░░░░  55%
2-3. 크롤링 Job 1 (마스터)             █████████████████░░░░░  80%  ← 거상짱 전환+DB 적재 완료
2-4. 크롤링 Job 2 (가격)               ██████████░░░░░░░░░░░░  50%
2-5. 관리자 기능                       █████████████████████░  95%  ← 아이템·용병 수정 API 추가
2-6. 개인화 서비스 & 캐싱             ██████████░░░░░░░░░░░░  45%

[인프라·배포]
3-1. Flyway 마이그레이션               ░░░░░░░░░░░░░░░░░░░░░░   0%
3-2. 프론트엔드                        ░░░░░░░░░░░░░░░░░░░░░░   0%
3-3. 운영 환경 구성                    ██░░░░░░░░░░░░░░░░░░░░  10%
```

---

## 다음 우선순위 추천 (MVP 완성 순서)

### Phase 1 — 빠른 완성 (Quick Win)
거의 완성된 항목. 작은 작업 하나씩으로 100% 도달 가능.

1. **1-2 아이템 카탈로그 조회** — `ItemQueryRepository` 데드코드 제거 + `@NotBlank` 검증 추가 → 완성
2. **1-3 거래 등록글 (판매)** — 세트 표기 문자열 생성 로직(`풀 XX 00세트` 형식) 구현 → 100%
3. **1-4 구매 희망 등록글** — 강화 범위 경계값 테스트 보완 → 100%
4. **1-1 인증 (OAuth2)** — 기존 구현된 테스트 실행·검증 (구현 완료 상태) → 완성

### Phase 2 — 핵심 거래 흐름 완성
1-5 채팅이 중심. 이후 항목(알림·평가·시세)은 1-5 완료 후에야 의미 있음.

5. **1-5 채팅 기반 거래 신청·확정** — `ChatServiceTest` 작성 (채팅방 생성→메시지 전송→거래완료 흐름 + 이중 방 방지·상태 전이 예외 케이스)
6. **1-10 알림 시스템 (SSE)** — `NotificationService` + SSE 엔드포인트 구현 (1-5 메시지 전송과 연계, sendMessage TODO 해소)
7. **1-9 거래 평가 (블라인드 리뷰)** — 거래 확정 시 `TradeReview` 2건 자동 생성 + 공개 배치 Job + EXP·매너점수 반영 (1-5 완료 후)
8. **1-7 시세 조회** — 거래 확정 집계 트리거 + 조회 API `GET /api/items/{id}/price-history` (1-5 완료 후 데이터 확보)

### Phase 3 — 보조 기능
1-5와 독립적으로 구현 가능. Phase 2와 병행 가능.

9. **1-8 유저 프로필·등급** — `GET /api/users/me` 등 조회 API + `ExpGradeCalculator` 단위 테스트 (경계값)
10. **1-6 신고 시스템** — `KeywordDetectionService` (캐시 + 자동 신고) + 신고 접수·처리·기각 API + 사용자 차단/해제

### Phase 4 — 배포 준비
11. **3-1 Flyway** — 전체 스키마 마이그레이션 스크립트 작성 (`V1__init.sql` ~ `V4__seed_rituals.sql`)

### Phase 5 — 추가 기능 (MVP 완성 후)
12. **2-2 데미지 가성비 계산기** — 공식 단위 테스트 + 경계값 테스트 (저항 260, 속성 cap ±50)
13. **2-3 크롤링 Job 1 (마스터)** — 장비 플래그(`ritualApplicable`, `hasSlotOption`) 수정 + 파싱 단위 테스트
14. **2-4 크롤링 Job 2 (가격)** — IQR 단위 테스트 + 트랜잭션 범위 수정
15. **2-1 능력치 가성비 비교** — 월간 집계 배치 + 조회 API
16. **2-5 관리자 기능** — 등록글 숨김 서비스 단위 테스트만 남음 (아이템·용병 수정 API는 완료)
17. **2-6 개인화 서비스 & 캐싱** — 덱 CRUD API + `UserRecommendation` 엔티티 + `RecommendationService` + Spring Cache 설정 (엔티티·레포·calculateTotalStats 구현 완료. 이후 작업: 덱 API, 추천 서비스, 캐시 설정)