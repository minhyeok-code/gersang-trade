# 용병/보석 엔티티 및 저장(적재) 로직 리뷰 (오류/모순 점검)

기준: `back/gersangtrade/src/main/java`의 **현재 워크스페이스 코드**를 읽고 검증  
작성일: 2026-03-18  
범위: 용병(Mercenary), 보석(Gem) 및 관련 매핑/레포지토리, 매물 내 보석 연결(BundleEquipmentGem), 카탈로그 의존 엔티티(Server/Ritual 등)

> 주의: “파생되는 저장 로직(크롤링/시드/배치/서비스)”이 구현돼 있다고 하셨는데,  
> 이 워크스페이스의 `src/main/java` 내에서는 `MercenaryRepository` / `GemRepository`를 **실제로 호출(save/update)하는 서비스/배치/크롤러 코드가 검색되지 않았습니다**.  
> (엔티티/레포 정의만 존재)  
> → 저장 로직이 다른 모듈/브랜치/미커밋 파일/외부 Job에 있을 가능성이 있어 **위치 확인이 우선 필요**합니다.

---

## 1) 용병(Mercenary) 엔티티 점검

### 1.1 스키마/제약 요약
- 파일: `back/gersangtrade/src/main/java/org/example/gersangtrade/domain/catalog/Mercenary.java`
- `mercenaries.name`이 `unique=true` (용병명 유니크)
- `resistPierce`, `elementValue`는 nullable(Integer)
- `imageUrl` nullable

### 1.2 확인된 리스크/모호점
- **(정책 모호)** `resistPierce`가 “%인지 수치인지” 단위가 문서/주석만으로 확정되지 않음
  - `Mercenary` 주석은 “저항깎 수치”로만 표현 (정수형)
  - 크롤링 문서에서는 `"100%" → 디버프 수치로 변환` 같은 표현이 있는데, 변환 룰이 코드로 고정돼 있지 않음
- **(유니크 기준 위험)** 용병명이 게임 업데이트/표기 변화로 동일/유사 명칭이 생기면 충돌 가능
  - `name`만 unique이므로 “동명이인/표기 변경” 대응이 어려움  
  - 대안(설계 방향): (provider, sourceId) 같은 자연키 추가 고려

---

## 2) 용병 고용 재료(MercenaryMaterial) 점검

### 2.1 스키마/제약 요약
- 파일: `.../domain/catalog/MercenaryMaterial.java`
- UNIQUE(mercenary_id, item_id) 적용 → 동일 용병-재료 중복 방지 OK

### 2.2 확인된 리스크/모호점
- **(정합성 정책 미표기)** 고용 재료는 보통 재료 아이템인데, `ItemType.MATERIAL` 강제는 엔티티/레포만으로는 보장 불가
  - 실제 저장 로직에서 검증이 필요하지만, 호출부를 현재 워크스페이스에서 찾지 못함
- **(재적재 방식 의존)** `MercenaryMaterialRepository.deleteByMercenaryId()`가 존재
  - 크롤링 재수집 시 “전체 삭제 후 재삽입” 패턴을 전제로 한 것으로 보이나, 실제 구현 여부/트랜잭션 경계가 확인되지 않음

---

## 3) 보석(Gem) 엔티티 점검 — **가장 큰 잠재 오류 지점**

### 3.1 스키마/제약 요약
- 파일: `.../domain/catalog/Gem.java`, `.../domain/catalog/enums/GemGrade.java`
- UNIQUE(name, gem_grade, ritual_id)
- `ritual`은 nullable (ManyToOne)
- 주석상 정책: gemGrade가 `주술됨`일 때만 ritual이 non-null이어야 함

### 3.2 오류/모순 가능성

#### A) **MySQL UNIQUE + NULL 문제**로 “주술 없는 보석” 중복이 DB 레벨에서 막히지 않을 수 있음
- UNIQUE 컬럼에 `ritual_id`가 포함되고, 주술이 없는 보석은 `ritual_id = NULL`입니다.
- MySQL(InnoDB)에서 UNIQUE 인덱스는 **NULL을 서로 다른 값으로 취급**하므로,
  동일한 `(name, gem_grade, ritual_id=NULL)` 레코드가 **여러 개 들어갈 수 있습니다**.
