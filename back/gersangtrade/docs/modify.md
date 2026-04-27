카탈로그 레이어 — 신규 추가

MercenaryTrait  (특성 트리 노드 정의)
- id
- mercenaryId (FK → Mercenary)   ← 사천왕/주인공 등 특성 트리 보유 카테고리만
- name                           ← 특성 노드명
- statType (StatType Enum)       ← RESIST_PIERCE / ELEMENT_VALUE / DAMAGE_BONUS 등
- effectValue (Float)            ← 해당 노드 선택 시 증가값
- parentTraitId (FK → MercenaryTrait, nullable)  ← 트리 구조 (선행 노드)

MercenaryPassive  (전설장수 패시브 정의)
- id
- mercenaryId (FK → Mercenary)   ← LEGENDARY_GENERAL 카테고리만
- name
- statType (StatType Enum)
- effectValue (Float)

  ---
유저 덱 레이어 — 전체 구조

UserDeck
- id
- userId (FK → User)
- isActive (boolean)
- attrXValue (Integer)    ← 저장 시점 계산 캐싱
- totalResDown (Integer)  ← 저장 시점 계산 캐싱
- createdAt               ← 불변 스냅샷이므로 updatedAt 없음

UserDeckMember  (용병 슬롯 — 주인공 포함)
- id
- deckId (FK → UserDeck)
- mercenaryId (FK → Mercenary)  ← PROTAGONIST도 Mercenary로 통일
- slotIndex (Integer)           ← 0~11

UserDeckMemberEquip  (용병별 장비 상세)
- id
- deckMemberId (FK → UserDeckMember)
- equipmentSetId (FK → EquipmentSet, nullable)   ← 전설장수 세트
- enhanceLevel (Integer, nullable)               ← 전설장수 강화 수치
- setPieceCount (Integer, nullable)              ← 전설장수 세트 피스 수
- hasAffinity (boolean)                          ← 전설장수 인연 여부
- equipmentItemId (FK → EquipmentItem, nullable) ← 명왕 개별 장비

UserDeckMemberTrait  (선택한 특성 트리 노드)
- deckMemberId (FK → UserDeckMember)
- traitId (FK → MercenaryTrait)

UserDeckMemberPassive  (선택한 패시브)
- deckMemberId (FK → UserDeckMember)
- passiveId (FK → MercenaryPassive)

  ---
카테고리별 사용 엔티티 매핑

┌─────────────────────────────────┬────────┬──────────────────────┬─────────────────────────────────┬─────────┐
│            카테고리             │ Member │        Equip         │              Trait              │ Passive │
├─────────────────────────────────┼────────┼──────────────────────┼─────────────────────────────────┼─────────┤
│ PROTAGONIST                     │ ✅     │ ❌                   │ ✅ (국적 이미 Mercenary.nation) │ ❌      │
├─────────────────────────────────┼────────┼──────────────────────┼─────────────────────────────────┼─────────┤
│ FOUR_HEAVENLY_KINGS / AWAKENING │ ✅     │ ❌                   │ ✅                              │ ❌      │
├─────────────────────────────────┼────────┼──────────────────────┼─────────────────────────────────┼─────────┤
│ MYEONG_KING / AWAKENING         │ ✅     │ ✅ (equipmentItemId) │ ❌                              │ ❌      │
├─────────────────────────────────┼────────┼──────────────────────┼─────────────────────────────────┼─────────┤
│ LEGENDARY_GENERAL               │ ✅     │ ✅ (setId·강화·인연) │ ❌                              │ ✅      │
├─────────────────────────────────┼────────┼──────────────────────┼─────────────────────────────────┼─────────┤
│ 나머지 (신수·흉수·장수류 등)    │ ✅     │ ❌                   │ ❌                              │ ❌      │
└─────────────────────────────────┴────────┴──────────────────────┴─────────────────────────────────┴─────────┘

  ---
합산 흐름

UserDeckService.calculateTotalStats(deckId):

    for each UserDeckMember:
      1. Mercenary.MercenaryStat → RESIST_PIERCE, ELEMENT_VALUE 기본값 합산
      2. UserDeckMemberEquip → 세트 보너스, 장비 스탯 합산
      3. UserDeckMemberTrait → traitId의 effectValue 합산
      4. UserDeckMemberPassive → passiveId의 effectValue 합산

    → 최종 attrXValue, totalResDown → UserDeck에 캐싱 저장

  ---
주의할 점 하나

MercenaryTrait와 MercenaryPassive는 카탈로그 데이터입니다. 크롤링으로 수집하기 어려운 구조(게임 내 UI 기반)이므로, 관리자가 직접 입력하거나 Flyway seed로 적재해야 합니다. 이 초기 데이터 입력 작업이 이 기능에서 가장 큰 선행
작업입니다.

이 구조로 personalization_cache_design.md와 recommend-plan.md 두 문서를 업데이트할까요?

Mercenary-characteristic-crawling.md 파일을 보고 크롤링이 되는건지 확인해줘

