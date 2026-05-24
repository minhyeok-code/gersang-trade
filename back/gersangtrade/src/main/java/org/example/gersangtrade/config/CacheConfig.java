package org.example.gersangtrade.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * 캐시 설정.
 * CaffeineCacheManager는 spring.cache.* 프로퍼티로 자동 구성된다.
 * DevTools 재시작 시 CaffeineCacheManager ClassNotFoundException 방지를 위해
 * 직접 빈 등록 대신 Spring Boot 자동 구성을 사용한다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String SERVERS          = "servers";
    public static final String MERCENARIES      = "mercenaries";
    public static final String EQUIPMENT_SLOT   = "equipment-by-slot";
    public static final String RITUALS_BY_ITEM  = "rituals-by-item";
}
