import type { EquipmentItemDto } from '@/lib/api';

export function equipmentItemKey(item: EquipmentItemDto) {
  return item.itemId ?? item.id ?? 0;
}

export function equipmentItemToSearchResult(item: EquipmentItemDto, selectedSlot: string) {
  return {
    id: equipmentItemKey(item),
    name: item.name,
    type: 'EQUIPMENT' as const,
    equipmentKind: item.equipmentKind,
    slot: item.slot,
    setId: item.setId,
    setName: item.setName,
    imageUrl: item.imageUrl,
    equipSlot: item.equipSlot ?? selectedSlot,
  };
}
