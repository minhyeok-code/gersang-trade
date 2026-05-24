package org.example.gersangtrade.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.dto.response.MercenaryResponse;
import org.example.gersangtrade.catalog.service.MercenaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 공개 용병 목록 API — DPS 계산기 용병 선택 화면에서 사용.
 *
 * <pre>
 * GET /api/mercenaries — 용병 목록 (인증 불필요)
 * </pre>
 */
@RestController
@RequestMapping("/api/mercenaries")
@RequiredArgsConstructor
public class MercenaryController {

    private final MercenaryService mercenaryService;

    /**
     * 용병 목록 조회.
     *
     * @param element 속성 필터 — FIRE | WATER | THUNDER | WIND | EARTH (생략 시 전체)
     * @param q       이름 검색어 (생략 시 전체)
     * @param limit   최대 반환 수 (생략 시 전체)
     */
    @GetMapping
    public ResponseEntity<List<MercenaryResponse>> listMercenaries(
            @RequestParam(required = false) String element,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(mercenaryService.listMercenaries(element, q, limit));
    }
}
