package org.example.gersangtrade.crawler.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.MonsterRepository;
import org.example.gersangtrade.crawler.parser.GersangjjangMonsterParser;
import org.example.gersangtrade.crawler.parser.GersangjjangMonsterParser.MonsterRow;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.example.gersangtrade.domain.catalog.Monster;
import org.jsoup.nodes.Document;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 거상짱 몬스터 정보 수집 Tasklet.
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>인덱스 페이지에서 몬스터 상세 URL 목록 수집</li>
 *   <li>각 URL 순회 → monster-row 파싱</li>
 *   <li>name+pageUrl 기준 upsert (있으면 수치 업데이트, 없으면 신규 저장)</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GersangjjangMonsterTasklet implements Tasklet {

    private final JsoupFetcher jsoupFetcher;
    private final MonsterRepository monsterRepository;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {

        log.info("몬스터 크롤링 시작: {}", GersangjjangMonsterParser.INDEX_URL);

        // 1. 인덱스 페이지에서 URL 수집
        Document indexDoc = jsoupFetcher.fetch(GersangjjangMonsterParser.INDEX_URL);
        List<String> urls = GersangjjangMonsterParser.parseIndexUrls(indexDoc);
        log.info("수집된 몬스터 페이지 URL: {}개", urls.size());

        int saved = 0, updated = 0, skipped = 0;

        // 2. 각 URL 파싱 및 저장
        for (String url : urls) {
            try {
                Document doc = jsoupFetcher.fetch(url);
                List<MonsterRow> rows = GersangjjangMonsterParser.parseMonsterRows(doc, url);

                for (MonsterRow row : rows) {
                    boolean isNew = upsert(row);
                    if (isNew) saved++;
                    else updated++;
                }
            } catch (Exception e) {
                log.warn("몬스터 페이지 크롤링 실패 [url={}]: {}", url, e.getMessage());
                skipped++;
            }
        }

        log.info("몬스터 크롤링 완료: 신규={}, 업데이트={}, 스킵={}", saved, updated, skipped);
        return RepeatStatus.FINISHED;
    }

    /** name+pageUrl 기준 upsert. true=신규, false=업데이트 */
    private boolean upsert(MonsterRow row) {
        return monsterRepository.findByNameAndPageUrl(row.name(), row.pageUrl())
                .map(existing -> {
                    existing.update(row.hp(), row.hittingResistance(), row.magicResistance(),
                            row.elementValue(), row.element());
                    return false;
                })
                .orElseGet(() -> {
                    monsterRepository.save(Monster.builder()
                            .name(row.name())
                            .pageUrl(row.pageUrl())
                            .hp(row.hp())
                            .hittingResistance(row.hittingResistance())
                            .magicResistance(row.magicResistance())
                            .elementValue(row.elementValue())
                            .element(row.element())
                            .build());
                    return true;
                });
    }
}
