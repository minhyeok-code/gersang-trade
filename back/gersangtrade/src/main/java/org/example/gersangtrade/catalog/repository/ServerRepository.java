package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.config.CacheConfig;
import org.example.gersangtrade.domain.catalog.Server;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 서버 JPA 레포지토리.
 * 서버 목록은 초기 Flyway 시드 데이터로 적재되며, 런타임 조회 전용으로 사용한다.
 */
public interface ServerRepository extends JpaRepository<Server, Integer> {

    /** 활성 서버 목록만 조회 — 거래 등록 시 서버 선택 드롭다운에 사용된다 */
    @Cacheable(CacheConfig.SERVERS)
    List<Server> findByIsActiveTrue();
}
