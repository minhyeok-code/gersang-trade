package org.example.gersangtrade.crawler.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 크롤링 Job 스케줄러.
 *
 * <p>Job 2 (가격 데이터 수집)를 매월 1일 새벽 3시에 자동 실행한다.
 * Job 1 (마스터 데이터 수집)은 자동 스케줄 없음 — 관리자 수동 트리거만 지원한다.
 *
 * <p>실행 시 현재 타임스탬프를 JobParameter로 포함하여
 * Spring Batch의 동일 파라미터 중복 실행 제한을 우회한다.
 */
@Slf4j
@Component
public class CrawlerScheduler {

    private final JobLauncher jobLauncher;
    private final Job priceCrawlJob;

    public CrawlerScheduler(JobLauncher jobLauncher,
                             @Qualifier("priceCrawlJob") Job priceCrawlJob) {
        this.jobLauncher = jobLauncher;
        this.priceCrawlJob = priceCrawlJob;
    }

    /**
     * 매월 1일 새벽 3시 가격 데이터 수집 Job 자동 실행.
     * cron 표현식: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 3 1 * *")
    public void runMonthlyCrawl() {
        log.info("=== 월간 가격 수집 스케줄 실행 시작 ===");
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(priceCrawlJob, params);
            log.info("=== 월간 가격 수집 스케줄 실행 완료 ===");
        } catch (Exception e) {
            log.error("월간 가격 수집 스케줄 실행 실패: {}", e.getMessage(), e);
        }
    }
}
