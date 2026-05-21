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
import org.example.gersangtrade.catalog.repository.ItemSkillRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.MercenarySkillRepository;
import org.example.gersangtrade.catalog.repository.SkillCoefficientRepository;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.ItemSkill;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenarySkill;
import org.example.gersangtrade.domain.catalog.SkillCoefficient;
import org.example.gersangtrade.domain.catalog.enums.SkillType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillCoefficientAdminService {

    private final SkillCoefficientRepository skillCoefficientRepository;
    private final MercenaryRepository mercenaryRepository;
    private final MercenarySkillRepository mercenarySkillRepository;
    private final ItemRepository itemRepository;
    private final ItemSkillRepository itemSkillRepository;

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
    public Map<String, Integer> bulkUpsert(List<SkillCoefficientJsonRow> rows) {
        int upserted = 0;
        int skipped = 0;

        for (SkillCoefficientJsonRow row : rows) {
            try {
                upsertOne(row);
                upserted++;
            } catch (Exception e) {
                log.warn("스킬 계수 upsert 스킵: rowId={}, reason={}", row.rowId(), e.getMessage());
                skipped++;
            }
        }

        log.info("스킬 계수 bulk upsert 완료: upserted={}, skipped={}", upserted, skipped);
        return Map.of("upserted", upserted, "skipped", skipped);
    }

    private void upsertOne(SkillCoefficientJsonRow row) {
        if (row.isItem()) {
            upsertItemSkillCoefficient(row);
        } else {
            upsertMercenarySkillCoefficient(row);
        }
    }

    private void upsertMercenarySkillCoefficient(SkillCoefficientJsonRow row) {
        if (row.mercenaryKey() == null || row.mercenaryKey().isBlank()) {
            throw new IllegalArgumentException("mercenaryKey 누락 (type=mercenary)");
        }

        Mercenary mercenary = mercenaryRepository.findByKey(row.mercenaryKey())
                .orElseThrow(() -> new IllegalArgumentException(
                        "용병 미존재: mercenaryKey=" + row.mercenaryKey()));

        MercenarySkill mercenarySkill = mercenarySkillRepository
                .findByMercenaryIdAndSkillName(mercenary.getId(), row.skillName())
                .orElseGet(() -> mercenarySkillRepository.save(
                        MercenarySkill.builder()
                                .mercenary(mercenary)
                                .skillName(row.skillName())
                                .skillKey(row.skillKey())
                                .build()));
        mercenarySkill.updateSkillKey(row.skillKey());

        upsertCoefficient(row, mercenarySkill, null);
    }

    private void upsertItemSkillCoefficient(SkillCoefficientJsonRow row) {
        if (row.itemKey() == null || row.itemKey().isBlank()) {
            throw new IllegalArgumentException("itemKey 누락 (type=item)");
        }

        Item item = itemRepository.findByItemKey(row.itemKey())
                .orElseThrow(() -> new IllegalArgumentException(
                        "아이템 미존재: itemKey=" + row.itemKey()));

        ItemSkill itemSkill = itemSkillRepository
                .findByItemIdAndSkillName(item.getId(), row.skillName())
                .orElseGet(() -> itemSkillRepository.save(
                        ItemSkill.builder()
                                .item(item)
                                .skillName(row.skillName())
                                .skillKey(row.skillKey())
                                .build()));
        itemSkill.updateSkillKey(row.skillKey());

        upsertCoefficient(row, null, itemSkill);
    }

    private void upsertCoefficient(SkillCoefficientJsonRow row,
                                   MercenarySkill mercenarySkill,
                                   ItemSkill itemSkill) {
        skillCoefficientRepository.findByRowId(row.rowId())
                .ifPresentOrElse(
                        sc -> sc.updateCoefficients(
                                row.coefStr(), row.coefDex(), row.coefVit(),
                                row.coefInt(), row.coefAtk(), row.coefLvl(),
                                row.hitCount(), row.damageRangeFactor(),
                                row.skillType(), row.castsPerSecond(), row.tickIntervalMs(),
                                row.confidence(), row.note()),
                        () -> skillCoefficientRepository.save(
                                mercenarySkill != null
                                        ? buildForMercenary(row, mercenarySkill)
                                        : buildForItem(row, itemSkill))
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
        if (req.mercenarySkillId() == null && req.itemSkillId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "mercenarySkillId 또는 itemSkillId 중 하나는 필수입니다.");
        }
        if (req.mercenarySkillId() != null && req.itemSkillId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "mercenarySkillId와 itemSkillId는 동시에 지정할 수 없습니다.");
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
        } else {
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

        sc.updateMeasurement(req.castsPerSecond(), req.tickIntervalMs(), req.measurementNote());
        return SkillCoefficientAdminResponse.of(sc);
    }
}
