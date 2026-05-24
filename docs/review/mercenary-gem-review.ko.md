# 용병/보석 엔티티 및 저장(적재) 로직 리뷰 (오류/모순 점검)

기준: `back/gersangtrade/src/main/java`의 **현재 워크스페이스 코드**를 읽고 검증
작성일: 2026-03-18
재검증일: 2026-03-18
범위: 용병(Mercenary), 보석(Gem) 및 관련 매핑/레포지토리, 매물 내 보석 연결(BundleEquipmentGem), 카탈로그 의존 엔티티(Server/Ritual 등)

> 주의: "파생되는 저장 로직(크롤링/시드/배치/서비스)"이 구현돼 있다고 하셨는데,
> 이 워크스페이스의 `src/main/java` 내에서는 `MercenaryRepository` / `GemRepository`를 **실제로 호출(save/update)하는 서비스/배치/크롤러 코드가 검색되지 않았습니다**.
> (엔티티/레포 정의만 존재)
> → 크롤링·배치는 추가기능(확장) 범위이므로 저장 로직 미구현은 현재 단계에서 정상 상태입니다.

---

## 1) 용병(Mercenary) 엔티티 점검

### 1.1 스키마/제약 요약
- 파일: `back/gersangtrade/src/main/java/org/example/gersangtrade/domain/catalog/Mercenary.java`
- `mercenaries.name`이 `unique=true` (용병명 유니크)
- `resistPierce`, `elementValue`는 nullable(Integer)
- `imageUrl` nullable

### 1.2 확인된 리스크/모호점

#### A) resistPierce 단위 모호
- **수정 여부: 수정 불필요**
- 코드 주석 확인 결과: `"몬스터의 저항을 감소시키는 디버프 값. 가성비 계산기의 총 저항깎 구성 요소."` — 정수형 수치로 명확히 기술됨
- CLAUDE.md 도메인 사전에도 `저항깎 (resist pierce) | 몬스터 저항을 감소시키는 디버프 수치`로 정의됨
- 크롤링 파싱 시 "100%" 등 문자열을 정수로 변환하는 로직은 저장 로직(추가기능) 구현 시 파서에서 처리해야 할 사항이며, 엔티티 수준 수정 대상이 아님

#### B) 유니크 기준(name만) 위험
- **수정 여부: 수정 불필요 — 향후 주의 필요**
- 크롤링 소스(geota)에서 용병명이 사실상 유일한 식별자로 사용되므로 현재 설계가 크롤링 문서 기준과 일치함
- 게임 업데이트로 동명 용병이 생길 경우를 대비한 `(provider, sourceId)` 자연키 추가는 추가기능 범위이며 MVP 이후 검토

---

## 2) 용병 고용 재료(MercenaryMaterial) 점검

### 2.1 스키마/제약 요약
- 파일: `.../domain/catalog/MercenaryMaterial.java`
- UNIQUE(mercenary_id, item_id) 적용 → 동일 용병-재료 중복 방지 OK

### 2.2 확인된 리스크/모호점

#### A) ItemType.MATERIAL 강제 미검증
- **수정 여부: 수정 불필요 — 저장 로직(추가기능) 구현 시 반영**
- 엔티티 자체는 `Item`과 ManyToOne 관계이며 `ItemType` 검증은 저장 서비스/배치에서 처리해야 함
- 현재 저장 로직이 미구현(추가기능 범위)이므로 지금 엔티티에 강제할 수 없음
- 저장 로직 구현 시 반드시 `item.getItemType() == ItemType.MATERIAL` 검증 추가 필요

#### B) deleteByMercenaryId() 트랜잭션/멱등성
- **수정 여부: 수정 불필요 — 저장 로직(추가기능) 구현 시 반영**
- `MercenaryMaterialRepository.deleteByMercenaryId()`는 정의됨. 재적재 패턴(전체 삭제 후 재삽입)의 트랜잭션 경계는 이를 호출하는 서비스/배치 메서드에서 `@Transactional`로 보장해야 함
- 저장 로직 구현 시 단일 트랜잭션 안에서 delete → save 순서 보장 필요

---

## 3) 보석(Gem) 엔티티 점검 — **가장 큰 잠재 오류 지점**

