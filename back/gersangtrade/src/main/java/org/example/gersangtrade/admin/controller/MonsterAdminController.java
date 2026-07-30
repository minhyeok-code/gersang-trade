package org.example.gersangtrade.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.monster.MonsterAdminResponse;
import org.example.gersangtrade.admin.dto.monster.MonsterCreateRequest;
import org.example.gersangtrade.admin.dto.monster.MonsterUpdateRequest;
import org.example.gersangtrade.admin.service.MonsterAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 몬스터 관리자 API.
 *
 * <ul>
 *   <li>GET    /admin/monsters               — 목록 (name 검색, 페이징)</li>
 *   <li>GET    /admin/monsters/{id}          — 단건 조회</li>
 *   <li>POST   /admin/monsters               — 등록 (201)</li>
 *   <li>PUT    /admin/monsters/{id}          — 수정</li>
 *   <li>PATCH  /admin/monsters/{id}/hidden   — 숨김 여부 수동 설정</li>
 *   <li>DELETE /admin/monsters/{id}          — 삭제 (204)</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/monsters")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class MonsterAdminController {

    private final MonsterAdminService monsterAdminService;

    /**
     * 몬스터 목록 조회.
     *
     * @param name 이름 부분 검색 (생략 시 전체)
     */
    @GetMapping
    public ResponseEntity<Page<MonsterAdminResponse>> list(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 30, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(monsterAdminService.list(name, pageable));
    }

    /** 몬스터 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<MonsterAdminResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(monsterAdminService.get(id));
    }

    /** 몬스터 신규 등록 — 이름 중복 시 409 */
    @PostMapping
    public ResponseEntity<MonsterAdminResponse> create(
            @Valid @RequestBody MonsterCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(monsterAdminService.create(req));
    }

    /** 몬스터 수정 — null 필드는 기존 값 유지 */
    @PutMapping("/{id}")
    public ResponseEntity<MonsterAdminResponse> update(
            @PathVariable Long id,
            @RequestBody MonsterUpdateRequest req) {
        return ResponseEntity.ok(monsterAdminService.update(id, req));
    }

    /**
     * 몬스터 숨김 여부 수동 설정.
     * Body: { "hidden": true } 또는 { "hidden": false }
     */
    @PatchMapping("/{id}/hidden")
    public ResponseEntity<MonsterAdminResponse> setHidden(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Boolean> body) {
        Boolean hidden = body.get("hidden");
        if (hidden == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "hidden 필드가 필요합니다.");
        }
        return ResponseEntity.ok(monsterAdminService.setHidden(id, hidden));
    }

    /** 몬스터 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        monsterAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
