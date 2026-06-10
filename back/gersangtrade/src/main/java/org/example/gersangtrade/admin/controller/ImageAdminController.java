package org.example.gersangtrade.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.response.GemImageTargetResponse;
import org.example.gersangtrade.admin.dto.response.ImageUploadResponse;
import org.example.gersangtrade.admin.dto.response.ItemImageTargetResponse;
import org.example.gersangtrade.admin.dto.response.MercenaryImageTargetResponse;
import org.example.gersangtrade.admin.service.ImageAdminService;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 관리자 이미지 업로드 API.
 *
 * <p>엔드포인트:
 * <ul>
 *   <li>GET  /admin/images/items                  — 아이템 검색 (name·type 필터, 페이징)</li>
 *   <li>GET  /admin/images/items/missing           — 거래 이력 있으나 이미지 없는 장비 목록</li>
 *   <li>POST /admin/images/items/{itemId}          — 아이템 이미지 업로드</li>
 *   <li>GET  /admin/images/gems                    — 보석 검색 (name 필터, 페이징)</li>
 *   <li>GET  /admin/images/gems/missing            — 이미지 없는 보석 전체 목록</li>
 *   <li>POST /admin/images/gems/{gemId}            — 보석 이미지 업로드</li>
 *   <li>GET  /admin/images/mercenaries             — 용병 검색 (name 필터, 페이징)</li>
 *   <li>GET  /admin/images/mercenaries/missing     — 이미지 없는 용병 전체 목록</li>
 *   <li>POST /admin/images/mercenaries/{mercenaryId} — 용병 이미지 업로드</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/images")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ImageAdminController {

    private final ImageAdminService imageAdminService;

    // ── 아이템 ───────────────────────────────────────────────────────────────────

    /**
     * 아이템 검색.
     * type 생략 시 재료·장비 모두 반환한다.
     *
     * @param type MATERIAL | EQUIPMENT (생략 시 전체)
     * @param name 이름 부분 일치 (생략 시 전체)
     */
    @GetMapping("/items")
    public ResponseEntity<Page<ItemImageTargetResponse>> searchItems(
            @RequestParam(required = false) ItemType type,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 30, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(imageAdminService.searchItems(type, name, pageable));
    }

    /** 거래 이력(매물·확정)이 있으나 이미지가 없는 장비 아이템 목록. */
    @GetMapping("/items/missing")
    public ResponseEntity<List<ItemImageTargetResponse>> missingItemImages() {
        return ResponseEntity.ok(imageAdminService.missingItemImages());
    }

    /** 아이템 이미지 업로드 — multipart/form-data, 파라미터명: file */
    @PostMapping(value = "/items/{itemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadItemImage(
            @PathVariable Long itemId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imageAdminService.uploadItemImage(itemId, file));
    }

    // ── 보석 ───────────────────────────────────────────────────────────────────

    /**
     * 보석 검색.
     *
     * @param name 이름 부분 일치 (생략 시 전체)
     */
    @GetMapping("/gems")
    public ResponseEntity<Page<GemImageTargetResponse>> searchGems(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 30, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(imageAdminService.searchGems(name, pageable));
    }

    /** 이미지가 없는 보석 전체 목록. */
    @GetMapping("/gems/missing")
    public ResponseEntity<List<GemImageTargetResponse>> missingGemImages() {
        return ResponseEntity.ok(imageAdminService.missingGemImages());
    }

    /** 보석 이미지 업로드 — multipart/form-data, 파라미터명: file */
    @PostMapping(value = "/gems/{gemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadGemImage(
            @PathVariable Long gemId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imageAdminService.uploadGemImage(gemId, file));
    }

    // ── 용병 ───────────────────────────────────────────────────────────────────

    /**
     * 용병 검색.
     *
     * @param name 이름 부분 일치 (생략 시 전체)
     */
    @GetMapping("/mercenaries")
    public ResponseEntity<Page<MercenaryImageTargetResponse>> searchMercenaries(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 30, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(imageAdminService.searchMercenaries(name, pageable));
    }

    /** 이미지가 없는 용병 전체 목록. */
    @GetMapping("/mercenaries/missing")
    public ResponseEntity<List<MercenaryImageTargetResponse>> missingMercenaryImages() {
        return ResponseEntity.ok(imageAdminService.missingMercenaryImages());
    }

    /** 용병 이미지 업로드 — multipart/form-data, 파라미터명: file */
    @PostMapping(value = "/mercenaries/{mercenaryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadMercenaryImage(
            @PathVariable Long mercenaryId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imageAdminService.uploadMercenaryImage(mercenaryId, file));
    }
}
