package org.example.gersangtrade.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.setgrantedskill.*;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetSkillEffectRepository;
import org.example.gersangtrade.catalog.repository.SetGrantedSkillRepository;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.EquipmentSetSkillEffect;
import org.example.gersangtrade.domain.catalog.SetGrantedSkill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SetGrantedSkillAdminService {

    private final SetGrantedSkillRepository setGrantedSkillRepository;
    private final EquipmentSetSkillEffectRepository skillEffectRepository;
    private final EquipmentSetRepository equipmentSetRepository;

    // ── SetGrantedSkill CRUD ──────────────────────────────────────────

    /** 세트 부여 스킬 목록 */
    @Transactional(readOnly = true)
    public Page<SetGrantedSkillResponse> getSkills(Pageable pageable) {
        return setGrantedSkillRepository.findAll(pageable).map(SetGrantedSkillResponse::from);
    }

    /** 세트 부여 스킬 단건 조회 */
    @Transactional(readOnly = true)
    public SetGrantedSkillResponse getSkill(Long id) {
        return SetGrantedSkillResponse.from(findSkillOrThrow(id));
    }

    /** 세트 부여 스킬 생성 */
    @Transactional
    public SetGrantedSkillResponse createSkill(SetGrantedSkillCreateRequest req) {
        if (setGrantedSkillRepository.findBySkillKey(req.skillKey()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이미 존재하는 skillKey입니다: " + req.skillKey());
        }
        SetGrantedSkill skill = SetGrantedSkill.builder()
                .skillKey(req.skillKey())
                .skillName(req.skillName())
                .skillBehaviorType(req.skillBehaviorType())
                .statSource(req.statSource())
                .triggerSource(req.triggerSource())
                .triggerEveryN(req.triggerEveryN())
                .triggerBaseSkillKey(req.triggerBaseSkillKey())
                .note(req.note())
                .build();
        return SetGrantedSkillResponse.from(setGrantedSkillRepository.save(skill));
    }

    /** 세트 부여 스킬 수정 */
    @Transactional
    public SetGrantedSkillResponse updateSkill(Long id, SetGrantedSkillUpdateRequest req) {
        SetGrantedSkill skill = findSkillOrThrow(id);
        skill.updateInfo(req.skillName(), req.skillBehaviorType(), req.statSource(),
                req.triggerSource(), req.triggerEveryN(), req.triggerBaseSkillKey(), req.note());
        return SetGrantedSkillResponse.from(skill);
    }

    /** 세트 부여 스킬 삭제 */
    @Transactional
    public void deleteSkill(Long id) {
        SetGrantedSkill skill = findSkillOrThrow(id);
        setGrantedSkillRepository.delete(skill);
    }

    // ── EquipmentSetSkillEffect CRUD ──────────────────────────────────

    /** 특정 세트의 스킬 효과 목록 */
    @Transactional(readOnly = true)
    public List<SkillEffectResponse> getSkillEffects(Long setId) {
        findSetOrThrow(setId); // 세트 존재 확인
        return skillEffectRepository.findByEquipmentSetId(setId)
                .stream().map(SkillEffectResponse::from).toList();
    }

    /** 특정 세트에 스킬 효과 추가 */
    @Transactional
    public SkillEffectResponse createSkillEffect(Long setId, SkillEffectCreateRequest req) {
        EquipmentSet set = findSetOrThrow(setId);
        SetGrantedSkill skill = findSkillOrThrow(req.setGrantedSkillId());
        EquipmentSetSkillEffect effect = EquipmentSetSkillEffect.builder()
                .equipmentSet(set)
                .requiredPieces(req.requiredPieces())
                .enhancement(req.enhancement())
                .setGrantedSkill(skill)
                .build();
        return SkillEffectResponse.from(skillEffectRepository.save(effect));
    }

    /** 특정 세트의 스킬 효과 삭제 */
    @Transactional
    public void deleteSkillEffect(Long setId, Long effectId) {
        EquipmentSetSkillEffect effect = skillEffectRepository.findById(effectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "스킬 효과를 찾을 수 없습니다. id=" + effectId));
        if (!effect.getEquipmentSet().getId().equals(setId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "해당 세트의 스킬 효과가 아닙니다. setId=" + setId);
        }
        skillEffectRepository.delete(effect);
    }

    // ── private ──────────────────────────────────────────────────────

    private SetGrantedSkill findSkillOrThrow(Long id) {
        return setGrantedSkillRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "세트 부여 스킬을 찾을 수 없습니다. id=" + id));
    }

    private EquipmentSet findSetOrThrow(Long id) {
        return equipmentSetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "세트를 찾을 수 없습니다. id=" + id));
    }
}
