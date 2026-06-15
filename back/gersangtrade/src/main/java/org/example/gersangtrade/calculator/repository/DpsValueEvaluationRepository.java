package org.example.gersangtrade.calculator.repository;

import org.example.gersangtrade.domain.calculator.DpsValueEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * DPS 가성비 평가 레포지토리.
 */
public interface DpsValueEvaluationRepository extends JpaRepository<DpsValueEvaluation, Long> {

    /** 동일 요청 재제출 시 기존 결과 반환 (멱등 처리) */
    Optional<DpsValueEvaluation> findByUserIdAndEvaluationHash(Long userId, String evaluationHash);

    /** 내 평가 목록 — 최신순, 페이징 */
    @Query("SELECT e FROM DpsValueEvaluation e JOIN FETCH e.monster WHERE e.user.id = :userId ORDER BY e.createdAt DESC")
    Page<DpsValueEvaluation> findByUserIdWithMonster(@Param("userId") Long userId, Pageable pageable);

    /** 내 평가 상세 — 본인 소유 확인 포함 */
    @Query("SELECT e FROM DpsValueEvaluation e JOIN FETCH e.monster WHERE e.id = :id AND e.user.id = :userId")
    Optional<DpsValueEvaluation> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /** 상세·diff — baseline·scenario 스냅샷 fetch */
    @Query("""
            SELECT e FROM DpsValueEvaluation e
            JOIN FETCH e.monster
            LEFT JOIN FETCH e.baselineDeckSnapshot
            LEFT JOIN FETCH e.scenarioDeckSnapshot
            WHERE e.id = :id AND e.user.id = :userId
            """)
    Optional<DpsValueEvaluation> findByIdAndUserIdWithSnapshots(
            @Param("id") Long id, @Param("userId") Long userId);
}
