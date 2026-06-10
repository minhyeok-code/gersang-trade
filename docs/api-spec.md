# API 명세

Base URL: `/` (로컬 `http://localhost:8080`)

## 범례

| 표시 | 의미 |
|------|------|
| 🔒 | `isAuthenticated()` — 로그인 필수 |
| 🛡️ | `hasRole('ADMIN')` — 관리자 전용 |
| (없음) | 공개 |

---

## 1. 인증 (Auth)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/auth/refresh` | — | 리프레시 토큰(Cookie)으로 액세스 토큰 재발급 |
| POST | `/auth/logout` | — | 로그아웃 (리프레시 토큰 무효화) |

---

## 2. 사용자 (User)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/users/me` | 🔒 | 내 프로필 조회 |
| PATCH | `/api/users/me` | 🔒 | 내 프로필 수정 |
| PATCH | `/api/users/me/server` | 🔒 | 서버 설정 |
| DELETE | `/api/users/me` | 🔒 | 회원 탈퇴 (소프트딜리트) |
| GET | `/api/users/me/listings` | 🔒 | 내 거래 등록 목록 |
| GET | `/api/users/me/trades` | 🔒 | 내 거래 내역 |
| POST | `/api/users/me/clear-time` | 🔒 | 클리어 타임 등록 |
| GET | `/api/users/me/clear-times` | 🔒 | 내 클리어 타임 목록 |
| GET | `/api/users/me/hunt-hub-status` | 🔒 | 헌팅 허브 참여 상태 조회 |
| GET | `/api/users/{userId}` | — | 타 유저 공개 프로필 조회 |
| GET | `/api/users/{userId}/reviews` | — | 타 유저 거래 후기 목록 |
| GET | `/api/grades` | — | 등급 정책 목록 |
| GET | `/api/users/me/grade` | 🔒 | 내 등급 조회 |

---

## 3. 카탈로그 (Catalog)

### 서버

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/servers` | — | 전체 서버 목록 (13개 고정) |

### 아이템

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/items/search` | — | 아이템 검색 (`q`, `type`, `kind`, `limit`) |
| GET | `/api/items/equipment` | — | 슬롯별 장비 목록 (`slot`) |
| GET | `/api/items/{itemId}/rituals` | — | 아이템에 적용 가능한 주술 목록 |
| GET | `/api/items/{itemId}/price-history` | — | 아이템 시세 기록 (`serverId` 필수, `from`, `to`, `days`) |

### 세트

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/sets` | — | 세트 목록 (`name`, 페이징) |
| GET | `/api/sets/{id}` | — | 세트 상세 |

### 주술

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/rituals` | — | 전체 주술 목록 |

### 몬스터

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/monsters` | — | 몬스터 목록 (`element` 필터) |
| GET | `/api/monsters/search` | — | 몬스터 자동완성 (`q` 필수, `limit` 최대 20) |
| GET | `/api/monsters/{id}` | — | 몬스터 상세 |

### 용병

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/mercenaries` | — | 용병 목록 (`element`, `q`, `limit`) |
| GET | `/api/mercenaries/{mercenaryId}/characteristics` | — | 용병 특성 카탈로그 |
| GET | `/api/mercenaries/{mercenaryId}/exclusive-equipment` | — | 용병 전용 장비 목록 (`slot` 필수) |

---

## 4. 거래 등록 (Listing)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/listings` | 🔒 | 거래 등록 생성 |
| GET | `/api/listings` | — | 거래 등록 목록 (`server`, `status`, `bundleType`, `itemId`, `keyword`, 페이징) |
| GET | `/api/listings/{listingId}` | — | 거래 등록 상세 |
| PATCH | `/api/listings/{listingId}` | 🔒 | 거래 등록 수정 |
| DELETE | `/api/listings/{listingId}` | 🔒 | 거래 등록 취소 (소프트딜리트) |

---

