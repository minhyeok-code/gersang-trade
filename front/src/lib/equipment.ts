import type { EquipmentItemDto, ItemSearchResult } from '@/lib/api';

export function equipmentItemKey(item: EquipmentItemDto) {
  return item.itemId ?? item.id ?? 0;
}

type ExclusiveEquipFields = {
  exclusiveMercenaryId?: number | null;
  restrictionMercenaryIds?: number[];
};

/** 해당 용병 전용장비 여부 (mercenary_id 또는 restriction 기준) */
export function isExclusiveForMercenary(item: ExclusiveEquipFields, mercenaryId: number) {
  if (item.exclusiveMercenaryId === mercenaryId) return true;
  return item.restrictionMercenaryIds?.includes(mercenaryId) ?? false;
}

/**
 * 해당 용병이 착용 가능한 공용 장비 여부.
 * restriction·전용 FK가 없거나, 다른 용병 전용이 아닌 경우 true.
 */
export function isCommonEquipmentForMercenary(item: ExclusiveEquipFields, mercenaryId: number) {
  if (isExclusiveForMercenary(item, mercenaryId)) return false;
  if (item.restrictionMercenaryIds && item.restrictionMercenaryIds.length > 0) return false;
  if (item.exclusiveMercenaryId != null) return false;
  return true;
}

/** 덱 장비 선택 — 해당 용병 전용장비만 반환 (없으면 빈 배열) */
export function exclusiveEquipmentForMercenary(items: EquipmentItemDto[], mercenaryId: number) {
  return items
    .filter((item) => isExclusiveForMercenary(item, mercenaryId))
    .sort((a, b) => equipmentItemKey(b) - equipmentItemKey(a));
}

/**
 * 덱 장비 선택 목록 — 기본은 전용장비만, 검색 시 공용 장비도 포함.
 */
export function buildDeckEquipmentDisplayItems(
  exclusiveItems: EquipmentItemDto[],
  slotItems: EquipmentItemDto[],
  mercenaryId: number,
  selectedSlot: string,
  query: string,
) {
  const keyword = query.trim().toLowerCase();
  const toResult = (item: EquipmentItemDto) => equipmentItemToSearchResult(item, selectedSlot);

  const exclusive = exclusiveItems.map(toResult);
  if (!keyword) {
    return exclusive.sort((a, b) => b.id - a.id);
  }

  const exclusiveMatches = exclusive.filter((item) => item.name.toLowerCase().includes(keyword));
  const commonMatches = slotItems
    .filter((item) => isCommonEquipmentForMercenary(item, mercenaryId))
    .filter((item) => item.name.toLowerCase().includes(keyword))
    .map(toResult);

  const seen = new Set<number>();
  const merged: ItemSearchResult[] = [];
  for (const item of [...exclusiveMatches, ...commonMatches]) {
    if (seen.has(item.id)) continue;
    seen.add(item.id);
    merged.push(item);
  }
  return merged.sort((a, b) => b.id - a.id);
}

export function equipmentItemToSearchResult(item: EquipmentItemDto, selectedSlot: string): ItemSearchResult {
  return {
    id: equipmentItemKey(item),
    name: item.name,
    type: 'EQUIPMENT',
    equipmentKind: item.equipmentKind,
    slot: item.slot,
    setId: item.setId,
    setName: item.setName,
    imageUrl: item.imageUrl,
    equipSlot: item.equipSlot ?? selectedSlot,
    exclusiveMercenaryId: item.exclusiveMercenaryId ?? undefined,
    restrictionMercenaryIds: item.restrictionMercenaryIds,
  };
}
