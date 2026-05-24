# 거래 흐름 설계 — 채팅 기반 흥정·거래신청·거래완료 모델

> 작성일: 2026-03-24
> 대상 범위: 판매/구매 게시물 등록, 흥정하기·거래신청(채팅방), 거래완료, 시세 반영, 실시간 알림, 현금거래 자동 감지
> 기준 문서: `docs/gersang-trade-prd.ko.md`, `docs/entity-model.ko.md`
> 연관 문서: `docs/report-system.ko.md` — 신고·자동 감지 상세 설계

---

## 1. 요구사항 정리

### 1.1 핵심 규칙

| 규칙 | 설명 |
|------|------|
| 게시물 종류 | **판매 게시물** (SELL) 또는 **구매 게시물** (BUY) 중 택1 |
| 가격 필수 | 게시자는 반드시 가격을 기재해야 함. Offer(가격 미기재) 없음 |
| 채팅방 개설 방법 | **흥정하기** (가격 협상 목적) 또는 **거래신청** (게시 가격 그대로 거래 의사 표시) — 둘 다 1:1 채팅방 생성 |
| 채팅방 복수 허용 | 하나의 게시물에 여러 상대방이 각자 채팅방 개설 가능. 제한 없음 |
| 거래완료 순서 | **게시자가 먼저** 거래완료 버튼을 눌러야 상대방의 버튼이 활성화됨 |
| 가격 재입력 | 협의된 거래가가 다를 경우, **게시자**가 거래완료 버튼을 누르기 전 실제 거래가를 입력 |
| 시세 반영 | 거래확정 시 `TradeStatDaily` 자동 집계 |
| 채팅 보존 기간 | 사용자 열람 가능 기간: **6개월** / 관리자·신고 판단용 로그: **2년** (상세는 5절 참고) |
| 알림 | 흥정하기·거래신청 수신 시 실시간 알림 (SSE 기반, 사이트 접속 중이면 소리 포함) |
| 현금거래 감지 | 채팅 메시지 전송 시 금지 키워드·패턴 자동 검사. **Soft 방식** — 메시지는 전송되고 자동 신고 + 관리자 알림 생성 |

### 1.2 기존 설계와의 차이점

| 항목 | 기존 PRD 설계 | 변경 후 |
|------|-------------|--------|
| 거래 신청 방식 | `TradeApplication` (PENDING → ACCEPTED) | `ChatRoom` 개설로 대체 |
| 가격 | 판매 게시물만 필수 | 판매·구매 게시물 모두 필수 |
| 구매 게시물 가격 | WantedListing에 가격 필드 없음 | `price` 필드 추가 (필수) |
| 거래완료 트리거 | 판매자 단독 확정 | **게시자 먼저 → 상대방** 순서로 상호 확인 |
| 실시간 알림 | 미정 | SSE (Server-Sent Events) 기반 브라우저 알림 |

---

## 2. 엔티티 설계

### 2.1 기존 엔티티 변경

#### `WantedListing` — `price` 필드 추가

```
WantedListing:
  + price: Long (NOT NULL) — 구매 희망 가격 (게임 내 재화 단위)
```

> 변경 이유: 흥정·거래신청 흐름에서 상대방이 참고할 기준가가 반드시 필요.

#### `TradeApplication` — ChatRoom으로 대체, 제거

`TradeApplication` (PENDING → ACCEPTED 흐름)은 `ChatRoom`이 완전히 대체한다.
기존에 구현된 코드가 없으므로 해당 엔티티를 **삭제하고 ChatRoom으로 통합**한다.

---

### 2.2 신규 엔티티: `ChatRoom` (채팅방)

