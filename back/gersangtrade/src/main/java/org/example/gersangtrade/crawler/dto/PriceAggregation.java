package org.example.gersangtrade.crawler.dto;

/**
 * IQR 이상치 제거 후 집계된 아이템+서버별 가격 데이터.
 * PriceCrawlTasklet에서 MaterialPriceHistory UPSERT에 사용된다.
 */
public record PriceAggregation(

        /** 아이템명 (DB 조회 키로 사용) */
        String itemName,

        /** 집계 서버 ID */
        Integer serverId,

        /** 집계 연월 ('yyyy-MM' 형식) */
        String yearMonth,

        /** IQR 이상치 제거 후 평균가 (골드 단위) */
        Long avgPrice,

        /** IQR 이상치 제거 후 최저가 (골드 단위) */
        Long minPrice,

        /** 집계에 사용된 유효 거래 건수 */
        Integer sampleCount

) {}
