# DPS 계산기 프론트엔드 구현 가이드

## 개요

DPS 계산기는 **저장된 덱**과 **몬스터**를 선택해 멤버별 DPS 및 데미지 비중을 계산한다.  
계산 결과는 서버에 저장되지 않으며, 요청마다 실시간으로 계산된다.  
**로그인 필요** (덱 데이터 접근 권한).

---

## 전체 흐름

```
1. 내 덱 목록 조회          GET /api/decks
        ↓
2. 덱 선택 → 덱 상세 조회   GET /api/decks/{deckId}
        ↓  (멤버 목록 + 슬롯 정보 포함)
3. 몬스터 선택              GET /api/monsters/search?q=...
        ↓
4. 멤버별 입력값 설정        (레벨 250/260, 보너스 스탯 배분)
        ↓
5. DPS 계산 요청            POST /api/calculator/dps
        ↓
6. 결과 화면 렌더링
```

---

## Step 1 — 덱 목록 조회

```
GET /api/decks
Authorization: 쿠키 (로그인 필요)
```

**응답 예시**
```json
[
  { "id": 1, "name": "파밍덱", "active": true },
  { "id": 2, "name": "레이드덱", "active": false }
]
```

유저가 덱을 선택하면 `deckId`를 기억해둔다.

---

## Step 2 — 덱 상세 조회

```
GET /api/decks/{deckId}
```

**응답 주요 필드**
```json
{
  "id": 1,
  "name": "파밍덱",
  "members": [
    {
      "id": 101,
      "mercenaryId": 5,
      "mercenaryName": "지국천왕",
      "mercenaryImageUrl": "https://...",
      "slots": [...]
    }
  ]
}
```

`members[].id` → `UserDeckMember.id` → DPS 요청의 `memberId`로 사용된다.

---

## Step 3 — 몬스터 선택

### 자동완성 검색
```
GET /api/monsters/search?q=드래곤&limit=10
```

```json
[
  { "id": 12, "name": "드래곤", "element": "FIRE" }
]
```

### 몬스터 단건 조회 (선택 후 저항값 미리보기용)
```
GET /api/monsters/{id}
```

```json
{
  "id": 12,
  "name": "드래곤",
  "elementValue": 40,
  "hittingResistance": 300,
  "magicResistance": 200
}
```

> `resistanceType`을 `HITTING`으로 보낼지 `MAGIC`으로 보낼지 유저가 선택하게 한다.

---

## Step 4 — 멤버별 입력값 설정 UI

각 멤버에 대해 다음을 UI에서 입력받는다.

| 필드 | 타입 | 설명 | 기본값 |
|------|------|------|--------|
| `memberId` | Long | 덱 멤버 ID (Step 2에서 확보) | — |
| `level` | int | 용병 레벨 | `250` |
| `bonusTarget` | enum | 보너스 스탯 투자 대상 | `MAIN_STAT` |
| `bonusAmount` | int | 보너스 스탯 총량 | `0` |

**`bonusTarget` 선택지**
- `MAIN_STAT` — 해당 용병의 주스탯(힘/민첩/생명/지력 중 계수 최대값)에 투자
- `VITALITY` — 생명력에 투자

**`bonusAmount` 추천 선택지** (직접 입력도 가능)
- 300, 500, 700, 900, 1000

**`level` 선택지**
- 250, 260

---

## Step 5 — DPS 계산 요청

```
POST /api/calculator/dps
Content-Type: application/json
```

**요청 바디**
```json
{
  "deckId": 1,
  "monsterId": 12,
  "resistanceType": "HITTING",
  "memberInputs": [
    {
      "memberId": 101,
      "level": 250,
      "bonusTarget": "MAIN_STAT",
      "bonusAmount": 500
    },
    {
      "memberId": 102,
      "level": 260,
      "bonusTarget": "VITALITY",
      "bonusAmount": 300
    }
  ]
}
```

> `memberInputs`를 생략하거나 특정 멤버를 누락하면 해당 멤버는 **레벨 250, 보너스 없음**으로 처리된다.

---

## Step 6 — 응답 구조 및 렌더링

### 응답 전체 구조