흥정하기 또는 거래신청 버튼을 누르면 생성되는 1:1 채팅방.

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `listingType` | Enum | NO | `SELL` \| `BUY` — 어느 게시물에서 시작됐는지 |
| `listingId` | Long | NO | TradeListing.id 또는 WantedListing.id |
| `initiationType` | Enum | NO | `NEGOTIATE` (흥정하기) \| `APPLY` (거래신청) |
| `posterId` | Long (FK → User) | NO | 게시물 작성자 |
| `counterpartyId` | Long (FK → User) | NO | 채팅방을 연 상대방 |
| `status` | Enum | NO | `OPEN` \| `POSTER_CONFIRMED` \| `COMPLETED` \| `CLOSED` |
| `finalPrice` | Long | YES | 실제 거래가. 게시자가 거래완료 시 입력. 미입력 시 게시물 `price` 사용 |
| `posterConfirmedAt` | LocalDateTime | YES | 게시자 거래완료 누른 시각 |
| `counterpartyConfirmedAt` | LocalDateTime | YES | 상대방 거래완료 누른 시각 |
| `completedAt` | LocalDateTime | YES | 양측 모두 확인된 시각 (= 거래 확정 시각) |
| `createdAt` | LocalDateTime | NO | |
| `updatedAt` | LocalDateTime | NO | |

**`initiationType` 차이:**

| 구분 | 흥정하기 (NEGOTIATE) | 거래신청 (APPLY) |
|------|---------------------|----------------|
| 의도 | 가격·조건 협의 | 게시 가격 그대로 거래 희망 |
| 채팅방 개설 자동 메시지 | "[시스템] {닉네임}님이 흥정을 요청했습니다." | "[시스템] {닉네임}님이 거래를 신청했습니다." |
| 알림 메시지 | "흥정 요청이 도착했습니다" | "거래 신청이 도착했습니다" |
| 이후 흐름 | 동일 (채팅 → 거래완료) | 동일 |

**상태 전이:**
```
흥정하기 또는 거래신청 버튼 누름 → OPEN
게시자 거래완료 버튼 누름         → POSTER_CONFIRMED
상대방 거래완료 버튼 누름         → COMPLETED → TradeConfirmed 생성
어느 한쪽이 채팅방 종료/취소      → CLOSED
거래 완료 후 다른 채팅방들        → CLOSED (일괄 처리)
```

**유니크 제약:**
```sql
UNIQUE(listing_type, listing_id, counterparty_id, status)
-- 같은 게시물-상대방 쌍이라도 CLOSED/COMPLETED 후 재채팅 허용
-- 단, OPEN 또는 POSTER_CONFIRMED 상태는 1개만 허용
```

> **복수 채팅방 정책**: 게시물 1개에 여러 상대방이 동시에 채팅방을 열 수 있다.
> 예) 판매자 A의 게시물 → 구매 희망자 B, C, D가 각자 채팅방 개설 가능.
> 단, 거래가 완료(COMPLETED)되면 나머지 채팅방은 모두 CLOSED 처리된다.

---

### 2.3 신규 엔티티: `ChatMessage` (채팅 메시지)

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `chatRoomId` | Long (FK → ChatRoom) | NO | |
| `senderId` | Long (FK → User) | YES | SYSTEM 메시지는 null |
| `content` | String (최대 1000자) | NO | 메시지 내용 |
| `messageType` | Enum | NO | `TEXT` \| `SYSTEM` |
| `archivedAt` | LocalDateTime | YES | 사용자 열람 만료 시각. null이면 열람 가능 |
| `sentAt` | LocalDateTime | NO | |

**`archivedAt` 동작 (5절 참고):**
- null: 사용자 열람 가능
- non-null: 6개월 경과하여 사용자 화면에서 숨김 처리. 관리자는 계속 조회 가능

**`messageType` 예시:**
- `TEXT`: 사용자 직접 입력
- `SYSTEM`: 서버 자동 생성
  - `"[시스템] 거상왕님이 거래를 신청했습니다."`
  - `"[시스템] 게시자가 거래완료를 요청했습니다. 확인 후 거래완료 버튼을 눌러주세요."`
  - `"[시스템] 거래가 900,000골드로 확정되었습니다."`
  - `"[시스템] 다른 사용자와 거래가 완료되어 채팅방이 종료되었습니다."`

---

### 2.4 신규 엔티티: `Notification` (알림)

