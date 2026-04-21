package org.example.gersangtrade.listing.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * EQUIPMENT_SET 번들의 표시 제목을 생성하는 유틸리티.
 *
 * <ul>
 *   <li>전체 동일 주술: {@code 풀 {마크} {세트명}} — 예: {@code 풀 ** 00세트}</li>
 *   <li>부분 주술:     {@code 풀 {세트명} {피스수}{마크}} — 예: {@code 풀 00세트 3**}</li>
 *   <li>주술 없음:     {@code 풀 {세트명}}</li>
 *   <li>혼재 마크:     {@code 풀 {세트명}} (폴백)</li>
 * </ul>
 */
public class SetTitleGenerator {

    private SetTitleGenerator() {}

    /**
     * 세트 표시 제목을 생성한다.
     *
     * @param setName    세트명 (예: "00세트")
     * @param pieceMarks 피스별 주술 마크 목록. 주술이 없는 피스는 null 항목으로 표현.
     * @return 생성된 표시 제목
     */
    public static String generate(String setName, List<String> pieceMarks) {
        List<String> nonNullMarks = pieceMarks.stream()
                .filter(Objects::nonNull)
                .toList();

        // 주술 없음 — 세트명만 표시
        if (nonNullMarks.isEmpty()) {
            return "풀 " + setName;
        }

        // 마크 종류 확인
        Set<String> distinctMarks = new HashSet<>(nonNullMarks);
        if (distinctMarks.size() > 1) {
            // 혼재된 마크 — 폴백 (단순 세트명 표시)
            return "풀 " + setName;
        }

        String mark = distinctMarks.iterator().next();

        // 전체 피스 동일 주술: 풀 {마크} {세트명}
        if (nonNullMarks.size() == pieceMarks.size()) {
            return "풀 " + mark + " " + setName;
        }

        // 부분 주술: 풀 {세트명} {피스수}{마크}
        return "풀 " + setName + " " + nonNullMarks.size() + mark;
    }
}
