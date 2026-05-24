# 거상 거래 플랫폼 — API 설계 및 구현 명세

> 작성 기준일: 2026-05-20  
> 목적: API 구현 점검 및 미구현 항목 명세  
> 대상: 백엔드 개발자 / Claude Code

> **Flyway 정책**: Flyway(V1 스키마 마이그레이션)는 프론트엔드 구현 완료 후 로컬 개발 전체가 끝난 시점에 한 번에 진행한다. 개발 중 엔티티 변경이 잦으므로 스키마 안정화 전까지 `ddl-auto=create` 유지.

> **경로 규칙**: 유저 API는 `/api/` 접두사 사용. 관리자 API는 `/admin/` 접두사 사용.

---

## 목차

1. [서버(게임 서버) 설정](#1-서버게임-서버-설정)
2. [유저](#2-유저)
3. [아이템 카탈로그](#3-아이템-카탈로그)
4. [거래 리스팅](#4-거래-리스팅)
5. [채팅 및 거래 확정 흐름](#5-채팅-및-거래-확정-흐름)
6. [거래 평가 (블라인드 리뷰)](#6-거래-평가-블라인드-리뷰)
7. [시세 조회](#7-시세-조회)
8. [유저 등급](#8-유저-등급)
9. [신고 시스템](#9-신고-시스템)
10. [DPS 계산기 (덱파워)](#10-dps-계산기-덱파워)
11. [가성비 비교](#11-가성비-비교)
12. [홈 화면](#12-홈-화면)
13. [알림 (SSE)](#13-알림-sse)
14. [관리자](#14-관리자)

---

## 1. 서버(게임 서버) 설정

### 1.1 설계 확정 사항

거상 게임 내 서버(예: 하늘, 바람 등)를 선택하면 거래 관련 모든 조회가 해당 서버 데이터만 노출된다.  
서버 변경은 거의 발생하지 않으며, 비로그인 유저도 서버를 선택하여 탐색할 수 있어야 한다.

**저장 방식: localStorage + 로그인 시 DB 동기화 (하이브리드)**

```
비로그인 유저:
  서버 선택 → 프론트 localStorage에 저장
  모든 API 요청 헤더에 X-Server-Id: "{serverId}" 포함

로그인 유저:
  로그인 시 localStorage에 serverId가 있으면 → DB에 PATCH /api/users/me/server 동기화
  localStorage에 serverId가 없으면 → DB에서 꺼내 localStorage에 씀
  이후 서버 변경 시 → localStorage + DB 동시 업데이트

백엔드:
  모든 거래 관련 API에서 X-Server-Id 헤더를 추출하여 쿼리 조건에 자동 적용
  헤더 없는 요청 → 400 또는 기본 서버 fallback (정책 결정 필요)
```

### 1.2 API

| 메서드 | 경로 | 설명 | 인증 | 구현 여부 |
|---|---|---|---|---|
| `GET` | `/api/servers` | 게임 서버 목록 조회 | 불필요 | ✅ |
| `PATCH` | `/api/users/me/server` | 로그인 유저 서버 설정 저장 | 필요 | ✅ |



### 1.3 헤더 규칙

모든 거래 관련 API는 `X-Server-Id` 헤더를 필수로 받는다.  
인터셉터에서 헤더 추출 → `ServerContext`에 저장 → 서비스 레이어에서 사용.

---

## 2. 유저

### 2.1 설계 확정 사항

- 유저 등급은 직접 수정 불가. 거래 경험치 누적으로만 상승.
- 닉네임, 프로필 사진, 게임 내 닉네임, 접속 가능 시간은 유저가 직접 등록·수정 가능.
- 개발자에게 유용한 데이터(덱, 클리어타임, 몬스터)를 저장하면 경험치 지급.

### 2.2 API

| 메서드 | 경로 | 설명 | 인증 | 구현 여부 |
|---|---|---|---|---|
| `GET` | `/api/users/me` | 내 프로필 조회 | 필요 | ✅ |
| `PATCH` | `/api/users/me` | 닉네임·사진·접속시간 등 수정 | 필요 | ✅ (`profileImageUrl` 포함) |
| `PATCH` | `/api/users/me/server` | 서버 설정 저장 | 필요 | ✅ |
| `GET` | `/api/users/{userId}` | 타 유저 프로필 조회 | 불필요 | ✅ |
| `POST` | `/api/users/me/clear-time` | 클리어타임 저장 (경험치 지급) | 필요 | ✅ (5 EXP 지급) |
| `GET` | `/api/decks` | 내 덱 목록 조회 | 필요 | ✅ |
| `POST` | `/api/decks` | 덱 생성 | 필요 | ✅ |
| `GET` | `/api/decks/{deckId}` | 덱 상세 (멤버·슬롯 포함) | 필요 | ✅ |
| `PATCH` | `/api/decks/{deckId}` | 덱 이름·활성 여부 수정 | 필요 | ✅ |
| `DELETE` | `/api/decks/{deckId}` | 덱 삭제 | 필요 | ✅ |
| `POST` | `/api/decks/{deckId}/members` | 덱에 용병 추가 (최대 12) | 필요 | ✅ |
| `DELETE` | `/api/decks/{deckId}/members/{memberId}` | 덱에서 용병 제거 | 필요 | ✅ |
| `GET` | `/api/decks/{deckId}/members/{memberId}/stats` | 용병 합산 스탯 조회 (기본+장비 합산) | 필요 | ✅ |
| `PUT` | `/api/decks/{deckId}/members/{memberId}/slots/{slot}` | 슬롯 장비 착용/교체 | 필요 | ✅ |
| `DELETE` | `/api/decks/{deckId}/members/{memberId}/slots/{slot}` | 슬롯 장비 해제 | 필요 | ✅ |
| `PUT` | `/api/decks/{deckId}/members/{memberId}/slots/{slot}/ritual` | 슬롯 주술 등록/교체 | 필요 | ✅ |
| `DELETE` | `/api/decks/{deckId}/members/{memberId}/slots/{slot}/ritual` | 슬롯 주술 해제 | 필요 | ✅ |

### 2.3 덱 구조 상세

```
엔티티 레이어 구현 완료 (2026-05-20):
  UserDeck           — name + isActive + 스탯 캐시(attrXValue, totalResDown)
  UserDeckMember     — 덱 내 용병 (최대 12, 중복 불가)
  UserDeckMemberSlot — 용병별 18슬롯 (일반 9 + 외변 9). 착용 시만 row 생성
  UserDeckMemberSlotRitual — 슬롯별 주술 (1:1, 미적용 시 row 없음)

슬롯 구성 (EquipSlot enum):
  일반 (9슬롯): HELMET / ARMOR / WEAPON / SHOES / GLOVES / BELT / CHARM / RING_1 / RING_2
  외변 (9슬롯): APP_SPIRIT / APP_HELMET / APP_ARMOR / APP_WEAPON / APP_WAR_GOD
                / APP_EARRING / APP_NECKLACE / APP_BRACELET / APP_GREAVES

세트효과·주술 세트효과: DB 저장 없음. 계산기 호출 시 런타임 계산.
```

---

## 3. 아이템 카탈로그

### 3.1 설계 확정 사항

카탈로그 API는 마스터 데이터(크롤링으로 구축된 아이템 원장) 조회 전용이다.  
거래 리스팅 검색과 **2단계로 분리**한다.

```
1단계: 자동완성 (카탈로그 API)
  GET /api/items/search?q=지국
  → 리스팅 유무와 무관하게 아이템명 목록 반환
  → 유저가 아이템을 선택

2단계: 리스팅 조회 (거래 API)
  GET /api/listings?itemId=123
  → 결과 0건이면 "현재 등록된 매물이 없어요" 표시
```

리스팅에 없는 아이템도 검색 가능해야 한다.  
유저가 "아이템 자체가 없다"고 오인하는 상황을 방지하기 위함.

### 3.2 API

| 메서드 | 경로 | 설명 | 인증 | 구현 여부 |
|---|---|---|---|---|
| `GET` | `/api/items/search` | 아이템명 자동완성 (`q` 파라미터) | 불필요 | ✅ |
| `GET` | `/api/items/{itemId}/rituals` | 장비 적용 가능 주술 목록 | 불필요 | ✅ |
| `GET` | `/api/items/equipment?slot={EquipSlot}` | 덱 슬롯별 착용 가능 장비 목록 + 스탯 | 불필요 | ✅ |
| `GET` | `/api/sets` | 세트 목록 조회 | 불필요 | ✅ |
| `GET` | `/api/sets/{setId}` | 세트 상세 | 불필요 | ✅ |

> **참고**: 설계서에 있던 `GET /items/autocomplete`는 `/api/items/search`로 구현되었고 응답 구조도 유사함.  
> `GET /api/items/{itemId}` 단건 조회는 아이템 CRUD out-of-scope 정책으로 미구현. 필요 시 추가 결정 필요.  
> `/api/sets`는 `isTradeable=true` 세트만 노출한다. 피스·세트효과 상세는 현재 미포함.

### 3.3 자동완성 응답 예시

```json
GET /api/items/search?q=지국

[
  { "id": 123, "name": "지국천왕 투구", "type": "EQUIPMENT" },
  { "id": 124, "name": "지국천왕 갑옷", "type": "EQUIPMENT" }
]
```

---

## 4. 거래 리스팅

### 4.1 설계 확정 사항

- 리스팅 등록 시 유저가 입력하는 정보: **아이템 선택, 가격, 설명(20자 내외)**
- 나머지(닉네임, 접속 가능 시간, 서버)는 유저 프로필에서 자동으로 가져옴
- 목록 조회 시 구매 리스팅과 판매 리스팅을 함께 반환, 프론트에서 좌/우 분리 표시
- 단품과 세트 모두 등록·조회 가능

> **구현 현황**: 현재 판매 리스팅(`/api/listings`)과 구매 희망(`/api/wanted`)이 별도 컨트롤러로 분리되어 있음.  
> 설계서는 `type=SELL|BUY`로 통합 조회를 원하나, 현재 구조는 분리됨. 통합 여부 결정 필요.

### 4.2 리스팅 목록 노출 항목

```
- 아이템명 (단품) 또는 세트명 + 구성 요약 (세트)
- 판매자/구매자 닉네임
- 가격
- 설명 (20자 내외)
- 접속 가능 시간
- 리스팅 유형 (SELL / BUY)
```

### 4.3 리스팅 상세 페이지 구성

```
좌측: 등록자 프로필
  - 닉네임
  - 게임 내 닉네임
  - 접속 가능 시간
  - 유저 등급
  - 프로필 사진

우측: 거래 내용
  - 아이템 정보 (단품 또는 세트·피스·주술 구성)
  - 가격
  - 설명

하단: 채팅하기 버튼 (채팅 API 연동)
```

### 4.4 API

| 메서드 | 경로 | 설명 | 인증 | 구현 여부 |
|---|---|---|---|---|
| `GET` | `/api/listings` | 판매 리스팅 목록 (itemId·type·server 필터) | 불필요 | ⚠️ 구현됨 — `itemId` 필터 미구현 |
| `GET` | `/api/listings/{listingId}` | 판매 리스팅 상세 | 불필요 | ✅ |
| `POST` | `/api/listings` | 판매 리스팅 등록 (단품/세트) | 필요 | ✅ |
| `PATCH` | `/api/listings/{listingId}` | 판매 리스팅 수정 | 필요 | ❌ 미구현 |
| `DELETE` | `/api/listings/{listingId}` | 판매 리스팅 취소 | 필요 | ✅ |
| `GET` | `/api/users/me/listings` | 내 판매 리스팅 목록 | 필요 | ✅ |
| `GET` | `/api/wanted` | 구매 희망 목록 | 불필요 | ✅ |
| `POST` | `/api/wanted` | 구매 희망 등록 | 필요 | ✅ |
| `GET` | `/api/wanted/{wantedId}` | 구매 희망 상세 | 불필요 | ✅ |
| `DELETE` | `/api/wanted/{wantedId}` | 구매 희망 취소 | 필요 | ✅ |

### 4.5 목록 조회 쿼리 파라미터

```
itemId      : 아이템 ID (카탈로그 자동완성에서 선택한 값) — ⚠️ 미구현, 추가 필요
type        : SELL | BUY | ALL (기본값 ALL) — ⚠️ 현재 판매/구매 컨트롤러 분리 구조
X-Server-Id : 헤더로 서버 필터 자동 적용
page, size  : 페이지네이션
sort        : latest | price_asc | price_desc
```

---

## 5. 채팅 및 거래 확정 흐름

### 5.1 설계 확정 사항

```
거래 확정 흐름:
  1. 구매자가 리스팅 상세에서 "채팅하기" → 채팅방 생성
  2. 채팅으로 협의 진행
  3. 양쪽 모두 "거래완료" 버튼 클릭
     → 채팅방 종료
     → 양쪽에 알림 발송: "거래완료! 3일 이내 블라인드 리뷰를 남겨보세요"
     → 거래 경험치 양쪽 지급
     → TradeLog INSERT
     → PriceStatDaily UPSERT (시세 통계 즉시 반영)

한쪽만 거래완료 클릭:
  → 24시간 대기 (상대방에게 알림 발송)
  → 24시간 후 자동 거래 종료
  → 거래완료 미클릭 유저: 경험치 없음
  → 해당 유저의 미확정 카운트 +1 (누적 N회 시 제재 대상)
  → N 기준값: 추후 결정, 카운트 컬럼은 미리 생성
```

> **구현 현황**: 거래 확정은 2단계로 분리 구현됨.  
> 게시자 먼저 확정(`poster-confirm`) → 상대방이 최종 확정(`counterparty-confirm`) 시 거래 완료.

### 5.2 API

| 메서드 | 경로 | 설명 | 인증 | 구현 여부 |
|---|---|---|---|---|
| `POST` | `/api/chat-rooms` | 채팅방 생성 (listingId 기반) | 필요 | ✅ |
| `GET` | `/api/chat-rooms` | 내 채팅방 목록 | 필요 | ✅ |
| `GET` | `/api/chat-rooms/{chatRoomId}` | 채팅방 상세 + 메시지 목록 | 필요 | ✅ |
| `POST` | `/api/chat-rooms/{chatRoomId}/messages` | 메시지 전송 | 필요 | ✅ |
| `POST` | `/api/chat-rooms/{chatRoomId}/poster-confirm` | 게시자 거래완료 (1단계) | 필요 | ✅ |
| `POST` | `/api/chat-rooms/{chatRoomId}/counterparty-confirm` | 상대방 거래완료 확인 (2단계) | 필요 | ✅ |

---

## 6. 거래 평가 (블라인드 리뷰)

### 6.1 설계 확정 사항

```
- 거래완료 시점(채팅 종료 시점)부터 3일간 리뷰 창 열림
- 선택적으로 진행 (미진행 시 패널티 없음)
- 리뷰 제출 시 추가 경험치 지급
- 양쪽 리뷰는 3일 만료 후 동시 공개 (블라인드)
- 채팅 종료 후 상대방에게 리뷰를 강요하는 행위 불가
  (채팅방이 닫혀 있으므로 구조적으로 차단)
```

> **구현 현황**: 거래 완료 시 Review 레코드가 양쪽에 자동 생성되며, 유저는 기존 레코드에 rating을 제출.  
> 리뷰 ID(`reviewId`)는 거래 완료 응답 또는 내 리뷰 목록 API에서 확인 가능.

### 6.2 API

| 메서드 | 경로 | 설명 | 인증 | 구현 여부 |
|---|---|---|---|---|
| `POST` | `/api/reviews/{reviewId}` | 블라인드 리뷰 등록 (`GOOD`\|`NEUTRAL`\|`BAD`) | 필요 | ✅ |
| `GET` | `/api/reviews/received` | 내가 받은 리뷰 목록 (만료 후 공개) | 필요 | ✅ |
| `GET` | `/api/users/{userId}/reviews` | 타 유저 리뷰 목록 | 불필요 | ✅ |

---

## 7. 시세 조회

### 7.1 설계 확정 사항

배치 없이 **거래 완료 즉시** 일별 통계 테이블에 반영한다.

```
거래 완료 시 처리 순서:
  1. TradeLog INSERT (원장, 불변)
  2. PriceStatDaily UPSERT
     - 행 없으면 INSERT
     - 행 있으면 UPDATE (거래수 +1, 평균가 재계산)

평균가 재계산 공식:
  new_avg = (old_avg * old_count + new_price) / (old_count + 1)

날짜 기준:
  KST 기준 (Asia/Seoul)
  LocalDate.now(ZoneId.of("Asia/Seoul"))
```

### 7.2 PriceStatDaily 테이블 구조

```
PK: (item_id, stat_date)

컬럼:
  item_id      BIGINT FK
  stat_date    DATE          -- KST 기준
  trade_count  INT
  avg_price    BIGINT
  min_price    BIGINT        -- 차트 활용 여지
  max_price    BIGINT        -- 차트 활용 여지
```

### 7.3 조회 범위

| 범위 | 설명 | 데이터 소스 |
|---|---|---|
| 5일 | 오늘 포함 최근 5일 | PriceStatDaily |
| 10일 | 오늘 포함 최근 10일 | PriceStatDaily |
| 15일 | 오늘 포함 최근 15일 | PriceStatDaily |

> 당일 거래는 거래 완료 즉시 PriceStatDaily에 반영되므로 별도 처리 불필요.

### 7.4 API

| 메서드 | 경로 | 설명 | 인증 | 구현 여부 |
|---|---|---|---|---|
| `GET` | `/api/items/{itemId}/price-history` | 아이템 시세 조회 | 불필요 | ⚠️ 구현됨 — `days` 파라미터 미구현 |

```
현재 구현 파라미터:
  from : 시작일 yyyy-MM-dd (선택)
  to   : 종료일 yyyy-MM-dd (선택)

설계 목표 파라미터:
  days : 5 | 10 | 15 (기본값 10)

→ days 파라미터 추가 또는 from/to로 통일 여부 결정 필요
```

---

## 8. 유저 등급

### 8.1 설계 확정 사항

```
- 총 5단계
- 거래 완료 시 경험치 지급 (양쪽 모두)
- 거래 평가(블라인드 리뷰) 완료 시 추가 경험치
- 덱 저장, 클리어타임 저장 등 데이터 제공 시 경험치
- 등급 직접 수정 불가 (관리자 포함)
- UI에 등급 설명 툴팁 제공
```

### 8.2 API

| 메서드 | 경로 | 설명 | 인증 | 구현 여부 |
|---|---|---|---|---|
| `GET` | `/api/grades` | 등급 정책 목록 (툴팁용) | 불필요 | ❓ 점검 필요 |
| `GET` | `/api/users/me/grade` | 내 등급·경험치 조회 | 필요 | ❓ 점검 필요 |

---

## 9. 신고 시스템

### 9.1 설계 확정 사항

```
신고 대상: 유저 / 리스팅 / 거래
신고 사유 카테고리: 사기 의심, 욕설·비방, 현금 유도, 허위 매물, 기타
키워드 감지: 현금 유도 문구 자동 탐지 (구현 완료)
어뷰징 모니터링: 미구현 (MVP 잔여 항목)
```

### 9.2 API

| 메서드 | 경로 | 설명 | 인증 | 구현 여부 |
|---|---|---|---|---|
| `POST` | `/api/reports` | 신고 등록 | 필요 | ✅ |
| `GET` | `/api/reports/me` | 내 신고 내역 | 필요 | ✅ |

---

## 10. DPS 계산기 (덱파워)

### 10.1 설계 확정 사항

```
입력값:
  - 용병 최대 12명 (주인공 포함)
  - 각 용병 레벨: 250 또는 260 (버튼 선택, 자유 입력 없음)
  - 각 용병 착용 장비
  - 각 용병 보너스 스탯 및 주스탯/체력 배분
    * 주스탯 = 스탯 계수 중 가장 높은 것
  - 각 용병 특성 포인트 배분

계산 출력:
  - 디버프 적용 (저항깎 / 속성값 증가 / 몹 속성값 감소) 후 용병별 DPS
  - 전체 DPS 합산

미확정 항목:
  - 캐스팅 속도 계수 → 확정 전까지 TODO 처리, 플레이스홀더 상수 사용
```

### 10.2 API

| 메서드 | 경로 | 설명 | 인증 | 구현 여부 |
|---|---|---|---|---|
| `POST` | `/api/calculator/dps` | DPS 계산 (덱 기반) | 불필요 | ✅ 구현 (`CalculatorController`) |
| `GET` | `/api/monsters` | 몬스터 목록 조회 | 불필요 | ✅ 구현 (`MonsterController`) |
| `GET` | `/api/monsters/{monsterId}` | 몬스터 스펙 (저항·속성·속성값) | 불필요 | ✅ 구현 (`MonsterController`) |

---

## 11. 가성비 비교

### 11.1 설계 확정 사항

DPS 계산기가 완성된 이후에 의미 있는 기능이다.  
현재 진척률 10%, DPS 계산기 완성 후 이어서 구현.

```
흐름:
  1. 유저가 저장한 덱 조회
  2. 몬스터 1종 선택 → 몬스터 스펙 표시
  3. 현재 덱 기준 DPS 확인
  4. 비교 아이템 3~5개 선택
     → 각 아이템의 최저 거래가 + 평균 거래가 표시 (PriceStatDaily 연동)
  5. 아이템별 DPS 상승률 + 가격 기준 가성비 순위 표시
```

### 11.2 API

| 메서드 | 경로 | 설명 | 인증 | 구현 여부 |
|---|---|---|---|---|
| `POST` | `/api/calculator/value-comparison` | 가성비 비교 계산 | 필요 | ❌ 미구현 |

```
요청 바디:
  deckId      : 저장된 덱 ID
  monsterId   : 몬스터 ID
  itemIds     : [itemId, ...] (3~5개)
```

---

## 12. 홈 화면

### 12.1 설계 확정 사항

```
구현 완료:
  - 유저가 설정한 아이템의 시세 변동 조회

초기 단계 (현재 다른 기능 완성 후 구현 예정):
  - 덱 기준 다음 스펙업 추천
  - 개발자가 정의한 기본 루트를 순차 비교하여 추천
  - 가격 미표시

서비스 운영 단계 (데이터 충분히 쌓인 후):
  - 유저 덱·몬스터·클리어타임 데이터 기반 배치 추천
  - 구현 일정 미정
```

### 12.2 API

| 메서드 | 경로 | 설명 | 인증 | 구현 여부 |
|---|---|---|---|---|
| `GET` | `/api/home/price-watch` | 관심 아이템 시세 변동 | 필요 | ❓ 점검 필요 |
| `GET` | `/api/home/spec-up` | 스펙업 추천 (초기 단계) | 필요 | ❌ 미구현 |

---

## 13. 알림 (SSE)

### 13.1 설계 확정 사항

알림이 발생하는 시점:

```
- 거래완료 버튼 클릭 후 상대방에게 알림
- 한쪽만 거래완료 클릭 시 24시간 후 자동 종료 알림
- 거래 완료 시 양쪽에 "블라인드 리뷰" 안내 알림
- 채팅 메시지 수신
```

### 13.2 API

| 메서드 | 경로 | 설명 | 인증 | 구현 여부 |
|---|---|---|---|---|
| `GET` | `/api/notifications/subscribe` | SSE 구독 | 필요 | ✅ |
| `GET` | `/api/notifications` | 알림 목록 조회 | 필요 | ✅ |
| `PATCH` | `/api/notifications/read-all` | 미읽음 알림 전체 읽음 처리 | 필요 | ✅ |
| `PATCH` | `/api/notifications/{id}/read` | 알림 개별 읽음 처리 | 필요 | ✅ |

---

## 14. 관리자

### 14.1 API

| 메서드 | 경로 | 설명 | 구현 여부 |
|---|---|---|---|
| `GET` | `/admin/reports` | 신고 목록 조회 | ✅ |
| `PATCH` | `/admin/reports/{reportId}/review` | 신고 검토 시작 | ✅ |
| `PATCH` | `/admin/reports/{reportId}/process` | 신고 처리 완료 | ✅ |
| `PATCH` | `/admin/reports/{reportId}/dismiss` | 신고 기각 | ✅ |
| `POST` | `/admin/users/{userId}/block` | 유저 차단 | ✅ |
| `POST` | `/admin/users/{userId}/unblock` | 차단 해제 | ✅ |
| `PATCH` | `/admin/listings/{listingId}/hide` | 리스팅 숨김 | ✅ |
| `PATCH` | `/admin/listings/{listingId}/unhide` | 리스팅 숨김 해제 | ✅ |
| `PATCH` | `/admin/messages/{messageId}/hide` | 채팅 메시지 숨김 | ✅ |
| `PATCH` | `/admin/messages/{messageId}/unhide` | 채팅 메시지 숨김 해제 | ✅ |
| `GET` | `/admin/sets` | 세트 목록 조회 | ✅ |
| `GET` | `/admin/sets/{id}` | 세트 단건 조회 | ✅ |
| `PATCH` | `/admin/sets/{id}` | 세트 수정 (isTradeable·enhancement 포함) | ✅ |
| `GET` | `/admin/items` | 아이템 목록 조회 | ✅ |
| `GET` | `/admin/items/{itemId}` | 아이템 상세 조회 | ✅ |
| `PUT` | `/admin/items/{itemId}` | 아이템 기본정보 수정 | ✅ |
| `PUT` | `/admin/items/{itemId}/equipment-detail` | 장비 상세 수정 (slot·kind·ritual·hasSlot·set·equipSlot·**mercenary·enhancement** 포함) | ✅ |
| `PUT` | `/admin/items/{itemId}/stats` | 아이템 스탯 전체 교체 | ✅ |
| `PUT` | `/admin/items/{itemId}/skills` | 아이템 스킬 전체 교체 (**skillBehaviorType·replacesBaseSkill·triggerEveryN·triggerBaseSkillKey·note** 포함) | ✅ |
| `GET` | `/admin/mercenaries` | 용병 목록 조회 | ✅ |
| `GET` | `/admin/mercenaries/{mercenaryId}` | 용병 상세 조회 | ✅ |
| `PUT` | `/admin/mercenaries/{mercenaryId}` | 용병 기본정보 수정 | ✅ |
| `PUT` | `/admin/mercenaries/{mercenaryId}/stats` | 용병 스탯 전체 교체 | ✅ |
| `PUT` | `/admin/mercenaries/{mercenaryId}/skills` | 용병 스킬 전체 교체 | ✅ |
| `PATCH` | `/admin/mercenaries/bulk` | 용병 대량 속성/국가 수정 | ✅ |
| `GET` | `/admin/mercenaries/{mercenaryId}/characteristics` | 용병 특성 목록 | ✅ |
| `POST` | `/admin/mercenaries/{mercenaryId}/characteristics` | 특성 추가 | ✅ |
| `PUT` | `/admin/mercenaries/{mercenaryId}/characteristics/{charId}` | 특성 수정 | ✅ |
| `DELETE` | `/admin/mercenaries/{mercenaryId}/characteristics/{charId}` | 특성 삭제 | ✅ |
| `PUT` | `/admin/mercenaries/{mercenaryId}/characteristics/{charId}/levels` | 레벨 수치 일괄 저장 | ✅ |
| `GET` | `/admin/skill-coefficients` | 스킬 계수 목록 | ✅ |
| `PUT` | `/admin/skill-coefficients` | JSON 파일 bulk upsert (mercenarySkill·itemSkill·**setGrantedSkill** 3종 지원) | ✅ |
| `PATCH` | `/admin/skill-coefficients/{id}/measurement` | 측정값 입력 | ✅ |
| `GET` | `/admin/set-granted-skills` | 세트 부여 스킬 목록 조회 | ❌ 미구현 |
| `POST` | `/admin/set-granted-skills` | 세트 부여 스킬 등록 | ❌ 미구현 |
| `PUT` | `/admin/set-granted-skills/{id}` | 세트 부여 스킬 수정 | ❌ 미구현 |
| `DELETE` | `/admin/set-granted-skills/{id}` | 세트 부여 스킬 삭제 | ❌ 미구현 |
| `GET` | `/admin/sets/{setId}/skill-effects` | 세트 스킬 효과 목록 조회 (세트+피스수+강화단계→스킬 매핑) | ❌ 미구현 |
| `POST` | `/admin/sets/{setId}/skill-effects` | 세트 스킬 효과 등록 | ❌ 미구현 |
| `DELETE` | `/admin/sets/{setId}/skill-effects/{effectId}` | 세트 스킬 효과 삭제 | ❌ 미구현 |
| `POST` | `/admin/crawler/master` | 전체 마스터 데이터 수집 | ✅ |
| `POST` | `/admin/crawler/items` | 장비·보석 수집 | ✅ |
| `POST` | `/admin/crawler/materials` | 재료 수집 | ✅ |
| `POST` | `/admin/crawler/mercenaries` | 용병 수집 | ✅ |
| `POST` | `/admin/crawler/sets` | 세트 수집 | ✅ |
| `POST` | `/admin/crawler/rituals` | 주술 수집 | ✅ |
| `GET` | `/admin/abuse-monitor` | 어뷰징 모니터링 대시보드 | ❌ 미구현 |

---

## 부록 — 구현 상태 요약

| 항목 | 진척률 | 잔여 작업 |
|---|---|---|
| 인증 (OAuth2) | 95% | 코드 완성, 네이버 앱 등록·환경변수 설정 대기 |
| 아이템 카탈로그 | 95% | itemId 필터 미구현. 슬롯별 장비 조회 API 추가 |
| 거래 리스팅 (판매) | 90% | itemId 필터, PATCH 수정 API |
| 거래 리스팅 (구매) | 100% | — |
| 채팅·거래 확정 | 100% | — |
| 신고 시스템 | 95% | 어뷰징 모니터링 |
| 시세 조회 | 90% | `days` 파라미터 추가 여부 결정 |
| 유저 프로필·등급 | 100% | 프로필 조회·수정(사진 포함)·클리어타임 저장 완료 |
| 덱 관리 | 100% | CRUD API·슬롯 API·주술 API·용병 합산 스탯 조회 API 구현 완료 |
| 거래 평가 | 100% | — |
| 알림 (SSE) | 100% | — |
| DPS 계산기 | 100% | `DpsCalculatorService` + `POST /api/calculator/dps` + 단위 테스트 11케이스 완료 |
| 가성비 비교 | 10% | DPS 완성 후 이어서 |
| 관리자 기능 | 90% | SetGrantedSkill·EquipmentSetSkillEffect CRUD API 미구현, 어뷰징 모니터링 |
| 홈 화면 | — | 관심아이템 시세 점검, 스펙업 미구현 |
| 크롤링 (마스터) | 80% | 거상짱 전환 완료, 잔여 점검 |
| 크롤링 (가격) | 50% | — |
