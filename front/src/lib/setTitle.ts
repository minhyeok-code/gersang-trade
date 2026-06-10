/**
 * 거상 세트 거래 표기(네이밍) 유틸리티.
 *
 * ── 구성 종류 (주술 없을 때) ─────────────────────────────────────────────
 * | 포함 피스                         | 표기 예시 (00 = 주술 마크 자리)   |
 * |----------------------------------|-----------------------------------|
 * | 갑옷 + 투구                      | {세트명}갑투  (예: 각성광목천왕갑투) |
 * | 장갑 + 요대 + 신발 (변두리)       | 변{세트명}    (예: 변각성광목천왕)   |
 * | 반지 2개                         | {세트명}반쌍  (예: 각성광목천왕반쌍) |
 * | 갑투 + 변 (5피스)                | 풀 {세트명}   (예: 풀 각성광목천왕)  |
 * | 5피스 + 반지                     | 풀 {세트명}반쌍                    |
 *
 * ── 주술 접두 숫자 (동일 마크·규칙 위치에만 적용) ─────────────────────────
 * | 접두   | 적용 조건                                              |
 * |--------|--------------------------------------------------------|
 * | 2{마크} | **갑투** 구성으로 팔 때, 주술이 갑옷·투구에만 있을 때      |
 * | 3{마크} | **풀** 또는 **변** 구성, 주술이 변두리(장갑·요대·신발)에만 |
 * | 5{마크} | **풀**·**풀반쌍**, 주술 가능 5피스(갑투+변) 전부에 있을 때   |
 *
 * 표기 예 (세트명=각성광목천왕, 마크=<00>):
 * - 2<00>각성광목천왕갑투
 * - 3<00>변각성광목천왕
 * - 5<00> 풀 각성광목천왕
 * - 5<00> 풀 각성광목천왕반쌍
 * - 3<00> 풀 각성광목천왕 (풀 판매·변두리만 주술)
 *
 * 마크가 서로 다르면 접두 숫자 없이 구성 표기만 사용 (폴백).
 */

import type { RitualDto, ScenarioLineBody, SetPieceDto } from '@/lib/api';

// ── 슬롯 그룹 ─────────────────────────────────────────────────────────────

/** 갑투: 갑옷 + 투구 */
export const GAMTU_SLOTS = ['ARMOR', 'HELMET'] as const;

/** 변(변두리): 장갑 + 요대 + 신발 */
export const BYEON_SLOTS = ['GLOVES', 'BELT', 'SHOES'] as const;

/** 반지 (세트 정의상 RING 슬롯 1종, 게임 내 양손 2개) */
export const RING_SLOTS = ['RING'] as const;

/** 풀 5피스 (반지 제외) */
export const FULL_ARMOR_SLOTS = [...GAMTU_SLOTS, ...BYEON_SLOTS] as const;

export type SetBundleKind = 'GAMTU' | 'BYEON' | 'BANSSANG' | 'FULL' | 'FULL_BANSSANG';

/** 주술 접두 숫자 — 0 = 미주술 */
export type RitualCountOption = 0 | 2 | 3 | 5;

export const BUNDLE_KIND_LABEL: Record<SetBundleKind, string> = {
  GAMTU: '갑투',
  BYEON: '변',
  BANSSANG: '반쌍',
  FULL: '풀',
  FULL_BANSSANG: '풀반쌍',
};

export const BUNDLE_KIND_DESCRIPTION: Record<SetBundleKind, string> = {
  GAMTU: '갑옷 + 투구',
  BYEON: '장갑 + 요대 + 신발',
  BANSSANG: '반지 2개',
  FULL: '갑투 + 변 (5피스)',
  FULL_BANSSANG: '5피스 + 반지',
};

export interface RitualMarkOption {
  mark: string;
  label: string;
  ritualId: number;
  outcome: 'SUCCESS' | 'GREAT_SUCCESS';
}

