package org.example.gersangtrade.crawler.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.crawler.parser.GerniverseParser;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.jsoup.nodes.Document;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Job 1 - Step 3: gerniverse 용병 전체 목록 수집 및 UPSERT Tasklet.
 *
 * <p>URL: https://gerniverse.app/mercenary
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>gerniverse /mercenary 페이지의 JSON-LD ItemList 파싱</li>
 *   <li>용병명 추출 및 UPSERT (신규면 저장, 기존이면 skip)</li>
 * </ol>
 *
 * <p>category·stats·재료 상세는 Step 4 MercenaryDetailWriter에서 gerniverse 상세 페이지 파싱으로 채운다.
 * crawledAt이 null인 용병이 Step 4의 처리 대상이 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MercenaryListTasklet implements Tasklet {

    private static final String GERNIVERSE_MERCENARY_LIST_URL = "https://gerniverse.app/mercenary";

    private final JsoupFetcher jsoupFetcher;
    private final MercenaryRepository mercenaryRepository;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {

        log.info("=== MercenaryListTasklet 시작: gerniverse 용병 목록 수집 ===");

        Document doc = jsoupFetcher.fetch(GERNIVERSE_MERCENARY_LIST_URL);

        // JSON-LD ItemList 파싱
        List<GerniverseParser.MercenaryListItem> items = GerniverseParser.parseMercenaryList(doc);
        log.info("gerniverse 용병 목록 {}개 파싱됨", items.size());

        int savedCount = 0;
        for (GerniverseParser.MercenaryListItem item : items) {
            String name = item.name();
            if (name.isBlank()) continue;

            // 이미 등록된 용병은 skip (crawledAt 초기화 없이 유지)
            if (mercenaryRepository.findByName(name).isEmpty()) {
                mercenaryRepository.save(Mercenary.builder().name(name).build());
                savedCount++;
                log.debug("용병 신규 저장: {}", name);
            }
        }

        log.info("=== MercenaryListTasklet 완료: 신규 용병 {}개 저장 ===", savedCount);
        return RepeatStatus.FINISHED;
    }
}
