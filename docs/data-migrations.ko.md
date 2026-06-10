# 데이터 마이그레이션 플레이북

> **용도**: 운영 단계·통합 테스트 직전에 실행할 **데이터(backfill) 마이그레이션**을 정리하는 문서.  
> 스키마 변경은 Flyway `V*__*.sql`로, 본 문서는 **기존 행의 값 보정·일괄 갱신**을 다룬다.  
> 스키마·시드 전략 개요는 [`db-seed-and-deploy.ko.md`](./db-seed-and-deploy.ko.md) 참고.

---

## 1. 이 문서를 쓰는 시점

| 시점 | 작업 |
|------|------|
| 기능 개발 중 | 아래 **「마이그레이션 등록」** 절에 항목만 추가 (상태: `예정`) |
| 스테이징/운영 테스트 직전 | 항목별 Flyway 스크립트·배치·검증 쿼리 작성 (상태: `준비됨`) |
| 배포 당일 | Flyway 적용 → 검증 쿼리 실행 → 상태 `완료`로 갱신 |

---

## 2. Flyway 원칙 (요약)

- 이미 적용된 `V{n}__*.sql`은 **수정하지 않는다**. 변경이 필요하면 **새 버전**을 추가한다.
- 파일 위치: `back/gersangtrade/src/main/resources/db/migration/`
- 현재 예시: `V3__rename_chat_room_poster_confirmed.sql`
- 데이터 마이그레이션도 동일하게 `V4__...sql` 형태로 추가한다.
- **롤백 SQL**은 Flyway가 자동 롤백하지 않으므로, 본 문서 각 항목에 수동 롤백 절차를 적어 둔다.

---

## 3. 마이그레이션 등록 절차 (체크리스트)

새 항목을 추가할 때 아래를 채운다.

- [ ] **ID** 부여 (`DM-00n`, 본 문목록에 중복 없게)
- [ ] **배경·목적** (왜 DB를 고쳐야 하는지)
- [ ] **대상 테이블·조건** (WHERE 절 수준으로 명시)
- [ ] **신규 값 규칙** (애플리케이션 코드와 동일한지 확인)
- [ ] **구현 방식** 선택 (§4 참고)
- [ ] **Flyway 파일명** (확정 시)
- [ ] **검증 쿼리** (적용 전·후)
- [ ] **롤백 방법**
- [ ] **상태** 갱신: `예정` → `준비됨` → `완료`

---

## 4. 구현 방식 선택 가이드

| 방식 | 적합한 경우 | 비고 |
|------|-------------|------|
| **Flyway SQL** | 규칙이 단순한 `UPDATE`/`JOIN`으로 표현 가능 | 가장 재현성 높음 |
| **일회성 Spring Batch / Admin API** | 애플리케이션 도메인 로직 재사용 필요 (세트 제목 등) | `SetTitleResolver`와 동일 규칙 보장 |
| **조회 시 재계산만 유지** | 화면 표시만 맞으면 됨, DB 정합성 불필요 | DB `title_override`는 구값 유지 |

운영 테스트 전에는 가능한 한 **Flyway 또는 배치로 DB 값까지 맞추는 것**을 권장한다.  
(직접 SQL 조회·외부 연동·검색 인덱스·조회 비용 절감)

---

## 5. 마이그레이션 목록

| ID | 제목 | 대상 | Flyway (예정) | 상태 | 완료일 |
|----|------|------|---------------|------|--------|
| DM-001 | 세트 거래 제목(`title_override`) 신포맷 백필 | `listing_bundles` | `V4__backfill_equipment_set_titles.sql` (또는 배치) | 예정 | — |
| *(추가)* | | | | 예정 | |

> 새 마이그레이션은 §6 템플릿을 복사해 §5 표에 한 줄 추가한다.

---

## 6. DM-001 — 세트 거래 제목 신포맷 백필

### 6.1 배경

