# 프론트엔드 API 연동 체크리스트

> 백엔드 `api-checklist-spec.md` 기준 API 목록.  
> 프론트에서 해당 API를 연동 완료하면 `[ ]` → `[x]`로 체크.

---

## 1. 서버(게임 서버) 설정

| 체크 | 메서드 | 경로 | 설명 | 인증 |
|------|--------|------|------|------|
| `[x]` | `GET` | `/api/servers` | 게임 서버 목록 조회 | 불필요 | ← `layout.tsx` 헤더 드롭다운
| `[x]` | `PATCH` | `/api/users/me/server` | 로그인 유저 서버 설정 저장 | 필요 | ← 헤더 서버 선택 + 프로필 저장

---

## 2. 유저

| 체크 | 메서드 | 경로 | 설명 | 인증 |
|------|--------|------|------|------|
| `[x]` | `GET` | `/api/users/me` | 내 프로필 조회 | 필요 | ← `profile/page.tsx`
| `[x]` | `PATCH` | `/api/users/me` | 닉네임·사진·접속시간 등 수정 | 필요 | ← 프로필 수정 폼
| `[x]` | `PATCH` | `/api/users/me/server` | 서버 설정 저장 | 필요 | ← 헤더 드롭다운
| `[x]` | `GET` | `/api/users/{userId}` | 타 유저 프로필 조회 | 불필요 | ← `api.ts` 정의됨 (trade 상세 모달에서 필요 시 활용)
| `[x]` | `POST` | `/api/users/me/clear-time` | 클리어타임 저장 (경험치 지급) | 필요 | ← `api.submitClearTime` 정의됨
| `[x]` | `GET` | `/api/decks` | 내 덱 목록 조회 | 필요 | ← `deck/page.tsx`
| `[x]` | `POST` | `/api/decks` | 덱 생성 | 필요 | ← 덱 빌더
| `[x]` | `GET` | `/api/decks/{deckId}` | 덱 상세 (멤버·슬롯 포함) | 필요 | ← 덱 빌더
| `[x]` | `PATCH` | `/api/decks/{deckId}` | 덱 이름·활성 여부 수정 | 필요 | ← 덱 이름 인라인 편집
| `[x]` | `DELETE` | `/api/decks/{deckId}` | 덱 삭제 | 필요 | ← 덱 카드 삭제 버튼
| `[x]` | `POST` | `/api/decks/{deckId}/members` | 덱에 용병 추가 (최대 12) | 필요 | ← 용병 선택 모달
| `[x]` | `DELETE` | `/api/decks/{deckId}/members/{memberId}` | 덱에서 용병 제거 | 필요 | ← 멤버 카드 X 버튼
| `[x]` | `GET` | `/api/decks/{deckId}/members/{memberId}/stats` | 용병 합산 스탯 조회 | 필요 | ← `api.getDeckMemberStats` 정의됨
| `[x]` | `PUT` | `/api/decks/{deckId}/members/{memberId}/slots/{slot}` | 슬롯 장비 착용/교체 | 필요 | ← 장비 슬롯 클릭
| `[x]` | `DELETE` | `/api/decks/{deckId}/members/{memberId}/slots/{slot}` | 슬롯 장비 해제 | 필요 | ← 장착된 슬롯 클릭 해제
| `[x]` | `PUT` | `/api/decks/{deckId}/members/{memberId}/slots/{slot}/ritual` | 슬롯 주술 등록/교체 | 필요 | ← `api.setSlotRitual` 정의됨
| `[x]` | `DELETE` | `/api/decks/{deckId}/members/{memberId}/slots/{slot}/ritual` | 슬롯 주술 해제 | 필요 | ← `api.removeSlotRitual` 정의됨

---

## 3. 아이템 카탈로그

