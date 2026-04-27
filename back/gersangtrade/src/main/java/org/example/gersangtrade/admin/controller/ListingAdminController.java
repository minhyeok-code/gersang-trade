package org.example.gersangtrade.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.listing.service.ListingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 거래 등록글 관리자 API 컨트롤러.
 *
 * <pre>
 * PATCH  /admin/listings/{id}/hide    — 등록글 숨김 처리
 * PATCH  /admin/listings/{id}/unhide  — 등록글 숨김 해제
 * </pre>
 *
 * <p>모든 엔드포인트는 ADMIN 권한이 필요하다.
 */
@RestController
@RequestMapping("/admin/listings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ListingAdminController {

    private final ListingService listingService;

    /**
     * 거래 등록글 숨김 처리.
     * 정책 위반 등의 이유로 관리자가 등록글을 숨긴다.
     *
     * @param listingId 숨김 처리 대상 등록글 ID
     * @return 204 No Content
     */
    @PatchMapping("/{listingId}/hide")
    public ResponseEntity<Void> hideListing(@PathVariable Long listingId) {
        listingService.hideListing(listingId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 거래 등록글 숨김 해제.
     *
     * @param listingId 숨김 해제 대상 등록글 ID
     * @return 204 No Content
     */
    @PatchMapping("/{listingId}/unhide")
    public ResponseEntity<Void> unhideListing(@PathVariable Long listingId) {
        listingService.unhideListing(listingId);
        return ResponseEntity.noContent().build();
    }
}