export interface SetPieceState {
  itemId: number;
  itemName: string;
  slot: string;
  included: boolean;
  hasRitual: boolean;
  hasRitualOptions: boolean;
}

const SLOT_ORDER: Record<string, number> = {
  ARMOR: 0, HELMET: 1, GLOVES: 2, BELT: 3, SHOES: 4, RING: 5, WEAPON: 6, TALISMAN: 7,
};

export function sortSetPiecesBySlot<T extends { slot: string }>(pieces: T[]): T[] {
  return [...pieces].sort((a, b) => (SLOT_ORDER[a.slot] ?? 99) - (SLOT_ORDER[b.slot] ?? 99));
}

function slotsForKind(kind: SetBundleKind): readonly string[] {
  switch (kind) {
    case 'GAMTU': return GAMTU_SLOTS;
    case 'BYEON': return BYEON_SLOTS;
    case 'BANSSANG': return RING_SLOTS;
    case 'FULL': return FULL_ARMOR_SLOTS;
    case 'FULL_BANSSANG': return [...FULL_ARMOR_SLOTS, ...RING_SLOTS];
  }
}

/** 구성별 선택 가능한 주술 개수 */
export function getRitualCountOptions(kind: SetBundleKind): RitualCountOption[] {
  switch (kind) {
    case 'GAMTU': return [0, 2];
    case 'BYEON': return [0, 3];
    case 'BANSSANG': return [0];
    case 'FULL':
    case 'FULL_BANSSANG': return [0, 3, 5];
  }
}

function ritualSlotsForCount(kind: SetBundleKind, count: RitualCountOption): readonly string[] {
  if (count === 0) return [];
  if (count === 2) return GAMTU_SLOTS;
  if (count === 3) return BYEON_SLOTS;
  if (count === 5) return FULL_ARMOR_SLOTS;
  return [];
}

/** 구성 + 주술 개수에 맞게 피스 포함·주술 플래그 일괄 설정 */
export function applyBundleKindToPieces(
  pieces: SetPieceState[],
  kind: SetBundleKind,
  ritualCount: RitualCountOption,
): SetPieceState[] {
  const includedSlots = new Set(slotsForKind(kind));
  const ritualSlots = new Set(ritualSlotsForCount(kind, ritualCount));

  return pieces.map((p) => {
    const included = includedSlots.has(p.slot);
    const hasRitual =
      included && ritualCount > 0 && ritualSlots.has(p.slot) && p.hasRitualOptions;
    return { ...p, included, hasRitual };
  });
}

/** 피스 선택 상태에서 구성 종류 추론 */
export function inferBundleKind(pieces: SetPieceState[]): SetBundleKind | null {
  const included = new Set(pieces.filter((p) => p.included).map((p) => p.slot));
  if (included.size === 0) return null;

  const has = (slots: readonly string[]) => slots.every((s) => included.has(s));
  const hasAny = (slots: readonly string[]) => slots.some((s) => included.has(s));
  const only = (slots: readonly string[]) =>
    [...included].every((s) => (slots as readonly string[]).includes(s));

  if (has(RING_SLOTS) && has(FULL_ARMOR_SLOTS) && only([...FULL_ARMOR_SLOTS, ...RING_SLOTS])) {
    return 'FULL_BANSSANG';
  }
  if (has(FULL_ARMOR_SLOTS) && only(FULL_ARMOR_SLOTS)) return 'FULL';
  if (has(GAMTU_SLOTS) && only(GAMTU_SLOTS)) return 'GAMTU';
  if (has(BYEON_SLOTS) && only(BYEON_SLOTS)) return 'BYEON';
  if (has(RING_SLOTS) && only(RING_SLOTS)) return 'BANSSANG';

  // 부분·혼합 선택 — 풀 계열로 근사
  if (hasAny(FULL_ARMOR_SLOTS)) return included.has('RING') ? 'FULL_BANSSANG' : 'FULL';
  return null;
}

