package org.example.gersangtrade.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.skill.ItemSkillAdminResponse;
import org.example.gersangtrade.admin.dto.skill.ItemSkillCreateRequest;
import org.example.gersangtrade.admin.dto.skill.ItemSkillUpdateRequest;
import org.example.gersangtrade.admin.service.ItemSkillAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 아이템 스킬 관리자 API.
 *
 * <ul>
 *   <li>GET    /admin/skills        — 목록 (skillName 검색, 페이징)</li>
 *   <li>GET    /admin/skills/{id}   — 단건 조회</li>
 *   <li>POST   /admin/skills        — 등록 (201)</li>
 *   <li>PUT    /admin/skills/{id}   — 수정</li>
 *   <li>DELETE /admin/skills/{id}   — 삭제 (204)</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/skills")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ItemSkillAdminController {

    private final ItemSkillAdminService itemSkillAdminService;

    /**
     * 아이템 스킬 목록 조회.
     *
     * @param skillName 스킬명 부분 검색 (생략 시 전체)
     */
    @GetMapping
    public ResponseEntity<Page<ItemSkillAdminResponse>> list(
            @RequestParam(required = false) String skillName,
            @PageableDefault(size = 30, sort = "skillName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(itemSkillAdminService.list(skillName, pageable));
    }

    /** 아이템 스킬 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<ItemSkillAdminResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(itemSkillAdminService.get(id));
    }

    /** 아이템 스킬 신규 등록 — skillName 중복 시 409 */
    @PostMapping
    public ResponseEntity<ItemSkillAdminResponse> create(
            @Valid @RequestBody ItemSkillCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemSkillAdminService.create(req));
    }

    /** 아이템 스킬 수정 — null 필드는 기존 값 유지 */
    @PutMapping("/{id}")
    public ResponseEntity<ItemSkillAdminResponse> update(
            @PathVariable Long id,
            @RequestBody ItemSkillUpdateRequest req) {
        return ResponseEntity.ok(itemSkillAdminService.update(id, req));
    }

    /** 아이템 스킬 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itemSkillAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
