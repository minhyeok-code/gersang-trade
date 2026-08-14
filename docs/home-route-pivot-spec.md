# 홈 콜드스타트 개편 & 사천왕 육성 루트 스펙

## 0. 배경 / 목적

런칭 초기(등록 매물 少)에는 로그인 홈이 `GuideChecklistPanel` + `InterestPriceWatchPanel`
두 데이터 의존 패널로만 구성되어, 둘 다 빈 상태로 무너지며 화면이 휑하다.
(역설적으로 비로그인 홈보다 더 비어 보인다.)

동시에 기존 큐레이션 **가이드 콘텐츠는 작성자가 공유 거부**하여 삭제해야 한다.

→ 방향: 홈을 "매물·시세(콜드스타트엔 빈값)" 의존에서 벗어나
**"속성/사천왕 선택 → 목표 몬스터 → 육성 루트"** 지식·계획 허브로 리프레이밍한다.
사천왕 초상 그리드 자체가 유동성과 무관한 실제 콘텐츠라 콜드스타트에도 화면이 찬다.

가이드는 삭제하되, 그 자리를 **유저 생성 + 유저↔유저 클론 가능한 "루트"** 로 대체한다.

---

## 1. 확정된 결정 사항

1. **엔티티**: 기존 `UserGuide`/`UserGuideStep`을 "Route"로 승격 재사용.
   큐레이션 `Guide`/`GuideStep` + guide-import 계층은 폐기.
2. **다중 루트**: 유저 1명이 여러 루트를 만들 수 있다.
3. **공개범위**: 루트별 `visibility`. **기본 PUBLIC**, 언제든 PRIVATE로 전환 가능.
4. **목표 몬스터 필수**: 루트는 목표 몬스터 단위로 갈린다. `targetMonster`는 필수 FK.
5. **클론(fork)**: 공개 루트를 다른 유저가 자신의 계정으로 복제해 진행할 수 있다.
6. **진입 축**: 속성 4종(=사천왕 1:1) → 일반/각성 → 몬스터 → 공개 루트 리스트 → 열람/클론.
   덱이 설정돼 있으면 해당 덱 주력 사천왕을 자동 선택.
7. **관심 시세 배너 비활성화** (콜드스타트에 부적합).
8. **관심 매물 조회**: 동일 서버 listing만, 판매/구매 분리 count + 각 min 가격.
9. **관리자**: 몬스터 이미지 등록 + S3 경로 동기화 대상에 몬스터 추가,
   이미지 미등록 몬스터/용병/아이템을 비노출 처리하는 API 버튼 추가. (루트 개편의 **선행 조건**)
10. **비주얼**: 놀이터풍 다채로운 스타일. **파일럿**(추후 앱 전역 전환 전제) — 별도 테마 토큰 레이어로 구현.

---

## 2. 도메인 근거 (스펙 확인 완료)

속성 4종 ↔ 사천왕 1:1, 각성 4종 모두 존재. 빠지는 조합 없음.

| 속성 | 사천왕 | 각성 |
|------|--------|------|
| 화(FIRE)    | 지국천왕 | 각성 지국천왕 |
| 풍(WIND)    | 광목천왕 | 각성 광목천왕 |
| 뇌(THUNDER) | 증장천왕 | 각성 증장천왕 |
| 수(WATER)   | 다문천왕 | 각성 다문천왕 |

- **명왕(明王)은 사천왕이 아닌 별도 계열** → 이번 범위 제외. 추후 동일 틀에 계열만 추가.
- 속성 선택 = 사천왕 선택(1:1)이므로 **한 동작**으로 처리. 속성→사천왕 2단 분리 금지.

---

## 3. 엔티티 개편

### 3-1. 폐기
- `domain/guide/Guide.java`, `domain/guide/GuideStep.java`
- guide-import 일체: `guide/dto/request/GuideImport*.java`, `GuideStepImport.java`,
  `GuideAdminController`/`GuideAdminService`의 import 경로, `admin/guide-import` 프론트 페이지.
