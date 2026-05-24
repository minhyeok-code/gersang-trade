# 크롤링 HTML 선택자 검증

> 브라우저 직접 접속 및 DevTools 확인을 통해 검증한 결과.
> 구현 시 이 문서의 선택자를 그대로 사용할 것.

---

## 1. geota.co.kr 아이템 계산기

### 접근 방식 확정 — URL 파라미터

```
https://geota.co.kr/gersang/calculator/item?serverId={1~13}&keyword={아이템명}
```

검색창 form_input 방식은 React 이벤트 문제로 목록이 렌더링되지 않음.
URL 파라미터 `keyword`로 직접 접근하는 방식이 안정적.

### 아이템 목록 전체 수집 방법

전체 목록은 검색 없이 한번에 뜨지 않음.
geota 아이템 계산기는 `li.cursor-pointer` 선택자로 드롭다운 목록을 렌더링하는데,
이 목록은 검색어 입력 후에만 나타남.

**권장 방식**: 검색어 없이 전체 목록을 가져오려면
아이템 제작 계산기 대신 geota 다른 페이지를 확인하거나,
이전 대화에서 확인한 것처럼 검색창 `li` 드롭다운 목록 전체를 렌더링 후 파싱.

```
드롭다운 li 선택자: li.cursor-pointer
클래스 확인됨: "cursor-pointer p-2 hover:bg-blue-600 hover:text-white"
```

### 가격 데이터 구조 (keyword 접근 후)

```
최저가:
  선택자: span.whitespace-nowrap ("최저 비용 보기" 텍스트를 가진 span)
  다음 sibling span: class="block truncate" → "26,350,000냥" 형식

평균가:
  선택자: span.whitespace-nowrap ("평균 비용 보기" 텍스트를 가진 span)
  다음 sibling span: class="block truncate" → "29,534,272냥" 형식

숫자 추출: 문자열에서 숫자+콤마만 추출 후 "냥" 제거
  예: "1,490,000냥" → replaceAll(",", "").replace("냥", "") → 1490000
```

### 재료 행 구조

```
재료 1행 컨테이너:
  선택자: div[class*="flex"][class*="gap"][class*="rounded-lg"]
  조건: textContent에 "개" 포함 && "냥" 포함 && 길이 < 50

재료 컨테이너 내부 구조:
  ┌─ div.flex.flex-1 (재료명 + 수량 영역)
  │    ├─ img (재료 이미지, 선택적)
  │    └─ div (재료명 텍스트 + "N/N개" 수량)
  └─ div.flex.flex-shrink-0 (가격 영역)
       └─ span.block.truncate → "1,050,000냥" 형식

재료명 파싱:
  textContent에서 "N/N개" 패턴 앞 텍스트 = 재료명
  예: "검은색가루5/5개" → name="검은색가루", quantity=5

수량 파싱:
  정규식: /(\d+)\/\d+개/ → 첫 번째 숫자가 실제 필요 수량
```

### 제조 수수료 처리

```
"제조 수수료" 텍스트를 포함하는 행은 재료가 아니므로 파싱 시 skip
"육의전 최저 가격" 행도 skip
```

---

## 2. gerniverse.app 아이템 상세 페이지

### 접근 URL

```
https://gerniverse.app/item/{아이템명(한글 URL 인코딩)}
예: https://gerniverse.app/item/챠우인형
    https://gerniverse.app/item/각성석
```

### h1 태그 — 아이템명

```java
// Jsoup
String itemName = doc.select("h1").first().text();
// → "챠우인형"

// 확인된 클래스:
// "font-black text-3xl md:text-5xl text-slate-900 dark:text-slate-100 text-center"
```

### JSON-LD — 이미지 경로

```java
// script[type="application/ld+json"] 파싱
Element jsonLdEl = doc.select("script[type=application/ld+json]").first();
JsonNode root = mapper.readTree(jsonLdEl.data());
JsonNode imageArr = root.get("image");  // 배열 형태

String imageKey = imageArr.get(0).asText();
// 장비 예: "item/weapon/doll/cidndlsgud"
// 재료 예: "item/cash/rkrtjdtjr"
// 용병 예: "thumbnail/myeong-kings/awakening/gakGoondari"
```

**주의사항**:
- `image` 필드는 항상 배열(`[]`)로 감싸져 있음
- 재료 아이템도 image 필드 존재함 (null 케이스 미확인 → 방어 코드 필요)
- JSON-LD에 카테고리 정보 없음 → HTML에서 별도 파싱 필요

### 카테고리 배지 — 대분류/소분류

```java
// 선택자: div.inline-flex > span (class 없는 span)
// 부모 div 클래스 키워드: "inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-50"

Elements badgeDivs = doc.select("div.inline-flex.items-center.gap-1\\.5");
// 첫 번째 배지 div의 첫 번째 span = 대분류 (예: "무기")
// 첫 번째 배지 div의 두 번째 span = 소분류 (예: "인형")
// chevron icon(svg)는 span이 아니므로 자동으로 무시됨

String categoryMain = badgeDivs.get(0).select("span").get(0).text(); // "무기"
String categorySub  = badgeDivs.get(0).select("span").get(1).text(); // "인형"
```

**주의사항**:
- 재료 아이템(각성석 등)은 카테고리 배지 없음 → null 처리 필요
- 배지 div가 2개 이상인 경우도 있음 (카테고리 + 기타 정보 배지)
- 첫 번째 배지만 카테고리로 취급

