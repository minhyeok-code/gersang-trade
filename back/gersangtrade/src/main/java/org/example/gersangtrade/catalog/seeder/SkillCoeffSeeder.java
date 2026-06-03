package org.example.gersangtrade.catalog.seeder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.ItemSkillMappingRepository;
import org.example.gersangtrade.catalog.repository.ItemSkillRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.MercenarySkillRepository;
import org.example.gersangtrade.catalog.repository.SetGrantedSkillRepository;
import org.example.gersangtrade.catalog.repository.SkillCoefficientRepository;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.ItemSkill;
import org.example.gersangtrade.domain.catalog.ItemSkillMapping;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenarySkill;
import org.example.gersangtrade.domain.catalog.SetGrantedSkill;
import org.example.gersangtrade.domain.catalog.SkillCoefficient;
import org.example.gersangtrade.domain.catalog.enums.SkillType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 스킬 계수 초기 데이터 시딩.
 * skill-coeff-entity.json을 읽어 skill_coefficients 테이블에 UPSERT한다.
 *
 * <p>전제 조건: Mercenary.key, Item.itemKey가 DB에 채워져 있어야 한다.
 * 해당 키가 없는 항목은 경고 로그와 함께 스킵된다.
 *
 * <p>실행 순서: 다른 용병/아이템 시더가 먼저 실행되어야 하므로 LOWEST_PRECEDENCE로 설정.
 */
@Slf4j
@Component
@Order(Integer.MAX_VALUE)
@RequiredArgsConstructor
public class SkillCoeffSeeder implements ApplicationRunner {

    private final MercenaryRepository mercenaryRepository;
    private final ItemRepository itemRepository;
    private final MercenarySkillRepository mercenarySkillRepository;
    private final ItemSkillRepository itemSkillRepository;
    private final ItemSkillMappingRepository itemSkillMappingRepository;
    private final SetGrantedSkillRepository setGrantedSkillRepository;
    private final SkillCoefficientRepository skillCoefficientRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // row_id 기준 upsert이므로 전역 skip 없이 매 기동 시 실행한다
        Resource resource = resourceLoader.getResource("classpath:skill-coeff-entity.json");
        List<SkillCoeffEntry> entries = objectMapper.readValue(
                resource.getInputStream(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, SkillCoeffEntry.class)
        );

        log.info("스킬 계수 시딩 시작: {}건", entries.size());
        int saved = 0, skipped = 0;

        for (SkillCoeffEntry e : entries) {
            try {
                if (processEntry(e)) saved++;
                else skipped++;
            } catch (Exception ex) {
                log.warn("스킬 계수 시딩 오류 [rowId={}]: {}", e.rowId, ex.getMessage());
                skipped++;
            }
        }

