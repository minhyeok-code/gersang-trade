package org.example.gersangtrade.crawler.job;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.crawler.tasklet.GersangjjangExclusiveEquipmentTasklet;
import org.example.gersangtrade.crawler.tasklet.GersangjjangItemTasklet;
import org.example.gersangtrade.crawler.tasklet.GersangjjangMaterialTasklet;
import org.example.gersangtrade.crawler.tasklet.GersangjjangMercenaryTasklet;
import org.example.gersangtrade.crawler.tasklet.GersangjjangMonsterTasklet;
import org.example.gersangtrade.crawler.tasklet.GersangjjangRitualTasklet;
import org.example.gersangtrade.crawler.tasklet.GersangjjangSetTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 마스터 데이터 수집 Job 설정.
 *
 * <ul>
 *   <li>{@code masterDataJob}    — 아이템 + 재료 + 세트 + 용병 + 주술 순차 실행 (POST /admin/crawler/master)</li>
 *   <li>{@code itemDataJob}      — 장비/보석만 실행 (POST /admin/crawler/items)</li>
 *   <li>{@code materialDataJob}  — 잡화/소모품/재료만 실행 (POST /admin/crawler/materials)</li>
 *   <li>{@code mercenaryDataJob} — 용병만 실행 (POST /admin/crawler/mercenaries)</li>
 *   <li>{@code setDataJob}       — 장비 세트만 실행 (POST /admin/crawler/sets)</li>
 *   <li>{@code ritualDataJob}    — 주술만 실행 (POST /admin/crawler/rituals)</li>
 *   <li>{@code exclusiveEquipmentDataJob} — 전용장비만 실행 (POST /admin/crawler/exclusive-equipment)</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class MasterDataJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final GersangjjangItemTasklet gersangjjangItemTasklet;
    private final GersangjjangMaterialTasklet gersangjjangMaterialTasklet;
    private final GersangjjangMercenaryTasklet gersangjjangMercenaryTasklet;
    private final GersangjjangExclusiveEquipmentTasklet gersangjjangExclusiveEquipmentTasklet;
    private final GersangjjangSetTasklet gersangjjangSetTasklet;
    private final GersangjjangRitualTasklet gersangjjangRitualTasklet;
    private final GersangjjangMonsterTasklet gersangjjangMonsterTasklet;

    /** 아이템 → 재료 → 세트 → 용병 → 주술 순서 전체 수집 Job (의존성 순서 준수) */
    @Bean
    public Job masterDataJob() {
        return new JobBuilder("masterDataJob", jobRepository)
                .start(itemListStep())
                .next(materialListStep())
                .next(setListStep())
                .next(mercenaryListStep())
                .next(exclusiveEquipmentListStep())
                .next(ritualListStep())
                .build();
    }

    /** 장비/보석만 수집 Job */
    @Bean
    public Job itemDataJob() {
        return new JobBuilder("itemDataJob", jobRepository)
                .start(itemListStep())
                .build();
    }

    /** 잡화/소모품/재료만 수집 Job */
    @Bean
    public Job materialDataJob() {
        return new JobBuilder("materialDataJob", jobRepository)
                .start(materialListStep())
                .build();
    }

    /** 용병만 수집 Job */
    @Bean
    public Job mercenaryDataJob() {
        return new JobBuilder("mercenaryDataJob", jobRepository)
                .start(mercenaryListStep())
                .build();
    }

    /** 전용장비만 수집 Job — mercenaryListStep 이후 실행 권장 */
    @Bean
    public Job exclusiveEquipmentDataJob() {
        return new JobBuilder("exclusiveEquipmentDataJob", jobRepository)
                .start(exclusiveEquipmentListStep())
                .build();
    }

    /** 장비 세트만 수집 Job */
    @Bean
    public Job setDataJob() {
        return new JobBuilder("setDataJob", jobRepository)
                .start(setListStep())
                .build();
    }

    @Bean
    public Step itemListStep() {
        return new StepBuilder("itemListStep", jobRepository)
                .tasklet(gersangjjangItemTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step materialListStep() {
        return new StepBuilder("materialListStep", jobRepository)
                .tasklet(gersangjjangMaterialTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step mercenaryListStep() {
        return new StepBuilder("mercenaryListStep", jobRepository)
                .tasklet(gersangjjangMercenaryTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step exclusiveEquipmentListStep() {
        return new StepBuilder("exclusiveEquipmentListStep", jobRepository)
                .tasklet(gersangjjangExclusiveEquipmentTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step setListStep() {
        return new StepBuilder("setListStep", jobRepository)
                .tasklet(gersangjjangSetTasklet, transactionManager)
                .build();
    }

    /** 주술만 수집 Job — 아이템/세트 크롤러 완료 후 실행해야 FK 조회 가능 */
    @Bean
    public Job ritualDataJob() {
        return new JobBuilder("ritualDataJob", jobRepository)
                .start(ritualListStep())
                .build();
    }

    @Bean
    public Step ritualListStep() {
        return new StepBuilder("ritualListStep", jobRepository)
                .tasklet(gersangjjangRitualTasklet, transactionManager)
                .build();
    }

    /** 몬스터만 수집 Job */
    @Bean
    public Job monsterDataJob() {
        return new JobBuilder("monsterDataJob", jobRepository)
                .start(monsterListStep())
                .build();
    }

    @Bean
    public Step monsterListStep() {
        return new StepBuilder("monsterListStep", jobRepository)
                .tasklet(gersangjjangMonsterTasklet, transactionManager)
                .build();
    }

}
