package org.example.gersangtrade.crawler.tasklet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Job 2 — 서버별 육의전 실거래가 수집 및 MaterialPriceHistory UPSERT Tasklet.
 *
 * <p>geota API: GET https://geota.co.kr/api/item?serverId={serverId}
 * 응답: [{itemId, itemUuid, itemName, sellPrice, ...}] 형태의 JSON 배열
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>활성 서버 목록 조회 (13개)</li>
 *   <li>서버별 geota API 호출 → JSON 배열 파싱</li>
 *   <li>itemName 기준 sellPrice 목록 집계</li>
 *   <li>IQR 이상치 제거 후 avg/min/sampleCount 계산</li>
 *   <li>material_price_history UPSERT (동일 날짜+서버+아이템 → 덮어쓰기)</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceCrawlTasklet implements Tasklet {

    private static final String GEOTA_API_URL      = "https://geota.co.kr/api/item";
    private static final String GEOTA_PAGE_URL     = "https://geota.co.kr/gersang/yukeuijeon?serverId=";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsoupFetcher jsoupFetcher;
    private final ServerRepository serverRepository;
    private final ItemRepository itemRepository;
    private final MaterialPriceHistoryRepository materialPriceHistoryRepository;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {

        // 크롤링 실행 날짜 = 오늘 (API 데이터는 현재 등록된 거래 목록 기준)
        LocalDate tradeDate = LocalDate.now();
        log.info("=== PriceCrawlTasklet 시작: {} 가격 데이터 수집 ===", tradeDate);

        List<Server> servers = serverRepository.findByIsActiveTrue();
        log.info("활성 서버 {}개 수집 시작", servers.size());

        int totalUpsertCount = 0;
        for (Server server : servers) {
            try {
                int upserted = processServer(server, tradeDate);
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
     * 단일 서버의 거래 데이터를 API로 수집하고 IQR 집계 후 UPSERT한다.
     *
     * @return UPSERT된 아이템 수
     */
    private int processServer(Server server, LocalDate tradeDate) throws Exception {
        String referer = GEOTA_PAGE_URL + server.getServerId();
        String body = jsoupFetcher.fetchBody(GEOTA_API_URL, referer);

        JsonNode root = MAPPER.readTree(body);
        if (!root.isArray()) {
            log.warn("서버 {} API 응답이 배열이 아님 (skip)", server.getName());
            return 0;
        }

        // itemName → sellPrice 목록 집계
        Map<String, List<Long>> priceMap = new HashMap<>();
        for (JsonNode item : root) {
            String itemName = item.path("itemName").asText("").trim();
            long sellPrice = item.path("sellPrice").asLong(0);

            if (itemName.isBlank() || sellPrice <= 0) continue;
            priceMap.computeIfAbsent(itemName, k -> new ArrayList<>()).add(sellPrice);
        }

        log.debug("서버 {} API 응답: {}종 아이템, {}건 거래 수신",
                server.getName(), priceMap.size(),
                priceMap.values().stream().mapToInt(List::size).sum());

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

            // DB 아이템 조회 — 미등록 아이템은 skip
            Optional<Item> itemOpt = itemRepository.findByName(itemName);
            if (itemOpt.isEmpty()) {
                log.debug("아이템 미등록 (skip): {}", itemName);
                continue;
            }

            IqrCalculator.Result result = resultOpt.get();
            upsertPriceHistory(itemOpt.get(), server, tradeDate, result);
            upsertCount++;
        }

        return upsertCount;
    }

    /**
     * MaterialPriceHistory를 UPSERT한다.
     * 동일 아이템+서버+날짜 조합이 이미 존재하면 update, 없으면 save.
     */
    private void upsertPriceHistory(Item item, Server server, LocalDate tradeDate,
                                    IqrCalculator.Result result) {
        materialPriceHistoryRepository
                .findByItemIdAndServerIdAndTradeDate(item.getId(), server.getServerId(), tradeDate)
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
                                    .tradeDate(tradeDate)
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
