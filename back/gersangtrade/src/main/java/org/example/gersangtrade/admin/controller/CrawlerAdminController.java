package org.example.gersangtrade.admin.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 크롤링 Job 수동 트리거 관리자 API.
 *
 * <p>ADMIN 권한 필요 (@PreAuthorize).
 * 요청 수신 즉시 Job을 실행하고 JobExecution ID 및 상태를 반환한다.
 *
 * <p>엔드포인트:
 * <ul>
 *   <li>POST /admin/crawler/master — Job 1 (마스터 데이터 수집)</li>
 *   <li>POST /admin/crawler/price  — Job 2 (가격 데이터 수집)</li>
 * </ul>
 *
 * <p>JobParameter에 timestamp를 포함하여 Spring Batch 중복 실행 제한을 우회한다.
 * 동일 Job을 여러 번 실행해도 매번 고유한 JobInstance로 생성된다.
 */
@Slf4j
@RestController
@RequestMapping("/admin/crawler")
public class CrawlerAdminController {


    private final JobLauncher jobLauncher;
    private final Job masterDataJob;
    private final Job priceCrawlJob;


    public CrawlerAdminController(JobLauncher jobLauncher,
                                   @Qualifier("masterDataJob") Job masterDataJob,
                                   @Qualifier("priceCrawlJob") Job priceCrawlJob) {
        this.jobLauncher = jobLauncher;
        this.masterDataJob = masterDataJob;
        this.priceCrawlJob = priceCrawlJob;
    }

    /**
     * Job 1 수동 트리거 — 마스터 데이터(아이템/용병) 수집.
     *
     * @return JobExecution ID 및 배치 상태 메시지
     */
    @PostMapping("/master")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerMasterDataJob() {
        log.info("관리자 수동 트리거: 마스터 데이터 수집 Job 시작");
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            JobExecution execution = jobLauncher.run(masterDataJob, params);
            String message = "마스터 데이터 수집 Job 시작됨 (executionId=%d, status=%s)"
                    .formatted(execution.getId(), execution.getStatus());
            log.info(message);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("마스터 데이터 수집 Job 실행 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Job 실행 실패: " + e.getMessage());
        }
    }

    /**
     * Job 2 수동 트리거 — 서버별 가격 데이터 수집.
     *
     * @return JobExecution ID 및 배치 상태 메시지
     */
    @PostMapping("/price")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerPriceCrawlJob() {
        log.info("관리자 수동 트리거: 가격 데이터 수집 Job 시작");
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            JobExecution execution = jobLauncher.run(priceCrawlJob, params);
            String message = "가격 데이터 수집 Job 시작됨 (executionId=%d, status=%s)"
                    .formatted(execution.getId(), execution.getStatus());
            log.info(message);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("가격 데이터 수집 Job 실행 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Job 실행 실패: " + e.getMessage());
        }
    }
}
