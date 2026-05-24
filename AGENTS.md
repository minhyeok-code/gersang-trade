# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

**거상 아이템 거래 서비스 (Gersang Trade)** — a trading platform for the Korean MMORPG 거상(Gersang). Users can list, search, and confirm trades for game items (equipment sets, ritual-enhanced gear, materials). The service provides price history (시세), value-for-money analysis, and a damage calculator for upgrade decisions.

## Repository Structure

```
gersangTrade/
  back/gersangtrade/   # Spring Boot backend (active development)
  front/               # Frontend (empty/not started)
  docs/                # Planning documents (Korean)
    gersang-trade-prd.ko.md          # Full PRD
    entity-model.ko.md               # Entity design
    db-seed-and-deploy.ko.md         # DB migration/deployment strategy
    instance-data-input-strategy.ko.md
    value-for-money-feature.ko.md    # 능력치별 가성비 비교 기획
    calculator.md                    # 가성비 계산기 기획 (데미지 공식, 가성비 점수)
    gersang-items-crawling.md        # 크롤링 전략·엔티티 설계 (geota/gerniverse)
```

## Tech Stack (Backend)

- **Spring Boot 4.0.3**, Java 21
- **Spring Data JPA** + MySQL
- **Spring Security** + OAuth2 Client
- **Spring Batch** — 크롤링 Job (마스터 데이터 수집, 가격 수집) + 거래 통계 집계
- **Jsoup 1.17.2** — geota/gerniverse 정적 HTML 크롤링
- **AWS SDK v2 (S3)** — 아이템/용병 이미지 S3 업로드
- **Lombok**
- **Gradle** wrapper (`gradlew`)
- Planned: **Flyway** for schema migrations and seed data

## Commands

Run from `back/gersangtrade/`:

```bash
./gradlew build
./gradlew bootRun
./gradlew test
./gradlew test --tests "org.example.gersangtrade.SomeTestClass"
./gradlew clean build
```

## Development Rules

- **한국어 주석 필수** — all code comments must be in Korean
- **현금 거래 관련 기능 구현 금지** — never implement cash-for-item trade features (policy violation)
- Each implementation step builds on the previous step's output
- **`items` CRUD API·admin UI는 구현하지 않는다** — Item 조회/등록/수정 REST API와 관리자 화면은 out of scope. 단, 크롤링 Batch Job에서 items/material_items/equipment_items 테이블에 UPSERT하는 것은 허용
- **사통팔달 크롤링·연동 기능 구현 금지** — 사통팔달은 기획 문서에서 서비스 필요성 정당화 맥락으로만 언급; 관련 수집·파싱 기능은 구현하지 않는다
- **geota/gerniverse 크롤링은 구현 범위** — `gersang-items-crawling.md` 기준. geota.co.kr(아이템/용병 목록·가격)과 gerniverse.app(상세 정보·이미지)은 robots.txt 허용 정적 HTML이며 Spring Batch Job으로 구현한다. gersanginfo.com은 외부 접근 차단으로 사용하지 않는다
- **OAuth2: Google만 MVP 범위** — Kakao 소셜 로그인은 추가기능(확장)으로 분류; MVP에서 구현하지 않는다
- **가성비 계산기·크롤링은 추가기능(확장)** — PRD 기준 추가기능. MVP 완성 후 구현. 상세 기획은 `docs/calculator.md`, `docs/gersang-items-crawling.md` 참고
- **적극적 피드백 원칙** — 요청에 설계·용어·논리 오류가 있거나 더 나은 방법이 있으면, 구현 전 또는 완료 직후 반드시 먼저 언급한다. 이상한 점을 그냥 넘어가지 않는다
- **User 삭제 정책** — 하드딜리트 없음; `deletedAt` 소프트딜리트(1년 보관 후 배치 하드딜리트), 차단은 `status=BLOCKED`(거래불가, 영구 유지)
- **TradeListing 삭제 정책** — `deletedAt` 소프트딜리트; 관리자 숨김은 `hidden=true`로 별도 처리(판매자 취소와 구분)
- **TradeListing.server 타입** — 현재 String. `Server` 엔티티 구현 후 FK로 전환 예정. 전환 전까지 서버명 직접 저장

