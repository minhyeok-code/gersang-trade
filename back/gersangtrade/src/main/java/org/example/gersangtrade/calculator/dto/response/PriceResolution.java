package org.example.gersangtrade.calculator.dto.response;

import java.util.List;

/**
 * 가성비 평가용 가격 조회 결과.
 * CatalogPriceResolverService가 반환하는 불변 record.
 */
public record PriceResolution(

        /** 조회된 총 가격 — MISSING이면 null */
        Long totalPrice,

        /** 가격 출처 */
        PriceSource source,

        /** 시세 기반 가격일 때의 거래 건수 — USER_INPUT·MIXED·MISSING이면 null */
        Integer tradeCount,

        /** 조회에 사용된 서버 ID */
        Integer serverId,

        /**
         * 세트 피스별 가격 breakdown.
         * 단품 조회 시 빈 리스트, 세트 피스 합산 시 피스별 항목 포함.
         */
        List<ItemPriceLine> breakdown

) {}
