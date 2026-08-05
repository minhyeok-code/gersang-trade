package org.example.gersangtrade.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.request.ItemRestrictionAddRequest;
import org.example.gersangtrade.admin.dto.response.ItemRestrictionResponse;
import org.example.gersangtrade.admin.dto.set.AdminSetCreateRequest;
import org.example.gersangtrade.admin.dto.set.AdminSetDetailResponse;
import org.example.gersangtrade.admin.dto.set.AdminSetResponse;
import org.example.gersangtrade.admin.dto.set.AdminSetUpdateRequest;
import org.example.gersangtrade.admin.dto.set.SetEffectAddRequest;
import org.example.gersangtrade.admin.dto.set.SetEffectResponse;
import org.example.gersangtrade.admin.dto.set.SetPieceAddRequest;
import org.example.gersangtrade.admin.dto.set.SetPieceResponse;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetEffectRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetPieceRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.example.gersangtrade.catalog.repository.ItemMercenaryRestrictionRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.EquipmentSetEffect;
import org.example.gersangtrade.domain.catalog.EquipmentSetPiece;
import org.example.gersangtrade.domain.catalog.ItemMercenaryRestriction;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.Element;
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
    private final EquipmentSetPieceRepository equipmentSetPieceRepository;
    private final EquipmentSetEffectRepository equipmentSetEffectRepository;

    // ── 세트 신규 등록 ────────────────────────────────────────────────────────

    /**
     * 장비 세트를 신규 등록한다.
     * 동일 이름이 이미 존재하면 409를 반환한다.
     */
    @Transactional
    public AdminSetResponse createSet(AdminSetCreateRequest req) {
        if (equipmentSetRepository.findByName(req.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이미 동일한 이름의 세트가 존재합니다: " + req.name());
        }
        EquipmentSet set = equipmentSetRepository.save(EquipmentSet.builder()
                .name(req.name())
                .totalPieces(req.totalPieces())
                .isTradeable(req.isTradeable())
                .build());
        return AdminSetResponse.from(set);
    }

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

    // ── 세트 상세 (피스·효과 포함) ──────────────────────────────────────────────

    /** 세트 상세 — 기본 정보 + 피스 목록 + 효과 목록. */
    @Transactional(readOnly = true)
    public AdminSetDetailResponse getSetDetail(Long setId) {
        EquipmentSet set = findOrThrow(setId);
        List<SetPieceResponse> pieces = equipmentSetPieceRepository
                .findWithItemByEquipmentSetId(setId)
                .stream().map(SetPieceResponse::from).toList();
        List<SetEffectResponse> effects = equipmentSetEffectRepository
                .findByEquipmentSetId(setId)
                .stream().map(SetEffectResponse::from).toList();
        return AdminSetDetailResponse.of(set, pieces, effects);
    }

    // ── 피스 ────────────────────────────────────────────────────────────────────

    /** 세트에 피스(슬롯+아이템+개수) 추가. */
    @Transactional
    public SetPieceResponse addPiece(Long setId, SetPieceAddRequest req) {
        EquipmentSet set = findOrThrow(setId);
        EquipmentItem item = equipmentItemRepository.findWithItemByItemId(req.equipmentItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "장비 아이템을 찾을 수 없습니다: " + req.equipmentItemId()));
        EquipmentSetPiece piece = EquipmentSetPiece.builder()
                .equipmentSet(set)
                .slot(req.slot())
                .equipmentItem(item)
                .pieceCount(req.pieceCount() != null ? req.pieceCount() : 1)
                .build();
        equipmentSetPieceRepository.save(piece);
        return SetPieceResponse.from(piece);
    }

    /** 세트 피스 삭제. */
    @Transactional
    public void deletePiece(Long setId, Long pieceId) {
        EquipmentSetPiece piece = equipmentSetPieceRepository.findById(pieceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "피스를 찾을 수 없습니다: " + pieceId));
        if (!piece.getEquipmentSet().getId().equals(setId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 세트의 피스가 아닙니다.");
        }
        equipmentSetPieceRepository.delete(piece);
    }

    // ── 효과 ────────────────────────────────────────────────────────────────────

    /** 세트에 효과(임계 피스수별 스탯 보너스) 추가. */
    @Transactional
    public SetEffectResponse addEffect(Long setId, SetEffectAddRequest req) {
        EquipmentSet set = findOrThrow(setId);
        EquipmentSetEffect effect = EquipmentSetEffect.builder()
                .equipmentSet(set)
                .requiredPieces(req.requiredPieces())
                .statType(req.statType())
                .statValue(req.statValue())
                .statUnit(req.statUnit())
                .element(req.element() != null ? req.element() : Element.NONE)
                .scope(req.scope() != null ? req.scope() : BuffTarget.SELF)
                .build();
        equipmentSetEffectRepository.save(effect);
        return SetEffectResponse.from(effect);
    }

    /** 세트 효과 삭제. */
    @Transactional
    public void deleteEffect(Long setId, Long effectId) {
        EquipmentSetEffect effect = equipmentSetEffectRepository.findById(effectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "효과를 찾을 수 없습니다: " + effectId));
        if (!effect.getEquipmentSet().getId().equals(setId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 세트의 효과가 아닙니다.");
        }
        equipmentSetEffectRepository.delete(effect);
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
