package org.example.gersangtrade.admin.dto.response;

import java.util.List;

public record ItemBulkDeleteResponse(
        int deletedCount,
        List<Long> deletedIds,
        List<FailedEntry> failed
) {
    public record FailedEntry(Long itemId, String reason) {}
}