Searched for 1 pattern, read 1 file (ctrl+o to expand)

● 크롤링 가능합니다. 다만 이 문서를 보고 설계에서 수정·추가해야 할 부분이 몇 가지 생겼습니다.
                                                    
---                                                                                                                                                                                                                              
크롤링 가능 여부 — 확인

gerniverse가 SSR이므로 Jsoup으로 가능합니다. 이미 구현된 MercenaryDetailWriter에 RSC payload 파싱 로직만 추가하면 됩니다. Playwright 불필요.
                                                                                                                                                                                                                                   
---                                                       
이 문서로 인한 설계 변경 사항

1. 엔티티 이름 변경

제가 제안했던 MercenaryTrait는 이 문서에서 이미 MercenaryCharacteristic으로 정의되어 있습니다. 이름을 맞춰야 합니다.

┌─────────────────────┬──────────────────────────────┐
│      이전 제안      │           변경 후            │
├─────────────────────┼──────────────────────────────┤
│ MercenaryTrait      │ MercenaryCharacteristic      │
├─────────────────────┼──────────────────────────────┤
│ UserDeckMemberTrait │ UserDeckMemberCharacteristic │
└─────────────────────┴──────────────────────────────┘

  ---
2. 레벨 테이블 추가 필요

특성 하나에 레벨 1~5가 있고, label도 여러 개입니다. 제가 제안한 구조에는 이 깊이가 없었습니다.

MercenaryCharacteristic (1)
└── MercenaryCharacteristicLevel (N)  ← label × level 1~5 조합

그리고 UserDeckMemberCharacteristic에 선택한 레벨이 필요합니다.

UserDeckMemberCharacteristic
- deckMemberId (FK → UserDeckMember)
- characteristicId (FK → MercenaryCharacteristic)
- selectedLevel (Integer, 1~5)  ← 이게 빠져있었음

  ---
3. 합산 계산 시 파싱 문제

MercenaryCharacteristicLevel.amount가 "20%" 같은 문자열로 저장됩니다. 덱 합산 계산(calculateTotalStats)에서 이 값을 쓰려면 파싱이 필요합니다.

두 가지 선택지가 있습니다.

┌──────────────┬──────────────────────────────────────────────────────────┬────────────────────────────────────────┐
│     방식     │                           설명                           │                  비고                  │
├──────────────┼──────────────────────────────────────────────────────────┼────────────────────────────────────────┤
│ 저장 시 파싱 │ 크롤링 시 "20%" → 20.0 Float으로 변환해 별도 컬럼에 저장 │ 계산 편리, 단위 정보 별도 보관 필요    │
├──────────────┼──────────────────────────────────────────────────────────┼────────────────────────────────────────┤
│ 조회 시 파싱 │ String 그대로 저장, 계산 시 파싱                         │ 문서 현재 방식, 계산마다 파싱 오버헤드 │
└──────────────┴──────────────────────────────────────────────────────────┴────────────────────────────────────────┘

amountValue (Float) 컬럼을 하나 더 추가하는 게 낫습니다.

  ---
4. label → StatType 매핑 미정

label이 "풍극진멸 데미지", "생명력 증가", "타격저항력" 같은 자연어입니다. 덱 합산에서 이걸 StatType(RESIST_PIERCE, ELEMENT_VALUE 등)으로 매핑하는 방법이 없습니다.

MercenaryCharacteristicLevel에 statType (StatType Enum, nullable) 컬럼 추가를 권장합니다. 크롤링 시 label 문자열을 보고 매핑 규칙으로 자동 채우거나, null이면 관리자가 수동 보정하는 방식입니다.

  ---
5. 전설장수 패시브 — 이 문서에 없음

이 문서는 특성(characteristics)만 다루고 있고, MercenaryPassive는 별도입니다. gerniverse에 패시브 데이터가 있는지, 있다면 같은 RSC payload에 포함되는지 확인이 필요합니다.

  ---
최종 카탈로그 엔티티 구조 (확정판)

Mercenary (1)
└── MercenaryCharacteristic (N)          ← 크롤링 가능 확인
└── MercenaryCharacteristicLevel (N) ← label × level 1~5
└── MercenaryPassive (N)                 ← gerniverse 확인 필요

UserDeckMember (1)
└── UserDeckMemberCharacteristic (N)     ← characteristicId + selectedLevel
└── UserDeckMemberPassive (N)            ← passiveId (패시브 구조 확인 후)
└── UserDeckMemberEquip (N)

전설장수 패시브 데이터가 gerniverse에 있는지 확인되면 전체 구조가 확정됩니다. 확인해볼 수 있는 gerniverse URL이 있으신가요?

❯ 1,2,3 네가 권장하는대로 수정해줘 4. 조금더 설명해줘 잘 이해못했어. 5. mercenary-characteristic-crawliing.md 파일 수정했으니 다시 확인해줘

