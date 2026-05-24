# 신고 시스템 설계

> 작성일: 2026-03-24
> 대상 범위: 사용자 신고, 자동 감지(현금거래), 관리자 처리 흐름, 제재 정책
> 연관 문서: `docs/trade-flow-design.ko.md` — 채팅 내 자동 감지 흐름 참고

---

## 1. 신고의 두 가지 유형

```
신고
├── 사용자 신고    — 다른 사용자가 직접 신고 버튼을 누른 경우
└── 자동 감지     — 서버가 채팅 메시지에서 금지 키워드/패턴을 감지한 경우 (Soft 방식)
```

두 유형 모두 동일한 `Report` 테이블에 저장되며, `reporterType`으로 구분한다.
관리자 처리 흐름도 동일하다. 단, 자동 감지는 **별도 배지(🤖)**로 시각적으로 구분한다.

---

## 2. 엔티티 설계

### 2.1 `Report` (신고)

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `reporterType` | Enum | NO | `USER` \| `SYSTEM` |
| `reporterId` | Long (FK → User) | YES | 사용자 신고 시 신고자 ID. SYSTEM이면 null |
| `targetType` | Enum | NO | `USER` \| `TRADE_LISTING` \| `WANTED_LISTING` \| `CHAT_MESSAGE` |
| `targetId` | Long | NO | 신고 대상 ID |
| `chatRoomId` | Long (FK → ChatRoom) | YES | 채팅 관련 신고 시 채팅방 참조 |
| `reasonCategory` | Enum | NO | 아래 참고 |
| `description` | String (최대 1000자) | YES | 상세 내용. SYSTEM 감지는 자동 생성 |
| `evidenceUrl` | String | YES | 증빙 스크린샷 URL (S3) |
| `status` | Enum | NO | `PENDING` \| `REVIEWING` \| `PROCESSED` \| `DISMISSED` |
| `adminNote` | String | YES | 관리자 처리 메모 |
| `processedBy` | Long (FK → User) | YES | 처리한 관리자 ID |
| `processedAt` | LocalDateTime | YES | |
| `createdAt` | LocalDateTime | NO | |
| `updatedAt` | LocalDateTime | NO | |

**`reasonCategory` Enum:**

| 값 | 설명 | 주요 발생 경로 |
|----|------|-------------|
| `CASH_TRADE` | 현금 거래 유도 | 자동 감지 + 사용자 신고 |
| `FRAUD` | 사기 의심 | 사용자 신고 |
| `ABUSE` | 욕설·비방 | 사용자 신고 |
| `FAKE_LISTING` | 허위 매물 | 사용자 신고 |
| `OTHER` | 기타 | 사용자 신고 |

---

### 2.2 `KeywordBlacklist` (금지 키워드)

관리자가 코드 배포 없이 키워드를 추가·삭제할 수 있도록 DB 테이블로 관리한다.

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `pattern` | String | NO | 키워드 또는 정규식 문자열 |
| `isRegex` | boolean | NO | false: 단순 포함 검사 / true: 정규식 매칭 |
| `description` | String | YES | 이 패턴을 추가한 이유 |
| `isActive` | boolean | NO | 기본 true. false면 검사에서 제외 |
| `createdBy` | Long (FK → User) | NO | 등록한 관리자 |
| `createdAt` | LocalDateTime | NO | |

**초기 등록 패턴 (예시):**

| pattern | isRegex | 설명 |
|---------|---------|------|
| `계좌` | false | 계좌 언급 |
| `입금` | false | 입금 언급 |
| `송금` | false | 송금 언급 |
| `현금` | false | 현금 언급 |
| `현질` | false | 게임 현금 구매 속어 |
| `카카오페이` | false | 간편결제 |
| `토스` | false | 간편결제 |
| `페이코` | false | 간편결제 |
| `\d{3,4}-\d{3,4}-\d{4,6}` | true | 계좌번호 형식 |
| `01[0-9]-\d{3,4}-\d{4}` | true | 휴대폰 번호 형식 |
| `[0-9,]+(만원\|천원\|원)` | true | 원화 금액 표기 |

> **오탐 주의**: `"원"` 단독 검사는 금지. 반드시 숫자 패턴과 결합한 정규식으로만 사용.

---

### 2.3 `ChatMessage` 수정 — `flagged` 필드 추가

자동 감지된 메시지에 표시하여 관리자가 해당 메시지를 식별할 수 있게 한다.

