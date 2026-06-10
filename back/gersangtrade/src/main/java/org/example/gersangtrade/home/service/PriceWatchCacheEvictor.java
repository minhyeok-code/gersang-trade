package org.example.gersangtrade.home.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.config.CacheConfig;
import org.example.gersangtrade.home.event.TradeConfirmedEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 거래 확정 등 시세 데이터 변경 시 priceWatch 캐시를 무효화한다.
 *
 * <p>거래 확정 이벤트는 트랜잭션 커밋 후에 처리되어야 한다.
 * 트랜잭션 내부에서 캐시를 비우면 커밋 전에 다른 요청이 stale 데이터를 재캐싱할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class PriceWatchCacheEvictor {

    private final CacheManager cacheManager;

    /** 거래 확정 트랜잭션 커밋 후 전체 priceWatch 캐시 삭제 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTradeConfirmed(TradeConfirmedEvent event) {
        evictAll();
    }

    /** 전체 사용자·서버 priceWatch 캐시 삭제 */
    public void evictAll() {
        Cache cache = cacheManager.getCache(CacheConfig.PRICE_WATCH);
        if (cache != null) {
            cache.clear();
        }
    }
}
