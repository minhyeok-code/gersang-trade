package org.example.gersangtrade.crawler.job;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.crawler.tasklet.GersangjjangItemTasklet;
import org.example.gersangtrade.crawler.tasklet.GersangjjangMercenaryTasklet;
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
 *   <li>{@code masterDataJob} — 아이템 + 용병 순차 실행 (POST /admin/crawler/master)</li>
 *   <li>{@code itemDataJob}   — 아이템만 실행 (POST /admin/crawler/items)</li>
 *   <li>{@code mercenaryDataJob} — 용병만 실행 (POST /admin/crawler/mercenaries)</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class MasterDataJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final GersangjjangItemTasklet gersangjjangItemTasklet;
    private final GersangjjangMercenaryTasklet gersangjjangMercenaryTasklet;

    /** 아이템 + 용병 전체 수집 Job */
    @Bean
    public Job masterDataJob() {
        return new JobBuilder("masterDataJob", jobRepository)
                .start(itemListStep())
                .next(mercenaryListStep())
                .build();
    }

    /** 아이템만 수집 Job */
    @Bean
    public Job itemDataJob() {
        return new JobBuilder("itemDataJob", jobRepository)
                .start(itemListStep())
                .build();
    }

    /** 용병만 수집 Job */
    @Bean
    public Job mercenaryDataJob() {
        return new JobBuilder("mercenaryDataJob", jobRepository)
                .start(mercenaryListStep())
                .build();
    }

    @Bean
    public Step itemListStep() {
        return new StepBuilder("itemListStep", jobRepository)
                .tasklet(gersangjjangItemTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step mercenaryListStep() {
        return new StepBuilder("mercenaryListStep", jobRepository)
                .tasklet(gersangjjangMercenaryTasklet, transactionManager)
                .build();
    }

}
