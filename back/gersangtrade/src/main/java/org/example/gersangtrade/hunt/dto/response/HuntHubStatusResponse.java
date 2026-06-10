package org.example.gersangtrade.hunt.dto.response;

/**
 * 사냥 허브 해금 상태 응답 DTO.
 */
public record HuntHubStatusResponse(
        int distinctMonsterCount,
        int requiredDistinctMonsters,
        boolean unlocked
) {
}