실시간(SSE)으로 전송되며, 오프라인 사용자도 다음 접속 시 확인할 수 있도록 DB에 저장한다.

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `userId` | Long (FK → User) | NO | 알림 수신 대상 |
| `type` | Enum | NO | `CHAT_OPENED` \| `CHAT_MESSAGE` \| `POSTER_CONFIRMED` \| `TRADE_COMPLETED` |
| `chatRoomId` | Long (FK → ChatRoom) | YES | 관련 채팅방 |
| `message` | String | NO | 알림 문구 (예: "거상왕님이 거래를 신청했습니다") |
| `isRead` | boolean | NO | 기본값 false |
| `createdAt` | LocalDateTime | NO | |

**알림 발생 시점:**

| 알림 타입 | 발생 시점 | 수신자 |
|----------|----------|--------|
| `CHAT_OPENED` | 흥정하기 또는 거래신청으로 채팅방 생성 시 | 게시자 |
| `CHAT_MESSAGE` | 상대방이 메시지 전송 시 (읽지 않은 경우) | 상대방 |
| `POSTER_CONFIRMED` | 게시자가 거래완료 버튼 누름 | 상대방(counterparty) |
| `TRADE_COMPLETED` | 양측 모두 확인 완료 | 양측 모두 |

---

### 2.5 `TradeConfirmed` 수정

기존 `applicationId(FK → TradeApplication)` → `chatRoomId(FK → ChatRoom)`으로 교체.

| 필드 | 타입 | Null | 변경 내용 |
|------|------|------|----------|
| `chatRoomId` | Long (FK → ChatRoom) | YES | ← applicationId 대체. UNIQUE 제약으로 이중 확정 방지 |
| `listingType` | Enum | NO | SELL \| BUY (스냅샷) |
| `confirmedPrice` | Long | NO | finalPrice or listing.price 스냅샷 |

> 나머지 필드(`listingId`, `sellerId`, `buyerId`, `serverSnapshot`, `statKeySnapshot`, `confirmedAt`, `cancelled`)는 기존 설계 유지.
> 의미상 `sellerId` = 게시자, `buyerId` = 상대방으로 확장 적용 (컬럼명 유지).

---

## 3. API 설계

### 3.1 채팅방 생성 — 흥정하기 / 거래신청

두 버튼 모두 같은 엔드포인트를 사용하고 `initiationType` 바디로 구분한다.

```
POST /api/listings/{listingId}/chat       # 판매 게시물
POST /api/wanted/{listingId}/chat         # 구매 게시물
```

**요청:**
```json
{
  "initiationType": "APPLY"   // "NEGOTIATE" 또는 "APPLY"
}
```

**응답:**
```json
{
  "chatRoomId": 42,
  "initiationType": "APPLY",
  "status": "OPEN",
  "listing": {
    "id": 1,
    "type": "SELL",
    "price": 1000000,
    "title": "풀 XX 00세트"
  },
  "posterId": 10,
  "counterpartyId": 20,
  "createdAt": "2026-03-24T10:00:00"
}
```

**예외 처리:**
- 게시자 본인 요청 → `400` "본인 게시물에 거래 신청할 수 없습니다"
- 이미 `OPEN` 또는 `POSTER_CONFIRMED` 상태인 해당 쌍의 채팅방 존재 → 기존 채팅방 ID 반환
- 게시물 `SOLD` / `CANCELLED` 상태 → `409 Conflict`
- 차단된 사용자 → `403`

---

### 3.2 채팅방 목록 조회

```
GET /api/chats?status=OPEN&role=POSTER     # 내가 게시자인 채팅방
GET /api/chats?status=OPEN&role=COUNTERPARTY  # 내가 상대방인 채팅방
```

**응답 (페이지네이션):**
```json
{
  "content": [
    {
      "chatRoomId": 42,
      "listingType": "SELL",
      "listingId": 1,
      "listingTitle": "풀 XX 00세트",
      "listingPrice": 1000000,
      "initiationType": "APPLY",
      "otherUserId": 20,
      "otherUserNickname": "거상왕",
      "status": "OPEN",
      "lastMessage": "900,000에 해주실 수 있나요?",
      "lastMessageAt": "2026-03-24T10:05:00",
      "unreadCount": 2
    }
  ],
  "totalElements": 5
}
```

