package org.example.gersangtrade.admin.dto.monster;

import org.example.gersangtrade.domain.catalog.enums.Element;

/** 몬스터 수정 요청 — 모든 필드 nullable, 있는 값만 업데이트 */
public record MonsterUpdateRequest(
        String name,
        Long hp,
        Integer hittingResistance,
        Integer magicResistance,
        Integer elementValue,
        Element element
) {}