```
ChatMessage:
  + flagged: boolean (기본 false)  — 자동 감지 시 true
  + flagReason: String (nullable)  — 감지된 패턴/키워드 (예: "pattern=현금, 계좌번호형식")
```

---

## 3. 자동 감지 흐름 (Soft 방식)

> **Soft 방식**: 메시지를 차단하지 않고 전송은 허용하되, 시스템이 자동으로 신고를 생성한다.
> 실제 제재는 관리자가 맥락을 확인한 후 결정한다.

```
[사용자 메시지 전송 요청]
        ↓
[ChatRoomService.sendMessage()]
        ↓
  ① 채팅방·권한 유효성 검사
        ↓
  ② KeywordDetectionService.detect(content)
     - DB에서 isActive=true 패턴 목록 로드 (캐싱 권장)
     - 단순 포함 검사 + 정규식 매칭 순서로 검사
     - 감지된 패턴 목록 반환 (없으면 빈 리스트)
        ↓
  ③ ChatMessage 저장
     - flagged = (감지된 패턴이 있으면 true)
     - flagReason = "pattern=현금, 01[0-9]-..." (감지된 패턴 목록)
        ↓
  ④ [감지된 경우만] 자동 신고 생성
     - reporterType = SYSTEM
     - reporterId = null
     - targetType = CHAT_MESSAGE
     - targetId = chatMessage.id
     - chatRoomId = chatRoom.id
     - reasonCategory = CASH_TRADE
     - description = "[자동감지] 금지 패턴 포함: {감지된 패턴 목록}"
     - status = PENDING
        ↓
  ⑤ [감지된 경우만] 관리자 알림 전송 (SSE)
     - type = CASH_TRADE_DETECTED
     - message = "현금거래 의심 메시지가 감지되었습니다. (채팅방 #{chatRoomId})"
     - 온라인 관리자 → SSE 즉시 전송
     - 오프라인 관리자 → Notification DB 저장 후 다음 접속 시 표시
        ↓
  ⑥ 정상 응답 반환 (메시지 전송 완료)
     — 사용자는 감지 여부를 알 수 없음
```

**키워드 목록 캐싱 전략:**
- 애플리케이션 시작 시 `KeywordBlacklist`(isActive=true)를 메모리에 로드
- 관리자가 키워드를 추가/수정하면 캐시 무효화 및 재로드
- 구현: `@Cacheable("keywordBlacklist")` + 관리자 키워드 변경 API에서 `@CacheEvict`

---

## 4. 사용자 신고 흐름

```
[사용자가 신고 버튼 클릭]
        ↓
POST /api/reports
{
  "targetType": "CHAT_MESSAGE",
  "targetId": 101,
  "chatRoomId": 42,       // 선택: 채팅 관련 신고일 때
  "reasonCategory": "CASH_TRADE",
  "description": "상대방이 계좌번호를 알려달라고 합니다."
}
        ↓
  ① 신고자 본인 대상 신고 불가 검증
  ② 동일 대상에 대한 중복 신고 검증 (같은 신고자 + targetType + targetId → 409)
  ③ Report 저장 (reporterType = USER)
  ④ 관리자에게 Notification 생성 (SSE 전송)
        ↓
응답: { "reportId": 55, "status": "PENDING" }
```

**신고 가능 대상:**

| 화면 | 신고 대상 | targetType |
|------|----------|------------|
| 게시물 상세 | 판매 게시물 | `TRADE_LISTING` |
| 게시물 상세 | 구매 게시물 | `WANTED_LISTING` |
| 채팅방 내 | 메시지 또는 상대방 | `CHAT_MESSAGE` \| `USER` |
| 마이페이지 | 사용자 프로필 | `USER` |

---

## 5. 관리자 처리 흐름

### 5.1 신고 목록 화면

```
GET /admin/reports?status=PENDING&type=SYSTEM   # 자동감지 미처리 목록
GET /admin/reports?status=PENDING&type=USER     # 사용자 신고 미처리 목록
GET /admin/reports?status=PENDING               # 전체 미처리 목록
```

**목록 항목:**
```
[🤖 자동감지] CASH_TRADE | 채팅방 #42 | 2026-03-24 10:05 | PENDING
[👤 사용자신고] FRAUD | 게시물 #7 | 2026-03-24 09:30 | PENDING
[👤 사용자신고] ABUSE | 사용자 홍길동 | 2026-03-24 09:10 | REVIEWING
```

