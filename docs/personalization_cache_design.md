# 개인화 서비스 & 캐싱 설계 — 용병 덱 기반 추천·캐시 전략

> 작성일: 2026-04-21 / 최종 수정: 2026-04-22
> 대상 범위: 유저 덱(용병 조합) 기반 개인화 추천, 캐싱 전략, UserDeck 엔티티 설계
> 기준 문서: `docs/entity-model.ko.md`, `docs/calculator.md`, `docs/Mercenary-characteristic-crawling.md`

---

## 1. 목표

- 유저의 **용병 조합(덱)**을 기반으로 개인화된 추천 제공
- **계산 최소화 + 캐싱 활용**으로 조회 성능 확보
- 추후 확장 가능한 구조 설계

---

## 2. 개인화 전략

### 2.1 유저 속성 기반 개인화

유저가 선택한 사천왕 용병의 속성을 집계하여 `mainAttribute`(주요 속성)를 자동 판단한다.

**예시:** 불 속성 3개 + 물 속성 1개 → `mainAttribute = 불`

**활용:**
- 속성 기반 아이템 추천
- 메인 페이지 커스터마이징

---

### 2.2 조합 기반 개인화

유저가 사용하는 주인공 캐릭터 + 용병 11마리, 총 12마리 조합을 기준으로 추천을 제공한다.

---

## 3. 엔티티 설계

> ✅ 3.2~3.5 엔티티는 구현 완료 (2026-04-22). 상세 설계는 `docs/entity-model.ko.md` 16절 참고.

### 3.1 `User` — 주요 속성 필드 추가

기존 `User` 엔티티에 `mainAttribute` 컬럼을 추가한다.

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `mainAttribute` | Enum | YES | 유저 주요 속성. 덱 변경 시 재계산 |

> ❌ 미구현. 덱 API 구현 시 함께 추가 예정.

---

### 3.2 `UserDeck` — 유저 덱 ✅

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `userId` | Long (FK → User) | NO | |
| `isActive` | boolean | NO | 유저당 최대 1개 true |
| `attrXValue` | Integer | YES | 저장 시점 속성값 합산 캐시 |
| `totalResDown` | Integer | YES | 저장 시점 저항깎 합산 캐시 |
| `createdAt` | LocalDateTime | NO | 불변. updatedAt 없음 |

> 덱은 불변 스냅샷. 수정 시 새 행 생성 후 isActive 전환.

---

### 3.3 `UserDeckMember` — 덱 구성원 ✅

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `deckId` | Long (FK → UserDeck) | NO | |
| `mercenaryId` | Long (FK → Mercenary) | NO | 주인공(PROTAGONIST)도 Mercenary로 통일 |
| `slotIndex` | Integer | NO | 0~11. 덱 안에서 UNIQUE |

---

### 3.4 `UserDeckMemberEquip` — 슬롯 용병 장비 ✅

카테고리별로 사용 필드가 다르다.

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `deckMemberId` | FK → UserDeckMember (UNIQUE) | NO | 1:1 |
| `equipmentSetId` | FK → EquipmentSet | YES | LEGENDARY_GENERAL 전용 |
| `enhanceLevel` | Integer | YES | 전설장수 세트 강화 수치 |
| `setPieceCount` | Integer | YES | 전설장수 세트 피스 수 |
| `hasAffinity` | boolean | NO | 전설장수 인연 여부 |
| `equipmentItemId` | FK → EquipmentItem | YES | MYEONG_KING / MYEONG_KING_AWAKENING 전용 |

---

### 3.5 `UserDeckMemberCharacteristic` — 선택된 특성 ✅

전설장수 패시브도 `MercenaryCharacteristic`으로 통합 (별도 Passive 엔티티 없음).

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | Long (PK) | NO | |
| `deckMemberId` | FK → UserDeckMember | NO | |
| `characteristicId` | FK → MercenaryCharacteristic | NO | |
| `selectedLevel` | Integer | NO | 투자 레벨. 각성 사천왕·명왕·주인공 1~5, 전설장수 1~10 |

- `UNIQUE(deckMemberId, characteristicId)`

