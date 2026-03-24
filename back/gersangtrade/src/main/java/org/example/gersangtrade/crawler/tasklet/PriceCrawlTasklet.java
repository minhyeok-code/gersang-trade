package org.example.gersangtrade.crawler.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.ServerRepository;
import org.example.gersangtrade.crawler.repository.MaterialPriceHistoryRepository;
import org.example.gersangtrade.crawler.util.IqrCalculator;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Server;
import org.example.gersangtrade.domain.crawler.MaterialPriceHistory;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Job 2 — 서버별 육의전 실거래가 수집 및 MaterialPriceHistory UPSERT Tasklet.
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>활성 서버 목록 조회 (13개)</li>
 *   <li>서버별 geota 육의전 페이지 순환 수집 (빈 행 감지 시 종료)</li>
 *   <li>아이템별 단가 목록 수집 후 IQR 이상치 제거 및 집계</li>
 *   <li>material_price_history UPSERT (동일 연월+서버+아이템 → 덮어쓰기)</li>
 * </ol>
 *
 * <p>⚠ geota 육의전 HTML 선택자는 실제 사이트 확인 후 조정 필요.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceCrawlTasklet implements Tasklet {

    /** geota 육의전 기본 URL — serverId 파라미터로 서버 구분 */
    private static final String GEOTA_YUKEUIJEON_URL =
            "https://geota.co.kr/gersang/yukeuijeon?serverId=";

    /** 페이지 파라미터 — 서버사이드 페이지네이션 여부 확인 후 조정 필요 */
    private static final String PAGE_PARAM = "&page=";

    /** 무한 루프 방지용 최대 페이지 수 */
    private static final int MAX_PAGES = 50;

    private final JsoupFetcher jsoupFetcher;
    private final ServerRepository serverRepository;
    private final ItemRepository itemRepository;
    private final MaterialPriceHistoryRepository materialPriceHistoryRepository;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {

        // 집계 대상 연월 = 직전 달 (매월 1일 실행 → 지난 달 실거래가 집계)
        // CalculatorService가 조회하는 기준("직전 달")과 정합성을 맞춘다
        String yearMonth = YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("=== PriceCrawlTasklet 시작: {} 가격 데이터 수집 ===", yearMonth);

        List<Server> servers = serverRepository.findByIsActiveTrue();
        log.info("활성 서버 {}개 수집 시작", servers.size());

        int totalUpsertCount = 0;
        for (Server server : servers) {
            try {
                int upserted = processServer(server, yearMonth);
                totalUpsertCount += upserted;
                log.info("서버 {} 완료: {}건 UPSERT", server.getName(), upserted);
            } catch (Exception e) {
                log.warn("서버 {} 처리 실패 (skip): {}", server.getName(), e.getMessage());
            }
        }

        log.info("=== PriceCrawlTasklet 완료: 총 {}건 UPSERT ===", totalUpsertCount);
        return RepeatStatus.FINISHED;
    }

    /**
     * 단일 서버의 육의전 페이지를 순환 수집하고 IQR 집계 후 UPSERT한다.
     *
     * @return UPSERT된 아이템 수
     */
    private int processServer(Server server, String yearMonth) throws Exception {
        // 아이템명 → 단가 목록 (서버 전체 수집분)
        Map<String, List<Long>> priceMap = new HashMap<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            String url = GEOTA_YUKEUIJEON_URL + server.getServerId() + PAGE_PARAM + page;
            Document doc = jsoupFetcher.fetch(url);

            // ⚠ 실제 선택자 확인 필요
            // geota 육의전 거래 행 선택자 (거래 테이블 tbody 하위 tr)
            Elements rows = doc.select("table tbody tr");
            if (rows.isEmpty()) {
                log.debug("서버 {} page {} 빈 데이터 → 수집 종료", server.getName(), page);
                break;
            }

            for (Element row : rows) {
                // ⚠ 실제 td 순서 확인 필요 (아이템명, 수량, 단가 컬럼 위치)
                Elements cells = row.select("td");
                if (cells.size() < 3) continue;

                String itemName = cells.get(0).text().trim();   // 1번째 td: 아이템명
                String priceText = cells.get(2).text().trim();  // 3번째 td: 단가

                if (itemName.isBlank() || priceText.isBlank()) continue;

                try {
                    // 숫자 이외 문자 제거 후 파싱 (예: "1,234냥" → 1234)
                    long unitPrice = Long.parseLong(priceText.replaceAll("[^0-9]", ""));
                    if (unitPrice > 0) {
                        priceMap.computeIfAbsent(itemName, k -> new ArrayList<>()).add(unitPrice);
                    }
                } catch (NumberFormatException e) {
                    log.debug("단가 파싱 실패 (skip): {} — {}", itemName, priceText);
                }
            }

            log.debug("서버 {} page {} 완료: {}행 처리", server.getName(), page, rows.size());
        }

        // 아이템별 IQR 집계 및 UPSERT
        int upsertCount = 0;
        for (Map.Entry<String, List<Long>> entry : priceMap.entrySet()) {
            String itemName = entry.getKey();
            List<Long> prices = entry.getValue();

            Optional<IqrCalculator.Result> resultOpt = IqrCalculator.calculate(prices);
            if (resultOpt.isEmpty()) {
                log.debug("최소 샘플 미달 (skip): 서버={}, 아이템={}, 건수={}",
                        server.getName(), itemName, prices.size());
                continue;
            }

            // DB에서 아이템 조회 — 미등록 아이템은 skip
            Optional<Item> itemOpt = itemRepository.findByName(itemName);
            if (itemOpt.isEmpty()) {
                log.debug("아이템 미등록 (skip): {}", itemName);
                continue;
            }

            IqrCalculator.Result result = resultOpt.get();
            upsertPriceHistory(itemOpt.get(), server, yearMonth, result);
            upsertCount++;
        }

        return upsertCount;
    }

    /**
     * MaterialPriceHistory를 UPSERT한다.
     * 동일 아이템+서버+연월 조합이 이미 존재하면 update, 없으면 save.
     */
    private void upsertPriceHistory(Item item, Server server, String yearMonth,
                                    IqrCalculator.Result result) {
        materialPriceHistoryRepository
                .findByItemIdAndServerIdAndYearMonth(item.getId(), server.getServerId(), yearMonth)
                .ifPresentOrElse(
                        existing -> {
                            existing.update(result.avgPrice(), result.minPrice(), result.sampleCount());
                            log.debug("가격 이력 업데이트: 서버={}, 아이템={}, avg={}",
                                    server.getName(), item.getName(), result.avgPrice());
                        },
                        () -> {
                            materialPriceHistoryRepository.save(MaterialPriceHistory.builder()
                                    .item(item)
                                    .server(server)
                                    .yearMonth(yearMonth)
                                    .avgPrice(result.avgPrice())
                                    .minPrice(result.minPrice())
                                    .sampleCount(result.sampleCount())
                                    .build());
                            log.debug("가격 이력 신규 저장: 서버={}, 아이템={}, avg={}",
                                    server.getName(), item.getName(), result.avgPrice());
                        }
                );
    }
}
