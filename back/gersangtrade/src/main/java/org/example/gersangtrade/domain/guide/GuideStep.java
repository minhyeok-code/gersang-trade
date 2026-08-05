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

/**
 * 가이드 원본 스텝 엔티티.
 * 하나의 육성 단계를 나타낸다 (예: "고급 천왕검", "사냥터 - 얼음게").
 *
 * <p>스텝은 자기 필드(label/note/iconUrl)로 스스로를 설명한다.
 * 카탈로그 FK(linkedItem/linkedSet/linkedMercenary)는 매물 funnel·이미지 고리로만 쓰인다.
 * <ul>
 *   <li>GET_ITEM/SELL — linkedItem 연결 → 매물 조회</li>
 *   <li>GET_SET       — linkedSet 연결 → 세트 매물 조회 (is_tradeable일 때만)</li>
 *   <li>HIRE_MERCENARY— linkedMercenary는 이미지·정보 표시용 (매물 없음)</li>
 *   <li>HUNT/TRAIT 등 — 연결 없이 iconUrl로 표시</li>
 * </ul>
 */
@Entity
@Table(
        name = "guide_steps",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_guide_step_order",
                columnNames = {"guide_id", "step_order"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 가이드 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guide;

    /** 스텝 순번 (가이드 내 정렬 기준) */
    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    /** 스텝 유형 — 렌더링·매물 버튼 결정 */
    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 30)
    private GuideStepType stepType;

    /** 화면 표시명 — 예: "고급 천왕검", "화염셋" (직접 작성) */
    @Column(name = "label", nullable = false, length = 100)
    private String label;

    /** 부가 설명 — 예: "Lv.260 달성 후 판매", "거불 → 5강" (nullable) */
    @Column(name = "note", length = 255)
    private String note;

    /** 연결 아이템 — GET_ITEM/SELL 매물 funnel (nullable) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_item_id")
    private Item linkedItem;

    /** 연결 세트 — GET_SET 매물 funnel (nullable) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_set_id")
    private EquipmentSet linkedSet;

    /** 연결 용병 — 이미지·정보 표시용 (nullable) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_mercenary_id")
    private Mercenary linkedMercenary;

    /**
     * 스텝 아이콘 URL.
     * 카탈로그 연결이 없거나(사냥터·특성 등), 세트처럼 카탈로그에 이미지가 없는 경우 사용.
     * 연결 아이템/용병이 있으면 그쪽 imageUrl을 우선 쓰고 이 값은 fallback.
     */
    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Builder
    public GuideStep(Guide guide, int stepOrder, GuideStepType stepType,
                     String label, String note,
                     Item linkedItem, EquipmentSet linkedSet, Mercenary linkedMercenary,
                     String iconUrl) {
        this.guide = guide;
        this.stepOrder = stepOrder;
        this.stepType = stepType;
        this.label = label;
        this.note = note;
        this.linkedItem = linkedItem;
        this.linkedSet = linkedSet;
        this.linkedMercenary = linkedMercenary;
        this.iconUrl = iconUrl;
    }

    /** 관리자 수정 — 표시 내용 */
    public void updateContent(String label, String note, String iconUrl) {
        if (label != null && !label.isBlank()) this.label = label;
        this.note = note;
        this.iconUrl = iconUrl;
    }

    /** 순번 변경 */
    public void updateOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }

    /** 관리자 검수 — 유형 변경 */
    public void updateType(GuideStepType stepType) {
        if (stepType != null) this.stepType = stepType;
    }

    /** 관리자 검수 — 카탈로그 연결 재설정 (각 null이면 해당 연결 해제) */
    public void updateLinks(Item linkedItem, EquipmentSet linkedSet, Mercenary linkedMercenary) {
        this.linkedItem = linkedItem;
        this.linkedSet = linkedSet;
        this.linkedMercenary = linkedMercenary;
    }
}
