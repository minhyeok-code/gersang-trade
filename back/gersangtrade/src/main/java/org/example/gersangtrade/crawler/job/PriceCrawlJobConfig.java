package org.example.gersangtrade.crawler.job;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.crawler.tasklet.PriceCrawlTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Job 2 — 가격 데이터 수집 Job 설정.
 *
 * <p>단일 Tasklet Step으로 구성되며, PriceCrawlTasklet이 아래 작업을 일괄 처리한다:
 * <ul>
 *   <li>활성 서버 목록 조회 (13개)</li>
 *   <li>서버별 geota 육의전 페이지 순환 수집</li>
 *   <li>IQR 이상치 제거 및 집계 (최소 샘플 5건)</li>
 *   <li>material_price_history UPSERT</li>
 * </ul>
 *
 * <p>CrawlerScheduler가 매월 1일 새벽 3시에 자동 실행.
 * 관리자 수동 실행: POST /admin/crawler/price
 */
@Configuration
@RequiredArgsConstructor
public class PriceCrawlJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PriceCrawlTasklet priceCrawlTasklet;

    /**
     * Job 2: 가격 데이터 수집 Job.
     * Bean 이름 "priceCrawlJob" — CrawlerAdminController와 CrawlerScheduler에서 참조.
     */
    @Bean
    public Job priceCrawlJob() {
        return new JobBuilder("priceCrawlJob", jobRepository)
                .start(priceCrawlStep())
                .build();
    }

    /** Step 1: 서버별 육의전 가격 수집 Tasklet Step */
    @Bean
    public Step priceCrawlStep() {
        return new StepBuilder("priceCrawlStep", jobRepository)
                .tasklet(priceCrawlTasklet, transactionManager)
                .build();
    }
}
