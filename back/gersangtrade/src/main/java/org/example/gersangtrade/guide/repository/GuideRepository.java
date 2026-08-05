package org.example.gersangtrade.guide.repository;

import org.example.gersangtrade.domain.guide.Guide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 가이드 원본 레포지토리.
 */
public interface GuideRepository extends JpaRepository<Guide, Long> {

    /** 공개된 전체 가이드 */
    List<Guide> findByPublishedTrue();

    /** 관리자용 — 비공개 포함 전체 (id순) */
    List<Guide> findAllByOrderByIdAsc();

    /**
     * 대상 사천왕이 주어진 용병 목록에 포함되는 공개 가이드.
     * 유저 덱의 용병들과 매칭해 추천 가이드를 고를 때 사용한다.
     */
    List<Guide> findByPublishedTrueAndTargetMercenary_IdIn(Collection<Long> mercenaryIds);

    /** 공개된 단일 가이드 조회 */
    Optional<Guide> findByIdAndPublishedTrue(Long id);
}
