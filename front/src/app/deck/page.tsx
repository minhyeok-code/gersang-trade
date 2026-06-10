'use client';

import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { createPortal } from 'react-dom';
import Link from 'next/link';
import {
  api,
  getToken,
  type MercenaryDto,
  type MonsterDto,
  type DeckDetailDto,
  type EquipmentItemDto,
  type DpsResultDto,
  type ItemSearchResult,
  type MemberCharacteristicDto,
  type DeckEffectCatalogDto,
  type DeckEffectUpdateBody,
  type RitualDto,
  type SlotRitualDto,
} from '@/lib/api';
import {
  buildDeckEquipmentDisplayItems,
  equipmentItemKey,
  equipmentItemToSearchResult,
  isExclusiveForMercenary,
} from '@/lib/equipment';
import {
  characteristicSelectionCost,
  usedMemberCharacteristicPoints,
} from '@/lib/mercenaryCharacteristicUi';
import { DeckEffectPanel } from '@/components/deck/DeckEffectPanel';
import { Plus, Trash2, Sword, Shield, Search, X, ChevronDown, ChevronUp, Zap, Sparkles, Timer } from 'lucide-react';

// ══════════ 상수 ══════════

const RITUAL_SLOT = 'RITUAL';
const NORMAL_SLOTS = ['CHARM', 'HELMET', 'GLOVES', 'ARMOR', 'BELT', 'WEAPON', 'SHOES', 'RING_1', 'RING_2'];
const APP_SLOTS = ['APP_SPIRIT', 'APP_EARRING', 'APP_HELMET', 'APP_NECKLACE', 'APP_ARMOR', 'APP_GREAVES', 'APP_WEAPON', 'APP_BRACELET'];
const ALL_EQUIP_SLOTS = [...NORMAL_SLOTS, ...APP_SLOTS];
const DPS_CALC_DEBOUNCE_MS = 400;
const NORMAL_SLOT_ROWS: (string | null)[][] = [
  ['CHARM', RITUAL_SLOT],
  ['HELMET', 'GLOVES'],
  ['ARMOR', 'BELT'],
  ['WEAPON', 'SHOES'],
  ['RING_1', 'RING_2'],
];
const APP_SLOT_ROWS = [
  ['APP_SPIRIT', 'APP_EARRING'],
  ['APP_HELMET', 'APP_NECKLACE'],
  ['APP_ARMOR', 'APP_GREAVES'],
  ['APP_WEAPON', 'APP_BRACELET'],
];

const SLOT_LABEL: Record<string, string> = {
  HELMET: '투구', ARMOR: '갑옷', WEAPON: '무기', SHOES: '신발', GLOVES: '장갑',
  BELT: '요대', CHARM: '신수부', RING_1: '반지', RING_2: '반지',
  [RITUAL_SLOT]: '주술',
  APP_SPIRIT: '기운', APP_HELMET: '외투구', APP_ARMOR: '외갑옷', APP_WEAPON: '외무기',
  APP_WAR_GOD: '전신', APP_EARRING: '귀걸이', APP_NECKLACE: '목걸이',
  APP_BRACELET: '팔찌', APP_GREAVES: '각반',
};

const ELEMENT_LABELS: Record<string, string> = {
  FIRE: '화', WATER: '수', THUNDER: '뇌', WIND: '풍', EARTH: '토',
};

const ELEMENT_COLORS: Record<string, string> = {
  FIRE: '#DC2626',
  WATER: '#2563EB',
  THUNDER: '#D97706',
  WIND: '#16A34A',
  EARTH: '#92400E',
};

// ══════════ 헬퍼 ══════════

type DeckMember = NonNullable<DeckDetailDto['members']>[number];
type MemberStats = Awaited<ReturnType<typeof api.getDeckMemberStats>>;

/** 장비·특성 등 설정이 있으면 제거 전 확인 */
async function memberNeedsRemovalConfirm(deckId: number, member: DeckMember) {
  if (member.slots.length > 0) return true;
  try {
    const chars = await api.getDeckMemberCharacteristics(deckId, member.id);
    return chars.characteristics.some((c) => (c.selectedLevel ?? 0) > 0);
  } catch {
    return false;
  }
}

async function confirmMemberRemoval(deckId: number, member: DeckMember, mercenaryName: string) {
  if (!(await memberNeedsRemovalConfirm(deckId, member))) return true;
  return confirm(`${mercenaryName}을(를) 정말 삭제하시겠습니까?`);
}

const STAT_LABEL: Record<string, string> = {
  ELEMENT_VALUE: '속성값',
  ELEMENT_PIERCE: '속성깎',
  RESIST_PIERCE: '저항깎',
  MIN_POWER: '최소공격력',
  MAX_POWER: '최대공격력',
  ATTACK_POWER: '공격력',
  STRENGTH: '힘',
  VITALITY: '생명력',
  DEXTERITY: '민첩',
  INTELLECT: '지력',
  DEFENSE: '방어',
  SIGHT: '시야',
  HIT_RATE: '명중률',
  CRITICAL_CHANCE: '크리티컬확률',
  MAGIC_RESISTANCE: '마법저항',
  HITTING_RESISTANCE: '타격저항',
  MAGIC_RESISTANCE_PIERCE: '마법저항깎',
  HITTING_RESISTANCE_PIERCE: '타격저항깎',
  DAMAGE_PERCENT: '데미지증가',
  SKILL_DAMAGE_PERCENT: '스킬데미지증가',
  FIELD_MOVE_SPEED: '필드이동속도',
  ALL_STAT: '모든능력치',
  CRITICAL_RATE: '치명타확률',
  CRITICAL_DAMAGE: '치명타피해',
  MIN_DAMAGE: '최소데미지',
  MAX_DAMAGE: '최대데미지',
  ATTACK_SPEED: '공격속도',
  MOVE_SPEED: '이동속도',
  HP_RECOVERY: '체력회복',
  MP_RECOVERY: '마력회복',
  DAMAGE_PERCENT_GROUND: '지상데미지증가',
  DAMAGE_PERCENT_AIR: '공중데미지증가',
  STUN_DURATION: '기절시간',
  SKILL_RANGE: '사거리',
};

function mercenaryCategory(merc?: Pick<MercenaryDto, 'category'> | null) {
  return merc?.category ?? '미분류';
}

function isHeroMercenary(merc?: Pick<MercenaryDto, 'category'> | null) {
  const category = mercenaryCategory(merc);
  return category.includes('주인공') || category === 'PROTAGONIST';
}

function isFourKingMercenary(merc?: Pick<MercenaryDto, 'category'> | null) {
  const category = mercenaryCategory(merc);
  return category.includes('사천왕') || category === 'FOUR_HEAVENLY_KINGS' || category === 'FOUR_HEAVENLY_KINGS_AWAKENING';
}

function isMyeongwangMercenary(merc?: Pick<MercenaryDto, 'category'> | null) {
  const category = mercenaryCategory(merc);
  return category.includes('명왕') || category === 'MYEONG_KING' || category === 'MYEONG_KING_AWAKENING';
}

/** 부동명왕(EARTH)은 명왕 2명 제한에서 제외 */
function isNonEarthMyeongwangMercenary(merc?: Pick<MercenaryDto, 'category' | 'element'> | null) {
  return isMyeongwangMercenary(merc) && merc?.element !== 'EARTH';
}

function isAwakenedMyeongwangMercenary(merc?: Pick<MercenaryDto, 'category'> | null) {
  const category = mercenaryCategory(merc);
  return category.includes('각성명왕') || category === 'MYEONG_KING_AWAKENING';
}

function isMyeongwangPickerCategory(category: string) {
  return category.includes('명왕');
}

function myeongwangVariantLabel(merc: MercenaryDto): '일반' | '각성' | null {
  if (!isMyeongwangMercenary(merc)) return null;
  return isAwakenedMyeongwangMercenary(merc) ? '각성' : '일반';
}

function findMyeongwangSameNatureConflict(merc: MercenaryDto, deck: MercenaryDto[]) {
  if (!isMyeongwangMercenary(merc) || !merc.element || merc.element === 'NONE') return undefined;
  return deck.find(
    (m) => m.id !== merc.id && isMyeongwangMercenary(m) && m.element === merc.element
  );
}

const MYEONGWANG_RULES_LINES = [
  '같은 속성 계열은 일반 / 각성 중 1명만 선택',
  '부동명왕(토) 제외, 명왕·각성명왕 합산 최대 2명',
];

/** 명왕 편성 규칙 — ! 아이콘 + 호버/포커스 툴팁 */
function MyeongwangRulesHint() {
  return (
    <div className="relative inline-flex group">
      <button
        type="button"
        aria-label="명왕 편성 규칙"
        className="w-5 h-5 rounded-full flex items-center justify-center text-[11px] font-bold leading-none shrink-0"
        style={{
          border: '1.5px solid var(--brown)',
          color: 'var(--brown)',
          background: 'var(--card)',
        }}
      >
        !
      </button>
      <div
        role="tooltip"
        className="pointer-events-none absolute right-0 top-full z-20 mt-2 w-max max-w-[260px] rounded-lg px-3 py-2 text-xs leading-relaxed opacity-0 invisible translate-y-1 transition-all group-hover:opacity-100 group-hover:visible group-hover:translate-y-0 group-focus-within:opacity-100 group-focus-within:visible group-focus-within:translate-y-0"
        style={{
          background: 'var(--text)',
          color: 'var(--beige)',
          boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
        }}
      >
        <p className="font-semibold mb-1">명왕 편성 규칙</p>
        <ul className="list-disc list-inside space-y-0.5 text-[11px] opacity-90">
          {MYEONGWANG_RULES_LINES.map((line) => (
            <li key={line}>{line}</li>
          ))}
        </ul>
      </div>
    </div>
  );
}

function isLegendaryMercenary(merc?: Pick<MercenaryDto, 'category'> | null) {
  const category = mercenaryCategory(merc);
  return category.includes('전설장수') || category === 'LEGENDARY_GENERAL';
}

const ELEMENT_SORT_ORDER = ['FIRE', 'WATER', 'THUNDER', 'WIND', 'EARTH'] as const;

/** 덱 용병 선택 탭 표시 순서 */
const DECK_MERCENARY_CATEGORY_ORDER = ['주인공', '각성사천왕', '사천왕', '각성명왕', '명왕', '전설장수', '기타'];

const MISC_CATEGORY = '기타';
const MISC_MERCENARY_NAMES = ['원숭이 주술사', '코끼리 주술사', '염력사', '음양사'];

/** 헤더(76px) 아래 sticky 여백 */
const DECK_STICKY_TOP = 88;

function sortDeckMercenaryCategories(categories: string[]) {
  return [...categories].sort((a, b) => {
    const aOrder = DECK_MERCENARY_CATEGORY_ORDER.indexOf(a);
    const bOrder = DECK_MERCENARY_CATEGORY_ORDER.indexOf(b);
    return (aOrder === -1 ? 999 : aOrder) - (bOrder === -1 ? 999 : bOrder);
  });
}

/** 덱 카드 표시 슬롯: 주인공(1) → 사천왕/각성사천왕(2) → 각성명왕/명왕 → 전설장수 → 기타 */
function arrangeDeckDisplaySlots(
  members: DeckMember[],
  mercenaryById: Map<number, MercenaryDto>,
  maxMembers: number,
): (DeckMember | null)[] {
  const used = new Set<number>();

  const takeFirst = (predicate: (merc: MercenaryDto) => boolean): DeckMember | null => {
    for (const member of members) {
      if (used.has(member.id)) continue;
      const merc = mercenaryById.get(member.mercenaryId);
      if (merc && predicate(merc)) {
        used.add(member.id);
        return member;
      }
    }
    return null;
  };

  const takeAll = (
    predicate: (merc: MercenaryDto) => boolean,
    sortFn?: (a: DeckMember, b: DeckMember) => number,
  ): DeckMember[] => {
    const picked = members.filter((member) => {
      if (used.has(member.id)) return false;
      const merc = mercenaryById.get(member.mercenaryId);
      return merc != null && predicate(merc);
    });
    if (sortFn) picked.sort(sortFn);
    picked.forEach((member) => used.add(member.id));
    return picked;
  };

  const myeongwangSort = (a: DeckMember, b: DeckMember) => {
    const ma = mercenaryById.get(a.mercenaryId);
    const mb = mercenaryById.get(b.mercenaryId);
    const aAwake = isAwakenedMyeongwangMercenary(ma) ? 0 : 1;
    const bAwake = isAwakenedMyeongwangMercenary(mb) ? 0 : 1;
    if (aAwake !== bAwake) return aAwake - bAwake;
    return a.mercenaryName.localeCompare(b.mercenaryName, 'ko');
  };

  const legendarySort = (a: DeckMember, b: DeckMember) => {
    const ma = mercenaryById.get(a.mercenaryId)!;
    const mb = mercenaryById.get(b.mercenaryId)!;
    const byElement = elementSortIndex(ma.element) - elementSortIndex(mb.element);
    if (byElement !== 0) return byElement;
    return a.mercenaryName.localeCompare(b.mercenaryName, 'ko');
  };

  const slots: (DeckMember | null)[] = [
    takeFirst(isHeroMercenary),
    takeFirst(isFourKingMercenary),
    ...takeAll(isMyeongwangMercenary, myeongwangSort),
    ...takeAll(isLegendaryMercenary, legendarySort),
    ...members.filter((member) => !used.has(member.id)),
  ];

  while (slots.length < maxMembers) slots.push(null);
  return slots.slice(0, maxMembers);
}

function elementSortIndex(element?: string | null) {
  if (!element) return ELEMENT_SORT_ORDER.length;
  const idx = ELEMENT_SORT_ORDER.indexOf(element as (typeof ELEMENT_SORT_ORDER)[number]);
  return idx >= 0 ? idx : ELEMENT_SORT_ORDER.length + 1;
}

/** 전설장수 — 속성(화→수→뇌→풍→토) 순, 동일 속성은 이름순 */
function sortMercenariesByElement(mercenaries: MercenaryDto[]) {
  return [...mercenaries].sort((a, b) => {
    const byElement = elementSortIndex(a.element) - elementSortIndex(b.element);
    if (byElement !== 0) return byElement;
    return a.name.localeCompare(b.name, 'ko');
  });
}

function isLegendaryPickerCategory(category: string) {
  return category.includes('전설');
}

function isMiscMercenary(merc: MercenaryDto) {
  return MISC_MERCENARY_NAMES.includes(merc.name);
}

function displayMercenaryCategory(merc: MercenaryDto): string {
  if (isMiscMercenary(merc)) return MISC_CATEGORY;
  return mercenaryCategory(merc);
}

function isDeckSelectableMercenary(merc: MercenaryDto) {
  return isHeroMercenary(merc)
    || isFourKingMercenary(merc)
    || isMyeongwangMercenary(merc)
    || isLegendaryMercenary(merc)
    || isMiscMercenary(merc);
}

function roundedDamageShare(value?: number) {
  return value != null ? Math.round(value * 10) / 10 : null;
}

function formatStatType(statType: string) {
  return STAT_LABEL[statType] ?? statType;
}

function statValue(
  stats: MemberStats | null,
  section: 'baseStats' | 'equipStats' | 'setEffectStats' | 'characteristicStats' | 'partyCharacteristicStats' | 'enemyDebuffStats' | 'ritualStats' | 'ritualSetEffectStats' | 'deckBuffStats' | 'levelBonusStats' | 'bonusStats' | 'protagonistBuffStats' | 'awakenedMyeongwangBuffStats' | 'partyItemBuffStats' | 'myungwangTransferStats' | 'totalStats',
  statType: string
) {
  return stats?.[section]?.find((s) => s.statType === statType)?.value ?? 0;
}


function equipmentItemId(item: EquipmentItemDto) {
  return equipmentItemKey(item);
}

function toSearchResult(item: EquipmentItemDto, selectedSlot: string): ItemSearchResult {
  return equipmentItemToSearchResult(item, selectedSlot);
}

function characteristicUsedPoints(characteristics: MemberCharacteristicDto | null) {
  if (!characteristics) return 0;
  return usedMemberCharacteristicPoints(characteristics.characteristics);
}

function characteristicMaxPoints(characteristics: MemberCharacteristicDto | null) {
  return characteristics?.maxCharacteristicPoints ?? characteristics?.maxPoints ?? null;
}

/** 장비 탭에 표시할 스탯 (장비 합산만) */
const EQUIP_TAB_STATS = [
  'STRENGTH', 'DEXTERITY', 'VITALITY', 'INTELLECT',
  'ELEMENT_VALUE', 'DAMAGE_PERCENT',
] as const;

const BONUS_AMOUNT_PRESETS = [0, 300, 500, 700, 900, 1000] as const;

const BONUS_TARGET_LABEL: Record<'MAIN_STAT' | 'VITALITY', string> = {
  MAIN_STAT: '주스탯',
  VITALITY: '생명력',
};

function formatStatValue(statType: string, value: number, statUnit?: 'FLAT' | 'PERCENT') {
  if (statUnit === 'PERCENT' || statType === 'DAMAGE_PERCENT' || statType === 'SKILL_DAMAGE_PERCENT') {
    return `+${value}%`;
  }
  return `+${value}`;
}

const PERCENT_STAT_TYPES = new Set([
  'DAMAGE_PERCENT', 'SKILL_DAMAGE_PERCENT', 'MAGIC_RESISTANCE', 'HITTING_RESISTANCE',
  'CRITICAL_CHANCE', 'ATTACK_SPEED', 'DAMAGE_PERCENT_GROUND', 'DAMAGE_PERCENT_AIR',
  'CRITICAL_DAMAGE', 'CRITICAL_RATE', 'HIT_RATE',
]);

const COMPOUND_STAT_LABEL: Record<string, string> = {
  'DAMAGE_PERCENT_FIRE':    '화속성 데미지증가',
  'DAMAGE_PERCENT_WIND':    '풍속성 데미지증가',
  'DAMAGE_PERCENT_WATER':   '수속성 데미지증가',
  'DAMAGE_PERCENT_THUNDER': '뇌속성 데미지증가',
  'DAMAGE_PERCENT_EARTH':   '지속성 데미지증가',
};

// compound key (예: DAMAGE_PERCENT_FIRE) → 기반 StatType (예: DAMAGE_PERCENT)
function baseStatFromCompoundKey(statKey: string): string {
  for (const base of Object.keys(STAT_LABEL)) {
    if (statKey.startsWith(base + '_')) return base;
  }
  return statKey;
}

type LevelEntryLike = { label?: string | null; amount?: string | null; amountValue?: number | null; statType?: string | null; element?: string | null };

function formatLevelEntryAmount(entry: LevelEntryLike): string {
  if (entry.label && entry.amount) return `${entry.label} ${entry.amount}`;
  if (entry.amount) return entry.amount;
  if (entry.amountValue == null || !entry.statType) return '';
  const compoundKey = entry.element && entry.element !== 'NONE' ? `${entry.statType}_${entry.element}` : null;
  const label = (compoundKey && COMPOUND_STAT_LABEL[compoundKey]) ?? STAT_LABEL[entry.statType] ?? entry.statType;
  const v = entry.amountValue;
  const sign = v >= 0 ? '+' : '';
  const valueStr = PERCENT_STAT_TYPES.has(entry.statType) ? `${sign}${v}%` : `${sign}${v}`;
  return `${label} ${valueStr}`;
}

function equipmentTabStatTotal(stats: MemberStats | null, statType: string) {
  if (!stats) return 0;
  return (
    statValue(stats, 'equipStats', statType)
    + statValue(stats, 'setEffectStats', statType)
    + statValue(stats, 'ritualStats', statType)
    + statValue(stats, 'ritualSetEffectStats', statType)
  );
}

function formatEquipAttackPower(stats: MemberStats | null) {
  if (!stats) return '—';
  const min = statValue(stats, 'equipStats', 'MIN_POWER');
  const max = statValue(stats, 'equipStats', 'MAX_POWER');
  if (min === 0 && max === 0) return '—';
  if (min === max) return String(min);
  return `${min}~${max}`;
}

type SetupTab = 'equipment' | 'characteristic' | 'stats';

type CharacteristicItem = MemberCharacteristicDto['characteristics'][number];

type StatContribution = { label: string; value: number };

function isSelectableCharacteristic(c: CharacteristicItem) {
  return c.applyType !== 'SELF_AUTO' && c.applyType !== 'ALLY_AUTO';
}

