package org.example.gersangtrade.trade.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.trade.dto.request.TradeReviewSubmitRequest;
import org.example.gersangtrade.trade.dto.response.TradeReviewResponse;
import org.example.gersangtrade.trade.service.TradeReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 거래 평가 컨트롤러.
 * 평가 제출 및 내가 받은 평가 조회 엔드포인트를 제공한다.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class TradeReviewController {

    private final TradeReviewService tradeReviewService;

    /**
     * 거래 평가 제출.
     * 평가 기간(revealAt 이전) 내, 미제출 상태에서만 가능.
     *
     * POST /api/reviews/{reviewId}
     */
    @PostMapping("/{reviewId}")
    public ResponseEntity<Void> submit(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long reviewId,
            @RequestBody @Valid TradeReviewSubmitRequest request
    ) {
        tradeReviewService.submit(userId, reviewId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 내가 받은 공개된 평가 목록 조회 (published=true).
     *
     * GET /api/reviews/received
     */
    @GetMapping("/received")
    public List<TradeReviewResponse> getMyReceivedReviews(
            @AuthenticationPrincipal Long userId
    ) {
        return tradeReviewService.getMyReceivedReviews(userId);
    }
}
