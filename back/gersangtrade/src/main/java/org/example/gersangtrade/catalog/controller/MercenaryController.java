package org.example.gersangtrade.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.dto.EquipmentSlotItemResponse;
import org.example.gersangtrade.catalog.dto.response.MercenaryCharacteristicCatalogResponse;
import org.example.gersangtrade.catalog.dto.response.MercenaryCharacteristicSetupResponse;
import org.example.gersangtrade.catalog.dto.response.MercenaryResponse;
import org.example.gersangtrade.catalog.service.ItemSearchService;
import org.example.gersangtrade.catalog.service.MercenaryService;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final ItemSearchService itemSearchService;

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

    /** 용병 특성 카탈로그 — 스냅샷 뷰어 이름·레벨 라벨 조회용 */
    @GetMapping("/{mercenaryId}/characteristics")
    public ResponseEntity<MercenaryCharacteristicCatalogResponse> getCharacteristicCatalog(
            @PathVariable Long mercenaryId) {
        return ResponseEntity.ok(mercenaryService.getCharacteristicCatalog(mercenaryId));
    }

    /** 용병 특성 배분 UI — 가성비 시뮬레이션·후보 용병 설정용 */
    @GetMapping("/{mercenaryId}/characteristics/setup")
    public ResponseEntity<MercenaryCharacteristicSetupResponse> getCharacteristicSetup(
            @PathVariable Long mercenaryId) {
        return ResponseEntity.ok(mercenaryService.getCharacteristicSetup(mercenaryId));
    }

    /**
     * 용병 전용장비 목록 — 덱 장비 선택 UI용.
     * restriction·mercenary_id 기준으로 조회하므로 슬롯 인덱스 불일치 전용무기도 포함된다.
     */
    @GetMapping("/{mercenaryId}/exclusive-equipment")
    public ResponseEntity<List<EquipmentSlotItemResponse>> getExclusiveEquipment(
            @PathVariable Long mercenaryId,
            @RequestParam EquipSlot slot) {
        return ResponseEntity.ok(itemSearchService.getExclusiveEquipmentForMercenary(mercenaryId, slot));
    }
}
