package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 용병 전직 재료 엔티티.
 * 특정 용병을 완성(전직)하는 데 필요한 재료를 정의한다.
 * 재료는 아이템(materialItemKey) 또는 재료 용병(materialMercenary) 중 하나다.
 *
 * <p>gerniverse 상세 페이지에서 a[href^=/item/] 및 a[href^=/mercenary/] 링크를 파싱하여 적재한다.
 *
 * <p>중복 방지는 delete-reinsert 패턴(재크롤링 시 전체 삭제 후 재삽입)으로 보장한다.
 */
@Entity
@Table(name = "mercenary_materials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MercenaryMaterial {

    /** 전직 재료 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 완성되는 용병 (전직 결과물) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_mercenary_id", nullable = false)
    private Mercenary resultMercenary;

    /**
     * 재료 용병 — 전직 재료가 다른 용병인 경우 설정된다.
     * 아이템 재료인 경우 null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_mercenary_id")
    private Mercenary materialMercenary;

    /**
     * 재료 아이템 키 — gerniverse /item/{key} 경로에서 추출한 URL-decoded 아이템명.
     * 용병 재료인 경우 null.
     */
    @Column(name = "material_item_key", length = 200)
    private String materialItemKey;

    /** 필요 수량 */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 전직 요구 레벨 — 특정 단계에서만 요구되는 경우 설정된다.
     * nullable.
     */
    @Column(name = "required_level")
    private Integer requiredLevel;

    /**
     * 전직 요구 공헌도 — nullable.
     */
    @Column(name = "required_credit")
    private Integer requiredCredit;

    @Builder
    public MercenaryMaterial(Mercenary resultMercenary, Mercenary materialMercenary,
                              String materialItemKey, Integer quantity,
                              Integer requiredLevel, Integer requiredCredit) {
        this.resultMercenary = resultMercenary;
        this.materialMercenary = materialMercenary;
        this.materialItemKey = materialItemKey;
        this.quantity = quantity;
        this.requiredLevel = requiredLevel;
        this.requiredCredit = requiredCredit;
    }
}