---

### 3.3 채팅방 상세 + 메시지 조회

```
GET /api/chats/{chatRoomId}                              # 채팅방 정보
GET /api/chats/{chatRoomId}/messages?after={messageId}   # 메시지 폴링 (사용자: archivedAt IS NULL 필터)
```

**메시지 응답:**
```json
{
  "messages": [
    {
      "messageId": 101,
      "senderId": 10,
      "senderNickname": "판매자닉",
      "content": "안녕하세요!",
      "messageType": "TEXT",
      "sentAt": "2026-03-24T10:01:00"
    },
    {
      "messageId": 102,
      "senderId": null,
      "content": "[시스템] 게시자가 거래완료를 요청했습니다. 확인 후 거래완료 버튼을 눌러주세요.",
      "messageType": "SYSTEM",
      "sentAt": "2026-03-24T10:10:00"
    }
  ],
  "hasMore": false
}
```

---

### 3.4 메시지 전송

```
POST /api/chats/{chatRoomId}/messages
```

**요청:**
```json
{
  "content": "900,000으로 해주실 수 있나요?"
}
```

**제약:**
- 채팅방 참여자(poster 또는 counterparty)만 전송 가능
- 채팅방 `status`가 `OPEN` 또는 `POSTER_CONFIRMED`일 때만 가능
- content 최대 1000자

**현금거래 자동 감지 (Soft 방식):**

메시지 저장 후 `KeywordDetectionService`가 내용을 검사한다.
감지 시에도 메시지는 정상 전송되며, 사용자는 감지 여부를 알 수 없다.

```
전송 요청 → 저장 → 감지 검사
                        ↓ (감지됨)
              ChatMessage.flagged = true
              ChatMessage.flagReason = "감지된 패턴 목록"
              Report 자동 생성 (reporterType = SYSTEM)
              관리자에게 SSE 알림 전송
```

> 자동 감지 상세 설계(키워드 목록, 정규식 패턴, 관리자 처리 흐름)는
> `docs/report-system.ko.md` 3절 참고.

---

### 3.5 거래완료 확인 API

```
POST /api/chats/{chatRoomId}/confirm
```

**요청 (게시자만 finalPrice 입력 가능):**
```json
{
  "finalPrice": 900000   // 선택. 게시자만 입력 가능. 미입력 시 게시물 price 적용
}
```

**처리 흐름:**

```
[게시자 confirm 호출]
  1. ChatRoom.status == OPEN 확인 (아니면 400)
  2. finalPrice 입력 시 유효성 검사 (> 0)
  3. finalPrice 설정:
     - 입력 있으면 → ChatRoom.finalPrice = finalPrice
     - 입력 없으면 → ChatRoom.finalPrice = listing.price
  4. ChatRoom.posterConfirmedAt = now()
  5. ChatRoom.status = POSTER_CONFIRMED
  6. SYSTEM 메시지 자동 생성:
     "[시스템] 게시자가 거래완료를 요청했습니다. 확인 후 거래완료 버튼을 눌러주세요."
  7. Notification 생성 → SSE로 상대방에게 POSTER_CONFIRMED 알림 전송

[상대방 confirm 호출]
  1. ChatRoom.status == POSTER_CONFIRMED 확인 (OPEN이면 400 "게시자의 거래완료 확인을 기다리는 중입니다")
  2. ChatRoom.counterpartyConfirmedAt = now()
  3. ChatRoom.status = COMPLETED
  4. ChatRoom.completedAt = now()
  5. → 거래 확정 처리 (4절) 호출
```

> **순서 정책**: 게시자가 먼저 거래완료를 눌러야 상대방 버튼이 활성화된다.
> 이유: 가격 변경 입력이 게시자 확인 단계에 있으므로 게시자 의사 표시가 선행되어야 함.

**응답:**
```json
{
  "chatRoomId": 42,
  "status": "COMPLETED",
  "confirmedPrice": 900000,
  "confirmedAt": "2026-03-24T10:30:00"
}
```

