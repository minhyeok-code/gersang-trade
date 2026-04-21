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
import org.example.gersangtrade.domain.report.enums.ReporterType;
import org.example.gersangtrade.domain.user.User;

import java.time.LocalDateTime;

/**
 * 신고(Report) 엔티티.
 * USER 신고: 사용자가 다른 사용자·게시물·채팅 메시지를 신고할 때 생성.
 * SYSTEM 신고: KeywordDetectionService가 현금거래 의심 키워드를 감지할 때 자동 생성 (reporter=null).
 * 신고 대상은 targetType + targetId로 다형적으로 참조한다 (FK 대신 ID + 타입).
 * 관리자는 신고를 REVIEWING → PROCESSED(처리 완료) 또는 DISMISSED(기각) 순으로 처리한다.
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

    /**
     * 신고 생성 주체 유형.
     * USER: 사용자가 직접 신고.
     * SYSTEM: KeywordDetectionService가 자동 생성.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reporter_type", nullable = false, length = 10)
    private ReporterType reporterType;

    /**
     * 신고를 접수한 사용자.
     * SYSTEM 신고인 경우 null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    /** 신고 대상 유형 — USER, TRADE_LISTING, WANTED_LISTING, CHAT_MESSAGE */
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

    /**
     * 관련 채팅방 ID.
     * SYSTEM 신고(키워드 감지) 또는 CHAT_MESSAGE 신고 시 설정.
     * FK 없이 ID만 저장 (다형 참조).
     */
    @Column(name = "chat_room_id")
    private Long chatRoomId;

    /** 신고 처리 상태 — PENDING / REVIEWING / PROCESSED / DISMISSED */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    /** 관리자 처리 메모 (처리/기각 사유 기록용) */
    @Column(name = "admin_note", length = 500)
    private String adminNote;

    /** 신고를 검토·처리한 관리자 — 처리 전에는 null */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    /** 관리자가 신고를 처리한 시각 — 처리 전에는 null */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Builder
    public Report(ReporterType reporterType, User reporter,
                  ReportTargetType targetType, Long targetId,
                  ReportReason reasonCategory, String description,
                  String evidenceUrl, Long chatRoomId) {
        this.reporterType = reporterType;
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reasonCategory = reasonCategory;
        this.description = description;
        this.evidenceUrl = evidenceUrl;
        this.chatRoomId = chatRoomId;
        // 초기 상태는 항상 PENDING
        this.status = ReportStatus.PENDING;
    }

    /** 관리자가 검토 시작 — PENDING → REVIEWING */
    public void startReview(User admin) {
        this.status = ReportStatus.REVIEWING;
        this.processedBy = admin;
    }

    /** 관리자 처리 완료 처리 — REVIEWING → PROCESSED */
    public void process(String adminNote) {
        this.status = ReportStatus.PROCESSED;
        this.adminNote = adminNote;
        this.processedAt = LocalDateTime.now();
    }

    /** 신고 기각 처리 — REVIEWING → DISMISSED */
    public void dismiss(String adminNote) {
        this.status = ReportStatus.DISMISSED;
        this.adminNote = adminNote;
        this.processedAt = LocalDateTime.now();
    }
}
