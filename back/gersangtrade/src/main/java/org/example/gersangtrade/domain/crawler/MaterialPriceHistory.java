package org.example.gersangtrade.domain.crawler;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Server;

import java.time.LocalDateTime;

/**
 * 재료 아이템 월간 가격 집계 엔티티.
 * geota.co.kr 육의전 페이지를 크롤링하여 IQR 이상치 제거 후 집계한 결과를 저장한다.
 * 가성비 계산기에서 아이템별 기본 가격값으로 사용된다.
 *
 * <p>집계 주기: 매월 1일 새벽 3시 Spring Batch Job 자동 실행 (Job 2).
 * 관리자는 POST /admin/crawler/price 로 즉시 실행 가능.
 *
 * <p>동일 아이템+서버+연월 조합은 UPSERT(덮어쓰기)로 처리된다.
 *
 * <p>최소 샘플 수(5건) 미달 시 해당 조합은 저장하지 않는다.
 */
@Entity
@Table(
        name = "material_price_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_material_price_history_item_server_month",
                columnNames = {"item_id", "server_id", "year_month"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MaterialPriceHistory {

    /** 가격 집계 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 집계 대상 재료 아이템 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** 집계 대상 서버 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    /**
     * 집계 연월 — 'YYYY-MM' 형식.
     * 예: "2025-03"
     */
    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    /**
     * IQR 이상치 제거 후 평균가.
     * 가성비 계산기 기본 표시값으로 사용된다.
     */
    @Column(name = "avg_price", nullable = false)
    private Long avgPrice;

    /**
     * IQR 이상치 제거 후 최저가.
     * 최저가 구매 기준 가성비 계산 시 사용된다.
     */
    @Column(name = "min_price", nullable = false)
    private Long minPrice;

    /**
     * 집계에 사용된 유효 거래 건수.
     * 5건 미만이면 신뢰도가 낮아 저장하지 않는다.
     */
    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount;

    /** 크롤링 및 집계 완료 시각 */
    @Column(name = "crawled_at", nullable = false)
    private LocalDateTime crawledAt;

    @Builder
    public MaterialPriceHistory(Item item, Server server, String yearMonth,
                                 Long avgPrice, Long minPrice, Integer sampleCount) {
        this.item = item;
        this.server = server;
        this.yearMonth = yearMonth;
        this.avgPrice = avgPrice;
        this.minPrice = minPrice;
        this.sampleCount = sampleCount;
        this.crawledAt = LocalDateTime.now();
    }

    /** 동일 연월 재수집 시 집계 결과 갱신 */
    public void update(Long avgPrice, Long minPrice, Integer sampleCount) {
        this.avgPrice = avgPrice;
        this.minPrice = minPrice;
        this.sampleCount = sampleCount;
        this.crawledAt = LocalDateTime.now();
    }
}
