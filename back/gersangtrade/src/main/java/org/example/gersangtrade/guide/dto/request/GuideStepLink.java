package org.example.gersangtrade.guide.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 스텝의 카탈로그 연결 후보.
 * kind: "ITEM" | "SET" | "MERCENARY", matchName: 카탈로그 이름 검색어.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuideStepLink(
        String kind,
        String matchName
) {}
