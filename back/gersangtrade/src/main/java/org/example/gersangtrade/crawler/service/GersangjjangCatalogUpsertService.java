package org.example.gersangtrade.crawler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.GemRepository;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.ItemSkillMappingRepository;
import org.example.gersangtrade.catalog.repository.ItemSkillRepository;
import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.catalog.repository.RitualRepository;
import org.example.gersangtrade.crawler.dto.ParsedItemDto;
import org.example.gersangtrade.crawler.parser.GersangjjangParser;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.Gem;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.ItemSkill;
import org.example.gersangtrade.domain.catalog.ItemSkillMapping;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.Enhancement;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.GemGrade;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.example.gersangtrade.domain.catalog.enums.StatUnit;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 거상짱 크롤러 공통 카탈로그 UPSERT 서비스.
 * 일반 아이템·전용장비 크롤러가 Item/EquipmentItem/스탯/스킬 적재 로직을 공유한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GersangjjangCatalogUpsertService {

    private final ItemRepository itemRepository;
    private final ItemStatRepository itemStatRepository;
    private final ItemSkillRepository itemSkillRepository;
    private final ItemSkillMappingRepository itemSkillMappingRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final GemRepository gemRepository;
    private final RitualRepository ritualRepository;

    /** 보석 UPSERT — 이름+등급+주술 조합이 없으면 신규 저장 */
    public void upsertGem(ParsedItemDto dto) {
        Long ritualId = null;
        if (dto.gemGrade() == GemGrade.ENHANCED && dto.ritualName() != null) {
            ritualId = ritualRepository.findByDisplayName(dto.ritualName())
                    .map(Ritual::getId)
                    .orElse(null);
            if (ritualId == null) {
                log.debug("주술 미등록으로 보석 skip: {} - {}", dto.cleanName(), dto.ritualName());
                return;
            }
        }

        Long finalRitualId = ritualId;
        gemRepository.findByNameAndGemGradeAndRitualId(dto.cleanName(), dto.gemGrade(), ritualId)
                .ifPresentOrElse(
                        existing -> log.debug("보석 이미 존재: {} {}", dto.gemGrade(), dto.cleanName()),
                        () -> {
                            Ritual ritual = finalRitualId != null
                                    ? ritualRepository.findById(finalRitualId).orElse(null)
                                    : null;
                            gemRepository.save(Gem.builder()
                                    .name(dto.cleanName())
                                    .gemGrade(dto.gemGrade())
                                    .ritual(ritual)
                                    .build());
                            log.debug("보석 신규 저장: {} {}", dto.gemGrade(), dto.cleanName());
                        }
                );
    }

    /** 재료 아이템 UPSERT */
    public Item upsertMaterialItem(String name) {
        return itemRepository.findByName(name)
                .orElseGet(() -> {
                    Item saved = itemRepository.save(Item.builder()
                            .name(name)
                            .type(ItemType.MATERIAL)
                            .build());
                    log.debug("재료 아이템 신규 저장: {}", name);
                    return saved;
                });
    }

    /** 장비 아이템 UPSERT (강화 단계 미지정) */
    public Item upsertEquipmentItem(String name, EquipmentSlot slot, EquipmentKind kind) {
        return upsertEquipmentItem(name, slot, kind, null);
    }

    /**
     * 장비 아이템 UPSERT.
     * 전설장수 전용장비는 enhancement를 함께 설정한다.
     */
    public Item upsertEquipmentItem(String name, EquipmentSlot slot, EquipmentKind kind,
                                    Enhancement enhancement) {
        return upsertEquipmentItem(name, slot, kind, enhancement, null);
    }

    /**
     * 장비 아이템 UPSERT — 전용장비는 exclusiveMercenary FK를 함께 설정한다.
     *
     * @param exclusiveMercenary 단일 전용 용병 (2명 이상·카테고리 전용이면 null)
     */
    public Item upsertEquipmentItem(String name, EquipmentSlot slot, EquipmentKind kind,
                                    Enhancement enhancement, Mercenary exclusiveMercenary) {
        Item item = itemRepository.findByName(name)
                .orElseGet(() -> {
                    Item saved = itemRepository.save(Item.builder()
                            .name(name)
                            .type(ItemType.EQUIPMENT)
                            .build());
                    log.debug("장비 아이템 신규 저장: {} (slot={}, kind={})", name, slot, kind);
                    return saved;
                });

        if (item.getType() != ItemType.EQUIPMENT) {
            item.updateType(ItemType.EQUIPMENT);
        }

        EquipSlot equipSlot = deriveEquipSlot(slot, kind);
        Optional<EquipmentItem> existing = equipmentItemRepository.findById(item.getId());
        if (existing.isEmpty()) {
            equipmentItemRepository.save(EquipmentItem.builder()
                    .item(item)
                    .equipmentKind(kind)
                    .slot(slot)
                    .equipSlot(equipSlot)
                    .mercenary(exclusiveMercenary)
                    .enhancement(enhancement)
                    .build());
        } else {
            EquipmentItem ei = existing.get();
            // 기존 행(다른 크롤러·수동 입력)의 슬롯이 틀려도 전용장비 크롤러 값으로 덮어쓴다
            ei.updateSlotInfo(slot, equipSlot);
            ei.updateExclusiveMercenaryIfAbsent(exclusiveMercenary);
            if (enhancement != null && ei.getEnhancement() == null) {
                ei.updateInfo(slot, kind, ei.isRitualApplicable(), ei.isHasSlotOption(),
                        ei.getEquipmentSet(), equipSlot, ei.getMercenary(), enhancement, ei.isSainSword());
            }
        }
        return item;
    }

    /** 파싱된 스탯 전체 UPSERT — 슬롯 미지정 시 전부 SELF */
    public void upsertAllStats(Item item, List<GersangjjangParser.ParsedStat> parsedStats) {
        upsertAllStats(item, null, parsedStats);
    }

    /**
     * 파싱된 스탯 전체 UPSERT.
     *
     * @param slot 장비 슬롯 — 명왕부(TALISMAN) 속성값 scope 판별에 사용. null이면 SELF만 적용.
     */
    public void upsertAllStats(Item item, EquipmentSlot slot,
                               List<GersangjjangParser.ParsedStat> parsedStats) {
        for (GersangjjangParser.ParsedStat ps : parsedStats) {
            StatUnit unit = ps.statUnit() != null ? ps.statUnit() : StatUnit.FLAT;
            BuffTarget scope = ItemStatScopeResolver.resolve(
                    item.getName(), slot, ps.statType(), unit);
            if (!itemStatRepository.existsByItemIdAndStatTypeAndElement(
                    item.getId(), ps.statType(), ps.element(), scope)) {
                itemStatRepository.save(ItemStat.builder()
                        .item(item)
                        .statType(ps.statType())
                        .element(ps.element())
                        .value(ps.value())
                        .statUnit(ps.statUnit() != null ? ps.statUnit() : StatUnit.FLAT)
                        .scope(scope)
                        .build());
                log.debug("스탯 저장: {} {} {} scope={} → {}",
                        item.getName(), ps.element(), ps.statType(), scope, ps.value());
            }
        }
    }

    /** 파싱된 스킬명 전체 UPSERT */
    public void upsertSkills(Item item, List<String> skills) {
        for (String skillName : skills) {
            ItemSkill skill = itemSkillRepository.findBySkillName(skillName)
                    .orElseGet(() -> itemSkillRepository.save(
                            ItemSkill.builder().skillName(skillName).build()));
            if (!itemSkillMappingRepository.existsByItemIdAndSkillId(item.getId(), skill.getId())) {
                itemSkillMappingRepository.save(new ItemSkillMapping(item, skill));
                log.debug("스킬 매핑 저장: {} → {}", item.getName(), skillName);
            }
        }
    }

    /** EquipmentSlot + EquipmentKind → 덱 슬롯(EquipSlot) */
    public static EquipSlot deriveEquipSlot(EquipmentSlot slot, EquipmentKind kind) {
        if (kind == EquipmentKind.APPEARANCE) {
            return switch (slot) {
                case ACCESSORY -> EquipSlot.APP_SPIRIT;
                case DIVINE    -> EquipSlot.APP_WAR_GOD;
                case BRACELET  -> EquipSlot.APP_BRACELET;
                case LEGGING   -> EquipSlot.APP_GREAVES;
                case EARRING   -> EquipSlot.APP_EARRING;
                case NECKLACE  -> EquipSlot.APP_NECKLACE;
                case HELMET    -> EquipSlot.APP_HELMET;
                case ARMOR     -> EquipSlot.APP_ARMOR;
                case WEAPON    -> EquipSlot.APP_WEAPON;
                default        -> null;
            };
        }
        return switch (slot) {
            case WEAPON   -> EquipSlot.WEAPON;
            case HELMET   -> EquipSlot.HELMET;
            case ARMOR    -> EquipSlot.ARMOR;
            case GLOVES   -> EquipSlot.GLOVES;
            case BELT     -> EquipSlot.BELT;
            case SHOES    -> EquipSlot.SHOES;
            case TALISMAN -> EquipSlot.CHARM;
            default       -> null;
        };
    }
}