등록 시 `listing_bundles.title_override`에 세트 표시 제목이 **등록 당시 규칙으로 스냅샷** 저장된다.

**구 포맷 예시** (2026-06 이전 등록분):

```
풀 <북두칠성> 각성광목천왕          ← 전체 동일 주술
풀 각성광목천왕 3<북두칠성>          ← 부분 주술
```

**신 포맷** (현재 애플리케이션 규칙):

```
{N}<마크> 풀 {세트명}[반쌍]

예) 5<북두칠성_개양> 풀 각성광목천왕
예) 3<북두칠성_개양> 풀 각성광목천왕반쌍
예) 풀 각성광목천왕반쌍              ← 주술 없음, 반지 포함
```

대성공 마크는 `rituals.success_mark` + `rituals.great_success_mark`를 조합한다.

- `success_mark` = `<개양>`
- `great_success_mark` = `<북두칠성>`
- 표시 = `<북두칠성_개양>` (`<>` 제거 후 `{대성공}_{성공}`)

### 6.2 현재 애플리케이션 동작 (마이그레이션 전에도 화면은 맞음)

`EQUIPMENT_SET` 번들은 **조회 시** `title_override`를 쓰지 않고 아래 데이터로 제목을 **재계산**한다.

| 소스 | 테이블 |
|------|--------|
| 피스 구성 | `bundle_lines` |
| 반지 여부 | `bundle_equipment_details` → `equipment_items.slot` |
| 주술 결과 | `bundle_equipment_rituals` (`outcome`) |
| 마크 정의 | `rituals` (`success_mark`, `great_success_mark`) |

관련 코드:

- `SetTitleGenerator` / `SetTitleResolver` — `back/gersangtrade/src/main/java/org/example/gersangtrade/listing/service/`
- `ListingBundleTitleService` — 목록·상세·거래내역 조회 시 적용

**→ 마이그레이션을 안 해도 UI 표시는 신포맷.**  
**→ DB 백필 목적**: 직접 SQL 분석, 외부 검색, 조회 비용 절감, `title_override` 단독 조회 시 정합성.

### 6.3 마이그레이션 대상

```sql
-- 대상: 자동 생성된 세트 번들 (판매자 수동 입력 제목은 제외 — 현재 MVP에서 세트는 titleOverride=null로만 등록)
SELECT lb.id, lb.title_override
FROM listing_bundles lb
WHERE lb.bundle_type = 'EQUIPMENT_SET'
  AND lb.title_override IS NOT NULL;
```

**제외 후보** (정책 확정 후 WHERE에 반영):

- 판매자가 직접 입력한 `title_override` (향후 UI 지원 시)
- 소프트 삭제된 등록글의 번들 (`trade_listings.deleted_at IS NOT NULL` 등)

### 6.4 신규 제목 생성 규칙 (코드와 동일해야 함)

`SetTitleGenerator.generate(setName, pieceMarks, hasRing)`:

1. `hasRing` = 라인 중 `equipment_items.slot = 'RING'` 존재 → 세트명 뒤 `반쌍`
2. 주술 없는 피스 → `pieceMarks`에 `null`
3. 주술 있는 피스 → `SetTitleGenerator.buildTitleMark(ritual, outcome)`
   - `SUCCESS` → `success_mark` 그대로
   - `GREAT_SUCCESS` → `<{great에서<>제거}_{success에서<>제거}>`
4. non-null 마크가 0개 → `풀 {세트명}[반쌍]`
5. 마크 종류가 2개 이상 혼재 → `풀 {세트명}[반쌍]` (폴백)
6. 그 외 → `{N}{마크} 풀 {세트명}[반쌍]` (`N` = 주술 있는 피스 수)

### 6.5 권장 구현: 일회성 백필 배치 (Flyway SQL 대신)

세트 제목 규칙은 **피스별 마크 집계·대성공 조합·혼재 폴백**이 있어 순수 SQL만으로는 유지보수 비용이 크다.  
운영 테스트 직전에 아래 중 하나로 구현하는 것을 **권장**한다.

