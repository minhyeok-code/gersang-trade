# 데이터 기반 사냥 효율 & 가성비 추천 서비스 — 덱·사냥 기록 기반 스펙업 경로 추천

> 작성일: 2026-04-21 / 최종 수정: 2026-04-22
> 대상 범위: 유저 덱 정보 수집, 사냥 기록(클리어 타임) 기반 집단 분석, 최적 스펙업 경로 추천
> 기준 문서: `docs/personalization_cache_design.md`, `docs/calculator.md`, `docs/entity-model.ko.md`

---

## 1. 서비스 핵심 메커니즘 — 데이터 선순환 구조

### 1.1 데이터 수집

유저는 추천 서비스를 이용하기 위해 다음 데이터를 제공한다.

**필수 입력:**
- 현재 장착 덱 정보 (사천왕 + 용병 구성)
- 사냥 중인 특정 몬스터
- 평균 클리어 시간 (초)

**UX 전략:** 데이터 입력 시 등급 EXP 부여 + `사냥터 점프업 리포트` 해금

---

### 1.2 서비스 단계별 디벨롭

| 단계 | 명칭 | 내용 |
|------|------|------|
| 1단계 | 개인 기록 도구 | 유저가 스펙업에 따른 사냥 속도 단축 히스토리를 관리하는 '사냥 다이어리' |
| 2단계 | 집단지성 통계 | 모인 데이터를 분석하여 사천왕별·몬스터별 평균 클리어 타임 통계 제공 |
| 3단계 | 맞춤형 경로 추천 | 유저의 현재 속도와 유사한 그룹 분석 → 사냥 시간 단축에 효과적인 다음 아이템·용병을 가성비순으로 추천 |

---

## 2. 엔티티 설계

### 2.1 `UserDeck` — 유저 덱 정보 ✅ 구현 완료

유저가 저장한 용병 구성과 스펙 스냅샷을 관리한다. 덱은 불변 스냅샷으로 수정 시 새 행을 생성한다.

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `userId` | Long (FK → User) | NO | |
| `isActive` | boolean | NO | 현재 활성 덱 여부 |
| `attrXValue` | Integer | YES | 합산된 최종 속성값. `calculateTotalStats()` 캐싱 결과 |
| `totalResDown` | Integer | YES | 합산된 총 저항깎 수치. `calculateTotalStats()` 캐싱 결과 |
| `createdAt` | LocalDateTime | NO | 불변. updatedAt 없음 |

> `mainKing` 필드 및 `deckJson` 필드는 채택하지 않음.
> 용병 조합은 `UserDeckMember` (슬롯 12개)로 정규화해 저장한다.
> `attrXValue`·`totalResDown`은 `UserDeckService.calculateTotalStats()` 호출 시 계산·저장된다.
> 상세 엔티티 구조는 `docs/entity-model.ko.md` 16절 참고.

---

### 2.2 `Monster` — 몬스터 정보

계산의 기준이 되는 몬스터 고정 수치. 카탈로그 데이터로 관리자가 적재한다.

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `name` | String | NO | 몬스터 명칭 (예: `도사_허`, `흑의신녀`) |
| `resistance` | Integer | NO | 고유 저항값 |
| `attrYValue` | Integer | NO | 고유 속성값 (계산기 공식의 y) |

---

### 2.3 `HuntLog` — 사냥 기록

유저가 직접 입력한 실제 사냥 성능 데이터. 추천 알고리즘의 원천 데이터.

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `userId` | Long (FK → User) | NO | |
| `deckId` | Long (FK → UserDeck) | NO | 사냥 당시 사용한 덱 스냅샷 |
| `monsterId` | Long (FK → Monster) | NO | 대상 몬스터 |
| `clearTime` | Float | NO | 평균 클리어 타임 (초 단위) |
| `createdAt` | LocalDateTime | NO | |

---

### 2.4 `RecommendationCache` — 추천 결과 캐시

데이터가 충분히 쌓였을 때 연산된 추천 경로를 저장한다.
몬스터 + 클리어 타임 구간 단위로 집계하여 전역 캐시로 관리한다.

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `monsterId` | Long (FK → Monster) | NO | 목표 몬스터 |
| `currentTimeRange` | String | NO | 현재 클리어 타임 구간 (예: `12~14초`) |
| `nextBestItemId` | Long (FK → Item) | NO | 가장 효율적인 다음 추천 아이템 |
| `expectedReduction` | Float | NO | 예상 단축 시간 (초) |
| `updatedAt` | LocalDateTime | NO | 마지막 집계 시각 |

---

## 3. 비즈니스 가치 및 활용

| 활용 | 설명 |
|------|------|
| 거래소 연동 | "이 아이템을 사면 사냥 시간이 2초 단축됩니다" 문구와 함께 즉시 구매 버튼 배치 |
| 유저 이탈 방지 | 자신의 성장을 수치·그래프로 확인하게 하여 지속적인 플레이 동기 부여 |
| 데이터 해자 | 유저가 직접 입력한 실제 사냥 시간 데이터 — 타 커뮤니티가 보유하지 못한 유일한 자산 |