## 5. 구매 요청 (Wanted)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/wanted` | 🔒 | 구매 요청 등록 |
| GET | `/api/wanted` | — | 구매 요청 목록 (`server`, `status`, `keyword`, 페이징) |
| GET | `/api/wanted/{wantedId}` | — | 구매 요청 상세 |
| DELETE | `/api/wanted/{wantedId}` | 🔒 | 구매 요청 취소 |

---

## 6. 채팅 · 거래 확정 (Chat)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/chat-rooms` | 🔒 | 채팅방 생성 |
| GET | `/api/chat-rooms` | 🔒 | 내 채팅방 목록 |
| GET | `/api/chat-rooms/{chatRoomId}` | 🔒 | 채팅방 상세 (메시지 포함) |
| POST | `/api/chat-rooms/{chatRoomId}/read` | 🔒 | 읽음 처리 |
| POST | `/api/chat-rooms/{chatRoomId}/messages` | 🔒 | 메시지 전송 |
| POST | `/api/chat-rooms/{chatRoomId}/trade-confirm` | 🔒 | 거래 확정 요청 |
| POST | `/api/chat-rooms/{chatRoomId}/poster-confirm` | 🔒 | 등록자 거래 확정 |
| POST | `/api/chat-rooms/{chatRoomId}/counterparty-confirm` | 🔒 | 상대방 거래 확정 |

---

## 7. 거래 후기 (Review)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/reviews/{reviewId}` | 🔒 | 거래 후기 작성 |
| GET | `/api/reviews/received` | 🔒 | 받은 후기 목록 |
| GET | `/api/reviews/pending` | 🔒 | 작성 대기 후기 목록 |

---

## 8. 알림 (Notification)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/notifications/subscribe` | 🔒 | SSE 알림 구독 |
| GET | `/api/notifications` | 🔒 | 알림 목록 조회 |
| PATCH | `/api/notifications/read-all` | 🔒 | 전체 읽음 처리 |
| PATCH | `/api/notifications/{id}/read` | 🔒 | 단건 읽음 처리 |

---

## 9. 홈 (Home)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/home/price-watch` | 🔒 | 관심 아이템 시세 조회 (`serverId` 필수) |

**응답 형태 (`PriceWatchResponse`)**

```
{
  serverId: number,
  serverName: string,
  targets: [
    {
      entryId: number,
      targetType: "ITEM" | "SET",
      watchKey: string,
      displayLabel: string,
      sell: { avgPrice, count, listings: [{ listingId, price, createdAt }] },
      buy:  { avgPrice, count, listings: [{ wantedId, offeredPrice, createdAt }] },
      completed: { count, dataQuality: "OK" | "LIMITED", trades: [{ confirmedPrice, confirmedAt }] }
    }
  ]
}
```

> `dataQuality`: ITEM에 ritualMark가 있거나 SET 타입이면 `"LIMITED"` (주술 필터 미적용 근사값)

---

## 10. 관심목록 (Watchlist)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/watchlist` | 🔒 | 관심 항목 목록 조회 (최대 5개, sort_order·created_at 정렬) |
| POST | `/api/watchlist` | 🔒 | 관심 항목 추가 |
| DELETE | `/api/watchlist/{entryId}` | 🔒 | 관심 항목 삭제 |

**POST 요청 바디 (`WatchTargetAddRequest`)**

```
{
  targetType: "ITEM" | "SET",
  itemId?: number,          // ITEM 타입 필수
  ritualMark?: string,      // ITEM: 주술 마크 (재료 아이템 불가)
  setId?: number,           // SET 타입 필수
  composition?: string,     // SET 타입 필수 — "GAMTU" | "BYEON" | "BANSSANG" | "FULL" | "FULL_BANSSANG"
  ritualCount?: number,     // SET 타입 선택 — BANSSANG·FULL_BANSSANG은 무시됨
}
```

**에러 코드**

