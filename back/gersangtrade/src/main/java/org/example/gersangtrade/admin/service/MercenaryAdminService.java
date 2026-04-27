package org.example.gersangtrade.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.request.CharacteristicCreateRequest;
import org.example.gersangtrade.admin.dto.request.CharacteristicLevelSaveRequest;
import org.example.gersangtrade.admin.dto.request.CharacteristicUpdateRequest;
import org.example.gersangtrade.admin.dto.request.MercenaryStatReplaceRequest;
import org.example.gersangtrade.admin.dto.request.MercenaryUpdateRequest;
import org.example.gersangtrade.admin.dto.request.SkillReplaceRequest;
import org.example.gersangtrade.admin.dto.response.CharacteristicAdminResponse;
import org.example.gersangtrade.admin.dto.response.MercenaryAdminResponse;
import org.example.gersangtrade.admin.dto.response.MercenaryDetailAdminResponse;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.MercenarySkillRepository;
import org.example.gersangtrade.catalog.repository.MercenaryStatRepository;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.MercenarySkill;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
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
public class MercenaryAdminService {

    private final MercenaryRepository mercenaryRepository;
    private final MercenaryCharacteristicRepository characteristicRepository;
    private final MercenaryCharacteristicLevelRepository levelRepository;
    private final MercenaryStatRepository statRepository;
    private final MercenarySkillRepository skillRepository;

    // ── 용병 상세 조회 ───────────────────────────────────────────────────────────

    /**
     * 용병 단건 상세 조회 — 기본정보 + 스탯 + 스킬 반환.
     */
    @Transactional(readOnly = true)
    public MercenaryDetailAdminResponse getMercenary(Long mercenaryId) {
        Mercenary mercenary = getMercenaryOrThrow(mercenaryId);
        List<MercenaryStat> stats = statRepository.findByMercenaryId(mercenaryId);
        List<MercenarySkill> skills = skillRepository.findByMercenaryId(mercenaryId);
        return MercenaryDetailAdminResponse.of(mercenary, stats, skills);
    }

    // ── 용병 기본정보 수정 ───────────────────────────────────────────────────────

    /**
     * 용병 이름·카테고리·국가·속성·속성값·출시예정 여부를 수정한다.
     * name은 null/공백이면 기존 값을 유지한다 (엔티티 updateInfo 동일 정책).
     */
    @Transactional
    public MercenaryDetailAdminResponse updateInfo(Long mercenaryId, MercenaryUpdateRequest req) {
        Mercenary mercenary = getMercenaryOrThrow(mercenaryId);
        mercenary.updateInfo(req.name(), req.category(), req.nation(),
                req.nature(), req.natureValue(), req.comingSoon());
        List<MercenaryStat> stats = statRepository.findByMercenaryId(mercenaryId);
        List<MercenarySkill> skills = skillRepository.findByMercenaryId(mercenaryId);
        return MercenaryDetailAdminResponse.of(mercenary, stats, skills);
    }

    // ── 용병 스탯 전체 교체 ─────────────────────────────────────────────────────

    /**
     * 용병 스탯을 PUT 의미론으로 교체한다.
     * 기존 스탯을 전부 삭제하고 요청 목록으로 재적재한다.
     */
    @Transactional
    public MercenaryDetailAdminResponse replaceStats(Long mercenaryId, MercenaryStatReplaceRequest req) {
        Mercenary mercenary = getMercenaryOrThrow(mercenaryId);
        statRepository.deleteByMercenaryId(mercenaryId);
        List<MercenaryStat> saved = req.stats().stream()
                .map(e -> statRepository.save(MercenaryStat.builder()
                        .mercenary(mercenary)
                        .statKey(e.statType())
                        .statValue(e.value())
                        .build()))
                .toList();
        List<MercenarySkill> skills = skillRepository.findByMercenaryId(mercenaryId);
        return MercenaryDetailAdminResponse.of(mercenary, saved, skills);
    }

    // ── 용병 스킬 전체 교체 ─────────────────────────────────────────────────────