스킬
"'군신 노부츠나' 의 '습격' 스킬(마법계열)은 힘에 영향을 받으며 9등급 이하 몬스터에게 스턴 피해를 입힙니다.\"]},{\"id\":\"434e83c2-9684-98c0-c5e5-be241933b5c0\",\"mp\":null,\"key\":\"wlgnl\",\"name\":\"지휘\",\"isActive\":false,\"created_at\":\"2025-01-24T13:42:10+00:00\",\"skill_type\":\"BASIC\",\"description\":\"아군의 공격력을 상승시켜준다.\",\"target_range\":\"ground_only\",\"mercenary_key\":\"dokugawa\",\"resistanceType\":null,\"skill_explanation\":null}],\"used_by\":[],\"npc_info\":{\"id\":\"1ea228d3-e183-96ae-6de6-8b3bdadcc1dd\",\"key\":\"ehtk_qkrdudrka\",\"name\":\"도사_박영감\",\"type\":\"advanced\",\"location\":\"저잣거리 비밀저택\",\"created_at\":\"2024-12-02T15:16:14+00:00\",\"image_path\":\"npc/ehtk_qkrdudrka\",\"dungeon_key\":null,\"making_list\":null,\"category_key\":null,\"image_version\":1,\"location_detail\":null},\"materials\":{\"items\":[{\"c\":20,\"d\":1,\"id\":\"8001e90f-5853-e423-64b4-6144dfa86357\",\"key\":\"cjfrhlfldmlghflqud\",\"name\":\"철괴리의호리병\",\"only\":null,\"type\":\"drop\",\"order\":null,\"price\":500000,\"skill\":null,\"stats\":null,\"nature\":\"none\",\"is_trade\":true,\"material\":null,\"sub_type\":null,\"created_at\":\"2024-10-29T13:17:14+00:00\",\"how_to_get\":[{\"list\":\"cjfrhlfl-aud|dkrtjscjfrhlfl-vnd\",\"category\":\"몬스터\"}],\"image_path\":\"item/drop/cjfrhlfldmlghflqud\",\"description\":null,\"abbreviation\":[],\"artisan_type\":\"-\",\"isComingSoon\":false,\"item_set_key\":null,\"requirements\":null,\"only-category\":null,\"enchantment_name\":null,\"enhancement_level\":0,\"package_item_list\":null,\"enchantment_set_key\":null,\"isExistVillageStore\":false,\"is_integrated_production\":false},

패시브
\"원거리 넓은 범위의 적에게 닌자가 습격 피해를 입힌다.\",\"target_range\":\"ground_only\",\"mercenary_key\":\"nobootsuna\",\"resistanceType\":\"M\",\"skill_explanation\":[\"'군신 노부츠나' 의 '습격' 스킬(마법계열)은 힘에 영향을 받으며 9등급 이하 몬스터에게 스턴 피해를 입힙니다.\"]},{\"id\":\"434e83c2-9684-98c0-c5e5-be241933b5c0\",\"mp\":null,\"key\":\"wlgnl\",\"name\":\"지휘\",\"isActive\":false,\"created_at\":\"2025-01-24T13:42:10+00:00\",\"skill_type\":\"BASIC\",\"description\":\"아군의 공격력을 상승시켜준다.\",\"target_range\":\"ground_only\",\"mercenary_key\":\"dokugawa\",\"resistanceType\":null,\"skill_explanation\":null}],\"used_by\":[],\"npc_info\":{\"id\":\"1ea228d3-e183-96ae-6de6-8b3bdadcc1dd\",\"key\":\"ehtk_qkrdudrka\",\"name\":

\"기습\",\"level\":[{\"label\":\"습격 피해량\",\"amount\":[\"10%\",\"20%\",\"30%\",\"40%\",\"50%\",\"60%\",\"70%\",\"80%\",\"90%\",\"100%\"]},{\"label\":\"3배 피해 확률\",\"amount\":[\"4%\",\"8%\",\"12%\",\"16%\",\"20%\",\"24%\",\"28%\",\"32%\",\"36%\",\"40%\"]}],\"point\":1,\"created_at\":\"2024-09-30T14:23:16+00:00\",\"description\":\"습격 피해가 증가하고, 3배 피해를 입힐 확률이 증가합니다.\",\"mercenary_key\":\"nobootsuna\",\"required_characteristic_key\":null},{\"id\":\"b38cb73d-546b-e80d-2c26-7dccc75542b8\",\"key\":\"rurfuf\",\"name\":\"격렬\",\"level\":[{\"label\":\"추가 공격력\",\"amount\":[\"50\",\"100\",\"150\",\"200\",\"250\",\"300\",\"350\",\"400\",\"450\",\"500\"]}],\"point\":1,\"created_at\":\"2024-09-30T14:23:17+00:00\",\"description\":\"지휘 스킬의 공격력이 추가로 증가합니다.\",\"mercenary_key\":\"nobootsuna\",\"required_characteristic_key\":null}],\"exclusive_equipment\":[{\"id\":\"0eab8805-2523-4526-bf65-7957f79afb0b\",\"key\":\"shqncmskdmlckd9\",\"name\":\"

간략히 보기