| 코드 | 상태 | 설명 |
|------|------|------|
| `WATCH_LIMIT_EXCEEDED` | 422 | 관심목록 5개 초과 |
| `DUPLICATE_WATCH_ITEM` | 409 | 동일 watchKey 중복 |
| `INVALID_WATCH_TARGET` | 400 | 잘못된 대상 (존재하지 않는 아이템/세트, 재료에 주술 등) |
| `WATCH_TARGET_NOT_FOUND` | 404 | 항목 없음 |
| `WATCH_FORBIDDEN` | 403 | 타인 항목 삭제 시도 |

---

## 11. 계산기 (Calculator)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/calculator` | — | 능력치 계산기 |
| POST | `/api/calculator/dps` | — | DPS 계산기 |

### DPS 가성비 평가

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/calculator/dps/evaluations` | 🔒 | 가성비 평가 실행 (`persist` 플래그로 저장 여부 결정, 동일 요청 멱등 처리) |
| GET | `/api/calculator/dps/evaluations` | 🔒 | 내 평가 목록 (최신순, `page`/`size` 페이징, 최대 100) |
| GET | `/api/calculator/dps/evaluations/{id}` | 🔒 | 내 평가 상세 (DB 저장값 복원, 타인 평가 404) |

---

## 12. 덱 (Deck)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/decks` | 🔒 | 내 덱 목록 |
| POST | `/api/decks` | 🔒 | 덱 생성 |
| GET | `/api/decks/effect-options` | 🔒 | 덱 효과 카탈로그 |
| GET | `/api/decks/{deckId}` | 🔒 | 덱 상세 |
| PATCH | `/api/decks/{deckId}` | 🔒 | 덱 수정 |
| DELETE | `/api/decks/{deckId}` | 🔒 | 덱 삭제 |
| PUT | `/api/decks/{deckId}/effects` | 🔒 | 덱 효과 전체 교체 |
| POST | `/api/decks/{deckId}/members` | 🔒 | 덱 멤버 추가 |
| DELETE | `/api/decks/{deckId}/members/{memberId}` | 🔒 | 덱 멤버 제거 |
| PATCH | `/api/decks/{deckId}/members/{memberId}/build` | 🔒 | 멤버 빌드 수정 |
| PATCH | `/api/decks/{deckId}/members/{memberId}/level` | 🔒 | 멤버 레벨 수정 |
| PUT | `/api/decks/{deckId}/members/{memberId}/characteristics` | 🔒 | 멤버 특성 설정 |
| GET | `/api/decks/{deckId}/members/{memberId}/characteristics` | 🔒 | 멤버 특성 조회 |
| GET | `/api/decks/{deckId}/members/element-values` | 🔒 | 멤버별 속성값 조회 |
| GET | `/api/decks/{deckId}/members/{memberId}/stats` | 🔒 | 멤버 스탯 조회 |
| PUT | `/api/decks/{deckId}/members/{memberId}/sets/{setId}` | 🔒 | 멤버 세트 착용 |
| PUT | `/api/decks/{deckId}/members/{memberId}/slots/{slot}` | 🔒 | 슬롯 아이템 장착 |
| DELETE | `/api/decks/{deckId}/members/{memberId}/slots/{slot}` | 🔒 | 슬롯 아이템 해제 |
| PUT | `/api/decks/{deckId}/members/{memberId}/slots/{slot}/ritual` | 🔒 | 슬롯 주술 설정 |
| DELETE | `/api/decks/{deckId}/members/{memberId}/slots/{slot}/ritual` | 🔒 | 슬롯 주술 해제 |

---

## 13. 헌팅 허브 (Hunt Hub)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/hunt/monsters` | 🔒 | 헌팅 허브 몬스터 목록 |
| GET | `/api/hunt/monsters/{monsterId}/records` | 🔒 | 몬스터별 공개 클리어 기록 |
| GET | `/api/hunt/snapshots/{snapshotId}` | 🔒 | 덱 스냅샷 조회 |

