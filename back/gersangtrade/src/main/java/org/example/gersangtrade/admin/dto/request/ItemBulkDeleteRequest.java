package org.example.gersangtrade.admin.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ItemBulkDeleteRequest(
        @NotEmpty @Size(max = 200) List<Long> itemIds
) {}
