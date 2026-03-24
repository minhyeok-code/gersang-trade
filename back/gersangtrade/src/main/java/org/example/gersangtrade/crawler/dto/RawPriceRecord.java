package org.example.gersangtrade.crawler.dto;

/**
 * geota 육의전 페이지에서 파싱한 단일 거래 레코드.
 * IqrCalculator에 전달되기 전 원시 데이터 상태다.
 */
public record RawPriceRecord(

        /** 거래된 아이템명 (geota 표시명 그대로) */
        String itemName,

        /** 거래 서버 ID (1~13) */
        Integer serverId,

        /** 거래 단가 (골드 단위) */
        Long unitPrice

) {}
