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
 *   <li>POST /admin/crawler/master      — 아이템 + 재료 + 세트 + 용병 + 주술 전체 수집</li>
 *   <li>POST /admin/crawler/items       — 장비/보석만 수집</li>
 *   <li>POST /admin/crawler/materials   — 잡화/소모품/재료만 수집</li>
 *   <li>POST /admin/crawler/mercenaries — 용병만 수집 (거상짱)</li>
 *   <li>POST /admin/crawler/sets        — 장비 세트만 수집</li>
 *   <li>POST /admin/crawler/rituals     — 주술만 수집 (아이템/세트 크롤러 완료 후)</li>
 *   <li>POST /admin/crawler/monsters    — 몬스터만 수집 (거상짱)</li>
 *   <li>POST /admin/crawler/exclusive-equipment — 전용장비만 수집 (거상짱)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/admin/crawler")
public class CrawlerAdminController {

    private final JobLauncher jobLauncher;
    private final Job masterDataJob;
    private final Job itemDataJob;
    private final Job materialDataJob;
    private final Job mercenaryDataJob;
    private final Job setDataJob;
    private final Job ritualDataJob;
    private final Job monsterDataJob;
    private final Job exclusiveEquipmentDataJob;

    public CrawlerAdminController(JobLauncher jobLauncher,
                                  @Qualifier("masterDataJob") Job masterDataJob,
                                  @Qualifier("itemDataJob") Job itemDataJob,
                                  @Qualifier("materialDataJob") Job materialDataJob,
                                  @Qualifier("mercenaryDataJob") Job mercenaryDataJob,
                                  @Qualifier("setDataJob") Job setDataJob,
                                  @Qualifier("ritualDataJob") Job ritualDataJob,
                                  @Qualifier("monsterDataJob") Job monsterDataJob,
                                  @Qualifier("exclusiveEquipmentDataJob") Job exclusiveEquipmentDataJob) {
        this.jobLauncher = jobLauncher;
        this.masterDataJob = masterDataJob;
        this.itemDataJob = itemDataJob;
        this.materialDataJob = materialDataJob;
        this.mercenaryDataJob = mercenaryDataJob;
        this.setDataJob = setDataJob;
        this.ritualDataJob = ritualDataJob;
        this.monsterDataJob = monsterDataJob;
        this.exclusiveEquipmentDataJob = exclusiveEquipmentDataJob;
    }

    /** 아이템 + 재료 + 세트 + 용병 + 주술 전체 수집 */
    @PostMapping("/master")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerMasterDataJob() {
        return runJob(masterDataJob, "마스터 데이터(아이템+용병) 수집");
    }

    /** 장비/보석만 수집 */
    @PostMapping("/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerItemDataJob() {
        return runJob(itemDataJob, "장비/보석 수집");
    }

    /** 잡화/소모품/재료만 수집 */
    @PostMapping("/materials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerMaterialDataJob() {
        return runJob(materialDataJob, "잡화/소모품/재료 수집");
    }

    /** 용병만 수집 (거상짱) */
    @PostMapping("/mercenaries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerMercenaryDataJob() {
        return runJob(mercenaryDataJob, "용병 수집");
    }

    /** 장비 세트만 수집 */
    @PostMapping("/sets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerSetDataJob() {
        return runJob(setDataJob, "장비 세트 수집");
    }

    /** 주술 수집 — 아이템/세트 크롤러 완료 후 실행 권장 */
    @PostMapping("/rituals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerRitualDataJob() {
        return runJob(ritualDataJob, "주술 수집");
    }

    /** 몬스터 수집 (거상짱) */
    @PostMapping("/monsters")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerMonsterDataJob() {
        return runJob(monsterDataJob, "몬스터 수집");
    }

    /** 전용장비 수집 — mercenary 크롤러·시더 완료 후 실행 권장 */
    @PostMapping("/exclusive-equipment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerExclusiveEquipmentDataJob() {
        return runJob(exclusiveEquipmentDataJob, "전용장비 수집");
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