| 체크 | 메서드 | 경로 | 설명 | 인증 |
|------|--------|------|------|------|
| `[x]` | `GET` | `/api/items/search` | 아이템명 자동완성 (`q` 파라미터) | 불필요 | ← 거래 사이드바 + 시세 드로어
| `[x]` | `GET` | `/api/items/{itemId}/rituals` | 장비 적용 가능 주술 목록 | 불필요 | ← `api.getItemRituals` 정의됨 (슬롯 주술 설정 시 활용)
| `[x]` | `GET` | `/api/items/equipment?slot={EquipSlot}` | 덱 슬롯별 착용 가능 장비 목록 + 스탯 | 불필요 | ← 덱 빌더 장비 선택 모달
| `[x]` | `GET` | `/api/sets` | 세트 목록 조회 | 불필요 | ← `api.getSets` 정의됨
| `[x]` | `GET` | `/api/sets/{setId}` | 세트 상세 | 불필요 | ← `api.getSet` 정의됨

---

## 4. 거래 리스팅

| 체크 | 메서드 | 경로 | 설명 | 인증 |
|------|--------|------|------|------|
| `[x]` | `GET` | `/api/listings` | 판매 리스팅 목록 (type·server 필터) | 불필요 | ← `trade/page.tsx`
| `[x]` | `GET` | `/api/listings/{listingId}` | 판매 리스팅 상세 | 불필요 | ← `api.getListing` 정의됨 (listings/[id])
| `[x]` | `POST` | `/api/listings` | 판매 리스팅 등록 (단품/세트) | 필요 | ← `listings/create/page.tsx`
| `[ ]` | `PATCH` | `/api/listings/{listingId}` | 판매 리스팅 수정 | 필요 | ← 백엔드 미구현 (backend-gaps.md #4)
| `[x]` | `DELETE` | `/api/listings/{listingId}` | 판매 리스팅 취소 | 필요 | ← 거래 상세 모달 삭제 버튼
| `[x]` | `GET` | `/api/users/me/listings` | 내 판매 리스팅 목록 | 필요 | ← 프로필 거래 내역
| `[x]` | `GET` | `/api/wanted` | 구매 희망 목록 | 불필요 | ← `trade/page.tsx`
| `[x]` | `POST` | `/api/wanted` | 구매 희망 등록 | 필요 | ← `wanted/page.tsx`
| `[x]` | `GET` | `/api/wanted/{wantedId}` | 구매 희망 상세 | 불필요 | ← `api.getWantedDetail` 정의됨
| `[x]` | `DELETE` | `/api/wanted/{wantedId}` | 구매 희망 취소 | 필요 | ← 거래 상세 모달 삭제 버튼

---

## 5. 채팅 및 거래 확정

| 체크 | 메서드 | 경로 | 설명 | 인증 |
|------|--------|------|------|------|
| `[x]` | `POST` | `/api/chat-rooms` | 채팅방 생성 (listingId 기반) | 필요 | ← 거래 카드 채팅하기
| `[x]` | `GET` | `/api/chat-rooms` | 내 채팅방 목록 | 필요 | ← `chat/page.tsx`
| `[x]` | `GET` | `/api/chat-rooms/{chatRoomId}` | 채팅방 상세 + 메시지 목록 | 필요 | ← `chat/[id]/page.tsx`
| `[x]` | `POST` | `/api/chat-rooms/{chatRoomId}/messages` | 메시지 전송 | 필요 | ← 채팅 모달 + chat/[id]
| `[x]` | `POST` | `/api/chat-rooms/{chatRoomId}/poster-confirm` | 게시자 거래완료 (1단계) | 필요 | ← `chat/[id]/page.tsx`
| `[x]` | `POST` | `/api/chat-rooms/{chatRoomId}/counterparty-confirm` | 상대방 거래완료 확인 (2단계) | 필요 | ← `chat/[id]/page.tsx`

---

## 6. 거래 평가 (블라인드 리뷰)

| 체크 | 메서드 | 경로 | 설명 | 인증 |
|------|--------|------|------|------|
| `[x]` | `POST` | `/api/reviews/{reviewId}` | 블라인드 리뷰 등록 (`GOOD`\|`NEUTRAL`\|`BAD`) | 필요 | ← 프로필 리뷰 제출 모달
| `[x]` | `GET` | `/api/reviews/received` | 내가 받은 리뷰 목록 | 필요 | ← 프로필 받은 리뷰 섹션
| `[x]` | `GET` | `/api/users/{userId}/reviews` | 타 유저 리뷰 목록 | 불필요 | ← `api.getUserReviews` 정의됨

---

## 7. 시세 조회

| 체크 | 메서드 | 경로 | 설명 | 인증 |
|------|--------|------|------|------|
| `[x]` | `GET` | `/api/items/{itemId}/price-history` | 아이템 시세 조회 | 불필요 | ← 거래 페이지 시세 드로어 (`days` 파라미터 사용)

---

## 8. 유저 등급

| 체크 | 메서드 | 경로 | 설명 | 인증 |
|------|--------|------|------|------|
| `[x]` | `GET` | `/api/grades` | 등급 정책 목록 (툴팁용) | 불필요 | ← `api.getGrades` 정의됨
| `[x]` | `GET` | `/api/users/me/grade` | 내 등급·경험치 조회 | 필요 | ← 프로필 경험치 바

---

## 9. 신고

| 체크 | 메서드 | 경로 | 설명 | 인증 |
|------|--------|------|------|------|
| `[x]` | `POST` | `/api/reports` | 신고 등록 | 필요 | ← 거래 상세 모달 신고 버튼
| `[x]` | `GET` | `/api/reports/me` | 내 신고 내역 | 필요 | ← `api.getMyReports` 정의됨

---

## 10. DPS 계산기

| 체크 | 메서드 | 경로 | 설명 | 인증 |
|------|--------|------|------|------|
| `[x]` | `POST` | `/api/calculator/dps` | DPS 계산 (덱 기반) | 불필요 | ← 덱 빌더 DPS 계산 버튼
| `[x]` | `GET` | `/api/monsters` | 몬스터 목록 조회 | 불필요 | ← 덱 빌더 몬스터 선택 모달
| `[x]` | `GET` | `/api/monsters/{monsterId}` | 몬스터 스펙 조회 | 불필요 | ← `api.getMonster` 정의됨

> ⚠️ `GET /api/mercenaries` — 체크리스트 누락 항목. 덱 빌더 용병 선택 모달에서 사용 (`api.getMercenaries` 구현됨). 백엔드 구현 여부 확인 필요.

---

## 11. 가성비 비교

| 체크 | 메서드 | 경로 | 설명 | 인증 |
|------|--------|------|------|------|
| `[ ]` | `POST` | `/api/calculator/value-comparison` | 가성비 비교 계산 | 필요 | ← 백엔드 미구현

---

## 12. 홈 화면

| 체크 | 메서드 | 경로 | 설명 | 인증 |
|------|--------|------|------|------|
| `[x]` | `GET` | `/api/home/price-watch` | 관심 아이템 시세 변동 | 필요 | ← 홈 페이지 (API 실패 시 빈 화면 graceful 처리)
| `[ ]` | `GET` | `/api/home/spec-up` | 스펙업 추천 | 필요 | ← 백엔드 미구현 (플레이스홀더 표시)

---

## 13. 알림 (SSE)

| 체크 | 메서드 | 경로 | 설명 | 인증 |
|------|--------|------|------|------|
| `[x]` | `GET` | `/api/notifications/subscribe` | SSE 구독 | 필요 | ← `notifications/page.tsx`
| `[x]` | `GET` | `/api/notifications` | 알림 목록 조회 | 필요 | ← notifications + 헤더 미읽음 뱃지
| `[x]` | `PATCH` | `/api/notifications/read-all` | 미읽음 전체 읽음 처리 | 필요 | ← `notifications/page.tsx`
| `[x]` | `PATCH` | `/api/notifications/{id}/read` | 알림 개별 읽음 처리 | 필요 | ← `api.markRead` 정의됨

---

## 인증 (OAuth2)

| 체크 | 경로 | 설명 |
|------|------|------|
| `[x]` | `GET /oauth2/authorization/google` | Google 소셜 로그인 시작 | ← `login/page.tsx`
| `[x]` | `GET /oauth2/authorization/naver` | Naver 소셜 로그인 시작 | ← `login/page.tsx`
| `[x]` | `POST /api/auth/logout` | 로그아웃 | ← `layout.tsx` 로그아웃 버튼