---

## 14. 신고 (Report)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/reports` | 🔒 | 신고 접수 |
| GET | `/api/reports/me` | 🔒 | 내 신고 내역 (페이징) |

---

## 15. 관리자 (Admin)

### 거래 등록 관리

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| PATCH | `/admin/listings/{listingId}/hide` | 🛡️ | 거래 등록 숨김 |
| PATCH | `/admin/listings/{listingId}/unhide` | 🛡️ | 거래 등록 숨김 해제 |

### 신고 처리

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/admin/reports` | 🛡️ | 신고 목록 (`status` 필터, 페이징) |
| PATCH | `/admin/reports/{reportId}/review` | 🛡️ | 신고 검토 중 처리 |
| PATCH | `/admin/reports/{reportId}/process` | 🛡️ | 신고 처리 완료 |
| PATCH | `/admin/reports/{reportId}/dismiss` | 🛡️ | 신고 기각 |
| POST | `/admin/users/{userId}/block` | 🛡️ | 사용자 차단 |
| POST | `/admin/users/{userId}/unblock` | 🛡️ | 사용자 차단 해제 |
| PATCH | `/admin/messages/{messageId}/hide` | 🛡️ | 채팅 메시지 숨김 |
| PATCH | `/admin/messages/{messageId}/unhide` | 🛡️ | 채팅 메시지 숨김 해제 |

### 아이템 관리

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/admin/items` | 🛡️ | 아이템 목록 (`type`, `name`, 페이징) |
| GET | `/admin/items/{itemId}` | 🛡️ | 아이템 상세 |
| PUT | `/admin/items/{itemId}` | 🛡️ | 아이템 기본 정보 수정 |
| PUT | `/admin/items/{itemId}/equipment-detail` | 🛡️ | 장비 상세 수정 |
| PUT | `/admin/items/{itemId}/stats` | 🛡️ | 아이템 스탯 전체 교체 |
| PUT | `/admin/items/{itemId}/skills` | 🛡️ | 아이템 스킬 전체 교체 |
| PUT | `/admin/items/{itemId}/skills/{skillId}/effects` | 🛡️ | 스킬 효과 전체 교체 |
| DELETE | `/admin/items/{itemId}` | 🛡️ | 아이템 삭제 |
| GET | `/admin/items/cleanup-candidates` | 🛡️ | 정리 대상 아이템 목록 |
| POST | `/admin/items/bulk-delete` | 🛡️ | 아이템 일괄 삭제 |
| GET | `/admin/items/{itemId}/restrictions` | 🛡️ | 아이템 착용 제한 목록 |
| POST | `/admin/items/{itemId}/restrictions` | 🛡️ | 착용 제한 추가 |
| DELETE | `/admin/items/{itemId}/restrictions/{restrictionId}` | 🛡️ | 착용 제한 삭제 |

### 세트 관리

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/admin/sets` | 🛡️ | 세트 목록 (페이징) |
| GET | `/admin/sets/{id}` | 🛡️ | 세트 상세 |
| PATCH | `/admin/sets/{id}` | 🛡️ | 세트 수정 |
| POST | `/admin/sets/{id}/restrictions` | 🛡️ | 세트 착용 제한 추가 |

### 세트 부여 스킬 관리

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/admin/set-granted-skills` | 🛡️ | 세트 부여 스킬 목록 (페이징) |
| GET | `/admin/set-granted-skills/{id}` | 🛡️ | 세트 부여 스킬 상세 |
| POST | `/admin/set-granted-skills` | 🛡️ | 세트 부여 스킬 생성 |
| PUT | `/admin/set-granted-skills/{id}` | 🛡️ | 세트 부여 스킬 수정 |
| DELETE | `/admin/set-granted-skills/{id}` | 🛡️ | 세트 부여 스킬 삭제 |
| GET | `/admin/sets/{setId}/skill-effects` | 🛡️ | 세트 스킬 효과 목록 |
| POST | `/admin/sets/{setId}/skill-effects` | 🛡️ | 세트 스킬 효과 추가 |
| DELETE | `/admin/sets/{setId}/skill-effects/{effectId}` | 🛡️ | 세트 스킬 효과 삭제 |

