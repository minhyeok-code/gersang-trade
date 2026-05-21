package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.CharacteristicApplyType;

/**
 * 용병 특성(Characteristic) 엔티티.
 * 각성 사천왕·명왕·주인공·전설장수의 특성 트리 노드 또는 전설장수 패시브를 정의한다.
 * gerniverse RSC payload 크롤링으로 적재된다.
 *
 * <p>각성 특성은 level/point 모두 null이며, MercenaryCharacteristicLevel 행이 생성되지 않는다.
 * 전설장수 패시브도 이 엔티티로 통합 관리된다 (gerniverse가 동일 구조로 제공).
 */
@Entity
@Table(
        name = "mercenary_characteristics",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_mercenary_characteristics_key",
                columnNames = {"characteristic_key"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MercenaryCharacteristic {

    /** 특성 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 특성을 보유한 용병 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercenary_id", nullable = false)
    private Mercenary mercenary;

    /**
     * gerniverse 내부 특성 키 — 예: "gakGwangmok-rhkdvnd".
     * 크롤링 재적재 시 UPSERT 기준 키로 사용된다.
     */
    @Column(name = "characteristic_key", nullable = false, length = 100)
    private String key;

    /** 특성명 — 예: "광풍", "기습" */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 특성 포인트 비용.
     * 각성 특성은 null (레벨 구조 없음).
     */
    @Column(name = "point")
    private Integer point;

    /** 특성 설명 */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 선행 특성 키 (FK 대신 String으로 저장).
     * null이면 선행 조건 없는 루트 노드.
     * 자기 참조 FK 대신 String으로 두어 크롤링 순서에 따른 순환 참조 문제를 피한다.
     */
    @Column(name = "required_characteristic_key", length = 100)
    private String requiredCharacteristicKey;

    /**
     * 특성 적용 방식.
     * NORMAL: 포인트 배분형, SELF_AUTO: 각성 사천왕 각성 특성, ALLY_AUTO: 주인공 국적 버프.
     * SELF_AUTO는 point/level이 null이며 MercenaryCharacteristicLevel 행이 생성되지 않는다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "apply_type", nullable = false, length = 20)
    private CharacteristicApplyType applyType = CharacteristicApplyType.NORMAL;

    @Builder
    public MercenaryCharacteristic(Mercenary mercenary, String key, String name,
                                   Integer point, String description,
                                   String requiredCharacteristicKey,
                                   CharacteristicApplyType applyType) {
        this.mercenary = mercenary;
        this.key = key;
        this.name = name;
        this.point = point;
        this.description = description;
        this.requiredCharacteristicKey = requiredCharacteristicKey;
        this.applyType = applyType != null ? applyType : CharacteristicApplyType.NORMAL;
    }

    /** 관리자 수동 수정 — 이름·포인트·설명·선행특성키 갱신 */
    public void update(String name, Integer point, String description, String requiredCharacteristicKey) {
        this.name = name;
        this.point = point;
        this.description = description;
        this.requiredCharacteristicKey = requiredCharacteristicKey;
    }
}
