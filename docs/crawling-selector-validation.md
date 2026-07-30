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

## 2. 파싱 주의사항 종합

| 항목 | 주의사항 |
|------|----------|
| geota keyword URL | 한글 아이템명 URL 인코딩 필요 (`URLEncoder.encode(name, UTF-8)`) |
| geota 가격 | "냥" 제거 + 콤마 제거 후 Long 변환 |
| geota 구 데이터 | `강화된 {보석명}(+N)` 패턴은 가격 없음 → skip |

---

## 3. 검증이 필요한 잔여 항목

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

