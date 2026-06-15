import type {
  BonusStatTargetDto,
  DeckDetailDto,
  DpsEvaluationRequestBody,
  ResistanceTypeDto,
  ScenarioItemTypeDto,
} from '@/lib/api';

export const EOK = 100_000_000;

/** 억당 가성비 — final DPS 증가율(%) ÷ 가격(억). 예: 억당 +2.94% */
export function formatEfficiencyPerEok(eff: number | null | undefined): string {
  if (eff == null) return '—';
  const sign = eff > 0 ? '+' : '';
  return `억당 ${sign}${eff.toFixed(2)}%`;
}

/** final DPS 총 증가율(%) */
export function formatFinalDpsIncreaseRate(rate: number | null | undefined): string {
  if (rate == null) return '—';
  const sign = rate > 0 ? '+' : '';
  return `${sign}${rate.toFixed(2)}%`;
}

/** 억 단위 문자열 → 전(골드) */
export function parseEokPrice(value: string): number {
  const amount = Number(value);
  if (!Number.isFinite(amount) || amount <= 0) return 0;
  return Math.round(amount * EOK);
}

export function buildMemberInputs(deck: DeckDetailDto) {
  return deck.members.map((m) => ({
    memberId: m.id,
    level: (m.level === 260 ? 260 : 250) as 250 | 260,
    bonusTarget: (m.bonusTarget ?? 'MAIN_STAT') as BonusStatTargetDto,
    bonusAmount: m.bonusAmount ?? 0,
  }));
}

/** 덱 기본 저항 종류 — 풍속성 사천왕·각성사천왕 있으면 HITTING */
export function resolveResistanceType(
  deck: DeckDetailDto,
  mercenaryById: Map<number, { category?: string; element?: string }>
): ResistanceTypeDto {
  const hasWindHeavenlyKing = deck.members.some((m) => {
    const merc = mercenaryById.get(m.mercenaryId);
    if (!merc) return false;
    const cat = merc.category ?? '';
    const isKing =
      cat.includes('사천왕') ||
      cat === 'FOUR_HEAVENLY_KINGS' ||
      cat === 'FOUR_HEAVENLY_KINGS_AWAKENING';
    return isKing && merc.element === 'WIND';
  });
  return hasWindHeavenlyKing ? 'HITTING' : 'MAGIC';
}

export interface BuildEvaluationParams {
  deckId: number;
  deck: DeckDetailDto;
  monsterId: number;
  resistanceType: ResistanceTypeDto;
  candidateType: ScenarioItemTypeDto;
  affectedMemberId: number | null;
  // 아이템 단품
  singleItemId?: number;
  singleItemRitual?: { ritualId: number; outcome: 'SUCCESS' | 'GREAT_SUCCESS' } | null;
  // 아이템 세트
  setId?: number;
  setLines?: DpsEvaluationRequestBody['scenario']['lines'];
  priceOverrides?: Record<string, number>;
  // 용병
  mercenaryId?: number;
  mercenaryMode?: 'REPLACE' | 'APPEND';
  mercenaryPrice?: number;
  mercenaryLevel?: 250 | 260;
  mercenaryBonusTarget?: BonusStatTargetDto;
  mercenaryBonusAmount?: number;
  mercenaryCharacteristics?: { characteristicId: number; selectedLevel: number }[];
}

export function buildEvaluationRequest(params: BuildEvaluationParams): DpsEvaluationRequestBody {
  const memberInputs = buildMemberInputs(params.deck);

  if (params.candidateType === 'MERCENARY') {
    return {
      deckId: params.deckId,
      monsterId: params.monsterId,
      resistanceType: params.resistanceType,
      memberInputs,
      persist: true,
      price: params.mercenaryPrice ?? null,
      scenario: {
        type: 'MERCENARY',
        mercenaryId: params.mercenaryId!,
        mode: params.mercenaryMode!,
        affectedMemberId: params.mercenaryMode === 'REPLACE' ? params.affectedMemberId : null,
        level: params.mercenaryLevel ?? 250,
        bonusTarget: params.mercenaryBonusTarget ?? 'MAIN_STAT',
        bonusAmount: params.mercenaryBonusAmount ?? 0,
        characteristics: params.mercenaryCharacteristics ?? [],
      },
    };
  }

  if (params.candidateType === 'ITEM_SINGLE') {
    return {
      deckId: params.deckId,
      monsterId: params.monsterId,
      resistanceType: params.resistanceType,
      memberInputs,
      persist: true,
      priceOverrides: params.priceOverrides,
      scenario: {
        type: 'ITEM_SINGLE',
        affectedMemberId: params.affectedMemberId!,
        lines: [
          {
            itemId: params.singleItemId!,
            quantity: 1,
            sortOrder: 0,
            equipmentDetail: {
              enhanceLevel: 0,
              hasRitual: params.singleItemRitual != null,
              rituals: params.singleItemRitual ? [params.singleItemRitual] : [],
            },
          },
        ],
      },
    };
  }

  // ITEM_SET
  const scenario: DpsEvaluationRequestBody['scenario'] = {
    type: 'ITEM_SET',
    setId: params.setId!,
    affectedMemberId: params.affectedMemberId!,
  };
  if (params.setLines && params.setLines.length > 0) {
    scenario.lines = params.setLines;
  }

  return {
    deckId: params.deckId,
    monsterId: params.monsterId,
    resistanceType: params.resistanceType,
    memberInputs,
    persist: true,
    priceOverrides: params.priceOverrides,
    scenario,
  };
}

export const CANDIDATE_TYPE_LABEL: Record<ScenarioItemTypeDto, string> = {
  ITEM_SINGLE: '아이템 단품',
  ITEM_SET: '아이템 세트',
  MERCENARY: '용병',
};
