package org.example.gersangtrade.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.set.AdminSetResponse;
import org.example.gersangtrade.admin.dto.set.AdminSetUpdateRequest;
import org.example.gersangtrade.admin.service.AdminEquipmentSetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 장비 세트 관리 API.
 *
 * <ul>
 *   <li>GET   /admin/sets       — 목록 (이름 검색)</li>
 *   <li>GET   /admin/sets/{id}  — 단건 조회</li>
 *   <li>PATCH /admin/sets/{id}  — 수정 (이름, 피스 수, 거래 노출 여부)</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/sets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEquipmentSetController {

    private final AdminEquipmentSetService adminEquipmentSetService;

    /**
     * 세트 목록.
     *
     * @param name 이름 부분 검색 (생략 시 전체)
     */
    @GetMapping
    public ResponseEntity<Page<AdminSetResponse>> getSets(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 30, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(adminEquipmentSetService.getSets(name, pageable));
    }

    /** 세트 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<AdminSetResponse> getSet(@PathVariable Long id) {
        return ResponseEntity.ok(adminEquipmentSetService.getSet(id));
    }

    /** 세트 수정 (이름, 피스 수, 거래 노출 여부) */
    @PatchMapping("/{id}")
    public ResponseEntity<AdminSetResponse> updateSet(
            @PathVariable Long id,
            @RequestBody AdminSetUpdateRequest req) {
        return ResponseEntity.ok(adminEquipmentSetService.updateSet(id, req));
    }
}
