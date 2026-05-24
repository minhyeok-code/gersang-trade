# 속성값(ELEMENT_VALUE) 버그 수정 체크리스트

> 참고: `front/docs/element-value-backend.ko.md`
> 작성일: 2026-05-24

---

## 이슈 목록

### 1. SELF_AUTO 각성 속성값 DPS 미반영 — ✅ 완료

**증상**: 각성 사천왕(+20), 각성 명왕(+15) 속성값이 DPS 계산에서 누락.

**원인**: 시더에서 `applyType=SELF_AUTO` 특성이 저장되지만 효과값이 DB에 없었고, DPS 파이프라인에 SELF_AUTO 처리 코드도 없었음.

**수정 내용**:
- `AwakenedHeavenlyKingSeeder.upsertAwakening()` — level=1 행 저장 (amountValue=20, statType=ELEMENT_VALUE)
- `AwakenedMyeongwangSeeder.upsertAwakening()` — level=1 행 저장 (amountValue=15, statType=ELEMENT_VALUE)
- `MercenaryCharacteristicRepository` — `findSelfAutoByMercenaryIdIn()` 쿼리 추가
- `DpsCalculatorService` — SELF_AUTO 특성 배치 로딩 후 `memberSelfFlatPref`에 자동 합산
- Javadoc 수정 (`MercenaryCharacteristic`, `MercenaryCharacteristicLevel`)

---

### 2. 정령 속성 필터 불일치 — ✅ 의도된 동작 (수정 불필요)

정령 버프는 게임 룰상 속성 구분 없이 덱 전원에 적용되는 것이 맞음.
진법/층진과 규칙이 달라 보이지만, 각 소스별 게임 룰의 차이이므로 버그 아님.

---

### 3. `getMemberStats()` 스탯 API와 DPS 수치 불일치 — ✅ 완료

**수정 내용**:
- `DeckService` — `PlayerCharacterBuffCalculator`, `AwakenedMyungwangBuffCalculator` 주입
- `getMemberStats()` — 명왕 이전 합산 후 주인공 국가 버프 + 각성 명왕 N×(+5, EARTH +2) 추가 반영

---

### 4. 각성 명왕 N명 중첩 + 덱 편성 제한 — ✅ 완료

**수정 내용**:
- `DpsCalculatorService.applyAwakenedMyeongwangAllyBuff()` — `anyMatch` → count 기반 N × (+5, EARTH +2) 중첩
- `DeckService.validateMyeongwangComposition()` 신규 추가 (`addMember()` 시 호출):
  - 부동명왕(EARTH) 제외 명왕·각성명왕 합산 최대 2명 제한
  - 대위덕명왕/각성 대위덕명왕(WIND 계열) 공존 불가

---

### 5. `natureValue` vs `MercenaryStat(ELEMENT_VALUE)` 동기화 — ✅ 완료 (안A)

**수정 내용**:
- `MercenaryResponse.of()` — `m.getNatureValue()` 대신 `Integer elementValue` 파라미터로 변경
- `MercenaryService.listMercenaries()` — 기존 배치 조회 결과에서 `ELEMENT_VALUE`도 함께 추출해 전달
- `Mercenary.natureValue` 필드는 관리자 API·크롤링 용도로 유지 (공개 API에서는 사용 안 함)

---

## 진행 현황

| # | 제목 | 상태 |
|---|------|------|
| 1 | SELF_AUTO 각성 속성값 DPS 반영 | ✅ 완료 |
| 2 | 정령 속성 필터 | ✅ 의도된 동작 |
| 3 | 스탯 API / DPS 불일치 | ✅ 완료 |
| 4 | 각성 명왕 중첩 + 편성 제한 | ✅ 완료 |
| 5 | natureValue 동기화 | ✅ 완료 |
