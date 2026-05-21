package org.example.gersangtrade.admin.dto.request;

import org.example.gersangtrade.domain.catalog.enums.Nation;
import org.example.gersangtrade.domain.catalog.enums.Nature;

import java.util.List;

/**
 * 용병 대량 속성/국가 변경 요청.
 * nature, nation 중 하나만 보내도 동작한다. 둘 다 보내면 동시에 변경한다.
 */
public record MercenaryBulkUpdateRequest(
        List<Long> ids,
        Nature nature,   // null이면 변경 안 함
        Nation nation    // null이면 변경 안 함
) {}