- `Guide`/`GuideStep` 리포지토리, 관련 DTO(`GuideSummaryResponse`, `GuideStepResponse` 등 원본 전용).
- `UserGuide.sourceGuide` / `sourceVersion`(원본 스냅샷 의미) 제거.
- "덱 주력 → 추천 가이드" 매칭 로직 폐기. 단, "덱 있으면 해당 사천왕 자동선택" 용도로만 재활용.

### 3-2. 재사용 (Route로 승격)
- `UserGuide` → **Route**: 유저 소유, 제목, 대상 사천왕(용병), 소프트삭제, 다중 허용.
- `UserGuideStep` → **RouteStep**: 수정 가능 스텝, `stepOrder` 재정렬, `stepType`,
  `label`/`note`, 카탈로그 링크(item/set/mercenary) + 매물 funnel, `iconUrl`,
  `custom` 플래그, `checkedAt` 진행도. → **구조 그대로 유지**.
- 기존 "원본 Guide → UserGuide 스텝 복제" 루틴은 **fork(공개 루트 → 내 루트)와 동일 연산**이라
  source 타입만 바꿔 재사용.

> 리네임(Guide→Route)은 의미 명확성 vs 마이그레이션 비용의 트레이드오프.
> 권장: 리네임하되 Flyway 마이그레이션으로 테이블·컬럼 rename. (팀 결정 사항이면 rename 없이 필드만 추가해도 동작함.)

### 3-3. 추가 필드 (Route)
| 필드 | 타입 | 설명 |
|------|------|------|
| `visibility` | enum(PUBLIC, PRIVATE) | 기본 PUBLIC. 유저가 언제든 전환 |
| `phase` | enum(NORMAL, AWAKENED) | 일반/각성 (기존 GuidePhase 재사용) |
| `targetMonster` | FK(Monster) NOT NULL | 목표 몬스터. 루트 분류 키 |
| `sourceRoute` | FK(Route) nullable | fork 출처(계보 추적). 원본 삭제돼도 유지되게 nullable |
| `cloneCount` | int (선택) | 이 루트가 복제된 횟수(정렬·노출용, 캐시성) |
| `deckSnapshot` | FK(DeckSnapshot) nullable | 작성자 덱의 불변 스냅샷(발행 시점). 라이브 UserDeck 연결 금지 |
| `deckShared` | boolean (기본 false) | 덱 공개 옵트인. false면 `deckSnapshot`이 있어도 열람자에게 미노출 |

- `targetMercenary`(사천왕)는 기존 필드 유지. `phase`와 함께 (사천왕, phase, 몬스터)가 조회 키.

### 3-4. 덱 첨부 (DeckSnapshot 재사용)
- **라이브 `UserDeck`을 루트에 직접 연결하지 않는다.** 주인이 계속 수정·삭제하므로 열람자에겐
  내용이 바뀌거나 링크가 깨진다. 루트는 "그 시점의 길"이므로 덱도 얼려야 한다.
- 기존 `domain/hunt/DeckSnapshot`(불변 JSON + content_hash 중복제거) + `DeckSnapshotBuilderService` 재사용.
  클리어타임 제출이 남의 덱을 스냅샷으로 보여주는 것과 **동일 패턴**.
- **프라이버시**: 루트 기본 PUBLIC이므로 "루트 공개 = 빌드 공개"가 되지 않게, 덱 첨부는
  루트 visibility와 **별개의 옵트인**(`deckShared`)으로 둔다.
- **fork 시**: 복제본은 forker 소유의 새 루트다. 원본의 `deckSnapshot`을 복사하지 않는다
  (forker의 덱은 다름). `deckSnapshot=null`, `deckShared=false`로 시작.

---

## 4. API

### 4-1. 공개 루트 조회
```
GET /routes?mercenaryId={}&phase={NORMAL|AWAKENED}&monsterId={}
  → visibility=PUBLIC 인 루트 목록. 정렬 기본: cloneCount desc, 최근순.
  → 각 항목: 제목, 작성자, phase, 진행도(작성자 기준 표시는 선택), cloneCount, 스텝 요약.
  → deckShared=true 인 루트만 응답에 deckSnapshot(요약/식별자) 포함. false면 미포함.
```