#### 옵션 A — Admin 일회성 API (권장)

```
POST /admin/migrations/dm-001/backfill-set-titles
```

- `ListingBundleTitleService` / `SetTitleResolver`를 그대로 호출
- `bundle_type = EQUIPMENT_SET`인 번들만 순회
- 계산된 제목을 `listing_bundles.title_override`에 `UPDATE`
- dry-run 쿼리 파라미터 지원 (`?dryRun=true` → 변경 건수·샘플만 반환)

**의사 코드**:

```java
for (ListingBundle bundle : equipmentSetBundles) {
    List<BundleLine> lines = ...;
    String newTitle = listingBundleTitleService.resolveTitle(bundle, lines);
    if (!newTitle.equals(bundle.getTitleOverride())) {
        bundle.updateTitle(newTitle);
    }
}
```

#### 옵션 B — Flyway + 임시 저장 프로시저

- MySQL 프로시저로 DM-001 규칙 전체 구현 (가능하나 코드 이중 관리)
- 적용 후 프로시저 삭제하는 후속 `V5__drop_...` 필요

#### 옵션 C — Flyway SQL (단순 치환만 해당하는 행)

구 포맷 중 **패턴이 단순한 일부만** 임시로 고칠 때만 사용. 전체 규칙 대체 불가.

```sql
-- ⚠️ 예시: 전체 백필이 아님. 운영 적용 전 반드시 스테이징에서 검증.
-- TODO: DM-001 전체 백필은 옵션 A 구현 후 본 절을 "배치 실행 절차"로 교체
```

### 6.6 Flyway 파일 (확정 시 기입)

| 항목 | 값 |
|------|-----|
| 파일명 | `V4__backfill_equipment_set_titles.sql` *(또는 배치 전용 — SQL 파일 없음)* |
| 선행 버전 | `V3__rename_chat_room_poster_confirmed.sql` |
| 예상 소요 | EQUIPMENT_SET 건수 × ~6라인 조회 — 수백~수천 건 기준 수 초~수십 초 |

**배치 방식 선택 시**: Flyway에는 “마이그레이션 완료 마커”만 둘지, 배치를 배포 후 수동 1회 실행할지 팀에서 결정한다.

- **자동**: Flyway에서 `UPDATE` 가능한 범위만 SQL로
- **수동 1회**: Admin API 실행 후 본 문서 §6.7 검증 → 상태 `완료`

### 6.7 검증 쿼리

**적용 전 — 구 포맷 잔존 건수**

```sql
SELECT COUNT(*) AS legacy_format_count
FROM listing_bundles
WHERE bundle_type = 'EQUIPMENT_SET'
  AND title_override IS NOT NULL
  AND (
    title_override LIKE '풀 <%'           -- 구: 풀 <마크> 세트명
    OR title_override LIKE '풀 % %<%'    -- 구: 풀 세트명 N<마크>
  );
```

**적용 후 — 신 포맷 샘플 확인**

```sql
SELECT id, title_override
FROM listing_bundles
WHERE bundle_type = 'EQUIPMENT_SET'
ORDER BY id DESC
LIMIT 20;
```

**적용 후 — API와 교차 검증 (수동)**

1. 스테이징에서 세트 거래 목록 API 호출
2. 동일 `bundle_id`에 대해 DB `title_override`와 응답 `displayTitle`이 **일치**하는지 확인  
   *(백필 완료 후에는 조회 재계산 결과 = DB 값이어야 함)*

### 6.8 롤백

Flyway SQL `UPDATE`만 사용한 경우:

