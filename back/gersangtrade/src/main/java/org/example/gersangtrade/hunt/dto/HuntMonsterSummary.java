package org.example.gersangtrade.hunt.dto;

/**
 * 사냥 허브 몬스터 목록용 요약 (공개 기록 수).
 */
public record HuntMonsterSummary(
        Long monsterId,
        String monsterName,
        long publicRecordCount
) {
}
