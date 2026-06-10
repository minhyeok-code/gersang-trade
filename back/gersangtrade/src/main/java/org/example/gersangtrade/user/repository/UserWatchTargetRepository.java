package org.example.gersangtrade.user.repository;

import org.example.gersangtrade.domain.user.UserWatchTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserWatchTargetRepository extends JpaRepository<UserWatchTarget, Long> {

    List<UserWatchTarget> findByUserIdOrderBySortOrderAscCreatedAtAsc(Long userId);

    long countByUserId(Long userId);

    boolean existsByUserIdAndWatchKey(Long userId, String watchKey);

    @Modifying
    @Query("DELETE FROM UserWatchTarget w WHERE w.id = :id AND w.user.id = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