function characteristicMaxLevel(c: CharacteristicItem) {
  const fromLevels = c.levels.map((l) => l.level);
  return fromLevels.length > 0 ? Math.max(...fromLevels) : 5;
}

function levelsAtTier(c: CharacteristicItem, tier: number) {
  return c.levels.filter((l) => l.level === tier);
}

function layoutCharacteristicGrid(characteristics: CharacteristicItem[]) {
  const normal = characteristics.filter(isSelectableCharacteristic);
  const lower = normal.filter((c) => (c.point ?? 1) === 1);
  const upper = normal.filter((c) => c.point === 2);

  const findUpper = (lowerKey?: string | null) => {
    if (!lowerKey) return null;
    return upper.find((c) => c.requiredCharacteristicKey === lowerKey) ?? null;
  };

  const grid = [
    { slot: 'top-left', lower: lower[0] ?? null, upper: findUpper(lower[0]?.key) },
    { slot: 'top-right', lower: lower[1] ?? null, upper: findUpper(lower[1]?.key) },
  ];

  const usedUpperIds = new Set(
    grid.flatMap((g) => (g.upper ? [g.upper.characteristicId] : []))
  );
  const orphanUpper = upper.filter((c) => !usedUpperIds.has(c.characteristicId));
  const overflowLower = lower.slice(2);
  const overflow = [...overflowLower, ...orphanUpper];

  return { grid, overflow, auto: characteristics.filter((c) => !isSelectableCharacteristic(c)) };
}

// ALL_STAT 값이 분배될 기본 스탯 4종
const ALL_STAT_TARGETS = ['STRENGTH', 'DEXTERITY', 'VITALITY', 'INTELLECT'] as const;

// 스탯 출처(우측) 패널에서 집계할 섹션 목록 (기본·레벨·보너스 제외)
const SOURCE_SECTIONS = [
  'equipStats', 'setEffectStats', 'characteristicStats', 'partyCharacteristicStats',
  'enemyDebuffStats', 'ritualStats', 'ritualSetEffectStats', 'deckBuffStats',
  'protagonistBuffStats', 'awakenedMyeongwangBuffStats', 'partyItemBuffStats', 'myungwangTransferStats',
] as const;

// 기본+레벨+장비+보너스 합산. ALL_STAT → 힘/민첩/생명력/지력에 분배
function computePrimaryStats(stats: MemberStats | null): Map<string, number> {
  const map = new Map<string, number>();
  const sections = ['baseStats', 'levelBonusStats', 'equipStats', 'bonusStats'] as const;
  for (const section of sections) {
    stats?.[section]?.forEach((s) => {
      map.set(s.statType, (map.get(s.statType) ?? 0) + s.value);
    });
  }
  const allStatVal = map.get('ALL_STAT') ?? 0;
  if (allStatVal !== 0) {
    map.delete('ALL_STAT');
    for (const t of ALL_STAT_TARGETS) {
      map.set(t, (map.get(t) ?? 0) + allStatVal);
    }
  }
  return map;
}

function displayPrimaryStatTypes(stats: MemberStats | null): string[] {
  return Array.from(computePrimaryStats(stats).entries())
    .filter(([, v]) => v !== 0)
    .map(([k]) => k);
}

// 아이템·덱 효과·명왕 이전만 합산 (기본·레벨·보너스 제외), ALL_STAT 분배 포함
function sourceStatTotal(stats: MemberStats | null, statType: string): number {
  if (!stats) return 0;
  const includeAllStat = (ALL_STAT_TARGETS as readonly string[]).includes(statType);
  const sectionSum = SOURCE_SECTIONS.reduce((sum, section) => {
    const val = statValue(stats, section, statType);
    const allVal = includeAllStat ? statValue(stats, section, 'ALL_STAT') : 0;
    return sum + val + allVal;
  }, 0);
  const lgAllyVal = stats.lgAllyDetails
    ?.filter((e) => baseStatFromCompoundKey(e.statKey) === statType)
    .reduce((s, e) => s + e.value, 0) ?? 0;
  return sectionSum + lgAllyVal;
}

function displaySourceStatTypes(stats: MemberStats | null): string[] {
  const keys = new Set<string>();
  for (const section of SOURCE_SECTIONS) {
    stats?.[section]?.forEach((s) => keys.add(s.statType));
  }
  stats?.ritualSetEffects?.forEach((s) => keys.add(s.statType));
  stats?.myungwangTransferDetails?.forEach((s) => keys.add(s.statType));
  stats?.lgAllyDetails?.forEach((e) => keys.add(baseStatFromCompoundKey(e.statKey)));
  if (keys.has('ALL_STAT')) {
    keys.delete('ALL_STAT');
    for (const t of ALL_STAT_TARGETS) keys.add(t);
  }
  return Array.from(keys).filter((k) => sourceStatTotal(stats, k) !== 0);
}

// 스탯 출처 기여 내역: 기본·레벨·보너스 제외, ALL_STAT → 4개 기본 스탯에 분배
function buildStatContributions(stats: MemberStats | null, statType: string): StatContribution[] {
  if (!stats) return [];
  const includeAllStat = (ALL_STAT_TARGETS as readonly string[]).includes(statType);
  const v = (section: typeof SOURCE_SECTIONS[number]) => {
    const base = statValue(stats, section, statType);
    const allVal = includeAllStat ? statValue(stats, section, 'ALL_STAT') : 0;
    return base + allVal;
  };
  const rows: StatContribution[] = [
    { label: '장비', value: v('equipStats') },
    { label: '세트 효과', value: v('setEffectStats') },
    { label: '특성(자신)', value: v('characteristicStats') },
    { label: '특성(아군)', value: v('partyCharacteristicStats') },
    { label: '특성(적군)', value: v('enemyDebuffStats') },
    { label: '주술', value: v('ritualStats') },
    { label: '주술 세트', value: v('ritualSetEffectStats') },
    { label: '주인공 국가 버프', value: v('protagonistBuffStats') },
    { label: '각성 명왕 버프', value: v('awakenedMyeongwangBuffStats') },
    { label: '사인검 파티 버프', value: v('partyItemBuffStats') },
  ];

  // 덱 효과: 정령·진법·층진 출처별 개별 행 (sourceName 기준 합산)
  if (stats.deckBuffDetails) {
    const sourceMap = new Map<string, { label: string; value: number }>();
    for (const d of stats.deckBuffDetails) {
      const match = d.statType === statType || (includeAllStat && d.statType === 'ALL_STAT');
      if (!match || d.value === 0) continue;
      const key = `${d.sourceType}:${d.sourceName}`;
      const label = `${d.sourceType}: ${d.sourceName}`;
      const existing = sourceMap.get(key);
      sourceMap.set(key, { label, value: (existing?.value ?? 0) + d.value });
    }
    sourceMap.forEach((entry) => rows.push(entry));
  } else {
    // deckBuffDetails 미제공 시 집계값으로 fallback
    rows.push({ label: '덱 효과', value: v('deckBuffStats') });
  }

  stats.ritualSetEffects?.forEach((eff) => {
    const match = eff.statType === statType || (includeAllStat && eff.statType === 'ALL_STAT');
    if (match && eff.statValue !== 0) {
      rows.push({ label: `주술 세트 상세 (${eff.outcome} ${eff.setName})`, value: eff.statValue });
    }
  });
  stats.myungwangTransferDetails?.forEach((detail) => {
    const match = detail.statType === statType || (includeAllStat && detail.statType === 'ALL_STAT');
    if (match && detail.value !== 0) {
      rows.push({ label: `명왕 스탯 이전 (${detail.sourceMercenaryName})`, value: detail.value });
    }
  });
  stats.lgAllyDetails?.forEach((e) => {
    if (baseStatFromCompoundKey(e.statKey) === statType && e.value !== 0) {
      const label = COMPOUND_STAT_LABEL[e.statKey] ?? STAT_LABEL[baseStatFromCompoundKey(e.statKey)] ?? e.statKey;
      rows.push({ label: `${e.mercenaryName} : ${label}`, value: e.value });
    }
  });
  return rows.filter((r) => r.value !== 0);
}

// 최종 스탯 표시용: totalStats에서 ALL_STAT 분배
function computeDisplayTotalMap(stats: MemberStats | null): Map<string, number> {
  const map = new Map<string, number>();
  stats?.totalStats?.forEach((s) => {
    map.set(s.statType, (map.get(s.statType) ?? 0) + s.value);
  });
  const allStatVal = map.get('ALL_STAT') ?? 0;
  if (allStatVal !== 0) {
    map.delete('ALL_STAT');
    for (const t of ALL_STAT_TARGETS) {
      map.set(t, (map.get(t) ?? 0) + allStatVal);
    }
  }
  return map;
}

// 최종 스탯 팝오버: 모든 출처 포함 (기본·레벨·보너스 포함), ALL_STAT 분배
function buildFullStatContributions(stats: MemberStats | null, statType: string): StatContribution[] {
  if (!stats) return [];
  const includeAllStat = (ALL_STAT_TARGETS as readonly string[]).includes(statType);
  const allSections = [
    'baseStats', 'levelBonusStats', 'bonusStats', 'equipStats', 'setEffectStats',
    'characteristicStats', 'partyCharacteristicStats', 'enemyDebuffStats',
    'ritualStats', 'ritualSetEffectStats',
    'protagonistBuffStats', 'awakenedMyeongwangBuffStats', 'partyItemBuffStats', 'myungwangTransferStats',
  ] as const;
  const SECTION_LABELS: Record<typeof allSections[number], string> = {
    baseStats: '기본',
    levelBonusStats: '레벨 스탯',
    bonusStats: '보너스 스탯',
    equipStats: '장비',
    setEffectStats: '세트 효과',
    characteristicStats: '특성(자신)',
    partyCharacteristicStats: '특성(아군)',
    enemyDebuffStats: '특성(적군)',
    ritualStats: '주술',
    ritualSetEffectStats: '주술 세트',
    protagonistBuffStats: '주인공 국가 버프',
    awakenedMyeongwangBuffStats: '각성 명왕 버프',
    partyItemBuffStats: '사인검 파티 버프',
    myungwangTransferStats: '명왕 스탯 이전',
  };
  const getVal = (section: typeof allSections[number]) => {
    const base = statValue(stats, section, statType);
    const allVal = includeAllStat ? statValue(stats, section, 'ALL_STAT') : 0;
    return base + allVal;
  };
  const rows: StatContribution[] = allSections.map((section) => ({
    label: SECTION_LABELS[section],
    value: getVal(section),
  }));
  if (stats.deckBuffDetails) {
    const sourceMap = new Map<string, { label: string; value: number }>();
    for (const d of stats.deckBuffDetails) {
      const match = d.statType === statType || (includeAllStat && d.statType === 'ALL_STAT');
      if (!match || d.value === 0) continue;
      const key = `${d.sourceType}:${d.sourceName}`;
      const existing = sourceMap.get(key);
      sourceMap.set(key, { label: `${d.sourceType}: ${d.sourceName}`, value: (existing?.value ?? 0) + d.value });
    }
    sourceMap.forEach((entry) => rows.push(entry));
  } else {
    const v = statValue(stats, 'deckBuffStats', statType)
      + (includeAllStat ? statValue(stats, 'deckBuffStats', 'ALL_STAT') : 0);
    rows.push({ label: '덱 효과', value: v });
  }
  stats.lgAllyDetails?.forEach((e) => {
    if (baseStatFromCompoundKey(e.statKey) === statType && e.value !== 0) {
      const label = COMPOUND_STAT_LABEL[e.statKey] ?? STAT_LABEL[baseStatFromCompoundKey(e.statKey)] ?? e.statKey;
      rows.push({ label: `${e.mercenaryName} : ${label}`, value: e.value });
    }
  });
  return rows.filter((r) => r.value !== 0);
}

// 최종 스탯 표에 고정 표시할 스탯 목록
const LEFT_STAT_COLS = ['STRENGTH', 'DEXTERITY', 'VITALITY', 'INTELLECT'] as const;
const RIGHT_STAT_COLS = ['ATTACK_POWER', 'ELEMENT_VALUE', 'DAMAGE_PERCENT', 'CRITICAL_CHANCE'] as const;
const FIXED_STAT_ROWS = Math.max(LEFT_STAT_COLS.length, RIGHT_STAT_COLS.length); // 4

