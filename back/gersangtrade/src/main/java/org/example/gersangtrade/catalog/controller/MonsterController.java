package org.example.gersangtrade.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.dto.response.MonsterAutocompleteResponse;
import org.example.gersangtrade.catalog.dto.response.MonsterResponse;
import org.example.gersangtrade.catalog.service.MonsterService;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 몬스터 조회 API.
 *
 * <pre>
 * GET /api/monsters            — 몬스터 목록 조회 (?element=WATER 등 필터 가능)
 * GET /api/monsters/search     — 몬스터 이름 자동완성 (?q=드래곤&limit=10)
 * GET /api/monsters/{id}       — 몬스터 단건 조회
 * </pre>
 */
@RestController
@RequestMapping("/api/monsters")
@RequiredArgsConstructor
public class MonsterController {

    private final MonsterService monsterService;

    /**
     * 몬스터 목록 조회.
     * element 파라미터로 속성 필터링 가능 (WATER, FIRE, THUNDER, WIND, EARTH, NONE).
     */
    @GetMapping
    public ResponseEntity<List<MonsterResponse>> getMonsters(
            @RequestParam(required = false) Element element) {
        return ResponseEntity.ok(monsterService.getMonsters(element));
    }

    /**
     * 몬스터 이름 자동완성.
     * q: 검색어 (필수), limit: 최대 결과 수 (기본 10, 최대 20)
     */
    @GetMapping("/search")
    public ResponseEntity<List<MonsterAutocompleteResponse>> searchMonsters(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.min(limit, 20);
        return ResponseEntity.ok(monsterService.searchMonsters(q, safeLimit));
    }

    /**
     * 몬스터 단건 조회.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MonsterResponse> getMonster(@PathVariable Long id) {
        return ResponseEntity.ok(monsterService.getMonster(id));
    }
}
