package org.example.gersangtrade.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.request.ItemStatReplaceRequest;
import org.example.gersangtrade.admin.dto.request.ItemUpdateRequest;
import org.example.gersangtrade.admin.dto.request.SkillReplaceRequest;
import org.example.gersangtrade.admin.dto.response.ItemAdminResponse;
import org.example.gersangtrade.admin.dto.response.ItemDetailAdminResponse;
import org.example.gersangtrade.admin.service.ItemAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 아이템 수동 관리 API.
 *
 * <p>크롤링으로 적재된 아이템 데이터의 이름·타입·스탯·스킬을 관리자가 직접 수정한다.
 *
 * <p>엔드포인트:
 * <ul>
 *   <li>GET  /admin/items              — 아이템 목록 (스탯 수 포함)</li>
 *   <li>GET  /admin/items/{id}         — 아이템 상세 (기본정보 + 장비정보 + 스탯 + 스킬)</li>
 *   <li>PUT  /admin/items/{id}         — 아이템 기본정보 수정</li>
 *   <li>PUT  /admin/items/{id}/stats   — 스탯 전체 교체</li>
 *   <li>PUT  /admin/items/{id}/skills  — 스킬 전체 교체</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/items")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ItemAdminController {

    private final ItemAdminService itemAdminService;

    /**
     * 아이템 목록 조회.
     * statCount로 스탯 미입력 아이템을 빠르게 파악할 수 있다.
     */
    @GetMapping
    public ResponseEntity<Page<ItemAdminResponse>> listItems(
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(itemAdminService.listItems(pageable));
    }

    /** 아이템 단건 상세 조회 (기본정보 + 장비정보 + 스탯 + 스킬). */
    @GetMapping("/{itemId}")
    public ResponseEntity<ItemDetailAdminResponse> getItem(
            @PathVariable Long itemId) {
        return ResponseEntity.ok(itemAdminService.getItem(itemId));
    }

    /** 아이템 기본정보 수정 — 이름·타입·거래 카테고리. */
    @PutMapping("/{itemId}")
    public ResponseEntity<ItemDetailAdminResponse> updateInfo(
            @PathVariable Long itemId,
            @Valid @RequestBody ItemUpdateRequest request) {
        return ResponseEntity.ok(itemAdminService.updateInfo(itemId, request));
    }

    /** 아이템 스탯 전체 교체 (PUT 의미론 — 기존 전부 삭제 후 재적재). */
    @PutMapping("/{itemId}/stats")
    public ResponseEntity<ItemDetailAdminResponse> replaceStats(
            @PathVariable Long itemId,
            @Valid @RequestBody ItemStatReplaceRequest request) {
        return ResponseEntity.ok(itemAdminService.replaceStats(itemId, request));
    }

    /** 아이템 스킬 전체 교체 (PUT 의미론 — 기존 전부 삭제 후 재적재). */
    @PutMapping("/{itemId}/skills")
    public ResponseEntity<ItemDetailAdminResponse> replaceSkills(
            @PathVariable Long itemId,
            @Valid @RequestBody SkillReplaceRequest request) {
        return ResponseEntity.ok(itemAdminService.replaceSkills(itemId, request));
    }
}
