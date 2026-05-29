# 채팅 거래확정 플로우 버그 수정 기록

## 1. confirmTrade API 500 에러 (ClassCastException)

**문제 현상**
`POST /api/chat-rooms/{id}/trade-confirm` 호출 시 500 에러 반환.

**원인**
`ChatRoomRepository.existsOtherCompletedRoom` 쿼리가 MySQL `SELECT EXISTS(...)` 반환값을 `boolean`으로 선언했으나, MySQL JDBC는 이를 `Long(0 or 1)`으로 반환 → `ClassCastException`.

**해결 방법**
메서드를 `countOtherCompletedRoom`으로 교체, `long` 타입 반환 + `> 0` 비교로 변경.

---

## 2. confirmTrade 200 응답이지만 DB 미반영 (listing status 미변경)

**문제 현상**
거래 확정 후 `trade_listings.status`가 `SOLD`로 변경되지 않음.

**원인**
`completeListingStatus`에서 dirty checking에만 의존, 명시적 `save` 호출 누락.

**해결 방법**
`listing.completeTrade()` 이후 `tradeListingRepository.save(listing)` 명시적 호출 추가.

---

## 3. GET /api/users/me → 500 에러

**문제 현상**
`GET /api/users/me` 호출 시 500 Internal Server Error 반환, 로그에 다수의 `NoSuchElementException: 사용자를 찾을 수 없습니다.` 출력.

**원인**
`UserService.loadActiveUser`에서 사용자 미존재 시 `NoSuchElementException`을 던지는데, `GlobalExceptionHandler`에 해당 예외 핸들러가 없어 500으로 처리됨.

**해결 방법**
`GlobalExceptionHandler`에 `@ExceptionHandler(NoSuchElementException.class)` 추가 → HTTP 404 반환.

---

## 4. 거래 목록 페이지(trade/page.tsx)에서 거래 확정 불가

**문제 현상**
거래 목록 페이지에서 "채팅하기" 버튼으로 열리는 `ChatModal`에 거래 완료 버튼 자체가 없음. 상대방의 거래 확정이 해당 화면에서 전혀 반영되지 않음.

**원인**
`ChatModal` 컴포넌트에 거래 확정 UI(버튼, 상태 표시)가 구현되어 있지 않았음. `room_status` WebSocket 이벤트도 단순 메시지 리로드만 처리했고, `detail` 상태를 갖지 않아 `myTradeConfirmed`/`partnerTradeConfirmed` 추적이 불가능.

**해결 방법**
`ChatModal`에 `detail`, `finalPrice`, `confirming`, `error` 상태 추가. `getChatRoom` 응답으로 `detail` 세팅. 거래 완료 버튼, 최종가 입력, 상태 메시지("상대방 확인 대기 중…", "상대방이 먼저 확인했습니다") UI 추가. `room_status` WebSocket 핸들러에서 `detail` 즉시 업데이트.

---

## 5. 상대방 confirmTrade 후 게시자 confirmTrade가 DB에 반영되지 않음 (Lost Update)

**문제 현상**
상대방이 먼저 거래완료를 누른 뒤 게시자가 확인하면, confirmTrade API가 200을 반환하고 in-memory 상태는 COMPLETED이나 페이지 새로고침 시 DB에서 `status=AWAITING_PARTNER`로 조회됨.

**원인**
`confirmTrade` 트랜잭션이 `saveAndFlush`로 `COMPLETED` 상태를 DB에 flush(잠금, 미커밋)하는 사이, 동시에 실행된 `markChatRoomRead` 트랜잭션이 READ COMMITTED으로 커밋 전 상태(`AWAITING_PARTNER`)를 읽은 뒤 Hibernate의 **전체 필드 UPDATE**로 `status`, `poster_confirmed_at`을 덮어씀.

```
exec-10 (confirmTrade):  saveAndFlush → status=COMPLETED (잠금, 미커밋)
exec-5  (markChatRoomRead): SELECT → status=AWAITING_PARTNER 읽음
exec-10: 트랜잭션 커밋 → DB: COMPLETED
exec-5:  UPDATE 전체 필드 → poster_confirmed_at=null, status=AWAITING_PARTNER 덮어씀
```

**해결 방법**
`ChatRoom` 엔티티에 `@DynamicUpdate` 추가. Hibernate가 변경된 필드(`posterLastReadAt` 또는 `counterpartyLastReadAt`)만 UPDATE에 포함시켜, `status`와 `confirmedAt` 컬럼을 건드리지 않게 됨.

```java
@DynamicUpdate
@Entity
public class ChatRoom extends BaseEntity { ... }
```

---

## 6. 거래완료 클릭 후 상대방이 확인해도 COMPLETED로 자동 갱신 안 됨

**문제 현상**
사용자가 거래완료를 누른 뒤 상대방이 거래완료를 눌러도 UI가 COMPLETED로 자동 갱신되지 않고 새로고침이 필요함.

**원인**
`pushRoomStatus`가 `confirmTrade` 트랜잭션 **커밋 전**에 호출됨. 프론트엔드 `room_status` WebSocket 핸들러가 이벤트 수신 후 `load(true)` (또는 `loadMessages()`)를 호출하는데, 이 시점에 트랜잭션이 아직 커밋 전이어서 GET /chat/{id}가 구버전 상태(`AWAITING_PARTNER`)를 반환 → WebSocket 이벤트가 전달한 `COMPLETED` 상태를 덮어씀. `ChatModal`에는 폴링 자체가 없어 복구 불가.

**해결 방법**
1. `ChatPanel`의 `room_status` WebSocket 핸들러에서 `load(true)` 제거. 상태 갱신은 이벤트 데이터(`setDetail`)로만 처리. 시스템 메시지는 `chat_message` 이벤트로 별도 수신되므로 재로드 불필요.
2. `ChatModal`의 `room_status` 핸들러에서 `loadMessages()` 제거.
3. `ChatModal`에 폴링(`setInterval 3000ms`) 추가 — `waitingPartner` 또는 `needsMyConfirm` 상태일 때 활성화.
