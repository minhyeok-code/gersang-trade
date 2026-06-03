package org.example.gersangtrade.domain.deck;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.DeckBuffSource;
import org.example.gersangtrade.domain.catalog.Spirit;
import org.example.gersangtrade.domain.user.User;

import java.time.LocalDateTime;

/**
 * 유저 용병 덱 엔티티.
 * 유저가 구성한 용병 슬롯 12개의 스냅샷이다.
 * 저장 시점에 attrXValue·totalResDown을 계산해 캐싱한다.
 *
 * <p>덱은 불변 스냅샷이므로 updatedAt이 없다.
 * 동일 유저가 여러 덱을 보유할 수 있으며, isActive=true인 덱이 현재 활성 덱이다.
 */
@Entity
@Table(name = "user_decks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDeck {

    /** 덱 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 덱 소유자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 덱 이름 */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 현재 활성 덱 여부. 유저당 최대 1개만 true */
    @Column(name = "is_active", nullable = false)
    private boolean active = false;

    /**
     * 속성값(x) 합산 캐시.
     * 덱 저장 시 UserDeckService.calculateTotalStats()가 계산해 저장한다.
     */
    @Column(name = "attr_x_value")
    private Integer attrXValue;

    /**
     * 저항깎 합산 캐시.
     * 덱 저장 시 UserDeckService.calculateTotalStats()가 계산해 저장한다.
     */
    @Column(name = "total_res_down")
    private Integer totalResDown;

    /** 덱에 적용된 정령 1번 슬롯 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spirit_1_id")
    private Spirit spirit1;

    /** 덱에 적용된 정령 2번 슬롯 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spirit_2_id")
    private Spirit spirit2;

    /** 덱에 적용된 진법 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jinbeop_source_id")
    private DeckBuffSource jinbeopSource;

    /** 덱에 적용된 층진 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cheungjin_source_id")
    private DeckBuffSource cheungjinSource;

    /**
     * 공명 레벨 (1~30). null이면 미적용.
     * 주인공 주스텟 증가 + 전체 용병 데미지 증가.
     */
    @Column(name = "gonmyeong_level")
    private Integer gonmyeongLevel;

    /**
     * 가호 레벨 (1~30). null이면 미적용.
     * 전체 용병 주스텟 증가 + 속성값 증가 + 전체 용병 데미지 증가.
     */
    @Column(name = "gaho_level")
    private Integer gahoLevel;

    /** 덱 생성 시각 (불변) */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public UserDeck(User user, String name, boolean active) {
        this.user = user;
        this.name = name;
        this.active = active;
    }

    /** 덱 이름 수정 */
    public void rename(String name) {
        this.name = name;
    }

    /** 계산된 합산 스탯 캐싱 */
    public void applyStats(Integer attrXValue, Integer totalResDown) {
        this.attrXValue = attrXValue;
        this.totalResDown = totalResDown;
    }

    /** 활성 덱 전환 */
    public void activate() {
        this.active = true;
    }

    /** 비활성 전환 */
    public void deactivate() {
        this.active = false;
    }

    /** 덱 단위 버프 선택값 수정 */
    public void updateEffects(Spirit spirit1, Spirit spirit2,
                              DeckBuffSource jinbeopSource,
                              DeckBuffSource cheungjinSource,
                              Integer gonmyeongLevel,
                              Integer gahoLevel) {
        this.spirit1 = spirit1;
        this.spirit2 = spirit2;
        this.jinbeopSource = jinbeopSource;
        this.cheungjinSource = cheungjinSource;
        this.gonmyeongLevel = gonmyeongLevel;
        this.gahoLevel = gahoLevel;
    }
}