### 재료 링크 — 필요 재료 목록

```java
// 선택자: a[href^="/item/"]
Elements matLinks = doc.select("a[href^=/item/]");

for (Element link : matLinks) {
    String href = link.attr("href");
    // href 예: "/item/%EB%AC%BC%EC%9D%98%EC%86%8D%EC%84%B1%EC%84%9D"
    String matName = URLDecoder.decode(href.replace("/item/", ""), StandardCharsets.UTF_8);
    // → "물의속성석"

    // 수량: "x15" 형식 텍스트 요소 (link 내부 또는 인접 sibling)
    String qtyText = link.select("[class*='x']").text(); // "x15" 형식
    // 또는 정규식으로 link 주변 텍스트에서 x\d+ 패턴 추출
    int quantity = Integer.parseInt(qtyText.replace("x", "").trim());
}
```

**주의사항**:
- 수량 텍스트("x15")는 a 태그 내부에 있을 수도, sibling에 있을 수도 있음
- 일부 아이템은 재료 없이 NPC 구매만 가능 → matLinks가 빈 리스트
- 수량 없는 경우 quantity = 1로 기본값 처리

### 상점 판매가 / 제작 비용

```java
// 페이지 텍스트에서 정규식 파싱 (이전 대화에서 확인됨)
String fullText = doc.select("main").text();

// 상점 판매가: "상점 판매가1,000,000냥" 패턴
Pattern pricePattern = Pattern.compile("상점 판매가([\\d,]+)냥");

// 제작 비용: "제작 비용100,000,000냥" 패턴
Pattern craftPattern = Pattern.compile("제작 비용([\\d,]+)냥");
```

---

## 3. gerniverse.app 용병 상세 페이지

### 접근 URL

```
https://gerniverse.app/mercenary/{용병명(한글 URL 인코딩)}
예: https://gerniverse.app/mercenary/각성 군다리명왕
```

### JSON-LD — 저항깎 수치

```java
// JSON-LD additionalProperty 배열에서 파싱
JsonNode props = root.get("additionalProperty"); // 배열
for (JsonNode prop : props) {
    String name  = prop.get("name").asText();   // "타격 저항", "마법 저항"
    String value = prop.get("value").asText();  // "100%"

    // value 파싱: "100%" → 100
    int debuff = Integer.parseInt(value.replace("%", "").trim());

    if (name.equals("마법 저항")) { magicResistDebuff = debuff; }
    if (name.equals("타격 저항")) { physicalResistDebuff = debuff; }
}
```

**주의사항**:
- value 형식이 "100%" 문자열임 (숫자가 아님)
- 마법저항깎과 타격저항깎이 분리되어 있음
- 저항깎 수치가 스킬에서 추가로 발생하는 경우는 텍스트 파싱 별도 필요
  예: "공중 몬스터 마법저항 10 감소" → 스킬 설명 텍스트에서만 확인 가능

### 이미지 경로

```java
// JSON-LD image 필드 (아이템과 동일한 구조)
String imageKey = root.get("image").get(0).asText();
// 예: "thumbnail/myeong-kings/awakening/gakGoondari"
```

---

## 4. 파싱 주의사항 종합

| 항목 | 주의사항 |
|------|----------|
| geota keyword URL | 한글 아이템명 URL 인코딩 필요 (`URLEncoder.encode(name, UTF-8)`) |
| geota 가격 | "냥" 제거 + 콤마 제거 후 Long 변환 |
| geota 구 데이터 | `강화된 {보석명}(+N)` 패턴은 가격 없음 → skip |
| gerniverse h1 | 페이지 로딩 완료 후 파싱 (SSR이므로 Jsoup 직접 접근 가능) |
| gerniverse image | 배열 첫 번째 요소 사용, null 방어 코드 필요 |
| gerniverse 카테고리 | 재료 아이템은 카테고리 배지 없음 → null 처리 |
| gerniverse 재료 수량 | "x15" 형식, x 제거 후 정수 변환 |
| gerniverse 저항깎 | "100%" 문자열 → % 제거 후 정수 변환 |

---

## 5. 검증이 필요한 잔여 항목

### 미확인 1 — geota 전체 아이템 목록 수집 방식

```
현재 확인: keyword 파라미터로 개별 아이템 접근 가능
미확인: 전체 아이템명 목록을 한 번에 수집하는 방법
        (드롭다운 목록이 검색어 입력 없이 렌더링되는지)
확인 방법: 검색창 포커스만 줬을 때 li.cursor-pointer 요소가 생기는지 확인
```

### 미확인 2 — geota 육의전 페이지네이션 서버/클라이언트 여부

```
URL: https://geota.co.kr/gersang/yukeuijeon?serverId=1&page=2
확인 항목: page 파라미터가 서버 사이드로 동작하는지
           전체 거래 건수 파악
```

### 미확인 3 — gerniverse 재료 수량 선택자 정확한 위치

```
URL: https://gerniverse.app/item/한상자의인형 (재료 여러 개인 아이템)
확인 항목: "x15" 수량 텍스트가 a[href^=/item/] 내부에 있는지,
           sibling 요소에 있는지 정확한 DOM 위치
```

### 미확인 4 — gerniverse image 필드 null 케이스

```
확인 항목: 이미지가 없는 아이템의 JSON-LD image 필드가
           null인지, 빈 배열([])인지, 아예 필드가 없는지
```