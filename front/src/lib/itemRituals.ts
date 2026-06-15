import { api, type RitualDto, type SetPieceDto } from '@/lib/api';
import {
  applyBundleKindToPieces,
  buildRitualMarkOptions,
  initSetPieces,
  type RitualMarkOption,
  type SetPieceState,
} from '@/lib/setTitle';

export interface SetRitualResult {
  initialPieces: SetPieceState[];
  uniqueRituals: RitualMarkOption[];
}

/** 단품 장비의 주술 옵션 목록 반환. 실패 시 빈 배열. */
export async function fetchSingleItemRituals(itemId: number): Promise<RitualMarkOption[]> {
  const rituals = await api.getItemRituals(itemId).catch(() => [] as RitualDto[]);
  return buildRitualMarkOptions([rituals]);
}

/** 세트 피스 전체의 주술을 병렬 조회해 초기 피스 상태와 고유 주술 옵션 반환. */
export async function fetchSetRituals(pieces: SetPieceDto[]): Promise<SetRitualResult> {
  const perPieceRituals = await Promise.all(
    pieces.map((p) => api.getItemRituals(p.itemId).catch(() => [] as RitualDto[])),
  );
  const ritualMap = new Map(pieces.map((p, i) => [p.itemId, perPieceRituals[i].length > 0]));
  const initialPieces = applyBundleKindToPieces(initSetPieces(pieces, ritualMap), 'FULL', 0);
  const uniqueRituals = buildRitualMarkOptions(perPieceRituals);
  return { initialPieces, uniqueRituals };
}
