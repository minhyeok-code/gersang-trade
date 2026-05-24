# 용병 특성(Characteristic) 크롤링 전략 및 엔티티 구조

## 1. 데이터 소스

### 출처: 거니버스 용병 상세 페이지

```
https://gerniverse.app/mercenary/{용병명}
```

특성 데이터는 페이지 HTML 내 **RSC payload** (`self.__next_f.push`) 안에 포함되어 있다.
Playwright 없이 BeautifulSoup으로 파싱 가능하다 (SSR 렌더링 확인).

---

## 2. 원본 데이터 구조

거니버스 RSC payload에서 확인된 특성 데이터 예시 (`각성 광목천왕`):

```json
"characteristics": [
  {
    "id": "8062a403-7c8a-7397-6626-0cdaa4a9559e",
    "key": "gakGwangmok-rhkdvnd",
    "name": "광풍",
    "level": [
      {"label": "풍극진멸 데미지", "amount": ["20%", "40%", "70%", "100%", "150%"]}
    ],
    "point": 1,
    "description": "풍극진멸의 데미지가 증가합니다.",
    "mercenary_key": "gakGwangmok",
    "required_characteristic_key": null
  },
  {
    "id": "4f17be7c-56f4-ce33-80ce-e943b519dbd0",
    "key": "dusvk",
    "name": "연파",
    "level": [
      {"label": "생명력 증가",    "amount": ["2%", "5%", "10%", "15%", "30%"]},
      {"label": "고정 추가 피해", "amount": ["3%", "8%", "15%", "25%", "50%"]}
    ],
    "point": 2,
    "description": "전투 시 생명력이 증가하고...",
    "mercenary_key": "gakGwangmok",
    "required_characteristic_key": "gakGwangmok-rhkdvnd"
  },
  {
    "id": "8289283c-bd9c-9f1d-617a-848ac1674e4d",
    "key": "gakGwangmok-cndehf",
    "name": "충돌",
    "level": [
      {"label": "타격저항력", "amount": ["1%", "3%", "6%", "10%", "15%"]}
    ],
    "point": 1,
    "description": "풍극진멸에 피해를 입은 대상은 15초간 타격저항력이 감소합니다.",
    "mercenary_key": "gakGwangmok",
    "required_characteristic_key": null
  },
  {
    "id": "1f0e5288-42e3-324d-a3db-ef0328de83fa",
    "key": "tlsqh",
    "name": "신보",
    "level": [
      {"label": "기본 데미지", "amount": ["2%",  "5%",  "10%", "20%",  "40%"]},
      {"label": "추가 데미지", "amount": ["10%", "25%", "50%", "150%", "200%"]}
    ],
    "point": 2,
    "description": "풍극진멸로 피해를 입은 대상은...",
    "mercenary_key": "gakGwangmok",
    "required_characteristic_key": "gakGwangmok-cndehf"
  },
  {
    "id": "1b109bc1-9ed4-410e-8c8b-c61ec8acac48",
    "key": "rkrtjdrhkdahrcjsdhkd",
    "name": "각성 광목천왕",
    "level": null,
    "point": null,
    "description": "각성 광목천왕의 속성값이 20 영구적으로 증가합니다.",
    "mercenary_key": "gakGwangmok",
    "required_characteristic_key": null
  }
]
```

### 주요 관찰 사항

- 한 용병에 특성이 여러 개 → **Mercenary : MercenaryCharacteristic = 1:N**
- 한 특성에 label이 여러 개일 수 있음 (예: 연파 - 생명력 증가 / 고정 추가 피해)
- 각 label마다 레벨별 수치 배열 → **MercenaryCharacteristic : MercenaryCharacteristicLevel = 1:N**
- `required_characteristic_key`: 선행 특성 키 (특성 트리 구조, null이면 선행 특성 없음)
- `level: null`, `point: null`: 각성 특성은 레벨 구조 없음
- 최대 레벨과 특성 수는 용병 종류에 따라 다름 → 엔티티에 하드코딩하지 않고 동적으로 처리

### 특성 트리 구조 (각성 광목천왕 예시)

```
광풍 (point: 1)
  └── 연파 (point: 2, required: 광풍)

충돌 (point: 1)
  └── 신보 (point: 2, required: 충돌)

각성 광목천왕 (각성 특성, level: null)
```

---

## 3. 파싱 방법

RSC payload는 HTML 내 `<script>` 태그 안에 아래 형태로 존재한다:

```javascript
self.__next_f.push([1, "...JSON 데이터..."])
```

### 파싱 예시 코드 (Python)

```python
import re
import json
from bs4 import BeautifulSoup

def extract_characteristics(html: str) -> list[dict]:
    soup = BeautifulSoup(html, "html.parser")
    
    for script in soup.find_all("script"):
        if not script.string:
            continue
        if "characteristics" not in script.string:
            continue
        
        # RSC payload에서 JSON 추출
        matches = re.findall(r'self\.__next_f\.push\(\[.*?\],"(.*?)"\]\)', script.string)
        for match in matches:
            try:
                data = json.loads(match)
                if "characteristics" in data:
                    return data["characteristics"]
            except (json.JSONDecodeError, TypeError):
                continue
    
    return []
```

---

## 4. 엔티티 모델

