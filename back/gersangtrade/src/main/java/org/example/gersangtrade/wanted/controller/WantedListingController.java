package org.example.gersangtrade.wanted.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.domain.wanted.enums.WantedStatus;
import org.example.gersangtrade.wanted.dto.request.WantedListingCreateRequest;
import org.example.gersangtrade.wanted.dto.request.WantedSearchCondition;
import org.example.gersangtrade.wanted.dto.response.WantedListingDetailResponse;
import org.example.gersangtrade.wanted.dto.response.WantedListingSummaryResponse;
import org.example.gersangtrade.wanted.service.WantedListingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * 구매 희망 등록글 API 컨트롤러.
 *
 * <pre>
 * POST   /api/wanted              — 구매 희망 등록 (로그인 필수)
 * GET    /api/wanted              — 구매 희망 목록 조회 (비로그인 허용)
 * GET    /api/wanted/{wantedId}   — 구매 희망 상세 조회 (비로그인 허용)
 * DELETE /api/wanted/{wantedId}   — 구매 희망 취소 (본인)
 * </pre>
 */
@RestController
@RequestMapping("/api/wanted")
@RequiredArgsConstructor
public class WantedListingController {

    private final WantedListingService wantedListingService;

    /**
     * 구매 희망 등록글 신규 등록.
     * 등록 성공 시 201 Created와 Location 헤더를 반환한다.
     *
     * @param buyerId 인증된 구매자 ID (JWT principal)
     * @param request 등록 요청 DTO
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal Long buyerId,
            @Valid @RequestBody WantedListingCreateRequest request
    ) {
        Long wantedId = wantedListingService.createWantedListing(buyerId, request);
        URI location = URI.create("/api/wanted/" + wantedId);
        return ResponseEntity.created(location).build();
    }

    /**
     * 구매 희망 등록글 목록 조회.
     * 비로그인 사용자도 조회 가능하다.
     * status 미입력 시 OPEN 기본값이 적용된다.
     *
     * @param server  서버 필터 (선택)
     * @param status  상태 필터 (선택, 기본 OPEN)
     * @param keyword 아이템명 키워드 (선택)
     * @param page    페이지 번호 (0부터, 기본 0)
     * @param size    페이지당 결과 수 (기본 20, 최대 50)
     */
    @GetMapping
    public ResponseEntity<List<WantedListingSummaryResponse>> getWantedListings(
            @RequestParam(required = false) String server,
            @RequestParam(required = false) WantedStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        WantedStatus resolvedStatus = (status != null) ? status : WantedStatus.OPEN;

        WantedSearchCondition cond = new WantedSearchCondition(
                server, resolvedStatus, keyword, page, size);
        return ResponseEntity.ok(wantedListingService.getWantedListings(cond));
    }

    /**
     * 구매 희망 등록글 상세 조회.
     * 비로그인 사용자도 조회 가능하다.
     *
     * @param wantedId 등록글 ID
     */
    @GetMapping("/{wantedId}")
    public ResponseEntity<WantedListingDetailResponse> getDetail(@PathVariable Long wantedId) {
        return ResponseEntity.ok(wantedListingService.getDetail(wantedId));
    }

    /**
     * 구매 희망 등록글 취소.
     * 본인 등록글만 취소 가능하다.
     *
     * @param buyerId  인증된 구매자 ID (JWT principal)
     * @param wantedId 취소 대상 등록글 ID
     */
    @DeleteMapping("/{wantedId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal Long buyerId,
            @PathVariable Long wantedId
    ) {
        wantedListingService.cancelWantedListing(buyerId, wantedId);
        return ResponseEntity.noContent().build();
    }
}
