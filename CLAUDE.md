# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**거상 아이템 거래 서비스 (Gersang Trade)** — a trading platform for the Korean MMORPG 거상(Gersang). Users can list, search, and confirm trades for game items (equipment sets, ritual-enhanced gear, materials). The service provides price history (시세) and value-for-money analysis.

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
    value-for-money-feature.ko.md
```

## Tech Stack (Backend)

- **Spring Boot 4.0.3**, Java 21
- **Spring Data JPA** + MySQL
- **Spring Security** + OAuth2 Client
- **Spring Batch** (for trade stat aggregation)
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
- **`items` 관련 코드는 구현하지 않는다** — Item entity/CRUD/seed/admin is currently out of scope
- **사통팔달 크롤링·연동 기능 구현 금지** — 사통팔달은 기획 문서에서 서비스 필요성 정당화 맥락으로만 언급; 관련 수집·파싱 기능은 구현하지 않는다
- **OAuth2: Google만 MVP 범위** — Kakao 소셜 로그인은 추가기능(확장)으로 분류; MVP에서 구현하지 않는다
- **적극적 피드백 원칙** — 요청에 설계·용어·논리 오류가 있거나 더 나은 방법이 있으면, 구현 전 또는 완료 직후 반드시 먼저 언급한다. 이상한 점을 그냥 넘어가지 않는다
- **User 삭제 정책** — 하드딜리트 없음; `deletedAt` 소프트딜리트(1년 보관 후 배치 하드딜리트), 차단은 `status=BLOCKED`(거래불가, 영구 유지)
- **TradeListing 삭제 정책** — `deletedAt` 소프트딜리트; 관리자 숨김은 `hidden=true`로 별도 처리(판매자 취소와 구분)

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
| 사통팔달 | Paid server-wide broadcast message used for high-value trades — **기획 문서에서 서비스 필요성 설명용으로만 언급됨; 크롤링·연동 기능 구현 없음** |
| 시세 | Market price history (avg/min/max/volume by period) |
| 가성비 | Value-for-money metric: `avg_price ÷ stat_value` (lower = better) |

## Domain Architecture (Three Layers)

### Catalog (정의/사전 데이터) — Master/Reference Data
Seeded via Flyway. Rarely changes. Core to all other features.

- `Item` — base entity (`MATERIAL` | `EQUIPMENT`)
- `MaterialItem` — 1:1 with Item for materials
- `EquipmentItem` — 1:1 with Item; holds `slot`, `equipmentKind` (`APPEARANCE`|`NORMAL`), optional `setId`
- `EquipmentSet` / `EquipmentSetPiece` — defines standard 5-piece armor sets
- `Ritual` — ritual definition with `successMark` and `greatSuccessMark` snapshots
- `RitualApplicability` — M:N mapping of which rituals apply to which equipment items
- `ItemStat` — base stats per item (`ELEMENT_VALUE` | `ELEMENT_PIERCE` | `RESIST_PIERCE`)

### Listing (거래 등록 데이터) — User-generated trade posts
- `TradeListing` — the trade post; status: `ACTIVE → IN_TRADE → SOLD | CANCELLED`
- `ListingBundle` — sale unit within a post (`MATERIAL_BUNDLE` | `EQUIPMENT_SINGLE` | `EQUIPMENT_SET`)
- `BundleLine` — individual item+quantity within a bundle
- `BundleEquipmentDetail` — 1:1 extension of BundleLine for equipment (enhance level, ritual flag)
- `BundleEquipmentRitual` — ritual outcome per equipment line (`SUCCESS` | `GREAT_SUCCESS` + snapshot of final display mark)

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

## DB / Deployment

- **Local**: MySQL + `spring.jpa.hibernate.ddl-auto` (dev only)
- **Production**: AWS EC2 + RDS (MySQL). DB credentials via environment variables.
- **Schema + seed data**: managed by **Flyway** migrations (`V1__init.sql`, `V2__seed_items.sql`, etc.)
- Production `ddl-auto` should be `validate` or `none`; Flyway owns schema changes.
- Catalog data (items, rituals, mappings) is seeded via Flyway — never rely on application-level seeders in production.

## User Roles

| Role | Capabilities |
|------|-------------|
| Guest | Browse listings, search, view price history |
| User (로그인) | All of above + register listings, apply for trades, confirm trades, file reports |
| Admin | Process reports, block/unblock users, hide content |