---

### 3.6 알림 API

```
GET /api/notifications/subscribe          # SSE 구독 엔드포인트 (접속 유지)
GET /api/notifications                    # 알림 목록 조회 (미읽음 우선)
PATCH /api/notifications/{id}/read       # 알림 읽음 처리
PATCH /api/notifications/read-all        # 전체 읽음 처리
```

---

## 4. 거래 확정 처리 흐름 (서비스 내부)

채팅방 양측 모두 확인 완료 시 다음 처리를 **단일 트랜잭션**으로 실행한다.

```
거래 확정 트랜잭션:
  1. TradeConfirmed INSERT
     - listingType, listingId, chatRoomId (UNIQUE — 이중 확정 방지)
     - posterId(sellerId), counterpartyId(buyerId)
     - serverSnapshot (listing.server)
     - confirmedPrice = ChatRoom.finalPrice
     - statKeySnapshot = "ITEM:{itemId}" (또는 "SET:{setId}")
     - confirmedAt = now()

  2. 게시물 상태 업데이트
     - SELL → TradeListing.status = SOLD
     - BUY  → WantedListing.status = PURCHASED

  3. 동일 게시물의 다른 OPEN / POSTER_CONFIRMED 채팅방 일괄 CLOSED
     - 각 채팅방에 SYSTEM 메시지:
       "[시스템] 다른 사용자와 거래가 완료되어 채팅방이 종료되었습니다."

  4. TradeStatDaily UPSERT
     - statDate = today
     - statKey = statKeySnapshot
     - tradeCount += 1
     - priceSum += confirmedPrice
     - priceMin = min(current, confirmedPrice)
     - priceMax = max(current, confirmedPrice)

  5. EXP 지급 (양측 모두)
     - 기본 EXP: +20
     - 거래 금액별 추가 EXP:
         confirmedPrice < 1억            → +10
         1억  ≤ confirmedPrice < 5억     → +20
         5억  ≤ confirmedPrice < 15억    → +35
         15억 ≤ confirmedPrice < 50억    → +55
         confirmedPrice ≥ 50억           → +80
     - User.totalExp += (기본 + 추가)
     - User.tradeCount += 1
     - ExpGradeCalculator로 grade·gradeStep 재계산 → User 업데이트

  6. TradeReview 2건 생성 (블라인드 평가 슬롯)
     - TradeReview(reviewer=poster,       target=counterparty, revealAt=now+3일, isPublished=false)
     - TradeReview(reviewer=counterparty,  target=poster,      revealAt=now+3일, isPublished=false)

  7. Notification 생성 + SSE 전송 (게시자·상대방 모두)
     - type = TRADE_COMPLETED
     - message = "거래가 {confirmedPrice}골드로 확정되었습니다."
     - type = REVIEW_REQUESTED (별도 알림)
     - message = "[거래명] 거래가 완료됐어요. 상대방을 3일 이내에 평가해보세요."

  8. 어뷰징 모니터링 — 동일 쌍 반복 거래 탐지
     - (poster ↔ counterparty) 간 최근 7일 이내 확정 거래 수 조회
     - 3건 이상이면 → Notification(type=ABUSE_SUSPECTED) 생성, 수신자 = ADMIN 전체
     - 자동 제재 없음 (탐지·모니터링만)
```

> **멱등성 보장**: TradeConfirmed `chatRoomId UNIQUE` 제약으로 이중 확정 방지.
> **EXP·등급 정책 상세**: `docs/gersang-grade-policy.md` 참고.

---

## 5. 채팅 메시지 보존 전략

### 5.1 요구사항 정리

| 대상 | 보존 기간 | 목적 |
|------|----------|------|
| 사용자 열람 | **6개월** | 채팅 내용 확인 |
| 관리자 조회 (신고 판단) | **2년** | 분쟁·신고 처리 |

### 5.2 설계 옵션 비교

#### Option A: 두 테이블로 분리 (사용자 제안)