- 영향:
  - “기본/세공됨/강화됨/빛나는” 같은 보석은 중복 insert를 DB가 막지 못할 수 있음
  - 동시 적재/재시드 시 중복 데이터가 쌓일 수 있음

#### B) `GemGrade`가 한글 Enum 값(예: `기본`, `주술됨`)을 `EnumType.STRING`으로 저장
- 파일: `.../domain/catalog/enums/GemGrade.java`
- 기술적으로는 가능하지만,
  - DB collation/charset 이슈
  - 외부 시스템/운영툴에서 다루기 어려움(검색/ETL/운영자 쿼리)
  - 다국어/표기 변경에 취약
  같은 운영 리스크가 있음

#### C) “주술됨이면 ritual 필수” 정책이 코드/DB에서 강제되지 않음
- `Gem` 엔티티/DB 제약만으로는
  - `gem_grade=주술됨`인데 ritual NULL
  - `gem_grade!=주술됨`인데 ritual non-NULL  
  같은 상태가 들어갈 수 있음
- 실제 저장 로직에서 validation 또는 DB CHECK(지원 시)로 강제해야 함

---

## 4) GemRepository 쿼리의 잠재 함정 (JPA 조인 최적화/inner join 위험)

- 파일: `.../catalog/repository/GemRepository.java`
- 메서드: `findByNameAndGemGradeAndRitualId(name, gemGrade, ritualId)`

현재 JPQL은 `g.ritual.id`를 WHERE 조건에서 직접 참조합니다.
JPA provider 구현에 따라, optional association의 id 경로 접근이 **암묵적 inner join**으로 번역될 수 있어,
`g.ritual IS NULL` 레코드(주술 없는 보석)가 조회 대상에서 사라지는 류의 버그가 발생할 수 있습니다.

> 실제로 이 버그가 터지는지는 Hibernate가 생성하는 SQL을 봐야 확정되지만,  
> 설계상 “주술 없는 보석을 이 쿼리로 찾아야 한다”는 요구와 충돌할 여지가 있어 **리스크로 기록**합니다.

---

## 5) 매물 내 보석 연결(BundleEquipmentGem) 모델링 점검

### 5.1 스키마/관계 요약
- 파일: `.../domain/listing/BundleEquipmentGem.java`
- 관계: BundleLine(N) : Gem(1)

### 5.2 확인된 리스크/모호점
- **(중복/슬롯 위치 표현 불가)** 현재 모델에는
  - “몇 번 슬롯(홈)에 박힌 보석인지”
  - “같은 보석이 여러 슬롯에 반복 장착됐는지”
  를 구분할 필드가 없음 (`slotIndex`, `position`, `quantity` 등)
- **(중복 방지 제약 없음)** 같은 `bundle_line_id + gem_id`가 여러 번 들어갈 수 있음
  - 이게 “의도적으로 허용”인지(동일 보석을 여러 홈에 박을 수 있음) vs “실수로 중복 저장”인지 정책이 필요

---

## 6) 저장(적재) 로직 관점에서의 “검증 불가” 항목

아래는 엔티티/레포/주석에서 **의도는 보이지만**, 실제 구현 코드가 워크스페이스에서 확인되지 않아 검증할 수 없었습니다.
- mercenary 목록/상세 크롤링 결과를 `MercenaryRepository.save(...)`로 업서트하는 로직
- mercenary 재료 목록을 `deleteByMercenaryId()` 후 재적재하는 로직의 트랜잭션/멱등성
- gem 이미지 수집 후 `Gem.updateImageUrl()` 호출 및 저장하는 로직
- gem 초기 데이터 “Flyway 시드” (현재 리포 내 `db/migration` SQL 파일 미발견)

---

## 7) 권장 확인 체크리스트(빠른 검증용)

- (필수) 실제 저장 로직 파일 경로 확인: `MercenaryRepository` / `GemRepository`를 사용하는 클래스가 어디인지
- (필수) `gems` 테이블에 “주술 없는 보석” 중복이 들어갈 수 있는지(UNIQUE+NULL 동작) 정책 결정
- (필수) Gem “주술됨 ↔ ritual 필수” 정합성 강제 방식 결정(애플리케이션 검증 vs DB 제약)
- (권장) BundleEquipmentGem에 슬롯/순서/중복 정책(허용/불허) 명문화

