package org.example.gersangtrade.admin.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 스킬 전체 교체 요청 (아이템/용병 공통) */
public record SkillReplaceRequest(
        @NotNull List<String> skills
) {}
