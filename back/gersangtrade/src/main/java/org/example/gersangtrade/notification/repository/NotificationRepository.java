package org.example.gersangtrade.notification.repository;

import org.example.gersangtrade.domain.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 알림 레포지토리.
 * 사용자별 미읽음 알림 조회 및 일괄 읽음 처리에 사용된다.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자의 미읽음 알림 목록 조회 (최신순).
     * SSE 연결 시점 이후 누적된 알림을 반환하는 데 사용된다.
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.read = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 알림 목록 전체 조회 (최신순, 최대 50건).
     * 알림 센터 화면에서 사용한다.
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findTop50ByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 미읽음 알림 전체 읽음 처리.
     * 알림 센터 열람 시 일괄 읽음 처리에 사용된다.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    void markAllReadByUserId(@Param("userId") Long userId);

    /**
     * 특정 알림 읽음 처리 (본인 소유 확인 포함).
     * 다른 유저의 알림을 임의로 읽음 처리하는 것을 방지한다.
     * 반환값이 0이면 알림이 없거나 타인 소유.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :id AND n.user.id = :userId")
    int markReadByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