```
chat_messages     — 사용자 열람용 (6개월 후 삭제)
chat_message_logs — 관리자 로그용 (2년 보관)
```

| 항목 | 평가 |
|------|------|
| 장점 | 역할 분리 명확, 각자 독립 보존 정책 적용 가능 |
| 단점 | 스토리지 2배 소비, 모든 INSERT가 두 테이블 동시 기록 필요, 정합성 유지 복잡 |

#### Option B: 단일 테이블 + `archivedAt` 필드 ← **권장**

```
chat_messages:
  archivedAt (nullable) — null: 사용자 열람 가능 / non-null: 사용자 화면 숨김
```

| 항목 | 평가 |
|------|------|
| 장점 | 중복 없음, 단일 진실의 원천, 신고 처리 시 동일 테이블 조회, 구현 단순 |
| 단점 | 테이블 행 수가 시간이 지날수록 증가 (인덱스·파티셔닝으로 관리 가능) |

### 5.3 권장 방식 — 단일 테이블 + archivedAt + 2단계 배치

```
[1단계 배치 — 매일 실행]
  sentAt < now() - 6개월 AND archivedAt IS NULL
  → archivedAt = now() 로 업데이트
  → 사용자 화면에서 숨겨짐 (관리자는 계속 조회 가능)

[2단계 배치 — 매월 실행]
  archivedAt < now() - 2년
  → 실제 DELETE (관리자 보존 기간 만료)
```

**쿼리 적용:**
```sql
-- 사용자 메시지 조회
SELECT * FROM chat_messages
WHERE chat_room_id = ? AND archived_at IS NULL
ORDER BY sent_at;

-- 관리자(신고 판단) 조회 — 필터 없음
SELECT * FROM chat_messages
WHERE chat_room_id = ?
ORDER BY sent_at;
```

**인덱스:**
```sql
INDEX idx_chat_messages_room_sent (chat_room_id, sent_at)
INDEX idx_chat_messages_archived (archived_at) WHERE archived_at IS NOT NULL
```

> **결론**: 두 테이블로 분리하면 INSERT 비용과 정합성 부담이 생긴다.
> `archivedAt` 단일 컬럼으로 사용자 노출 여부를 제어하고,
> 관리자는 동일 테이블에서 필터 없이 조회하는 방식이 더 효율적이다.

---

## 6. 실시간 알림 시스템 (SSE)

### 6.1 기술 선택: SSE vs WebSocket

| 항목 | SSE (권장) | WebSocket |
|------|-----------|-----------|
| 방향 | 서버 → 클라이언트 (단방향) | 양방향 |
| 적합 용도 | **알림 전용** | 실시간 채팅 |
| Spring 지원 | `SseEmitter` (추가 의존성 없음) | `spring-boot-starter-websocket` |
| 프론트 | `EventSource` API (표준) | 별도 라이브러리 |
| 인프라 | HTTP 그대로 사용 | 별도 커넥션 관리 |

> MVP 단계에서는 알림에 SSE, 채팅 메시지 조회는 HTTP 폴링으로 구현한다.
> 추후 채팅 실시간화가 필요할 때 WebSocket(STOMP)으로 전환하되,
> SSE 알림은 그대로 유지하거나 WebSocket 토픽으로 통합한다.

### 6.2 SSE 구조

**서버 (Spring Boot):**

```
GET /api/notifications/subscribe
Content-Type: text/event-stream
Authorization: Bearer {token}
```

- `SseEmitter`를 userId 기준으로 Map에 보관
- 이벤트 발생 시 해당 userId의 emitter에 `send()` 호출
- 연결 끊기면 Map에서 제거

**이벤트 형식:**
```
event: CHAT_OPENED
data: {"chatRoomId":42,"initiationType":"APPLY","counterpartyNickname":"거상왕","listingTitle":"풀 XX 00세트","message":"거상왕님이 거래를 신청했습니다"}

event: POSTER_CONFIRMED
data: {"chatRoomId":42,"message":"게시자가 거래완료를 요청했습니다. 확인 후 거래완료 버튼을 눌러주세요."}

event: TRADE_COMPLETED
data: {"chatRoomId":42,"confirmedPrice":900000,"message":"거래가 900,000골드로 확정되었습니다."}
```