### 3.1 스키마/제약 요약
- 파일: `.../domain/catalog/Gem.java`, `.../domain/catalog/enums/GemGrade.java`
- UNIQUE(name, gem_grade, ritual_id)
- `ritual`은 nullable (ManyToOne)
- 주석상 정책: gemGrade가 `주술됨`일 때만 ritual이 non-null이어야 함

### 3.2 오류/모순 및 수정 결과

#### A) MySQL UNIQUE + NULL 문제로 "주술 없는 보석" 중복이 DB 레벨에서 막히지 않음
- **수정 여부: DB 레벨 수정 보류 — 애플리케이션 레벨 대응으로 전환**
- **근거**: MySQL(InnoDB)에서 UNIQUE 인덱스에 포함된 `ritual_id = NULL`은 서로 다른 값으로 취급하므로 동일한 `(name, gem_grade, ritual_id=NULL)` 레코드가 여러 개 삽입될 수 있음
- DB 레벨 완전 해결책(MySQL 8.0.13+ functional index / generated column)은 Flyway 마이그레이션 SQL에서 처리해야 하며, 현재 Flyway 미적용 단계이므로 보류
- 대신 `GemRepository.findByNameAndGemGradeAndRitualId()` 쿼리로 저장 전 중복 확인하는 UPSERT 패턴이 이미 설계되어 있으므로, 저장 로직(배치/시드) 구현 시 반드시 해당 메서드로 선조회 후 저장해야 함
- Flyway 적용 시 `UNIQUE(name, gem_grade, COALESCE(ritual_id, 0))` 또는 전용 generated column 인덱스 추가 권장

#### B) GemGrade가 한글 Enum 값을 EnumType.STRING으로 저장
- **수정 여부: 수정 불필요 — 리팩토링 범위로 추후 검토**
- 한글 Enum + EnumType.STRING은 기술적으로 동작하나, DB collation/운영툴 쿼리/다국어 변경에 취약함
- 현재 단계에서 변경하면 기존 데이터 마이그레이션이 필요하므로 추가기능(크롤링/계산기) 구현 전에 Flyway 시드 설계와 함께 리팩토링 검토 권장
- 리팩토링 방향: Enum에 `displayName` 필드 추가 후 영문 코드를 DB 저장값으로 사용 (`BASIC`, `POLISHED`, `ENHANCED`, `GLOWING`, `RITUALISED`)

#### C) "주술됨이면 ritual 필수" 정책이 코드/DB에서 강제되지 않음 → **수정 완료**
- **수정 여부: 수정 완료**
- **수정 파일**: `Gem.java`
- **수정 내용**: `@PrePersist` + `@PreUpdate` 콜백 메서드 `validateRitual()` 추가
  - `gemGrade == 주술됨`이고 `ritual == null` → `IllegalStateException`
  - `gemGrade != 주술됨`이고 `ritual != null` → `IllegalStateException`
- **근거**: JPA 라이프사이클 콜백으로 영속화·수정 직전 자동 실행되므로 애플리케이션 레이어 어디서 save해도 정책 강제가 보장됨. DB 제약(CHECK)이 없는 현 상황에서 가장 실효성 있는 방어선

---

## 4) GemRepository 쿼리의 잠재 함정 (암묵적 inner join) → **수정 완료**

- **수정 여부: 수정 완료**
- **수정 파일**: `GemRepository.java`
- **문제**: 기존 JPQL에서 `g.ritual.id`를 WHERE 조건에 직접 참조 시 Hibernate가 `ritual` 연관에 암묵적 INNER JOIN을 생성하여 `ritual IS NULL`인 보석(주술됨 이외 등급)이 조회 결과에서 누락되는 버그 가능성
- **수정 내용**: `LEFT JOIN g.ritual r` 명시적 추가 후 WHERE 조건에서 `r.id = :ritualId`로 변경
  ```sql
  -- 변경 전
  AND (:ritualId IS NULL AND g.ritual IS NULL OR g.ritual.id = :ritualId)

  -- 변경 후 (LEFT JOIN 명시)
  LEFT JOIN g.ritual r
  AND (:ritualId IS NULL AND g.ritual IS NULL OR r.id = :ritualId)
  ```
