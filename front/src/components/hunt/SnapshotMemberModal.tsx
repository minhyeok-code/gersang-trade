'use client';

import { useEffect, useMemo, useState } from 'react';
import { X } from 'lucide-react';
import {
  api,
  type DeckSnapshotCharacteristicSelectionDto,
  type DeckSnapshotContentDto,
  type MemberStatsDto,
  type MercenaryCharacteristicCatalogDto,
} from '@/lib/api';
import { SnapshotFinalStatsTable } from '@/components/hunt/SnapshotFinalStatsTable';

const NORMAL_SLOTS = ['CHARM', 'HELMET', 'GLOVES', 'ARMOR', 'BELT', 'WEAPON', 'SHOES', 'RING_1', 'RING_2'];
const APP_SLOTS = ['APP_SPIRIT', 'APP_EARRING', 'APP_HELMET', 'APP_NECKLACE', 'APP_ARMOR', 'APP_GREAVES', 'APP_WEAPON', 'APP_BRACELET'];

const SLOT_LABEL: Record<string, string> = {
  HELMET: '투구', ARMOR: '갑옷', WEAPON: '무기', SHOES: '신발', GLOVES: '장갑',
  BELT: '요대', CHARM: '신수부', RING_1: '반지', RING_2: '반지',
  RITUAL: '주술',
  APP_SPIRIT: '기운', APP_HELMET: '외투구', APP_ARMOR: '외갑옷', APP_WEAPON: '외무기',
  APP_WAR_GOD: '전신', APP_EARRING: '귀걸이', APP_NECKLACE: '목걸이',
  APP_BRACELET: '팔찌', APP_GREAVES: '각반',
};

type MemberSlot = DeckSnapshotContentDto['members'][number]['member']['slots'][number];
type SnapshotMemberEntry = DeckSnapshotContentDto['members'][number];

function slotsInOrder(slots: MemberSlot[], order: string[]): MemberSlot[] {
  const map = new Map(slots.map((s) => [s.slot, s]));
  return order.flatMap((slot) => {
    const equipped = map.get(slot);
    return equipped ? [equipped] : [];
  });
}

function characteristicDetail(
  catalog: MercenaryCharacteristicCatalogDto | undefined,
  characteristicId: number,
  selectedLevel: number,
) {
  const entry = catalog?.characteristics.find((c) => c.characteristicId === characteristicId);
  if (!entry) return null;
  const levelEntry = entry.levels.find((l) => l.level === selectedLevel);
  return { name: entry.name, level: selectedLevel, tierLabel: levelEntry?.label ?? null };
}