## Agent Roles

작업은 아래 에이전트 역할 단위로 진행한다. 각 단계는 이전 단계 결과물을 기반으로 한다.

| 에이전트 | 역할 |
|---------|------|
| **기획** | PRD 기반 기능 목록·API 명세 초안 작성, 우선순위 및 구현 순서 결정 |
| **데이터설계** | ERD 설계(세트/피스/주술/주술세트 포함), JPA Entity·연관관계 정의, 인덱스 전략 수립 |
| **구현** | 기획·데이터설계 결과물 기반 코드 작성, Controller/Service/Repository 레이어 구현 |
| **검증** | 구현된 코드 리뷰 — 로직 오류·누락 기능·보안 이슈 확인, 수정 필요 사항 목록 작성 |
| **테스트** | JUnit 단위 테스트 작성, Service 레이어 중심, 경계값·예외 케이스 포함 |
| **배포** | 빌드/배포 스크립트 작성, 환경변수 및 설정 관리 |

## Domain Vocabulary

| Term | Meaning |
|------|---------|
| 세트 (set) | A group of equipment pieces with set bonus effects |
| 피스 (piece) | Individual item in a set (e.g., 5-piece armor set: helmet/armor/gloves/belt/shoes) |
| 주술 (ritual) | Enhancement applied to a piece; success=`<00>`, great success=`<**>` |
| 외변 (appearance) | Appearance-type equipment; only 5-강(+5 enhance) is traded |
| 보석 (gem) | 11종 보석 아이템 (흑요석 등). 기본→세공됨→강화됨→주술됨 4단계 상태를 가짐 |
| 용병 (mercenary) | 게임 내 고용 캐릭터. 저항깎·속성값 등 스펙을 보유하며 가성비 계산기 대상 |
| 사통팔달 | Paid server-wide broadcast message used for high-value trades — **기획 문서에서 서비스 필요성 설명용으로만 언급됨; 크롤링·연동 기능 구현 없음** |
| 시세 | Market price history (avg/min/max/volume by period) |
| 저항깎 (resist pierce) | 몬스터 저항을 감소시키는 디버프 수치. 용병 또는 장비에서 제공 |
| 속성값 (element value) | 속성 추가 데미지 계산에 사용되는 수치. 공식: `n% = (3x - y) / 2`, cap ±50% |
| 가성비 | 두 맥락으로 사용됨. (1) 능력치 비교: `stat_value ÷ avg_price` (높을수록 좋음) (2) 계산기: `데미지 상승률(%) ÷ 아이템 가격(만 골드)` (높을수록 좋음). 상세 공식은 `docs/calculator.md` 참고 |

## Domain Architecture (Four Layers)

### Catalog (정의/사전 데이터) — Master/Reference Data
Flyway 또는 크롤링 Batch로 적재. 거의 변경되지 않으며 모든 기능의 기반.

- `Item` — base entity (`MATERIAL` | `EQUIPMENT`)
- `MaterialItem` — 1:1 with Item for materials
- `EquipmentItem` — 1:1 with Item; holds `slot`, `equipmentKind` (`APPEARANCE`|`NORMAL`), optional `setId`
- `EquipmentSet` / `EquipmentSetPiece` — defines standard 5-piece armor sets
- `Ritual` — ritual definition with `successMark` and `greatSuccessMark` snapshots
- `RitualApplicability` — M:N mapping of which rituals apply to which equipment items
- `ItemStat` — base stats per item (`ELEMENT_VALUE` | `ELEMENT_PIERCE` | `RESIST_PIERCE`)
- `Gem` — 보석 엔티티. 11종 × 등급(기본/세공됨/강화됨/빛나는/주술됨). `ritual_id` nullable
- `Server` — 거상 서버 13개 고정 목록 (`server_id` 1~13). 가격 집계 및 거래 등록의 서버 구분 기준
- `Mercenary` — 용병 정보. `name`, `resistPierce`(저항깎), `elementValue`(속성값), 고용 재료, 이미지 URL. 가성비 계산기 대상

### Crawler (크롤링 집계 데이터) — 추가기능 범위
geota/gerniverse 크롤링 결과 적재. 가성비 계산기의 가격 기본값으로 사용.