- **근거**: LEFT JOIN을 명시하면 ritual이 없는 Gem도 결과 집합에 포함되며, `r.id`는 join alias를 통해 조인 후 필터링하므로 암묵적 INNER JOIN 위험이 제거됨

---

## 5) 매물 내 보석 연결(BundleEquipmentGem) 모델링 점검

### 5.1 스키마/관계 요약
- 파일: `.../domain/listing/BundleEquipmentGem.java`
- 관계: BundleLine(N) : Gem(1)

### 5.2 확인된 리스크/모호점

#### A) 슬롯 위치/중복 표현 불가
- **수정 여부: 수정 불필요 — 정책 명문화 필요**
- 현재 모델에 `slotIndex` 등 슬롯 순서 필드가 없음
- PRD에 슬롯 순서 표시 요구사항이 없으며, 거래 등록 UI 기획에도 슬롯 순서 식별 필요성이 명시되지 않음
- 동일 보석이 여러 슬롯에 박힐 수 있는 경우(예: 흑요석 2개) 현재 모델로는 두 개의 BundleEquipmentGem 레코드가 생성되어 표현 가능
- **결론**: 슬롯 순서 없이 "박힌 보석 종류·개수" 표현만으로 충분한지 PRD 확인 후 필요 시 `slotIndex INTEGER` 필드 추가

#### B) (bundle_line_id, gem_id) 중복 방지 제약 없음
- **수정 여부: 수정 불필요 — 정책 결정 후 반영**
- "동일 보석을 여러 슬롯에 박을 수 있다"는 게임 규칙상 `(bundle_line_id, gem_id)` UNIQUE 제약을 두면 안 됨
- 실수에 의한 중복 저장은 매물 등록 서비스 로직에서 보석 목록 validation으로 방지 예정
- 슬롯 순서 필드(`slotIndex`) 추가 시 `UNIQUE(bundle_line_id, slot_index)`로 슬롯 단위 중복만 방지하는 방향이 적절함

---

## 6) 저장(적재) 로직 관점에서의 "검증 불가" 항목

아래는 엔티티/레포/주석에서 **의도는 보이지만**, 크롤링·배치가 **추가기능(확장) 범위**이므로 현재 미구현이 정상 상태입니다. 구현 시 반드시 반영해야 할 사항들입니다.

| 항목 | 내용 | 구현 시 주의 |
|------|------|------------|
| Mercenary UPSERT | `findByName()` 선조회 후 save/updateSpec | 이미 레포 메서드 설계됨 |
| 재료 재적재 | `deleteByMercenaryId()` 후 save — 단일 `@Transactional` 필수 | 멱등성 보장 |
| ItemType 검증 | 재료 아이템이 `ItemType.MATERIAL`인지 확인 | save 전 명시적 검증 |
| Gem UPSERT | `findByNameAndGemGradeAndRitualId()` 선조회 후 save | NULL UNIQUE 문제 우회 |
| 이미지 갱신 | `Gem.updateImageUrl()` / `Mercenary.updateSpec()` 호출 후 save | 이미 메서드 설계됨 |
| Flyway 시드 | `db/migration/` SQL 파일 작성 (gems 초기 적재) | UNIQUE NULL 문제를 SQL 인덱스로 해결할 기회 |

---

## 7) 수정 사항 요약

| 번호 | 항목 | 수정 여부 | 파일 |
|------|------|----------|------|
| 3-C | 주술됨 ↔ ritual 필수 정합성 강제 | ✅ 수정 완료 | `domain/catalog/Gem.java` |
| 4 | GemRepository LEFT JOIN 명시로 암묵적 inner join 방지 | ✅ 수정 완료 | `catalog/repository/GemRepository.java` |
| 3-A | MySQL UNIQUE+NULL 중복 문제 | ⏸ 보류 (Flyway 적용 시 반영) | — |
| 3-B | GemGrade 한글 Enum | ⏸ 보류 (추가기능 전 리팩토링) | — |
| 5 | BundleEquipmentGem 슬롯/중복 정책 | ⏸ PRD 확인 후 결정 | — |
| 1-B | Mercenary name unique 위험 | ⏸ 추가기능 범위 | — |
| 6 | 저장 로직 미구현 전반 | ⏸ 추가기능 범위 | — |
