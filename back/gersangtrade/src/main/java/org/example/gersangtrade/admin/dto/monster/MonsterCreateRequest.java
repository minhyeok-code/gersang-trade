package org.example.gersangtrade.admin.dto.monster;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.gersangtrade.domain.catalog.enums.Element;

/** 몬스터 신규 등록 요청 */
public record MonsterCreateRequest(
        @NotBlank @Size(max = 100) String name,
        Long hp,
        Integer hittingResistance,
        Integer magicResistance,
        Integer elementValue,
        Element element
) {}
