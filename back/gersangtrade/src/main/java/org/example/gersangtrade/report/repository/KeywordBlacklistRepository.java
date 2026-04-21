package org.example.gersangtrade.report.repository;

import org.example.gersangtrade.domain.report.KeywordBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 키워드 블랙리스트 레포지토리.
 * KeywordDetectionService가 캐시 갱신 시 active 패턴 목록을 조회하는 데 사용된다.
 */
public interface KeywordBlacklistRepository extends JpaRepository<KeywordBlacklist, Long> {

    /**
     * 활성화된 키워드·패턴 목록 전체 조회.
     * KeywordDetectionService가 서버 시작 및 캐시 갱신 시 호출한다.
     */
    List<KeywordBlacklist> findByActiveTrue();
}
