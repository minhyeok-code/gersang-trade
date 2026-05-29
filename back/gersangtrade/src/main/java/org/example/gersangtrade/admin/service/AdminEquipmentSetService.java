package org.example.gersangtrade.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.request.ItemRestrictionAddRequest;
import org.example.gersangtrade.admin.dto.response.ItemRestrictionResponse;
import org.example.gersangtrade.admin.dto.set.AdminSetResponse;
import org.example.gersangtrade.admin.dto.set.AdminSetUpdateRequest;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.example.gersangtrade.catalog.repository.ItemMercenaryRestrictionRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.ItemMercenaryRestriction;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminEquipmentSetService {

    private final EquipmentSetRepository equipmentSetRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final ItemMercenaryRestrictionRepository itemMercenaryRestrictionRepository;
    private final MercenaryRepository mercenaryRepository;

    /** 세트 목록 — 이름 검색 + 페이징 */
    @Transactional(readOnly = true)
    public Page<AdminSetResponse> getSets(String name, Pageable pageable) {
        if (name != null && !name.isBlank()) {
            return equipmentSetRepository.findByNameContaining(name, pageable)
                    .map(AdminSetResponse::from);
        }
        return equipmentSetRepository.findAll(pageable).map(AdminSetResponse::from);
    }

    /** 세트 단건 조회 */
    @Transactional(readOnly = true)
    public AdminSetResponse getSet(Long id) {
        return AdminSetResponse.from(findOrThrow(id));
    }

    /** 세트 수정 (이름, 피스 수, 거래 노출 여부) */
    @Transactional
    public AdminSetResponse updateSet(Long id, AdminSetUpdateRequest req) {
        EquipmentSet set = findOrThrow(id);
        set.updateInfo(req.name(), req.totalPieces(), req.isTradeable(), null);
        return AdminSetResponse.from(set);
    }

    /**
     * 세트에 속한 모든 피스에 착용 제한을 일괄 적용한다.
     * mercenaryId와 category 중 하나만 설정해야 한다.
     * 피스별로 동일한 제한이 이미 존재하면 건너뛴다.
     */
    @Transactional
    public List<ItemRestrictionResponse> applyRestrictionsToSet(Long setId, ItemRestrictionAddRequest req) {
        if ((req.mercenaryId() == null) == (req.category() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "mercenaryId와 category 중 하나만 설정해야 합니다.");
        }
        findOrThrow(setId);

        Mercenary mercenary = null;
        if (req.mercenaryId() != null) {
            mercenary = mercenaryRepository.findById(req.mercenaryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "용병을 찾을 수 없습니다: " + req.mercenaryId()));
        }

        List<EquipmentItem> pieces = equipmentItemRepository.findBySetIdWithItem(setId);
        if (pieces.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "세트에 속한 피스가 없습니다. setId=" + setId);
        }

        final Mercenary finalMercenary = mercenary;
        List<ItemMercenaryRestriction> created = pieces.stream()
                .filter(piece -> !isDuplicate(piece.getItemId(), req))
                .map(piece -> ItemMercenaryRestriction.builder()
                        .item(piece.getItem())
                        .mercenary(finalMercenary)
                        .category(req.category())
                        .build())
                .map(itemMercenaryRestrictionRepository::save)
                .toList();

        return created.stream().map(ItemRestrictionResponse::of).toList();
    }

    private boolean isDuplicate(Long itemId, ItemRestrictionAddRequest req) {
        return itemMercenaryRestrictionRepository.findByItemId(itemId).stream()
                .anyMatch(r -> req.mercenaryId() != null
                        ? r.getMercenary() != null && r.getMercenary().getId().equals(req.mercenaryId())
                        : r.getCategory() == req.category());
    }

    private EquipmentSet findOrThrow(Long id) {
        return equipmentSetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "세트를 찾을 수 없습니다. id=" + id));
    }
}