### 전체 구조

```
Mercenary (1)
  └── MercenaryCharacteristic (N)   ← 특성 하나당 행 하나
        └── MercenaryCharacteristicLevel (N)  ← label + 레벨별 수치
```

---

### MercenaryCharacteristic (용병 특성)

```
MercenaryCharacteristic
- id                        : Long (Auto Increment)
- mercenary_id              : FK → Mercenary
- key                       : String (거니버스 특성 키, unique)
- name                      : String (특성명)
- point                     : Integer (nullable, 각성 특성은 null)
- description               : String
- required_characteristic_key : String (nullable, 선행 특성 키)
```

> `required_characteristic_key`는 같은 테이블의 `key`를 참조하는 자기 참조 관계이다.
> FK 대신 String으로 두어 순환 참조 문제를 피한다.

---

### MercenaryCharacteristicLevel (특성 레벨별 수치)

```
MercenaryCharacteristicLevel
- id                      : Long (Auto Increment)
- characteristic_id       : FK → MercenaryCharacteristic
- label                   : String (수치 항목명, 예: "풍극진멸 데미지")
- level                   : Integer (amount 배열 인덱스 기반, 제약 없음)
- amount                  : String (원본 수치값, 예: "20%", "150%", "500")
- amount_value            : Float (nullable) — 크롤링 시 파싱된 Float. "20%" → 20.0. 파싱 불가 시 null
- stat_type               : StatType Enum (nullable) — label 자동 매핑. 미매핑은 null(관리자 수동 보정)
```

> `level`은 `amount` 배열의 인덱스(1부터 시작)로 결정된다. 최대 레벨은 용병 종류에 따라 다르므로 제약을 걸지 않는다.
> `level: null`인 각성 특성은 MercenaryCharacteristicLevel 행을 생성하지 않는다.
> `stat_type` 자동 매핑 규칙: label에 "저항깎"/"저항감소" 포함 → `RESIST_PIERCE`, "속성값" 포함 → `ELEMENT_VALUE`, 그 외 → `null`.
> `null`인 stat_type은 `UserDeckService.calculateTotalStats()`에서 skip 처리된다.

---

### 실제 행 예시 (각성 광목천왕 - 광풍)

**MercenaryCharacteristic**

| id | mercenary_id | key | name | point | required_characteristic_key |
|----|-------------|-----|------|-------|----------------------------|
| 1 | 1 | gakGwangmok-rhkdvnd | 광풍 | 1 | null |
| 2 | 1 | dusvk | 연파 | 2 | gakGwangmok-rhkdvnd |
| 3 | 1 | gakGwangmok-cndehf | 충돌 | 1 | null |
| 4 | 1 | tlsqh | 신보 | 2 | gakGwangmok-cndehf |
| 5 | 1 | rkrtjdrhkdahrcjsdhkd | 각성 광목천왕 | null | null |

**MercenaryCharacteristicLevel**

| id | characteristic_id | label | level | amount |
|----|------------------|-------|-------|--------|
| 1 | 1 | 풍극진멸 데미지 | 1 | 20% |
| 2 | 1 | 풍극진멸 데미지 | 2 | 40% |
| 3 | 1 | 풍극진멸 데미지 | 3 | 70% |
| 4 | 1 | 풍극진멸 데미지 | 4 | 100% |
| 5 | 1 | 풍극진멸 데미지 | 5 | 150% |
| 6 | 2 | 생명력 증가 | 1 | 2% |
| 7 | 2 | 생명력 증가 | 2 | 5% |
| ... | ... | ... | ... | ... |
| 11 | 2 | 고정 추가 피해 | 1 | 3% |
| 12 | 2 | 고정 추가 피해 | 2 | 8% |
| ... | ... | ... | ... | ... |

---

## 5. 비즈니스 규칙 (검증용)

특성 구조는 용병 종류에 따라 다르다. 엔티티에 하드코딩하지 않고, 크롤링 후 검증 단계에서 아래 규칙을 적용한다.

| 용병 종류 | 특성 수 | 최대 레벨 |
|---------|--------|---------|
| 각성 사천왕 / 각성 명왕 / 주인공 2차전직 | 4개 (+ 각성 특성 1개) | 5 |
| 전설장수 등 나머지 | 2개 | 10 |

> 크롤링 후 위 규칙을 벗어나는 데이터(예: 전설장수인데 특성이 4개)는 경고 로그를 남기고 수동 확인한다.
> 거니버스 데이터 업데이트로 규칙이 바뀔 수 있으므로 엔티티 자체는 유연하게 유지한다.

---

## 6. 크롤링 주의사항

- RSC payload 구조는 Next.js 버전 업데이트 시 변경될 수 있으므로 파싱 실패 시 fallback 처리 필요
- `level: null`인 각성 특성은 `MercenaryCharacteristicLevel` 행 생성을 건너뛴다
- `amount` 값은 퍼센트 문자열(`"20%"`) 그대로 저장한다 (숫자 변환 시 단위 정보 손실)
- 특성 `key`가 용병에 종속되지 않는 경우도 있음 (예: `dusvk` - 연파는 `mercenary_key: "gakGwangmok"`이지만 다른 용병과 공유될 수 있음) → `key` unique 제약 전에 실제 데이터 확인 필요