package org.example.gersangtrade.domain.user;

import org.example.gersangtrade.domain.hunt.ClearTimeRecordStatus;
import org.example.gersangtrade.hunt.dto.HuntMonsterSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserClearTimeRepository extends JpaRepository<UserClearTime, Long> {

    /** 당일(KST) EXP 지급 건수 — 일일 상한 판별용 */
    long countByUserIdAndRecordedAtGreaterThanEqualAndExpGrantedTrue(Long userId, LocalDateTime recordedAtFrom);

    /** 동일 몬스터 + 동일 덱 구성(hash) 재저장 — EXP 미지급 판별용 */
    boolean existsByUserIdAndMonsterIdAndDeckSnapshot_ContentHash(Long userId, Long monsterId, String contentHash);

    /** 해금 판별 — 서로 다른 몬스터 수 (ACTIVE 기록만) */
    @Query("""
            SELECT COUNT(DISTINCT u.monster.id)
            FROM UserClearTime u
            WHERE u.user.id = :userId
              AND u.status = :status
            """)
    long countDistinctMonsterIdByUserIdAndStatus(@Param("userId") Long userId,
                                                 @Param("status") ClearTimeRecordStatus status);

    /** 내 클리어타임 목록 (최신순) */
    @Query("""
            SELECT u FROM UserClearTime u
            JOIN FETCH u.monster
            JOIN FETCH u.deckSnapshot
            WHERE u.user.id = :userId
            ORDER BY u.recordedAt DESC
            """)
    List<UserClearTime> findByUserIdWithDetailsOrderByRecordedAtDesc(@Param("userId") Long userId);

    /** 몬스터별 공개 기록 목록 (클리어타임 빠른 순) */
    @Query("""
            SELECT u FROM UserClearTime u
            JOIN FETCH u.monster
            JOIN FETCH u.user
            JOIN FETCH u.deckSnapshot
            WHERE u.monster.id = :monsterId
              AND u.isPublic = true
              AND u.status = :status
            ORDER BY u.clearTimeSeconds ASC, u.recordedAt DESC
            """)
    List<UserClearTime> findPublicByMonsterId(@Param("monsterId") Long monsterId,
                                              @Param("status") ClearTimeRecordStatus status);

    /** 공개 스냅샷 조회 허용 여부 — 공개·ACTIVE 기록에 연결된 스냅샷만 */
    boolean existsByDeckSnapshot_IdAndIsPublicTrueAndStatus(Long deckSnapshotId, ClearTimeRecordStatus status);

    /** 사냥 허브 몬스터 목록 — 공개 기록이 있는 몬스터만 */
    @Query("""
            SELECT new org.example.gersangtrade.hunt.dto.HuntMonsterSummary(
                u.monster.id, u.monster.name, COUNT(u))
            FROM UserClearTime u
            WHERE u.isPublic = true
              AND u.status = :status
            GROUP BY u.monster.id, u.monster.name
            ORDER BY u.monster.name
            """)
    List<HuntMonsterSummary> summarizePublicRecordsByMonster(@Param("status") ClearTimeRecordStatus status);
}