### 스킬 계수 관리

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/admin/skill-coefficients` | 🛡️ | 스킬 계수 목록 (`unmeasured` 필터) |
| GET | `/admin/skill-coefficients/issues` | 🛡️ | 측정 이슈 목록 |
| POST | `/admin/skill-coefficients` | 🛡️ | 스킬 계수 생성 |
| PUT | `/admin/skill-coefficients` | 🛡️ | 스킬 계수 일괄 교체 (JSON 업로드) |
| PUT | `/admin/skill-coefficients/{id}` | 🛡️ | 스킬 계수 수정 |
| PATCH | `/admin/skill-coefficients/{id}/measurement` | 🛡️ | 측정값 업데이트 |

### 용병 관리

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/admin/mercenaries` | 🛡️ | 용병 목록 (`category`, `nature`, `nation`, `name`, 페이징) |
| GET | `/admin/mercenaries/{mercenaryId}` | 🛡️ | 용병 상세 |
| PUT | `/admin/mercenaries/{mercenaryId}` | 🛡️ | 용병 기본 정보 수정 |
| PUT | `/admin/mercenaries/{mercenaryId}/stats` | 🛡️ | 용병 스탯 전체 교체 |
| PATCH | `/admin/mercenaries/{mercenaryId}/stats/{statType}` | 🛡️ | 용병 스탯 단건 수정 |
| PUT | `/admin/mercenaries/{mercenaryId}/skills` | 🛡️ | 용병 스킬 전체 교체 |
| PUT | `/admin/mercenaries/{mercenaryId}/skills/{skillId}/effects` | 🛡️ | 용병 스킬 효과 전체 교체 |
| PATCH | `/admin/mercenaries/bulk` | 🛡️ | 용병 일괄 수정 |
| DELETE | `/admin/mercenaries/{mercenaryId}` | 🛡️ | 용병 삭제 |
| GET | `/admin/mercenaries/{mercenaryId}/characteristics` | 🛡️ | 용병 특성 목록 |
| POST | `/admin/mercenaries/{mercenaryId}/characteristics` | 🛡️ | 용병 특성 추가 |
| PUT | `/admin/mercenaries/{mercenaryId}/characteristics/{charId}` | 🛡️ | 용병 특성 수정 |
| DELETE | `/admin/mercenaries/{mercenaryId}/characteristics/{charId}` | 🛡️ | 용병 특성 삭제 |
| PUT | `/admin/mercenaries/{mercenaryId}/characteristics/{charId}/levels` | 🛡️ | 용병 특성 레벨 저장 |

### 크롤러 수동 트리거

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/admin/crawler/master` | 🛡️ | 마스터 데이터 전체 수집 Job 실행 |
| POST | `/admin/crawler/items` | 🛡️ | 아이템 수집 |
| POST | `/admin/crawler/materials` | 🛡️ | 재료 수집 |
| POST | `/admin/crawler/mercenaries` | 🛡️ | 용병 수집 |
| POST | `/admin/crawler/sets` | 🛡️ | 세트 수집 |
| POST | `/admin/crawler/rituals` | 🛡️ | 주술 수집 |
| POST | `/admin/crawler/monsters` | 🛡️ | 몬스터 수집 |
| POST | `/admin/crawler/exclusive-equipment` | 🛡️ | 전용 장비 수집 |

---

## 16. 로컬 테스트 전용

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/local/token` | — | 테스트 JWT 발급 (`email` 파라미터) — `local` 프로파일 한정 |
