package org.example.gersangtrade.hunt.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.gersangtrade.domain.hunt.DeckSnapshot;

import java.time.LocalDateTime;

/**
 * 공개 덱 스냅샷 조회 응답 DTO.
 */
public record HuntSnapshotResponse(
        Long id,
        JsonNode content,
        LocalDateTime createdAt
) {
    public static HuntSnapshotResponse of(DeckSnapshot snapshot, JsonNode content) {
        return new HuntSnapshotResponse(snapshot.getId(), content, snapshot.getCreatedAt());
    }
}