### 5.2 신고 상세 + 맥락 조회

관리자는 신고 상세 화면에서 아래 정보를 함께 확인한다.

```
GET /admin/reports/{reportId}
```

**응답에 포함:**
- 신고 정보 (유형, 사유, 설명)
- **채팅방 전체 메시지** (archivedAt 필터 없이 — 2년 이내 전체 조회)
  - 감지된 메시지는 강조 표시 (flagged=true)
- 신고 대상 사용자의 과거 신고 이력 (최근 5건)
- 신고 대상 사용자의 거래 이력 요약

### 5.3 관리자 처리 액션

```
PATCH /admin/reports/{reportId}/process
{
  "action": "BLOCK_USER",          // 아래 액션 중 택1
  "adminNote": "계좌번호 노출 확인",
  "blockDuration": null            // null=영구, 숫자(일)=기간 차단
}
```

**처리 액션 종류:**

| action | 설명 | 부수 처리 |
|--------|------|----------|
| `WARN_USER` | 사용자에게 경고 알림 전송 | Notification 생성 (수신자: 대상 사용자) |
| `HIDE_MESSAGE` | 해당 채팅 메시지 숨김 | `ChatMessage.hidden = true` |
| `HIDE_LISTING` | 게시물 숨김 | `TradeListing.hidden = true` |
| `BLOCK_USER` | 사용자 차단 | `User.status = BLOCKED`, `blockedUntil` 설정 |
| `DISMISS` | 오탐·무혐의 처리 | 신고 종결 |

**처리 후 Report 상태:**
- `WARN_USER` / `HIDE_MESSAGE` / `HIDE_LISTING` / `BLOCK_USER` → `PROCESSED`
- `DISMISS` → `DISMISSED`

---

## 6. 추가 엔티티 수정

### 6.1 `ChatMessage` — hidden 필드 추가

관리자가 메시지를 숨길 수 있어야 한다.

```
ChatMessage:
  + hidden: boolean (기본 false) — true이면 채팅방에서 "[삭제된 메시지입니다]"로 표시
```

사용자 조회 쿼리: `WHERE archivedAt IS NULL AND hidden = false`
관리자 조회 쿼리: 필터 없음 (hidden=true 메시지도 원문 표시)

---

## 7. API 전체 목록

### 사용자 API

```
POST   /api/reports                      # 신고 접수
GET    /api/reports/my                   # 내 신고 내역 조회
```

### 관리자 API

```
GET    /admin/reports                    # 신고 목록 (필터: status, type, reasonCategory)
GET    /admin/reports/{id}               # 신고 상세 + 맥락
PATCH  /admin/reports/{id}/process       # 신고 처리 (액션 선택)
PATCH  /admin/reports/{id}/reviewing     # 검토 중으로 상태 변경

GET    /admin/keywords                   # 금지 키워드 목록
POST   /admin/keywords                   # 키워드 추가
PATCH  /admin/keywords/{id}              # 키워드 수정/비활성화
DELETE /admin/keywords/{id}              # 키워드 삭제
```

---

## 8. 어뷰징 모니터링 — 동일 상대 반복 거래 탐지

> `gersang-grade-policy.md` 4-5절 반영. 거래 확정 트랜잭션 내 8번 단계에서 실행.

### 8.1 탐지 조건

| 항목 | 내용 |
|------|------|
| 탐지 대상 | 동일 두 사용자 간 거래 |
| 탐지 조건 | **7일 이내 3건 이상** 거래 확정 발생 시 |
| 자동 제재 | **없음** — 탐지 및 관리자 알림만 |

### 8.2 탐지 후 처리

```
TradeConfirmed 생성 시 (거래 확정 트랜잭션 내):
  SELECT COUNT(*) FROM trade_confirmed
  WHERE ((seller_id = A AND buyer_id = B) OR (seller_id = B AND buyer_id = A))
    AND confirmed_at >= now() - 7일
    AND cancelled = false

  결과 >= 3이면:
    Notification 생성
    - type = ABUSE_SUSPECTED
    - userId = 각 ADMIN 사용자 (role=ADMIN인 User 전체)
    - message = "반복 거래 의심: {A닉네임} ↔ {B닉네임} 7일 내 {count}건"
    - chatRoomId = 현재 chatRoomId
```