export function SnapshotMemberModal({
  entry,
  deckId,
  catalog,
  catalogLoading,
  level,
  elementValue,
  onClose,
}: {
  entry: SnapshotMemberEntry;
  deckId: number;
  catalog?: MercenaryCharacteristicCatalogDto;
  catalogLoading: boolean;
  level?: number | null;
  elementValue?: number | null;
  onClose: () => void;
}) {
  const { member, characteristics } = entry;
  const [mainTab, setMainTab] = useState<'equipment' | 'build'>('equipment');
  const [stats, setStats] = useState<MemberStatsDto | null>(null);
  const [statsLoading, setStatsLoading] = useState(false);
  const [statsError, setStatsError] = useState<string | null>(null);

  const normalEquip = useMemo(
    () => slotsInOrder(member.slots, NORMAL_SLOTS),
    [member.slots],
  );
  const appEquip = useMemo(
    () => slotsInOrder(member.slots, APP_SLOTS),
    [member.slots],
  );

  useEffect(() => {
    let cancelled = false;
    setStatsLoading(true);
    setStatsError(null);
    setStats(null);
    api.getDeckMemberStats(deckId, member.id)
      .then((res) => {
        if (!cancelled) setStats(res);
      })
      .catch(() => {
        if (!cancelled) {
          setStatsError('스탯 정보를 불러올 수 없습니다.');
        }
      })
      .finally(() => {
        if (!cancelled) setStatsLoading(false);
      });
    return () => { cancelled = true; };
  }, [deckId, member.id]);

  return (
    <div
      className="fixed inset-0 flex items-center justify-center z-[500] p-4"
      style={{ background: 'rgba(0,0,0,0.65)' }}
      onClick={onClose}
    >
      <div
        style={{
          background: 'var(--card)',
          border: '1px solid var(--border)',
          width: 920,
          maxWidth: '96vw',
          height: 640,
          maxHeight: '82vh',
        }}
        className="rounded-xl overflow-hidden flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        <div
          style={{ borderBottom: '1px solid var(--border)' }}
          className="flex items-center justify-between px-5 py-3 shrink-0"
        >
          <div>
            <h2 className="font-semibold" style={{ color: 'var(--text)' }}>{member.mercenaryName}</h2>
            <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>
              Lv.{level ?? '-'}
              {elementValue != null && <> · 속성값 {elementValue.toLocaleString()}</>}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            style={{ color: 'var(--text-muted)' }}
            className="p-1 rounded hover:text-[var(--text)]"
          >
            <X style={{ width: 20, height: 20 }} />
          </button>
        </div>

        <div style={{ borderBottom: '1px solid var(--border)' }} className="flex gap-2 px-5 py-2.5 shrink-0">
          {([
            ['equipment', '장비 · 외형장비'],
            ['build', '특성 · 스탯'],
          ] as const).map(([key, label]) => {
            const active = mainTab === key;
            return (
              <button
                key={key}
                type="button"
                onClick={() => setMainTab(key)}
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
          {mainTab === 'equipment' && (
            <div className="grid md:grid-cols-2 h-full overflow-hidden">
              <EquipmentTextList
                title="장비"
                slots={normalEquip}
                emptyMessage="착용한 장비가 없습니다."
                borderedRight
              />
              <EquipmentTextList
                title="외형장비"
                slots={appEquip}
                emptyMessage="착용한 외형장비가 없습니다."
              />
            </div>
          )}

          {mainTab === 'build' && (
            <div className="grid md:grid-cols-2 h-full overflow-hidden">
              <div
                className="h-full overflow-y-auto p-5 md:border-r"
                style={{ borderColor: 'var(--border)', background: 'var(--bg)' }}
              >
                <p className="text-sm font-semibold mb-3" style={{ color: 'var(--text)' }}>특성</p>
                <CharacteristicPanel
                  characteristics={characteristics}
                  catalog={catalog}
                  catalogLoading={catalogLoading}
                />
              </div>
              <div className="h-full overflow-y-auto p-5">
                <SnapshotFinalStatsTable
                  stats={stats}
                  loading={statsLoading}
                  error={statsError}
                />
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function EquipmentTextList({
  title,
  slots,
  emptyMessage,
  borderedRight = false,
}: {
  title: string;
  slots: MemberSlot[];
  emptyMessage: string;
  borderedRight?: boolean;
}) {
  return (
    <div
      className={`h-full overflow-y-auto p-5 ${borderedRight ? 'md:border-r' : ''}`}
      style={{ borderColor: 'var(--border)' }}
    >
      <p className="text-sm font-semibold mb-3" style={{ color: 'var(--text)' }}>{title}</p>
      {slots.length === 0 ? (
        <p className="text-sm" style={{ color: 'var(--text-muted)' }}>{emptyMessage}</p>
      ) : (
        <ul className="space-y-2">
          {slots.map((slot) => (
            <li
              key={`${slot.slot}-${slot.itemId}`}
              className="text-sm leading-snug"
              style={{ color: 'var(--text)' }}
            >
              <span style={{ color: 'var(--text-muted)' }}>
                {SLOT_LABEL[slot.slot] ?? slot.slot}
              </span>
              {' : '}
              <span>{slot.itemName}</span>
              {slot.ritual?.displayName && (
                <span style={{ color: 'var(--brown)' }}> · {slot.ritual.displayName}</span>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function CharacteristicPanel({
  characteristics,
  catalog,
  catalogLoading,
}: {
  characteristics: DeckSnapshotCharacteristicSelectionDto[];
  catalog?: MercenaryCharacteristicCatalogDto;
  catalogLoading: boolean;
}) {
  if (characteristics.length === 0) {
    return <p className="text-sm" style={{ color: 'var(--text-muted)' }}>선택한 특성이 없습니다.</p>;
  }
  if (catalogLoading && !catalog) {
    return <p className="text-sm" style={{ color: 'var(--text-muted)' }}>특성 정보 불러오는 중...</p>;
  }

  return (
    <ul className="space-y-2">
      {characteristics.map((c) => {
        const detail = characteristicDetail(catalog, c.characteristicId, c.selectedLevel);
        return (
          <li
            key={c.characteristicId}
            className="px-4 py-3 rounded-xl"
            style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
          >
            <p className="text-sm font-medium" style={{ color: 'var(--text)' }}>
              {detail?.name ?? `특성 #${c.characteristicId}`}
            </p>
            <p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>
              Lv.{c.selectedLevel}
              {detail?.tierLabel && <> · {detail.tierLabel}</>}
            </p>
          </li>
        );
      })}
    </ul>
  );
}
