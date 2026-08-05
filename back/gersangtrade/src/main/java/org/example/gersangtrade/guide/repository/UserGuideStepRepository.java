package org.example.gersangtrade.guide.repository;

import org.example.gersangtrade.domain.guide.UserGuideStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 유저 가이드 스텝 레포지토리.
 */
public interface UserGuideStepRepository extends JpaRepository<UserGuideStep, Long> {

    /** 사본의 스텝을 순번대로 조회 */
    List<UserGuideStep> findByUserGuideIdOrderByStepOrderAsc(Long userGuideId);

    /** 사본의 전체 스텝 수 */
    int countByUserGuideId(Long userGuideId);

    /** 사본의 완료(체크된) 스텝 수 */
    int countByUserGuideIdAndCheckedAtIsNotNull(Long userGuideId);

    /** 사본의 현재 최대 순번 — 스텝 추가 시 맨 뒤에 붙이기 위함 (스텝 없으면 0) */
    @Query("SELECT COALESCE(MAX(s.stepOrder), 0) FROM UserGuideStep s WHERE s.userGuide.id = :userGuideId")
    int findMaxStepOrder(@Param("userGuideId") Long userGuideId);
}