### 8.3 관리자 대시보드 표시

- 신고 목록과 **별도 탭**으로 제공: `[⚠️ 반복 거래 의심]`
- 해당 두 사용자 간 전체 거래 이력 열람 가능
- 관리자가 직접 판단 후 신고로 전환하거나 무시 처리

---

## 9. 알림 타입 추가

기존 `Notification.type`에 아래 추가:

| 타입 | 수신자 | 발생 시점 |
|------|--------|----------|
| `CASH_TRADE_DETECTED` | 관리자(ADMIN 역할 전체) | 자동 감지 시 |
| `REPORT_RECEIVED` | 관리자 | 사용자 신고 접수 시 |
| `REPORT_PROCESSED` | 신고자 | 관리자가 신고 처리 완료 시 |
| `USER_WARNED` | 신고 대상 사용자 | 경고 처리 시 |
| `USER_BLOCKED` | 차단된 사용자 | 차단 처리 시 |
| `REVIEW_REQUESTED` | 거래 양측 | 거래 확정 직후 (평가 요청) |
| `REVIEW_PUBLISHED` | 거래 양측 | 3일 만료 후 평가 공개 시 |
| `ABUSE_SUSPECTED` | 관리자(ADMIN 역할 전체) | 동일 쌍 7일 내 3건 이상 거래 탐지 시 |

---

## 10. 구현 순서

```
Phase 1 — 엔티티·스키마
  1. Report 엔티티 생성 (reporterType 포함)
  2. KeywordBlacklist 엔티티 생성
  3. ChatMessage.flagged, flagReason, hidden 필드 추가
  4. Notification.type 에 신고·어뷰징·평가 관련 타입 추가

Phase 2 — 서비스
  1. KeywordDetectionService
     - 패턴 목록 캐시 로드 (@Cacheable)
     - detect(content): 감지된 패턴 목록 반환
  2. ReportService
     - createUserReport (사용자 신고)
     - createAutoReport (자동 감지)
     - processReport (관리자 액션 분기)
  3. AbuseMonitoringService
     - checkRepeatTrade(userId1, userId2): 7일 내 3건 이상 시 관리자 알림
  4. ChatRoomService.sendMessage()에 감지 로직 연결
  5. TradeService.handleTradeCompletion()에 어뷰징 탐지 연결

Phase 3 — 컨트롤러
  1. ReportController (/api/reports)
  2. AdminReportController (/admin/reports)
  3. AdminKeywordController (/admin/keywords)
  4. AdminAbuseController (/admin/abuse-alerts)

Phase 4 — 테스트
  1. KeywordDetectionServiceTest
     - 단순 키워드 감지 (현금, 계좌)
     - 정규식 감지 (계좌번호 형식, 전화번호 형식)
     - 오탐 방지 (일반 문장)
     - 비활성 키워드 무시
  2. ReportServiceTest
     - 자동 신고 생성 흐름
     - 사용자 중복 신고 방지
     - 관리자 액션별 부수 처리 (차단, 숨김, 경고)
  3. AbuseMonitoringServiceTest
     - 7일 내 2건: 미탐지
     - 7일 내 3건: 탐지 → 관리자 알림 생성
     - 8일 전 거래 포함 3건: 미탐지 (기간 외)
```

---

## 11. 미결 이슈

| 번호 | 이슈 | 권장 방향 |
|------|------|----------|
| 11-1 | 자동 감지 오탐 시 사용자 불이익 방지 | DISMISS 처리 시 flagged 메시지 복원 (hidden=false) |
| 11-2 | 관리자 다중 처리 시 충돌 | Report.status = REVIEWING 선점 후 처리 |
| 11-3 | 키워드 캐시 갱신 타이밍 | 관리자 CRUD 후 즉시 `@CacheEvict` + 재로드 |
| 11-4 | 채팅 외 현금거래 감지 | 게시물 `note` 필드도 동일 감지 로직 적용 검토 |
| 11-5 | 차단 해제 후 알림 | `User.status = ACTIVE` 복원 + 차단 해제 알림 필요 |
| 11-6 | 어뷰징 탐지 임계값 조정 | 7일/3건은 초기값. 실제 운영 데이터 기반으로 추후 조정 |
| 11-7 | 어뷰징 알림 중복 | 동일 쌍에 대해 이미 알림 발송한 경우 재발송 방지 로직 필요 |
