package org.example.gersangtrade.listing.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.domain.listing.enums.BundleType;
import org.example.gersangtrade.domain.listing.enums.ListingStatus;
import org.example.gersangtrade.listing.dto.request.ListingCreateRequest;
import org.example.gersangtrade.listing.dto.request.ListingSearchCondition;
import org.example.gersangtrade.listing.dto.response.ListingDetailResponse;
import org.example.gersangtrade.listing.dto.response.ListingSummaryResponse;
import org.example.gersangtrade.listing.service.ListingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * 거래 등록글 API 컨트롤러.
 *
 * <pre>
 * POST   /api/listings              — 등록글 신규 등록 (로그인 필수)
 * GET    /api/listings              — 등록글 목록 조회 (비로그인 허용)
 * GET    /api/listings/{listingId}  — 등록글 상세 조회 (비로그인 허용)
 * DELETE /api/listings/{listingId}  — 등록글 소프트 삭제 (본인 또는 관리자)
 * </pre>
 */
@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    /**
     * 거래 등록글 신규 등록.
     * 로그인한 사용자만 호출 가능하다.
     * 등록 성공 시 201 Created와 Location 헤더를 반환한다.
     *
     * @param userDetails 인증된 사용자 정보 (Spring Security)
     * @param request     등록 요청 DTO
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ListingCreateRequest request
    ) {
        // JWT 인증 시 principal은 Long userId (JwtAuthenticationFilter 참고)
        Long listingId = listingService.createListing(userId, request);
        URI location = URI.create("/api/listings/" + listingId);
        return ResponseEntity.created(location).build();
    }

    /**
     * 거래 등록글 목록 조회.
     * 비로그인 사용자도 조회 가능하다.
     * 조건이 없으면 최신순 전체 ACTIVE 목록을 반환한다.
     *
     * @param server     서버 필터 (선택)
     * @param status     상태 필터 (선택, 미입력 시 ACTIVE)
     * @param bundleType 번들 유형 필터 (선택)
     * @param keyword    아이템명 키워드 (선택)
     * @param page       페이지 번호 (0부터, 기본 0)
     * @param size       페이지당 결과 수 (기본 20, 최대 50)
     */
    @GetMapping
    public ResponseEntity<List<ListingSummaryResponse>> getListings(
            @RequestParam(required = false) String server,
            @RequestParam(required = false) ListingStatus status,
            @RequestParam(required = false) BundleType bundleType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        // status 미입력 시 ACTIVE 기본값 적용
        ListingStatus resolvedStatus = (status != null) ? status : ListingStatus.ACTIVE;

        ListingSearchCondition cond = new ListingSearchCondition(
                server, resolvedStatus, bundleType, keyword, page, size);
        List<ListingSummaryResponse> result = listingService.getListings(cond);
        return ResponseEntity.ok(result);
    }

    /**
     * 거래 등록글 상세 조회.
     * 비로그인 사용자도 조회 가능하다.
     *
     * @param listingId 등록글 ID
     */
    @GetMapping("/{listingId}")
    public ResponseEntity<ListingDetailResponse> getDetail(@PathVariable Long listingId) {
        return ResponseEntity.ok(listingService.getDetail(listingId));
    }

    /**
     * 거래 등록글 소프트 삭제 (판매자 취소).
     * 본인 등록글만 취소 가능하다.
     * 소프트딜리트와 CANCELLED 상태 전환을 서비스에 위임한다.
     *
     * @param userId    인증된 사용자 ID (JWT principal)
     * @param listingId 취소 대상 등록글 ID
     */
    @DeleteMapping("/{listingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long listingId
    ) {
        listingService.cancelListing(userId, listingId);
        return ResponseEntity.noContent().build();
    }
}
