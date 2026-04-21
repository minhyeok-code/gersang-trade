
각성 광목천왕 
{
"id": "230b19a8-...",
"key": "gakGwangmok",
"name": "각성 광목천왕",
"category": "four-heavenly-kings_awakening",
"nature": "air",
"location": "수미산",
"npc": "jeseokcheon-saja",
"is_trade": false,
"statistics": {
"strength": 500, "vitality": 1000, "dexterity": 50,
"intellect": 300, "defense": 500, "hit_rate": 12,
"sight": 3, "nature_value": 20,
"hitting_resistance": 100, "magic_resistance": 100,
"min_power": 250, "max_power": 250, "critical_chance": 0
},
"requirements": { "credit": 130 },
"material": [{
"item": {
"rkrtjdtjr": 5,          // 각성석
"gladmlrmsdnjs": 10,     // 힘의근원
"tlstnqn-qorgh": 1,      // 신수부<백호>
"dbanf-qjqfbsvkvus": 8,  // [유물] 법륜 파편
"rldjrdmltjvks-vnd": 20, // 기억의서판(風)
"wjdrldmlrntmf-vnd": 30, // 정기의구슬(風)
"antlsdmlrhkdahrcjsdhkd": 1 // 무신의 광목천왕
},
"limit": { "level": "gwangmok_260", "credit": 130 },
"mercenary": { "gwangmok": 260 }
}],
"awakening_characteristic": ["rkrtjdrhkdahrcjsdhkd"],
"recommended_builds": [
{
"type": "보스사냥", "build_code": "5025",
"characteristics": [
{"key": "gakGwangmok-rhkdvnd", "points": 5}, // 광풍
{"key": "dusvk", "points": 0},               // 연파
{"key": "gakGwangmok-cndehf", "points": 2},  // 충돌
{"key": "tlsqh", "points": 5}                // 신보
]
},
{ "type": "상재사냥", "build_code": "2055", "..." }
]
}

용병 관련 필요한 데이터
: 용병명, 제작 재료+수량, 재료단가(내가 구현할 서비스 내의 데이터에서 쿼리를 통해 시세에서 계산된 후 가져와야함), 스킬목록, 특성정보, 전용장비, 각성특성, 진화체인 이 내가 가져와야하는 정보인데 엔티티를 고쳐야함..
전설장수,

SEO 확장성 (현재는 우선순위를 뒤로 구현 x)


# 용병 데이터 크롤링 전략

## 1. 크롤링 소스

### 주 소스: 거니버스 (gerniverse.app)

용병 마스터 데이터(스탯, 스킬, 특성, 전용 장비, 진화 체인 등)는 거니버스에서 크롤링한다.

- **robots.txt**: Allow (크롤링 허용 확인)
- **렌더링 방식**: Next.js SSR (서버사이드 렌더링)

### 보조 소스: 거타 (geota.co.kr)

용병 제작 재료의 **서버별 시세**는 거타에서 크롤링한다.

- **robots.txt**: Allow
- **렌더링 방식**: 정적 HTML
- **대상 페이지**: `/gersang/yukeuijeon`

---

## 2. 용병 목록 수집 전략

### 2-1. 목록 URL

```
https://gerniverse.app/mercenary
```

- 필터 파라미터 없이 접근해도 **SSR로 전체 목록이 렌더링**됨
- 하드코딩된 필터 URL 불필요
- ID 순회 방식 불필요

### 2-2. 목록 파싱 방법

`/mercenary` 응답 HTML의 `<script type="application/ld+json">` 태그 중
`@type: "ItemList"`인 블록에서 추출한다.

```json
{
  "@context": "https://schema.org",
  "@type": "ItemList",
  "name": "용병 도감",
  "numberOfItems": 292,
  "itemListElement": [
    {
      "@type": "ListItem",
      "position": 1,
      "name": "조철희",
      "url": "https://www.gerniverse.app/mercenary/%EC%A1%B0%EC%B2%A0%ED%9D%AC"
    },
    ...
  ]
}
```

**추출 필드:**
- `name`: 용병 풀네임
- `url`: 상세 페이지 URL (URL 인코딩된 한글)

**총 용병 수:** 292개 (`numberOfItems` 기준)
- `is_coming_soon: true`인 출시 예정 용병은 목록에서 제외됨 → 크롤링 대상 아님

> **주의:** `<meta name="keywords">` 태그는 SEO 목적으로 약칭·카테고리명·중복이 포함되어 있어 크롤링 소스로 부적합하다.

### 2-3. 파싱 예시 코드 (Python)

```python
import json
from bs4 import BeautifulSoup

def extract_mercenary_list(html: str) -> list[dict]:
    soup = BeautifulSoup(html, "html.parser")
    for tag in soup.find_all("script", type="application/ld+json"):
        try:
            data = json.loads(tag.string)
            if data.get("@type") == "ItemList":
                return [
                    {"name": item["name"], "url": item["url"]}
                    for item in data["itemListElement"]
                ]
        except (json.JSONDecodeError, KeyError):
            continue
    return []
```

