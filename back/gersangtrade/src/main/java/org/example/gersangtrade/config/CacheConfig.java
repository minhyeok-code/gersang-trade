package org.example.gersangtrade.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 캐시 설정.
 * CaffeineCacheManager는 spring.cache.* 프로퍼티로 자동 구성된다.
 * DevTools 재시작 시 CaffeineCacheManager ClassNotFoundException 방지를 위해
 * 직접 CacheManager 빈 등록 대신 Spring Boot 자동 구성 + CacheManagerCustomizer를 사용한다.
 *
 * <p>priceWatch는 마스터 데이터(24h)와 달리 TTL 2분이 필요하므로 registerCustomCache로 별도 등록.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String SERVERS                   = "servers";
    public static final String MERCENARIES               = "mercenaries";
    public static final String MERCENARY_CHARACTERISTICS = "mercenary-characteristics";
    public static final String EQUIPMENT_ALL             = "equipment-all";
    public static final String EQUIPMENT_SLOT            = "equipment-by-slot";
    public static final String RITUALS_BY_ITEM           = "rituals-by-item";
    public static final String RITUALS_ALL               = "rituals-all";
    /** 관심 아이템 시세 — userId:serverId 키, TTL 2분 */
    public static final String PRICE_WATCH               = "priceWatch";

    /** priceWatch만 TTL 2분으로 덮어쓴다. 나머지는 yml global spec(24h) 그대로 유지. */
    @Bean
    public CacheManagerCustomizer<CaffeineCacheManager> priceWatchCacheCustomizer() {
        return manager -> manager.registerCustomCache(
                PRICE_WATCH,
                Caffeine.newBuilder()
                        .expireAfterWrite(120, TimeUnit.SECONDS)
                        .maximumSize(500)
                        .build()
        );
    }
}
