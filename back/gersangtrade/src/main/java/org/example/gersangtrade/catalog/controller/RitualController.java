package org.example.gersangtrade.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.dto.RitualResponse;
import org.example.gersangtrade.catalog.service.ItemSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 주술 조회 API 컨트롤러.
 *
 * GET /api/rituals — 전체 주술 목록 (거래 페이지 주술 필터용, 비로그인 허용)
 */
@RestController
@RequestMapping("/api/rituals")
@RequiredArgsConstructor
public class RitualController {

    private final ItemSearchService itemSearchService;

    @GetMapping
    public ResponseEntity<List<RitualResponse>> getAll() {
        return ResponseEntity.ok(itemSearchService.findAllRituals());
    }
}