        log.info("스킬 계수 시딩 완료: {}건 저장, {}건 스킵", saved, skipped);
    }

    private boolean processEntry(SkillCoeffEntry e) {
        if ("mercenary".equals(e.type)) {
            return processMercenaryEntry(e);
        } else if ("item".equals(e.type)) {
            return processItemEntry(e);
        } else if ("set_granted".equals(e.type)) {
            return processSetGrantedEntry(e);
        }
        log.warn("알 수 없는 타입 스킵 [rowId={}, type={}]", e.rowId, e.type);
        return false;
    }

    // ── 용병 스킬 계수 ────────────────────────────────────────────────────────

    private boolean processMercenaryEntry(SkillCoeffEntry e) {
        Mercenary mercenary = mercenaryRepository.findByKey(e.mercenaryKey).orElse(null);
        if (mercenary == null) {
            log.warn("스킬 계수 스킵 — 용병 키 미등록 [key={}, skill={}]", e.mercenaryKey, e.skillName);
            return false;
        }
        MercenarySkill skill = findOrCreateMercenarySkill(mercenary, e.skillName, e.skillKey);
        upsertCoeff(e, skill, null);
        return true;
    }

    private MercenarySkill findOrCreateMercenarySkill(Mercenary mercenary, String skillName, String skillKey) {
        return mercenarySkillRepository
                .findByMercenaryIdAndSkillName(mercenary.getId(), skillName)
                .orElseGet(() -> mercenarySkillRepository.save(
                        MercenarySkill.builder()
                                .mercenary(mercenary)
                                .skillName(skillName)
                                .skillKey(skillKey)
                                .build()
                ));
    }

    // ── 세트 부여 스킬 계수 ───────────────────────────────────────────────────

    private boolean processSetGrantedEntry(SkillCoeffEntry e) {
        SetGrantedSkill skill = setGrantedSkillRepository.findBySkillKey(e.skillKey).orElse(null);
        if (skill == null) {
            log.warn("스킬 계수 스킵 — 세트 부여 스킬 미등록 [skillKey={}, skill={}]", e.skillKey, e.skillName);
            return false;
        }
        upsertSetGrantedCoeff(e, skill);
        return true;
    }

    private void upsertSetGrantedCoeff(SkillCoeffEntry e, SetGrantedSkill skill) {
        SkillType skillType = SkillType.valueOf(e.skillType);
        skillCoefficientRepository.findByRowId(e.rowId).ifPresentOrElse(
                existing -> existing.updateCoefficients(
                        e.coefStr, e.coefDex, e.coefVit, e.coefInt,
                        e.coefAtk, e.coefLvl, e.hitCount, e.damageRangeFactor,
                        skillType, e.castsPerSecond, e.tickIntervalMs, e.confidence, e.note
                ),
                () -> skillCoefficientRepository.save(
                        SkillCoefficient.ofSetGrantedSkill()
                                .setGrantedSkill(skill)
                                .rowId(e.rowId)
                                .coefStr(e.coefStr).coefDex(e.coefDex)
                                .coefVit(e.coefVit).coefInt(e.coefInt)
                                .coefAtk(e.coefAtk).coefLvl(e.coefLvl)
                                .hitCount(e.hitCount)
                                .damageRangeFactor(e.damageRangeFactor)
                                .skillType(skillType)
                                .castsPerSecond(e.castsPerSecond)
                                .tickIntervalMs(e.tickIntervalMs)
                                .confidence(e.confidence)
                                .measurementNote(e.note)
                                .build()
                )
        );
    }

    // ── 아이템 스킬 계수 ──────────────────────────────────────────────────────

    private boolean processItemEntry(SkillCoeffEntry e) {
        Item item = itemRepository.findByItemKey(e.itemKey).orElse(null);
        if (item == null) {
            log.warn("스킬 계수 스킵 — 아이템 키 미등록 [key={}, skill={}]", e.itemKey, e.skillName);
            return false;
        }
        ItemSkill skill = findOrCreateItemSkill(item, e.skillName, e.skillKey);
        upsertCoeff(e, null, skill);
        return true;
    }

    private ItemSkill findOrCreateItemSkill(Item item, String skillName, String skillKey) {
        ItemSkill skill = itemSkillRepository.findBySkillName(skillName)
                .orElseGet(() -> itemSkillRepository.save(
                        ItemSkill.builder()
                                .skillName(skillName)
                                .skillKey(skillKey)
                                .build()
                ));
        // 아이템-스킬 매핑이 없으면 신규 저장
        if (!itemSkillMappingRepository.existsByItemIdAndSkillId(item.getId(), skill.getId())) {
            itemSkillMappingRepository.save(new ItemSkillMapping(item, skill));
        }
        return skill;
    }

    // ── UPSERT ────────────────────────────────────────────────────────────────

    private void upsertCoeff(SkillCoeffEntry e, MercenarySkill mercSkill, ItemSkill itemSkill) {
        SkillType skillType = SkillType.valueOf(e.skillType);

        skillCoefficientRepository.findByRowId(e.rowId).ifPresentOrElse(
                existing -> existing.updateCoefficients(
                        e.coefStr, e.coefDex, e.coefVit, e.coefInt,
                        e.coefAtk, e.coefLvl, e.hitCount, e.damageRangeFactor,
                        skillType, e.castsPerSecond, e.tickIntervalMs, e.confidence, e.note
                ),
                () -> {
                    SkillCoefficient sc = (mercSkill != null)
                            ? SkillCoefficient.ofMercenary()
                                    .mercenarySkill(mercSkill)
                                    .rowId(e.rowId)
                                    .coefStr(e.coefStr).coefDex(e.coefDex)
                                    .coefVit(e.coefVit).coefInt(e.coefInt)
                                    .coefAtk(e.coefAtk).coefLvl(e.coefLvl)
                                    .hitCount(e.hitCount)
                                    .damageRangeFactor(e.damageRangeFactor)
                                    .skillType(skillType)
                                    .castsPerSecond(e.castsPerSecond)
                                    .tickIntervalMs(e.tickIntervalMs)
                                    .confidence(e.confidence)
                                    .measurementNote(e.note)
                                    .build()
                            : SkillCoefficient.ofItem()
                                    .itemSkill(itemSkill)
                                    .rowId(e.rowId)
                                    .coefStr(e.coefStr).coefDex(e.coefDex)
                                    .coefVit(e.coefVit).coefInt(e.coefInt)
                                    .coefAtk(e.coefAtk).coefLvl(e.coefLvl)
                                    .hitCount(e.hitCount)
                                    .damageRangeFactor(e.damageRangeFactor)
                                    .skillType(skillType)
                                    .castsPerSecond(e.castsPerSecond)
                                    .tickIntervalMs(e.tickIntervalMs)
                                    .confidence(e.confidence)
                                    .measurementNote(e.note)
                                    .build();
                    skillCoefficientRepository.save(sc);
                }
        );
    }

    // ── JSON 역직렬화용 DTO ───────────────────────────────────────────────────

    static class SkillCoeffEntry {
        @JsonProperty("row_id")        public String rowId;
        @JsonProperty("type")          public String type;
        @JsonProperty("mercenary_key") public String mercenaryKey;
        @JsonProperty("item_key")      public String itemKey;
        @JsonProperty("skill_key")     public String skillKey;
        @JsonProperty("skill_name")    public String skillName;
        @JsonProperty("coef_str")      public float coefStr;
        @JsonProperty("coef_dex")      public float coefDex;
        @JsonProperty("coef_vit")      public float coefVit;
        @JsonProperty("coef_int")      public float coefInt;
        @JsonProperty("coef_atk")      public float coefAtk;
        @JsonProperty("coef_lvl")      public float coefLvl;
        @JsonProperty("hit_count")     public int hitCount;
        @JsonProperty("damage_range_factor") public float damageRangeFactor;
        @JsonProperty("skill_type")    public String skillType;
        @JsonProperty("casts_per_second")  public Float castsPerSecond;
        @JsonProperty("tick_interval_ms")  public Integer tickIntervalMs;
        @JsonProperty("confidence")    public String confidence;
        @JsonProperty("note")          public String note;
    }
}