```sql
-- ⚠️ 적용 전 title_override 백업 테이블 권장
CREATE TABLE listing_bundles_title_override_backup_dm001 AS
SELECT id, title_override, NOW() AS backed_up_at
FROM listing_bundles
WHERE bundle_type = 'EQUIPMENT_SET';

-- 롤백
UPDATE listing_bundles lb
JOIN listing_bundles_title_override_backup_dm001 b ON lb.id = b.id
SET lb.title_override = b.title_override;
```

배치 API만 실행한 경우에도 **실행 전 동일 백업 테이블**을 만든다.

애플리케이션은 조회 시 재계산을 유지하므로, 롤백해도 **화면 표시는 신포맷 유지**된다.  
롤백 영향은 DB 직접 조회·백필 전후 diff 분석에 한정된다.

### 6.9 운영 테스트 직전 TODO

- [ ] 옵션 A/B/C 중 구현 방식 확정
- [ ] 스테이징 DB에 DM-001 실행
- [ ] §6.7 검증 쿼리·API 교차 검증
- [ ] (선택) 조회 재계산 제거 후 `title_override` 직독으로 전환 — **백필 완료·검증 후에만** 검토
- [ ] §5 목록 상태 `완료` 및 완료일 기록

---

## 7. 새 마이그레이션 항목 템플릿

아래 블록을 복사해 §5 표에 행을 추가하고, `## n. DM-00n — 제목` 섹션을 붙인다.

```markdown
## n. DM-00n — (제목)

### n.1 배경

(왜 필요한지, 어떤 버그/기능 변경인지)

### n.2 현재 애플리케이션 동작

(마이그레이션 없이도 동작하는지, 코드 경로)

### n.3 마이그레이션 대상

\`\`\`sql
-- 대상 행을 특정하는 SELECT
\`\`\`

### n.4 신규 값 규칙

(애플리케이션 코드·엔티티와 동일하게 기술)

### n.5 구현

| Flyway 파일 | (예: V{n}__description.sql) |
| 구현 방식 | SQL / 배치 / 혼합 |

\`\`\`sql
-- TODO: 마이그레이션 SQL 또는 배치 실행 절차
\`\`\`

### n.6 검증

\`\`\`sql
-- 적용 전
-- 적용 후
\`\`\`

### n.7 롤백

\`\`\`sql
-- 백업 및 복원 절차
\`\`\`

### n.8 운영 테스트 직전 TODO

- [ ] ...
```

---

## 8. 클리어타임·사냥 허브 마이그레이션 시점

`DeckSnapshot`, `UserClearTime` 확장 컬럼, `monster_clear_time_stats` 등 **사냥 허브 관련 Flyway 스크립트는 운영 테스트 직전 단계**에서 일괄 작성·적용한다.

| 단계 | 스키마 관리 |
|------|-------------|
| 로컬 개발 | `ddl-auto`로 엔티티 선행 가능 |
| 운영 테스트 ~ 프로덕션 | Flyway만 사용 (`validate` / `none`) |

신규 DM 항목은 스키마 확정 후 이 문서 §3 템플릿으로 등록한다. 기획: [`clear-time-hunt-hub.ko.md`](./clear-time-hunt-hub.ko.md) §9.

---

## 9. 관련 문서·코드

| 구분 | 경로 |
|------|------|
| DB 배포·Flyway 개요 | [`docs/db-seed-and-deploy.ko.md`](./db-seed-and-deploy.ko.md) |
| 세트 제목 생성 | `SetTitleGenerator.java` |
| 세트 제목 재계산 | `SetTitleResolver.java` |
| 조회 시 적용 | `ListingBundleTitleService.java` |
| Flyway 마이그레이션 디렉터리 | `back/gersangtrade/src/main/resources/db/migration/` |
| 클리어타임·사냥 허브 기획 | [`docs/clear-time-hunt-hub.ko.md`](./clear-time-hunt-hub.ko.md) |

---

## 10. 변경 이력

| 날짜 | 작성자 | 내용 |
|------|--------|------|
| 2026-06-08 | — | 문서 최초 작성, DM-001(세트 제목 백필) 등록 |
