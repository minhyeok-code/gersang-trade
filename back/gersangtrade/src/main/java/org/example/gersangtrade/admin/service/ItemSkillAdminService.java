package org.example.gersangtrade.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.skill.ItemSkillAdminResponse;
import org.example.gersangtrade.admin.dto.skill.ItemSkillCreateRequest;
import org.example.gersangtrade.admin.dto.skill.ItemSkillUpdateRequest;
import org.example.gersangtrade.catalog.repository.ItemSkillRepository;
import org.example.gersangtrade.domain.catalog.ItemSkill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 아이템 스킬 관리자 서비스 — CRUD */
@Service
@RequiredArgsConstructor
public class ItemSkillAdminService {

    private final ItemSkillRepository itemSkillRepository;

    // ── 목록 조회 ────────────────────────────────────────────────────────────

    /**
     * 아이템 스킬 목록 페이징 조회.
     * skillName 파라미터가 있으면 이름 부분 검색을 적용한다.
     */
    @Transactional(readOnly = true)
    public Page<ItemSkillAdminResponse> list(String skillName, Pageable pageable) {
        return itemSkillRepository.searchBySkillName(skillName, pageable)
                .map(ItemSkillAdminResponse::from);
    }

    // ── 단건 조회 ────────────────────────────────────────────────────────────

    /** 아이템 스킬 단건 조회 */
    @Transactional(readOnly = true)
    public ItemSkillAdminResponse get(Long id) {
        return ItemSkillAdminResponse.from(findOrThrow(id));
    }

    // ── 신규 등록 ────────────────────────────────────────────────────────────

    /**
     * 아이템 스킬을 신규 등록한다.
     * 동일 skillName이 이미 존재하면 409를 반환한다.
     */
    @Transactional
    public ItemSkillAdminResponse create(ItemSkillCreateRequest req) {
        if (itemSkillRepository.findBySkillName(req.skillName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이미 동일한 이름의 스킬이 존재합니다: " + req.skillName());
        }
        ItemSkill skill = itemSkillRepository.save(ItemSkill.builder()
                .skillName(req.skillName())
                .skillBehaviorType(req.skillBehaviorType())
                .replacesBaseSkill(req.replacesBaseSkill())
                .triggerEveryN(req.triggerEveryN())
                .triggerBaseSkillKey(req.triggerBaseSkillKey())
                .note(req.note())
                .build());
        return ItemSkillAdminResponse.from(skill);
    }

    // ── 수정 ─────────────────────────────────────────────────────────────────

    /**
     * 아이템 스킬 정보를 수정한다.
     * null이 아닌 필드만 업데이트한다.
     */
    @Transactional
    public ItemSkillAdminResponse update(Long id, ItemSkillUpdateRequest req) {
        ItemSkill skill = findOrThrow(id);

        // skillName 변경 시 중복 확인 (자기 자신 제외)
        if (req.skillName() != null && !req.skillName().isBlank()) {
            itemSkillRepository.findBySkillName(req.skillName())
                    .filter(s -> !s.getId().equals(id))
                    .ifPresent(s -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "이미 동일한 이름의 스킬이 존재합니다: " + req.skillName());
                    });
            skill.updateSkillName(req.skillName());
        }

        // null이 아닌 행동 관련 필드만 갱신 (기존 값 유지 처리)
        var behaviorType = req.skillBehaviorType() != null
                ? req.skillBehaviorType() : skill.getSkillBehaviorType();
        var replacesBase = req.replacesBaseSkill() != null
                ? req.replacesBaseSkill() : skill.isReplacesBaseSkill();
        var triggerN = req.triggerEveryN() != null
                ? req.triggerEveryN() : skill.getTriggerEveryN();
        var triggerKey = req.triggerBaseSkillKey() != null
                ? req.triggerBaseSkillKey() : skill.getTriggerBaseSkillKey();
        var note = req.note() != null ? req.note() : skill.getNote();

        skill.updateBehavior(behaviorType, replacesBase, triggerN, triggerKey, note);

        return ItemSkillAdminResponse.from(skill);
    }

    // ── 삭제 ─────────────────────────────────────────────────────────────────

    /** 아이템 스킬 삭제 */
    @Transactional
    public void delete(Long id) {
        ItemSkill skill = findOrThrow(id);
        itemSkillRepository.delete(skill);
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────────

    private ItemSkill findOrThrow(Long id) {
        return itemSkillRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "스킬을 찾을 수 없습니다: " + id));
    }
}
