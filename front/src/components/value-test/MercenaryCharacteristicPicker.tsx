'use client';

import { useMemo } from 'react';
import type { MercenaryCharacteristicSetupDto } from '@/lib/api';
import {
  characteristicMaxLevel,
  formatLevelPreview,
  isSelectableCharacteristic,
  layoutCharacteristicGrid,
  type CharacteristicDef,
  usedCharacteristicPoints,
} from '@/lib/mercenaryCharacteristicUi';

interface MercenaryCharacteristicPickerProps {
  setup: MercenaryCharacteristicSetupDto | null;
  loading?: boolean;
  selections: Record<number, number>;
  onSelectionsChange: (next: Record<number, number>) => void;
}

function CharacteristicCard({
  characteristic,
  selectedLevel,
  onSelectLevel,
}: {
  characteristic: CharacteristicDef;
  selectedLevel?: number;
  onSelectLevel: (id: number, level?: number) => void;
}) {
  const maxLevel = characteristicMaxLevel(characteristic);
  const tiers = Array.from({ length: maxLevel }, (_, i) => i + 1);
  const preview = selectedLevel ? formatLevelPreview(characteristic, selectedLevel) : [];

  return (
    <div
      className="rounded-lg p-3 min-h-[160px] flex flex-col"
      style={{ background: 'var(--bg)', border: '1px solid var(--border)' }}
    >
      <div className="flex items-start justify-between gap-2 mb-2">
        <p className="text-sm font-semibold" style={{ color: 'var(--text)' }}>
          {characteristic.name}
        </p>
        <span
          className="text-[10px] shrink-0 px-1.5 py-0.5 rounded"
          style={{ background: 'var(--card)', color: 'var(--brown)' }}
        >
          {characteristic.point != null && characteristic.point > 0
            ? `${characteristic.point}pt/lv`
            : 'Lv=pt'}
        </span>
      </div>
      <div className="flex flex-wrap gap-1 mb-2">
        {tiers.map((tier) => {
          const selected = selectedLevel === tier;
          return (
            <button
              key={tier}
              type="button"
              onClick={() => onSelectLevel(characteristic.characteristicId, tier)}
              className="flex-1 min-w-[2rem] rounded py-1.5 text-xs font-semibold transition-colors"
              style={{
                background: selected ? 'var(--brown)' : 'var(--card)',
                color: selected ? 'var(--beige)' : 'var(--text-muted)',
                border: '1px solid var(--border)',
              }}
            >
              {tier}
            </button>
          );
        })}
        {selectedLevel != null && selectedLevel > 0 && (
          <button
            type="button"
            onClick={() => onSelectLevel(characteristic.characteristicId)}
            className="rounded px-2 py-1.5 text-[10px]"
            style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
          >
            해제
          </button>
        )}
      </div>
      <div className="flex-1 text-[11px] leading-relaxed space-y-0.5" style={{ color: 'var(--text-muted)' }}>
        {preview.length === 0 ? (
          <span style={{ color: 'var(--text-disabled)' }}>레벨을 선택하면 효과가 표시됩니다</span>
        ) : (
          preview.map((line) => <div key={line}>{line}</div>)
        )}
      </div>
    </div>
  );
}

/** 후보 용병 특성 배분 UI — 덱 설정과 동일한 4분면·포인트 규칙 */
export default function MercenaryCharacteristicPicker({
  setup,
  loading = false,
  selections,
  onSelectionsChange,
}: MercenaryCharacteristicPickerProps) {
  const grid = useMemo(
    () => layoutCharacteristicGrid(setup?.characteristics ?? []),
    [setup],
  );

  const usedPoints = useMemo(
    () => usedCharacteristicPoints(setup?.characteristics ?? [], selections),
    [setup, selections],
  );

  const maxPoints = setup?.maxCharacteristicPoints ?? 0;
  const overBudget = maxPoints > 0 && usedPoints > maxPoints;

  function handleSelectLevel(id: number, level?: number) {
    const next = { ...selections };
    if (level == null || level <= 0) {
      delete next[id];
    } else {
      next[id] = level;
    }
    onSelectionsChange(next);
  }

  if (loading) {
    return <p className="text-xs" style={{ color: 'var(--text-muted)' }}>특성 불러오는 중…</p>;
  }

  if (!setup || setup.characteristics.length === 0) {
    return (
      <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
        이 용병은 선택 가능한 특성이 없습니다.
      </p>
    );
  }

  const selectable = setup.characteristics.filter(isSelectableCharacteristic);

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between gap-2">
        <label className="text-xs font-medium" style={{ color: 'var(--text-muted)' }}>
          특성 배분
        </label>
        <span
          className="text-xs font-semibold"
          style={{ color: overBudget ? 'var(--danger)' : 'var(--brown)' }}
        >
          {usedPoints} / {maxPoints} pt
        </span>
      </div>

      {selectable.length === 0 ? (
        <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
          자동 적용 특성만 있는 용병입니다.
        </p>
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {grid.grid.map((cell) => (
              <div key={cell.slot} className="space-y-2">
                {cell.lower && (
                  <CharacteristicCard
                    characteristic={cell.lower}
                    selectedLevel={selections[cell.lower.characteristicId]}
                    onSelectLevel={handleSelectLevel}
                  />
                )}
                {cell.upper && (
                  <CharacteristicCard
                    characteristic={cell.upper}
                    selectedLevel={selections[cell.upper.characteristicId]}
                    onSelectLevel={handleSelectLevel}
                  />
                )}
              </div>
            ))}
          </div>
          {grid.overflow.length > 0 && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {grid.overflow.map((c) => (
                <CharacteristicCard
                  key={c.characteristicId}
                  characteristic={c}
                  selectedLevel={selections[c.characteristicId]}
                  onSelectLevel={handleSelectLevel}
                />
              ))}
            </div>
          )}
        </>
      )}

      {grid.auto.length > 0 && (
        <div className="text-[11px] space-y-1" style={{ color: 'var(--text-disabled)' }}>
          <p className="font-medium" style={{ color: 'var(--text-muted)' }}>자동 적용 특성</p>
          {grid.auto.map((c) => (
            <p key={c.characteristicId}>· {c.name}</p>
          ))}
        </div>
      )}

      {overBudget && (
        <p className="text-xs" style={{ color: 'var(--danger)' }}>
          특성 포인트가 {maxPoints}pt를 초과했습니다.
        </p>
      )}
    </div>
  );
}
