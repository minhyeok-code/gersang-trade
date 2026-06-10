package org.example.gersangtrade.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.user.enums.SetComposition;
import org.example.gersangtrade.domain.user.enums.WatchTargetType;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "user_watch_targets",
    uniqueConstraints = @UniqueConstraint(name = "uq_uwt_user_watch_key", columnNames = {"user_id", "watch_key"})
)
public class UserWatchTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 10)
    private WatchTargetType targetType;

    @Column(name = "watch_key", nullable = false, length = 255)
    private String watchKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id")
    private EquipmentSet equipmentSet;

    @Enumerated(EnumType.STRING)
    @Column(name = "composition", length = 20)
    private SetComposition composition;

    @Column(name = "ritual_count")
    private Integer ritualCount;

    @Column(name = "ritual_mark", length = 20)
    private String ritualMark;

    // 2차 순서 변경 기능 전용. MVP에서는 항상 0 (정렬 기준: created_at)
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private UserWatchTarget(User user, WatchTargetType targetType, String watchKey,
                            Item item, EquipmentSet equipmentSet,
                            SetComposition composition, Integer ritualCount, String ritualMark) {
        this.user = user;
        this.targetType = targetType;
        this.watchKey = watchKey;
        this.item = item;
        this.equipmentSet = equipmentSet;
        this.composition = composition;
        this.ritualCount = ritualCount;
        this.ritualMark = ritualMark;
        this.sortOrder = 0;
        this.createdAt = LocalDateTime.now();
    }
}
