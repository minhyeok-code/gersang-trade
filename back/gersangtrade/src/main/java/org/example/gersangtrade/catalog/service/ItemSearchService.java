package org.example.gersangtrade.catalog.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.dto.ItemSearchResult;
import org.example.gersangtrade.catalog.dto.RitualResponse;
import org.example.gersangtrade.catalog.repository.ItemJooqRepository;
import org.example.gersangtrade.catalog.repository.RitualApplicabilityRepository;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 아이템 검색 및 주술 목록 조회 서비스.
 *
 * 자동완성 검색은 jOOQ ranked 검색을 기본으로 사용한다:
 * - starts-with 매칭이 contains 매칭보다 먼저 노출
 * - LEFT JOIN으로 equipment/material 정보를 한 번에 조회
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemSearchService {

    /** 자동완성 기본 최대 결과 수 */
    private static final int DEFAULT_SEARCH_LIMIT = 20;

    private final ItemJooqRepository itemJooqRepository;
    private final RitualApplicabilityRepository ritualApplicabilityRepository;

    /**
     * 아이템 자동완성 검색.
     * 사용자가 아이템 이름 일부를 입력하면 연관 아이템 목록을 반환한다.
     * starts-with 매칭이 상단에 노출된다.
     *
     * @param keyword 검색 키워드
     * @param type    타입 필터 (null이면 전체)
     * @param kind    장비 종류 필터 (null이면 전체)
     * @param limit   최대 결과 수 (null이면 기본값 20)
     */
    public List<ItemSearchResult> search(String keyword, ItemType type,
                                         EquipmentKind kind, Integer limit) {
        int maxResults = (limit != null && limit > 0 && limit <= 50) ? limit : DEFAULT_SEARCH_LIMIT;
        return itemJooqRepository.searchRanked(keyword, type, kind, maxResults);
    }

    /**
     * 특정 장비 아이템에 적용 가능한 주술 목록 조회.
     * 매물 등록 시 주술 선택 드롭다운 구성에 사용된다.
     *
     * @param equipmentItemId EquipmentItem의 ID (= Item.id)
     */
    public List<RitualResponse> findAvailableRituals(Long equipmentItemId) {
        return ritualApplicabilityRepository
                .findByEquipmentItemIdWithRitual(equipmentItemId)
                .stream()
                .map(ra -> RitualResponse.from(ra.getRitual()))
                .toList();
    }
}