```json
{
  "monsterId": 12,
  "monsterName": "드래곤",

  "totalResistPierce": 80,
  "resistAfterDebuff": 220,
  "resistPassRate": 17.8,

  "totalElementPierce": 10,
  "effectiveMonsterElement": 30,

  "rawTotalDps": 1500000,
  "adjustTotalDps": 1350000,
  "totalDps": 240300,

  "memberResults": [
    {
      "memberId": 101,
      "mercenaryId": 5,
      "mercenaryName": "지국천왕",
      "elementValue": 20,
      "elementBonus": 15.0,
      "rawDps": 900000,
      "elementAdjustedDps": 1035000,
      "adjustedDps": 184230,
      "damageShare": 76.67,
      "skillResults": [
        { "skillName": "겁화", "dps": 900000, "calculated": true },
        { "skillName": "화상", "dps": 0, "calculated": false }
      ]
    }
  ]
}
```

---

### 응답 필드 설명

#### 상단 요약

| 필드 | 설명 | 표시 예 |
|------|------|---------|
| `totalResistPierce` | 전 멤버 저항깎 합산 | `80` |
| `resistAfterDebuff` | 디버프 후 몬스터 저항 | `220` |
| `resistPassRate` | 저항 통과율 (%) | `17.8%` |
| `totalElementPierce` | 전 멤버 속성깎 합산 | `10` |
| `effectiveMonsterElement` | 속성깎 적용 후 몬스터 속성값 | `30` |
| `rawTotalDps` | 보정 없는 순수 DPS 합산 | `1,500,000` |
| `adjustTotalDps` | 속성 보정만 적용한 DPS 합산 | `1,350,000` |
| `totalDps` | 속성 + 저항 통과율 최종 DPS | `240,300` |

#### 멤버별 결과

| 필드 | 설명 |
|------|------|
| `mercenaryName` | 용병 이름 |
| `elementValue` | 해당 용병의 속성값 |
| `elementBonus` | 속성 보정 (%) — 음수 가능 |
| `rawDps` | 계수 기반 순수 DPS |
| `elementAdjustedDps` | 속성 보정 적용 DPS |
| `adjustedDps` | 속성 + 저항 통과율 최종 DPS |
| `damageShare` | `adjustTotalDps` 기준 데미지 비중 (%) |

#### 스킬 결과

| 필드 | 설명 |
|------|------|
| `skillName` | 스킬명 |
| `dps` | 해당 스킬의 DPS |
| `calculated` | `false`이면 측정값 미입력 → 계산 제외된 스킬 |

> `calculated: false`인 스킬은 회색 처리 또는 "측정 대기" 뱃지를 표시한다.

---

## UI 구성 권장 레이아웃

```
┌─────────────────────────────────────────────┐
│  덱 선택 드롭다운        몬스터 검색 자동완성  │
│  저항 종류  [타격 저항 ▼]                     │
├─────────────────────────────────────────────┤
│  멤버별 입력 카드                             │
│  ┌──────────────┐  ┌──────────────┐          │
│  │ 지국천왕      │  │ 광목천왕      │          │
│  │ 레벨 [250▼]  │  │ 레벨 [250▼]  │          │
│  │ 투자 [주스탯▼]│  │ 투자 [주스탯▼]│          │
│  │ 보너스 [500▼] │  │ 보너스 [500▼] │         │
│  └──────────────┘  └──────────────┘          │
│                              [DPS 계산]       │
├─────────────────────────────────────────────┤
│  결과 요약                                    │
│  저항깎 80 / 저항 통과율 17.8%                │
│  Raw DPS: 1,500,000                          │
│  속성 보정 DPS: 1,350,000                    │
│  최종 DPS: 240,300                           │
├─────────────────────────────────────────────┤
│  멤버별 DPS 바 차트 (damageShare 기준)         │
│  지국천왕 ████████████████████ 76.7%          │
│  광목천왕 ██████ 23.3%                        │
├─────────────────────────────────────────────┤
│  멤버 상세 접기/펼치기                         │
│  지국천왕                                    │
│    속성값: 20  속성보정: +15%                  │
│    겁화   900,000 DPS ✅                      │
│    화상   — (측정 대기) ⚠️                    │
└─────────────────────────────────────────────┘
```

---

## 주의사항

1. **덱이 없는 유저**: 덱 생성 화면으로 유도한다.
2. **멤버가 없는 덱**: `POST /api/calculator/dps` 호출 시 400 반환 — "덱에 용병이 없습니다." 메시지 표시.
3. **`calculated: false` 스킬**: 스킬 계수 미측정 상태. DPS 0으로 집계되므로 그 용병의 DPS가 과소 산출됨을 UI에 안내.
4. **`elementBonus` 음수**: 속성이 불리한 경우 (예: -20%). 붉은 색으로 표시.
5. **`resistPassRate`가 낮은 경우**: 몬스터 저항이 높아 저항 통과율이 낮음. 저항깎 증가 필요 안내 가능.
