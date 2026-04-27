package org.example.gersangtrade.admin.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.gersangtrade.domain.catalog.enums.StatType;

import java.util.List;

/**
 * 특성 레벨 수치 일괄 저장 요청.
 * PUT 의미론 — 기존 레벨 전체를 삭제하고 요청 목록으로 재적재한다.
 *
 * @param levels 레벨 수치 목록
 */
public record CharacteristicLevelSaveRequest(
        @NotNull @Valid List<LevelEntry> levels
) {
    /**
     * 단일 레벨 수치 항목.
     *
     * @param label       수치 항목명 (예: "풍극진멸 데미지", "타격저항력")
     * @param level       레벨 (1부터 시작)
     * @param amount      원본 수치 문자열 (예: "20%", "500")
     * @param statType    계산기 합산 대상 스탯 종류 (선택 — null이면 계산기에서 제외)
     */
    public record LevelEntry(
            @NotBlank @Size(max = 100) String label,
            @NotNull @Min(1) Integer level,
            @NotBlank @Size(max = 20) String amount,
            StatType statType
    ) {}
}