---

### 3.6 `UserRecommendation` — 추천 결과 캐시 (미구현)

| 필드 | 타입 | Null | 설명 |
|------|------|------|------|
| `userId` | Long (FK → User) | NO | |
| `itemId` | Long | NO | 추천 아이템 ID |
| `score` | Double | NO | 추천 점수 (높을수록 우선) |
| `type` | Enum | NO | 추천 유형 (예: `ATTRIBUTE_MATCH`, `DECK_SYNERGY`) |
| `updatedAt` | LocalDateTime | NO | 마지막 계산 시각 |

---

## 4. 추천 시스템 구조

### 4.1 잘못된 방식 — 실시간 계산

```
요청 → 유저 조합 불러옴 → 아이템 전체 비교 → 계산 → 응답
```

아이템 수가 많아질수록 응답 시간이 증가한다. 요청마다 동일한 계산을 반복하므로 비효율적이다.

---

### 4.2 권장 방식 — 사전 계산 + 캐싱

```
유저 조합 변경
   ↓
추천 결과 계산 (배치 또는 이벤트 기반)
   ↓
UserRecommendation 테이블 또는 캐시에 저장
   ↓
조회 시 바로 반환
```

계산과 조회를 분리하여 조회 시점에 추가 연산 없이 결과를 반환한다.

---

## 5. 캐싱 전략

### 5.1 핵심 원칙

- 캐시는 조회 성능 최적화 도구. DB는 항상 원본 데이터
- 캐시 삭제·갱신은 원본 데이터 변경 시점에 트리거

---

### 5.2 Spring Cache 주요 애노테이션

| 애노테이션 | 동작 |
|-----------|------|
| `@Cacheable` | 첫 요청 시 DB 조회 후 캐시 저장. 이후 요청은 캐시 반환 |
| `@CachePut` | 매번 실행 후 캐시 갱신 (캐시 우회 없이 항상 실행) |
| `@CacheEvict` | 캐시 삭제. 데이터 변경 시 호출 |

---

### 5.3 캐시 동작 흐름

```
첫 요청  → DB 조회 → 캐시 저장 → 응답
다음 요청 → 캐시 반환 → 응답
```

---

## 6. 캐시 갱신 전략

| 갱신 트리거 | 처리 |
|-----------|------|
| 유저 덱 조합 변경 | `UserRecommendation` 재계산 + 캐시 갱신 (`@CacheEvict` → 재계산) |
| 월별 가격 변동 (크롤링 Batch 완료 후) | 가성비 점수 변경 → 추천 점수 재계산 배치 실행 |

---

## 7. Fallback 전략

추천 계산에 필요한 데이터가 부족할 경우(신규 유저, 덱 미설정) 대체 결과를 제공한다.

| 상황 | 대체 방식 |
|------|----------|
| 덱 미설정 유저 | 전체 평균 가성비 기준 인기 아이템 상위 N개 노출 |
| `mainAttribute` 미결정 | 속성 무관 전체 추천 목록 사용 |
| 추천 계산 오류 | 빈 리스트 반환 (에러 노출 금지) |

---

## 8. 서버 캐시 구조

| 구조 | 평가 |
|------|------|
| 단일 서버 로컬 캐시 (Caffeine 등) | 서버 재시작 시 캐시 소실. 멀티 서버 환경에서 캐시 불일치 발생 |
| **Redis 기반 공유 캐시** (권장) | 서버 인스턴스 간 캐시 공유. 재시작 후에도 캐시 유지 가능 |

> MVP 단계에서는 단일 서버를 가정하므로 Spring Cache(Caffeine) + 인메모리로 시작해도 무방하다.
> EC2 멀티 인스턴스 전환 시 Redis로 교체한다.

---

## 9. 핵심 원칙

- **계산과 조회 분리**: 덱 변경 시 계산, 조회 시 결과만 반환
- **캐시는 보조 수단**: DB가 원본. 캐시 불일치 시 DB 기준으로 복구
- **구조는 유연하게**: 추천 유형(`type`)과 점수(`score`)로 다양한 추천 로직 확장 가능
