'use client';

import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { createPortal } from 'react-dom';
import {
  api,
  type MercenaryDto,
  type MonsterDto,
  type DeckDetailDto,
  type EquipmentItemDto,
  type DpsResultDto,
  type ItemSearchResult,
  type MemberCharacteristicDto,
  type DeckEffectCatalogDto,
  type DeckEffectUpdateBody,
} from '@/lib/api';
import { Plus, Trash2, Sword, Shield, Search, X, ChevronDown, ChevronUp, Zap } from 'lucide-react';

// ══════════ 상수 ══════════

const NORMAL_SLOTS = ['CHARM', 'HELMET', 'GLOVES', 'ARMOR', 'BELT', 'WEAPON', 'SHOES', 'RING_1', 'RING_2'];
const APP_SLOTS = ['APP_SPIRIT', 'APP_EARRING', 'APP_HELMET', 'APP_NECKLACE', 'APP_ARMOR', 'APP_GREAVES', 'APP_WEAPON', 'APP_BRACELET'];
const NORMAL_SLOT_ROWS = [
  ['CHARM', null],
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

const STAT_LABEL: Record<string, string> = {
  STRENGTH: '힘',
  DEXTERITY: '민첩',
  VITALITY: '생명',
  INTELLECT: '지력',
  RESIST_PIERCE: '저항깎',
  ELEMENT_VALUE: '속성값',
  ELEMENT_PIERCE: '속성깎',
  FIRE_RESIST: '화저항',
  WATER_RESIST: '수저항',
  THUNDER_RESIST: '뇌저항',
  WIND_RESIST: '풍저항',
  EARTH_RESIST: '토저항',
  HITTING_RESISTANCE: '타격저항',
  MAGIC_RESISTANCE: '마법저항',
  ATTACK_SPEED: '공격속도',
  MOVE_SPEED: '이동속도',
  HP_RECOVERY: '체력회복',
  MP_RECOVERY: '마력회복',
  MIN_POWER: '최소공격력',
  MAX_POWER: '최대공격력',
  DAMAGE_PERCENT: '데미지증가',
  SKILL_DAMAGE_PERCENT: '스킬데미지증가',
  ALL_STAT: '모든능력치',
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

function isLegendaryMercenary(merc?: Pick<MercenaryDto, 'category'> | null) {
  const category = mercenaryCategory(merc);
  return category.includes('전설장수') || category === 'LEGENDARY_GENERAL';
}

function isDeckSelectableMercenary(merc: MercenaryDto) {
  return isHeroMercenary(merc)
    || isFourKingMercenary(merc)
    || isMyeongwangMercenary(merc)
    || isLegendaryMercenary(merc);
}

function roundedDamageShare(value?: number) {
  return value != null ? Math.round(value * 10) / 10 : null;
}

function formatStatType(statType: string) {
  return STAT_LABEL[statType] ?? statType;
}

function statValue(
  stats: MemberStats | null,
  section: 'baseStats' | 'equipStats' | 'setEffectStats' | 'characteristicStats' | 'partyCharacteristicStats' | 'enemyDebuffStats' | 'ritualStats' | 'deckBuffStats' | 'levelBonusStats' | 'bonusStats' | 'myungwangTransferStats' | 'totalStats',
  statType: string
) {
  return stats?.[section]?.find((s) => s.statType === statType)?.value ?? 0;
}

function displayStatTypes(stats: MemberStats | null) {
  const keys = new Set<string>();
  stats?.baseStats?.forEach((s) => keys.add(s.statType));
  stats?.equipStats?.forEach((s) => keys.add(s.statType));
  stats?.setEffectStats?.forEach((s) => keys.add(s.statType));
  stats?.characteristicStats?.forEach((s) => keys.add(s.statType));
  stats?.partyCharacteristicStats?.forEach((s) => keys.add(s.statType));
  stats?.enemyDebuffStats?.forEach((s) => keys.add(s.statType));
  stats?.ritualStats?.forEach((s) => keys.add(s.statType));
  stats?.deckBuffStats?.forEach((s) => keys.add(s.statType));
  stats?.levelBonusStats?.forEach((s) => keys.add(s.statType));
  stats?.bonusStats?.forEach((s) => keys.add(s.statType));
  stats?.myungwangTransferStats?.forEach((s) => keys.add(s.statType));
  stats?.totalStats?.forEach((s) => keys.add(s.statType));
  return Array.from(keys);
}

function equipmentItemId(item: EquipmentItemDto) {
  return item.itemId ?? item.id ?? 0;
}

function equipmentItemToSearchResult(item: EquipmentItemDto, selectedSlot: string): ItemSearchResult {
  return {
    id: equipmentItemId(item),
    name: item.name,
    type: 'EQUIPMENT',
    equipmentKind: item.equipmentKind,
    slot: item.slot,
    setId: item.setId,
    setName: item.setName,
    imageUrl: item.imageUrl,
    equipSlot: selectedSlot,
  };
}

function characteristicUsedPoints(characteristics: MemberCharacteristicDto | null) {
  return characteristics?.characteristics.reduce((sum, characteristic) => {
    if (!characteristic.selectedLevel) return sum;
    return sum + ((characteristic.point ?? 0) * characteristic.selectedLevel);
  }, 0) ?? 0;
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

function formatEquipAttackPower(stats: MemberStats | null) {
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

function buildStatContributions(stats: MemberStats | null, statType: string): StatContribution[] {
  if (!stats) return [];
  const rows: StatContribution[] = [
    { label: '기본', value: statValue(stats, 'baseStats', statType) },
    { label: '장비', value: statValue(stats, 'equipStats', statType) },
    { label: '특성(자신)', value: statValue(stats, 'characteristicStats', statType) },
    { label: '특성(아군)', value: statValue(stats, 'partyCharacteristicStats', statType) },
    { label: '특성(적군)', value: statValue(stats, 'enemyDebuffStats', statType) },
    { label: '주술', value: statValue(stats, 'ritualStats', statType) },
    { label: '덱 효과', value: statValue(stats, 'deckBuffStats', statType) },
    { label: '레벨 스탯', value: statValue(stats, 'levelBonusStats', statType) },
    { label: '보너스 스탯', value: statValue(stats, 'bonusStats', statType) },
  ];
  stats.myungwangTransferDetails?.forEach((detail) => {
    if (detail.statType === statType && detail.value !== 0) {
      rows.push({
        label: `명왕 스탯 이전 (${detail.sourceMercenaryName})`,
        value: detail.value,
      });
    }
  });
  return rows.filter((r) => r.value !== 0);
}

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
                  <span style={{ color: 'var(--text)' }}>{effect.ritualName} / {effect.setName}</span>
                  <span style={{ color: 'var(--brown)' }}>{effect.appliedPieces}/{effect.requiredPieces}</span>
                </div>
                <p className={compact ? 'text-[9px] mt-0.5' : 'text-[10px] mt-0.5'} style={{ color: 'var(--text-muted)' }}>
                  {effect.outcome} · {formatStatType(effect.statType)} +{effect.statValue}
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
          const totalValue = equipValue + setValue;
          if (totalValue === 0) return null;
          return (
          <div
            key={statType}
            className={`grid grid-cols-2 ${compact ? 'px-2 py-1' : 'px-3 py-2'}`}
            style={{ borderTop: '1px solid var(--border)', color: 'var(--text)' }}
          >
            <span className={compact ? 'text-[11px] truncate' : ''}>{formatStatType(statType)}</span>
            <span className={`text-right font-medium ${compact ? 'text-[11px]' : ''}`}>
              {setValue > 0 ? `${equipValue}+${setValue}` : equipValue}
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
        {characteristic.point != null && (
          <span className="text-[10px] shrink-0 px-1.5 py-0.5 rounded" style={{ background: 'var(--bg)', color: 'var(--brown)' }}>
            {characteristic.point}pt/lv
          </span>
        )}
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
          preview.map((entry) => (
            <p key={`${entry.label}-${entry.level}`}>
              {entry.label ? `${entry.label} ` : ''}{entry.amount}
            </p>
          ))
        )}
      </div>
    </div>
  );
}

function StatContributionPopover({
  statType,
  contributions,
  total,
}: {
  statType: string;
  contributions: StatContribution[];
  total: number;
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
      {popover}
    </>
  );
}

// ══════════ 덱 목록 카드 ══════════

function DeckCard({ deck, isSelected, onSelect, onDelete, onRename }: {
  deck: { id: number; name: string; isActive: boolean };
  isSelected: boolean;
  onSelect: () => void;
  onDelete: () => void;
  onRename: (name: string) => void;
}) {
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(deck.name);

  function submit() {
    setEditing(false);
    if (name.trim() && name !== deck.name) onRename(name.trim());
  }

  return (
    <div
      onClick={onSelect}
      style={{
        background: isSelected ? 'var(--beige)' : 'var(--card)',
        border: `1px solid ${isSelected ? 'var(--brown)' : 'var(--border)'}`,
        cursor: 'pointer',
      }}
      className="rounded-lg px-4 py-3 flex items-center justify-between hover:border-[var(--brown)] transition-all"
    >
      {editing ? (
        <input
          autoFocus
          value={name}
          onChange={(e) => setName(e.target.value)}
          onBlur={submit}
          onKeyDown={(e) => e.key === 'Enter' && submit()}
          onClick={(e) => e.stopPropagation()}
          style={{ background: 'var(--bg)', border: '1px solid var(--brown)', color: 'var(--text)' }}
          className="flex-1 rounded px-2 py-0.5 text-sm focus:outline-none mr-2"
        />
      ) : (
        <span className="font-medium text-sm" style={{ color: 'var(--text)' }}>
          {deck.name}
          {deck.isActive && (
            <span style={{ background: 'var(--brown)', color: 'var(--beige)' }} className="text-[10px] ml-2 px-1.5 py-0.5 rounded">활성</span>
          )}
        </span>
      )}
      <div className="flex gap-1 ml-2" onClick={(e) => e.stopPropagation()}>
        <button onClick={() => setEditing(true)} style={{ color: 'var(--text-muted)' }} className="p-1 hover:text-[var(--brown)] transition-colors text-xs">✏️</button>
        <button onClick={onDelete} style={{ color: 'var(--text-muted)' }} className="p-1 hover:text-[var(--danger)] transition-colors">
          <Trash2 style={{ width: 13, height: 13 }} />
        </button>
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

// ══════════ 장비 선택 모달 ══════════

function EquipModal({ slot, onSelect, onClose }: {
  slot: string;
  onSelect: (item: EquipmentItemDto) => void;
  onClose: () => void;
}) {
  const [items, setItems] = useState<EquipmentItemDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');

  useEffect(() => {
    api.getEquipmentBySlot(slot)
      .then((res) => setItems(res as EquipmentItemDto[]))
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
  }, [slot]);

  const filtered = items.filter((i) => !query || i.name.includes(query));

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
          <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="장비명 검색..."
            style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
            className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]" />
        </div>

        <div className="overflow-y-auto flex-1 p-3 space-y-1">
          {loading ? (
            <div className="space-y-2">
              {[1, 2, 3].map((i) => (
                <div key={i} style={{ background: 'var(--bg)' }} className="h-12 rounded animate-pulse" />
              ))}
            </div>
          ) : filtered.length === 0 ? (
            <p style={{ color: 'var(--text-disabled)' }} className="text-center py-6 text-sm">해당 슬롯의 장비가 없습니다</p>
          ) : (
            filtered.map((item) => (
              <button key={equipmentItemId(item)} onClick={() => onSelect(item)}
                style={{ background: 'var(--bg)', border: '1px solid var(--border)', textAlign: 'left', width: '100%' }}
                className="rounded-lg px-4 py-2.5 hover:border-[var(--brown)] transition-colors">
                <p className="text-sm font-medium" style={{ color: 'var(--text)' }}>{item.name}</p>
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

function EquipmentSetupModal({ member, deckId, deckEffectSignature, initialTab = 'equipment', onClose, onRefresh }: {
  member: DeckMember;
  deckId: number;
  deckEffectSignature?: string;
  initialTab?: SetupTab;
  onClose: () => void;
  onRefresh: () => void | Promise<void>;
}) {
  const [activeTab, setActiveTab] = useState<SetupTab>(initialTab);
  const [selectedSlot, setSelectedSlot] = useState(NORMAL_SLOTS[0]);
  const [showAppSlots, setShowAppSlots] = useState(false);
  const [slotItems, setSlotItems] = useState<ItemSearchResult[]>([]);
  const [loading, setLoading] = useState(false);
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
  const slotMap = useMemo(() => new Map(equippedSlots.map((s) => [s.slot, s])), [equippedSlots]);
  const selectedEquipped = slotMap.get(selectedSlot);
  const slotRows = showAppSlots ? APP_SLOT_ROWS : NORMAL_SLOT_ROWS;
  const statTypes = displayStatTypes(stats);
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
  const displayedItems = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return slotItems
      .filter((item) => !keyword || item.name.toLowerCase().includes(keyword))
      .sort((a, b) => b.id - a.id);
  }, [slotItems, query]);

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
    setLoading(true);
    api.getEquipmentBySlot(selectedSlot)
      .then((items) => setSlotItems(items.map((item) => equipmentItemToSearchResult(item, selectedSlot))))
      .catch(() => setSlotItems([]))
      .finally(() => setLoading(false));
  }, [selectedSlot]);

  useEffect(() => {
    loadStats();
    loadCharacteristics();
  }, [loadStats, loadCharacteristics, deckEffectSignature]);

  async function maybeApplyRitual(slot: string, item: ItemSearchResult) {
    const rituals = await api.getItemRituals(item.id).catch(() => []) as { id: number; displayName?: string; successMark?: string; greatSuccessMark?: string }[];
    if (rituals.length === 0) return;
    if (!confirm('이 아이템은 주술 가능한 아이템입니다. 주술을 적용하시겠습니까?')) return;

    const menu = rituals
      .map((r, index) => `${index + 1}. ${r.displayName ?? `주술 ${r.id}`} (${r.successMark ?? '-'} / ${r.greatSuccessMark ?? '-'})`)
      .join('\n');
    const selected = Number(prompt(`적용할 주술 번호를 선택하세요.\n${menu}`));
    const ritual = rituals[selected - 1];
    if (!ritual) return;
    const outcomeInput = prompt('주술 결과를 선택하세요. 성공=1, 대성공=2', '1');
    const outcome = outcomeInput === '2' ? 'GREAT_SUCCESS' : 'SUCCESS';
    await api.setSlotRitual(deckId, member.id, slot, { ritualId: ritual.id, outcome });
  }

  async function equipPiece(item: ItemSearchResult) {
    const slot = selectedSlot;
    try {
      await api.equipSlot(deckId, member.id, slot, { itemId: item.id });
      await maybeApplyRitual(slot, item);
      await onRefresh();
      await loadStats();
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
      await onRefresh();
      await loadStats();
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
      loadStats();
      loadCharacteristics();
      await onRefresh();
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
        characteristic.point ?? 0,
      ])
    );
    const nextUsedPoints = nextEntries.reduce((sum, entry) => (
      sum + ((pointByCharacteristicId.get(entry.characteristicId) ?? 0) * entry.selectedLevel)
    ), 0);
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
      loadStats();
      loadCharacteristics();
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
    try {
      await api.unequipSlot(deckId, member.id, selectedSlot);
      await onRefresh();
      loadStats();
    } catch {
      alert('장비 해제 중 오류가 발생했습니다.');
    }
  }

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
                    onClick={() => { setShowAppSlots(false); setSelectedSlot(NORMAL_SLOTS[0]); }}
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
                          </button>
                        );
                      })}
                    </div>
                  ))}
                </div>
                </div>
              </div>

              <div className="p-3 overflow-y-auto min-w-0">
                <div className="flex items-center justify-between mb-3 gap-2">
                  <div>
                    <p className="text-sm font-semibold" style={{ color: 'var(--text)' }}>아이템 검색</p>
                    <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
                      {SLOT_LABEL[selectedSlot] ?? selectedSlot} 슬롯 장비만 item_id 내림차순으로 표시합니다
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

                <input
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder={`${SLOT_LABEL[selectedSlot] ?? selectedSlot} 장비명 검색...`}
                  style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
                  className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)] mb-3"
                />

                {loading ? (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(110px, 1fr))', gap: 10 }}>
                    {[1, 2, 3, 4, 5, 6].map((i) => <div key={i} style={{ background: 'var(--bg)', height: 145 }} className="rounded animate-pulse" />)}
                  </div>
                ) : displayedItems.length === 0 ? (
                  <p className="text-center py-8 text-sm" style={{ color: 'var(--text-disabled)' }}>
                    {query.trim() ? '검색 결과가 없습니다' : '해당 슬롯의 장비가 없습니다'}
                  </p>
                ) : (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(110px, 1fr))', gap: 10 }}>
                    {displayedItems.map((item) => (
                      <div
                        key={item.id}
                        style={{ background: 'var(--bg)', border: '1px solid var(--border)', overflow: 'hidden' }}
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
                          {item.setName && (
                            <p className="text-[10px] truncate text-center" style={{ color: 'var(--text-muted)' }}>{item.setName}</p>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
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
              <div style={{ borderRight: '1px solid var(--border)', background: 'var(--bg)' }} className="p-5 overflow-y-auto">
                <div
                  style={{ border: '1px solid var(--border)', background: 'var(--card)' }}
                  className="rounded-lg p-4 mb-4"
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

                <p className="text-sm font-semibold mb-4" style={{ color: 'var(--text)' }}>최종 스탯</p>
                {statsLoading ? (
                  <div style={{ background: 'var(--card)' }} className="h-48 rounded animate-pulse" />
                ) : statTypes.length === 0 ? (
                  <p className="text-xs" style={{ color: 'var(--text-disabled)' }}>표시할 스탯이 없습니다</p>
                ) : (
                  <div style={{ border: '1px solid var(--border)' }} className="rounded-lg overflow-hidden text-sm">
                    {statTypes.map((statType) => (
                      <div
                        key={statType}
                        className="flex items-center justify-between px-4 py-2.5"
                        style={{ borderTop: '1px solid var(--border)', color: 'var(--text)' }}
                      >
                        <span style={{ color: 'var(--text-muted)' }}>{formatStatType(statType)}</span>
                        <span className="font-semibold" style={{ color: 'var(--brown)' }}>
                          {statValue(stats, 'totalStats', statType)}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="p-5 overflow-y-auto">
                <p className="text-sm font-semibold mb-1" style={{ color: 'var(--text)' }}>스탯 구성</p>
                <p className="text-xs mb-4" style={{ color: 'var(--text-muted)' }}>
                  각 스탯의 기여 내역 버튼을 누르면 영향을 준 항목과 수치를 확인할 수 있습니다.
                </p>
                {statsLoading ? (
                  <div style={{ background: 'var(--card)' }} className="h-48 rounded animate-pulse" />
                ) : statTypes.length === 0 ? (
                  <p className="text-xs" style={{ color: 'var(--text-disabled)' }}>표시할 스탯이 없습니다</p>
                ) : (
                  <div style={{ border: '1px solid var(--border)' }} className="rounded-lg overflow-hidden text-xs">
                    <div className="grid grid-cols-[1fr_auto_auto] gap-2 px-3 py-2 font-semibold" style={{ background: 'var(--bg)', color: 'var(--text-muted)' }}>
                      <span>스탯</span>
                      <span className="text-right w-16">값</span>
                      <span className="w-[72px]" />
                    </div>
                    {statTypes.map((statType) => {
                      const total = statValue(stats, 'totalStats', statType);
                      const contributions = buildStatContributions(stats, statType);
                      return (
                        <div
                          key={statType}
                          className="grid grid-cols-[1fr_auto_auto] gap-2 items-center px-3 py-2"
                          style={{ borderTop: '1px solid var(--border)' }}
                        >
                          <span style={{ color: 'var(--text)' }}>{formatStatType(statType)}</span>
                          <span className="text-right w-16 font-semibold" style={{ color: 'var(--brown)' }}>{total}</span>
                          <div className="w-[72px] flex justify-end">
                            <StatContributionPopover
                              statType={statType}
                              contributions={contributions}
                              total={total}
                            />
                          </div>
                        </div>
                      );
                    })}
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

function MemberCard({ member, deckId, onRemove, onRefresh }: {
  member: DeckMember;
  deckId: number;
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

  // 용병 카테고리 선택 + 몬스터 자동완성
  const [mercenaries, setMercenaries] = useState<MercenaryDto[]>([]);
  const [mercenaryLoading, setMercenaryLoading] = useState(true);
  const [selectedMercenaryCategory, setSelectedMercenaryCategory] = useState<string>('');
  const [failedMercenaryImageIds, setFailedMercenaryImageIds] = useState<Set<number>>(new Set());
  const [equipmentMember, setEquipmentMember] = useState<DeckMember | null>(null);
  const [setupInitialTab, setSetupInitialTab] = useState<SetupTab>('equipment');
  const [monsters, setMonsters] = useState<MonsterDto[]>([]);
  const [monsterQuery, setMonsterQuery] = useState('');
  const [showMonsterSuggest, setShowMonsterSuggest] = useState(false);
  const [failedMonsterImageIds, setFailedMonsterImageIds] = useState<Set<number>>(new Set());
  const [effectOptions, setEffectOptions] = useState<DeckEffectCatalogDto | null>(null);
  const [effectSaving, setEffectSaving] = useState(false);

  // DPS 계산
  const [monster, setMonster] = useState<MonsterDto | null>(null);
  const [dpsResult, setDpsResult] = useState<DpsResultDto | null>(null);
  const [calcLoading, setCalcLoading] = useState(false);

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
    } catch {
      setDeckDetail(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadDecks(); }, [loadDecks]);
  useEffect(() => {
    api.getMercenaries()
      .then(setMercenaries)
      .catch(() => setMercenaries([]))
      .finally(() => setMercenaryLoading(false));
  }, []);
  useEffect(() => {
    api.getMonsters().then(setMonsters).catch(() => setMonsters([]));
  }, []);
  useEffect(() => {
    api.getDeckEffectOptions().then(setEffectOptions).catch(() => setEffectOptions(null));
  }, []);
  useEffect(() => {
    if (selectedDeckId) loadDeckDetail(selectedDeckId);
    else setDeckDetail(null);
  }, [selectedDeckId, loadDeckDetail]);
  useEffect(() => {
    if (monster) setMonsterQuery(monster.name);
  }, [monster]);

  async function handleCreateDeck() {
    const name = newDeckName.trim() || `덱 ${decks.length + 1}`;
    setCreating(true);
    try {
      await api.createDeck({ name });
      setNewDeckName('');
      await loadDecks();
    } catch {
      alert('덱 생성 중 오류가 발생했습니다.');
    } finally {
      setCreating(false);
    }
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
    if (!selectedDeckId) return;
    if (!confirm('이 용병을 덱에서 제거하시겠습니까?')) return;
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
    } catch {
      alert('덱 효과 저장 중 오류가 발생했습니다.');
    } finally {
      setEffectSaving(false);
    }
  }

  async function handleCalcDps() {
    if (!selectedDeckId || !monster) return;
    setCalcLoading(true);
    try {
      const memberInputs = (deckDetail?.members ?? []).map((m) => ({
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
      setDpsResult(result);
    } catch {
      alert('DPS 계산 중 오류가 발생했습니다.');
    } finally {
      setCalcLoading(false);
    }
  }

  const memberCount = deckDetail?.members.length ?? 0;
  const maxMembers = 12;
  const selectedMembers = deckDetail?.members ?? [];
  const mercenaryById = new Map(mercenaries.map((m) => [m.id, m]));
  const selectedMercenaryIds = new Set(selectedMembers.map((m) => m.mercenaryId));
  const selectedMemberMercenaries = selectedMembers.map((m) => mercenaryById.get(m.mercenaryId)).filter(Boolean) as MercenaryDto[];
  const heroMember = selectedMembers.find((m) => isHeroMercenary(mercenaryById.get(m.mercenaryId))) ?? null;
  const normalMembers = selectedMembers.filter((m) => m.id !== heroMember?.id);
  const deckSlots: (DeckMember | null)[] = [heroMember, ...normalMembers];
  while (deckSlots.length < maxMembers) deckSlots.push(null);
  const visibleDeckSlots = deckSlots.slice(0, maxMembers);
  const selectableMercenaries = mercenaries.filter(isDeckSelectableMercenary);
  const mercenaryCategories = Array.from(new Set(selectableMercenaries.map((m) => mercenaryCategory(m))));
  const activeMercenaryCategory = mercenaryCategories.includes(selectedMercenaryCategory)
    ? selectedMercenaryCategory
    : mercenaryCategories[0] || '';
  const displayedMercenaries = selectableMercenaries.filter((m) =>
    activeMercenaryCategory ? mercenaryCategory(m) === activeMercenaryCategory : false
  );
  const dpsShareByMemberId = new Map(
    (dpsResult?.memberResults ?? []).map((result) => [result.memberId, result.damageShare])
  );
  const elementValueByMemberId = new Map(
    (dpsResult?.memberResults ?? []).map((result) => [result.memberId, result.elementValue])
  );
  const deckEffectSignature = useMemo(() => {
    const effects = deckDetail?.effects;
    if (!effects) return '';
    const spiritIds = (effects.spirits ?? []).map((s) => s.id).join(',');
    return `${spiritIds}|${effects.jinbeop?.id ?? ''}|${effects.cheungjin?.id ?? ''}`;
  }, [deckDetail?.effects]);

  function selectedCountByCategory(category: string) {
    return selectedMemberMercenaries.filter((m) => mercenaryCategory(m) === category).length;
  }

  function getMercenaryRestrictionMessage(merc: MercenaryDto) {
    if (selectedMercenaryIds.has(merc.id)) return '이미 선택된 용병입니다.';

    const heroCount = selectedMemberMercenaries.filter(isHeroMercenary).length;
    const fourKingCount = selectedMemberMercenaries.filter(isFourKingMercenary).length;
    const myeongwangCount = selectedMemberMercenaries.filter(isMyeongwangMercenary).length;
    const normalSlotCount = selectedMembers.length - heroCount;

    if (isHeroMercenary(merc)) {
      if (heroCount >= 1) return '주인공은 1명만 선택할 수 있습니다.';
      if (memberCount >= maxMembers) return '덱이 가득 찼습니다.';
      return null;
    }

    if (normalSlotCount >= maxMembers - 1) return '주인공 전용 칸을 제외한 용병 칸이 가득 찼습니다.';
    if (isFourKingMercenary(merc) && fourKingCount >= 1) return '사천왕/각성사천왕은 1명만 선택할 수 있습니다.';
    if (isMyeongwangMercenary(merc) && myeongwangCount >= 2) return '명왕/각성명왕은 2명까지만 선택할 수 있습니다.';
    return null;
  }

  const normalizedMonsterQuery = monsterQuery.trim().toLowerCase();
  const monsterSuggestions = monsters
    .filter((m) => !normalizedMonsterQuery || m.name.toLowerCase().includes(normalizedMonsterQuery))
    .slice(0, 8);

  return (
    <div className="py-6">
      <div className="grid lg:grid-cols-[280px_minmax(0,1fr)] gap-6">
        {/* 덱 목록 사이드바 */}
        <div className="space-y-4">
          <div style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-4">
            <h2 className="font-semibold mb-3" style={{ color: 'var(--text)' }}>내 덱</h2>

            {/* 덱 생성 */}
            <div className="flex gap-2 mb-3">
              <input
                value={newDeckName}
                onChange={(e) => setNewDeckName(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleCreateDeck()}
                placeholder="새 덱 이름"
                style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
                className="flex-1 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-[var(--brown)]"
              />
              <button
                onClick={handleCreateDeck}
                disabled={creating}
                style={{ background: 'var(--brown)', color: 'var(--beige)' }}
                className="p-1.5 rounded hover:bg-[var(--brown-dark)] transition-colors disabled:opacity-50"
              >
                <Plus style={{ width: 16, height: 16 }} />
              </button>
            </div>

            {/* 덱 목록 */}
            <div className="space-y-2">
              {decks.length === 0 ? (
                <p style={{ color: 'var(--text-disabled)' }} className="text-sm text-center py-4">
                  로그인 후 덱을 만들어보세요
                </p>
              ) : (
                decks.map((deck) => (
                  <DeckCard
                    key={deck.id}
                    deck={deck}
                    isSelected={deck.id === selectedDeckId}
                    onSelect={() => setSelectedDeckId(deck.id)}
                    onDelete={() => handleDeleteDeck(deck.id)}
                    onRename={(name) => handleRenameDeck(deck.id, name)}
                  />
                ))
              )}
            </div>
          </div>

          {/* 덱 효과 */}
          <div style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-4">
            <h2 className="font-semibold mb-3" style={{ color: 'var(--text)' }}>덱 효과</h2>
            {!selectedDeckId ? (
              <p className="text-sm text-center py-4" style={{ color: 'var(--text-disabled)' }}>덱을 먼저 선택해 주세요</p>
            ) : !effectOptions ? (
              <div className="space-y-2">
                {[1, 2, 3].map((i) => (
                  <div key={i} style={{ background: 'var(--bg)' }} className="h-9 rounded animate-pulse" />
                ))}
              </div>
            ) : (
              <div className="space-y-3">
                <div className="grid grid-cols-2 gap-2">
                  {[0, 1].map((index) => (
                    <div key={index}>
                      <label className="text-xs block mb-1" style={{ color: 'var(--text-muted)' }}>정령 {index + 1}</label>
                      <select
                        value={deckDetail?.effects?.spirits?.[index]?.id ?? ''}
                        disabled={effectSaving}
                        onChange={(e) => handleUpdateDeckEffects({
                          [index === 0 ? 'spirit1Id' : 'spirit2Id']: e.target.value ? Number(e.target.value) : null,
                        })}
                        style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
                        className="w-full rounded px-2 py-1.5 text-xs focus:outline-none focus:border-[var(--brown)] disabled:opacity-60"
                      >
                        <option value="">미적용</option>
                        {effectOptions.spirits.map((spirit) => (
                          <option key={spirit.id} value={spirit.id}>{spirit.displayLabel}</option>
                        ))}
                      </select>
                    </div>
                  ))}
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="text-xs block mb-1" style={{ color: 'var(--text-muted)' }}>진법</label>
                    <select
                      value={deckDetail?.effects?.jinbeop?.id ?? ''}
                      disabled={effectSaving}
                      onChange={(e) => handleUpdateDeckEffects({ jinbeopSourceId: e.target.value ? Number(e.target.value) : null })}
                      style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
                      className="w-full rounded px-2 py-1.5 text-xs focus:outline-none focus:border-[var(--brown)] disabled:opacity-60"
                    >
                      <option value="">미적용</option>
                      {effectOptions.jinbeops.map((source) => (
                        <option key={source.id} value={source.id}>{source.name}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="text-xs block mb-1" style={{ color: 'var(--text-muted)' }}>층진</label>
                    <select
                      value={deckDetail?.effects?.cheungjin?.id ?? ''}
                      disabled={effectSaving}
                      onChange={(e) => handleUpdateDeckEffects({ cheungjinSourceId: e.target.value ? Number(e.target.value) : null })}
                      style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
                      className="w-full rounded px-2 py-1.5 text-xs focus:outline-none focus:border-[var(--brown)] disabled:opacity-60"
                    >
                      <option value="">미적용</option>
                      {effectOptions.cheungjins.map((source) => (
                        <option key={source.id} value={source.id}>{source.name}</option>
                      ))}
                    </select>
                  </div>
                </div>

                {deckDetail?.effects?.stats && deckDetail.effects.stats.length > 0 ? (
                  <div style={{ background: 'var(--bg)', border: '1px solid var(--border)' }} className="rounded-lg p-2">
                    <p className="text-xs font-medium mb-1" style={{ color: 'var(--text)' }}>적용 스탯</p>
                    <div className="flex flex-wrap gap-1">
                      {deckDetail.effects.stats.map((stat) => (
                        <span
                          key={stat.statType}
                          style={{ background: 'var(--card)', color: 'var(--text-muted)', border: '1px solid var(--border)' }}
                          className="rounded px-1.5 py-0.5 text-[10px]"
                        >
                          {formatStatType(stat.statType)} {stat.value > 0 ? '+' : ''}{stat.value}
                        </span>
                      ))}
                    </div>
                  </div>
                ) : (
                  <p className="text-xs" style={{ color: 'var(--text-disabled)' }}>적용된 덱 효과가 없습니다</p>
                )}
              </div>
            )}
          </div>

          {/* 대상 몬스터 */}
          <div style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-4">
            <h2 className="font-semibold mb-3" style={{ color: 'var(--text)' }}>대상 몬스터</h2>

            {/* 검색 + 자동완성 */}
            <div className="relative mb-3">
              <Search style={{ position: 'absolute', left: 10, top: 12, width: 14, height: 14, color: 'var(--text-muted)' }} />
              <input
                value={monsterQuery}
                onFocus={() => setShowMonsterSuggest(true)}
                onChange={(e) => {
                  setMonsterQuery(e.target.value);
                  setShowMonsterSuggest(true);
                }}
                onBlur={() => setTimeout(() => setShowMonsterSuggest(false), 120)}
                placeholder="몬스터 이름 검색..."
                style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)', paddingLeft: 32 }}
                className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]"
              />
              {showMonsterSuggest && (
                <div
                  style={{ background: 'var(--card)', border: '1px solid var(--border)', boxShadow: '0 8px 20px rgba(0,0,0,0.15)' }}
                  className="absolute z-20 left-0 right-0 mt-1 rounded-lg max-h-56 overflow-y-auto"
                >
                  {monsterSuggestions.length === 0 ? (
                    <p style={{ color: 'var(--text-disabled)' }} className="text-xs px-3 py-2">검색 결과가 없습니다</p>
                  ) : (
                    monsterSuggestions.map((m) => (
                      <button
                        key={m.id}
                        type="button"
                        onMouseDown={(e) => e.preventDefault()}
                        onClick={() => {
                          setMonster(m);
                          setMonsterQuery(m.name);
                          setShowMonsterSuggest(false);
                        }}
                        style={{ borderBottom: '1px solid var(--border)', width: '100%', textAlign: 'left' }}
                        className="px-3 py-2 hover:bg-[var(--bg)] transition-colors"
                      >
                        <p className="text-sm font-medium" style={{ color: 'var(--text)' }}>{m.name}</p>
                        <p className="text-[11px] mt-0.5" style={{ color: 'var(--text-muted)' }}>
                          속성: {m.element ?? '-'} · 저항: {m.resistance ?? '-'} · 속성값: {m.elementValue ?? '-'}
                        </p>
                      </button>
                    ))
                  )}
                </div>
              )}
            </div>

            {/* 몬스터 표시 박스 (기존 대비 약 2배 확대) */}
            <div
              style={{
                background: 'var(--bg)',
                border: '1px solid var(--border)',
                minHeight: 360,
                display: 'grid',
                gridTemplateRows: '2fr 1fr',
              }}
              className="rounded-lg overflow-hidden"
            >
              {monster ? (
                <>
                  <div style={{ background: 'var(--border)' }} className="relative overflow-hidden">
                    {monster.imageUrl && !failedMonsterImageIds.has(monster.id) ? (
                      <img
                        src={monster.imageUrl}
                        alt={monster.name}
                        className="w-full h-full object-cover"
                        onError={() => {
                          setFailedMonsterImageIds((prev) => {
                            const next = new Set(prev);
                            next.add(monster.id);
                            return next;
                          });
                        }}
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center">
                        <span className="font-serif text-5xl font-bold" style={{ color: 'var(--text-muted)' }}>
                          {monster.name.charAt(0)}
                        </span>
                      </div>
                    )}
                  </div>
                  <div className="px-3 py-2.5">
                    <div className="flex items-start justify-between gap-2">
                      <p className="font-semibold text-sm" style={{ color: 'var(--text)' }}>{monster.name}</p>
                      <button onClick={() => setMonster(null)} style={{ color: 'var(--text-muted)' }} className="hover:text-[var(--danger)] shrink-0">
                        <X style={{ width: 14, height: 14 }} />
                      </button>
                    </div>
                    <div className="text-xs mt-1.5 space-y-0.5" style={{ color: 'var(--text-muted)' }}>
                      <p>속성: {monster.element ? (ELEMENT_LABELS[monster.element] ?? monster.element) : '-'}</p>
                      <p>저항: {monster.resistance ?? '-'}</p>
                      <p>속성값: {monster.elementValue ?? '-'}</p>
                    </div>
                  </div>
                </>
              ) : (
                <div className="col-span-1 row-span-2 flex items-center justify-center">
                  <p style={{ color: 'var(--text-disabled)' }} className="text-sm">몬스터를 검색해서 선택하세요</p>
                </div>
              )}
            </div>
          </div>

          {/* DPS 계산 버튼 */}
          {selectedDeckId && monster && (
            <button
              onClick={handleCalcDps}
              disabled={calcLoading}
              style={{ background: 'var(--brown)', color: 'var(--beige)', width: '100%' }}
              className="flex items-center justify-center gap-2 py-3 rounded-xl font-semibold text-sm hover:bg-[var(--brown-dark)] transition-colors disabled:opacity-50"
            >
              <Zap style={{ width: 16, height: 16 }} />
              {calcLoading ? 'DPS 계산 중...' : 'DPS 계산'}
            </button>
          )}

          {/* DPS 결과 */}
          {dpsResult && (
            <div style={{ background: 'var(--card)', border: '1px solid var(--brown)' }} className="rounded-xl p-4">
              <h3 className="font-semibold mb-3 flex items-center gap-2" style={{ color: 'var(--brown)' }}>
                <Zap style={{ width: 16, height: 16 }} /> DPS 분석
              </h3>

              <div className="grid grid-cols-2 gap-2 mb-3">
                <div style={{ background: 'var(--beige)', borderRadius: 8 }} className="p-2.5 text-center">
                  <p style={{ color: 'var(--text-muted)' }} className="text-[11px] mb-0.5">최종 저항깎</p>
                  <p className="font-semibold text-sm" style={{ color: 'var(--brown)' }}>
                    {(dpsResult.totalResistPierce ?? 0).toLocaleString()}
                  </p>
                </div>
                <div style={{ background: 'var(--beige)', borderRadius: 8 }} className="p-2.5 text-center">
                  <p style={{ color: 'var(--text-muted)' }} className="text-[11px] mb-0.5">최종 속성깎</p>
                  <p className="font-semibold text-sm" style={{ color: 'var(--brown)' }}>
                    {(dpsResult.totalElementPierce ?? 0).toLocaleString()}
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2 mb-3">
                <div style={{ background: 'var(--beige)', borderRadius: 8 }} className="p-2.5 text-center">
                  <p style={{ color: 'var(--text-muted)' }} className="text-[11px] mb-0.5">Raw Total DPS</p>
                  <p className="font-serif text-lg font-bold" style={{ color: 'var(--brown)' }}>
                    {(dpsResult.rawTotalDps ?? dpsResult.totalDps ?? 0).toLocaleString()}
                  </p>
                  <p style={{ color: 'var(--text-muted)' }} className="text-[10px] mt-0.5">계수 기반 (보정 없음)</p>
                </div>
                <div style={{ background: 'var(--beige)', borderRadius: 8 }} className="p-2.5 text-center">
                  <p style={{ color: 'var(--text-muted)' }} className="text-[11px] mb-0.5">Adjust Total DPS</p>
                  <p className="font-serif text-lg font-bold" style={{ color: 'var(--brown)' }}>
                    {(dpsResult.adjustTotalDps ?? dpsResult.totalDps ?? 0).toLocaleString()}
                  </p>
                  <p style={{ color: 'var(--text-muted)' }} className="text-[10px] mt-0.5">속성값 보정 적용</p>
                </div>
              </div>

              {(dpsResult.memberResults ?? []).length > 0 && (
                <div className="space-y-2">
                  <p className="text-xs font-medium" style={{ color: 'var(--text-muted)' }}>용병별 속성값 · 데미지 비중</p>
                  {(dpsResult.memberResults ?? []).map((m) => {
                    const pct = roundedDamageShare(m.damageShare) ?? 0;
                    return (
                      <div key={m.memberId}>
                        <div className="flex justify-between text-xs mb-0.5 gap-2">
                          <span style={{ color: 'var(--text)' }} className="truncate">{m.mercenaryName}</span>
                          <span style={{ color: 'var(--text-muted)' }} className="shrink-0">
                            속성값 {m.elementValue.toLocaleString()} · {pct.toFixed(1)}%
                          </span>
                        </div>
                        <div style={{ background: 'var(--border)', borderRadius: 3, height: 4 }}>
                          <div style={{ background: 'var(--brown)', borderRadius: 3, height: 4, width: `${pct}%` }} />
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}
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
                    gridTemplateColumns: 'repeat(7, minmax(0, 1fr))',
                    gap: 8,
                  }}
                >
                  {mercenaryCategories.map((category) => {
                    const active = activeMercenaryCategory === category;
                    const selectedCount = selectedCountByCategory(category);
                    return (
                      <button
                        key={category}
                        onClick={() => setSelectedMercenaryCategory(category)}
                        style={{
                          background: active ? 'var(--brown)' : 'var(--bg)',
                          color: active ? 'var(--beige)' : 'var(--text-muted)',
                          border: '1px solid var(--border)',
                          minHeight: 40,
                        }}
                        className="rounded-lg px-3 py-2 font-medium hover:border-[var(--brown)] transition-colors"
                      >
                        <span style={{ fontSize: 16 }}>{category}</span>
                        {selectedCount > 0 && (
                          <span
                            style={{
                              background: active ? 'rgba(255,255,255,0.25)' : 'var(--beige)',
                              color: active ? 'var(--beige)' : 'var(--brown)',
                            }}
                            className="ml-1.5 px-1.5 py-0.5 rounded-full text-[10px]"
                          >
                            선택됨 {selectedCount}
                          </span>
                        )}
                      </button>
                    );
                  })}
                </div>
              </section>

              {/* 선택한 카테고리의 용병 리스트 */}
              <section style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-4">
                {mercenaryLoading ? (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(86px, 1fr))', gap: 10 }}>
                    {[1, 2, 3, 4, 5, 6].map((i) => (
                      <div key={i} style={{ background: 'var(--bg)', height: 124 }} className="rounded-lg animate-pulse" />
                    ))}
                  </div>
                ) : displayedMercenaries.length === 0 ? (
                  <p className="text-center py-8 text-sm" style={{ color: 'var(--text-disabled)' }}>해당 카테고리의 용병이 없습니다</p>
                ) : (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(86px, 1fr))', gap: 10 }}>
                    {displayedMercenaries.map((merc) => {
                      const selected = selectedMercenaryIds.has(merc.id);
                      const blocked = !selected && Boolean(getMercenaryRestrictionMessage(merc));
                      return (
                        <button
                          key={merc.id}
                          onClick={() => handleAddMember(merc)}
                          disabled={blocked}
                          style={{
                            background: selected ? 'var(--beige)' : 'var(--bg)',
                            border: `1px solid ${selected ? 'var(--brown)' : 'var(--border)'}`,
                            overflow: 'hidden',
                            opacity: !selected && blocked ? 0.45 : 1,
                            cursor: blocked ? 'not-allowed' : 'pointer',
                          }}
                          className="rounded-lg hover:border-[var(--brown)] transition-all group"
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
                            {selected && (
                              <span
                                style={{ background: 'var(--brown)', color: 'var(--beige)' }}
                                className="absolute top-1.5 left-1.5 text-[10px] px-1.5 py-0.5 rounded"
                              >
                                클릭 시 해제
                              </span>
                            )}
                          </div>
                          <div className="px-2 py-1.5">
                            <p className="text-xs font-medium truncate text-center" style={{ color: 'var(--text)' }}>{merc.name}</p>
                          </div>
                        </button>
                      );
                    })}
                  </div>
                )}
              </section>

              {/* 12칸 덱 카드 */}
              <section>
                <div className="flex items-center justify-between mb-3">
                  <h2 className="font-semibold" style={{ color: 'var(--text)' }}>
                    {deckDetail.name}
                    <span style={{ color: 'var(--text-muted)' }} className="font-normal text-sm ml-2">
                      ({memberCount} / {maxMembers})
                    </span>
                  </h2>
                  <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
                    첫 번째 칸은 주인공 전용입니다
                  </p>
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
                      const elementValue = member ? elementValueByMemberId.get(member.id) : null;
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
                          <div className="px-2 py-1.5 flex items-center justify-between" style={{ borderBottom: '1px solid var(--border)', minHeight: 28 }}>
                            <span className="text-[11px] font-semibold" style={{ color: damageShare != null ? 'var(--brown)' : 'var(--text-disabled)' }}>
                              {damageShare != null ? `${damageShare.toFixed(1)}%` : ''}
                            </span>
                            {member && (
                              <button
                                onClick={() => handleRemoveMember(member.id)}
                                style={{ color: 'var(--text-muted)' }}
                                className="hover:text-[var(--danger)] transition-colors"
                              >
                                <X style={{ width: 13, height: 13 }} />
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
                              <div className="p-2 flex-1 flex flex-col justify-between">
                                <div>
                                  <p className="font-medium text-sm truncate" style={{ color: 'var(--text)' }}>{member.mercenaryName}</p>
                                  <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>
                                    {mercenaryCategory(merc)}
                                    {elementValue != null && (
                                      <span> · 속성값 {elementValue.toLocaleString()}</span>
                                    )}
                                  </p>
                                </div>
                                <div className="grid grid-cols-2 gap-1.5 mt-2">
                                  <button
                                    onClick={() => {
                                      setSetupInitialTab('equipment');
                                      setEquipmentMember(member);
                                    }}
                                    style={{ background: 'var(--brown)', color: 'var(--beige)' }}
                                    className="rounded py-1.5 text-[11px] font-semibold hover:bg-[var(--brown-dark)] transition-colors"
                                  >
                                    장비
                                  </button>
                                  <button
                                    onClick={() => {
                                      setSetupInitialTab('characteristic');
                                      setEquipmentMember(member);
                                    }}
                                    style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
                                    className="rounded py-1.5 text-[11px] font-semibold hover:border-[var(--brown)] hover:text-[var(--brown)] transition-colors"
                                  >
                                    특성
                                  </button>
                                </div>
                              </div>
                            </>
                          ) : (
                            <div className="flex-1 flex items-center justify-center text-center px-4">
                              <p className="text-sm" style={{ color: 'var(--text-disabled)' }}>
                                {isHeroSlot ? '주인공을 선택하세요' : '용병을 선택하세요'}
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
              <p style={{ color: 'var(--text-disabled)' }} className="text-sm">왼쪽에서 덱을 생성하거나 선택하세요</p>
            </div>
          )}
        </div>
      </div>

      {equipmentMember && selectedDeckId && (
        <EquipmentSetupModal
          member={equipmentMember}
          deckId={selectedDeckId}
          deckEffectSignature={deckEffectSignature}
          initialTab={setupInitialTab}
          onClose={() => setEquipmentMember(null)}
          onRefresh={() => loadDeckDetail(selectedDeckId)}
        />
      )}
    </div>
  );
}