- `MaterialPriceHistory` — 서버별 재료 월간 실거래가 집계. `avgPrice`, `minPrice`, `sampleCount`. IQR 이상치 제거 후 저장

### Listing (거래 등록 데이터) — User-generated trade posts
- `TradeListing` — the trade post; status: `ACTIVE → IN_TRADE → SOLD | CANCELLED`. `server`는 현재 String (Server FK 전환 예정)
- `ListingBundle` — sale unit within a post (`MATERIAL_BUNDLE` | `EQUIPMENT_SINGLE` | `EQUIPMENT_SET`)
- `BundleLine` — individual item+quantity within a bundle
- `BundleEquipmentDetail` — 1:1 extension of BundleLine for equipment (enhance level, ritual flag)
- `BundleEquipmentRitual` — ritual outcome per equipment line (`SUCCESS` | `GREAT_SUCCESS` + snapshot of final display mark)
- `BundleEquipmentGem` — 장비 라인에 박힌 보석 (`BundleLine` → `Gem` 매핑)

### Trade (거래 확정/통계 데이터)
- `TradeConfirmed` — immutable confirmed trade record (source of truth for stats)
- `TradeStatDaily` — daily rollup (`statDate`, `statKey`, `tradeCount`, `priceSum`, `priceMin`, `priceMax`)
- `TradeStatMonthly` — monthly rollup for value-for-money feature
- `ValueMetricMonthly` — (optional) pre-computed `avgPrice / statValue` cache

## Key Design Patterns

**Set display notation** is computed from bundle lines:
- `풀 {주술마크} {세트명}` — all 5 pieces have the same ritual (e.g., `풀 XX 00세트`)
- `풀 {세트명} {주술피스수}{주술마크}` — partial ritual (e.g., `풀 00세트 3XX`)

**Ritual availability lookup**: when a user selects equipment during listing creation, filter available rituals via `RitualApplicability WHERE equipmentItemId = ?`.

**Stats aggregation strategy**: confirmed trades trigger upsert into `TradeStatDaily` (event-driven, or nightly batch). The daily table is always the query target for price history — never query raw confirmed trades for stats.

**Damage calculator formula** (`docs/calculator.md`):
- 저항 통과율 = `100 - (깎은 뒤 저항 × 0.16 + 57)` (저항 < 260일 때), 260 이상이면 1.4% 고정
- 속성 보정 = `clamp((3 × 용병 속성값 - 몬스터 속성값) / 2, -50, +50)`
- 가성비 점수 = `데미지 상승률(%) ÷ 아이템 가격(만 골드)` (높을수록 좋음)
- 가격 기본값: `MaterialPriceHistory`의 직전 달 avgPrice. 유저가 세션 내 수정 가능 (DB 미저장)

**Crawling schedule** (`docs/gersang-items-crawling.md`):
- Job 1 (마스터 데이터): 최초 1회 + 관리자 수동 트리거 (`POST /admin/crawler/master`)
- Job 2 (가격 수집): 매월 1일 새벽 3시 자동 실행 (`POST /admin/crawler/price`로 수동 트리거 가능)
- 요청 딜레이 1~2초, 타임아웃 10초, 최대 3회 재시도

## DB / Deployment

- **Local**: MySQL + `spring.jpa.hibernate.ddl-auto` (dev only)
- **Production**: AWS EC2 + RDS (MySQL). DB credentials via environment variables.
- **Schema + seed data**: managed by **Flyway** migrations (`V1__init.sql`, `V2__seed_items.sql`, etc.)
- Production `ddl-auto` should be `validate` or `none`; Flyway owns schema changes.
- Catalog data (items, rituals, mappings) is seeded via Flyway — never rely on application-level seeders in production.
- `MaterialPriceHistory`는 크롤링 Batch Job이 월 1회 적재; Flyway 대상 아님

## User Roles

| Role | Capabilities |
|------|-------------|
| Guest | Browse listings, search, view price history, use damage calculator |
| User (로그인) | All of above + register listings, apply for trades, confirm trades, file reports |
| Admin | Process reports, block/unblock users, hide content, trigger crawler jobs manually |
