package org.example.gersangtrade.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.Monster;

import java.time.LocalDateTime;

/**
 * 유저 클리어타임 기록 엔티티.
 * 유저가 특정 몬스터를 클리어한 시간을 저장한다.
 * 데이터 제공 기여에 대한 보상으로 소량의 EXP가 지급된다.
 */
@Entity
@Table(name = "user_clear_times")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserClearTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monster_id", nullable = false)
    private Monster monster;

    /** 사용한 덱 ID — 덱 삭제 시에도 기록은 유지되어야 하므로 FK 대신 단순 컬럼으로 저장 */
    @Column(name = "deck_id")
    private Long deckId;

    /** 클리어 소요 시간 (초 단위) */
    @Column(name = "clear_time_seconds", nullable = false)
    private Integer clearTimeSeconds;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Builder
    public UserClearTime(User user, Monster monster, Long deckId, Integer clearTimeSeconds) {
        this.user = user;
        this.monster = monster;
        this.deckId = deckId;
        this.clearTimeSeconds = clearTimeSeconds;
        this.recordedAt = LocalDateTime.now();
    }
}
