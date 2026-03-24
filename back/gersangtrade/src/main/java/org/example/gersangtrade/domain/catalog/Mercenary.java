package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.common.BaseEntity;

/**
 * 용병 엔티티.
 * 게임 내 고용 가능한 용병의 스펙 정보를 보관한다.
 * geota 목록 + gerniverse 상세 페이지 크롤링으로 적재된다.
 *
 * <p>저항깎(resistPierce)과 속성값(elementValue)은
 * 가성비 계산기에서 데미지 계산의 입력값으로 사용된다.
 * 스킬 조건부 저항깎 용병의 경우 별도 처리 정책이 필요하다.
 *
 * <p>고용 재료는 MercenaryMaterial을 통해 Item과 연결된다.
 */
@Entity
@Table(name = "mercenaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mercenary extends BaseEntity {

    /** 용병 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 용병명 — 예: "각성 군다리명왕", "채염사천왕" */
    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    /**
     * 용병 종류 — 예: "각성명왕", "사천왕", "오방신장".
     * gerniverse 상세 페이지 카테고리 배지에서 파싱한다.
     */
    @Column(name = "mercenary_type", length = 50)
    private String mercenaryType;

    /**
     * 저항깎 수치.
     * 몬스터의 저항을 감소시키는 디버프 값. 가성비 계산기의 총 저항깎 구성 요소.
     * 저항깎이 없는 용병은 null.
     * 스킬 조건부 저항깎(예: 공중 몬스터만)은 별도 처리가 필요하며, 현재는 고정 수치만 저장한다.
     */
    @Column(name = "resist_pierce")
    private Integer resistPierce;

    /**
     * 속성값.
     * 속성 추가 데미지 공식 n% = (3x - y) / 2에서 x 구성 요소.
     * 속성값이 없는 용병은 null.
     */
    @Column(name = "element_value")
    private Integer elementValue;

    /**
     * gerniverse에서 수집한 이미지 S3 URL.
     * 크롤링 완료 전에는 null.
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder
    public Mercenary(String name, String mercenaryType,
                     Integer resistPierce, Integer elementValue, String imageUrl) {
        this.name = name;
        this.mercenaryType = mercenaryType;
        this.resistPierce = resistPierce;
        this.elementValue = elementValue;
        this.imageUrl = imageUrl;
    }

    /**
     * 크롤링 상세 파싱 후 스펙 정보 업데이트.
     * mercenaryType은 null/공백이면 기존 값을 유지한다.
     * imageUrl은 null이면 기존 값을 유지한다 (재실행 시 S3 업로드 실패로 null이 덮어쓰이는 것을 방지).
     */
    public void updateSpec(String mercenaryType, Integer resistPierce, Integer elementValue, String imageUrl) {
        if (mercenaryType != null && !mercenaryType.isBlank()) {
            this.mercenaryType = mercenaryType;
        }
        this.resistPierce = resistPierce;
        this.elementValue = elementValue;
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
    }
}
