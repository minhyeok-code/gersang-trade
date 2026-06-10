package org.example.gersangtrade.catalog.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.dto.EquipmentSlotItemResponse;
import org.example.gersangtrade.catalog.dto.ItemSearchResult;
import org.example.gersangtrade.catalog.dto.RitualResponse;
import org.example.gersangtrade.catalog.service.ItemSearchService;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 아이템 검색 및 주술 조회 API 컨트롤러.
 *
 * GET /api/items/search?q=키워드[&type=EQUIPMENT][&kind=NORMAL][&limit=20]
 *   — 자동완성 검색 (비로그인 허용)
 *
 * GET /api/items/{itemId}/rituals
 *   — 해당 장비에 적용 가능한 주술 목록 (비로그인 허용)
 *
 * GET /api/items/equipment?slot={EquipSlot}
 *   — 덱 슬롯별 착용 가능 장비 목록 (비로그인 허용)
 *
 * GET /api/rituals
 *   — 전체 주술 목록 (거래 페이지 필터용)
 */
@Validated
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemSearchController {

    private final ItemSearchService itemSearchService;

    /**
     * 아이템 자동완성 검색.
     * 사용자가 아이템 이름 일부만 입력해도 연관 아이템을 찾을 수 있다.
     * starts-with 매칭이 contains 매칭보다 상단에 노출된다.
     *
     * @param q     검색 키워드 (필수, 1자 이상)
     * @param type  아이템 타입 필터 MATERIAL | EQUIPMENT (선택)
     * @param kind  장비 종류 필터 APPEARANCE | NORMAL (선택)
     * @param limit 최대 결과 수 (기본 20, 최대 50)
     */
    @GetMapping("/search")
    public ResponseEntity<List<ItemSearchResult>> search(
            @RequestParam @NotBlank String q,
            @RequestParam(required = false) ItemType type,
            @RequestParam(required = false) EquipmentKind kind,
            @RequestParam(required = false) Integer limit
    ) {
        List<ItemSearchResult> results = itemSearchService.search(q, type, kind, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * 특정 장비 아이템에 적용 가능한 주술 목록 조회.
     * 매물 등록 UI에서 주술 선택 드롭다운을 구성할 때 사용된다.
     *
     * @param itemId EquipmentItem의 Item ID
     */
    @GetMapping("/{itemId}/rituals")
    public ResponseEntity<List<RitualResponse>> getRituals(@PathVariable Long itemId) {
        List<RitualResponse> rituals = itemSearchService.findAvailableRituals(itemId);
        return ResponseEntity.ok(rituals);
    }

    /**
     * 장비 목록 조회.
     * slot 생략 시 전체 장비 반환 (덱 설정 페이지 초기 로딩용).
     * slot 지정 시 해당 슬롯 착용 가능 장비만 반환.
     *
     * @param slot 덱 장비 슬롯 (예: HELMET, WEAPON, APP_SPIRIT, RING_1 등) — 선택
     */
    @GetMapping("/equipment")
    public ResponseEntity<List<EquipmentSlotItemResponse>> getEquipment(
            @RequestParam(required = false) EquipSlot slot) {
        if (slot == null) {
            return ResponseEntity.ok(itemSearchService.getAllEquipment());
        }
        return ResponseEntity.ok(itemSearchService.getEquipmentBySlot(slot));
    }
}
