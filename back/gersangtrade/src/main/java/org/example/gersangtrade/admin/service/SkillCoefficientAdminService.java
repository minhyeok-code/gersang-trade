package org.example.gersangtrade.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.admin.dto.request.SkillCoefficientCreateRequest;
import org.example.gersangtrade.admin.dto.request.SkillCoefficientJsonRow;
import org.example.gersangtrade.admin.dto.request.SkillCoefficientMeasurementRequest;
import org.example.gersangtrade.admin.dto.request.SkillCoefficientUpdateRequest;
import org.example.gersangtrade.admin.dto.response.SkillCoefficientAdminResponse;
import org.example.gersangtrade.admin.dto.response.SkillCoefficientIssueListResponse;
import org.example.gersangtrade.admin.dto.response.SkillCoefficientIssueResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillCoefficientAdminService {

    private static final Map<String, String> MERCENARY_NAME_BY_GERNIVERSE_KEY = Map.ofEntries(
            Map.entry("jigook", "지국천왕"),
            Map.entry("gwangmok", "광목천왕"),
            Map.entry("jeungjang", "증장천왕"),
            Map.entry("damoon", "다문천왕"),
            Map.entry("gakJigook", "각성 지국천왕"),
            Map.entry("gakGwangmok", "각성 광목천왕"),
            Map.entry("gakJeungjang", "각성 증장천왕"),
            Map.entry("gakDamoon", "각성 다문천왕"),
            Map.entry("hangsamse", "항삼세명왕"),
            Map.entry("goondari", "군다리명왕"),
            Map.entry("daewideok", "대위덕명왕"),
            Map.entry("boodong", "부동명왕"),
            Map.entry("geumgangyacha", "금강야차명왕"),
            Map.entry("gakHangsamse", "각성 항삼세명왕"),
            Map.entry("gakGoondari", "각성 군다리명왕"),
            Map.entry("gakDaewideok", "각성 대위덕명왕"),
            Map.entry("gakGeumgangyacha", "각성 금강야차명왕"),
            Map.entry("joomong", "주몽"),
            Map.entry("maenghwaek", "맹획"),
            Map.entry("nobootsuna", "노부츠나"),
            Map.entry("bajirao", "바지라오"),
            Map.entry("chosun", "초선"),
            Map.entry("bokuten", "보쿠텐"),
            Map.entry("akbar", "악바르"),
            Map.entry("honggildong", "홍길동"),
            Map.entry("yeopo", "여포"),
            Map.entry("fpwlsktnfxksk", "레지나"),
            Map.entry("hwamokran", "화목란"),
            Map.entry("tjsdlsakstjsdi", "만선야"),
            Map.entry("majo", "마조"),
            Map.entry("choimoosun", "최무선"),
            Map.entry("sinsoo-cheongyong", "청룡"),
            Map.entry("sinsoo-girin", "기린"),
            Map.entry("sinsoo-airavata", "아이라바타"),
            Map.entry("sinsoo-joojak", "주작"),
            Map.entry("hyoongsoo-gakDoholl", "각성 도올"),
            Map.entry("hyoongsoo-gakGoonggi", "각성 궁기"),
            Map.entry("hyoongsoo-gakHondon", "각성 혼돈"),
            Map.entry("hyoongsoo-gakDocheol", "각성 도철")
    );

    private final SkillCoefficientRepository skillCoefficientRepository;
    private final MercenaryRepository mercenaryRepository;
    private final MercenarySkillRepository mercenarySkillRepository;
    private final ItemRepository itemRepository;
    private final ItemSkillRepository itemSkillRepository;
    private final ItemSkillMappingRepository itemSkillMappingRepository;
    private final SetGrantedSkillRepository setGrantedSkillRepository;

    // ── 목록 조회 ────────────────────────────────────────────────────────────

    /**
     * unmeasured=true이면 casts_per_second / tick_interval_ms 둘 다 null인 미측정 행만 반환한다.
     */
    @Transactional(readOnly = true)
    public List<SkillCoefficientAdminResponse> list(boolean unmeasured) {
        List<SkillCoefficient> list = unmeasured
                ? skillCoefficientRepository.findUnmeasuredWithOwners()
                : skillCoefficientRepository.findAllWithOwners();
        return list.stream().map(SkillCoefficientAdminResponse::of).toList();
    }

    // ── JSON 파일 bulk upsert ─────────────────────────────────────────────

    /**
     * Skill-coeff.json 배열을 bulk upsert한다.
     * row_id 기준으로 존재하면 전체 필드 업데이트, 없으면 신규 생성.
     *
     * @return { "upserted": N, "skipped": M }
     */
    @Transactional
    public Map<String, Object> bulkUpsert(List<SkillCoefficientJsonRow> rows) {
        int upserted = 0;
        int skipped = 0;
        List<String> skipReasons = new ArrayList<>();

        for (SkillCoefficientJsonRow row : rows) {
            try {
                upsertOne(row);
                upserted++;
            } catch (Exception e) {
                log.warn("스킬 계수 upsert 스킵: rowId={}, reason={}", row.rowId(), e.getMessage());
                if (skipReasons.size() < 10) {
                    skipReasons.add(row.rowId() + ": " + e.getMessage());
                }
                skipped++;
            }
        }

        log.info("스킬 계수 bulk upsert 완료: upserted={}, skipped={}", upserted, skipped);
        return Map.of("upserted", upserted, "skipped", skipped, "skipReasons", skipReasons);
    }

    private void upsertOne(SkillCoefficientJsonRow row) {
        if (row.isSetGranted()) {
            upsertSetGrantedSkillCoefficient(row);
        } else if (row.isItem()) {
            upsertItemSkillCoefficient(row);
        } else {
            upsertMercenarySkillCoefficient(row);
        }
    }

    private void upsertSetGrantedSkillCoefficient(SkillCoefficientJsonRow row) {
        if (row.skillBehaviorType() == null) {
            throw new IllegalArgumentException("skillBehaviorType 누락 (type=set_granted)");
        }
        if (row.statSource() == null) {
            throw new IllegalArgumentException("statSource 누락 (type=set_granted)");
        }
        if (row.triggerSource() == null) {
            throw new IllegalArgumentException("triggerSource 누락 (type=set_granted)");
        }

        // SetGrantedSkill을 skillKey 기준으로 upsert — 없으면 생성, 있으면 분류 정보 갱신
        SetGrantedSkill setGrantedSkill = setGrantedSkillRepository
                .findBySkillKey(row.skillKey())
                .orElseGet(() -> setGrantedSkillRepository.save(
                        SetGrantedSkill.builder()
                                .skillKey(row.skillKey())
                                .skillName(row.skillName())
                                .skillBehaviorType(row.skillBehaviorType())
                                .statSource(row.statSource())
                                .triggerSource(row.triggerSource())
                                .triggerEveryN(row.triggerEveryN())
                                .triggerBaseSkillKey(row.triggerBaseSkillKey())
                                .note(row.note())
                                .build()));

        setGrantedSkill.updateInfo(
                row.skillName(), row.skillBehaviorType(), row.statSource(), row.triggerSource(),
                row.triggerEveryN(), row.triggerBaseSkillKey(), row.note());

        upsertCoefficient(row, null, null, setGrantedSkill);
    }

    private void upsertMercenarySkillCoefficient(SkillCoefficientJsonRow row) {
        MercenarySkill mercenarySkill = resolveMercenarySkill(row);
        mercenarySkill.updateSkillKey(row.skillKey());

        upsertCoefficient(row, mercenarySkill, null, null);
    }

    private void upsertItemSkillCoefficient(SkillCoefficientJsonRow row) {
        ItemSkill itemSkill = resolveItemSkill(row);
        itemSkill.updateSkillKey(row.skillKey());

        upsertCoefficient(row, null, itemSkill, null);
    }

    private MercenarySkill resolveMercenarySkill(SkillCoefficientJsonRow row) {
        if (row.mercenaryKey() != null && !row.mercenaryKey().isBlank()) {
            Optional<Mercenary> mercenary = findMercenaryByGerniverseKeyOrName(row.mercenaryKey());
            if (mercenary.isPresent()) {
                return findOrCreateMercenarySkill(mercenary.get(), row);
            }
        }

        return findUniqueMercenarySkillBySkillKey(row)
                .or(() -> findUniqueMercenarySkillBySkillName(row))
                .orElseThrow(() -> new IllegalArgumentException(
                        "용병 스킬 매핑 실패: mercenaryKey=" + row.mercenaryKey()
                                + ", skillKey=" + row.skillKey()
                                + ", skillName=" + row.skillName()));
    }

    private Optional<Mercenary> findMercenaryByGerniverseKeyOrName(String mercenaryKey) {
        Optional<Mercenary> byKey = mercenaryRepository.findByKey(mercenaryKey);
        if (byKey.isPresent()) {
            return byKey;
        }

        // 로컬/거상짱 기반 데이터는 mercenary_key가 비어 있을 수 있어 한글 이름으로 보정한다.
        String mercenaryName = MERCENARY_NAME_BY_GERNIVERSE_KEY.get(mercenaryKey);
        return mercenaryName == null ? Optional.empty() : mercenaryRepository.findByName(mercenaryName);
    }

    private MercenarySkill findOrCreateMercenarySkill(Mercenary mercenary, SkillCoefficientJsonRow row) {
        return mercenarySkillRepository
                .findByMercenaryIdAndSkillName(mercenary.getId(), row.skillName())
                .orElseGet(() -> mercenarySkillRepository.save(
                        MercenarySkill.builder()
                                .mercenary(mercenary)
                                .skillName(row.skillName())
                                .skillKey(row.skillKey())
                                .build()));
    }

    private Optional<MercenarySkill> findUniqueMercenarySkillBySkillKey(SkillCoefficientJsonRow row) {
        if (row.skillKey() == null || row.skillKey().isBlank()) {
            return Optional.empty();
        }

        List<MercenarySkill> matches = mercenarySkillRepository.findBySkillKey(row.skillKey());
        if (matches.size() > 1) {
            throw new IllegalArgumentException("용병 스킬 skillKey 중복: skillKey=" + row.skillKey());
        }
        return matches.stream().findFirst();
    }

    private Optional<MercenarySkill> findUniqueMercenarySkillBySkillName(SkillCoefficientJsonRow row) {
        if (row.skillName() == null || row.skillName().isBlank()) {
            return Optional.empty();
        }

        List<MercenarySkill> matches = mercenarySkillRepository.findBySkillName(row.skillName());
        if (matches.size() > 1) {
            throw new IllegalArgumentException("용병 스킬명 중복: skillName=" + row.skillName());
        }
        return matches.stream().findFirst();
    }

    private ItemSkill resolveItemSkill(SkillCoefficientJsonRow row) {
        if (row.itemKey() != null && !row.itemKey().isBlank()) {
            Optional<Item> item = itemRepository.findByItemKey(row.itemKey());
            if (item.isPresent()) {
                ItemSkill skill = findOrCreateItemSkill(row);
                ensureItemSkillMapping(item.get(), skill);
                return skill;
            }
        }

        // itemKey 없으면 스킬만 find-or-create (매핑 없이)
        return findOrCreateItemSkill(row);
    }

    /**
     * skillKey/skillName 기반으로 전역 ItemSkill을 찾거나 신규 생성한다.
     * skill_name이 전역 UNIQUE이므로 item 파라미터 불필요.
     */
    private ItemSkill findOrCreateItemSkill(SkillCoefficientJsonRow row) {
        if (row.skillKey() != null && !row.skillKey().isBlank()) {
            Optional<ItemSkill> byKey = itemSkillRepository.findBySkillKey(row.skillKey());
            if (byKey.isPresent()) return byKey.get();
        }
        return itemSkillRepository.findBySkillName(row.skillName())
                .orElseGet(() -> itemSkillRepository.save(
                        ItemSkill.builder()
                                .skillName(row.skillName())
                                .skillKey(row.skillKey())
                                .build()));
    }

    /** (아이템, 스킬) 매핑이 없으면 신규 저장 */
    private void ensureItemSkillMapping(Item item, ItemSkill skill) {
        if (!itemSkillMappingRepository.existsByItemIdAndSkillId(item.getId(), skill.getId())) {
            itemSkillMappingRepository.save(new ItemSkillMapping(item, skill));
        }
    }

    private void upsertCoefficient(SkillCoefficientJsonRow row,
                                   MercenarySkill mercenarySkill,
                                   ItemSkill itemSkill,
                                   SetGrantedSkill setGrantedSkill) {
        skillCoefficientRepository.findByRowId(row.rowId())
                .ifPresentOrElse(
                        sc -> sc.updateCoefficients(
                                row.coefStr(), row.coefDex(), row.coefVit(),
                                row.coefInt(), row.coefAtk(), row.coefLvl(),
                                row.hitCount(), row.damageRangeFactor(),
                                row.skillType(), row.castsPerSecond(), row.tickIntervalMs(),
                                row.confidence(), row.note()),
                        () -> skillCoefficientRepository.save(
                                mercenarySkill != null ? buildForMercenary(row, mercenarySkill)
                                : itemSkill != null    ? buildForItem(row, itemSkill)
                                :                        buildForSetGrantedSkill(row, setGrantedSkill))
                );
    }

    private SkillCoefficient buildForMercenary(SkillCoefficientJsonRow row,
                                               MercenarySkill mercenarySkill) {
        return SkillCoefficient.ofMercenary()
                .mercenarySkill(mercenarySkill)
                .rowId(row.rowId())
                .coefStr(row.coefStr()).coefDex(row.coefDex()).coefVit(row.coefVit())
                .coefInt(row.coefInt()).coefAtk(row.coefAtk()).coefLvl(row.coefLvl())
                .hitCount(row.hitCount()).damageRangeFactor(row.damageRangeFactor())
                .skillType(row.skillType())
                .castsPerSecond(row.castsPerSecond()).tickIntervalMs(row.tickIntervalMs())
                .confidence(row.confidence()).measurementNote(row.note())
                .build();
    }

    private SkillCoefficient buildForItem(SkillCoefficientJsonRow row, ItemSkill itemSkill) {
        return SkillCoefficient.ofItem()
                .itemSkill(itemSkill)
                .rowId(row.rowId())
                .coefStr(row.coefStr()).coefDex(row.coefDex()).coefVit(row.coefVit())
                .coefInt(row.coefInt()).coefAtk(row.coefAtk()).coefLvl(row.coefLvl())
                .hitCount(row.hitCount()).damageRangeFactor(row.damageRangeFactor())
                .skillType(row.skillType())
                .castsPerSecond(row.castsPerSecond()).tickIntervalMs(row.tickIntervalMs())
                .confidence(row.confidence()).measurementNote(row.note())
                .build();
    }

    private SkillCoefficient buildForSetGrantedSkill(SkillCoefficientJsonRow row,
                                                     SetGrantedSkill setGrantedSkill) {
        return SkillCoefficient.ofSetGrantedSkill()
                .setGrantedSkill(setGrantedSkill)
                .rowId(row.rowId())
                .coefStr(row.coefStr()).coefDex(row.coefDex()).coefVit(row.coefVit())
                .coefInt(row.coefInt()).coefAtk(row.coefAtk()).coefLvl(row.coefLvl())
                .hitCount(row.hitCount()).damageRangeFactor(row.damageRangeFactor())
                .skillType(row.skillType())
                .castsPerSecond(row.castsPerSecond()).tickIntervalMs(row.tickIntervalMs())
                .confidence(row.confidence()).measurementNote(row.note())
                .build();
    }

    // ── 이슈 검증 ────────────────────────────────────────────────────────────

    /**
     * 전체 스킬 계수를 검사해 이슈가 있는 항목만 반환한다.
     * 이슈 종류: MISSING_SKILL_TYPE / UNMEASURED / ALL_COEFS_ZERO / HIT_COUNT_ZERO
     */
    @Transactional(readOnly = true)
    public SkillCoefficientIssueListResponse listIssues() {
        List<SkillCoefficient> all = skillCoefficientRepository.findAllWithOwners();

        List<SkillCoefficientIssueResponse> entries = all.stream()
                .map(sc -> {
                    var issues = SkillCoefficientIssueResponse.detect(sc);
                    return issues.isEmpty() ? null : SkillCoefficientIssueResponse.of(sc, issues);
                })
                .filter(r -> r != null)
                .toList();

        return new SkillCoefficientIssueListResponse(all.size(), entries.size(), entries);
    }

    // ── 수동 생성 ────────────────────────────────────────────────────────────

    /**
     * 스킬 계수 단건 수동 생성.
     * mercenarySkillId 또는 itemSkillId 중 하나만 지정한다.
     */
    @Transactional
    public SkillCoefficientAdminResponse create(SkillCoefficientCreateRequest req) {
        long specifiedCount = (req.mercenarySkillId() != null ? 1 : 0)
                + (req.itemSkillId() != null ? 1 : 0)
                + (req.setGrantedSkillId() != null ? 1 : 0);
        if (specifiedCount == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "mercenarySkillId / itemSkillId / setGrantedSkillId 중 하나는 필수입니다.");
        }
        if (specifiedCount > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "mercenarySkillId / itemSkillId / setGrantedSkillId는 동시에 지정할 수 없습니다.");
        }

        SkillCoefficient sc;
        if (req.mercenarySkillId() != null) {
            MercenarySkill skill = mercenarySkillRepository.findById(req.mercenarySkillId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "용병 스킬 미존재: id=" + req.mercenarySkillId()));
            sc = SkillCoefficient.ofMercenary()
                    .mercenarySkill(skill)
                    .rowId(req.rowId())
                    .coefStr(req.coefStr()).coefDex(req.coefDex()).coefVit(req.coefVit())
                    .coefInt(req.coefInt()).coefAtk(req.coefAtk()).coefLvl(req.coefLvl())
                    .hitCount(req.hitCount()).damageRangeFactor(req.damageRangeFactor())
                    .skillType(req.skillType())
                    .castsPerSecond(req.castsPerSecond()).tickIntervalMs(req.tickIntervalMs())
                    .confidence(req.confidence()).measurementNote(req.note())
                    .build();
        } else if (req.itemSkillId() != null) {
            ItemSkill skill = itemSkillRepository.findById(req.itemSkillId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "아이템 스킬 미존재: id=" + req.itemSkillId()));
            sc = SkillCoefficient.ofItem()
                    .itemSkill(skill)
                    .rowId(req.rowId())
                    .coefStr(req.coefStr()).coefDex(req.coefDex()).coefVit(req.coefVit())
                    .coefInt(req.coefInt()).coefAtk(req.coefAtk()).coefLvl(req.coefLvl())
                    .hitCount(req.hitCount()).damageRangeFactor(req.damageRangeFactor())
                    .skillType(req.skillType())
                    .castsPerSecond(req.castsPerSecond()).tickIntervalMs(req.tickIntervalMs())
                    .confidence(req.confidence()).measurementNote(req.note())
                    .build();
        } else {
            SetGrantedSkill skill = setGrantedSkillRepository.findById(req.setGrantedSkillId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "세트 부여 스킬 미존재: id=" + req.setGrantedSkillId()));
            sc = SkillCoefficient.ofSetGrantedSkill()
                    .setGrantedSkill(skill)
                    .rowId(req.rowId())
                    .coefStr(req.coefStr()).coefDex(req.coefDex()).coefVit(req.coefVit())
                    .coefInt(req.coefInt()).coefAtk(req.coefAtk()).coefLvl(req.coefLvl())
                    .hitCount(req.hitCount()).damageRangeFactor(req.damageRangeFactor())
                    .skillType(req.skillType())
                    .castsPerSecond(req.castsPerSecond()).tickIntervalMs(req.tickIntervalMs())
                    .confidence(req.confidence()).measurementNote(req.note())
                    .build();
        }

        return SkillCoefficientAdminResponse.of(skillCoefficientRepository.save(sc));
    }

    // ── 전체 수정 ─────────────────────────────────────────────────────────────

    /**
     * 스킬 계수 전체 수정 (PUT 의미론).
     * FK(스킬 대상)는 변경 불가. 계수·측정값·메타데이터를 모두 교체한다.
     */
    @Transactional
    public SkillCoefficientAdminResponse update(Long id, SkillCoefficientUpdateRequest req) {
        SkillCoefficient sc = skillCoefficientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "스킬 계수 미존재: id=" + id));

        sc.updateCoefficients(
                req.coefStr(), req.coefDex(), req.coefVit(),
                req.coefInt(), req.coefAtk(), req.coefLvl(),
                req.hitCount(), req.damageRangeFactor(),
                req.skillType(), req.castsPerSecond(), req.tickIntervalMs(),
                req.confidence(), req.note());

        if (req.rowId() != null) {
            sc.updateRowId(req.rowId());
        }

        return SkillCoefficientAdminResponse.of(sc);
    }

    // ── 측정값 업데이트 ──────────────────────────────────────────────────────

    /**
     * 직접 측정값(casts_per_second 또는 tick_interval_ms)을 업데이트한다.
     * 엔티티의 skill_type을 기준으로 어느 측정값이 필요한지 검증한다.
     */
    @Transactional
    public SkillCoefficientAdminResponse updateMeasurement(Long id,
                                                            SkillCoefficientMeasurementRequest req) {
        SkillCoefficient sc = skillCoefficientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "스킬 계수 미존재: id=" + id));

        if (sc.getSkillType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "skill_type이 설정되지 않음. JSON 재적재 후 시도하세요.");
        }
        if (sc.getSkillType() == SkillType.INSTANT && req.castsPerSecond() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "INSTANT 스킬은 castsPerSecond 필수");
        }
        if (sc.getSkillType() == SkillType.PERSISTENT && req.tickIntervalMs() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "PERSISTENT 스킬은 tickIntervalMs 필수");
        }
        // TRIGGER 타입은 trigger_every_n이 연결된 ItemSkill/SetGrantedSkill에 저장되므로
        // castsPerSecond·tickIntervalMs 검증 불필요

        sc.updateMeasurement(req.castsPerSecond(), req.tickIntervalMs(), req.measurementNote());
        return SkillCoefficientAdminResponse.of(sc);
    }
}
