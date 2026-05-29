# 거래완료(confirm) 500 에러 — LazyInitializationException 정리

> **작성 배경:** 채팅방 #6 (SELL listing #7)에서 양쪽 거래완료 확인 중,  
> 게시자(멋쟁i)가 두 번째로 confirm 할 때 `500 Internal Server Error` 발생.  
> **코드 수정:** 본 문서 작성 시점에는 미적용. 다음 작업에서 반영 예정.

---

## 1. 한 줄 요약

**통계 저장 쿼리가 Hibernate “작업 메모”를 통째로 지워 버린 뒤,  
아직 로드되지 않은 User(멋쟁i) 정보에 EXP를 주려다 실패한 버그**입니다.

채팅방 #4의 `409 Conflict`(stale 채팅방)와는 **완전히 다른 문제**입니다.

---

## 2. 사용자 입장에서 본 흐름 (채팅방 #6)

| 순서 | 누가 | 화면/결과 | 정상? |
|------|------|-----------|-------|
| 1 | 양쪽 새로고침 | `status: OPEN`, confirm 둘 다 false | ✅ |
| 2 | 2번(최민혁) 거래완료 클릭 | `AWAITING_PARTNER`, 본인 confirm true | ✅ |
| 3 | 1번(멋쟁i) 화면 | “상대가 거래완료 눌렀음” 표시 | ✅ |
| 4 | 1번(멋쟁i) 거래완료 클릭 | **500 에러**, 거래 미완료 | ❌ |

2번까지는 **의도대로 동작**했습니다.  
4번에서 **거래 최종 확정(`finalizeTrade`)** 단계가 터졌습니다.

---

## 3. 에러 메시지 쉬운 설명

```
Could not initialize proxy [User#1] - no session
```

### 비유

- **User#1** = 멋쟁i 계정 (DB에 있는 회원 정보)
- Hibernate **proxy** = “멋쟁i 정보 있어요”라고 **꼬리표만 달아 둔 상태** (아직 상세 내용은 안 꺼냄)
- **session** = DB와 대화하는 **작업 메모장** (영속성 컨텍스트)

EXP를 줄 때 `totalExp`(현재 경험치)가 필요한데,  
꼬리표만 있고 **메모장은 이미 비워진 뒤**라서 “멋쟁i 경험치 못 읽겠어요” → 500.

### 기술 용어 (참고)

| 용어 | 의미 |
|------|------|
| Lazy proxy | `@ManyToOne(fetch = LAZY)` 등으로 **나중에** DB에서 불러오는 연관 객체 |
| Session / Persistence Context | 한 트랜잭션 안에서 엔티티를 추적하는 Hibernate 1차 캐시 |
| LazyInitializationException | proxy에 **세션이 없을 때** 필드 접근하면 나는 예외 |

---

## 4. 코드에서 무슨 일이 일어나는가

### 거래완료 2단계 (복습)

1. **한쪽만** confirm → `AWAITING_PARTNER` (상대 확인 대기)
2. **양쪽 모두** confirm → `COMPLETED` + `finalizeTrade()` 실행  
   → `TradeConfirmed` 생성, 통계 반영, EXP 지급, 평가 생성 등

1번(멋쟁i)이 두 번째로 누른 순간 **2번 단계**에 진입합니다.

### `finalizeTradeInternal` 실행 순서 (문제 구간)

```
ChatService.confirmTrade()
  └─ finalizeTrade()
       └─ finalizeTradeInternal()
            1. TradeConfirmed 저장          ✅
            2. tradeStatService.upsertDailyStat()   ⚠️ 여기서 메모장 비움
            3. applyExpAndGrade(poster)     💥 User#1.getTotalExp() 실패
            4. applyExpAndGrade(counterparty)
            ...
```

### 직접 원인

`TradeStatDailyRepository.upsertAccumulate`에 아래 설정이 있습니다.

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
```

- **`clearAutomatically = true`**  
  → native upsert 실행 후 **영속성 컨텍스트 전체 clear**
- 그 직전에 `room.getPoster()` / `room.getCounterparty()`로 잡아 둔 **User는 lazy proxy**
- clear 이후 proxy는 **더 이상 관리되지 않음** → `getTotalExp()` 호출 시 **no session**

### 관련 파일

| 파일 | 역할 |
|------|------|
| `chat/service/ChatService.java` | `finalizeTradeInternal`, `applyExpAndGrade` |
| `trade/repository/TradeStatDailyRepository.java` | `upsertAccumulate` + `clearAutomatically = true` |
| `chat/repository/ChatRoomRepository.java` | `findWithLockById` (poster/counterparty lazy 로드) |

---

## 5. 500이 나면 DB는 어떻게 되나

`confirmTrade`는 `@Transactional`입니다.

**500 발생 = 트랜잭션 전체 롤백** → 1번의 confirm도 **저장되지 않을 수 있음**.

확인용 SQL:

```sql
SELECT id, status, poster_confirmed_at, counterparty_confirmed_at
FROM chat_rooms
WHERE id = 6;