**프론트엔드 (React):**
```javascript
const eventSource = new EventSource('/api/notifications/subscribe', {
  withCredentials: true
});

eventSource.addEventListener('CHAT_OPENED', (e) => {
  const data = JSON.parse(e.data);
  playNotificationSound();   // 띠링~ 소리 재생
  showToastNotification(data.message);
  updateNotificationBadge();
});
```

### 6.3 오프라인 사용자 처리

- SSE 전송 시 해당 userId의 emitter가 없으면(오프라인) → `Notification` DB에만 저장
- 다음 로그인 시 `/api/notifications` 조회로 미읽음 알림 확인

### 6.4 SSE 연결 관리 주의사항

- 동일 사용자가 탭을 여러 개 열면 emitter가 중복 등록될 수 있음
  → Map에 단일 emitter만 유지하거나, 목록(Set)으로 관리해 모든 탭에 전송
- 연결 타임아웃: `SseEmitter` 기본값 30초 → 무제한(`Long.MAX_VALUE`)으로 설정하고 heartbeat 주기적 전송
- 하트비트: 30초마다 빈 comment(`:`만 있는 라인) 전송하여 연결 유지

---

## 7. 채팅방 접근 제어 규칙

| 행위 | 조건 |
|------|------|
| 채팅방 생성 (흥정/신청) | 로그인 사용자 + 본인 게시물 아님 + 게시물 ACTIVE + 차단 상태 아님 |
| 메시지 전송 | 채팅방 참여자(poster 또는 counterparty) + `OPEN` \| `POSTER_CONFIRMED` 상태 |
| 메시지 조회 (사용자) | 채팅방 참여자 + `archivedAt IS NULL` 필터 적용 |
| 메시지 조회 (관리자) | ADMIN 권한 + 필터 없음 (신고 처리용) |
| 거래완료 1단계 (게시자) | poster 본인 + 채팅방 `OPEN` 상태 |
| 거래완료 2단계 (상대방) | counterparty 본인 + 채팅방 `POSTER_CONFIRMED` 상태 |
| finalPrice 입력 | poster 본인 + 거래완료 1단계 요청 시에만 |

---

## 8. 엔티티 관계 다이어그램 (간략)

```
TradeListing (SELL 게시물)        WantedListing (BUY 게시물)
      |                                    |
      |___________ ChatRoom ______________|
                       |   listingType=SELL|BUY
                       |   initiationType=NEGOTIATE|APPLY
                       |
              ChatMessage (N)  ← archivedAt으로 가시성 제어
                       |
              (양측 confirm 완료 시)
                       |
              TradeConfirmed ──── TradeStatDaily (UPSERT)

Notification (N) ← userId 기준 SSE 전송
```

---

## 9. 미결 이슈

| 번호 | 이슈 | 결정 |
|------|------|------|
| 9-1 | 거래완료 순서 | **게시자 먼저** 확정. 상대방은 POSTER_CONFIRMED 상태에서만 버튼 활성화 |
| 9-2 | 채팅방 복수 허용 | **제한 없음**. 여러 상대방과 동시 진행 가능. 거래 완료 시 나머지 CLOSED |
| 9-3 | 거래완료 후 가격 수정 | **불가**. TradeConfirmed 불변 원칙 |
| 9-4 | 채팅 보존 기간 | **사용자 6개월 / 관리자 2년**. 단일 테이블 + archivedAt 방식 |
| 9-5 | 통지 방법 | **SSE 기반 실시간 알림 + 소리**. 오프라인 시 DB 저장 후 다음 접속 시 표시 |
| 9-6 | 채팅 내 현금 유도 감지 | **Soft 방식** 자동 감지 구현. 메시지 차단 없이 자동 신고 + 관리자 알림. 상세: `docs/report-system.ko.md` 3절 |
| 9-7 | 흥정/거래신청 버튼 구분 | **두 버튼 모두 동일 API**, `initiationType` 바디로 구분 |

