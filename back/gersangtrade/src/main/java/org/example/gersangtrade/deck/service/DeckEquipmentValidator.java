package org.example.gersangtrade.deck.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.ItemMercenaryRestrictionRepository;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeckEquipmentValidator {

    private final ItemMercenaryRestrictionRepository itemMercenaryRestrictionRepository;

    public void validateSlotCompatibility(EquipSlot slot, EquipmentItem item) {
        if (slot.name().startsWith("APP_")) {
            if (item.getEquipmentKind() != EquipmentKind.APPEARANCE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "외변 슬롯에는 외변 아이템만 착용 가능합니다.");
            }
            if (item.getEquipSlot() != slot && fallbackEquipmentSlot(slot) != item.getSlot()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 외변 슬롯에 착용 불가능한 아이템입니다.");
            }
        } else if (slot == EquipSlot.RING_1 || slot == EquipSlot.RING_2) {
            if (item.getSlot() != EquipmentSlot.RING) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "반지 슬롯에는 반지 아이템만 착용 가능합니다.");
            }
        } else {
            if (item.getEquipmentKind() != EquipmentKind.NORMAL) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "일반 슬롯에는 일반 장비만 착용 가능합니다.");
            }
            if (item.getEquipSlot() != slot && fallbackEquipmentSlot(slot) != item.getSlot()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 슬롯에 착용 불가능한 아이템입니다.");
            }
        }
    }

    public EquipSlot fallbackEquipSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HELMET -> EquipSlot.HELMET;
            case ARMOR -> EquipSlot.ARMOR;
            case WEAPON -> EquipSlot.WEAPON;
            case SHOES -> EquipSlot.SHOES;
            case GLOVES -> EquipSlot.GLOVES;
            case BELT -> EquipSlot.BELT;
            case TALISMAN -> EquipSlot.CHARM;
            default -> null;
        };
    }

    public EquipmentSlot fallbackEquipmentSlot(EquipSlot slot) {
        return switch (slot) {
            case HELMET -> EquipmentSlot.HELMET;
            case ARMOR -> EquipmentSlot.ARMOR;
            case WEAPON -> EquipmentSlot.WEAPON;
            case SHOES -> EquipmentSlot.SHOES;
            case GLOVES -> EquipmentSlot.GLOVES;
            case BELT -> EquipmentSlot.BELT;
            case CHARM -> EquipmentSlot.TALISMAN;
            case APP_SPIRIT, APP_EARRING, APP_NECKLACE -> EquipmentSlot.ACCESSORY;
            case APP_BRACELET -> EquipmentSlot.BRACELET;
            case APP_GREAVES -> EquipmentSlot.LEGGING;
            case APP_WAR_GOD -> EquipmentSlot.DIVINE;
            default -> null;
        };
    }

    public EquipSlot resolveSetEquipSlot(EquipmentSlot pieceSlot, EquipmentItem item) {
        EquipSlot fromPiece = fallbackEquipSlot(pieceSlot);
        if (fromPiece != null) {
            return fromPiece;
        }
        if (item.getEquipSlot() != null) {
            return item.getEquipSlot();
        }
        return fallbackEquipSlot(item.getSlot());
    }

    /**
     * 아이템 착용 용병 제한 검증.
     * item_mercenary_restrictions 행이 없으면 공용. 있으면 하나 이상 조건 일치 시 통과.
     */
    public void validateMercenaryRestriction(Mercenary mercenary, EquipmentItem item) {
        var restrictions = itemMercenaryRestrictionRepository.findByItemId(item.getItemId());
        if (restrictions.isEmpty()) return;
        boolean allowed = restrictions.stream().anyMatch(r -> r.allows(mercenary));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "해당 용병은 이 아이템을 착용할 수 없습니다.");
        }
    }

    /**
     * 명왕 편성 제한 검증.
     * 1. 부동명왕(EARTH) 제외 명왕·각성명왕 합산 최대 2명.
     * 2. 동일 속성(Nature) 계열 명왕·각성명왕은 한 명만 (일반/각성 중 택1).
     */
    public void validateMyeongwangComposition(List<UserDeckMember> currentMembers, Mercenary incoming) {
        boolean isMyeongwang = incoming.getCategory() == MercenaryCategory.MYEONG_KING
                || incoming.getCategory() == MercenaryCategory.MYEONG_KING_AWAKENING;
        if (!isMyeongwang) return;

        if (incoming.getNature() != null && incoming.getNature() != Nature.NONE) {
            boolean hasSameNatureMyeongwang = currentMembers.stream()
                    .anyMatch(m -> isMyeongwangMember(m)
                            && m.getMercenary().getNature() == incoming.getNature());
            if (hasSameNatureMyeongwang) {
                String natureLabel = incoming.getNature().getDisplayName();
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        natureLabel + " 계열 명왕(일반/각성)은 한 명만 편성할 수 있습니다.");
            }
        }

        if (incoming.getNature() != Nature.EARTH) {
            long nonEarthCount = currentMembers.stream()
                    .filter(m -> isMyeongwangMember(m)
                            && m.getMercenary().getNature() != Nature.EARTH)
                    .count();
            if (nonEarthCount >= 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "부동명왕을 제외한 명왕·각성명왕은 최대 2명까지 편성할 수 있습니다.");
            }
        }
    }

    public static boolean isMyeongwangMember(UserDeckMember member) {
        MercenaryCategory category = member.getMercenary().getCategory();
        return category == MercenaryCategory.MYEONG_KING
                || category == MercenaryCategory.MYEONG_KING_AWAKENING;
    }
}
