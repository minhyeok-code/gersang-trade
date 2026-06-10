package org.example.gersangtrade.admin.dto.response;

import org.example.gersangtrade.admin.dto.enums.ItemCleanupCriterion;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.example.gersangtrade.domain.catalog.enums.ItemType;

import java.util.List;

/** 크롤링 정리 후보 아이템 1건 */
public record ItemCleanupCandidateResponse(
        Long id,
        String name,
        ItemType type,
        EquipmentSlot slot,
        String setName,
        int statCount,
        List<ItemCleanupCriterion> matchedCriteria
) {}
