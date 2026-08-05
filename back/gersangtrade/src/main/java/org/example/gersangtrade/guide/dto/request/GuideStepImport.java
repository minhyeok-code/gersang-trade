package org.example.gersangtrade.guide.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.example.gersangtrade.domain.guide.enums.GuideStepType;

/**
 * 주입할 스텝 한 건.
 * link가 있으면 kind(ITEM|SET|MERCENARY)에 따라 matchName으로 카탈로그를 이름 매칭한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuideStepImport(
        int order,
        GuideStepType stepType,
        String label,
        String note,
        GuideStepLink link
) {}
