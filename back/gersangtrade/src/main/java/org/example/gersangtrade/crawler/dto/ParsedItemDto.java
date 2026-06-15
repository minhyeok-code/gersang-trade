package org.example.gersangtrade.crawler.dto;

import org.example.gersangtrade.domain.catalog.enums.GemGrade;

/**
 * 아이템명 파싱 결과 DTO.
 * ItemNameParser가 raw 아이템명을 분류한 결과를 담는다.
 *
 * <p>분류 유형:
 * <ul>
 *   <li>GEM — 보석 (흑요석 등 11종 + 세공됨/강화됨/빛나는 변형. 주술은 강화됨+ritualName 조합으로 표현)</li>
 *   <li>EQUIPMENT — 확실한 장비 (이름에 &lt;ritual&gt; 또는 &lt;홈이있는&gt; 마커 포함)</li>
 *   <li>UNKNOWN — 일반 이름만 있어 분류 불명</li>
 * </ul>
 */
public record ParsedItemDto(

        /** 분류 유형 */
        ParsedType type,

        /**
         * 정제된 기본 아이템명.
         * 접두사(&lt;ritual&gt;, 세공된, 강화된 등)가 제거된 순수 이름.
         * 예: "&lt;태산북두&gt; 챠우인형" → "챠우인형"
         * 예: "세공된 흑요석" → "흑요석"
         */
        String cleanName,

        /** 보석 등급 (GEM 타입일 때만 non-null) */
        GemGrade gemGrade,

        /**
         * 주술명 (GEM 강화됨+주술 조합 또는 EQUIPMENT의 접두사 주술명).
         * 해당 사항 없으면 null.
         * 예: "&lt;태산북두&gt; 흑요석" → ritualName="태산북두", gemGrade=강화됨
         */
        String ritualName,

        /**
         * 홈이있는 버전 여부 (EQUIPMENT 타입일 때만 의미 있음).
         * "&lt;홈이있는&gt; 챠우인형" 또는 "홈이있는 &lt;ritual&gt; 챠우인형" 패턴에서 true.
         */
        boolean hasSlotOption

) {

    /** 분류 유형 */
    public enum ParsedType {
        /** 보석 아이템 — gems 테이블 UPSERT 대상 */
        GEM,
        /** 확실한 장비 아이템 — items(EQUIPMENT) UPSERT 대상 */
        EQUIPMENT,
        /**
         * 분류 불명 아이템.
         * items(MATERIAL) 임시 생성 후 수동·후속 분류 대상.
         */
        UNKNOWN
    }

    /** GEM 타입 생성 헬퍼 */
    public static ParsedItemDto gem(String cleanName, GemGrade grade, String ritualName) {
        return new ParsedItemDto(ParsedType.GEM, cleanName, grade, ritualName, false);
    }

    /** EQUIPMENT 타입 생성 헬퍼 */
    public static ParsedItemDto equipment(String cleanName, String ritualName, boolean hasSlotOption) {
        return new ParsedItemDto(ParsedType.EQUIPMENT, cleanName, null, ritualName, hasSlotOption);
    }

    /** UNKNOWN 타입 생성 헬퍼 (plain 이름) */
    public static ParsedItemDto unknown(String name) {
        return new ParsedItemDto(ParsedType.UNKNOWN, name, null, null, false);
    }
}
