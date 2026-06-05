package org.example.gersangtrade.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.request.EquipmentDetailUpdateRequest;
import org.example.gersangtrade.admin.dto.request.ItemStatReplaceRequest;
import org.example.gersangtrade.admin.dto.request.ItemUpdateRequest;
import org.example.gersangtrade.admin.dto.request.SkillEffectReplaceRequest;
import org.example.gersangtrade.admin.dto.request.SkillReplaceRequest;
import org.example.gersangtrade.admin.dto.response.ItemAdminResponse;
import org.example.gersangtrade.admin.dto.response.ItemDetailAdminResponse;
import org.example.gersangtrade.admin.dto.request.ItemRestrictionAddRequest;
import org.example.gersangtrade.admin.dto.response.ItemRestrictionResponse;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetPieceRepository;
import org.example.gersangtrade.catalog.repository.ItemMercenaryRestrictionRepository;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.ItemSkillEffectRepository;
import org.example.gersangtrade.catalog.repository.ItemSkillMappingRepository;
import org.example.gersangtrade.catalog.repository.ItemSkillRepository;
import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.catalog.repository.MaterialItemRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.RitualApplicabilityRepository;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.ItemMercenaryRestriction;
import org.example.gersangtrade.domain.catalog.ItemSkill;
import org.example.gersangtrade.domain.catalog.ItemSkillEffect;
import org.example.gersangtrade.domain.catalog.ItemSkillMapping;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.example.gersangtrade.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
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
    private final ItemSkillMappingRepository itemSkillMappingRepository;
    private final ItemSkillEffectRepository itemSkillEffectRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final EquipmentSetRepository equipmentSetRepository;
    private final MaterialItemRepository materialItemRepository;
    private final RitualApplicabilityRepository ritualApplicabilityRepository;
    private final EquipmentSetPieceRepository equipmentSetPieceRepository;
    private final ItemMercenaryRestrictionRepository itemMercenaryRestrictionRepository;
    private final MercenaryRepository mercenaryRepository;

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
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.EQUIPMENT_SLOT, allEntries = true),
            @CacheEvict(value = CacheConfig.RITUALS_BY_ITEM, allEntries = true)
    })
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
        eq.updateInfo(req.slot(), req.equipmentKind(), req.ritualApplicable(), req.hasSlotOption(), set, req.equipSlot(), null, req.sainSword());
        return buildDetail(itemId);
    }

    // ── 아이템 상세 조회 ─────────────────────────────────────────────────────────

    /**
     * 아이템 단건 상세 조회 — 기본정보 + 장비정보(type=EQUIPMENT시) + 스탯 + 스킬 반환.
     */
    @Transactional(readOnly = true)
    public ItemDetailAdminResponse getItem(Long itemId) {
        return buildDetail(itemId);
    }

    // ── 아이템 기본정보 수정 ─────────────────────────────────────────────────────

    /**
     * 아이템 이름·타입·거래 카테고리를 수정한다.
     * name은 null/공백이면 기존 값을 유지한다 (엔티티 updateInfo 동일 정책).
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.EQUIPMENT_SLOT, allEntries = true),
            @CacheEvict(value = CacheConfig.RITUALS_BY_ITEM, allEntries = true)
    })
    public ItemDetailAdminResponse updateInfo(Long itemId, ItemUpdateRequest req) {
        Item item = getItemOrThrow(itemId);
        item.updateInfo(req.name(), req.type(), req.tradeCategory());
        return buildDetail(itemId);
    }

    // ── 아이템 스탯 전체 교체 ───────────────────────────────────────────────────

    /**
     * 아이템 스탯을 PUT 의미론으로 교체한다.
     * 기존 스탯을 전부 삭제하고 요청 목록으로 재적재한다.
     * element가 null이면 NONE으로 처리된다 (ItemStat 빌더 동일 정책).
     */
    @Transactional
    @CacheEvict(value = CacheConfig.EQUIPMENT_SLOT, allEntries = true)
    public ItemDetailAdminResponse replaceStats(Long itemId, ItemStatReplaceRequest req) {
        Item item = getItemOrThrow(itemId);
        itemStatRepository.deleteByItemId(itemId);
        req.stats().stream()
                .map(e -> itemStatRepository.save(ItemStat.builder()
                        .item(item)
                        .statType(e.statType())
                        .element(e.element() != null ? e.element() : Element.NONE)
                        .value(e.value())
                        .build()))
                .toList();
        return buildDetail(itemId);
    }

    // ── 아이템 스킬 전체 교체 ───────────────────────────────────────────────────

    /**
     * 아이템 스킬 목록을 PUT 의미론으로 교체한다.
     * 기존 스킬을 전부 삭제하고 요청 목록으로 재적재한다.
     */
    @Transactional
    @CacheEvict(value = CacheConfig.EQUIPMENT_SLOT, allEntries = true)
    public ItemDetailAdminResponse replaceSkills(Long itemId, SkillReplaceRequest req) {
        Item item = getItemOrThrow(itemId);
        // 기존 매핑만 삭제 — 공유 스킬 행(ItemSkill) 자체는 삭제하지 않음
        itemSkillMappingRepository.deleteByItemId(itemId);
        req.skills().stream()
                .filter(s -> s != null && !s.isBlank())
                .forEach(s -> {
                    String name = s.trim();
                    ItemSkill skill = itemSkillRepository.findBySkillName(name)
                            .orElseGet(() -> itemSkillRepository.save(
                                    ItemSkill.builder().skillName(name).build()));
                    if (!itemSkillMappingRepository.existsByItemIdAndSkillId(item.getId(), skill.getId())) {
                        itemSkillMappingRepository.save(new ItemSkillMapping(item, skill));
                    }
                });
        return buildDetail(itemId);
    }

    // ── 아이템 하드 삭제 ────────────────────────────────────────────────────────

    /**
     * 아이템과 관리자 입력 하위 데이터를 실제 삭제한다.
     * 거래·덱·시세 등 다른 도메인에서 참조 중이면 DB 제약으로 삭제를 거부한다.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.EQUIPMENT_SLOT, allEntries = true),
            @CacheEvict(value = CacheConfig.RITUALS_BY_ITEM, allEntries = true)
    })
    public void deleteItem(Long itemId) {
        Item item = getItemOrThrow(itemId);
        try {
            itemStatRepository.deleteByItemId(itemId);
            // 매핑만 삭제 — 공유 스킬 행(ItemSkill) 자체는 삭제하지 않음
            itemSkillMappingRepository.deleteByItemId(itemId);

            equipmentItemRepository.findById(itemId).ifPresent(equipmentItem -> {
                ritualApplicabilityRepository.deleteByEquipmentItem_ItemId(itemId);
                equipmentSetPieceRepository.deleteByEquipmentItem_ItemId(itemId);
                equipmentItemRepository.delete(equipmentItem);
            });
            materialItemRepository.findById(itemId).ifPresent(materialItemRepository::delete);

            itemRepository.delete(item);
            itemRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "다른 데이터에서 참조 중인 아이템은 삭제할 수 없습니다.", e);
        }
    }

    // ── 아이템 착용 제한 관리 ──────────────────────────────────────────────────────

    /** 아이템의 착용 제한 목록 조회. */
    @Transactional(readOnly = true)
    public List<ItemRestrictionResponse> getRestrictions(Long itemId) {
        getItemOrThrow(itemId);
        return itemMercenaryRestrictionRepository.findByItemId(itemId).stream()
                .map(ItemRestrictionResponse::of)
                .toList();
    }

    /**
     * 착용 제한 추가.
     * mercenaryId와 category 중 하나만 설정해야 한다.
     */
    @Transactional
    public ItemRestrictionResponse addRestriction(Long itemId, ItemRestrictionAddRequest req) {
        if ((req.mercenaryId() == null) == (req.category() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "mercenaryId와 category 중 하나만 설정해야 합니다.");
        }

        Item item = getItemOrThrow(itemId);
        Mercenary mercenary = null;

        if (req.mercenaryId() != null) {
            mercenary = mercenaryRepository.findById(req.mercenaryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "용병을 찾을 수 없습니다: " + req.mercenaryId()));
        }

        ItemMercenaryRestriction restriction = ItemMercenaryRestriction.builder()
                .item(item)
                .mercenary(mercenary)
                .category(req.category())
                .build();

        return ItemRestrictionResponse.of(itemMercenaryRestrictionRepository.save(restriction));
    }

    /** 착용 제한 삭제. */
    @Transactional
    public void deleteRestriction(Long itemId, Long restrictionId) {
        ItemMercenaryRestriction restriction = itemMercenaryRestrictionRepository.findById(restrictionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "착용 제한을 찾을 수 없습니다: " + restrictionId));
        if (!restriction.getItem().getId().equals(itemId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "해당 아이템의 착용 제한이 아닙니다.");
        }
        itemMercenaryRestrictionRepository.delete(restriction);
    }

    // ── 스킬 효과 전체 교체 ──────────────────────────────────────────────────────

    /**
     * 스킬 효과를 PUT 의미론으로 교체한다.
     * 기존 효과를 전부 삭제하고 요청 목록으로 재적재한다.
     */
    @Transactional
    public ItemDetailAdminResponse.SkillEntry replaceSkillEffects(Long itemId, Long skillId, SkillEffectReplaceRequest req) {
        ItemSkill skill = itemSkillRepository.findById(skillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "스킬을 찾을 수 없습니다: " + skillId));
        // 해당 아이템에 이 스킬이 매핑되어 있는지 검증
        if (!itemSkillMappingRepository.existsByItemIdAndSkillId(itemId, skillId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "해당 아이템의 스킬이 아닙니다.");
        }

        itemSkillEffectRepository.deleteBySkillId(skillId);
        List<ItemSkillEffect> saved = req.effects().stream()
                .map(e -> itemSkillEffectRepository.save(ItemSkillEffect.builder()
                        .skill(skill)
                        .statKey(e.statKey())
                        .statValue(e.statValue())
                        .valueType(e.valueType())
                        .build()))
                .toList();

        List<ItemDetailAdminResponse.EffectEntry> effects = saved.stream()
                .map(e -> new ItemDetailAdminResponse.EffectEntry(e.getStatKey(), e.getStatValue(), e.getValueType()))
                .toList();
        return new ItemDetailAdminResponse.SkillEntry(skill.getId(), skill.getSkillName(), effects);
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────────────

    private Item getItemOrThrow(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "아이템을 찾을 수 없습니다: " + id));
    }

    /** 아이템 ID로 상세 응답을 조립한다. 스킬 효과를 배치 로드해 N+1을 방지한다. */
    private ItemDetailAdminResponse buildDetail(Long itemId) {
        Item item = getItemOrThrow(itemId);
        EquipmentItem equipmentItem = equipmentItemRepository.findWithItemByItemId(itemId).orElse(null);
        List<ItemStat> stats = itemStatRepository.findByItemId(itemId);
        // 매핑 테이블을 통해 이 아이템에 연결된 스킬 목록 조회
        List<ItemSkill> skills = itemSkillMappingRepository.findByItemId(itemId).stream()
                .map(ItemSkillMapping::getSkill)
                .toList();
        List<Long> skillIds = skills.stream().map(ItemSkill::getId).toList();
        Map<Long, List<ItemSkillEffect>> effectsBySkillId = itemSkillEffectRepository.findBySkillIdIn(skillIds)
                .stream().collect(Collectors.groupingBy(e -> e.getSkill().getId()));
        return ItemDetailAdminResponse.of(item, equipmentItem, stats, skills, effectsBySkillId);
    }
}
