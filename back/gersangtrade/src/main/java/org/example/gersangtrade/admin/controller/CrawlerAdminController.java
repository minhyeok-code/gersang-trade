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
 * <ul>
 *   <li>POST /admin/crawler/master      — 아이템 + 용병 전체 수집</li>
 *   <li>POST /admin/crawler/items       — 아이템만 수집</li>
 *   <li>POST /admin/crawler/mercenaries — 용병만 수집</li>
 *   <li>POST /admin/crawler/price       — 서버별 가격 수집</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/admin/crawler")
public class CrawlerAdminController {

    private final JobLauncher jobLauncher;
    private final Job masterDataJob;
    private final Job itemDataJob;
    private final Job mercenaryDataJob;
    private final Job priceCrawlJob;

    public CrawlerAdminController(JobLauncher jobLauncher,
                                  @Qualifier("masterDataJob") Job masterDataJob,
                                  @Qualifier("itemDataJob") Job itemDataJob,
                                  @Qualifier("mercenaryDataJob") Job mercenaryDataJob,
                                  @Qualifier("priceCrawlJob") Job priceCrawlJob) {
        this.jobLauncher = jobLauncher;
        this.masterDataJob = masterDataJob;
        this.itemDataJob = itemDataJob;
        this.mercenaryDataJob = mercenaryDataJob;
        this.priceCrawlJob = priceCrawlJob;
    }

    /** 아이템 + 용병 전체 수집 */
    @PostMapping("/master")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerMasterDataJob() {
        return runJob(masterDataJob, "마스터 데이터(아이템+용병) 수집");
    }

    /** 아이템만 수집 */
    @PostMapping("/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerItemDataJob() {
        return runJob(itemDataJob, "아이템 수집");
    }

    /** 용병만 수집 */
    @PostMapping("/mercenaries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerMercenaryDataJob() {
        return runJob(mercenaryDataJob, "용병 수집");
    }

    /** 서버별 가격 수집 */
    @PostMapping("/price")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerPriceCrawlJob() {
        return runJob(priceCrawlJob, "가격 데이터 수집");
    }

    private ResponseEntity<String> runJob(Job job, String jobName) {
        log.info("관리자 수동 트리거: {} Job 시작", jobName);
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            JobExecution execution = jobLauncher.run(job, params);
            String message = "%s Job 시작됨 (executionId=%d, status=%s)"
                    .formatted(jobName, execution.getId(), execution.getStatus());
            log.info(message);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("{} Job 실행 실패: {}", jobName, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Job 실행 실패: " + e.getMessage());
        }
    }
}
