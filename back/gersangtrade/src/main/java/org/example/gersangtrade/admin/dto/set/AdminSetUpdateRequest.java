package org.example.gersangtrade.admin.dto.set;

public record AdminSetUpdateRequest(
        String name,
        Integer totalPieces,
        boolean isTradeable
) {}
