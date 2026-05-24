package org.example.gersangtrade.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.setgrantedskill.*;
import org.example.gersangtrade.admin.service.SetGrantedSkillAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 세트 부여 스킬 / 세트 스킬 효과 관리 API.
 *
 * <ul>
 *   <li>GET    /admin/set-granted-skills             — 목록 (페이징)</li>
 *   <li>GET    /admin/set-granted-skills/{id}        — 단건 조회</li>
 *   <li>POST   /admin/set-granted-skills             — 생성</li>
 *   <li>PUT    /admin/set-granted-skills/{id}        — 수정</li>
 *   <li>DELETE /admin/set-granted-skills/{id}        — 삭제</li>
 *   <li>GET    /admin/sets/{setId}/skill-effects     — 세트별 스킬 효과 목록</li>
 *   <li>POST   /admin/sets/{setId}/skill-effects     — 세트별 스킬 효과 추가</li>
 *   <li>DELETE /admin/sets/{setId}/skill-effects/{effectId} — 스킬 효과 삭제</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SetGrantedSkillAdminController {

    private final SetGrantedSkillAdminService service;

    // ── SetGrantedSkill ───────────────────────────────────────────────

    @GetMapping("/admin/set-granted-skills")
    public ResponseEntity<Page<SetGrantedSkillResponse>> getSkills(
            @PageableDefault(size = 30, sort = "skillName") Pageable pageable) {
        return ResponseEntity.ok(service.getSkills(pageable));
    }

    @GetMapping("/admin/set-granted-skills/{id}")
    public ResponseEntity<SetGrantedSkillResponse> getSkill(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSkill(id));
    }

    @PostMapping("/admin/set-granted-skills")
    public ResponseEntity<SetGrantedSkillResponse> createSkill(
            @RequestBody @Valid SetGrantedSkillCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSkill(req));
    }

    @PutMapping("/admin/set-granted-skills/{id}")
    public ResponseEntity<SetGrantedSkillResponse> updateSkill(
            @PathVariable Long id,
            @RequestBody @Valid SetGrantedSkillUpdateRequest req) {
        return ResponseEntity.ok(service.updateSkill(id, req));
    }

    @DeleteMapping("/admin/set-granted-skills/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        service.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }

    // ── EquipmentSetSkillEffect ───────────────────────────────────────

    @GetMapping("/admin/sets/{setId}/skill-effects")
    public ResponseEntity<List<SkillEffectResponse>> getSkillEffects(@PathVariable Long setId) {
        return ResponseEntity.ok(service.getSkillEffects(setId));
    }

    @PostMapping("/admin/sets/{setId}/skill-effects")
    public ResponseEntity<SkillEffectResponse> createSkillEffect(
            @PathVariable Long setId,
            @RequestBody @Valid SkillEffectCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSkillEffect(setId, req));
    }

    @DeleteMapping("/admin/sets/{setId}/skill-effects/{effectId}")
    public ResponseEntity<Void> deleteSkillEffect(
            @PathVariable Long setId,
            @PathVariable Long effectId) {
        service.deleteSkillEffect(setId, effectId);
        return ResponseEntity.noContent().build();
    }
}
