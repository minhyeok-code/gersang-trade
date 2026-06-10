import type { MercenaryCharacteristicSetupDto } from '@/lib/api';

export type CharacteristicDef = MercenaryCharacteristicSetupDto['characteristics'][number];

/**
 * 특성 1개 선택의 포인트 소모.
 * 일반(사천왕·명왕): point × level / 전설장수(point null): level 자체가 소모 포인트
 */
export function characteristicSelectionCost(
  point: number | null | undefined,
  selectedLevel: number,
): number {
  if (point == null || point === 0) return selectedLevel;
  return point * selectedLevel;
}

export function isSelectableCharacteristic(c: CharacteristicDef) {
  return c.applyType !== 'SELF_AUTO' && c.applyType !== 'ALLY_AUTO';
}

export function characteristicMaxLevel(c: CharacteristicDef) {
  const fromLevels = c.levels.map((l) => l.level);
  return fromLevels.length > 0 ? Math.max(...fromLevels) : 5;
}

export function levelsAtTier(c: CharacteristicDef, tier: number) {
  return c.levels.filter((l) => l.level === tier);
}

export function layoutCharacteristicGrid(characteristics: CharacteristicDef[]) {
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

  const usedUpperIds = new Set(grid.flatMap((g) => (g.upper ? [g.upper.characteristicId] : [])));
  const orphanUpper = upper.filter((c) => !usedUpperIds.has(c.characteristicId));
  const overflowLower = lower.slice(2);
  const overflow = [...overflowLower, ...orphanUpper];

  return {
    grid,
    overflow,
    auto: characteristics.filter((c) => !isSelectableCharacteristic(c)),
  };
}

export function usedCharacteristicPoints(
  characteristics: CharacteristicDef[],
  selections: Record<number, number>,
) {
  return characteristics.reduce((sum, c) => {
    const level = selections[c.characteristicId];
    if (!level) return sum;
    return sum + characteristicSelectionCost(c.point, level);
  }, 0);
}

/** 덱 멤버 특성 DTO용 포인트 합산 */
export function usedMemberCharacteristicPoints(
  characteristics: {
    characteristicId: number;
    point?: number | null;
    selectedLevel?: number | null;
  }[],
) {
  return characteristics.reduce((sum, c) => {
    if (!c.selectedLevel) return sum;
    return sum + characteristicSelectionCost(c.point, c.selectedLevel);
  }, 0);
}

export function validateCharacteristicSelections(
  setup: MercenaryCharacteristicSetupDto | null,
  selections: Record<number, number>,
): string | null {
  if (!setup) return null;
  const used = usedCharacteristicPoints(setup.characteristics, selections);
  if (used > setup.maxCharacteristicPoints) {
    return `특성 포인트가 ${setup.maxCharacteristicPoints}pt를 초과했습니다.`;
  }
  return null;
}

export function toCharacteristicPayload(selections: Record<number, number>) {
  return Object.entries(selections)
    .filter(([, level]) => level > 0)
    .map(([id, level]) => ({
      characteristicId: Number(id),
      selectedLevel: level,
    }));
}

const STAT_LABEL: Record<string, string> = {
  STRENGTH: '힘',
  DEXTERITY: '민첩',
  VITALITY: '생명력',
  INTELLECT: '지력',
  ELEMENT_VALUE: '속성값',
  DAMAGE_PERCENT: '데미지증가',
};

export function formatLevelPreview(c: CharacteristicDef, tier: number): string[] {
  return levelsAtTier(c, tier)
    .map((entry) => {
      if (entry.label && entry.amount) return `${entry.label} ${entry.amount}`;
      if (entry.amount) return entry.amount;
      if (entry.amountValue == null || !entry.statType) return '';
      const label = STAT_LABEL[entry.statType] ?? entry.statType;
      const sign = entry.amountValue >= 0 ? '+' : '';
      return `${label} ${sign}${entry.amountValue}`;
    })
    .filter(Boolean);
}
