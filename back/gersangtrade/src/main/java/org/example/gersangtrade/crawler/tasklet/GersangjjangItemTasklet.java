package org.example.gersangtrade.crawler.tasklet;

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
import org.example.gersangtrade.crawler.parser.GersangjjangParser.CategoryInfo;
import org.example.gersangtrade.crawler.parser.GersangjjangParser.ItemRow;
import org.example.gersangtrade.crawler.parser.GersangjjangSetParser;
import org.example.gersangtrade.crawler.parser.ItemNameParser;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.Gem;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.ItemSkill;
import org.example.gersangtrade.domain.catalog.ItemSkillMapping;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;

import org.example.gersangtrade.domain.catalog.enums.GemGrade;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.jsoup.nodes.Document;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Job 1 - Step 1: 거상짱 아이템 목록 수집 및 UPSERT Tasklet.
 *
 * <p>URL: https://www.gersangjjang.com/item/index.asp
 * 거상짱은 SSR ASP 사이트로 Jsoup 정적 파싱이 정상 동작한다.
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>인덱스 페이지에서 카테고리 정보(URL + 슬롯) 수집</li>
 *   <li>카테고리별 페이지 파싱 → 아이템명 추출</li>
 *   <li>보석 이름 패턴(11종) → gems 테이블 UPSERT</li>
 *   <li>슬롯이 결정된 장비 카테고리 → items(EQUIPMENT) + equipment_items UPSERT</li>
 *   <li>슬롯 미결정 카테고리 → items(MATERIAL) UPSERT</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GersangjjangItemTasklet implements Tasklet {

    private static final String INDEX_URL = "https://www.gersangjjang.com/item/index.asp";

    private final JsoupFetcher jsoupFetcher;
    private final ItemRepository itemRepository;
    private final ItemStatRepository itemStatRepository;
    private final ItemSkillRepository itemSkillRepository;
    private final ItemSkillMappingRepository itemSkillMappingRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final GemRepository gemRepository;
    private final RitualRepository ritualRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {

        log.info("=== GersangjjangItemTasklet 시작: 거상짱 아이템 목록 수집 ===");

        // 인덱스 페이지에서 카테고리 링크 목록 수집
        Document indexDoc = jsoupFetcher.fetch(INDEX_URL);
        List<CategoryInfo> categories = GersangjjangParser.parseCategoryLinks(indexDoc);
        log.info("카테고리 {}개 수집 시작", categories.size());

        Set<String> processedNames = new HashSet<>();
        int gemCount = 0, equipmentCount = 0, materialCount = 0, skipCount = 0;

        for (CategoryInfo category : categories) {
            try {
                Document categoryDoc = jsoupFetcher.fetch(category.url());
                List<ItemRow> rows = GersangjjangParser.parseItemRows(categoryDoc);

                if (rows.isEmpty()) {
                    String bodySnippet = categoryDoc.body() != null
                            ? categoryDoc.body().html().replaceAll("\\s+", " ").substring(
                                    0, Math.min(300, categoryDoc.body().html().length()))
                            : "(body 없음)";
                    log.warn("카테고리 아이템 0개 (HTML 구조 확인 필요): {}\n  HTML snippet: {}",
                            category.url(), bodySnippet);
                }

                for (ItemRow row : rows) {
                    if (!processedNames.add(row.name())) continue;

                    ParsedItemDto parsed = ItemNameParser.parse(row.name()).orElse(null);
                    if (parsed == null) {
                        skipCount++;
                        continue;
                    }

                    if (parsed.type() == ParsedItemDto.ParsedType.GEM) {
                        upsertGem(parsed);
                        gemCount++;
                    } else if (category.isMixed()) {
                        // 슬롯 혼재 페이지 — suffix로 슬롯 감지
                        EquipmentSlot detectedSlot = GersangjjangSetParser
                                .detectSlotByName(parsed.cleanName()).orElse(null);
                        if (detectedSlot != null) {
                            Item item = upsertEquipmentItem(parsed.cleanName(), detectedSlot, category.kind());
                            upsertAllStats(item, row.stats());
                            upsertSkills(item, row.skills());
                            equipmentCount++;
                        } else {
                            log.warn("슬롯 감지 실패 — MATERIAL로 저장: [{}] ({})", parsed.cleanName(), category.text());
                            Item item = upsertMaterialItem(parsed.cleanName());
                            upsertAllStats(item, row.stats());
                            upsertSkills(item, row.skills());
                            materialCount++;
                        }
                    } else if (category.isEquipment()) {
                        Item item = upsertEquipmentItem(parsed.cleanName(), category.slot(), category.kind());
                        upsertAllStats(item, row.stats());
                        upsertSkills(item, row.skills());
                        equipmentCount++;
                    } else {
                        Item item = upsertMaterialItem(parsed.cleanName());
                        upsertAllStats(item, row.stats());
                        upsertSkills(item, row.skills());
                        materialCount++;
                    }
                }

                log.debug("카테고리 완료: [{}] {} → {}개",
                        category.text(), category.url(), rows.size());

            } catch (Exception e) {
                log.warn("카테고리 파싱 실패 (skip): {} — {}", category.url(), e.getMessage());
            }
        }

        log.info("=== GersangjjangItemTasklet 완료: 보석 {}개, 장비 {}개, 재료 {}개, skip {}개 ===",
                gemCount, equipmentCount, materialCount, skipCount);
        return RepeatStatus.FINISHED;
    }

    /** 보석 UPSERT — 이름+등급+주술 조합이 없으면 신규 저장 */
    private void upsertGem(ParsedItemDto dto) {
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

    /** 재료 아이템 UPSERT — MATERIAL 타입으로 저장 */
    private Item upsertMaterialItem(String name) {
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

    /**
     * 장비 아이템 UPSERT — EQUIPMENT 타입으로 저장하고 EquipmentItem도 생성.
     * 이미 MATERIAL 타입으로 저장된 경우 EQUIPMENT로 수정한다.
     * 기존 레코드의 slot/equipSlot이 null이면 갱신한다.
     */
    private Item upsertEquipmentItem(String name, EquipmentSlot slot, EquipmentKind kind) {
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
                    .build());
        } else {
            EquipmentItem ei = existing.get();
            if (ei.getSlot() == null || ei.getEquipSlot() == null) {
                ei.updateSlotInfo(slot, equipSlot);
            }
        }
        return item;
    }

    /**
     * EquipmentSlot + EquipmentKind 조합으로 덱 슬롯(EquipSlot)을 결정한다.
     * RING은 RING_1/RING_2 둘 다 가능하므로 null 반환.
     * ORB/WING/TITLE에 대응하는 덱 슬롯이 없으므로 null 반환.
     */
    private static EquipSlot deriveEquipSlot(EquipmentSlot slot, EquipmentKind kind) {
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
                default        -> null; // ORB, WING, TITLE — 덱 슬롯 없음
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
            default       -> null; // RING — RING_1/RING_2 모두 가능
        };
    }

    /** 파싱된 스탯 전체 UPSERT — (statType, element, scope) 조합이 이미 존재하면 건너뜀 */
    private void upsertAllStats(Item item, List<GersangjjangParser.ParsedStat> parsedStats) {
        for (GersangjjangParser.ParsedStat ps : parsedStats) {
            if (!itemStatRepository.existsByItemIdAndStatTypeAndElement(
                    item.getId(), ps.statType(), ps.element(),
                    org.example.gersangtrade.domain.catalog.enums.BuffTarget.SELF)) {
                itemStatRepository.save(ItemStat.builder()
                        .item(item)
                        .statType(ps.statType())
                        .element(ps.element())
                        .value(ps.value())
                        .statUnit(ps.statUnit())
                        .scope(org.example.gersangtrade.domain.catalog.enums.BuffTarget.SELF)
                        .build());
                log.debug("스탯 저장: {} {} {} → {}", item.getName(), ps.element(), ps.statType(), ps.value());
            }
        }
    }

    /** 파싱된 스킬명 전체 UPSERT — 스킬은 전역 공유, 아이템-스킬 매핑만 추가 */
    private void upsertSkills(Item item, List<String> skills) {
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
}
