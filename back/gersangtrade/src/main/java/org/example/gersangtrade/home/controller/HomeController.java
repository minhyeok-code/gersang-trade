package org.example.gersangtrade.home.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.home.dto.PriceWatchResponse;
import org.example.gersangtrade.home.exception.PriceWatchException;
import org.example.gersangtrade.home.service.PriceWatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final PriceWatchService priceWatchService;

    /**
     * 관심 아이템 시세 조회.
     * serverId는 필수 파라미터이며 누락 시 400으로 고정한다.
     */
    @GetMapping("/price-watch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PriceWatchResponse> getPriceWatch(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer serverId) {

        if (serverId == null || serverId <= 0) {
            throw new PriceWatchException(
                    HttpStatus.BAD_REQUEST,
                    "SERVER_ID_REQUIRED",
                    "serverId 쿼리 파라미터가 필요합니다.");
        }
        return ResponseEntity.ok(priceWatchService.getPriceWatch(userId, serverId));
    }

    @ExceptionHandler(PriceWatchException.class)
    public ResponseEntity<Map<String, Object>> handlePriceWatchException(PriceWatchException ex) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(Map.of(
                        "errorCode", ex.getErrorCode(),
                        "error", ex.getHttpStatus().getReasonPhrase().toLowerCase().replace(' ', '_'),
                        "message", ex.getMessage()
                ));
    }
}