    /**
     * 용병 스킬 목록을 PUT 의미론으로 교체한다.
     * 기존 스킬을 전부 삭제하고 요청 목록으로 재적재한다.
     */
    @Transactional
    public MercenaryDetailAdminResponse replaceSkills(Long mercenaryId, SkillReplaceRequest req) {
        Mercenary mercenary = getMercenaryOrThrow(mercenaryId);
        skillRepository.deleteByMercenaryId(mercenaryId);
        List<MercenarySkill> saved = req.skills().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> skillRepository.save(MercenarySkill.builder()
                        .mercenary(mercenary)
                        .skillName(s.trim())
                        .build()))
                .toList();
        List<MercenaryStat> stats = statRepository.findByMercenaryId(mercenaryId);
        return MercenaryDetailAdminResponse.of(mercenary, stats, saved);
    }

    // ── 용병 목록 조회 ───────────────────────────────────────────────────────────

    /**
     * 용병 목록을 페이지 단위로 조회한다.
     * 각 용병의 특성 등록 수를 포함해 관리자가 미입력 항목을 확인할 수 있도록 한다.
     */
    @Transactional(readOnly = true)
    public Page<MercenaryAdminResponse> listMercenaries(Pageable pageable) {
        Page<Mercenary> page = mercenaryRepository.findAll(pageable);

        // 특성 수 일괄 조회 (N+1 방지)
        List<Long> mercenaryIds = page.getContent().stream().map(Mercenary::getId).toList();
        Map<Long, Long> characteristicCounts = characteristicRepository
                .findByMercenaryIdIn(mercenaryIds)
                .stream()
                .collect(Collectors.groupingBy(
                        c -> c.getMercenary().getId(), Collectors.counting()));

        return page.map(m -> MercenaryAdminResponse.of(
                m, characteristicCounts.getOrDefault(m.getId(), 0L).intValue()));
    }

    // ── 특성 조회 ────────────────────────────────────────────────────────────────

    /**
     * 용병의 특성 목록과 각 특성의 레벨 수치를 반환한다.
     * requiredCharacteristicId를 통해 트리 구조를 프런트에서 렌더링할 수 있다.
     */
    @Transactional(readOnly = true)
    public List<CharacteristicAdminResponse> listCharacteristics(Long mercenaryId) {
        getMercenaryOrThrow(mercenaryId);
        List<MercenaryCharacteristic> chars = characteristicRepository.findByMercenaryId(mercenaryId);

        // key → id 역매핑 (requiredCharacteristicKey를 ID로 변환)
        Map<String, Long> keyToId = chars.stream()
                .collect(Collectors.toMap(MercenaryCharacteristic::getKey, MercenaryCharacteristic::getId));

        return chars.stream().map(c -> {
            Long requiredId = c.getRequiredCharacteristicKey() != null
                    ? keyToId.get(c.getRequiredCharacteristicKey())
                    : null;
            List<MercenaryCharacteristicLevel> levels = levelRepository.findByCharacteristicId(c.getId());
            return CharacteristicAdminResponse.from(c, requiredId, levels);
        }).toList();
    }

    // ── 특성 생성 ────────────────────────────────────────────────────────────────

    /**
     * 용병에 특성을 추가한다.
     *
     * <p>특성 구조 제약:
     * <ul>
     *   <li>최대 4개 — 2개(루트) 또는 4개(루트 2 + 자식 2)</li>
     *   <li>자식 특성은 동일 용병의 특성만 선행으로 지정 가능</li>
     * </ul>
     */
    @Transactional
    public CharacteristicAdminResponse createCharacteristic(Long mercenaryId,
                                                            CharacteristicCreateRequest req) {
        Mercenary mercenary = getMercenaryOrThrow(mercenaryId);
        List<MercenaryCharacteristic> existing = characteristicRepository.findByMercenaryId(mercenaryId);

        if (existing.size() >= 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "용병당 특성은 최대 4개까지 등록 가능합니다.");
        }

        String requiredKey = null;
        Long requiredId = req.requiredCharacteristicId();
        if (requiredId != null) {
            MercenaryCharacteristic parent = getCharacteristicOrThrow(requiredId);
            if (!parent.getMercenary().getId().equals(mercenaryId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "선행 특성이 해당 용병의 특성이 아닙니다.");
            }
            requiredKey = parent.getKey();
        }

        String key = generateKey(mercenaryId, req.name());
        MercenaryCharacteristic saved = characteristicRepository.save(
                MercenaryCharacteristic.builder()
                        .mercenary(mercenary)
                        .key(key)
                        .name(req.name())
                        .point(req.point())
                        .description(req.description())
                        .requiredCharacteristicKey(requiredKey)
                        .build());

        return CharacteristicAdminResponse.from(saved, req.requiredCharacteristicId(), List.of());
    }

    // ── 특성 수정 ────────────────────────────────────────────────────────────────

    @Transactional
    public CharacteristicAdminResponse updateCharacteristic(Long mercenaryId, Long charId,
                                                            CharacteristicUpdateRequest req) {
        validateCharacteristicBelongsTo(mercenaryId, charId);
        MercenaryCharacteristic characteristic = getCharacteristicOrThrow(charId);

        String requiredKey = null;
        Long requiredId = req.requiredCharacteristicId();
        if (requiredId != null) {
            if (requiredId.equals(charId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신을 선행 특성으로 지정할 수 없습니다.");
            }
            MercenaryCharacteristic parent = getCharacteristicOrThrow(requiredId);
            if (!parent.getMercenary().getId().equals(mercenaryId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "선행 특성이 해당 용병의 특성이 아닙니다.");
            }
            requiredKey = parent.getKey();
        }

        characteristic.update(req.name(), req.point(), req.description(), requiredKey);
        List<MercenaryCharacteristicLevel> levels = levelRepository.findByCharacteristicId(charId);
        return CharacteristicAdminResponse.from(characteristic, requiredId, levels);
    }

    // ── 특성 삭제 ────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteCharacteristic(Long mercenaryId, Long charId) {
        validateCharacteristicBelongsTo(mercenaryId, charId);
        // 해당 특성을 선행으로 참조하는 자식이 있으면 삭제 거부
        MercenaryCharacteristic c = getCharacteristicOrThrow(charId);
        boolean hasChild = characteristicRepository.findByMercenaryId(mercenaryId).stream()
                .anyMatch(ch -> c.getKey().equals(ch.getRequiredCharacteristicKey()));
        if (hasChild) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "이 특성을 선행으로 참조하는 자식 특성이 있습니다. 자식 특성을 먼저 삭제하세요.");
        }
        levelRepository.deleteByCharacteristicId(charId);
        characteristicRepository.deleteById(charId);
    }

    // ── 레벨 수치 일괄 저장 ─────────────────────────────────────────────────────

    /**
     * 특성의 레벨 수치를 일괄 저장한다.
     * PUT 의미론 — 기존 레벨 전체를 삭제하고 요청 목록으로 재적재한다.
     */
    @Transactional
    public CharacteristicAdminResponse saveLevels(Long mercenaryId, Long charId,
                                                   CharacteristicLevelSaveRequest req) {
        validateCharacteristicBelongsTo(mercenaryId, charId);
        MercenaryCharacteristic characteristic = getCharacteristicOrThrow(charId);

        levelRepository.deleteByCharacteristicId(charId);

        List<MercenaryCharacteristicLevel> saved = req.levels().stream()
                .map(entry -> levelRepository.save(MercenaryCharacteristicLevel.builder()
                        .characteristic(characteristic)
                        .label(entry.label())
                        .level(entry.level())
                        .amount(entry.amount())
                        .amountValue(parseAmountValue(entry.amount()))
                        .statType(entry.statType())
                        .build()))
                .toList();

        // key → id 역매핑
        Map<String, Long> keyToId = characteristicRepository.findByMercenaryId(mercenaryId).stream()
                .collect(Collectors.toMap(MercenaryCharacteristic::getKey, MercenaryCharacteristic::getId));
        Long requiredId = characteristic.getRequiredCharacteristicKey() != null
                ? keyToId.get(characteristic.getRequiredCharacteristicKey())
                : null;

        return CharacteristicAdminResponse.from(characteristic, requiredId, saved);
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────────────

    private Mercenary getMercenaryOrThrow(Long id) {
        return mercenaryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "용병을 찾을 수 없습니다: " + id));
    }

    private MercenaryCharacteristic getCharacteristicOrThrow(Long id) {
        return characteristicRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "특성을 찾을 수 없습니다: " + id));
    }

    private void validateCharacteristicBelongsTo(Long mercenaryId, Long charId) {
        MercenaryCharacteristic c = getCharacteristicOrThrow(charId);
        if (!c.getMercenary().getId().equals(mercenaryId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "해당 용병의 특성이 아닙니다.");
        }
    }

    /** "20%", "500" 문자열을 Float으로 파싱. 파싱 불가 시 null. */
    private Float parseAmountValue(String amount) {
        if (amount == null || amount.isBlank()) return null;
        try {
            return Float.parseFloat(amount.replace("%", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 특성 내부 키 자동 생성 — "merc-{mercenaryId}-{name슬러그}" */
    private String generateKey(Long mercenaryId, String name) {
        String slug = name.replaceAll("[^가-힣a-zA-Z0-9]", "");
        return "merc-" + mercenaryId + "-" + slug;
    }
}
