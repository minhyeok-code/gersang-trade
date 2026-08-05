package org.example.gersangtrade.guide.repository;

import org.example.gersangtrade.domain.guide.UserGuide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 유저 가이드 사본 레포지토리.
 * 소프트삭제(deletedAt) 방식이므로 조회 시 항상 deletedAt IS NULL 조건을 붙인다.
 */
public interface UserGuideRepository extends JpaRepository<UserGuide, Long> {

    /** 유저의 활성 사본 목록 (최신순) */
    List<UserGuide> findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    /**
     * 유저의 특정 원본 가이드에 대한 활성 사본.
     * "같은 원본당 활성 사본 1개" 유일성을 앱 레벨에서 강제할 때 사용한다.
     */
    Optional<UserGuide> findByUser_IdAndSourceGuide_IdAndDeletedAtIsNull(Long userId, Long sourceGuideId);

    /** 활성 사본 단건 조회 */
    Optional<UserGuide> findByIdAndDeletedAtIsNull(Long id);
}
