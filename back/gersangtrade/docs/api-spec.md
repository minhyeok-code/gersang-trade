# 거상 트레이드 API 명세

> **Base URL**: `http://localhost:8080` (개발)
> **인증**: JWT — `Authorization: Bearer <accessToken>` 헤더 또는 쿠키 방식
> **최종 업데이트**: 2026-05-07

---

## 인증 레벨

| 레벨 | 설명 |
|------|------|
| 없음 | 비로그인 접근 가능 |
| USER | 로그인 필요 (`ROLE_USER`) |
| ADMIN | 관리자 전용 (`ROLE_ADMIN`) |

---

## 목차

1. [인증](#1-인증)
2. [아이템 카탈로그](#2-아이템-카탈로그)
3. [서버](#3-서버)
4. [판매 등록글](#4-판매-등록글)
5. [구매 희망 등록글](#5-구매-희망-등록글)
6. [채팅 · 거래 확정](#6-채팅--거래-확정)
7. [신고](#7-신고)
8. [시세 조회](#8-시세-조회)
9. [거래 평가](#9-거래-평가)
10. [유저 프로필](#10-유저-프로필)
11. [알림 (SSE)](#11-알림-sse)
12. [데미지 계산기](#12-데미지-계산기)
13. [관리자 — 크롤러](#13-관리자--크롤러)
14. [관리자 — 아이템](#14-관리자--아이템)
15. [관리자 — 용병](#15-관리자--용병)
16. [관리자 — 장비 세트](#16-관리자--장비-세트)
17. [관리자 — 스킬 계수](#17-관리자--스킬-계수)
18. [관리자 — 등록글 숨김](#18-관리자--등록글-숨김)
19. [관리자 — 신고 처리](#19-관리자--신고-처리)

---

## 1. 인증

### POST /auth/refresh
액세스 토큰 재발급.

- **인증**: 없음 (RefreshToken 쿠키 필요)
- **요청**: 없음 (RT는 HttpOnly 쿠키로 전달)
- **응답 200**
```json
{ "accessToken": "eyJ..." }
```

---

### POST /auth/logout
로그아웃 — RefreshToken 무효화.

- **인증**: 없음
- **요청**: 없음
- **응답 200**: `"로그아웃 완료"`

---

## 2. 아이템 카탈로그

### GET /api/items/search
아이템 자동완성 검색.

- **인증**: 없음
- **Query Params**
  - `q` (필수) — 검색어 (공백 불가)
  - `type` (선택) — `MATERIAL` | `EQUIPMENT`
  - `limit` (선택, 기본 10) — 최대 결과 수
- **응답 200**
```json
[
  { "id": 1, "name": "사인검", "type": "EQUIPMENT" }
]
```

---

### GET /api/items/{itemId}/rituals
장비에 적용 가능한 주술 목록.

- **인증**: 없음
- **응답 200**
```json
[
  { "id": 1, "name": "00주술", "successMark": "00", "greatSuccessMark": "**" }
]
```

---

### GET /api/items/{itemId}/price-history
아이템 일별 시세 이력.

- **인증**: 없음
- **Query Params**
  - `from` (선택) — 시작일 `yyyy-MM-dd`
  - `to` (선택) — 종료일 `yyyy-MM-dd`
- **응답 200**
```json
[
  { "statDate": "2026-04-01", "avgPrice": 5000000, "minPrice": 4500000, "tradeCount": 3 }
]
```

---

## 3. 서버

### GET /api/servers
활성 거상 서버 목록 (13개 고정).

- **인증**: 없음
- **응답 200**
```json
[
  { "id": 1, "name": "1서버" }
]
```

---

## 4. 판매 등록글

### POST /api/listings
판매 등록글 생성.

- **인증**: USER
- **요청 Body** (JSON)
```json
{
  "server": "1서버",
  "title": "풀 00세트",
  "price": 50000000,
  "bundles": [
    {
      "bundleType": "EQUIPMENT_SET",
      "items": [
        {
          "itemId": 1,
          "quantity": 1,
          "enhanceLevel": 20,
          "hasRitual": true,
          "rituals": [{ "ritualId": 1, "result": "GREAT_SUCCESS" }]
        }
      ]
    }
  ]
}
```
- **응답 201** — 생성된 등록글 상세

---

### GET /api/listings
등록글 목록 조회.

- **인증**: 없음
- **Query Params**
  - `itemId` (선택) — 아이템 ID 필터 (카탈로그 자동완성에서 선택한 값) — ⚠️ 미구현
  - `server` — 서버명 필터
  - `status` — 상태 필터 (기본값 `ACTIVE`)
  - `bundleType` — `MATERIAL_BUNDLE` | `EQUIPMENT_SINGLE` | `EQUIPMENT_SET`
  - `keyword` — 아이템명 키워드
  - `page`, `size` — 페이징
- **응답 200** — 페이징된 등록글 목록

---

### GET /api/listings/{listingId}
등록글 상세 조회.

- **인증**: 없음
- **응답 200** — 등록글 상세 (번들·아이템·주술 포함)

---

### DELETE /api/listings/{listingId}
등록글 취소 (소프트딜리트).

- **인증**: USER (본인 또는 ADMIN)
- **응답 204**

---

## 5. 구매 희망 등록글

### POST /api/wanted
구매 희망 등록.

- **인증**: USER
- **요청 Body** (JSON)
```json
{
  "server": "1서버",
  "itemId": 1,
  "maxPrice": 30000000,
  "enhanceLevel": 20,
  "ritualConditions": [{ "ritualId": 1 }]
}
```
- **응답 201** — 생성된 구매희망 상세

---

### GET /api/wanted
구매 희망 목록 조회.

- **인증**: 없음
- **Query Params**: `server`, `itemId`, `page`, `size`
- **응답 200** — 페이징된 목록

---

### GET /api/wanted/{wantedId}
구매 희망 상세 조회.

- **인증**: 없음
- **응답 200**

---

### DELETE /api/wanted/{wantedId}
구매 희망 취소.

- **인증**: USER (본인)
- **응답 204**

---

## 6. 채팅 · 거래 확정

### POST /api/chat-rooms
채팅방 생성 (흥정하기 / 거래신청).

- **인증**: USER
- **요청 Body**
```json
{ "listingId": 1, "purpose": "TRADE" }
```
- **응답 201** — 생성된 채팅방 정보 (기존 방이 있으면 기존 방 반환)

---

### GET /api/chat-rooms
내 채팅방 목록.

- **인증**: USER
- **응답 200** — 채팅방 목록 (상대방 정보, 최근 메시지 포함)

---

### GET /api/chat-rooms/{chatRoomId}
채팅방 상세 + 메시지 목록 (최신 50건).

- **인증**: USER (참여자만)
- **응답 200**

---

### POST /api/chat-rooms/{chatRoomId}/messages
메시지 전송.

- **인증**: USER (참여자만)
- **요청 Body**: `{ "content": "안녕하세요" }`
- **응답 201** — 전송된 메시지

---

### POST /api/chat-rooms/{chatRoomId}/poster-confirm
게시자 거래완료 요청 (1단계 → `POSTER_CONFIRMED`).

- **인증**: USER (게시자만)
- **응답 200**

---

### POST /api/chat-rooms/{chatRoomId}/counterparty-confirm
상대방 거래완료 확인 (2단계 → `COMPLETED`, TradeConfirmed 생성).

- **인증**: USER (상대방만)
- **응답 200**

---

## 7. 신고

### POST /api/reports
신고 접수.

- **인증**: USER
- **요청 Body**
```json
{
  "targetType": "LISTING",
  "targetId": 1,
  "reason": "현금 거래 의심"
}
```
- **응답 201**

---

### GET /api/reports/me
내 신고 내역 조회.

- **인증**: USER
- **응답 200** — 내가 등록한 신고 목록 (처리 상태 포함)

---

## 8. 시세 조회

`GET /api/items/{itemId}/price-history` — [2. 아이템 카탈로그](#2-아이템-카탈로그) 참고.

---

## 9. 거래 평가

### POST /api/reviews/{reviewId}
거래 평가 제출 (revealAt 이전 제출, 블라인드).

- **인증**: USER
- **요청 Body**: `{ "rating": "GOOD" }` (`GOOD` | `NEUTRAL` | `BAD`)
- **응답 200**

---

### GET /api/reviews/received
내가 받은 공개된 평가 목록.

- **인증**: USER
- **응답 200**
```json
[
  { "id": 1, "rating": "GOOD", "revealAt": "2026-04-08T02:00:00" }
]
```

---

### GET /api/users/{userId}/reviews
타 유저가 받은 공개 리뷰 목록.

- **인증**: 없음
- **응답 200** — 공개된(revealAt 만료) 리뷰 목록

---

## 10. 유저 프로필

### GET /api/users/me
내 프로필 조회.

- **인증**: USER
- **응답 200**
```json
{
  "id": 1,
  "nickname": "홍길동",
  "grade": "SILVER",
  "gradeStep": 2,
  "mannerScore": 100,
  "tradeCount": 5,
  "defaultServer": "1서버"
}
```

---

### GET /api/users/me/listings
내 등록글 목록.

- **인증**: USER
- **응답 200** — 페이징된 내 등록글

---

### PATCH /api/users/me/server
기본 서버 변경.

- **인증**: USER
- **요청 Body**: `{ "server": "2서버" }`
- **응답 200**

---

### PATCH /api/users/me
프로필 수정 (닉네임, 프로필 사진, 게임 내 닉네임, 접속 가능 시간).

- **인증**: USER
- **요청 Body**
```json
{
  "nickname": "새닉네임",
  "profileImageUrl": "https://...",
  "gameNickname": "거상닉네임",
  "availableHours": "저녁 9시~12시"
}
```
- **응답 200**

---

### GET /api/users/{userId}
타 유저 공개 프로필 조회.

- **인증**: 없음
- **응답 200**
```json
{
  "id": 2,
  "nickname": "거래왕",
  "grade": "GOLD",
  "gradeStep": 1,
  "mannerScore": 98,
  "tradeCount": 42
}
```

---

### DELETE /api/users/me
회원 탈퇴 (소프트딜리트, 1년 보관).

- **인증**: USER
- **응답 204**

---

## 11. 알림 (SSE)

### GET /api/notifications/subscribe
SSE 구독. 연결 즉시 미읽음 알림 전송, 30분 타임아웃.

- **인증**: USER
- **응답**: `text/event-stream`

---

### GET /api/notifications
알림 목록 조회 (최신 50건).

- **인증**: USER
- **응답 200**
```json
[
  { "id": 1, "type": "CHAT_MESSAGE", "message": "새 메시지가 도착했습니다.", "isRead": false }
]
```

---

### PATCH /api/notifications/read-all
미읽음 알림 전체 읽음 처리.

- **인증**: USER
- **응답 200**

---

### PATCH /api/notifications/{id}/read
알림 개별 읽음 처리.

- **인증**: USER
- **응답 200**

---

## 12. 데미지 계산기

### POST /api/calculator
데미지 가성비 계산.

- **인증**: 없음
- **요청 Body**
```json
{
  "mercenaryId": 1,
  "characterLevel": 250,
  "monsterResist": 365,
  "monsterElementValue": 0,
  "items": [
    { "itemId": 1, "customPrice": null }
  ]
}
```
- **응답 200**
```json
{
  "resistPassRate": 43.0,
  "elementBonus": 30.0,
  "results": [
    {
      "itemId": 1,
      "itemName": "사인검",
      "damageIncreaseRate": 12.5,
      "price": 5000000,
      "valueScore": 2.5,
      "recommended": true
    }
  ]
}
```

---

## 13. 관리자 — 크롤러

모든 엔드포인트: **인증 ADMIN**

### POST /admin/crawler/master
전체 마스터 데이터 수집 (아이템 + 용병 + 세트 + 주술 순차 실행).

### POST /admin/crawler/items
장비·보석 아이템 수집.

### POST /admin/crawler/materials
잡화·소모품·재료 아이템 수집.

### POST /admin/crawler/mercenaries
용병 수집 (거상짱).

### POST /admin/crawler/sets
장비 세트 수집.

### POST /admin/crawler/rituals
주술 수집.

> ⚠️ 가격 수집 트리거 (`POST /admin/crawler/price`) — 2-4 기획 범위이나 현재 컨트롤러에 미구현. 가격 배치는 매월 1일 03:00 자동 실행.

---

## 14. 관리자 — 아이템

모든 엔드포인트: **인증 ADMIN**

### GET /admin/items
아이템 목록 조회.

- **Query Params**: `type` (`MATERIAL`|`EQUIPMENT`), `name`, `page`, `size`
- **응답 200** — statCount 포함 페이징 목록

---

### GET /admin/items/{itemId}
아이템 상세 조회 (기본정보 + 장비정보 + 스탯 + 스킬).

---

### PUT /admin/items/{itemId}
아이템 기본정보 수정.

- **요청 Body**: `{ "name": "...", "type": "EQUIPMENT", "tradeCategory": "..." }`

---

### PUT /admin/items/{itemId}/equipment-detail
장비 상세 수정 (슬롯, 종류, 세트 연결 등).

---

### PUT /admin/items/{itemId}/stats
아이템 스탯 전체 교체 (PUT 의미론 — 기존 전부 삭제 후 재적재).

- **요청 Body**: `{ "stats": [{ "statType": "RESIST_PIERCE", "value": 300 }] }`

---

### PUT /admin/items/{itemId}/skills
아이템 스킬 전체 교체.

- **요청 Body**: `{ "skills": ["은하수(强)", "징벌"] }`

---

## 15. 관리자 — 용병

모든 엔드포인트: **인증 ADMIN**

### GET /admin/mercenaries
용병 목록 조회.

- **Query Params**: `nature`, `nation`, `page`, `size` (기본 30, 이름순)

---

### GET /admin/mercenaries/{mercenaryId}
용병 상세 조회 (기본정보 + 스탯 + 스킬).

---

### PUT /admin/mercenaries/{mercenaryId}
용병 기본정보 수정.

- **요청 Body**: `{ "name": "...", "category": "GAK_CHEONWANG", "nation": "JOSEON", "nature": "FIRE", "natureValue": 50, "comingSoon": false }`

---

### PUT /admin/mercenaries/{mercenaryId}/stats
용병 스탯 전체 교체.

- **요청 Body**: `{ "stats": [{ "statType": "RESIST_PIERCE", "value": 136 }] }`

---

### PUT /admin/mercenaries/{mercenaryId}/skills
용병 스킬 전체 교체.

- **요청 Body**: `{ "skills": ["빙화", "빙화폭"] }`

---

### PATCH /admin/mercenaries/bulk
용병 대량 속성/국가 일괄 변경.

- **요청 Body**: `{ "ids": [1, 2, 3], "nature": "FIRE", "nation": null }`
- **응답 200**: `{ "updated": 3 }`

---

### GET /admin/mercenaries/{mercenaryId}/characteristics
용병 특성 목록 (레벨 수치 포함).

---

### POST /admin/mercenaries/{mercenaryId}/characteristics
특성 추가 (최대 4개).

- **요청 Body**: `{ "name": "저항깎 증가", "maxPoint": 10, "description": "..." }`

---

### PUT /admin/mercenaries/{mercenaryId}/characteristics/{charId}
특성 수정.

---

### DELETE /admin/mercenaries/{mercenaryId}/characteristics/{charId}
특성 삭제 (자식 특성 있으면 400).

---

### PUT /admin/mercenaries/{mercenaryId}/characteristics/{charId}/levels
레벨 수치 일괄 저장 (PUT 의미론).

- **요청 Body**: `{ "levels": [{ "level": 1, "value": 10 }, { "level": 2, "value": 20 }] }`

---

## 16. 관리자 — 장비 세트

모든 엔드포인트: **인증 ADMIN**

### GET /admin/sets
장비 세트 목록 조회.

- **Query Params**: `name` (이름 검색)
- **응답 200** — 세트 목록

---

### GET /admin/sets/{id}
세트 단건 조회 (피스 목록 포함).

---

### PATCH /admin/sets/{id}
세트 수정.

- **요청 Body**: `{ "name": "...", "totalPieces": 5, "isTradeable": true }`

---

## 17. 관리자 — 스킬 계수

모든 엔드포인트: **인증 ADMIN**

### GET /admin/skill-coefficients
스킬 계수 목록 조회.

- **Query Params**: `unmeasured=true` (casts_per_second / tick_interval_ms 미측정만)
- **응답 200**
```json
[
  {
    "id": 1,
    "gerniverseRowId": "4e561dac-...",
    "ownerType": "MERCENARY",
    "ownerName": "마조",
    "skillId": 1,
    "skillName": "팔괘진",
    "skillKey": "vkfrhowls",
    "coefStr": 0.0, "coefDex": 0.0, "coefVit": 0.0,
    "coefInt": 25.0, "coefAtk": 25.0, "coefLvl": 30.0,
    "hitCount": 20,
    "damageRangeFactor": 0.1,
    "skillType": "PERSISTENT",
    "castsPerSecond": null,
    "tickIntervalMs": null,
    "confidence": "HIGH",
    "measurementNote": null
  }
]
```

---

### PUT /admin/skill-coefficients
`Skill-coeff.json` 배열 bulk upsert. `gerniverse_row_id` 기준 — 없으면 신규, 있으면 업데이트.

- **요청 Body**: JSON 파일 내용 그대로 (배열)
- **응답 200**: `{ "upserted": 55, "skipped": 0 }`

**curl 예시**
```bash
curl -X PUT http://localhost:8080/admin/skill-coefficients \
  -H "Content-Type: application/json" \
  -b "SESSION=<쿠키>" \
  -d @docs/Skill-coeff-entity.json
```

---

### PATCH /admin/skill-coefficients/{id}/measurement
직접 측정값 입력.

- **요청 Body**
```json
// INSTANT 스킬
{ "castsPerSecond": 0.45, "measurementNote": "자동전투 기준" }

// PERSISTENT 스킬
{ "tickIntervalMs": 500, "measurementNote": "팔괘진 0.5초 주기" }
```
- **응답 200** — 업데이트된 스킬 계수

---

## 18. 관리자 — 등록글 숨김

모든 엔드포인트: **인증 ADMIN**

### PATCH /admin/listings/{listingId}/hide
등록글 숨김 (`hidden=true`). 판매자 취소와 구분.

### PATCH /admin/listings/{listingId}/unhide
등록글 숨김 해제.

---

## 19. 관리자 — 신고 처리

모든 엔드포인트: **인증 ADMIN**

### GET /admin/reports
신고 목록 조회.

- **Query Params**: `status` (`PENDING`|`REVIEWING`|`PROCESSED`|`DISMISSED`), `page`, `size`

---

### PATCH /admin/reports/{reportId}/review
신고 검토 시작 (`PENDING` → `REVIEWING`).

---

### PATCH /admin/reports/{reportId}/process
신고 처리 완료 (`REVIEWING` → `PROCESSED`).

---

### PATCH /admin/reports/{reportId}/dismiss
신고 기각 (`REVIEWING` → `DISMISSED`).

---

### POST /admin/users/{userId}/block
사용자 차단 (`status=BLOCKED`).

---

### POST /admin/users/{userId}/unblock
사용자 차단 해제.

---

### PATCH /admin/messages/{messageId}/hide
채팅 메시지 숨김.

---

### PATCH /admin/messages/{messageId}/unhide
채팅 메시지 숨김 해제.

---

## 공통 에러 응답

| 상태 코드 | 의미 |
|----------|------|
| 400 | 요청 파라미터 오류 (유효성 검증 실패) |
| 401 | 미인증 (토큰 없음·만료) |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 충돌 (중복 등록 등) |
| 500 | 서버 내부 오류 |
