package org.example.gersangtrade.guide.repository;

import org.example.gersangtrade.domain.guide.GuideStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 가이드 원본 스텝 레포지토리.
 */
public interface GuideStepRepository extends JpaRepository<GuideStep, Long> {

    /** 가이드의 스텝을 순번대로 조회 — 원본 조회 및 사본 복제에 사용 */
    List<GuideStep> findByGuideIdOrderByStepOrderAsc(Long guideId);

    /** 가이드 삭제 시 소속 스텝 일괄 삭제 (FK 정리) */
    void deleteByGuideId(Long guideId);

    /** 가이드의 스텝 수 (관리자 목록 표시용) */
    int countByGuideId(Long guideId);
}
