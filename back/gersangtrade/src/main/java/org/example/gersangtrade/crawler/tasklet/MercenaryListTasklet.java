package org.example.gersangtrade.crawler.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job 1 - Step 3: geota 용병 전체 목록 수집 및 UPSERT Tasklet.
 *
 * <p>URL: https://geota.co.kr/gersang/calculator/mercenary?serverId=1
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>geota 용병 목록 HTML 파싱</li>
 *   <li>용병명 추출 및 UPSERT (신규면 저장, 기존이면 skip)</li>
 * </ol>
 *
 * <p>저항깎·속성값·재료 상세는 Step 4 MercenaryDetailWriter에서 gerniverse 파싱으로 채운다.
 *
 * <p>⚠ 용병 목록 HTML 선택자는 실제 사이트 확인 후 조정이 필요하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MercenaryListTasklet implements Tasklet {

    private static final String GEOTA_MERCENARY_URL =
            "https://geota.co.kr/gersang/calculator/mercenary?serverId=1";

    private final JsoupFetcher jsoupFetcher;
    private final MercenaryRepository mercenaryRepository;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {

        log.info("=== MercenaryListTasklet 시작: geota 용병 목록 수집 ===");

        Document doc = jsoupFetcher.fetch(GEOTA_MERCENARY_URL);

        // ⚠ CSR 렌더링 이슈: geota 용병 계산기는 Next.js CSR.
        // li.cursor-pointer 드롭다운은 검색어 입력 후에만 렌더링됨.
        // 선택자 자체(클래스: "cursor-pointer p-2 hover:bg-blue-600 hover:text-white")는 검증됨.
        // TODO: 검색어 없이 드롭다운 렌더링 여부 확인 필요 (crawling-selector-validation.md 미확인 1항).
        Elements mercenaryElements = doc.select("li.cursor-pointer");
        log.info("geota 용병 목록 {}개 파싱됨", mercenaryElements.size());

        int savedCount = 0;
        for (var el : mercenaryElements) {
            String name = el.text().trim();
            if (name.isBlank()) continue;

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