function EquipSetEffectsPanel({ stats, loading, compact = false }: { stats: MemberStats | null; loading: boolean; compact?: boolean }) {
  if (loading) {
    return <div style={{ background: 'var(--card)' }} className={`rounded animate-pulse ${compact ? 'h-16 mt-2' : 'h-20 mt-3'}`} />;
  }

  const equipmentEffects = stats?.equipmentSetEffects ?? [];
  const ritualEffects = stats?.ritualSetEffects ?? [];
  if (equipmentEffects.length === 0 && ritualEffects.length === 0) {
    return (
      <p className={`${compact ? 'text-[10px] mt-2' : 'text-xs mt-3'}`} style={{ color: 'var(--text-disabled)' }}>
        발동 중인 세트효과 없음
      </p>
    );
  }

  return (
    <div className={compact ? 'mt-2 space-y-2' : 'mt-3 space-y-3'}>
      {equipmentEffects.length > 0 && (
        <div>
          <p className={`font-semibold mb-1 ${compact ? 'text-[10px]' : 'text-xs'}`} style={{ color: 'var(--text)' }}>
            장비 세트효과
          </p>
          <div className="space-y-1">
            {equipmentEffects.map((effect, index) => (
              <div
                key={`${effect.setName}-${effect.requiredPieces}-${effect.statType}-${index}`}
                style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
                className={`rounded ${compact ? 'px-2 py-1.5' : 'px-2.5 py-2'}`}
              >
                <div className={`flex items-center justify-between gap-2 ${compact ? 'text-[10px]' : 'text-xs'}`}>
                  <span style={{ color: 'var(--text)' }}>{effect.setName}</span>
                  <span style={{ color: 'var(--brown)' }}>{effect.appliedPieces}/{effect.requiredPieces}종</span>
                </div>
                <p className={compact ? 'text-[9px] mt-0.5' : 'text-[10px] mt-0.5'} style={{ color: 'var(--text-muted)' }}>
                  {formatStatType(effect.statType)} +{effect.statValue}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}
      {ritualEffects.length > 0 && (
        <div>
          <p className={`font-semibold mb-1 ${compact ? 'text-[10px]' : 'text-xs'}`} style={{ color: 'var(--text)' }}>
            주술 세트효과
          </p>
          <div className="space-y-1">
            {ritualEffects.map((effect, index) => (
              <div
                key={`${effect.ritualName}-${effect.setName}-${effect.outcome}-${effect.statType}-${index}`}
                style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
                className={`rounded ${compact ? 'px-2 py-1.5' : 'px-2.5 py-2'}`}
              >
                <div className={`flex items-center justify-between gap-2 ${compact ? 'text-[10px]' : 'text-xs'}`}>
                  <span style={{ color: 'var(--text)' }}>{effect.outcome} / {effect.setName}</span>
                  <span style={{ color: 'var(--brown)' }}>{effect.appliedPieces}/{effect.requiredPieces}</span>
                </div>
                <p className={compact ? 'text-[9px] mt-0.5' : 'text-[10px] mt-0.5'} style={{ color: 'var(--text-muted)' }}>
                  {formatStatType(effect.statType)} {formatStatValue(effect.statType, effect.statValue, effect.statUnit)}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function EquipStatTable({ stats, loading, compact = false }: { stats: MemberStats | null; loading: boolean; compact?: boolean }) {
  if (loading) {
    return (
      <div className={compact ? '' : 'mt-4'}>
        <div style={{ background: 'var(--card)' }} className={`rounded animate-pulse ${compact ? 'h-28' : 'h-36'}`} />
        <div style={{ background: 'var(--card)' }} className={`rounded animate-pulse ${compact ? 'h-16 mt-2' : 'h-20 mt-3'}`} />
      </div>
    );
  }
  return (
    <div className={compact ? '' : 'mt-4'}>
      <p className={`font-semibold mb-1.5 ${compact ? 'text-[11px]' : 'text-xs mb-2'}`} style={{ color: 'var(--text)' }}>장비 스탯 합산</p>
      <div style={{ border: '1px solid var(--border)' }} className="rounded-lg overflow-hidden text-xs">
        <div className={`grid grid-cols-2 font-semibold ${compact ? 'px-2 py-1.5' : 'px-3 py-2'}`} style={{ background: 'var(--bg)', color: 'var(--text-muted)' }}>
          <span>스탯</span>
          <span className="text-right">총합</span>
        </div>
        <div
          className={`grid grid-cols-2 ${compact ? 'px-2 py-1' : 'px-3 py-2'}`}
          style={{ borderTop: '1px solid var(--border)', color: 'var(--text)' }}
        >
          <span className={compact ? 'text-[11px]' : ''}>공격력</span>
          <span className={`text-right font-medium ${compact ? 'text-[11px]' : ''}`}>{formatEquipAttackPower(stats)}</span>
        </div>
        {EQUIP_TAB_STATS.map((statType) => {
          const equipValue = statValue(stats, 'equipStats', statType);
          const setValue = statValue(stats, 'setEffectStats', statType);
          const ritualValue = statValue(stats, 'ritualStats', statType);
          const ritualSetValue = statValue(stats, 'ritualSetEffectStats', statType);
          const totalValue = equipValue + setValue + ritualValue + ritualSetValue;
          if (totalValue === 0) return null;
          const isPercent = statType === 'DAMAGE_PERCENT';
          const formatPart = (value: number) => (isPercent ? `${value}%` : String(value));
          const detail = [equipValue, setValue, ritualValue, ritualSetValue]
            .map((value, index) => ({ value, label: ['장비', '세트', '주술', '주술세트'][index] }))
            .filter((part) => part.value > 0)
            .map((part) => `${part.label} ${formatPart(part.value)}`)
            .join(' · ');
          return (
          <div
            key={statType}
            className={`grid grid-cols-2 ${compact ? 'px-2 py-1' : 'px-3 py-2'}`}
            style={{ borderTop: '1px solid var(--border)', color: 'var(--text)' }}
          >
            <span className={compact ? 'text-[11px] truncate' : ''}>{formatStatType(statType)}</span>
            <span className={`text-right font-medium ${compact ? 'text-[11px]' : ''}`} title={detail || undefined}>
              {formatPart(totalValue)}
            </span>
          </div>
          );
        })}
      </div>
      <EquipSetEffectsPanel stats={stats} loading={loading} compact={compact} />
    </div>
  );
}

function CharacteristicQuadrantCard({
  characteristic,
  saving,
  onSelectLevel,
}: {
  characteristic: CharacteristicItem | null;
  saving: boolean;
  onSelectLevel: (id: number, level?: number) => void;
}) {
  if (!characteristic) {
    return (
      <div
        style={{ border: '1px dashed var(--border)', background: 'var(--bg)' }}
        className="rounded-lg flex items-center justify-center min-h-[180px] p-3"
      >
        <span className="text-xs" style={{ color: 'var(--text-disabled)' }}>특성 없음</span>
      </div>
    );
  }

  const maxLv = characteristicMaxLevel(characteristic);
  const tiers = Array.from({ length: maxLv }, (_, i) => i + 1);
  const preview = characteristic.selectedLevel
    ? levelsAtTier(characteristic, characteristic.selectedLevel)
    : [];

  return (
    <div
      style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
      className="rounded-lg p-3 min-h-[180px] flex flex-col"
    >
      <div className="flex items-start justify-between gap-2 mb-3">
        <p className="text-sm font-semibold" style={{ color: 'var(--text)' }}>{characteristic.name}</p>
        <span className="text-[10px] shrink-0 px-1.5 py-0.5 rounded" style={{ background: 'var(--bg)', color: 'var(--brown)' }}>
          {characteristic.point != null && characteristic.point > 0
            ? `${characteristic.point}pt/lv`
            : 'Lv=pt'}
        </span>
      </div>
      <div className="flex flex-wrap gap-1 mb-3">
        {tiers.map((tier) => {
          const selected = characteristic.selectedLevel === tier;
          return (
            <button
              key={tier}
              type="button"
              disabled={saving}
              onClick={() => onSelectLevel(characteristic.characteristicId, tier)}
              style={{
                background: selected ? 'var(--brown)' : 'var(--bg)',
                color: selected ? 'var(--beige)' : 'var(--text-muted)',
                border: '1px solid var(--border)',
              }}
              className="flex-1 rounded py-1.5 text-xs font-semibold disabled:opacity-50"
            >
              {tier}
            </button>
          );
        })}
        {characteristic.selectedLevel && (
          <button
            type="button"
            disabled={saving}
            onClick={() => onSelectLevel(characteristic.characteristicId)}
            style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
            className="rounded px-2 py-1.5 text-[10px] disabled:opacity-50"
          >
            해제
          </button>
        )}
      </div>
      <div className="flex-1 text-[11px] leading-relaxed space-y-1" style={{ color: 'var(--text-muted)' }}>
        {preview.length === 0 ? (
          <span style={{ color: 'var(--text-disabled)' }}>레벨을 선택하면 효과가 표시됩니다</span>
        ) : (
          preview.map((entry, idx) => {
            const text = formatLevelEntryAmount(entry);
            if (!text) return null;
            return (
              <p key={`${entry.statType ?? entry.label ?? idx}-${entry.level}`}>
                {text}
              </p>
            );
          })
        )}
      </div>
    </div>
  );
}

function StatContributionPopover({
  statType,
  contributions,
  total,
  compact = false,
}: {
  statType: string;
  contributions: StatContribution[];
  total: number;
  compact?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);
  const [panelStyle, setPanelStyle] = useState<{ top: number; left: number }>({ top: 0, left: 0 });

  const updatePosition = useCallback(() => {
    const button = buttonRef.current;
    if (!button) return;

    const rect = button.getBoundingClientRect();
    const margin = 8;
    const panelWidth = panelRef.current?.offsetWidth ?? 220;
    const panelHeight = panelRef.current?.offsetHeight ?? 180;

    let top = rect.bottom + margin;
    if (top + panelHeight > window.innerHeight - margin) {
      top = rect.top - panelHeight - margin;
    }
    top = Math.max(margin, Math.min(top, window.innerHeight - panelHeight - margin));

    let left = rect.right - panelWidth;
    left = Math.max(margin, Math.min(left, window.innerWidth - panelWidth - margin));

    setPanelStyle({ top, left });
  }, []);

  useEffect(() => {
    if (!open) return;

    updatePosition();
    const raf = requestAnimationFrame(updatePosition);

    const onReposition = () => updatePosition();
    window.addEventListener('resize', onReposition);
    window.addEventListener('scroll', onReposition, true);
    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('resize', onReposition);
      window.removeEventListener('scroll', onReposition, true);
    };
  }, [open, updatePosition, contributions, total]);

  const popover = open && typeof document !== 'undefined' ? createPortal(
    <>
      <div className="fixed inset-0 z-[600]" onClick={() => setOpen(false)} />
      <div
        ref={panelRef}
        style={{
          top: panelStyle.top,
          left: panelStyle.left,
          background: 'var(--card)',
          border: '1px solid var(--border)',
          boxShadow: '0 8px 24px rgba(0,0,0,0.18)',
        }}
        className="fixed z-[601] min-w-[220px] max-w-[min(280px,calc(100vw-16px))] rounded-lg p-3 text-xs"
      >
        <p className="font-semibold mb-2" style={{ color: 'var(--text)' }}>
          {formatStatType(statType)} 기여 내역
        </p>
        {contributions.length === 0 ? (
          <p style={{ color: 'var(--text-disabled)' }}>기여 항목 없음</p>
        ) : (
          <ul className="space-y-1">
            {contributions.map((c) => (
              <li key={c.label} className="flex justify-between gap-3" style={{ color: 'var(--text-muted)' }}>
                <span>{c.label}</span>
                <span style={{ color: 'var(--text)' }}>{c.value > 0 ? `+${c.value}` : c.value}</span>
              </li>
            ))}
          </ul>
        )}
        <div className="mt-2 pt-2 flex justify-between font-semibold" style={{ borderTop: '1px solid var(--border)', color: 'var(--brown)' }}>
          <span>합계(최종)</span>
          <span>{total}</span>
        </div>
      </div>
    </>,
    document.body,
  ) : null;

  return (
    <>
      {compact ? (
        <button
          ref={buttonRef}
          type="button"
          onClick={() => setOpen((v) => !v)}
          style={{ color: 'var(--text-muted)', lineHeight: 1 }}
          className="ml-1 text-[10px] hover:text-[var(--text)] shrink-0"
          title="이 스탯에 영향을 준 항목과 수치"
        >
          ?
        </button>
      ) : (
        <button
          ref={buttonRef}
          type="button"
          onClick={() => setOpen((v) => !v)}
          style={{ border: '1px solid var(--border)', color: 'var(--brown)' }}
          className="rounded px-2 py-0.5 text-[10px] hover:bg-[var(--bg)]"
          title="이 스탯에 영향을 준 항목과 수치"
        >
          기여 내역
        </button>
      )}
      {popover}
    </>
  );
}

// ══════════ 덱 탭 / 추가 모달 ══════════

function DeckTab({ deck, isSelected, onSelect, onDelete, onRename }: {
  deck: { id: number; name: string; isActive: boolean };
  isSelected: boolean;
  onSelect: () => void;
  onDelete: () => void;
  onRename: (name: string) => void;
}) {
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(deck.name);

  useEffect(() => {
    setName(deck.name);
  }, [deck.name]);

  function submitRename() {
    setEditing(false);
    if (name.trim() && name !== deck.name) onRename(name.trim());
    else setName(deck.name);
  }

  if (editing) {
    return (
      <input
        autoFocus
        value={name}
        onChange={(e) => setName(e.target.value)}
        onBlur={submitRename}
        onKeyDown={(e) => {
          if (e.key === 'Enter') submitRename();
          if (e.key === 'Escape') {
            setName(deck.name);
            setEditing(false);
          }
        }}
        style={{ background: 'var(--bg)', border: '1px solid var(--brown)', color: 'var(--text)' }}
        className="rounded-lg px-3 py-1.5 text-sm focus:outline-none w-28"
      />
    );
  }

  return (
    <div className="flex items-center gap-0.5">
      <button
        type="button"
        onClick={onSelect}
        style={{
          background: isSelected ? 'var(--brown)' : 'var(--card)',
          color: isSelected ? 'var(--beige)' : 'var(--text)',
          border: `1px solid ${isSelected ? 'var(--brown)' : 'var(--border)'}`,
        }}
        className="rounded-lg px-3 py-1.5 text-sm font-medium hover:border-[var(--brown)] transition-colors whitespace-nowrap"
      >
        {deck.name}
        {deck.isActive && !isSelected && (
          <span style={{ color: 'var(--text-muted)' }} className="text-[10px] ml-1">(활성)</span>
        )}
      </button>
      {isSelected && (
        <div className="flex items-center">
          <button
            type="button"
            onClick={() => setEditing(true)}
            style={{ color: 'var(--text-muted)' }}
            className="p-1 hover:text-[var(--brown)] transition-colors text-xs"
            title="이름 수정"
          >
            ✏️
          </button>
          <button
            type="button"
            onClick={onDelete}
            style={{ color: 'var(--text-muted)' }}
            className="p-1 hover:text-[var(--danger)] transition-colors"
            title="덱 삭제"
          >
            <Trash2 style={{ width: 13, height: 13 }} />
          </button>
        </div>
      )}
    </div>
  );
}

function AddDeckModal({
  name,
  creating,
  onChange,
  onConfirm,
  onClose,
}: {
  name: string;
  creating: boolean;
  onChange: (value: string) => void;
  onConfirm: () => void;
  onClose: () => void;
}) {
  return (
    <div
      className="fixed inset-0 flex items-center justify-center z-[500] p-4"
      style={{ background: 'rgba(0,0,0,0.65)' }}
      onClick={onClose}
    >
      <div
        style={{ background: 'var(--card)', border: '1px solid var(--border)', width: 360 }}
        className="rounded-xl p-5 space-y-4"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="font-semibold" style={{ color: 'var(--text)' }}>새 덱 추가</h2>
        <div>
          <label className="text-xs block mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>덱 이름</label>
          <input
            autoFocus
            value={name}
            onChange={(e) => onChange(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && onConfirm()}
            placeholder="덱 이름을 입력하세요"
            style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
            className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]"
          />
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={onClose}
            disabled={creating}
            style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
            className="flex-1 py-2 rounded text-sm disabled:opacity-50"
          >
            취소
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={creating}
            style={{ background: 'var(--brown)', color: 'var(--beige)' }}
            className="flex-1 py-2 rounded text-sm font-semibold hover:bg-[var(--brown-dark)] disabled:opacity-50"
          >
            {creating ? '추가 중...' : '확인'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ══════════ 용병 선택 모달 ══════════

function MercenaryModal({ onSelect, onClose }: {
  onSelect: (m: MercenaryDto) => void;
  onClose: () => void;
}) {
  const [query, setQuery] = useState('');
  const [mercenaries, setMercenaries] = useState<MercenaryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState<string>('전체');
  const [failedImageIds, setFailedImageIds] = useState<Set<number>>(new Set());

  useEffect(() => {
    api.getMercenaries()
      .then(setMercenaries)
      .catch(() => setMercenaries([]))
      .finally(() => setLoading(false));
  }, []);

  // 카테고리 목록 (순서 유지)
  const categories = ['전체', ...Array.from(
    mercenaries.reduce<Map<string, number>>((acc, m) => {
      const cat = m.category ?? '미분류';
      acc.set(cat, (acc.get(cat) ?? 0) + 1);
      return acc;
    }, new Map())
  ).map(([cat]) => cat)];

  const categoryCount = (cat: string) => {
    if (cat === '전체') return mercenaries.length;
    return mercenaries.filter((m) => (m.category ?? '미분류') === cat).length;
  };

  // 검색 + 카테고리 필터
  const displayed = mercenaries.filter((m) => {
    const catMatch = selectedCategory === '전체' || (m.category ?? '미분류') === selectedCategory;
    const queryMatch = !query || m.name.includes(query);
    return catMatch && queryMatch;
  });

  return (
    <div
      className="fixed inset-0 flex items-center justify-center z-[500] p-4"
      style={{ background: 'rgba(0,0,0,0.65)' }}
      onClick={onClose}
    >
      <div
        style={{ background: 'var(--card)', border: '1px solid var(--border)', width: 860, maxHeight: '84vh' }}
        className="rounded-xl overflow-hidden flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 헤더 + 검색 */}
        <div style={{ borderBottom: '1px solid var(--border)' }} className="px-5 py-3.5 shrink-0 space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold" style={{ color: 'var(--text)' }}>용병 선택</h2>
            <button onClick={onClose} style={{ color: 'var(--text-muted)' }} className="text-xl leading-none hover:text-[var(--text)] transition-colors">×</button>
          </div>
          <div className="relative">
            <Search style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', width: 14, height: 14, color: 'var(--text-muted)' }} />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="용병 이름 검색..."
              style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)', paddingLeft: 32 }}
              className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]"
            />
          </div>
        </div>

        {/* 본문: 사이드바 + 메인 */}
        <div className="flex flex-1 overflow-hidden">
          {/* 좌측 카테고리 사이드바 */}
          <div
            style={{ borderRight: '1px solid var(--border)', width: 160, background: 'var(--bg)' }}
            className="shrink-0 overflow-y-auto py-1"
          >
            {categories.map((cat) => {
              const active = selectedCategory === cat;
              return (
                <button
                  key={cat}
                  onClick={() => setSelectedCategory(cat)}
                  style={{
                    background: active ? 'var(--brown)' : 'transparent',
                    color: active ? 'var(--beige)' : 'var(--text-muted)',
                    width: '100%',
                    textAlign: 'left',
                  }}
                  className="flex items-center justify-between px-3 py-2 text-xs transition-colors hover:bg-[var(--beige)] hover:text-[var(--brown)]"
                >
                  <span className="truncate font-medium">{cat}</span>
                  <span
                    style={{
                      background: active ? 'rgba(255,255,255,0.25)' : 'var(--border)',
                      color: active ? 'var(--beige)' : 'var(--text-muted)',
                    }}
                    className="ml-1.5 shrink-0 text-[10px] px-1.5 py-0.5 rounded-full"
                  >
                    {categoryCount(cat)}
                  </span>
                </button>
              );
            })}
          </div>

          {/* 우측 용병 카드 그리드 */}
          <div className="flex-1 overflow-y-auto p-4">
            {loading ? (
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(108px, 1fr))',
                  gap: 12,
                }}
              >
                {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
                  <div
                    key={i}
                    style={{ background: 'var(--bg)', border: '1px solid var(--border)', height: 132 }}
                    className="rounded-lg animate-pulse"
                  />
                ))}
              </div>
            ) : displayed.length === 0 ? (
              <div className="flex items-center justify-center h-full">
                <p style={{ color: 'var(--text-disabled)' }} className="text-sm">
                  {query ? '검색 결과가 없습니다' : '용병 데이터가 없습니다'}
                </p>
              </div>
            ) : (
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(108px, 1fr))',
                  gap: 12,
                }}
              >
                {displayed.map((m) => (
                  <button
                    key={m.id}
                    onClick={() => onSelect(m)}
                    style={{
                      background: 'var(--bg)',
                      border: '1px solid var(--border)',
                      textAlign: 'left',
                      overflow: 'hidden',
                    }}
                    className="rounded-lg hover:border-[var(--brown)] hover:shadow-md transition-all group"
                  >
                    {/* 이미지 영역 */}
                    <div className="relative w-full overflow-hidden" style={{ background: 'var(--border)', aspectRatio: '1 / 1' }}>
                      {m.imageUrl && !failedImageIds.has(m.id) ? (
                        <img
                          src={m.imageUrl}
                          alt={m.name}
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                          onError={() => {
                            setFailedImageIds((prev) => {
                              const next = new Set(prev);
                              next.add(m.id);
                              return next;
                            });
                          }}
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center">
                          <span className="font-serif text-2xl font-bold" style={{ color: 'var(--text-muted)' }}>
                            {m.name.charAt(0)}
                          </span>
                        </div>
                      )}
                      {/* 속성 뱃지 */}
                      {m.element && m.element !== 'NONE' && (
                        <span
                          style={{
                            background: ELEMENT_COLORS[m.element] ?? 'var(--brown)',
                            color: '#fff',
                          }}
                          className="absolute top-1.5 right-1.5 text-[10px] font-bold w-5 h-5 rounded-full flex items-center justify-center shadow"
                        >
                          {ELEMENT_LABELS[m.element] ?? m.element}
                        </span>
                      )}
                    </div>

                    {/* 카드 하단: 이름만 표시 (요청사항 반영) */}
                    <div className="px-2 py-1.5">
                      <p className="text-xs font-medium truncate text-center" style={{ color: 'var(--text)' }}>{m.name}</p>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function monsterHittingResist(monster: MonsterDto) {
  return monster.hittingResistance ?? monster.resistance;
}

function StatWithDebuff({ label, base, debuffed }: {
  label: string;
  base?: number | null;
  debuffed?: number | null;
}) {
  if (base == null && debuffed == null) return null;
  const showDebuff = debuffed != null && debuffed !== base;
  return (
    <p className="text-[10px] leading-snug" style={{ color: 'var(--text-muted)' }}>
      {label}:{' '}
      <span style={{ color: 'var(--text)' }}>{base ?? '-'}</span>
      {showDebuff && (
        <>
          {' → '}
          <span style={{ color: 'var(--brown)' }}>{debuffed}</span>
        </>
      )}
    </p>
  );
}

function MonsterDpsSidebar({
  selectedDeckId,
  monster,
  monsterQuery,
  setMonsterQuery,
  showMonsterSuggest,
  setShowMonsterSuggest,
  monsterSuggestions,
  onSelectMonster,
  onClearMonster,
  failedMonsterImageIds,
  setFailedMonsterImageIds,
  dpsResult,
  calcLoading,
  clearTimeSeconds,
  onClearTimeSecondsChange,
  onSaveClearTime,
  clearTimeSaving,
  sticky = true,
}: {
  selectedDeckId: number | null;
  monster: MonsterDto | null;
  monsterQuery: string;
  setMonsterQuery: (v: string) => void;
  showMonsterSuggest: boolean;
  setShowMonsterSuggest: (v: boolean) => void;
  monsterSuggestions: MonsterDto[];
  onSelectMonster: (m: MonsterDto) => void;
  onClearMonster: () => void;
  failedMonsterImageIds: Set<number>;
  setFailedMonsterImageIds: (value: Set<number> | ((prev: Set<number>) => Set<number>)) => void;
  dpsResult: DpsResultDto | null;
  calcLoading: boolean;
  clearTimeSeconds: string;
  onClearTimeSecondsChange: (v: string) => void;
  onSaveClearTime: () => void;
  clearTimeSaving: boolean;
  sticky?: boolean;
}) {
  const outerClass = sticky ? 'hidden lg:block min-w-0' : 'lg:hidden min-w-0 mt-4';
  const innerClass = sticky
    ? 'sticky space-y-3 max-h-[calc(100vh-112px)] overflow-y-auto pr-0.5'
    : 'space-y-3';

  return (
    <aside className={outerClass}>
      <div
        className={innerClass}
        style={sticky ? { top: DECK_STICKY_TOP, scrollbarGutter: 'stable' } : undefined}
      >
        {/* 대상 몬스터 */}
        <div style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-3">
          <h2 className="font-semibold text-sm mb-2" style={{ color: 'var(--text)' }}>대상 몬스터</h2>

          <div className="relative mb-2">
            <Search style={{ position: 'absolute', left: 8, top: 9, width: 13, height: 13, color: 'var(--text-muted)' }} />
            <input
              value={monsterQuery}
              onFocus={() => setShowMonsterSuggest(true)}
              onChange={(e) => {
                setMonsterQuery(e.target.value);
                setShowMonsterSuggest(true);
              }}
              onBlur={() => setTimeout(() => setShowMonsterSuggest(false), 120)}
              placeholder="몬스터 검색..."
              style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)', paddingLeft: 28 }}
              className="w-full rounded px-2.5 py-1.5 text-xs focus:outline-none focus:border-[var(--brown)]"
            />
            {showMonsterSuggest && (
              <div
                style={{ background: 'var(--card)', border: '1px solid var(--border)', boxShadow: '0 6px 16px rgba(0,0,0,0.12)' }}
                className="absolute z-20 left-0 right-0 mt-1 rounded-lg max-h-44 overflow-y-auto"
              >
                {monsterSuggestions.length === 0 ? (
                  <p style={{ color: 'var(--text-disabled)' }} className="text-[11px] px-2.5 py-2">검색 결과 없음</p>
                ) : (
                  monsterSuggestions.map((m) => (
                    <button
                      key={m.id}
                      type="button"
                      onMouseDown={(e) => e.preventDefault()}
                      onClick={() => onSelectMonster(m)}
                      style={{ borderBottom: '1px solid var(--border)', width: '100%', textAlign: 'left' }}
                      className="px-2.5 py-1.5 hover:bg-[var(--bg)] transition-colors"
                    >
                      <p className="text-xs font-medium truncate" style={{ color: 'var(--text)' }}>{m.name}</p>
                    </button>
                  ))
                )}
              </div>
            )}
          </div>

          {monster ? (
            <div className="flex gap-2.5">
              <div
                className="w-[72px] h-[72px] shrink-0 rounded-md overflow-hidden"
                style={{ background: 'var(--border)' }}
              >
                {monster.imageUrl && !failedMonsterImageIds.has(monster.id) ? (
                  <img
                    src={monster.imageUrl}
                    alt={monster.name}
                    className="w-full h-full object-cover"
                    onError={() => {
                      setFailedMonsterImageIds((prev) => new Set(prev).add(monster.id));
                    }}
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center">
                    <span className="font-serif text-2xl font-bold" style={{ color: 'var(--text-muted)' }}>
                      {monster.name.charAt(0)}
                    </span>
                  </div>
                )}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between gap-1 mb-0.5">
                  <p className="font-semibold text-xs leading-tight truncate" style={{ color: 'var(--text)' }}>
                    {monster.name}
                  </p>
                  <button
                    type="button"
                    onClick={onClearMonster}
                    style={{ color: 'var(--text-muted)' }}
                    className="hover:text-[var(--danger)] shrink-0"
                  >
                    <X style={{ width: 12, height: 12 }} />
                  </button>
                </div>
                <p className="text-[10px] mb-0.5" style={{ color: 'var(--text-muted)' }}>
                  속성: {monster.element ? (ELEMENT_LABELS[monster.element] ?? monster.element) : '-'}
                </p>
                <StatWithDebuff
                  label="타격저항"
                  base={monsterHittingResist(monster)}
                  debuffed={dpsResult?.resistAfterDebuff}
                />
                {monster.magicResistance != null && (
                  <p className="text-[10px] leading-snug" style={{ color: 'var(--text-muted)' }}>
                    마법저항: <span style={{ color: 'var(--text)' }}>{monster.magicResistance}</span>
                  </p>
                )}
                <StatWithDebuff
                  label="속성값"
                  base={monster.elementValue}
                  debuffed={dpsResult?.effectiveMonsterElement}
                />
                {dpsResult && (
                  <p className="text-[10px] leading-snug mt-0.5" style={{ color: 'var(--text-muted)' }}>
                    저항통과: <span style={{ color: 'var(--brown)' }}>{dpsResult.resistPassRate.toFixed(1)}%</span>
                  </p>
                )}
              </div>
            </div>
          ) : (
            <p className="text-[11px] text-center py-4" style={{ color: 'var(--text-disabled)' }}>
              몬스터를 검색해 선택하세요
            </p>
          )}
        </div>

        {/* DPS 분석 */}
        {selectedDeckId && monster && calcLoading && (
          <div
            style={{ background: 'var(--card)', border: '1px solid var(--border)', color: 'var(--text-muted)' }}
            className="flex items-center justify-center gap-1.5 py-2 rounded-xl text-xs"
          >
            <Zap style={{ width: 12, height: 12 }} />
            DPS 계산 중...
          </div>
        )}

        {dpsResult && (
          <div style={{ background: 'var(--card)', border: '1px solid var(--brown)' }} className="rounded-xl p-3">
            <h3 className="font-semibold text-sm mb-2 flex items-center gap-1.5" style={{ color: 'var(--brown)' }}>
              <Zap style={{ width: 14, height: 14 }} /> DPS 분석
            </h3>

            <div className="grid grid-cols-2 gap-1.5 mb-2">
              <div style={{ background: 'var(--beige)', borderRadius: 6 }} className="p-2 text-center">
                <p style={{ color: 'var(--text-muted)' }} className="text-[9px] mb-0.5">저항깎</p>
                <p className="font-semibold text-xs" style={{ color: 'var(--brown)' }}>
                  {(dpsResult.totalResistPierce ?? 0).toLocaleString()}
                </p>
              </div>
              <div style={{ background: 'var(--beige)', borderRadius: 6 }} className="p-2 text-center">
                <p style={{ color: 'var(--text-muted)' }} className="text-[9px] mb-0.5">속성깎</p>
                <p className="font-semibold text-xs" style={{ color: 'var(--brown)' }}>
                  {(dpsResult.totalElementPierce ?? 0).toLocaleString()}
                </p>
              </div>
            </div>

            <div className="space-y-1.5 mb-2">
              <div style={{ background: 'var(--beige)', borderRadius: 6 }} className="p-2 text-center">
                <p style={{ color: 'var(--text-muted)' }} className="text-[9px] mb-0.5">Raw DPS</p>
                <p className="font-serif text-base font-bold leading-none" style={{ color: 'var(--brown)' }}>
                  {(dpsResult.rawTotalDps ?? 0).toLocaleString()}
                </p>
              </div>
              <div style={{ background: 'var(--beige)', borderRadius: 6 }} className="p-2 text-center">
                <p style={{ color: 'var(--text-muted)' }} className="text-[9px] mb-0.5">Adjust DPS</p>
                <p className="font-serif text-base font-bold leading-none" style={{ color: 'var(--brown)' }}>
                  {(dpsResult.adjustTotalDps ?? dpsResult.totalDps ?? 0).toLocaleString()}
                </p>
              </div>
              <div style={{ background: 'var(--beige)', borderRadius: 6 }} className="p-2 text-center">
                <p style={{ color: 'var(--text-muted)' }} className="text-[9px] mb-0.5">Final DPS</p>
                <p className="font-serif text-base font-bold leading-none" style={{ color: 'var(--brown)' }}>
                  {(dpsResult.totalDps ?? 0).toLocaleString()}
                </p>
              </div>
            </div>

            {(dpsResult.memberResults ?? []).length > 0 && (
              <div className="space-y-1.5">
                <p className="text-[10px] font-medium" style={{ color: 'var(--text-muted)' }}>용병별 비중</p>
                {(dpsResult.memberResults ?? []).map((m) => {
                  const pct = roundedDamageShare(m.damageShare) ?? 0;
                  return (
                    <div key={m.memberId}>
                      <div className="flex justify-between text-[10px] mb-0.5 gap-1">
                        <span style={{ color: 'var(--text)' }} className="truncate">{m.mercenaryName}</span>
                        <span style={{ color: 'var(--text-muted)' }} className="shrink-0">{pct.toFixed(1)}%</span>
                      </div>
                      <div style={{ background: 'var(--border)', borderRadius: 2, height: 3 }}>
                        <div style={{ background: 'var(--brown)', borderRadius: 2, height: 3, width: `${pct}%` }} />
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {selectedDeckId && monster && dpsResult && (
          <div style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-3">
            <h3 className="font-semibold text-sm mb-1 flex items-center gap-1.5" style={{ color: 'var(--text)' }}>
              <Timer style={{ width: 14, height: 14 }} /> 평균 클리어타임
            </h3>
            <p className="text-[10px] mb-2" style={{ color: 'var(--text-muted)' }}>
              해당 몬스터 평균 클리어 소요 시간 (6~26초)
            </p>
            <div className="flex gap-2 items-center mb-2">
              <input
                type="number"
                min={6}
                max={26}
                value={clearTimeSeconds}
                onChange={(e) => onClearTimeSecondsChange(e.target.value)}
                placeholder="초"
                style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
                className="w-20 rounded px-2 py-1.5 text-xs focus:outline-none focus:border-[var(--brown)]"
              />
              <span className="text-xs" style={{ color: 'var(--text-muted)' }}>초</span>
            </div>
            <button
              type="button"
              disabled={clearTimeSaving}
              onClick={onSaveClearTime}
              style={{
                background: clearTimeSaving ? 'var(--border)' : 'var(--brown)',
                color: clearTimeSaving ? 'var(--text-muted)' : '#fff',
              }}
              className="w-full rounded-lg py-2 text-xs font-medium transition-opacity disabled:cursor-not-allowed"
            >
              {clearTimeSaving ? '저장 중...' : '클리어타임 저장'}
            </button>
          </div>
        )}
      </div>
    </aside>
  );
}

// ══════════ 장비 선택 모달 ══════════

function EquipModal({ slot, mercenaryId, isHeroMember, equipmentBySlot, onSelect, onClose }: {
  slot: string;
  mercenaryId: number;
  isHeroMember: boolean;
  equipmentBySlot: Record<string, EquipmentItemDto[]>;
  onSelect: (item: EquipmentItemDto) => void;
  onClose: () => void;
}) {
  const [query, setQuery] = useState('');
  const [exclusiveItems, setExclusiveItems] = useState<EquipmentItemDto[]>([]);
  const [exclusiveLoading, setExclusiveLoading] = useState(false);

  useEffect(() => {
    if (isHeroMember) {
      setExclusiveItems([]);
      return;
    }
    let cancelled = false;
    setExclusiveLoading(true);
    api.getExclusiveEquipment(mercenaryId, slot)
      .then((items) => { if (!cancelled) setExclusiveItems(items); })
      .catch(() => { if (!cancelled) setExclusiveItems([]); })
      .finally(() => { if (!cancelled) setExclusiveLoading(false); });
    return () => { cancelled = true; };
  }, [mercenaryId, slot, isHeroMember]);

  const filtered = useMemo(() => {
    const displayItems = buildDeckEquipmentDisplayItems(
      exclusiveItems,
      equipmentBySlot[slot] ?? [],
      mercenaryId,
      slot,
      query,
    );
    const byId = new Map(
      [...exclusiveItems, ...(equipmentBySlot[slot] ?? [])].map((item) => [equipmentItemKey(item), item]),
    );
    return displayItems
      .map((item) => byId.get(item.id))
      .filter((item): item is EquipmentItemDto => item != null);
  }, [exclusiveItems, equipmentBySlot, slot, mercenaryId, query]);

  return (
    <div className="fixed inset-0 flex items-center justify-center z-[500] p-4" style={{ background: 'rgba(0,0,0,0.65)' }} onClick={onClose}>
      <div style={{ background: 'var(--card)', border: '1px solid var(--border)', width: 500, maxHeight: '70vh' }}
        className="rounded-xl overflow-hidden flex flex-col"
        onClick={(e) => e.stopPropagation()}>
        <div style={{ borderBottom: '1px solid var(--border)' }} className="flex items-center justify-between px-5 py-3.5 shrink-0">
          <h2 className="font-semibold" style={{ color: 'var(--text)' }}>
            {SLOT_LABEL[slot] ?? slot} 장비 선택
          </h2>
          <button onClick={onClose} style={{ color: 'var(--text-muted)' }} className="text-xl">×</button>
        </div>

        <div style={{ borderBottom: '1px solid var(--border)' }} className="p-3 shrink-0">
          <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="공용 장비 검색..."
            style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
            className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]" />
        </div>

        <div className="overflow-y-auto flex-1 p-3 space-y-1">
          {exclusiveLoading ? (
            <div className="space-y-2">
              {[1, 2, 3].map((i) => (
                <div key={i} style={{ background: 'var(--bg)' }} className="h-12 rounded animate-pulse" />
              ))}
            </div>
          ) : filtered.length === 0 ? (
            <p style={{ color: 'var(--text-disabled)' }} className="text-center py-6 text-sm">
              {isHeroMember
                ? '주인공은 장비를 선택하지 않습니다'
                : query.trim()
                  ? '검색 결과가 없습니다'
                  : '전용장비가 없습니다. 검색으로 공용 장비를 찾을 수 있습니다'}
            </p>
          ) : (
            filtered.map((item) => (
              <button key={equipmentItemId(item)} onClick={() => onSelect(item)}
                style={{ background: 'var(--bg)', border: '1px solid var(--border)', textAlign: 'left', width: '100%' }}
                className="rounded-lg px-4 py-2.5 hover:border-[var(--brown)] transition-colors">
                <p className="text-sm font-medium flex items-center gap-1.5" style={{ color: 'var(--text)' }}>
                  {item.name}
                  {isExclusiveForMercenary(item, mercenaryId) && (
                    <span className="text-[10px] px-1.5 py-0.5 rounded" style={{ background: 'var(--beige)', color: 'var(--brown)' }}>전용</span>
                  )}
                </p>
                {item.stats && item.stats.length > 0 && (
                  <p style={{ color: 'var(--text-muted)' }} className="text-xs mt-0.5">
                    {item.stats.slice(0, 2).map((s) => `${s.statType}: ${s.value}`).join(' / ')}
                  </p>
                )}
              </button>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

// ══════════ 주술 설정 패널 ══════════

type EquippedSlotInfo = {
  slot: string;
  itemId: number;
  itemName: string;
  imageUrl?: string;
  ritual?: SlotRitualDto | null;
};

type RitualOption = RitualDto & {
  applicableSlots: EquippedSlotInfo[];
};

function ritualOutcomeMark(ritual: RitualDto, outcome: 'SUCCESS' | 'GREAT_SUCCESS') {
  return outcome === 'GREAT_SUCCESS'
    ? (ritual.greatSuccessMark ?? ritual.successMark ?? '—')
    : (ritual.successMark ?? '—');
}

function resolveAppliedRitualMark(slotRitual: SlotRitualDto, ritualById: Map<number, RitualDto>) {
  const ritual = ritualById.get(slotRitual.ritualId);
  if (ritual) return ritualOutcomeMark(ritual, slotRitual.outcome);
  if (slotRitual.outcome === 'GREAT_SUCCESS') return '<북두칠성>';
  return slotRitual.displayName.startsWith('<') ? slotRitual.displayName : `<${slotRitual.displayName}>`;
}

function buildItemSetIdMap(equipmentBySlot: Record<string, EquipmentItemDto[]>) {
  const map = new Map<number, number>();
  Object.values(equipmentBySlot).forEach((items) => {
    items.forEach((item) => {
      if (item.setId) map.set(item.itemId, item.setId);
    });
  });
  return map;
}

function countSetRitualPieces(
  equippedSlots: EquippedSlotInfo[],
  itemSetIdByItemId: Map<number, number>,
  setId: number | undefined,
  ritualId: number,
  outcome: 'SUCCESS' | 'GREAT_SUCCESS',
) {
  if (!setId) return 0;
  return equippedSlots.filter((slot) => {
    if (itemSetIdByItemId.get(slot.itemId) !== setId) return false;
    if (!slot.ritual) return false;
    return slot.ritual.ritualId === ritualId && slot.ritual.outcome === outcome;
  }).length;
}

function useEquippedRitualCatalog(equippedSlots: EquippedSlotInfo[]) {
  const [ritualOptions, setRitualOptions] = useState<RitualOption[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    (async () => {
      const optionMap = new Map<number, RitualOption>();

      await Promise.all(
        equippedSlots.map(async (equipped) => {
          const rituals = await api.getItemRituals(equipped.itemId).catch(() => [] as RitualDto[]);
          rituals.forEach((ritual) => {
            const existing = optionMap.get(ritual.id);
            if (existing) {
              existing.applicableSlots.push(equipped);
              return;
            }
            optionMap.set(ritual.id, {
              ...ritual,
              applicableSlots: [equipped],
            });
          });
        }),
      );

      if (cancelled) return;

      setRitualOptions([...optionMap.values()].sort((a, b) => a.displayName.localeCompare(b.displayName, 'ko')));
      setLoading(false);
    })();

    return () => { cancelled = true; };
  }, [equippedSlots]);

  const ritualById = useMemo(
    () => new Map(ritualOptions.map((ritual) => [ritual.id, ritual])),
    [ritualOptions],
  );

  return { ritualOptions, ritualById, loading };
}

function RitualSetupPanel({ equippedSlots, equipmentBySlot, deckId, memberId, ritualOptions, ritualById, loading, onApplied }: {
  equippedSlots: EquippedSlotInfo[];
  equipmentBySlot: Record<string, EquipmentItemDto[]>;
  deckId: number;
  memberId: number;
  ritualOptions: RitualOption[];
  ritualById: Map<number, RitualDto>;
  loading: boolean;
  onApplied: () => void | Promise<void>;
}) {
  const [selectedRitualId, setSelectedRitualId] = useState<number | null>(null);
  const [selectedTargetSlots, setSelectedTargetSlots] = useState<string[]>([]);
  const [selectedOutcome, setSelectedOutcome] = useState<'SUCCESS' | 'GREAT_SUCCESS'>('SUCCESS');
  const [saving, setSaving] = useState(false);
  const [removingSlot, setRemovingSlot] = useState<string | null>(null);

  const appliedRituals = useMemo(
    () => equippedSlots.filter((slot) => slot.ritual),
    [equippedSlots],
  );

  useEffect(() => {
    setSelectedRitualId(null);
    setSelectedTargetSlots([]);
    setSelectedOutcome('SUCCESS');
  }, [equippedSlots]);

  const selectedRitual = ritualOptions.find((ritual) => ritual.id === selectedRitualId) ?? null;
  const targetSlots = selectedRitual?.applicableSlots ?? [];
  const itemSetIdByItemId = useMemo(() => buildItemSetIdMap(equipmentBySlot), [equipmentBySlot]);
  const canApplyGreatSuccess = Boolean(selectedRitual?.greatSuccessMark);

  useEffect(() => {
    if (!canApplyGreatSuccess && selectedOutcome === 'GREAT_SUCCESS') {
      setSelectedOutcome('SUCCESS');
    }
  }, [canApplyGreatSuccess, selectedOutcome]);

  function toggleTargetSlot(slot: string) {
    setSelectedTargetSlots((prev) => (
      prev.includes(slot) ? prev.filter((entry) => entry !== slot) : [...prev, slot]
    ));
  }

  function selectAllTargetSlots() {
    setSelectedTargetSlots(targetSlots.map((slot) => slot.slot));
  }

  async function applyRitual() {
    if (!selectedRitual || selectedTargetSlots.length === 0) return;
    setSaving(true);
    try {
      await Promise.all(
        selectedTargetSlots.map((slot) => api.setSlotRitual(deckId, memberId, slot, {
          ritualId: selectedRitual.id,
          outcome: selectedOutcome,
        })),
      );
      await onApplied();
      setSelectedRitualId(null);
      setSelectedTargetSlots([]);
      setSelectedOutcome('SUCCESS');
    } catch (error) {
      console.error('주술 적용 실패', error);
      alert('주술 적용 중 오류가 발생했습니다.');
    } finally {
      setSaving(false);
    }
  }

  async function removeRitual(slot: string) {
    setRemovingSlot(slot);
    try {
      await api.removeSlotRitual(deckId, memberId, slot);
      await onApplied();
    } catch (error) {
      console.error('주술 해제 실패', error);
      alert('주술 해제 중 오류가 발생했습니다.');
    } finally {
      setRemovingSlot(null);
    }
  }

  if (loading) {
    return (
      <div className="space-y-3">
        {[1, 2, 3].map((i) => (
          <div key={i} style={{ background: 'var(--bg)', height: 72 }} className="rounded-lg animate-pulse" />
        ))}
      </div>
    );
  }

  if (equippedSlots.length === 0) {
    return (
      <div style={{ background: 'var(--bg)', border: '1px dashed var(--border)' }} className="rounded-xl p-8 text-center">
        <Sparkles size={28} className="mx-auto mb-2" style={{ color: 'var(--text-muted)' }} />
        <p className="text-sm" style={{ color: 'var(--text-muted)' }}>착용 중인 장비가 없습니다.</p>
        <p className="text-xs mt-1" style={{ color: 'var(--text-disabled)' }}>장비를 먼저 착용한 뒤 주술을 적용할 수 있습니다.</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {appliedRituals.length > 0 && (
        <section>
          <p className="text-xs font-semibold mb-2" style={{ color: 'var(--text-muted)' }}>적용된 주술</p>
          <div className="space-y-2">
            {appliedRituals.map((slot) => (
              <div
                key={slot.slot}
                style={{ background: 'var(--bg)', border: '1px solid var(--border)' }}
                className="rounded-lg px-3 py-2.5 flex items-center gap-3"
              >
                <div className="w-9 h-9 rounded overflow-hidden shrink-0 flex items-center justify-center" style={{ background: 'var(--border)' }}>
                  {slot.imageUrl ? (
                    <img src={slot.imageUrl} alt={slot.itemName} className="w-full h-full object-cover" />
                  ) : (
                    <Sparkles size={16} style={{ color: 'var(--text-muted)' }} />
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-xs font-medium truncate" style={{ color: 'var(--text)' }}>
                    {SLOT_LABEL[slot.slot] ?? slot.slot} · {slot.itemName}
                  </p>
                  <p className="text-[11px] truncate font-medium" style={{ color: 'var(--brown)' }}>
                    {resolveAppliedRitualMark(slot.ritual!, ritualById)}
                  </p>
                </div>
                <button
                  onClick={() => removeRitual(slot.slot)}
                  disabled={removingSlot === slot.slot}
                  style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
                  className="px-2.5 py-1 rounded text-[11px] hover:border-[var(--danger)] hover:text-[var(--danger)] transition-colors disabled:opacity-50"
                >
                  해제
                </button>
              </div>
            ))}
          </div>
        </section>
      )}

      {ritualOptions.length === 0 ? (
        <div style={{ background: 'var(--bg)', border: '1px dashed var(--border)' }} className="rounded-xl p-8 text-center">
          <p className="text-sm" style={{ color: 'var(--text-muted)' }}>착용 중인 장비에 적용 가능한 주술이 없습니다.</p>
        </div>
      ) : (
        <>
          <section>
            <p className="text-xs font-semibold mb-2" style={{ color: 'var(--text-muted)' }}>1. 주술 선택</p>
            <div className="grid sm:grid-cols-2 gap-2">
              {ritualOptions.map((ritual) => {
                const active = selectedRitualId === ritual.id;
                return (
                  <button
                    key={ritual.id}
                    onClick={() => {
                      setSelectedRitualId(ritual.id);
                      setSelectedTargetSlots([]);
                      setSelectedOutcome('SUCCESS');
                    }}
                    style={{
                      background: active ? 'var(--beige)' : 'var(--bg)',
                      border: `1px solid ${active ? 'var(--brown)' : 'var(--border)'}`,
                      color: active ? 'var(--brown)' : 'var(--text)',
                      textAlign: 'left',
                    }}
                    className="rounded-lg px-3 py-2.5 hover:border-[var(--brown)] transition-colors"
                  >
                    <p className="text-sm font-medium">{ritual.displayName}</p>
                    <p className="text-[11px] mt-0.5 font-medium" style={{ color: 'var(--brown)' }}>
                      {ritual.successMark ?? '—'}
                      {ritual.greatSuccessMark ? ` · ${ritual.greatSuccessMark}` : ''}
                    </p>
                    <p className="text-[10px] mt-0.5" style={{ color: 'var(--text-muted)' }}>
                      {ritual.applicableSlots.length}피스 적용 가능
                    </p>
                  </button>
                );
              })}
            </div>
          </section>

          {selectedRitual && (
            <section>
              <p className="text-xs font-semibold mb-2" style={{ color: 'var(--text-muted)' }}>2. 주술 결과</p>
              <div className="flex gap-2">
                <button
                  onClick={() => setSelectedOutcome('SUCCESS')}
                  style={{
                    background: selectedOutcome === 'SUCCESS' ? 'var(--brown)' : 'var(--bg)',
                    color: selectedOutcome === 'SUCCESS' ? 'var(--beige)' : 'var(--text-muted)',
                    border: '1px solid var(--border)',
                  }}
                  className="flex-1 rounded-lg py-2 text-sm font-medium"
                >
                  {ritualOutcomeMark(selectedRitual, 'SUCCESS')}
                </button>
                {canApplyGreatSuccess && (
                  <button
                    onClick={() => setSelectedOutcome('GREAT_SUCCESS')}
                    style={{
                      background: selectedOutcome === 'GREAT_SUCCESS' ? 'var(--brown)' : 'var(--bg)',
                      color: selectedOutcome === 'GREAT_SUCCESS' ? 'var(--beige)' : 'var(--text-muted)',
                      border: '1px solid var(--border)',
                    }}
                    className="flex-1 rounded-lg py-2 text-sm font-medium"
                  >
                    {ritualOutcomeMark(selectedRitual, 'GREAT_SUCCESS')}
                  </button>
                )}
              </div>
            </section>
          )}

          {selectedRitual && (
            <section>
              <div className="flex items-center justify-between mb-2 gap-2">
                <p className="text-xs font-semibold" style={{ color: 'var(--text-muted)' }}>3. 피스 선택 (복수 가능)</p>
                <button
                  onClick={selectAllTargetSlots}
                  style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
                  className="px-2 py-0.5 rounded text-[11px] hover:border-[var(--brown)] hover:text-[var(--brown)] transition-colors"
                >
                  전체 선택
                </button>
              </div>
              <div className="grid sm:grid-cols-2 gap-2">
                {targetSlots.map((slot) => {
                  const active = selectedTargetSlots.includes(slot.slot);
                  const setId = itemSetIdByItemId.get(slot.itemId);
                  const appliedRitual = slot.ritual ?? null;
                  const ritualId = appliedRitual?.ritualId ?? selectedRitual?.id;
                  const outcome = appliedRitual?.outcome ?? selectedOutcome;
                  const pieceCount = ritualId
                    ? countSetRitualPieces(equippedSlots, itemSetIdByItemId, setId, ritualId, outcome)
                    : 0;
                  const ritualName = appliedRitual
                    ? resolveAppliedRitualMark(appliedRitual, ritualById)
                    : selectedRitual
                      ? ritualOutcomeMark(selectedRitual, selectedOutcome)
                      : '—';
                  const hasRitual = Boolean(appliedRitual);
                  return (
                    <button
                      key={slot.slot}
                      onClick={() => toggleTargetSlot(slot.slot)}
                      style={{
                        background: hasRitual ? 'var(--brown)' : active ? 'var(--beige)' : 'var(--bg)',
                        border: `1px solid ${hasRitual || active ? 'var(--brown)' : 'var(--border)'}`,
                        color: hasRitual ? 'var(--beige)' : active ? 'var(--brown)' : 'var(--text)',
                        textAlign: 'left',
                      }}
                      className="rounded-lg px-3 py-2.5 hover:border-[var(--brown)] transition-colors"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0">
                          <p className="text-sm font-semibold">{pieceCount}피스</p>
                          <p className="text-[11px] mt-0.5 font-medium truncate">{ritualName}</p>
                        </div>
                        <span
                          style={{
                            background: active ? (hasRitual ? 'var(--beige)' : 'var(--brown)') : 'transparent',
                            border: `1px solid ${active ? (hasRitual ? 'var(--beige)' : 'var(--brown)') : hasRitual ? 'rgba(232,220,203,0.45)' : 'var(--border)'}`,
                            color: active ? (hasRitual ? 'var(--brown)' : 'var(--beige)') : hasRitual ? 'rgba(232,220,203,0.45)' : 'var(--text-disabled)',
                          }}
                          className="shrink-0 w-4 h-4 rounded text-[10px] leading-4 text-center"
                        >
                          {active ? '✓' : ''}
                        </span>
                      </div>
                    </button>
                  );
                })}
              </div>
              {selectedTargetSlots.length > 0 && (
                <button
                  onClick={applyRitual}
                  disabled={saving}
                  style={{ background: 'var(--brown)', color: 'var(--beige)' }}
                  className="w-full mt-3 rounded-lg py-2.5 text-sm font-semibold disabled:opacity-50"
                >
                  {saving
                    ? '적용 중...'
                    : `${ritualOutcomeMark(selectedRitual, selectedOutcome)} · ${selectedTargetSlots.length}피스 적용`}
                </button>
              )}
            </section>
          )}
        </>
      )}
    </div>
  );
}

function EquipmentSetupModal({ member, isHeroMember, deckId, deckEffectSignature, equipmentBySlot, initialTab = 'equipment', onClose, onRefresh }: {
  member: DeckMember;
  /** 주인공 캐릭터 — 전용장비 목록 미노출 */
  isHeroMember: boolean;
  deckId: number;
  deckEffectSignature?: string;
  equipmentBySlot: Record<string, EquipmentItemDto[]>;
  initialTab?: SetupTab;
  onClose: () => void;
  onRefresh: () => void | Promise<void>;
}) {
  const [activeTab, setActiveTab] = useState<SetupTab>(initialTab);
  const [selectedSlot, setSelectedSlot] = useState(NORMAL_SLOTS[0]);
  const [showAppSlots, setShowAppSlots] = useState(false);
  const [query, setQuery] = useState('');
  const [level, setLevel] = useState<250 | 260>(250);
  const [bonusTarget, setBonusTarget] = useState<'MAIN_STAT' | 'VITALITY'>('MAIN_STAT');
  const [bonusAmount, setBonusAmount] = useState(0);
  const [buildSaving, setBuildSaving] = useState(false);
  const [stats, setStats] = useState<MemberStats | null>(null);
  const [statsLoading, setStatsLoading] = useState(false);
  const [characteristics, setCharacteristics] = useState<MemberCharacteristicDto | null>(null);
  const [characteristicsLoading, setCharacteristicsLoading] = useState(false);
  const [characteristicsSaving, setCharacteristicsSaving] = useState(false);
  const characteristicSaveQueue = useRef(Promise.resolve());
  const equippedSlots = useMemo(() => {
    if (stats?.slots && stats.slots.length > 0) return stats.slots;
    return member.slots;
  }, [stats?.slots, member.slots]);
  const { ritualOptions, ritualById, loading: ritualCatalogLoading } = useEquippedRitualCatalog(equippedSlots);
  const slotMap = useMemo(() => new Map(equippedSlots.map((s) => [s.slot, s])), [equippedSlots]);
  const selectedEquipped = slotMap.get(selectedSlot);
  const slotRows = showAppSlots ? APP_SLOT_ROWS : NORMAL_SLOT_ROWS;
  const displayTotalMap = useMemo(() => computeDisplayTotalMap(stats), [stats]);
  const characteristicGrid = useMemo(
    () => layoutCharacteristicGrid(characteristics?.characteristics ?? []),
    [characteristics]
  );
  const selectedCharacteristicEntries = useMemo(() => (
    characteristics?.characteristics
      .filter((c) => c.selectedLevel)
      .map((c) => ({ characteristicId: c.characteristicId, selectedLevel: c.selectedLevel as number })) ?? []
  ), [characteristics]);
  const usedCharacteristicPoints = useMemo(() => characteristicUsedPoints(characteristics), [characteristics]);
  const maxCharacteristicPoints = useMemo(() => characteristicMaxPoints(characteristics), [characteristics]);
  const [exclusiveItems, setExclusiveItems] = useState<EquipmentItemDto[]>([]);
  const [exclusiveLoading, setExclusiveLoading] = useState(false);

  useEffect(() => {
    if (isHeroMember || activeTab !== 'equipment') {
      setExclusiveItems([]);
      return;
    }
    let cancelled = false;
    setExclusiveLoading(true);
    api.getExclusiveEquipment(member.mercenaryId, selectedSlot)
      .then((items) => { if (!cancelled) setExclusiveItems(items); })
      .catch(() => { if (!cancelled) setExclusiveItems([]); })
      .finally(() => { if (!cancelled) setExclusiveLoading(false); });
    return () => { cancelled = true; };
  }, [member.mercenaryId, selectedSlot, isHeroMember, activeTab]);

  const displayedItems = useMemo(() => {
    if (isHeroMember) return [];
    return buildDeckEquipmentDisplayItems(
      exclusiveItems,
      equipmentBySlot[selectedSlot] ?? [],
      member.mercenaryId,
      selectedSlot,
      query,
    );
  }, [exclusiveItems, equipmentBySlot, selectedSlot, member.mercenaryId, query, isHeroMember]);

  const loadStats = useCallback(() => {
    setStatsLoading(true);
    return api.getDeckMemberStats(deckId, member.id)
      .then((nextStats) => {
        setStats(nextStats);
        if (nextStats.level === 250 || nextStats.level === 260) {
          setLevel(nextStats.level);
        }
        if (nextStats.bonusTarget === 'MAIN_STAT' || nextStats.bonusTarget === 'VITALITY') {
          setBonusTarget(nextStats.bonusTarget);
        }
        if (typeof nextStats.bonusAmount === 'number') {
          setBonusAmount(nextStats.bonusAmount);
        }
      })
      .catch(() => setStats(null))
      .finally(() => setStatsLoading(false));
  }, [deckId, member.id]);

  const loadCharacteristics = useCallback(() => {
    setCharacteristicsLoading(true);
    api.getDeckMemberCharacteristics(deckId, member.id)
      .then((nextCharacteristics) => {
        setCharacteristics(nextCharacteristics);
        if (nextCharacteristics.level === 250 || nextCharacteristics.level === 260) {
          setLevel(nextCharacteristics.level as 250 | 260);
        }
      })
      .catch(() => setCharacteristics(null))
      .finally(() => setCharacteristicsLoading(false));
  }, [deckId, member.id]);

  useEffect(() => {
    loadStats();
    loadCharacteristics();
  }, [loadStats, loadCharacteristics, deckEffectSignature]);

  async function equipPiece(item: ItemSearchResult) {
    const slot = selectedSlot;
    if (slot === RITUAL_SLOT) return;
    try {
      await api.equipSlot(deckId, member.id, slot, { itemId: item.id });
      await Promise.all([onRefresh(), loadStats()]);
    } catch (error) {
      console.error('장비 착용 실패', { item, slot, error });
      alert('장비 착용 중 오류가 발생했습니다.');
    }
  }

  async function equipSet(item: ItemSearchResult) {
    if (!item.setId) {
      alert('세트 장착은 세트 장비에만 사용할 수 있습니다.');
      return;
    }
    try {
      await api.equipSet(deckId, member.id, item.setId);
      await Promise.all([onRefresh(), loadStats()]);
    } catch (error) {
      console.error('세트 장착 실패', { item, error });
      alert('세트 장착 중 오류가 발생했습니다. 세트 피스 데이터가 등록되어 있는지 확인해 주세요.');
    }
  }

  async function saveBuild(next: { level: 250 | 260; bonusTarget: 'MAIN_STAT' | 'VITALITY'; bonusAmount: number }) {
    const previous = { level, bonusTarget, bonusAmount };
    setLevel(next.level);
    setBonusTarget(next.bonusTarget);
    setBonusAmount(next.bonusAmount);
    setBuildSaving(true);
    try {
      await api.updateDeckMemberBuild(deckId, member.id, next);
      await Promise.all([loadStats(), loadCharacteristics(), onRefresh()]);
    } catch (error) {
      console.error('빌드 설정 저장 실패', { next, error });
      setLevel(previous.level);
      setBonusTarget(previous.bonusTarget);
      setBonusAmount(previous.bonusAmount);
      alert('빌드 설정 저장 중 오류가 발생했습니다.');
    } finally {
      setBuildSaving(false);
    }
  }

  async function changeLevel(nextLevel: 250 | 260) {
    await saveBuild({ level: nextLevel, bonusTarget, bonusAmount });
  }

  async function changeBonusTarget(nextTarget: 'MAIN_STAT' | 'VITALITY') {
    await saveBuild({ level, bonusTarget: nextTarget, bonusAmount });
  }

  async function changeBonusAmount(nextAmount: number) {
    await saveBuild({ level, bonusTarget, bonusAmount: Math.max(0, nextAmount) });
  }

  async function changeCharacteristicLevel(characteristicId: number, selectedLevel?: number) {
    if (!characteristics) return;

    const nextEntries = characteristics.characteristics
      .map((characteristic) => {
        if (characteristic.characteristicId === characteristicId) {
          return selectedLevel
            ? { characteristicId: characteristic.characteristicId, selectedLevel }
            : null;
        }
        return characteristic.selectedLevel
          ? { characteristicId: characteristic.characteristicId, selectedLevel: characteristic.selectedLevel }
          : null;
      })
      .filter((entry): entry is { characteristicId: number; selectedLevel: number } => entry !== null);

    const pointByCharacteristicId = new Map(
      characteristics.characteristics.map((characteristic) => [
        characteristic.characteristicId,
        characteristic.point,
      ])
    );
    const nextUsedPoints = nextEntries.reduce(
      (sum, entry) =>
        sum + characteristicSelectionCost(
          pointByCharacteristicId.get(entry.characteristicId),
          entry.selectedLevel,
        ),
      0,
    );
    if (maxCharacteristicPoints != null && nextUsedPoints > maxCharacteristicPoints) {
      alert(`특성 포인트는 최대 ${maxCharacteristicPoints}까지 사용할 수 있습니다. 현재 선택: ${nextUsedPoints}`);
      return;
    }

    const previousCharacteristics = characteristics;
    setCharacteristics({
      ...characteristics,
      characteristics: characteristics.characteristics.map((characteristic) => (
        characteristic.characteristicId === characteristicId
          ? { ...characteristic, selectedLevel }
          : characteristic
      )),
    });
    setCharacteristicsSaving(true);
    const saveTask = characteristicSaveQueue.current.then(() =>
      api.setDeckMemberCharacteristics(deckId, member.id, { characteristics: nextEntries })
    );
    characteristicSaveQueue.current = saveTask.catch(() => undefined);
    try {
      await saveTask;
      await Promise.all([loadStats(), loadCharacteristics(), onRefresh()]);
    } catch (error) {
      console.error('특성 저장 실패', { characteristicId, selectedLevel, error });
      setCharacteristics(previousCharacteristics);
      const message = error instanceof Error ? error.message : '특성 저장 중 오류가 발생했습니다.';
      alert(message.includes(':') ? message.split(': ').slice(1).join(': ') : message);
    } finally {
      setCharacteristicsSaving(false);
    }
  }

  async function unequip() {
    if (selectedSlot === RITUAL_SLOT) return;
    try {
      await api.unequipSlot(deckId, member.id, selectedSlot);
      await Promise.all([onRefresh(), loadStats()]);
    } catch {
      alert('장비 해제 중 오류가 발생했습니다.');
    }
  }

  const ritualAppliedCount = useMemo(
    () => equippedSlots.filter((slot) => slot.ritual).length,
    [equippedSlots],
  );

  return (
    <div className="fixed inset-0 flex items-center justify-center z-[500] p-4" style={{ background: 'rgba(0,0,0,0.65)' }} onClick={onClose}>
      <div
        style={{ background: 'var(--card)', border: '1px solid var(--border)', width: 1120, maxWidth: '96vw', height: 680, maxHeight: '76vh' }}
        className="rounded-xl overflow-hidden flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ borderBottom: '1px solid var(--border)' }} className="flex items-center justify-between px-5 py-3 shrink-0">
          <div>
            <h2 className="font-semibold" style={{ color: 'var(--text)' }}>{member.mercenaryName} 설정</h2>
            <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>장비 · 특성 · 스탯을 설정합니다</p>
          </div>
          <button onClick={onClose} style={{ color: 'var(--text-muted)' }} className="text-xl hover:text-[var(--text)]">×</button>
        </div>

        <div style={{ borderBottom: '1px solid var(--border)' }} className="flex gap-2 px-5 py-2.5 shrink-0">
          {([
            ['equipment', '장비 착용'],
            ['characteristic', '특성'],
            ['stats', '스탯 요약'],
          ] as const).map(([key, label]) => {
            const active = activeTab === key;
            return (
              <button
                key={key}
                onClick={() => setActiveTab(key)}
                style={{
                  background: active ? 'var(--brown)' : 'var(--bg)',
                  color: active ? 'var(--beige)' : 'var(--text-muted)',
                  border: '1px solid var(--border)',
                }}
                className="rounded px-4 py-2 text-sm font-medium"
              >
                {label}
              </button>
            );
          })}
        </div>

        <div className="flex-1 overflow-hidden min-h-0">
          {activeTab === 'equipment' && (
            <div className="grid md:grid-cols-[minmax(400px,44%)_minmax(0,1fr)] h-full overflow-hidden">
              <div
                style={{ borderRight: '1px solid var(--border)', background: 'var(--bg)' }}
                className="grid grid-cols-[minmax(128px,34%)_minmax(0,1fr)] h-full overflow-hidden"
              >
                <div style={{ borderRight: '1px solid var(--border)' }} className="p-3 overflow-y-auto">
                  <EquipStatTable stats={stats} loading={statsLoading} compact />
                </div>

                <div className="p-3 overflow-y-auto min-w-0">
                <div className="flex gap-1.5 mb-2">
                  <button
                    onClick={() => { setShowAppSlots(false); setSelectedSlot((prev) => prev === RITUAL_SLOT ? RITUAL_SLOT : NORMAL_SLOTS[0]); }}
                    style={{
                      background: !showAppSlots ? 'var(--brown)' : 'var(--card)',
                      color: !showAppSlots ? 'var(--beige)' : 'var(--text-muted)',
                      border: '1px solid var(--border)',
                    }}
                    className="flex-1 rounded py-1 text-[11px] font-medium"
                  >
                    노멀타입
                  </button>
                  <button
                    onClick={() => { setShowAppSlots(true); setSelectedSlot(APP_SLOTS[0]); }}
                    style={{
                      background: showAppSlots ? 'var(--brown)' : 'var(--card)',
                      color: showAppSlots ? 'var(--beige)' : 'var(--text-muted)',
                      border: '1px solid var(--border)',
                    }}
                    className="flex-1 rounded py-1 text-[11px] font-medium"
                  >
                    외변타입
                  </button>
                </div>

                <div className="space-y-1.5">
                  {slotRows.map((row, rowIndex) => (
                    <div key={rowIndex} className="grid grid-cols-2 gap-1.5">
                      {row.map((slot, colIndex) => {
                        if (!slot) return <div key={`empty-${rowIndex}-${colIndex}`} />;
                        if (slot === RITUAL_SLOT) {
                          const active = selectedSlot === RITUAL_SLOT;
                          return (
                            <button
                              key={slot}
                              onClick={() => setSelectedSlot(RITUAL_SLOT)}
                              style={{
                                background: active ? 'var(--beige)' : 'var(--card)',
                                border: `1px ${ritualAppliedCount > 0 ? 'solid var(--brown)' : 'dashed var(--border)'}`,
                                color: active || ritualAppliedCount > 0 ? 'var(--brown)' : 'var(--text-muted)',
                              }}
                              className="aspect-square w-full rounded p-1.5 flex flex-col text-left hover:border-[var(--brown)] transition-colors overflow-hidden"
                            >
                              <div
                                className="flex-1 min-h-0 w-full rounded overflow-hidden flex items-center justify-center mb-0.5"
                                style={{ background: 'var(--bg)' }}
                              >
                                <Sparkles size={22} style={{ color: active ? 'var(--brown)' : 'var(--text-muted)' }} />
                              </div>
                              <p className="text-[9px] font-medium truncate leading-tight">주술</p>
                              <p className="text-[8px] truncate leading-tight" style={{ color: ritualAppliedCount > 0 ? 'var(--brown)' : 'var(--text-disabled)' }}>
                                {ritualAppliedCount > 0 ? `${ritualAppliedCount}개 적용` : '미적용'}
                              </p>
                            </button>
                          );
                        }
                        const equipped = slotMap.get(slot);
                        const active = selectedSlot === slot;
                        return (
                          <button
                            key={slot}
                            onClick={() => setSelectedSlot(slot)}
                            style={{
                              background: active ? 'var(--beige)' : 'var(--card)',
                              border: `1px ${equipped ? 'solid var(--brown)' : 'dashed var(--border)'}`,
                              color: active || equipped ? 'var(--brown)' : 'var(--text-muted)',
                            }}
                            className="aspect-square w-full rounded p-1.5 flex flex-col text-left hover:border-[var(--brown)] transition-colors overflow-hidden"
                          >
                            <div
                              className="flex-1 min-h-0 w-full rounded overflow-hidden flex items-center justify-center mb-0.5"
                              style={{ background: equipped?.imageUrl ? 'var(--border)' : 'transparent' }}
                            >
                              {equipped?.imageUrl ? (
                                <img src={equipped.imageUrl} alt={equipped.itemName} className="w-full h-full object-cover" />
                              ) : (
                                <span className="text-[10px] font-medium text-center px-1 leading-tight">
                                  {SLOT_LABEL[slot] ?? slot}
                                </span>
                              )}
                            </div>
                            {equipped?.imageUrl && (
                              <p className="text-[9px] font-medium truncate leading-tight">{SLOT_LABEL[slot] ?? slot}</p>
                            )}
                            <p className="text-[8px] truncate leading-tight" style={{ color: equipped ? 'var(--brown)' : 'var(--text-disabled)' }}>
                              {equipped?.itemName ?? '미착용'}
                            </p>
                            {equipped?.ritual && (
                              <p className="text-[8px] truncate leading-tight font-medium" style={{ color: 'var(--brown)' }}>
                                {resolveAppliedRitualMark(equipped.ritual, ritualById)}
                              </p>
                            )}
                          </button>
                        );
                      })}
                    </div>
                  ))}
                </div>
                </div>
              </div>

              <div className="p-3 overflow-y-auto min-w-0">
                {selectedSlot === RITUAL_SLOT ? (
                  <>
                    <div className="mb-3">
                      <p className="text-sm font-semibold" style={{ color: 'var(--text)' }}>주술 설정</p>
                      <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
                        착용 중인 장비에서 적용 가능한 주술을 선택하고 피스에 적용합니다
                      </p>
                    </div>
                    <RitualSetupPanel
                      equippedSlots={equippedSlots}
                      equipmentBySlot={equipmentBySlot}
                      deckId={deckId}
                      memberId={member.id}
                      ritualOptions={ritualOptions}
                      ritualById={ritualById}
                      loading={ritualCatalogLoading}
                      onApplied={async () => { await Promise.all([onRefresh(), loadStats()]); }}
                    />
                  </>
                ) : (
                  <>
                <div className="flex items-center justify-between mb-3 gap-2">
                  <div>
                    <p className="text-sm font-semibold" style={{ color: 'var(--text)' }}>아이템 검색</p>
                    <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
                      {isHeroMember
                        ? '주인공은 전용장비 목록을 표시하지 않습니다'
                        : `먼저 ${member.mercenaryName} 전용장비를 표시합니다. 검색하면 공용 장비도 착용할 수 있습니다`}
                    </p>
                  </div>
                  {selectedEquipped && (
                    <button
                      onClick={unequip}
                      style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
                      className="px-3 py-1.5 rounded text-xs hover:border-[var(--danger)] hover:text-[var(--danger)] transition-colors"
                    >
                      해제
                    </button>
                  )}
                </div>

                {!isHeroMember && (
                  <input
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder={`${SLOT_LABEL[selectedSlot] ?? selectedSlot} 공용 장비 검색...`}
                    style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
                    className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)] mb-3"
                  />
                )}

                {exclusiveLoading ? (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(110px, 1fr))', gap: 10 }}>
                    {[1, 2, 3, 4, 5, 6].map((i) => <div key={i} style={{ background: 'var(--bg)', height: 145 }} className="rounded animate-pulse" />)}
                  </div>
                ) : displayedItems.length === 0 ? (
                  <p className="text-center py-8 text-sm" style={{ color: 'var(--text-disabled)' }}>
                    {isHeroMember
                      ? '주인공은 이 화면에서 장비를 선택하지 않습니다'
                      : query.trim()
                        ? '검색 결과가 없습니다'
                        : `전용장비가 없습니다. 검색으로 공용 장비를 찾을 수 있습니다`}
                  </p>
                ) : (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(110px, 1fr))', gap: 10 }}>
                    {displayedItems.map((item) => {
                      const isExclusive = isExclusiveForMercenary(item, member.mercenaryId);
                      return (
                      <div
                        key={item.id}
                        style={{
                          background: 'var(--bg)',
                          border: `1px solid ${isExclusive ? 'var(--brown)' : 'var(--border)'}`,
                          overflow: 'hidden',
                        }}
                        className="rounded-lg relative group"
                      >
                        <div style={{ aspectRatio: '1 / 1', background: 'var(--border)' }} className="relative">
                          {item.imageUrl ? (
                            <img src={item.imageUrl} alt={item.name} className="w-full h-full object-cover" />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center">
                              <span className="font-serif text-2xl font-bold" style={{ color: 'var(--text-muted)' }}>
                                {item.name.charAt(0)}
                              </span>
                            </div>
                          )}
                          <div
                            className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity flex flex-col items-center justify-center gap-1.5 p-2"
                            style={{ background: 'rgba(0,0,0,0.58)' }}
                          >
                            <button
                              onClick={() => equipPiece(item)}
                              style={{ background: 'var(--brown)', color: 'var(--beige)', width: '100%' }}
                              className="rounded py-1.5 text-[11px] font-semibold"
                            >
                              피스 장착
                            </button>
                            {item.setId ? (
                              <button
                                onClick={() => equipSet(item)}
                                style={{ background: 'var(--card)', color: 'var(--text)', width: '100%' }}
                                className="rounded py-1.5 text-[11px] font-semibold"
                              >
                                세트 장착
                              </button>
                            ) : null}
                          </div>
                        </div>
                        <div className="px-2 py-1.5">
                          <p className="text-xs font-medium truncate text-center" style={{ color: 'var(--text)' }}>{item.name}</p>
                          {isExclusive && (
                            <p className="text-[10px] text-center font-semibold" style={{ color: 'var(--brown)' }}>전용</p>
                          )}
                          {item.setName && (
                            <p className="text-[10px] truncate text-center" style={{ color: 'var(--text-muted)' }}>{item.setName}</p>
                          )}
                        </div>
                      </div>
                    );
                    })}
                  </div>
                )}
                  </>
                )}
              </div>
            </div>
          )}

          {activeTab === 'characteristic' && (
            <div className="h-full overflow-y-auto p-5">
              <div className="flex flex-wrap items-center justify-end gap-3 mb-4">
                <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
                  {maxCharacteristicPoints != null
                    ? `${usedCharacteristicPoints}/${maxCharacteristicPoints}pt`
                    : `${usedCharacteristicPoints}pt`}
                  {' · '}선택 {selectedCharacteristicEntries.length}개
                </p>
              </div>

              {characteristicsLoading ? (
                <div style={{ aspectRatio: '16 / 10', background: 'var(--bg)' }} className="rounded-xl animate-pulse" />
              ) : !characteristics || characteristics.characteristics.length === 0 ? (
                <div style={{ background: 'var(--card)', border: '1px dashed var(--border)' }} className="rounded-xl p-8 text-center">
                  <p className="text-sm" style={{ color: 'var(--text-muted)' }}>선택 가능한 특성이 없습니다.</p>
                </div>
              ) : (
                <>
                  <div
                    style={{ border: '1px solid var(--border)', background: 'var(--bg)' }}
                    className="rounded-xl p-4 max-w-3xl mx-auto relative"
                  >
                    <div className="pointer-events-none absolute inset-4 z-0">
                      <div className="absolute left-1/2 top-0 bottom-0 w-px -translate-x-1/2" style={{ background: 'var(--border)' }} />
                      <div className="absolute top-1/2 left-0 right-0 h-px -translate-y-1/2" style={{ background: 'var(--border)' }} />
                    </div>
                    <div className="relative z-10 grid grid-cols-2 grid-rows-2 gap-4 min-h-[420px]">
                      <CharacteristicQuadrantCard
                        characteristic={characteristicGrid.grid[0]?.lower ?? null}
                        saving={characteristicsSaving}
                        onSelectLevel={changeCharacteristicLevel}
                      />
                      <CharacteristicQuadrantCard
                        characteristic={characteristicGrid.grid[1]?.lower ?? null}
                        saving={characteristicsSaving}
                        onSelectLevel={changeCharacteristicLevel}
                      />
                      <CharacteristicQuadrantCard
                        characteristic={characteristicGrid.grid[0]?.upper ?? null}
                        saving={characteristicsSaving}
                        onSelectLevel={changeCharacteristicLevel}
                      />
                      <CharacteristicQuadrantCard
                        characteristic={characteristicGrid.grid[1]?.upper ?? null}
                        saving={characteristicsSaving}
                        onSelectLevel={changeCharacteristicLevel}
                      />
                    </div>
                  </div>

                  {characteristicGrid.overflow.length > 0 && (
                    <div className="mt-4 max-w-3xl mx-auto grid sm:grid-cols-2 gap-3">
                      {characteristicGrid.overflow.map((c) => (
                        <CharacteristicQuadrantCard
                          key={c.characteristicId}
                          characteristic={c}
                          saving={characteristicsSaving}
                          onSelectLevel={changeCharacteristicLevel}
                        />
                      ))}
                    </div>
                  )}

                  {characteristicGrid.auto.length > 0 && (
                    <div className="mt-4 max-w-3xl mx-auto">
                      <p className="text-xs font-semibold mb-2" style={{ color: 'var(--text-muted)' }}>자동 적용 특성</p>
                      <div className="flex flex-wrap gap-2">
                        {characteristicGrid.auto.map((c) => (
                          <span
                            key={c.characteristicId}
                            style={{ background: 'var(--card)', border: '1px solid var(--border)', color: 'var(--text-muted)' }}
                            className="rounded px-3 py-1.5 text-xs"
                          >
                            {c.name}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          )}

          {activeTab === 'stats' && (
            <div className="grid md:grid-cols-2 h-full overflow-hidden">
              {/* 좌측: 빌드 설정 */}
              <div style={{ borderRight: '1px solid var(--border)', background: 'var(--bg)' }} className="p-5 overflow-y-auto">
                <div
                  style={{ border: '1px solid var(--border)', background: 'var(--card)' }}
                  className="rounded-lg p-4"
                >
                  <p className="text-sm font-semibold mb-3" style={{ color: 'var(--text)' }}>빌드 설정</p>
                  <div className="space-y-3">
                    <div>
                      <p className="text-xs mb-1.5" style={{ color: 'var(--text-muted)' }}>레벨</p>
                      <div className="flex gap-2">
                        {[250, 260].map((lv) => (
                          <button
                            key={lv}
                            disabled={buildSaving}
                            onClick={() => changeLevel(lv as 250 | 260)}
                            style={{
                              background: level === lv ? 'var(--brown)' : 'var(--bg)',
                              color: level === lv ? 'var(--beige)' : 'var(--text-muted)',
                              border: '1px solid var(--border)',
                            }}
                            className="rounded px-4 py-1.5 text-sm font-medium disabled:opacity-60"
                          >
                            Lv.{lv}
                          </button>
                        ))}
                      </div>
                    </div>
                    <div>
                      <p className="text-xs mb-1.5" style={{ color: 'var(--text-muted)' }}>보너스 스탯 투자</p>
                      <div className="flex gap-2">
                        {(['MAIN_STAT', 'VITALITY'] as const).map((target) => (
                          <button
                            key={target}
                            disabled={buildSaving}
                            onClick={() => changeBonusTarget(target)}
                            style={{
                              background: bonusTarget === target ? 'var(--brown)' : 'var(--bg)',
                              color: bonusTarget === target ? 'var(--beige)' : 'var(--text-muted)',
                              border: '1px solid var(--border)',
                            }}
                            className="rounded px-3 py-1.5 text-xs font-medium disabled:opacity-60"
                          >
                            {BONUS_TARGET_LABEL[target]}
                          </button>
                        ))}
                      </div>
                    </div>
                    <div>
                      <p className="text-xs mb-1.5" style={{ color: 'var(--text-muted)' }}>보너스 스탯 총량</p>
                      <div className="flex flex-wrap gap-1.5">
                        {BONUS_AMOUNT_PRESETS.map((amount) => (
                          <button
                            key={amount}
                            disabled={buildSaving}
                            onClick={() => changeBonusAmount(amount)}
                            style={{
                              background: bonusAmount === amount ? 'var(--brown)' : 'var(--bg)',
                              color: bonusAmount === amount ? 'var(--beige)' : 'var(--text-muted)',
                              border: '1px solid var(--border)',
                            }}
                            className="rounded px-2.5 py-1 text-xs font-medium disabled:opacity-60"
                          >
                            {amount === 0 ? '없음' : amount}
                          </button>
                        ))}
                      </div>
                      <div className="flex items-center gap-2 mt-2">
                        <span className="text-xs" style={{ color: 'var(--text-muted)' }}>직접 입력</span>
                        <input
                          type="number"
                          min={0}
                          step={1}
                          value={bonusAmount}
                          disabled={buildSaving}
                          onChange={(e) => {
                            const parsed = Number.parseInt(e.target.value, 10);
                            if (!Number.isNaN(parsed)) setBonusAmount(Math.max(0, parsed));
                          }}
                          onBlur={() => changeBonusAmount(bonusAmount)}
                          style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
                          className="w-24 rounded px-2 py-1 text-xs"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* 우측: 최종 스탯 — 4열 고정 표 */}
              <div className="p-5 overflow-y-auto">
                <p className="text-sm font-semibold mb-1" style={{ color: 'var(--text)' }}>최종 스탯</p>
                <p className="text-xs mb-4" style={{ color: 'var(--text-muted)' }}>
                  ? 를 누르면 스탯 출처를 확인할 수 있습니다.
                </p>
                {statsLoading ? (
                  <div style={{ background: 'var(--card)' }} className="h-40 rounded animate-pulse" />
                ) : (
                  <div style={{ border: '1px solid var(--border)' }} className="rounded-lg overflow-hidden text-sm">
                    {/* 헤더 */}
                    <div
                      className="grid grid-cols-4 text-xs font-semibold px-3 py-2"
                      style={{ background: 'var(--bg)', color: 'var(--text-muted)', borderBottom: '1px solid var(--border)' }}
                    >
                      <span>스탯</span>
                      <span className="text-right pr-4">값</span>
                      <span>스탯</span>
                      <span className="text-right pr-2">값</span>
                    </div>
                    {/* 데이터 행 */}
                    {Array.from({ length: FIXED_STAT_ROWS }).map((_, i) => {
                      const leftType = LEFT_STAT_COLS[i] as string | undefined;
                      const rightType = RIGHT_STAT_COLS[i] as string | undefined;
                      const leftVal = leftType ? (displayTotalMap.get(leftType) ?? 0) : null;

                      // 공격력 행: MIN_POWER - MAX_POWER 범위 표시
                      const isAtkRow = rightType === 'ATTACK_POWER';
                      const minPow = displayTotalMap.get('MIN_POWER') ?? 0;
                      const maxPow = displayTotalMap.get('MAX_POWER') ?? 0;
                      const rightVal = isAtkRow ? null : (rightType ? (displayTotalMap.get(rightType) ?? 0) : null);

                      return (
                        <div
                          key={i}
                          className="grid grid-cols-4 items-center px-3 py-2.5"
                          style={{ borderTop: '1px solid var(--border)' }}
                        >
                          {/* 좌측 스탯명 + ? */}
                          <span className="flex items-center" style={{ color: 'var(--text-muted)' }}>
                            {leftType && formatStatType(leftType)}
                            {leftType && (
                              <StatContributionPopover
                                statType={leftType}
                                contributions={buildFullStatContributions(stats, leftType)}
                                total={leftVal ?? 0}
                                compact
                              />
                            )}
                          </span>
                          {/* 좌측 값 */}
                          <span className="text-right pr-4 font-semibold" style={{ color: 'var(--brown)' }}>
                            {leftVal !== null ? leftVal : ''}
                          </span>
                          {/* 우측 스탯명 */}
                          <span className="flex items-center" style={{ color: 'var(--text-muted)' }}>
                            {isAtkRow ? '공격력' : (rightType && formatStatType(rightType))}
                            {isAtkRow ? (
                              <StatContributionPopover
                                statType="MIN_POWER"
                                contributions={[
                                  ...buildFullStatContributions(stats, 'MIN_POWER'),
                                  ...buildFullStatContributions(stats, 'MAX_POWER'),
                                ]}
                                total={minPow + maxPow}
                                compact
                              />
                            ) : (rightType && (
                              <StatContributionPopover
                                statType={rightType}
                                contributions={buildFullStatContributions(stats, rightType)}
                                total={rightVal ?? 0}
                                compact
                              />
                            ))}
                          </span>
                          {/* 우측 값 */}
                          <span className="text-right pr-2 font-semibold" style={{ color: 'var(--brown)' }}>
                            {isAtkRow
                              ? (minPow === 0 && maxPow === 0 ? '-' : `${minPow} - ${maxPow}`)
                              : (rightVal !== null ? rightVal : '')}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                )}
                {/* 전설장수 아군 속성 버프 */}
                {(stats?.lgAllyElementalStats?.length ?? 0) > 0 && (
                  <div className="mt-4">
                    <p className="text-xs font-semibold mb-2" style={{ color: 'var(--text-muted)' }}>전설장수(아군) 속성 버프</p>
                    <div style={{ border: '1px solid var(--border)' }} className="rounded-lg overflow-hidden text-xs">
                      {stats!.lgAllyElementalStats!.map((e) => (
                        <div
                          key={e.statKey}
                          className="flex justify-between items-center px-3 py-2"
                          style={{ borderTop: '1px solid var(--border)' }}
                        >
                          <span style={{ color: 'var(--text-muted)' }}>
                            {COMPOUND_STAT_LABEL[e.statKey] ?? e.statKey}
                          </span>
                          <span className="font-semibold" style={{ color: 'var(--brown)' }}>
                            +{e.value}%
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ══════════ 몬스터 선택 모달 ══════════

function MonsterModal({ onSelect, onClose }: {
  onSelect: (m: MonsterDto) => void;
  onClose: () => void;
}) {
  const [monsters, setMonsters] = useState<MonsterDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.getMonsters().then(setMonsters).catch(() => setMonsters([]))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="fixed inset-0 flex items-center justify-center z-[500] p-4" style={{ background: 'rgba(0,0,0,0.65)' }} onClick={onClose}>
      <div style={{ background: 'var(--card)', border: '1px solid var(--border)', width: 460, maxHeight: '70vh' }}
        className="rounded-xl overflow-hidden flex flex-col"
        onClick={(e) => e.stopPropagation()}>
        <div style={{ borderBottom: '1px solid var(--border)' }} className="flex items-center justify-between px-5 py-3.5 shrink-0">
          <h2 className="font-semibold" style={{ color: 'var(--text)' }}>몬스터 선택</h2>
          <button onClick={onClose} style={{ color: 'var(--text-muted)' }} className="text-xl">×</button>
        </div>
        <div className="overflow-y-auto flex-1 p-3 space-y-1">
          {loading ? (
            <div className="space-y-2 p-2">
              {[1, 2, 3].map((i) => <div key={i} style={{ background: 'var(--bg)' }} className="h-12 rounded animate-pulse" />)}
            </div>
          ) : monsters.length === 0 ? (
            <p style={{ color: 'var(--text-disabled)' }} className="text-center py-6 text-sm">몬스터 데이터가 없습니다</p>
          ) : (
            monsters.map((m) => (
              <button key={m.id} onClick={() => onSelect(m)}
                style={{ background: 'var(--bg)', border: '1px solid var(--border)', textAlign: 'left', width: '100%' }}
                className="rounded-lg px-4 py-2.5 hover:border-[var(--brown)] transition-colors">
                <p className="text-sm font-medium" style={{ color: 'var(--text)' }}>{m.name}</p>
                <p style={{ color: 'var(--text-muted)' }} className="text-xs mt-0.5">
                  {m.element ? `속성: ${ELEMENT_LABELS[m.element] ?? m.element}` : ''}
                  {m.resistance !== undefined ? `  저항: ${m.resistance}` : ''}
                  {m.elementValue !== undefined ? `  속성값: ${m.elementValue}` : ''}
                </p>
              </button>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

// ══════════ 멤버 슬롯 카드 ══════════

function MemberCard({ member, deckId, equipmentBySlot, onRemove, onRefresh }: {
  member: DeckMember;
  deckId: number;
  equipmentBySlot: Record<string, EquipmentItemDto[]>;
  onRemove: () => void;
  onRefresh: () => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const [equipModal, setEquipModal] = useState<string | null>(null);
  const [showApp, setShowApp] = useState(false);

  const slotMap = new Map(member.slots.map((s) => [s.slot, s]));

  async function handleEquip(slot: string, item: EquipmentItemDto) {
    try {
      await api.equipSlot(deckId, member.id, slot, { itemId: equipmentItemId(item) });
      onRefresh();
    } catch {
      alert('장비 착용 중 오류가 발생했습니다.');
    } finally {
      setEquipModal(null);
    }
  }

  async function handleUnequip(slot: string) {
    try {
      await api.unequipSlot(deckId, member.id, slot);
      onRefresh();
    } catch {
      alert('장비 해제 중 오류가 발생했습니다.');
    }
  }

  return (
    <div style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl overflow-hidden">
      <div className="flex items-center justify-between px-4 py-3">
        <div className="flex items-center gap-3">
          <div style={{ background: 'var(--brown)', color: 'var(--beige)', width: 36, height: 36 }}
            className="rounded-full flex items-center justify-center text-sm font-bold font-serif shrink-0">
            {member.mercenaryName?.charAt(0)}
          </div>
          <div>
            <p className="font-medium text-sm" style={{ color: 'var(--text)' }}>{member.mercenaryName}</p>
            <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
              장비 {member.slots.length}개
            </p>
          </div>
        </div>
        <div className="flex gap-1">
          <button onClick={() => setExpanded(!expanded)}
            style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
            className="text-xs px-2.5 py-1 rounded hover:border-[var(--brown)] hover:text-[var(--brown)] transition-colors flex items-center gap-1">
            장비 설정
            {expanded ? <ChevronUp style={{ width: 12, height: 12 }} /> : <ChevronDown style={{ width: 12, height: 12 }} />}
          </button>
          <button onClick={onRemove}
            style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
            className="p-1.5 rounded hover:border-[var(--danger)] hover:text-[var(--danger)] transition-colors">
            <X style={{ width: 13, height: 13 }} />
          </button>
        </div>
      </div>

      {expanded && (
        <div style={{ borderTop: '1px solid var(--border)', background: 'var(--bg)' }} className="p-4 space-y-3">
          {/* 일반 장비 */}
          <div>
            <p style={{ color: 'var(--text-muted)' }} className="text-xs font-medium mb-2 flex items-center gap-1.5">
              <Sword style={{ width: 12, height: 12 }} /> 일반 장비
            </p>
            <div className="grid grid-cols-9 gap-1">
              {NORMAL_SLOTS.map((slot) => {
                const equipped = slotMap.get(slot);
                return (
                  <div key={slot} className="flex flex-col items-center gap-0.5">
                    <button
                      onClick={() => equipped ? handleUnequip(slot) : setEquipModal(slot)}
                      style={{
                        background: equipped ? 'var(--beige)' : 'var(--card)',
                        border: `1px ${equipped ? 'solid var(--brown)' : 'dashed var(--border)'}`,
                        color: equipped ? 'var(--brown)' : 'var(--text-disabled)',
                        width: '100%',
                        aspectRatio: '1',
                      }}
                      className="rounded text-[9px] flex items-center justify-center hover:border-[var(--brown)] hover:text-[var(--brown)] transition-colors"
                      title={equipped ? `${SLOT_LABEL[slot]}: ${equipped.itemName} (클릭해서 해제)` : `${SLOT_LABEL[slot]} 장착`}
                    >
                      {equipped ? (
                        <span className="truncate px-0.5" style={{ fontSize: 8 }}>
                          {equipped.itemName.slice(0, 4)}
                        </span>
                      ) : '+'}
                    </button>
                    <span style={{ color: 'var(--text-disabled)', fontSize: 9 }}>{SLOT_LABEL[slot]}</span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* 외변 토글 */}
          <button onClick={() => setShowApp(!showApp)}
            style={{ color: 'var(--text-muted)' }}
            className="text-xs flex items-center gap-1 hover:text-[var(--brown)] transition-colors">
            <Shield style={{ width: 12, height: 12 }} />
            외변 장비
            {showApp ? <ChevronUp style={{ width: 10, height: 10 }} /> : <ChevronDown style={{ width: 10, height: 10 }} />}
          </button>

          {showApp && (
            <div className="grid grid-cols-9 gap-1">
              {APP_SLOTS.map((slot) => {
                const equipped = slotMap.get(slot);
                return (
                  <div key={slot} className="flex flex-col items-center gap-0.5">
                    <button
                      onClick={() => equipped ? handleUnequip(slot) : setEquipModal(slot)}
                      style={{
                        background: equipped ? 'var(--beige)' : 'var(--card)',
                        border: `1px ${equipped ? 'solid var(--brown)' : 'dashed var(--border)'}`,
                        color: equipped ? 'var(--brown)' : 'var(--text-disabled)',
                        width: '100%',
                        aspectRatio: '1',
                      }}
                      className="rounded text-[9px] flex items-center justify-center hover:border-[var(--brown)] transition-colors"
                      title={equipped ? `${SLOT_LABEL[slot]}: ${equipped.itemName}` : `${SLOT_LABEL[slot]} 장착`}
                    >
                      {equipped ? (
                        <span style={{ fontSize: 8 }}>{equipped.itemName.slice(0, 4)}</span>
                      ) : '+'}
                    </button>
                    <span style={{ color: 'var(--text-disabled)', fontSize: 9 }}>{SLOT_LABEL[slot]}</span>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {equipModal && (
        <EquipModal
          slot={equipModal}
          mercenaryId={member.mercenaryId}
          isHeroMember={false}
          equipmentBySlot={equipmentBySlot}
          onSelect={(item) => handleEquip(equipModal, item)}
          onClose={() => setEquipModal(null)}
        />
      )}
    </div>
  );
}

// ══════════ 메인 ══════════

export default function DeckPage() {
  const [decks, setDecks] = useState<{ id: number; name: string; isActive: boolean }[]>([]);
  const [selectedDeckId, setSelectedDeckId] = useState<number | null>(null);
  const [deckDetail, setDeckDetail] = useState<DeckDetailDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [newDeckName, setNewDeckName] = useState('');
  const [creating, setCreating] = useState(false);
  const [showAddDeckModal, setShowAddDeckModal] = useState(false);

  // 용병 카테고리 선택 + 몬스터 자동완성
  const [mercenaries, setMercenaries] = useState<MercenaryDto[]>([]);
  const [mercenaryLoading, setMercenaryLoading] = useState(true);
  const [selectedMercenaryCategory, setSelectedMercenaryCategory] = useState<string>('');
  const [miscSearchQuery, setMiscSearchQuery] = useState('');
  const [failedMercenaryImageIds, setFailedMercenaryImageIds] = useState<Set<number>>(new Set());
  const [equipmentMember, setEquipmentMember] = useState<DeckMember | null>(null);
  const [setupInitialTab, setSetupInitialTab] = useState<SetupTab>('equipment');
  const [monsters, setMonsters] = useState<MonsterDto[]>([]);
  const [monsterQuery, setMonsterQuery] = useState('');
  const [showMonsterSuggest, setShowMonsterSuggest] = useState(false);
  const [failedMonsterImageIds, setFailedMonsterImageIds] = useState<Set<number>>(new Set());
  const [effectOptions, setEffectOptions] = useState<DeckEffectCatalogDto | null>(null);
  const [effectSaving, setEffectSaving] = useState(false);
  const [equipmentBySlot, setEquipmentBySlot] = useState<Record<string, EquipmentItemDto[]>>({});
  const [equipmentLoading, setEquipmentLoading] = useState(true);

  // DPS 계산
  const [monster, setMonster] = useState<MonsterDto | null>(null);
  const [dpsResult, setDpsResult] = useState<DpsResultDto | null>(null);
  const [clearTimeSeconds, setClearTimeSeconds] = useState('');
  const [clearTimeSaving, setClearTimeSaving] = useState(false);
  const [calcLoading, setCalcLoading] = useState(false);
  const [dpsRecalcTick, setDpsRecalcTick] = useState(0);
  // DPS 결과에서 추출한 속성값 캐시 — 몬스터 해제 후에도 카드에 표시 유지
  const [elementValueCache, setElementValueCache] = useState<Map<number, number>>(new Map());

  const loadDecks = useCallback(async () => {
    try {
      const list = await api.getDecks() as { id: number; name: string; isActive: boolean }[];
      setDecks(list);
      if (!selectedDeckId && list.length > 0) {
        setSelectedDeckId(list[0].id);
      }
    } catch { /* 미로그인 등 */ }
  }, [selectedDeckId]);

  const loadDeckDetail = useCallback(async (deckId: number) => {
    setLoading(true);
    try {
      const detail = await api.getDeck(deckId) as DeckDetailDto;
      setDeckDetail(detail);
      setEquipmentMember((prev) => {
        if (!prev) return prev;
        const updated = detail.members.find((m) => m.id === prev.id);
        return updated ?? prev;
      });
      setDpsRecalcTick((t) => t + 1);
    } catch {
      setDeckDetail(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      setMercenaryLoading(true);
      setEquipmentLoading(true);

      try {
        const [mercenariesRes, effectsRes, decksRes, equipmentRes] = await Promise.all([
          api.getMercenaries().catch(() => [] as MercenaryDto[]),
          api.getDeckEffectOptions().catch(() => null),
          api.getDecks()
            .then((list) => list as { id: number; name: string; isActive: boolean }[])
            .catch(() => [] as { id: number; name: string; isActive: boolean }[]),
          api.getEquipmentByAllSlots(ALL_EQUIP_SLOTS),
        ]);

        if (cancelled) return;

        setMercenaries(mercenariesRes);
        setEffectOptions(effectsRes);
        setDecks(decksRes);
        setEquipmentBySlot(equipmentRes);

        const defaultDeck = decksRes.find((deck) => deck.isActive) ?? decksRes[0];
        if (defaultDeck) {
          setSelectedDeckId((prev) => prev ?? defaultDeck.id);
        }
      } finally {
        if (!cancelled) {
          setMercenaryLoading(false);
          setEquipmentLoading(false);
        }
      }
    })();

    return () => { cancelled = true; };
  }, []);
  useEffect(() => {
    api.getMonsters().then(setMonsters).catch(() => setMonsters([]));
  }, []);
  useEffect(() => {
    if (selectedDeckId) loadDeckDetail(selectedDeckId);
    else setDeckDetail(null);
  }, [selectedDeckId, loadDeckDetail]);
  useEffect(() => {
    if (monster) setMonsterQuery(monster.name);
  }, [monster]);
  // 덱 멤버 속성값 — 덱 로드 시 서버에서 일괄 조회 (DPS 불필요)
  useEffect(() => {
    if (!selectedDeckId || !deckDetail?.members.length) {
      setElementValueCache(new Map());
      return;
    }
    let cancelled = false;
    api.getDeckMemberElementValues(selectedDeckId)
      .then((entries) => {
        if (cancelled) return;
        setElementValueCache(new Map(entries.map((e) => [e.memberId, e.elementValue])));
      })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [selectedDeckId, dpsRecalcTick]);

  const dpsTriggerSignature = useMemo(() => {
    if (!selectedDeckId || !monster || !deckDetail) return '';
    const effects = deckDetail.effects;
    const spiritIds = (effects?.spirits ?? []).map((s) => s.id).join(',');
    const effectSig = `${spiritIds}|${effects?.jinbeop?.id ?? ''}|${effects?.cheungjin?.id ?? ''}|${effects?.gonmyeongLevel ?? ''}|${effects?.gahoLevel ?? ''}`;
    const membersSig = deckDetail.members
      .map((m) =>
        `${m.id}:${m.level ?? 250}:${m.bonusTarget ?? 'MAIN_STAT'}:${m.bonusAmount ?? 0}:${
          m.slots.map((s) => `${s.slot}:${s.itemId}`).join('|')
        }`
      )
      .join(';');
    return `${selectedDeckId}|${monster.id}|${effectSig}|${membersSig}|${dpsRecalcTick}`;
  }, [selectedDeckId, monster, deckDetail, dpsRecalcTick]);

  useEffect(() => {
    if (!dpsTriggerSignature || !selectedDeckId || !monster || !deckDetail?.members.length) {
      setDpsResult(null);
      return;
    }

    let cancelled = false;
    const timer = window.setTimeout(async () => {
      setCalcLoading(true);
      try {
        const memberInputs = deckDetail.members.map((m) => ({
          memberId: m.id,
          level: m.level === 260 ? 260 : 250,
          bonusTarget: m.bonusTarget ?? 'MAIN_STAT',
          bonusAmount: m.bonusAmount ?? 0,
        }));
        const result = await api.calcDps({
          deckId: selectedDeckId,
          monsterId: monster.id,
          memberInputs,
        });
        if (!cancelled) setDpsResult(result);
      } catch {
        if (!cancelled) setDpsResult(null);
      } finally {
        if (!cancelled) setCalcLoading(false);
      }
    }, DPS_CALC_DEBOUNCE_MS);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [dpsTriggerSignature, selectedDeckId, monster, deckDetail]);

  async function handleSaveClearTime() {
    if (!selectedDeckId || !monster) return;
    if (!getToken()) {
      alert('클리어타임 저장은 로그인 후 이용할 수 있습니다.');
      return;
    }
    const seconds = Number.parseInt(clearTimeSeconds, 10);
    if (!Number.isFinite(seconds) || seconds < 6 || seconds > 26) {
      alert('클리어타임은 6초 이상 26초 이하여야 합니다.');
      return;
    }
    setClearTimeSaving(true);
    try {
      const res = await api.submitClearTime({
        monsterId: monster.id,
        deckId: selectedDeckId,
        clearTimeSeconds: seconds,
      });
      const expMsg = res.expEarned > 0 ? `+${res.expEarned} EXP` : 'EXP 미지급';
      alert(`클리어타임이 저장되었습니다. (${expMsg})`);
    } catch (err) {
      alert(err instanceof Error ? err.message : '클리어타임 저장에 실패했습니다.');
    } finally {
      setClearTimeSaving(false);
    }
  }

  async function handleCreateDeck() {
    const name = newDeckName.trim();
    if (!name) {
      alert('덱 이름을 입력해 주세요.');
      return;
    }
    setCreating(true);
    try {
      const created = await api.createDeck({ name }) as { id: number };
      setNewDeckName('');
      setShowAddDeckModal(false);
      await loadDecks();
      if (created?.id) setSelectedDeckId(created.id);
    } catch {
      alert('덱 생성 중 오류가 발생했습니다.');
    } finally {
      setCreating(false);
    }
  }

  function openAddDeckModal() {
    setNewDeckName('');
    setShowAddDeckModal(true);
  }

  function closeAddDeckModal() {
    if (creating) return;
    setShowAddDeckModal(false);
    setNewDeckName('');
  }

  async function handleDeleteDeck(deckId: number) {
    if (!confirm('이 덱을 삭제하시겠습니까?')) return;
    await api.deleteDeck(deckId).catch(() => {});
    if (selectedDeckId === deckId) setSelectedDeckId(null);
    await loadDecks();
  }

  async function handleRenameDeck(deckId: number, name: string) {
    await api.updateDeck(deckId, { name }).catch(() => {});
    await loadDecks();
  }

  async function handleAddMember(merc: MercenaryDto) {
    if (!selectedDeckId) {
      alert('덱을 먼저 선택해 주세요.');
      return;
    }
    if (selectedMercenaryIds.has(merc.id)) {
      const selectedMember = selectedMembers.find((member) => member.mercenaryId === merc.id);
      if (!selectedMember) return;
      const name = selectedMember.mercenaryName ?? merc.name;
      if (!(await confirmMemberRemoval(selectedDeckId, selectedMember, name))) return;
      await api.removeDeckMember(selectedDeckId, selectedMember.id).catch(() => {});
      await loadDeckDetail(selectedDeckId);
      return;
    }
    const restrictionMessage = getMercenaryRestrictionMessage(merc);
    if (restrictionMessage) {
      alert(restrictionMessage);
      return;
    }
    try {
      await api.addDeckMember(selectedDeckId, { mercenaryId: merc.id });
      await loadDeckDetail(selectedDeckId);
    } catch {
      alert('용병 추가 중 오류가 발생했습니다.');
    }
  }

  async function handleRemoveMember(memberId: number) {
    if (!selectedDeckId || !deckDetail) return;
    const member = deckDetail.members.find((m) => m.id === memberId);
    if (!member) return;
    if (!(await confirmMemberRemoval(selectedDeckId, member, member.mercenaryName))) return;
    await api.removeDeckMember(selectedDeckId, memberId).catch(() => {});
    await loadDeckDetail(selectedDeckId);
  }

  async function handleUpdateDeckEffects(patch: Partial<DeckEffectUpdateBody>) {
    if (!selectedDeckId || !deckDetail) return;
    const current = deckDetail.effects;
    const body: DeckEffectUpdateBody = {
      spirit1Id: current?.spirits?.[0]?.id ?? null,
      spirit2Id: current?.spirits?.[1]?.id ?? null,
      jinbeopSourceId: current?.jinbeop?.id ?? null,
      cheungjinSourceId: current?.cheungjin?.id ?? null,
      gonmyeongLevel: current?.gonmyeongLevel ?? null,
      gahoLevel: current?.gahoLevel ?? null,
      ...patch,
    };

    if (body.spirit1Id && body.spirit2Id && body.spirit1Id === body.spirit2Id) {
      alert('같은 정령은 중복 적용할 수 없습니다.');
      return;
    }

    setEffectSaving(true);
    try {
      const effects = await api.updateDeckEffects(selectedDeckId, body);
      setDeckDetail({ ...deckDetail, effects });
      setDpsRecalcTick((t) => t + 1);
    } catch {
      alert('덱 효과 저장 중 오류가 발생했습니다.');
    } finally {
      setEffectSaving(false);
    }
  }

  const memberCount = deckDetail?.members.length ?? 0;
  const maxMembers = 12;
  const selectedMembers = deckDetail?.members ?? [];
  const mercenaryById = new Map(mercenaries.map((m) => [m.id, m]));
  const selectedMercenaryIds = new Set(selectedMembers.map((m) => m.mercenaryId));
  const selectedMemberMercenaries = selectedMembers.map((m) => mercenaryById.get(m.mercenaryId)).filter(Boolean) as MercenaryDto[];
  const visibleDeckSlots = arrangeDeckDisplaySlots(selectedMembers, mercenaryById, maxMembers);
  const selectableMercenaries = mercenaries.filter(isDeckSelectableMercenary);
  const mercenaryCategories = sortDeckMercenaryCategories(
    Array.from(new Set([...selectableMercenaries.map((m) => displayMercenaryCategory(m)), MISC_CATEGORY]))
  );
  const activeMercenaryCategory = mercenaryCategories.includes(selectedMercenaryCategory)
    ? selectedMercenaryCategory
    : mercenaryCategories[0] || '';
  const filteredMercenaries = selectableMercenaries.filter((m) =>
    activeMercenaryCategory ? displayMercenaryCategory(m) === activeMercenaryCategory : false
  );
  const displayedMercenaries = isLegendaryPickerCategory(activeMercenaryCategory)
    ? sortMercenariesByElement(filteredMercenaries)
    : filteredMercenaries;
  const miscSearchResults = useMemo(() => {
    if (!miscSearchQuery.trim()) return [];
    const q = miscSearchQuery.trim().toLowerCase();
    return mercenaries.filter((m) => !isMiscMercenary(m) && m.name.toLowerCase().includes(q));
  }, [miscSearchQuery, mercenaries]);
  const dpsShareByMemberId = new Map(
    (dpsResult?.memberResults ?? []).map((result) => [result.memberId, result.damageShare])
  );
  const deckEffectSignature = useMemo(() => {
    const effects = deckDetail?.effects;
    if (!effects) return '';
    const spiritIds = (effects.spirits ?? []).map((s) => s.id).join(',');
    return `${spiritIds}|${effects.jinbeop?.id ?? ''}|${effects.cheungjin?.id ?? ''}|${effects.gonmyeongLevel ?? ''}|${effects.gahoLevel ?? ''}`;
  }, [deckDetail?.effects]);

  function getMercenaryRestrictionMessage(merc: MercenaryDto) {
    if (selectedMercenaryIds.has(merc.id)) return '이미 선택된 용병입니다.';

    const heroCount = selectedMemberMercenaries.filter(isHeroMercenary).length;
    const fourKingCount = selectedMemberMercenaries.filter(isFourKingMercenary).length;
    const nonEarthMyeongwangCount = selectedMemberMercenaries.filter(isNonEarthMyeongwangMercenary).length;
    const normalSlotCount = selectedMembers.length - heroCount;

    if (isHeroMercenary(merc)) {
      if (heroCount >= 1) return '주인공은 1명만 선택할 수 있습니다.';
      if (memberCount >= maxMembers) return '덱이 가득 찼습니다.';
      return null;
    }

    if (normalSlotCount >= maxMembers - 1) return '주인공 전용 칸을 제외한 용병 칸이 가득 찼습니다.';
    if (isFourKingMercenary(merc) && fourKingCount >= 1) return '사천왕/각성사천왕은 1명만 선택할 수 있습니다.';
    if (isNonEarthMyeongwangMercenary(merc) && nonEarthMyeongwangCount >= 2) {
      return '부동명왕을 제외한 명왕/각성명왕은 2명까지만 선택할 수 있습니다.';
    }
    const natureConflict = findMyeongwangSameNatureConflict(merc, selectedMemberMercenaries);
    if (natureConflict) {
      const elementLabel = merc.element ? (ELEMENT_LABELS[merc.element] ?? merc.element) : '';
      return `${elementLabel} 계열은 ${natureConflict.name}과(와) 함께 선택할 수 없습니다. (일반/각성 중 택1)`;
    }
    return null;
  }

  function renderMercenaryPickCard(merc: MercenaryDto) {
    const selected = selectedMercenaryIds.has(merc.id);
    const restrictionMessage = getMercenaryRestrictionMessage(merc);
    const blocked = !selected && Boolean(restrictionMessage);
    const variant = myeongwangVariantLabel(merc);
    const natureConflict = findMyeongwangSameNatureConflict(merc, selectedMemberMercenaries);
    const shortBlockReason = natureConflict
      ? `${natureConflict.name} 선택됨`
      : restrictionMessage;

    return (
      <button
        key={merc.id}
        onClick={() => handleAddMember(merc)}
        disabled={blocked}
        title={restrictionMessage ?? undefined}
        style={{
          background: selected ? 'var(--beige)' : 'var(--bg)',
          border: `1px solid ${selected ? 'var(--brown)' : natureConflict ? 'var(--border)' : 'var(--border)'}`,
          overflow: 'hidden',
          opacity: !selected && blocked ? 0.45 : 1,
          cursor: blocked ? 'not-allowed' : 'pointer',
        }}
        className="rounded-lg hover:border-[var(--brown)] transition-all group relative"
      >
        <div className="relative w-full" style={{ aspectRatio: '1 / 1', background: 'var(--border)' }}>
          {merc.imageUrl && !failedMercenaryImageIds.has(merc.id) ? (
            <img
              src={merc.imageUrl}
              alt={merc.name}
              className="w-full h-full object-cover group-hover:scale-105 transition-transform"
              onError={() => {
                setFailedMercenaryImageIds((prev) => {
                  const next = new Set(prev);
                  next.add(merc.id);
                  return next;
                });
              }}
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <span className="font-serif text-2xl font-bold" style={{ color: 'var(--text-muted)' }}>
                {merc.name.charAt(0)}
              </span>
            </div>
          )}
          {merc.element && merc.element !== 'NONE' && (
            <span
              style={{
                background: ELEMENT_COLORS[merc.element] ?? 'var(--brown)',
                color: '#fff',
              }}
              className="absolute top-1.5 right-1.5 text-[10px] font-bold w-5 h-5 rounded-full flex items-center justify-center shadow"
            >
              {ELEMENT_LABELS[merc.element] ?? merc.element}
            </span>
          )}
          {variant && (
            <span
              style={{ background: variant === '각성' ? '#7C3AED' : 'var(--brown)', color: '#fff' }}
              className="absolute top-1.5 left-1.5 text-[10px] px-1.5 py-0.5 rounded font-medium"
            >
              {variant}
            </span>
          )}
          {selected && (
            <span
              style={{ background: 'var(--brown)', color: 'var(--beige)' }}
              className="absolute bottom-1.5 left-1.5 right-1.5 text-[10px] px-1.5 py-0.5 rounded text-center"
            >
              클릭 시 해제
            </span>
          )}
          {!selected && blocked && shortBlockReason && (
            <span
              style={{ background: 'rgba(0,0,0,0.72)', color: '#fff' }}
              className="absolute inset-0 flex items-center justify-center text-[10px] px-2 text-center leading-snug"
            >
              {shortBlockReason}
            </span>
          )}
        </div>
        <div className="px-2 py-1.5">
          <p className="text-xs font-medium truncate text-center" style={{ color: 'var(--text)' }}>{merc.name}</p>
        </div>
      </button>
    );
  }

  const normalizedMonsterQuery = monsterQuery.trim().toLowerCase();
  const monsterSuggestions = monsters
    .filter((m) => !normalizedMonsterQuery || m.name.toLowerCase().includes(normalizedMonsterQuery))
    .slice(0, 8);

  return (
    <div className="py-6">
      {/* 덱 설정 — 가로 탭 */}
      <div
        style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
        className="rounded-xl px-4 py-3 mb-4 flex items-center gap-3"
      >
        <h2 className="font-semibold text-sm shrink-0" style={{ color: 'var(--text)' }}>덱설정</h2>
        <div className="flex items-center gap-2 flex-wrap flex-1 min-w-0">
          {decks.length === 0 ? (
            <span className="text-sm" style={{ color: 'var(--text-disabled)' }}>등록된 덱이 없습니다</span>
          ) : (
            decks.map((deck) => (
              <DeckTab
                key={deck.id}
                deck={deck}
                isSelected={deck.id === selectedDeckId}
                onSelect={() => setSelectedDeckId(deck.id)}
                onDelete={() => handleDeleteDeck(deck.id)}
                onRename={(name) => handleRenameDeck(deck.id, name)}
              />
            ))
          )}
          <button
            type="button"
            onClick={openAddDeckModal}
            style={{ border: '1px dashed var(--border)', color: 'var(--brown)' }}
            className="rounded-lg px-3 py-1.5 text-sm font-medium hover:border-[var(--brown)] hover:bg-[var(--bg)] transition-colors whitespace-nowrap flex items-center gap-1"
          >
            <Plus style={{ width: 14, height: 14 }} />
            덱추가
          </button>
        </div>
        <Link
          href="/value-test"
          className="text-xs shrink-0 px-2 py-1 rounded ml-auto"
          style={{ color: 'var(--brown)', border: '1px solid var(--brown)' }}
        >
          가성비테스트 →
        </Link>
      </div>

      <div className="grid lg:grid-cols-[280px_minmax(0,1fr)_248px] gap-4 xl:gap-6">
        {/* 덱 효과 사이드바 */}
        <div className="space-y-4">
          <DeckEffectPanel
            selectedDeckId={selectedDeckId}
            effects={deckDetail?.effects}
            catalog={effectOptions}
            loading={!effectOptions && !!selectedDeckId}
            saving={effectSaving}
            onSave={handleUpdateDeckEffects}
          />
        </div>

        {/* 덱 멤버 구성 */}
        <div className="min-w-0">
          {selectedDeckId && deckDetail ? (
            <div className="space-y-5">
              {/* 상단 용병 카테고리 */}
              <section style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-4">
                <div className="flex items-center justify-between mb-3">
                  <h2 className="font-semibold" style={{ color: 'var(--text)' }}>용병 선택</h2>
                  <span className="text-xs" style={{ color: 'var(--text-muted)' }}>
                    선택 {memberCount} / {maxMembers}
                  </span>
                </div>
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: `repeat(${mercenaryCategories.length}, minmax(0, 1fr))`,
                    gap: 8,
                  }}
                >
                  {mercenaryCategories.map((category) => {
                    const active = activeMercenaryCategory === category;
                    return (
                      <button
                        key={category}
                        onClick={() => setSelectedMercenaryCategory(category)}
                        style={{
                          background: active ? 'var(--brown)' : 'var(--bg)',
                          color: active ? 'var(--beige)' : 'var(--text-muted)',
                          border: '1px solid var(--border)',
                          padding: '2px 8px',
                          fontSize: 14,
                          lineHeight: 1.25,
                        }}
                        className="rounded-lg font-medium hover:border-[var(--brown)] transition-colors truncate"
                      >
                        {category}
                      </button>
                    );
                  })}
                </div>
              </section>

              {/* 선택한 카테고리의 용병 리스트 */}
              <section style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-4">
                {isMyeongwangPickerCategory(activeMercenaryCategory) && (
                  <div className="flex items-center justify-end mb-3">
                    <MyeongwangRulesHint />
                  </div>
                )}
                {mercenaryLoading ? (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(86px, 1fr))', gap: 10 }}>
                    {[1, 2, 3, 4, 5, 6].map((i) => (
                      <div key={i} style={{ background: 'var(--bg)', height: 124 }} className="rounded-lg animate-pulse" />
                    ))}
                  </div>
                ) : activeMercenaryCategory === MISC_CATEGORY ? (
                  <div className="space-y-4">
                    {filteredMercenaries.length > 0 && (
                      <div>
                        <p className="text-xs font-medium mb-2" style={{ color: 'var(--text-muted)' }}>기본 제공</p>
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(86px, 1fr))', gap: 10 }}>
                          {filteredMercenaries.map((merc) => renderMercenaryPickCard(merc))}
                        </div>
                      </div>
                    )}
                    <div style={{ borderTop: filteredMercenaries.length > 0 ? '1px solid var(--border)' : undefined }} className={filteredMercenaries.length > 0 ? 'pt-4' : undefined}>
                      <p className="text-xs font-medium mb-2" style={{ color: 'var(--text-muted)' }}>용병 검색</p>
                      <input
                        type="text"
                        value={miscSearchQuery}
                        onChange={(e) => setMiscSearchQuery(e.target.value)}
                        placeholder="이름으로 검색..."
                        style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
                        className="w-full rounded-lg px-3 py-2 text-sm outline-none focus:border-[var(--brown)]"
                      />
                    </div>
                    {miscSearchQuery.trim() && (
                      miscSearchResults.length > 0 ? (
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(86px, 1fr))', gap: 10 }}>
                          {miscSearchResults.map((merc) => renderMercenaryPickCard(merc))}
                        </div>
                      ) : (
                        <p className="text-center py-4 text-sm" style={{ color: 'var(--text-disabled)' }}>검색 결과가 없습니다</p>
                      )
                    )}
                  </div>
                ) : displayedMercenaries.length === 0 ? (
                  <p className="text-center py-8 text-sm" style={{ color: 'var(--text-disabled)' }}>해당 카테고리의 용병이 없습니다</p>
                ) : (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(86px, 1fr))', gap: 10 }}>
                    {displayedMercenaries.map((merc) => renderMercenaryPickCard(merc))}
                  </div>
                )}
              </section>

              {/* 12칸 덱 카드 */}
              <section>
                <div className="mb-3">
                  <h2 className="font-semibold" style={{ color: 'var(--text)' }}>
                    {deckDetail.name}
                    <span style={{ color: 'var(--text-muted)' }} className="font-normal text-sm ml-2">
                      ({memberCount} / {maxMembers})
                    </span>
                  </h2>
                </div>

                {loading ? (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))', gap: 10 }}>
                    {Array.from({ length: 12 }).map((_, i) => (
                      <div key={i} style={{ background: 'var(--card)', border: '1px solid var(--border)', minHeight: 170 }} className="rounded-xl animate-pulse" />
                    ))}
                  </div>
                ) : (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))', gap: 10 }}>
                    {visibleDeckSlots.map((member, index) => {
                      const merc = member ? mercenaryById.get(member.mercenaryId) : null;
                      const imageUrl = member?.imageUrl ?? member?.mercenaryImageUrl ?? merc?.imageUrl;
                      const isHeroSlot = index === 0;
                      const damageShare = member
                        ? roundedDamageShare(dpsShareByMemberId.get(member.id))
                        : null;
                      const elementValue = member ? (elementValueCache.get(member.id) ?? null) : null;
                      return (
                        <div
                          key={member?.id ?? `empty-${index}`}
                          style={{
                            background: 'var(--card)',
                            border: `1px ${member ? 'solid var(--border)' : 'dashed var(--border)'}`,
                            minHeight: 170,
                          }}
                          className="rounded-xl overflow-hidden flex flex-col"
                        >
                          <div className="px-1.5 py-1 flex items-center justify-between" style={{ borderBottom: '1px solid var(--border)', minHeight: 22 }}>
                            <span className="text-[10px] font-semibold" style={{ color: damageShare != null ? 'var(--brown)' : 'var(--text-disabled)' }}>
                              {damageShare != null ? `${damageShare.toFixed(1)}%` : ''}
                            </span>
                            {member && (
                              <button
                                onClick={() => handleRemoveMember(member.id)}
                                style={{ color: 'var(--text-muted)' }}
                                className="hover:text-[var(--danger)] transition-colors"
                              >
                                <X style={{ width: 11, height: 11 }} />
                              </button>
                            )}
                          </div>

                          {member ? (
                            <>
                              <div style={{ background: 'var(--border)', aspectRatio: '1 / 1' }} className="relative overflow-hidden">
                                {imageUrl ? (
                                  <img src={imageUrl} alt={member.mercenaryName} className="w-full h-full object-cover" />
                                ) : (
                                  <div className="w-full h-full flex items-center justify-center">
                                    <span className="font-serif text-3xl font-bold" style={{ color: 'var(--text-muted)' }}>
                                      {member.mercenaryName.charAt(0)}
                                    </span>
                                  </div>
                                )}
                              </div>
                              <div className="p-1.5 flex-1 flex flex-col justify-between">
                                <div>
                                  <div className="flex items-center justify-between gap-1 min-w-0">
                                    <p className="font-medium text-[11px] leading-tight truncate text-left flex-1 min-w-0" style={{ color: 'var(--text)' }}>
                                      {member.mercenaryName}
                                    </p>
                                    <span className="text-[10px] leading-tight shrink-0" style={{ color: 'var(--text-muted)' }}>
                                      {mercenaryCategory(merc)}
                                    </span>
                                  </div>
                                  {elementValue != null && (
                                    <p className="text-[10px] leading-tight mt-0.5" style={{ color: 'var(--text-muted)' }}>
                                      속성값 {elementValue.toLocaleString()}
                                    </p>
                                  )}
                                </div>
                                <div className="grid grid-cols-2 gap-1 mt-1.5">
                                  <button
                                    onClick={() => {
                                      setSetupInitialTab('equipment');
                                      setEquipmentMember(member);
                                    }}
                                    style={{ background: 'var(--brown)', color: 'var(--beige)' }}
                                    className="rounded py-1 text-[10px] font-semibold hover:bg-[var(--brown-dark)] transition-colors"
                                  >
                                    장비
                                  </button>
                                  <button
                                    onClick={() => {
                                      setSetupInitialTab('characteristic');
                                      setEquipmentMember(member);
                                    }}
                                    style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
                                    className="rounded py-1 text-[10px] font-semibold hover:border-[var(--brown)] hover:text-[var(--brown)] transition-colors"
                                  >
                                    특성
                                  </button>
                                </div>
                              </div>
                            </>
                          ) : (
                            <div className="flex-1 flex items-center justify-center text-center px-3">
                              <p className="text-xs leading-snug" style={{ color: 'var(--text-disabled)' }}>
                                {isHeroSlot
                                  ? '주인공을 선택하세요'
                                  : index === 1
                                    ? '사천왕/각성사천왕을 선택하세요'
                                    : '용병을 선택하세요'}
                              </p>
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </section>
            </div>
          ) : (
            <div
              style={{ background: 'var(--card)', border: '2px dashed var(--border)' }}
              className="rounded-xl p-16 text-center"
            >
              <p style={{ color: 'var(--text-muted)' }} className="mb-2">덱을 선택하거나 새로 만드세요</p>
              <p style={{ color: 'var(--text-disabled)' }} className="text-sm">상단에서 덱을 추가하거나 선택하세요</p>
            </div>
          )}
        </div>

        <MonsterDpsSidebar
          selectedDeckId={selectedDeckId}
          monster={monster}
          monsterQuery={monsterQuery}
          setMonsterQuery={setMonsterQuery}
          showMonsterSuggest={showMonsterSuggest}
          setShowMonsterSuggest={setShowMonsterSuggest}
          monsterSuggestions={monsterSuggestions}
          onSelectMonster={(m) => {
            setMonster(m);
            setMonsterQuery(m.name);
            setShowMonsterSuggest(false);
          }}
          onClearMonster={() => setMonster(null)}
          failedMonsterImageIds={failedMonsterImageIds}
          setFailedMonsterImageIds={setFailedMonsterImageIds}
          dpsResult={dpsResult}
          calcLoading={calcLoading}
          clearTimeSeconds={clearTimeSeconds}
          onClearTimeSecondsChange={setClearTimeSeconds}
          onSaveClearTime={handleSaveClearTime}
          clearTimeSaving={clearTimeSaving}
        />
      </div>

      <MonsterDpsSidebar
        sticky={false}
        selectedDeckId={selectedDeckId}
        monster={monster}
        monsterQuery={monsterQuery}
        setMonsterQuery={setMonsterQuery}
        showMonsterSuggest={showMonsterSuggest}
        setShowMonsterSuggest={setShowMonsterSuggest}
        monsterSuggestions={monsterSuggestions}
        onSelectMonster={(m) => {
          setMonster(m);
          setMonsterQuery(m.name);
          setShowMonsterSuggest(false);
        }}
        onClearMonster={() => setMonster(null)}
        failedMonsterImageIds={failedMonsterImageIds}
        setFailedMonsterImageIds={setFailedMonsterImageIds}
        dpsResult={dpsResult}
        calcLoading={calcLoading}
        clearTimeSeconds={clearTimeSeconds}
        onClearTimeSecondsChange={setClearTimeSeconds}
        onSaveClearTime={handleSaveClearTime}
        clearTimeSaving={clearTimeSaving}
      />

      {showAddDeckModal && (
        <AddDeckModal
          name={newDeckName}
          creating={creating}
          onChange={setNewDeckName}
          onConfirm={handleCreateDeck}
          onClose={closeAddDeckModal}
        />
      )}

      {equipmentMember && selectedDeckId && (
        <EquipmentSetupModal
          member={equipmentMember}
          isHeroMember={isHeroMercenary(mercenaryById.get(equipmentMember.mercenaryId))}
          deckId={selectedDeckId}
          deckEffectSignature={deckEffectSignature}
          equipmentBySlot={equipmentBySlot}
          initialTab={setupInitialTab}
          onClose={() => setEquipmentMember(null)}
          onRefresh={() => loadDeckDetail(selectedDeckId)}
        />
      )}
    </div>
  );
}