---

## 3. 용병 상세 데이터 수집 전략

### 3-1. 상세 URL 패턴

```
https://gerniverse.app/mercenary/{용병명}
```

예시:
- `https://gerniverse.app/mercenary/각성%20사천왕`
- `https://gerniverse.app/mercenary/조철희`

목록에서 추출한 `url` 필드를 그대로 사용한다.

### 3-2. 수집 대상 데이터

거니버스 용병 상세 페이지에서 아래 정보를 수집한다.

| 항목 | 설명 |
|------|------|
| 용병명 | 풀네임 (예: `각성 다문천왕`) |
| 카테고리 | 주인공 / 사천왕 / 명왕 / 전설장수 / 신수 / 흉수 등 |
| 국가 | 조선 / 중국 / 일본 / 대만 / 인도 |
| 속성 | fire / water / thunder / air / earth / `-` |
| 기본 스탯 | 힘 / 민첩 / 생명력 / 지력 / 시야 / 방어 등 |
| 속성값 | `nature_value` |
| 타격/마법 저항 | `hitting_resistance`, `magic_resistance` |
| 스킬 목록 | 스킬명, 액티브/패시브 여부, 설명 |
| 특성 목록 | 특성명, 레벨별 효과 |
| 전용 장비 | 장비명, 강화 단계별 스탯 |
| 전직 재료 | 재료 용병명, 재료 아이템명 및 수량 |
| 진화 체인 | 하위 재료 → 상위 용병 관계 |
| 사냥터 추천 | 추천 사냥 몬스터 (인기 기반) |
| is_coming_soon | 출시 예정 여부 |

### 3-3. MVP 범위 제한

전투 스탯 계산기 MVP에서 필요한 필드는 아래 3가지로 제한한다.

| statKey | 설명 |
|---------|------|
| `ELEMENT_VALUE` | 속성값 |
| `ELEMENT_PIERCE` | 속성 관통 |
| `RESIST_PIERCE` | 저항 관통 |

나머지 스탯(공격력, 크리율 등)은 Phase 2에서 확장한다.

---

## 4. 용병 관련 엔티티 모델 (초안)

### Mercenary (용병 마스터)

```
Mercenary
- id: Long (Auto Increment, PK)
- name: String (풀네임, unique)
- key: String (거니버스 내부 키, unique, nullable — 상세 파싱 후 채워짐)
- category: Enum (주인공, 사천왕, 각성사천왕, 명왕, 각성명왕, 전설장수, 신수, 흉수, 각성흉수, 고용몬스터, 전직몬스터, 정령몬스터, 각성장수, 개조장수, 2차장수, 1차장수, 용병)
- nation: Enum (조선, 중국, 일본, 대만, 인도, -)
- nature: Enum (fire, water, thunder, air, earth, -)
- nature_value: Int (nullable — 무속성 용병은 null)
- is_coming_soon: Boolean
- crawled_at: DateTime (nullable — null이면 상세 크롤링 미완료)
```

### MercenaryStat (용병 스탯)

```
MercenaryStat
- id: Long (Auto Increment, PK)
- mercenary_id: FK → Mercenary
- stat_key: Enum (STRENGTH, VITALITY, DEXTERITY, INTELLECT, DEFENSE, SIGHT, HIT_RATE, CRITICAL_CHANCE, MIN_POWER, MAX_POWER, MAGIC_RESISTANCE, HITTING_RESISTANCE, ELEMENT_VALUE, ELEMENT_PIERCE, RESIST_PIERCE)
- stat_value: Int
```

### MercenaryMaterial (전직 재료)

```
MercenaryMaterial
- id: Long (Auto Increment, PK)
- result_mercenary_id: FK → Mercenary (완성 용병)
- material_mercenary_id: FK → Mercenary (nullable, 재료 용병)
- material_item_key: String (nullable, 재료 아이템 키)
- quantity: Int
- required_level: Int (nullable)
- required_credit: Int (nullable)
```

---

## 5. 크롤링 주의사항

- 거니버스는 개인 운영 사이트이므로 **요청 간격을 충분히 두어야 한다** (권장: 1~2초)
- `dpl` 파라미터가 URL에 붙는 경우가 있으나 크롤링 시 무시해도 됨
- Next.js RSC payload (`self.__next_f.push`) 안에도 용병 데이터가 있으나, JSON-LD 파싱이 더 안정적임
- 거니버스 업데이트 시 새 용병이 추가될 수 있으므로 **주기적 재크롤링** 필요

---

## 6. 크롤링 실행 순서

```
1. GET /mercenary
   → JSON-LD ItemList 파싱
   → 용병명 + 상세 URL 292개 추출

2. 각 용병 URL 순회 (1~2초 간격)
   → GET /mercenary/{용병명}
   → 상세 데이터 파싱
   → DB 저장 (Mercenary, MercenaryStat, MercenaryMaterial)

3. GET /gersang/yukeuijeon (거타)
   → 재료 아이템 서버별 시세 파싱
   → MaterialPriceHistory 저장
```

