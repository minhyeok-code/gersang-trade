package org.example.gersangtrade.report.repository;

import org.example.gersangtrade.domain.report.Report;
import org.example.gersangtrade.domain.report.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 신고 레포지토리.
 * 관리자 신고 목록 조회 및 신고 상태별 페이징을 지원한다.
 */
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * 신고 상태별 목록 페이징 조회 (관리자용).
     * reporter와 processedBy를 함께 fetch join한다.
     */
    @Query("SELECT r FROM Report r " +
           "LEFT JOIN FETCH r.reporter " +
           "LEFT JOIN FETCH r.processedBy " +
           "WHERE r.status = :status " +
           "ORDER BY r.createdAt ASC")
    Page<Report> findByStatus(@Param("status") ReportStatus status, Pageable pageable);

    /**
     * 전체 신고 목록 페이징 조회 (관리자용).
     * reporter와 processedBy를 함께 fetch join한다.
     */
    @Query("SELECT r FROM Report r " +
           "LEFT JOIN FETCH r.reporter " +
           "LEFT JOIN FETCH r.processedBy " +
           "ORDER BY r.createdAt ASC")
    Page<Report> findAllWithDetails(Pageable pageable);
}