### 4-1b. 루트 상세 (덱 포함)
```
GET /routes/{routeId}
  → 스텝 전체 + (deckShared=true 이면) deckSnapshot content_json.
POST /routes/{id}/deck   (내 활성 덱 스냅샷을 떠서 첨부, deckShared 토글)
DELETE /routes/{id}/deck (덱 첨부 해제)
```

### 4-2. fork(복제)
```
POST /routes/{routeId}/clone
  → 대상 공개 루트의 스텝을 복제해 로그인 유저 소유의 새 Route 생성.
    sourceRoute=원본, visibility=PRIVATE 기본(복제본은 우선 비공개 권장 — §7 참고),
    진행도(checkedAt) 초기화, custom=false.
  → 원본 cloneCount++.
```

### 4-3. 내 루트 CRUD (기존 UserGuide 계열 재사용/이관)
```
GET    /routes/mine
POST   /routes                      (빈 루트 새로 만들기: 사천왕·phase·몬스터 지정)
PATCH  /routes/{id}                 (제목·visibility 변경)
DELETE /routes/{id}                 (소프트삭제)
POST   /routes/{id}/steps           (스텝 추가)
PATCH  /routes/{id}/steps/{stepId}  (내용·순서 수정)
POST   /routes/{id}/steps/{stepId}/check  · DELETE (체크/해제)
```

### 4-4. 관리자 (선행)
```
POST /admin/images/monster/sync     (몬스터 S3 이미지 경로 동기화 — 기존 아이템/용병 동기화에 몬스터 추가)
POST /admin/catalog/hide-missing-image
  → 이미지 미등록 몬스터/용병/아이템을 비노출(exposed=false)로 일괄 전환. 카테고리 파라미터로 대상 구분.
```

---

## 5. 프론트 — 홈 흐름

로그인/비로그인 공통으로 아래 드릴다운을 홈 메인에 배치.

1. **사천왕 카드 4개**(요소 배지 + 일반/각성 스탠딩 2컷) — 콜드스타트 앵커.
   - 카드 상단 중앙에 **요소 배지**(게임 메달), 그 아래 **일반/각성 전신 스탠딩 2컷**.
   - **텍스트 라벨 없음(이미지 온리)** — PNG 자체가 속성·천왕·일반/각성을 모두 표현. 이름/속성 캡션 미표기.
   - **스탠딩 이미지 클릭 = (사천왕 + 일반/각성) 동시 선택.** 기존 1·2단계(속성→일반/각성)를 카드 하나로 병합.
2. **목표 몬스터 리스트** — 해당 사천왕의 몬스터 목록.
   (사천왕→몬스터 매핑은 루트 존재 여부와 분리. 매핑엔 있으나 루트 없는 몬스터는 "루트 준비 중" 표기 → 리스트가 항상 참.)
3. **공개 루트 리스트** — (사천왕, phase, 몬스터)로 필터된 공개 루트. 열람/미리보기/클론.
   + "빈 루트로 새로 만들기" CTA.
   - 루트 상세에서 `deckShared=true`면 작성자 덱을 **기존 `DeckSnapshotViewer`로 렌더**(신규 화면 X).
4. **되돌리기/재선택**: 브레드크럼으로 상위 단계 클릭 시 재선택·접기.

### 5b. 사천왕 이미지 소스 (2종)
- **헤드샷(증명사진)**: 기존 `mercenary.imageUrl`(용병 이미지 컬럼) 재사용. 일반 용병 포함 모든 용병의 기본 이미지.
- **전신 스탠딩(사천왕 전용)**: 별도 **S3 폴더 트리**에 업로드, **파일명 = 사천왕 id**.
  URL 규칙(예: `{STAND_BASE}/{id}.png`)으로 **직접 로드** — 도감 DB 컬럼/관리자 이미지 파이프라인 불필요(수동 업로드).
  일반/각성이 각각 별도 id → 카드의 두 슬롯이 각기 다른 스탠딩 이미지를 가리킴.
- 스탠딩 이미지는 §9 P0(몬스터 이미지 동기화)와 무관한 별도 경로. base URL만 환경설정에 둔다.

- **덱 설정 시**: 진입하면 덱 주력 사천왕이 자동 선택된 상태. 유저가 직접 타 사천왕 재선택 가능.
- 비주얼: 목업(`sacheonwang-route-mockup.html`) 기준. 스타일은 §8 토큰 레이어로.

