package org.example.gersangtrade.crawler.job;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.crawler.reader.ItemDetailReader;
import org.example.gersangtrade.crawler.reader.MercenaryDetailReader;
import org.example.gersangtrade.crawler.tasklet.ItemListTasklet;
import org.example.gersangtrade.crawler.tasklet.MercenaryListTasklet;
import org.example.gersangtrade.crawler.writer.ItemDetailWriter;
import org.example.gersangtrade.crawler.writer.MercenaryDetailWriter;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Job 1 — 마스터 데이터 수집 Job 설정.
 *
 * <p>4단계로 구성된다:
 * <ol>
 *   <li>itemListStep — geota 아이템 전체 목록 수집 및 UPSERT (Tasklet)</li>
 *   <li>itemDetailStep — gerniverse 아이템 상세 수집 (Chunk, size=10)</li>
 *   <li>mercenaryListStep — geota 용병 전체 목록 수집 및 UPSERT (Tasklet)</li>
 *   <li>mercenaryDetailStep — gerniverse 용병 상세 수집 (Chunk, size=5)</li>
 * </ol>
 *
 * <p>최초 1회 실행 후 관리자 수동 트리거(POST /admin/crawler/master)로 재실행.
 * 재실행 시 고유 timestamp JobParameter를 포함하여 Spring Batch 중복 실행 제한을 우회한다.
 */
@Configuration
@RequiredArgsConstructor
public class MasterDataJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final ItemListTasklet itemListTasklet;
    private final MercenaryListTasklet mercenaryListTasklet;
    private final ItemDetailReader itemDetailReader;
    private final ItemDetailWriter itemDetailWriter;
    private final MercenaryDetailReader mercenaryDetailReader;
    private final MercenaryDetailWriter mercenaryDetailWriter;

    /**
     * Job 1: 마스터 데이터 수집 Job.
     * Bean 이름 "masterDataJob" — CrawlerAdminController와 CrawlerScheduler에서 참조.
     */
    @Bean
    public Job masterDataJob() {
        return new JobBuilder("masterDataJob", jobRepository)
                .start(itemListStep())
                .next(itemDetailStep())
                .next(mercenaryListStep())
                .next(mercenaryDetailStep())
                .build();
    }

    /** Step 1: geota 아이템 목록 수집 Tasklet Step */
    @Bean
    public Step itemListStep() {
        return new StepBuilder("itemListStep", jobRepository)
                .tasklet(itemListTasklet, transactionManager)
                .build();
    }

    /**
     * Step 2: gerniverse 아이템 상세 수집 Chunk-oriented Step.
     * chunk size 10 — 10개씩 묶어 처리. 개별 아이템 실패는 ItemDetailWriter에서 catch하여 skip.
     * beforeStep 리스너로 Reader 큐를 초기화하여 Job 재실행 시 중복 처리를 방지한다.
     */
    @Bean
    public Step itemDetailStep() {
        return new StepBuilder("itemDetailStep", jobRepository)
                .<Item, Item>chunk(10, transactionManager)
                .reader(itemDetailReader)
                .writer(itemDetailWriter)
                .listener(itemDetailReaderResetListener())
                .build();
    }

    /** Step 3: geota 용병 목록 수집 Tasklet Step */
    @Bean
    public Step mercenaryListStep() {
        return new StepBuilder("mercenaryListStep", jobRepository)
                .tasklet(mercenaryListTasklet, transactionManager)
                .build();
    }

    /**
     * Step 4: gerniverse 용병 상세 수집 Chunk-oriented Step.
     * chunk size 5 — 용병 수가 아이템보다 적으므로 소규모 청크 사용.
     */
    @Bean
    public Step mercenaryDetailStep() {
        return new StepBuilder("mercenaryDetailStep", jobRepository)
                .<Mercenary, Mercenary>chunk(5, transactionManager)
                .reader(mercenaryDetailReader)
                .writer(mercenaryDetailWriter)
                .listener(mercenaryDetailReaderResetListener())
                .build();
    }

    /**
     * Step 2 시작 전 ItemDetailReader 큐 초기화 리스너.
     * Job 재실행 시 이전 실행의 큐 상태가 남아있지 않도록 보장한다.
     */
    private StepExecutionListener itemDetailReaderResetListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                itemDetailReader.reset();
            }
        };
    }

    /**
     * Step 4 시작 전 MercenaryDetailReader 큐 초기화 리스너.
     */
    private StepExecutionListener mercenaryDetailReaderResetListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                mercenaryDetailReader.reset();
            }
        };
    }
}
