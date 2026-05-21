package org.example.gersangtrade.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.request.EquipmentDetailUpdateRequest;
import org.example.gersangtrade.admin.dto.request.ItemStatReplaceRequest;
import org.example.gersangtrade.admin.dto.request.ItemUpdateRequest;
import org.example.gersangtrade.admin.dto.request.SkillReplaceRequest;
import org.example.gersangtrade.admin.dto.response.ItemAdminResponse;
import org.example.gersangtrade.admin.dto.response.ItemDetailAdminResponse;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.ItemSkillRepository;
import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.ItemSkill;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemAdminService {

    private final ItemRepository itemRepository;
    private final ItemStatRepository itemStatRepository;
    private final ItemSkillRepository itemSkillRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final EquipmentSetRepository equipmentSetRepository;

    // ── 아이템 목록 조회 ─────────────────────────────────────────────────────────

    /**
     * 아이템 목록을 페이지 단위로 조회한다.
     * type, name 필터 적용 가능. 각 아이템의 스탯 등록 수를 포함해 관리자가 미입력 항목을 확인할 수 있도록 한다.
     *
     * @param type null이면 전체 타입 조회
     * @param name null이면 이름 필터 없음
     */
    @Transactional(readOnly = true)
    public Page<ItemAdminResponse> listItems(ItemType type, String name, Pageable pageable) {
        Page<Item> page = (type != null || (name != null && !name.isBlank()))
                ? itemRepository.findByTypeAndNameContaining(type, name, pageable)
                : itemRepository.findAll(pageable);

        // 스탯 수 일괄 조회 (N+1 방지)
        List<Long> itemIds = page.getContent().stream().map(Item::getId).toList();
        Map<Long, Long> statCounts = itemStatRepository.findByItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(s -> s.getItem().getId(), Collectors.counting()));

        return page.map(item -> ItemAdminResponse.of(item,
                statCounts.getOrDefault(item.getId(), 0L).intValue()));
    }

    /**
     * 장비 상세 수정 — slot, kind, setId, ritualApplicable, hasSlotOption.
     * MATERIAL 아이템에 호출하면 400 반환.
     */
    @Transactional
    public ItemDetailAdminResponse updateEquipmentDetail(Long itemId, EquipmentDetailUpdateRequest req) {
        Item item = getItemOrThrow(itemId);
        if (item.getType() != ItemType.EQUIPMENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "EQUIPMENT 타입 아이템에만 장비 상세 수정이 가능합니다.");
        }
        EquipmentItem eq = equipmentItemRepository.findWithItemByItemId(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "장비 상세 정보가 없습니다. itemId=" + itemId));

        EquipmentSet set = null;
        if (req.setId() != null) {
            set = equipmentSetRepository.findById(req.setId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "존재하지 않는 세트 ID입니다: " + req.setId()));
        }
        eq.updateInfo(req.slot(), req.equipmentKind(), req.ritualApplicable(), req.hasSlotOption(), set, req.equipSlot());

        List<ItemStat> stats = itemStatRepository.findByItemId(itemId);
        List<ItemSkill> skills = itemSkillRepository.findByItemId(itemId);
        return ItemDetailAdminResponse.of(item, eq, stats, skills);
    }

    // ── 아이템 상세 조회 ─────────────────────────────────────────────────────────

    /**
     * 아이템 단건 상세 조회 — 기본정보 + 장비정보(type=EQUIPMENT시) + 스탯 + 스킬 반환.
     */
    @Transactional(readOnly = true)
    public ItemDetailAdminResponse getItem(Long itemId) {
        Item item = getItemOrThrow(itemId);
        EquipmentItem equipmentItem = equipmentItemRepository.findWithItemByItemId(itemId).orElse(null);
        List<ItemStat> stats = itemStatRepository.findByItemId(itemId);
        List<ItemSkill> skills = itemSkillRepository.findByItemId(itemId);
        return ItemDetailAdminResponse.of(item, equipmentItem, stats, skills);
    }

    // ── 아이템 기본정보 수정 ─────────────────────────────────────────────────────

    /**
     * 아이템 이름·타입·거래 카테고리를 수정한다.
     * name은 null/공백이면 기존 값을 유지한다 (엔티티 updateInfo 동일 정책).
     */
    @Transactional
    public ItemDetailAdminResponse updateInfo(Long itemId, ItemUpdateRequest req) {
        Item item = getItemOrThrow(itemId);
        item.updateInfo(req.name(), req.type(), req.tradeCategory());
        EquipmentItem equipmentItem = equipmentItemRepository.findWithItemByItemId(itemId).orElse(null);
        List<ItemStat> stats = itemStatRepository.findByItemId(itemId);
        List<ItemSkill> skills = itemSkillRepository.findByItemId(itemId);
        return ItemDetailAdminResponse.of(item, equipmentItem, stats, skills);
    }

    // ── 아이템 스탯 전체 교체 ───────────────────────────────────────────────────

    /**
     * 아이템 스탯을 PUT 의미론으로 교체한다.
     * 기존 스탯을 전부 삭제하고 요청 목록으로 재적재한다.
     * element가 null이면 NONE으로 처리된다 (ItemStat 빌더 동일 정책).
     */
    @Transactional
    public ItemDetailAdminResponse replaceStats(Long itemId, ItemStatReplaceRequest req) {
        Item item = getItemOrThrow(itemId);
        itemStatRepository.deleteByItemId(itemId);
        List<ItemStat> saved = req.stats().stream()
                .map(e -> itemStatRepository.save(ItemStat.builder()
                        .item(item)
                        .statType(e.statType())
                        .element(e.element() != null ? e.element() : Element.NONE)
                        .value(e.value())
                        .build()))
                .toList();
        EquipmentItem equipmentItem = equipmentItemRepository.findWithItemByItemId(itemId).orElse(null);
        List<ItemSkill> skills = itemSkillRepository.findByItemId(itemId);
        return ItemDetailAdminResponse.of(item, equipmentItem, saved, skills);
    }

    // ── 아이템 스킬 전체 교체 ───────────────────────────────────────────────────

    /**
     * 아이템 스킬 목록을 PUT 의미론으로 교체한다.
     * 기존 스킬을 전부 삭제하고 요청 목록으로 재적재한다.
     */
    @Transactional
    public ItemDetailAdminResponse replaceSkills(Long itemId, SkillReplaceRequest req) {
        Item item = getItemOrThrow(itemId);
        itemSkillRepository.deleteByItemId(itemId);
        List<ItemSkill> saved = req.skills().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> itemSkillRepository.save(ItemSkill.builder()
                        .item(item)
                        .skillName(s.trim())
                        .build()))
                .toList();
        EquipmentItem equipmentItem = equipmentItemRepository.findWithItemByItemId(itemId).orElse(null);
        List<ItemStat> stats = itemStatRepository.findByItemId(itemId);
        return ItemDetailAdminResponse.of(item, equipmentItem, stats, saved);
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────────────

    private Item getItemOrThrow(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "아이템을 찾을 수 없습니다: " + id));
    }
}
