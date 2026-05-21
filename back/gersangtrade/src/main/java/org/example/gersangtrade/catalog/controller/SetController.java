package org.example.gersangtrade.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.dto.response.SetResponse;
import org.example.gersangtrade.catalog.service.SetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 유저용 장비 세트 카탈로그 API.
 *
 * <ul>
 *   <li>GET /api/sets       — 목록 (이름 검색, isTradeable=true만 노출)</li>
 *   <li>GET /api/sets/{id}  — 단건 조회</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/sets")
@RequiredArgsConstructor
public class SetController {

    private final SetService setService;

    /**
     * 세트 목록.
     *
     * @param name 이름 부분 검색 (생략 시 전체)
     */
    @GetMapping
    public ResponseEntity<Page<SetResponse>> getSets(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 30, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(setService.getSets(name, pageable));
    }

    /** 세트 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<SetResponse> getSet(@PathVariable Long id) {
        return ResponseEntity.ok(setService.getSet(id));
    }
}