SELECT id, chat_room_id FROM trade_confirmed WHERE chat_room_id = 6;
```

- `poster_confirmed_at`이 null이고 `trade_confirmed` 행이 없으면 → **아직 거래 미완료**
- 수정 후 **1번이 다시 거래완료**를 눌러야 함

---

## 6. #4 409 문제와 구분

| | 채팅방 #4 (BUY #5) | 채팅방 #6 (SELL #7) |
|--|-------------------|---------------------|
| 증상 | 409 Conflict, duplicate `COMPLETED` | **500**, LazyInitializationException |
| 원인 | 이미 끝난 게시물 + stale 채팅방 | stat upsert 후 User lazy 로드 실패 |
| 거래 자체 | 원래 완료 불가능한 방 | **정상 거래**, 마지막 단계만 버그 |

---

## 7. 수정 방안 (다음 코드 작업용)

### 방안 A — `clearAutomatically = false` (권장, 변경 최소)

**파일:** `TradeStatDailyRepository.java`

```java
@Modifying(clearAutomatically = false, flushAutomatically = true)
```

- upsert 후에도 ChatRoom·User 등이 컨텍스트에 남음
- native upsert만 쓰고 JPA dirty checking에 의존하지 않으면 **부작용 적음**

### 방안 B — EXP 지급을 stat upsert **앞**으로 이동

**파일:** `ChatService.finalizeTradeInternal`

순서를 `TradeConfirmed 저장 → EXP 지급 → stat upsert → …` 로 변경.

- clear가 나중에 일어나므로 EXP 단계는 안전
- 다만 clear **이후** `poster.getNickname()` 등 User 접근 코드가 있으면 **같은 문제 재발** 가능 → 함께 점검 필요

### 방안 C — upsert 후 User 재조회 (방어적)

**파일:** `ChatService.finalizeTradeInternal`

```java
User poster = userRepository.findById(room.getPoster().getId()).orElseThrow(...);
User counterparty = userRepository.findById(room.getCounterparty().getId()).orElseThrow(...);
// applyExpAndGrade, incrementTradeCount, 알림 등
```

- clear 여부와 무관하게 **항상 managed User** 사용
- `userRepository` 주입 필요 (이미 있음)

### 방안 D — 채팅방 조회 시 User JOIN FETCH

**파일:** `ChatRoomRepository.findWithLockById`

```java
@Query("SELECT r FROM ChatRoom r JOIN FETCH r.poster JOIN FETCH r.counterparty WHERE r.id = :id")
```

- confirm 시점부터 User가 초기화됨
- **clear 후에는 여전히 detached** → A 또는 C와 **함께** 쓰는 것이 안전

### 권장 조합

1. **A** (`clearAutomatically = false`) — 근본 원인 제거  
2. **C** (upsert 후 User 재조회) — clear 쓰는 다른 `@Modifying`과의 방어  
3. (선택) **D** — confirm 경로 전반 안정화  

### 테스트 체크리스트 (수정 후)

- [ ] SELL 채팅방: 2번 먼저 confirm → 1번 confirm → **200**, `COMPLETED`
- [ ] `trade_confirmed` 1건 생성
- [ ] `trade_stat_daily` upsert 정상
- [ ] 양쪽 User `totalExp` / `tradeCount` 증가
- [ ] 500 / LazyInitializationException 없음

---

## 8. 참고 — `AWAITING_PARTNER` (구 `POSTER_CONFIRMED`)

status `AWAITING_PARTNER`는 **“한쪽만 거래완료 확인, 상대 확인 대기”** 를 뜻합니다.  
게시자·상대방 **누구든** 먼저 눌러도 이 status가 됩니다.

> **2026-05-25 변경:** 혼동 방지를 위해 `POSTER_CONFIRMED` → `AWAITING_PARTNER` 로 enum 이름 변경.

---

## 9. 적용 완료 (2026-05-25)

- [x] **방안 A:** `TradeStatDailyRepository.upsertAccumulate` — `clearAutomatically = false`
- [x] **방안 C:** `finalizeTradeInternal` — stat upsert 후 `userRepository.findById` 재조회
- [x] **enum 개명:** `ChatRoomStatus.POSTER_CONFIRMED` → `AWAITING_PARTNER`

### DB legacy 값 (POSTER_CONFIRMED)

enum 개명 후 DB에 `POSTER_CONFIRMED`가 남아 있으면 API 400 발생.

**앱 코드:** `ChatRoomStatusConverter`가 읽을 때 `AWAITING_PARTNER`로 매핑.  
**권장:** DB도 한 번 갱신 (JPQL `IN` 조회·unique 제약 일관성):

```sql
UPDATE chat_rooms SET status = 'AWAITING_PARTNER' WHERE status = 'POSTER_CONFIRMED';
```

Flyway 사용 시: `db/migration/V3__rename_chat_room_poster_confirmed.sql`
