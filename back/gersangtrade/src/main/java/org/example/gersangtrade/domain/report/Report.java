package org.example.gersangtrade.domain.report;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.common.BaseEntity;
import org.example.gersangtrade.domain.report.enums.ReportReason;
import org.example.gersangtrade.domain.report.enums.ReportStatus;
import org.example.gersangtrade.domain.report.enums.ReportTargetType;
import org.example.gersangtrade.domain.user.User;

import java.time.LocalDateTime;

/**
 * 신고(Report) 엔티티.
 * 사용자가 다른 사용자, 거래 등록글, 거래 신청을 신고하면 생성된다.
 * 신고 대상은 targetType + targetId로 다형적으로 참조한다 (FK 대신 ID + 타입).
 * 관리자는 신고를 검토 후 PROCESSED(처리 완료) 또는 DISMISSED(기각) 처리한다.
 */
@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

    /** 신고 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신고를 접수한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    /** 신고 대상 유형 — USER, TRADE_LISTING, TRADE_APPLICATION */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private ReportTargetType targetType;

    /** 신고 대상 엔티티 ID — targetType에 따라 해당 테이블의 PK 값 */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** 신고 사유 분류 — FRAUD, ABUSE, FAKE_LISTING, CASH_TRADE, OTHER */
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_category", nullable = false, length = 30)
    private ReportReason reasonCategory;

    /** 신고 상세 내용 (필수 입력) */
    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    /** 증빙 스크린샷 URL (선택 입력) */
    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    /** 신고 처리 상태 — PENDING / PROCESSED / DISMISSED */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    /** 관리자 처리 메모 (처리/기각 사유 기록용) */
    @Column(name = "admin_note", length = 500)
    private String adminNote;

    /** 관리자가 신고를 처리한 시각 — 처리 전에는 null */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Builder
    public Report(User reporter, ReportTargetType targetType, Long targetId,
                  ReportReason reasonCategory, String description, String evidenceUrl) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reasonCategory = reasonCategory;
        this.description = description;
        this.evidenceUrl = evidenceUrl;
        // 초기 상태는 항상 PENDING
        this.status = ReportStatus.PENDING;
    }

    /** 관리자 처리 완료 처리 */
    public void process(String adminNote) {
        this.status = ReportStatus.PROCESSED;
        this.adminNote = adminNote;
        this.processedAt = LocalDateTime.now();
    }

    /** 신고 기각 처리 */
    public void dismiss(String adminNote) {
        this.status = ReportStatus.DISMISSED;
        this.adminNote = adminNote;
        this.processedAt = LocalDateTime.now();
    }
}
