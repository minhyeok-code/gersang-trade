package org.example.gersangtrade.domain.guide;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.guide.enums.GuideStepType;

import java.time.LocalDateTime;

/**
 * 유저 가이드 스텝 엔티티 (수정 가능 + 진행도).
 * 원본 {@link GuideStep}을 복제해 만들어지며, 유저가 자유롭게 추가/삭제/순서변경/수정한다.
 *
 * <p>진행도는 별도 테이블 없이 이 엔티티의 {@code checkedAt}으로 관리한다.
 * null이면 미완료, 값이 있으면 완료(체크한 시각).
 */
@Entity
@Table(name = "user_guide_steps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGuideStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 유저 가이드 사본 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_guide_id", nullable = false)
    private UserGuide userGuide;

    /** 스텝 순번 — 유저가 재정렬 가능 */
    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    /** 스텝 유형 */
    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 30)
    private GuideStepType stepType;

    /** 화면 표시명 */
    @Column(name = "label", nullable = false, length = 100)
    private String label;

    /** 부가 설명 (nullable) */
    @Column(name = "note", length = 255)
    private String note;

    /** 연결 아이템 — 매물 funnel (nullable) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_item_id")
    private Item linkedItem;

    /** 연결 세트 — 매물 funnel (nullable) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_set_id")
    private EquipmentSet linkedSet;

    /** 연결 용병 — 이미지·정보 표시용 (nullable) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_mercenary_id")
    private Mercenary linkedMercenary;

    /** 스텝 아이콘 URL — 카탈로그 이미지 없을 때 fallback (nullable) */
    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    /** 유저가 직접 추가한 스텝 여부 — 원본 유래(false)와 구분 */
    @Column(name = "is_custom", nullable = false)
    private boolean custom = false;

    /** 완료 체크 시각 — null이면 미완료 (진행도) */
    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Builder
    public UserGuideStep(UserGuide userGuide, int stepOrder, GuideStepType stepType,
                         String label, String note,
                         Item linkedItem, EquipmentSet linkedSet, Mercenary linkedMercenary,
                         String iconUrl, boolean custom) {
        this.userGuide = userGuide;
        this.stepOrder = stepOrder;
        this.stepType = stepType;
        this.label = label;
        this.note = note;
        this.linkedItem = linkedItem;
        this.linkedSet = linkedSet;
        this.linkedMercenary = linkedMercenary;
        this.iconUrl = iconUrl;
        this.custom = custom;
    }

    /** 완료 체크 */
    public void check() {
        this.checkedAt = LocalDateTime.now();
    }

    /** 완료 해제 */
    public void uncheck() {
        this.checkedAt = null;
    }

    /** 표시 내용 수정 */
    public void updateContent(String label, String note, String iconUrl) {
        if (label != null && !label.isBlank()) this.label = label;
        this.note = note;
        this.iconUrl = iconUrl;
    }

    /** 순번 변경 */
    public void updateOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }
}