### 부가
- `InterestPriceWatchPanel`의 **시세 배너 비활성화**.
- **관심 매물 조회 패널**: 관심 아이템별로 동일 서버 listing 조회 → 판매/구매 분리,
  각 count + min 가격 표기. (콜드스타트엔 0/— 가능 → 각 항목에 "이 서버 첫 매물 등록" CTA 병기 권장.)

---

## 6. 시드 = 예시 목업

- 시드 루트는 **가이드가 아니라 "이런 기능이야" 데모**. 시스템 계정 소유의 공개 루트 몇 개,
  `예시` 라벨. 권위 없음.
- 유저가 자기 루트를 만들면 자연히 밀려나거나 접히게 처리.

---

## 7. 유의점 (반대 아님 · 인지/자리만)

1. **기본 PUBLIC의 품질 리스크**: 유저 증가 시 "몬스터별 공개 루트 리스트"에 미완성·장난 루트 유입.
   런칭 초기(시드 예시뿐)엔 무해. 향후 필요: 정렬(진행도·cloneCount), 완성도 필터, 신고.
   → **지금 구현 X, 자리만 비워둔다.** (fork 복제본을 기본 PRIVATE로 두는 것도 오염 완화책 → §4-2)
2. **파일럿 스타일은 일회성 인라인 금지**: 놀이터 팔레트를 별도 테마 토큰 레이어로 분리(§8).
   전역 확장 시 이 레이어만 넓힌다.

---

## 8. 테마 토큰 (파일럿 — 레트로 아케이드 / 다크)

- 방향 확정: **레트로 아케이드(딥 네이비 + 네온).** 다크 테마.
- 기존 `globals.css`의 잔잔·모던 토큰은 유지하되, 이 섹션은 **다크 테마 토큰 레이어**로 추가한다.

베이스 토큰:
```
--pg-bg: #1E2140   --pg-bg2: #282C56   --pg-card: #2A2E52   --pg-line: #3A4070
--pg-ink: #EDEFFF  --pg-ink-soft: #A6ABD6   --pg-primary(네온핑크): #FF4D8D
```
속성 4색(다크 위 강조색, 사천왕/몬스터/루트 UI 전반 일관 사용) — **게임 메달 규칙 기준**:
```
화=#DE3A2E  풍=#3FAE3A  수=#2596E6  뇌=#F0C020
```
- 게임 내 속성 표기는 초상화 우측 상단 **원형 메달**(화=주황빨강 불꽃 / 풍=초록 / 수=파랑 / 뇌=노랑 번개).
  위 값은 그 색상(hue) 규칙을 따르되 웹용으로 정리한 것. (게임 원본 색은 글로우·그라데이션이라 그대로 쓰면 탁함.)
- **주의**: 게임 카드 테두리색(초록/빨강/파랑/금색)은 속성이 아니라 별개 표기 → 혼동 금지.
- 폰트·라운드·그림자 등 상세 토큰은 목업 상단 주석(`레트로 아케이드` 블록) 참고.

> **다크 커밋 주의**: 이건 다크 테마라, 파일럿을 앱 전역으로 넓히면 **모든 화면을 다크로 전환**해야 한다.
> 지금은 홈 루트 섹션에만 적용하고, 전역 전환은 별도 결정으로 둔다.

---

## 9. 구현 순서

- **P0 (선행)**: 관리자 몬스터 이미지 등록 + S3 동기화 + 이미지 미등록 비노출 토글.
  (몬스터 리스트/그리드가 깨져 보이지 않게 하는 전제)
- **P1**: 엔티티 개편(Guide 계층 폐기, Route 승격 + 필드 추가, Flyway) → 공개 조회/fork/내 루트 API →
  홈 사천왕 드릴다운 + 루트 렌더(체크리스트 재사용).
- **P2**: 시세 배너 비활성화 + 관심 매물 조회(동일 서버 판매/구매 count·min).
- **나중**: 유저 루트 공유 품질 관리(정렬·필터·신고), 클리어타임 데이터 축적 후 DPS 기반 예상 클리어타임/가능성.