function slotInGroup(slots: readonly string[], slot: string): boolean {
  return slots.includes(slot);
}

function ritualOnlyOnSlots(pieces: SetPieceState[], slots: readonly string[]): boolean {
  const ritual = pieces.filter((p) => p.included && p.hasRitual && p.hasRitualOptions);
  return ritual.length === slots.length && ritual.every((p) => slotInGroup(slots, p.slot));
}

/** 주술이 규칙 위치에만 있는지 검사 후 RitualCountOption 반환 */
export function inferRitualCount(pieces: SetPieceState[], kind: SetBundleKind): RitualCountOption {
  if (!pieces.some((p) => p.included && p.hasRitual && p.hasRitualOptions)) return 0;

  if ((kind === 'FULL' || kind === 'FULL_BANSSANG') && ritualOnlyOnSlots(pieces, FULL_ARMOR_SLOTS)) {
    return 5;
  }
  if (
    (kind === 'BYEON' || kind === 'FULL' || kind === 'FULL_BANSSANG') &&
    ritualOnlyOnSlots(pieces, BYEON_SLOTS)
  ) {
    return 3;
  }
  if (kind === 'GAMTU' && ritualOnlyOnSlots(pieces, GAMTU_SLOTS)) {
    return 2;
  }
  return 0;
}

/**
 * 세트 거래 표기 문자열 생성.
 * @param ritualMarkLabel 주술 마크 문자열 (예: &lt;00&gt;, &lt;북두칠성_개양&gt;)
 */
export function generateSetBundleTitle(
  setName: string,
  kind: SetBundleKind,
  ritualCount: RitualCountOption,
  ritualMarkLabel: string | null,
): string {
  const mark = ritualMarkLabel ?? '';

  switch (kind) {
    case 'GAMTU':
      if (ritualCount === 2 && mark) return `2${mark}${setName}갑투`;
      return `${setName}갑투`;
    case 'BYEON':
      if (ritualCount === 3 && mark) return `3${mark}변${setName}`;
      return `변${setName}`;
    case 'BANSSANG':
      return `${setName}반쌍`;
    case 'FULL':
      if (ritualCount === 5 && mark) return `5${mark} 풀 ${setName}`;
      if (ritualCount === 3 && mark) return `3${mark} 풀 ${setName}`;
      return `풀 ${setName}`;
    case 'FULL_BANSSANG':
      if (ritualCount === 5 && mark) return `5${mark} 풀 ${setName}반쌍`;
      if (ritualCount === 3 && mark) return `3${mark} 풀 ${setName}반쌍`;
      return `풀 ${setName}반쌍`;
  }
}

/** 피스 상태 기반 표기 (구성·주술 자동 추론) */
export function generateSetDisplayTitle(
  setName: string,
  pieces: SetPieceState[],
  ritualMark: RitualMarkOption | null,
): string {
  const kind = inferBundleKind(pieces);
  if (!kind) return setName;

  let ritualCount = inferRitualCount(pieces, kind);
  const markLabel = ritualMark?.label ?? null;

  if (ritualCount > 0 && !ritualMark) ritualCount = 0;
  if (ritualCount > 0 && !getRitualCountOptions(kind).includes(ritualCount)) ritualCount = 0;

  return generateSetBundleTitle(setName, kind, ritualCount, ritualCount > 0 ? markLabel : null);
}

/** 거래 목록 필터용 부분 문자열 */
export function buildSetSearchFilterTokens(
  setName: string,
  kind: SetBundleKind,
  ritualCount: RitualCountOption,
  ritualMarkLabel: string | null,
): string[] {
  const title = generateSetBundleTitle(setName, kind, ritualCount, ritualMarkLabel);
  const tokens = new Set<string>([title, setName, BUNDLE_KIND_LABEL[kind]]);
  if (ritualMarkLabel) {
    tokens.add(ritualMarkLabel);
    if (ritualCount > 0) tokens.add(`${ritualCount}${ritualMarkLabel}`);
  }
  if (kind === 'FULL_BANSSANG' || kind === 'BANSSANG') tokens.add('반쌍');
  if (kind === 'FULL' || kind === 'FULL_BANSSANG') tokens.add('풀');
  return [...tokens].filter(Boolean);
}

