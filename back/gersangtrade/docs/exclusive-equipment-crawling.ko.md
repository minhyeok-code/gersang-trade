# 전용장비 크롤링 설계

> 거상 거래 플랫폼 — 거상짱 전용장비 ↔ 용병 매핑 크롤러 설계  
> 작성일: 2026-06-09  
> 관련: `crawler-design.md`, `legend-equipment-spec.md`, `heavenly-king-skill-name.md`

---

## 목차

1. [개요](#1-개요)
2. [전용장비 정의 (카테고리별)](#2-전용장비-정의-카테고리별)
3. [데이터 모델](#3-데이터-모델)
4. [크롤러 분리 원칙](#4-크롤러-분리-원칙)
5. [크롤링 소스 및 URL 목록](#5-크롤링-소스-및-url-목록)
6. [파싱 및 매핑 전략](#6-파싱-및-매핑-전략)
7. [실행 순서 및 Batch Job](#7-실행-순서-및-batch-job)
8. [일반 아이템 크롤러 변경](#8-일반-아이템-크롤러-변경)
9. [구현 대상 파일](#9-구현-대상-파일)
10. [검증 체크리스트](#10-검증-체크리스트)
11. [오픈 이슈](#11-오픈-이슈)

---

## 1. 개요

### 1.1 목적

거상짱(gersangjjang.com)에서 **용병 전용장비**를 수집하고, 카탈로그 아이템(`Item`)과 시더로 관리하는 용병(`Mercenary`)을 `ItemMercenaryRestriction`으로 연결한다.

### 1.2 범위

| 포함 | 제외 |
|------|------|
| 사천왕·각성 사천왕 전용장비 | 거니버스(gerniverse) — 사용하지 않음 |
| 명왕·각성 명왕 전용장비 | 용병 특성·스킬·스탯 (각 *Seeder*가 담당) |
| 전설장수 전용장비 (6세트 + 무기) | 일반 거래용 215~250lv 메타 세트 (`set.asp`) |
| 주인공 변신무기(인형) | 사천왕·명왕 6피스 세트 방어구 (착용 제한 없음) |

### 1.3 전제

- 용병 마스터(이름·카테고리·특성·스킬)는 **ApplicationRunner 시더**가 source of truth이다.
  - `HeavenlyKingSeeder`, `AwakenedHeavenlyKingSeeder`
  - `MyeongwangSeeder`, `AwakenedMyeongwangSeeder`
  - `LegendGeneralSeeder`, `PlayerCharacterSeeder`
- 거상짱 용병 크롤러(`GersangjjangMercenaryTasklet`)는 위 카테고리를 **중복 적재하지 않도록 제외**한다.
- 전용장비 크롤러는 `MercenaryRepository.findByName(시더 canonical name)`으로 FK를 조회한다.

---

## 2. 전용장비 정의 (카테고리별)

> **핵심:** 거상짱 전용 페이지에 올라와 있다고 해서 전부 전용장비가 아니다.  
> 카테고리마다 restriction을 걸 대상이 다르다.

### 2.1 `ExclusiveEquipPolicy` 요약

| Policy | 대상 카테고리 | restriction 대상 | 제외 |
|--------|--------------|------------------|------|
| `HEAVENLY_KING_AND_MYEONGWANG` | 사천왕·각성 사천왕·명왕·각성 명왕 | **무기**, **수호부**, **무신** | 6피스 세트 (투구·갑옷·장갑·요대·신발·반지) |
| `LEGENDARY_GENERAL` | 전설장수 15명 | **섹션 내 아이템 전부** (6피스 + 무기 + 수호부 등) | — |
| `PROTAGONIST_DOLL` | 주인공 10명 (공통) | `zhu_bian.asp` 인형 전체 | — |
| `PROTAGONIST_NATIONAL` | 주인공 1명 (국가·성별) | `z_kr*` 등 해당 페이지 아이템 전부 | — (별도 확정, [11절](#11-오픈-이슈) 참고) |

### 2.2 사천왕·명왕 — 6피스는 전용이 **아님**

#### 명왕 전용 무기·수호부 규칙 (확정)

| 아이템 유형 | restriction 대상 | 비고 |
|------------|------------------|------|
| 고급명왕검·명왕검·명왕장 등 **일반 명왕 무기** | 해당 **일반 명왕** 1명 | 행 태그 `<항삼세명왕>` 또는 이름 패턴 |
| 각성명왕장·진각명왕장 등 **각성 명왕 무기** | 해당 **각성 명왕** 1명 | 행 태그 `<각성 항삼세명왕>` 또는 이름 패턴 |
| **명왕부** (`*명왕부`) | **일반 + 각성 명왕 2행** | 둘 다 착용 가능. `item_mercenary_restrictions` 2행 |

사천왕 수호부(`*천왕부`, `증장천왕부` 등)는 섹션 용병 **1명**만 restriction.

`wang_*.asp`, `ming_*.asp` 페이지에는 다음이 **같이** 나열된다.

```
[증장천왕 전용템]  (예: wang_zz.asp)

  ✅ restriction O
     - 천왕주, 고급천왕주          (WEAPON)
     - 무신의 증장천왕             (DIVINE / APPEARANCE)
     - 증장천왕부                  (TALISMAN)

  ❌ restriction X  (이름에 용병명이 있어도 공용)
     - 증장천왕의 투구/갑옷/허리띠/손목보호구/전투화/반지
     - 세트효과(2셋 민+100 등)는 해당 페이지에 묶여 있을 뿐, 착용 제한 없음
```

**슬롯 필터 (Java):**

```java
boolean isHeavenlyKingOrMyeongwangExclusive(EquipmentSlot slot, EquipmentKind kind) {
    if (kind == EquipmentKind.APPEARANCE && slot == EquipmentSlot.DIVINE) {
        return true;  // 무신
    }
    return slot == EquipmentSlot.WEAPON
        || slot == EquipmentSlot.TALISMAN;
}
```

검증용 참고: `heavenly-king-skill-name.md` — 사천왕 8종 전용 **무기** 목록.

### 2.3 전설장수 — 6세트 + 무기 **전부** 전용

`4j_*.asp` 페이지의 해당 용병 섹션 아이템은 **모두** 전용장비이다.

- 6피스(투구·갑옷·장갑·요대·신발·반지) + 무기
- 강화 단계(0강 / 5강 / 10강)별로 아이템·세트가 분리됨
- 상세 스펙: `legend-equipment-spec.md`
- restriction과 함께 `EquipmentItem.enhancement`(0/5/10) 설정 대상

```java
// LEGENDARY_GENERAL: 섹션 내 파싱된 모든 아이템에 restriction 적용
boolean isLegendaryGeneralExclusive(...) {
    return true;
}
```

### 2.4 주인공 변신무기(인형)

- 페이지: `zhu_bian.asp`
- **국적·성별 무관** — 주인공(`PROTAGONIST`) 카테고리 전원 착용 가능
- restriction 저장 방식:

```java
ItemMercenaryRestriction.builder()
    .item(item)
    .category(MercenaryCategory.PROTAGONIST)  // mercenary_id = null
    .build();
```

---

## 3. 데이터 모델

### 3.1 `ItemMercenaryRestriction` (주 저장소)

| 필드 | 용도 |
|------|------|
| `item_id` | 전용장비 아이템 |
| `mercenary_id` | 특정 1명 전용 (사천왕·명왕·전설장수·주인공 국가별) |
| `category` | 카테고리 전체 전용 (`PROTAGONIST` — 인형) |

- 행이 없으면 **공용** (누구나 착용)
- `mercenary_id`와 `category`는 **둘 중 하나만** 설정
- 덱 착용 검증: `DeckService.validateMercenaryRestriction()`

### 3.2 `EquipmentItem.mercenary_id` (선택, 카탈로그 표시용)

`legend-equipment-spec.md` 설계와 동일. restriction과 역할 분리:

| 컬럼/엔티티 | 역할 |
|------------|------|
| `ItemMercenaryRestriction` | 런타임 착용 ACL |
| `EquipmentItem.mercenary_id` | 카탈로그 "이 장비의 전용 소유자" (nullable) |

- 사천왕·명왕 6피스: `mercenary_id` **설정하지 않음**
- 인형: `mercenary_id` null (`category` restriction만 사용)

---

## 4. 크롤러 분리 원칙

### 4.1 왜 별도 Tasklet인가

| 기존 크롤러 | 한계 |
|------------|------|
| `GersangjjangItemTasklet` | 전용 페이지를 일반 장비 bulk UPSERT만 함. restriction 없음 |
| `GersangjjangMercenaryTasklet` | 용병 스탯·스킬 보완용. 시더 카테고리와 중복 |

**신규:** `GersangjjangExclusiveEquipmentTasklet` (가칭)

- 전용 페이지만 순회
- 아이템 UPSERT (기존 파서 재사용) + policy별 restriction UPSERT
- 시더 `Mercenary.name` 기준 FK 연결

### 4.2 멱등성

- restriction: `(item_id, mercenary_id)` 또는 `(item_id, category)` 중복 시 skip
- 재실행해도 시더·관리자가 입력한 데이터를 덮어쓰지 않음

---

## 5. 크롤링 소스 및 URL 목록

기본 URL: `https://www.gersangjjang.com/item/`

### 5.1 사천왕 (`HEAVENLY_KING_AND_MYEONGWANG`)

인덱스 라벨은 "(각성)"이지만, 페이지 내 **일반·각성 섹션 2개**가 공존한다.  
섹션 제목(예: `증장천왕 전용템`, `각성 증장천왕 전용템`)으로 용병을 분기한다.

| URL | 섹션 1 → Mercenary.name | 섹션 2 → Mercenary.name |
|-----|-------------------------|-------------------------|
| `wang_cg.asp` | 지국천왕 | 각성 지국천왕 |
| `wang_gm.asp` | 광목천왕 | 각성 광목천왕 |
| `wang_zz.asp` | 증장천왕 | 각성 증장천왕 |
| `wang_dm.asp` | 다문천왕 | 각성 다문천왕 |

보조 진입점: `/yongbing/wang/*.asp` 용병 상세의 **「전용장비」** 버튼 → 위 item URL.

### 5.2 명왕 (`HEAVENLY_KING_AND_MYEONGWANG`)

| URL | Mercenary.name (시더) | 비고 |
|-----|----------------------|------|
| `ming_hang.asp` | 항삼세명왕 | 각성 섹션 있으면 → 각성 항삼세명왕 |
| `ming_gun.asp` | 군다리명왕 | 각성 섹션 있으면 → 각성 군다리명왕 |
| `ming_da.asp` | 대위덕명왕 | 각성 섹션 있으면 → 각성 대위덕명왕 |
| `ming_kum.asp` | 금강야차명왕 | 각성 섹션 있으면 → 각성 금강야차명왕 |
| `ming_bu.asp` | 부동명왕 | 각성 명왕 없음 (시더에 각성 부동 없음) |

### 5.3 전설장수 (`LEGENDARY_GENERAL`)

현재 `GersangjjangParser.EXCLUDED_HREFS`에서 제외 중 → 전용 크롤러에서 처리.

| URL | 인덱스 라벨 | 시더 `Mercenary.name` |
|-----|------------|----------------------|
| `4j_lvbu.asp` | 여포(火) | 여포 |
| `4j_nobu.asp` | 노부츠나(火) | 노부츠나 |
| `4j_choi.asp` | 최무선(火) | 최무선 |
| `4j_chiyome.asp` | 치요메(水) | 치요메 |
| `4j_chosen.asp` | 초선(水) | 초선 |
| `4j_mazo.asp` | 마조(水) | 마조 |
| `4j_meng.asp` | 맹획(風) | 맹획 |
| `4j_boku.asp` | 보쿠텐(風) | 보쿠텐 |
| `4j_hong.asp` | 홍길동(風) | 홍길동 |
| `4j_zhumeng.asp` | 주몽(雷) | 주몽 |
| `4j_hua.asp` | 화목란(雷) | 화목란 |
| `4j_baji.asp` | 바지라오(土) | 바지라오 |
| `4j_akbar.asp` | 악바르(土) | 악바르 |
| `4j_manxian.asp` | 선인 만선야(雷) | 만선야 (6종, 반지 없음) |
| `4j_rejina.asp` | 레지나 술타나 | 레지나 (6종+무기2, 반지 없음) |

> 거상짱 용병 카드 풀네임(예: `도령 최무선`)과 시더명(`최무선`)이 다를 수 있다.  
> 매핑 테이블은 **시더 canonical name**을 기준으로 고정한다.

### 5.4 주인공

| URL | Policy | restriction |
|-----|--------|-------------|
| `zhu_bian.asp` | `PROTAGONIST_DOLL` | `category = PROTAGONIST` |
| `z_kr1.asp` | `PROTAGONIST_NATIONAL` | `mercenary_id` → 신궁 |
| `z_kr2.asp` | `PROTAGONIST_NATIONAL` | 포수 |
| `z_jp1.asp` | | 검호 |
| `z_jp2.asp` | | 이타코 |
| `z_cn1.asp` | | 일대종사 |
| `z_cn2.asp` | | 강신 |
| `z_tw1.asp` | | 도사 (대만男) |
| `z_tw2.asp` | | 백수왕 (대만女) |
| `z_in1.asp` | | 투신 |
| `z_in2.asp` | | 무희 |

`zhu_jiangren.asp`, `zhu_qita.asp` — **크롤링 제외 확정** (장인·기타무기, 전용장비 아님). [11절](#11-오픈-이슈) 참고.

---

## 6. 파싱 및 매핑 전략

### 6.1 HTML 구조

거상짱 전용 페이지 공통:

- **Type A:** `.data-row` → `.w-name strong` (사천왕·명왕·주인공)
- **Type B:** `.item-row .sub-row` → `.w-name` 첫 텍스트 노드 (전설장수 `4j_*.asp`)
- `parseSingleDataRow()` / `parseSingleSubRow()` 사용

사천왕·명왕·전설장수 페이지는 **섹션 헤더**로 용병·강화 단계를 구분한다.

```
예) wang_zz.asp
  [증장천왕 전용템]     ← section → mercenaryName = "증장천왕"
    .data-row × N
  [각성 증장천왕 전용템] ← section → mercenaryName = "각성 증장천왕"
    .data-row × N
```

섹션 감지: `main-title`, 섹션 구분 텍스트(`○○ 전용템`), 또는 HTML 앵커(`#2`) — 구현 시 실제 DOM 확인.

### 6.2 처리 흐름

```
for each ExclusivePageConfig (url, policy, sectionMercenaryMap):
  1. JsoupFetcher.fetch(url)
  2. parseSections(doc) → List<Section(mercenaryName, rows)>
  3. for each Section:
       for each ItemRow:
         a. Item + EquipmentItem UPSERT (슬롯: GersangjjangSetParser.detectSlotByName 또는 mixed)
         b. ItemStat / ItemSkill UPSERT (기존 로직 공유)
         c. if shouldApplyRestriction(policy, slot, kind):
              Mercenary m = findByName(section.mercenaryName)
              if m == null → WARN skip
              else upsertRestriction(item, m)
         d. (전설장수) enhancement 파싱 → EquipmentItem.enhancement 갱신
```

### 6.3 슬롯 감지

| 페이지 유형 | 슬롯 결정 |
|------------|----------|
| 사천왕·명왕·전설장수 | `mixedSlots` — `GersangjjangSetParser.detectSlotByName(아이템명)` |
| 인형 (`zhu_bian`) | `WEAPON` 고정 |

### 6.4 사천왕·명왕 전용 무기 이름 패턴 (교차 검증)

`heavenly-king-skill-name.md` 기준:

| Mercenary.name | 전용 무기 (restriction 대상) |
|----------------|------------------------------|
| 지국천왕 | 천왕검, 고급천왕검 |
| 각성 지국천왕 | 각성천왕검 |
| 광목천왕 | 천왕극, 고급천왕극 |
| 각성 광목천왕 | 각성천왕극 |
| 증장천왕 | 천왕주, 고급천왕주 |
| 각성 증장천왕 | 각성천왕궁 |
| 다문천왕 | 천왕비, 고급천왕비 |
| 각성 다문천왕 | 각성천왕비 |

---

## 7. 실행 순서 및 Batch Job

### 7.1 선행 조건

1. 앱 기동 → 시더가 `Mercenary` 행 생성 완료
2. (선택) `GersangjjangItemTasklet`이 일반 장비 선행 적재 — 전용 페이지는 [8절](#8-일반-아이템-크롤러-변경)대로 제외 가능

### 7.2 권장 Job 순서

```
itemListStep              # 일반 장비 (전용 URL 제외)
mercenaryListStep         # 일반 용병 (시더 카테고리 제외)
exclusiveEquipmentStep    # ★ 신규 — 전용장비 + restriction
setListStep
ritualListStep
```

`exclusiveEquipmentStep`은 **mercenaryListStep 이후** — `findByName` 실패 방지.

### 7.3 API 트리거 (예정)

```
POST /admin/crawler/exclusive-equipment   # 전용장비만
POST /admin/crawler/master              # masterDataJob에 step 포함
```

---

## 8. 일반 아이템 크롤러 변경

`GersangjjangParser`에서 아래 href를 **일반 크롤링 대상에서 제외**하고, 전용 크롤러가 단독 소유한다.

```java
// EXCLUDED_HREFS에 추가 (4j_*는 이미 제외됨)
"wang_cg.asp", "wang_dm.asp", "wang_gm.asp", "wang_zz.asp",
"ming_hang.asp", "ming_kum.asp", "ming_da.asp", "ming_gun.asp", "ming_bu.asp",
"z_kr1.asp", "z_kr2.asp", "z_jp1.asp", "z_jp2.asp",
"z_cn1.asp", "z_cn2.asp", "z_tw1.asp", "z_tw2.asp", "z_in1.asp", "z_in2.asp",
"zhu_bian.asp"
// zhu_jiangren.asp, zhu_qita.asp — 장인·기타무기, 크롤링 영구 제외 (전용장비 아님)
```

`CATEGORY_SLOT_MAP`에서도 해당 링크 텍스트 항목 제거.

---

## 9. 구현 대상 파일

| 파일 | 역할 |
|------|------|
| `crawler/config/ExclusiveEquipmentPageConfig.java` | URL ↔ policy ↔ mercenaryName 정적 테이블 |
| `crawler/parser/GersangjjangExclusiveEquipmentParser.java` | 섹션 분리 + policy 필터 |
| `crawler/tasklet/GersangjjangExclusiveEquipmentTasklet.java` | Batch Tasklet |
| `crawler/job/MasterDataJobConfig.java` | `exclusiveEquipmentStep` 등록 |
| `crawler/parser/GersangjjangParser.java` | `EXCLUDED_HREFS` 확장 |
| `catalog/repository/ItemMercenaryRestrictionRepository.java` | `existsByItemIdAndMercenaryId` 등 헬퍼 (선택) |

공통 UPSERT 로직은 `GersangjjangItemTasklet`의 `upsertEquipmentItem` 등을 **서비스 클래스로 추출**해 재사용한다.

---

## 10. 검증 체크리스트

### 10.1 사천왕·명왕

- [ ] `증장천왕의 투구` 등 6피스에 `item_mercenary_restrictions` 행 **없음**
- [ ] `천왕주`, `증장천왕부`, `무신의 증장천왕`에 restriction **있음**
- [ ] `heavenly-king-skill-name.md` 무기 8종 전부 restriction 연결

### 10.2 전설장수

- [ ] `4j_choi.asp` 등 15 URL 전부 크롤링
- [ ] 6피스 + 무기 **전부** restriction 있음
- [ ] 0/5/10강 `EquipmentItem.enhancement` 구분 (구현 시)

### 10.3 주인공

- [ ] `zhu_bian.asp` 인형 → `category = PROTAGONIST` (mercenary_id null)
- [ ] 인형이 특정 1명 주인공에만 연결되지 않음

### 10.4 공통

- [ ] 시더에 없는 mercenaryName → WARN 로그, restriction skip
- [ ] Item 미존재 → UPSERT 후 restriction (전용 크롤러가 Item도 생성)
- [ ] 재실행 시 restriction 중복 insert 없음

---

## 11. 오픈 이슈

| # | 이슈 | 상태 |
|---|------|------|
| 1 | `z_kr1~z_in2` 전용 범위 | **확정** — 페이지 전체를 해당 주인공 1명 `mercenary_id` 전용 (`PROTAGONIST_NATIONAL`) |
| 2 | `zhu_jiangren.asp`, `zhu_qita.asp` | **확정·제외** — 장인무기([zhu_jiangren](https://www.gersangjjang.com/item/zhu_jiangren.asp))·기타무기([zhu_qita](https://www.gersangjjang.com/item/zhu_qita.asp))는 전용장비 크롤러·restriction 대상 아님. `EXCLUDED_HREFS`로 일반 아이템 크롤러에서도 제외 |
| 3 | 전설장수 페이지 HTML | **구현 완료** — [아래 설명](#113-전설장수-강화-단계-파싱-설명) |
| 4 | 명왕 무기·명왕부 규칙 | **확정·구현** — [2.2절](#22-사천왕명왕--6피스는-전용이-아님) 명왕 전용 무기·수호부 규칙 표 참고 |
| 5 | `EquipmentItem.mercenary_id` | **구현** — [아래 설명](#115-equipmentitemmercenary_id-역할-분리) |

### 11.3 전설장수 강화 단계 파싱 설명

거상짱 `4j_*.asp` 페이지는 **섹션 헤더로 0/5/10강을 나누지 않는다**.

- 페이지 상단에 `5강세트` / `10강세트` **세트효과 설명**만 있고, 아이템은 **이름 접미사**로 구분한다.
  - 예: `최무선의화포(+5)`, `최무선의화포(+10)` — 별도 `Item` 행
- 크롤러는 **아이템명 `(+N)` 접미사**에서 `EquipmentItem.enhancement`를 추출한다.
  - `(+5)` → `FIVE`, `(+10)` → `TEN`, 접미사 없음 → `NONE`(0강)
- 0강 아이템이 페이지에 없는 전설장수도 있다(거상짱에 5/10강만 게재). 이 경우 0강 행은 생성되지 않는다.

### 11.5 `EquipmentItem.mercenary_id` 역할 분리

`legend-equipment-spec.md`대로 `equipment_items.mercenary_id` FK를 추가했다.  
**컬럼 추가로 카탈로그 표시 문제는 해결**되며, restriction과 **역할을 나눠** 함께 쓴다.

| 저장소 | 역할 | 런타임 착용 검증 |
|--------|------|------------------|
| `ItemMercenaryRestriction` | ACL (누가 착용 가능한지) | **O** — `DeckService` |
| `EquipmentItem.mercenary_id` | 카탈로그 “이 장비의 대표 전용 용병” | **X** — UI·필터·검색용 |

크롤러 적재 규칙:

| 케이스 | `mercenary_id` | `item_mercenary_restrictions` |
|--------|----------------|------------------------------|
| 사천왕·명왕·전설장수 무기 등 **1명 전용** | 해당 용병 FK | `mercenary_id` 1행 |
| **명왕부** (일반+각성 공유) | **null** | 2행 (일반·각성 각각) |
| **인형** (`zhu_bian`) | **null** | `category=PROTAGONIST` 1행 |
| **주인공 국가별** (`z_kr*`) | 해당 주인공 FK | `mercenary_id` 1행 |

FK 1개로 2명 착용(명왕부)·카테고리 전용(인형)을 표현할 수 없으므로, 그 경우는 restriction만 담당하고 `mercenary_id`는 null로 둔다.

---

## 부록: Policy 의사코드

```java
enum ExclusiveEquipPolicy {
    HEAVENLY_KING_AND_MYEONGWANG,
    LEGENDARY_GENERAL,
    PROTAGONIST_DOLL,
    PROTAGONIST_NATIONAL
}

boolean shouldApplyRestriction(ExclusiveEquipPolicy policy,
                               EquipmentSlot slot, EquipmentKind kind) {
    return switch (policy) {
        case HEAVENLY_KING_AND_MYEONGWANG ->
            (kind == EquipmentKind.APPEARANCE && slot == EquipmentSlot.DIVINE)
            || slot == EquipmentSlot.WEAPON
            || slot == EquipmentSlot.TALISMAN;
        case LEGENDARY_GENERAL, PROTAGONIST_NATIONAL -> true;
        case PROTAGONIST_DOLL -> true;  // 저장 시 category=PROTAGONIST
    };
}
```