---

## 10. 구현 순서

```
Phase 1 — 엔티티·스키마 (데이터설계 에이전트)
  1. WantedListing에 price 컬럼 추가
  2. TradeApplication 엔티티 제거
  3. ChatRoom 엔티티 생성 (initiationType 포함)
  4. ChatMessage 엔티티 생성 (archivedAt 포함)
  5. Notification 엔티티 생성
  6. TradeConfirmed 수정 (applicationId → chatRoomId)

Phase 2 — 서비스·레포지토리 (구현 에이전트)
  1. ChatRoomRepository, ChatMessageRepository, NotificationRepository
  2. KeywordDetectionService
     - 패턴 목록 캐시 로드 (@Cacheable)
     - detect(content): 감지된 패턴 목록 반환
  3. ChatRoomService
     - createChatRoom (흥정하기 / 거래신청)
     - sendMessage → 내부에서 KeywordDetectionService 호출 (Soft 감지)
     - confirmTrade — 게시자 1단계, 상대방 2단계 분기
     - handleTradeCompletion — TradeConfirmed + 통계 + 알림
  4. ReportService
     - createAutoReport (자동 감지 시 호출)
     - createUserReport (사용자 신고)
     - processReport (관리자 액션 분기)
  5. NotificationService
     - send (SSE emitter 조회 + DB 저장)
     - markRead
  6. SseEmitterRegistry (userId ↔ SseEmitter 관리)
  7. ChatRoomController, NotificationController, ReportController, AdminReportController, AdminKeywordController

  → 신고 시스템 상세: `docs/report-system.ko.md` 참고

Phase 3 — 통합 처리 (구현 에이전트)
  1. TradeStatService (TradeStatDaily UPSERT)
  2. MessageArchiveBatchJob (매일 6개월 경과 메시지 archivedAt 설정)
  3. MessagePurgeBatchJob (매월 2년 경과 메시지 hard delete)
  4. SecurityConfig 업데이트 (/api/chats/**, /api/notifications/**)

Phase 4 — 검증·테스트 (검증·테스트 에이전트)
  1. ChatRoomServiceTest
     - 흥정하기 / 거래신청 채팅방 생성
     - 거래완료 순서 위반 (상대방 먼저 → 400)
     - 이중 확정 방지 (UNIQUE 제약)
     - 거래 완료 시 다른 채팅방 CLOSED
  2. KeywordDetectionServiceTest
     - 단순 키워드 감지 (현금, 계좌)
     - 정규식 감지 (계좌번호 형식, 전화번호 형식)
     - 오탐 방지 (일반 문장)
     - 비활성 키워드 무시
  3. ReportServiceTest — 자동/사용자 신고, 관리자 액션 처리
  4. NotificationServiceTest
     - SSE emitter 없는 오프라인 사용자 → DB 저장만
  5. TradeStatServiceTest — UPSERT 집계 로직
  6. 경계값: 본인 흥정, 차단 사용자, 완료된 게시물 신청 등
```

---

## 11. 설계 리뷰 포인트 (구현 전 확인)

1. **statKey 단위**: `TradeListing` 1개에 여러 번들(아이템)이 있을 수 있음. 거래 확정 시 statKey를 `LISTING:{id}`로 잡을지, 번들별 `ITEM:{itemId}`로 분해할지 결정 필요. 시세 정확도 vs 구현 복잡도 트레이드오프
2. **낙관적 락**: 두 사용자가 동시에 confirm 호출 시 ChatRoom 상태 중복 업데이트 방지를 위해 `@Version` 컬럼 추가 검토
3. **SSE 서버 스케일아웃**: EC2 인스턴스가 2대 이상이면 emitter가 다른 서버에 있을 수 있음. MVP는 단일 서버 가정. 멀티 서버로 확장 시 Redis Pub/Sub으로 SSE 이벤트 중계 필요
4. **WantedListing 거래완료 statKey**: 구매 게시물은 아이템 조건(WantedItem)이 여러 개일 수 있음. statKey를 어떤 기준으로 설정할지 확인 필요