// ── 피스 선택 헬퍼 (API 연동) ─────────────────────────────────────────────

export function initSetPieces(pieces: SetPieceDto[], ritualByItemId: Map<number, boolean>): SetPieceState[] {
  return sortSetPiecesBySlot(pieces).map((p) => ({
    itemId: p.itemId,
    itemName: p.itemName,
    slot: p.slot,
    included: true,
    hasRitual: false,
    hasRitualOptions: ritualByItemId.get(p.itemId) ?? false,
  }));
}

export function buildRitualMarkOptions(perPieceRituals: RitualDto[][]): RitualMarkOption[] {
  const seen = new Set<number>();
  const opts: RitualMarkOption[] = [];
  for (const rituals of perPieceRituals) {
    for (const r of rituals) {
      if (seen.has(r.id)) continue;
      seen.add(r.id);
      const mark = r.successMark || r.displayName;
      if (mark) opts.push({ mark, label: mark, ritualId: r.id, outcome: 'SUCCESS' });
      if (r.greatSuccessMark) {
        opts.push({
          mark: r.greatSuccessMark,
          label: `<${r.greatSuccessMark.replace(/[<>]/g, '')}_${(r.successMark ?? r.displayName).replace(/[<>]/g, '')}>`,
          ritualId: r.id,
          outcome: 'GREAT_SUCCESS',
        });
      }
    }
  }
  return opts;
}

export function buildSetScenarioLines(
  pieces: SetPieceState[],
  ritualMark: RitualMarkOption | null,
): ScenarioLineBody[] {
  return pieces
    .filter((p) => p.included)
    .map((p, i) => ({
      itemId: p.itemId,
      quantity: 1,
      sortOrder: i,
      equipmentDetail: {
        enhanceLevel: 0,
        hasRitual: p.hasRitual && ritualMark != null && p.hasRitualOptions,
        rituals:
          p.hasRitual && ritualMark && p.hasRitualOptions
            ? [{ ritualId: ritualMark.ritualId, outcome: ritualMark.outcome }]
            : [],
      },
    }));
}

export function shouldSendSetLines(pieces: SetPieceState[], ritualMark: RitualMarkOption | null): boolean {
  const included = pieces.filter((p) => p.included);
  if (included.length === 0) return false;
  if (included.length < pieces.length) return true;
  if (ritualMark != null && included.some((p) => p.hasRitual && p.hasRitualOptions)) return true;
  return false;
}

export function validateSetBundleSelection(
  kind: SetBundleKind,
  ritualCount: RitualCountOption,
  ritualMark: RitualMarkOption | null,
  pieces: SetPieceState[],
): string | null {
  const included = pieces.filter((p) => p.included);
  if (included.length === 0) return '최소 1개 이상의 피스를 선택해주세요.';

  const inferred = inferBundleKind(pieces);
  if (inferred !== kind) return '선택한 구성과 포함 피스가 일치하지 않습니다.';

  if (ritualCount > 0) {
    if (!ritualMark) return '주술 마크를 선택해주세요.';
    if (!getRitualCountOptions(kind).includes(ritualCount)) {
      return `${BUNDLE_KIND_LABEL[kind]} 구성에서는 주술 ${ritualCount}개를 사용할 수 없습니다.`;
    }
    const actual = inferRitualCount(pieces, kind);
    if (actual !== ritualCount) {
      return `주술 ${ritualCount}개 규칙에 맞게 피스에 주술을 적용해주세요.`;
    }
  } else if (ritualMark) {
    return '주술 개수를 선택해주세요.';
  }

  return null;
}
