package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.Element;

/**
 * 몬스터 엔티티.
 * 거상짱 몬스터 페이지에서 크롤링한 정보를 저장한다.
 *
 * <p>UNIQUE 제약: (name, page_url) — 동일 페이지에서 같은 이름의 몬스터 중복 방지.
 * 다른 페이지에 동명의 몬스터가 있을 수 있으므로 pageUrl을 함께 기준 키로 사용한다.
 */
@Entity
@Table(
        name = "monsters",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_monsters_name_page_url",
                columnNames = {"name", "page_url"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Monster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 몬스터 이름 (원문 그대로 — 등급/속성 괄호 포함) */
    @Column(nullable = false, length = 100)
    private String name;

    /** 출처 페이지 URL */
    @Column(name = "page_url", nullable = false, length = 200)
    private String pageUrl;

    /** 생명력 — 구형 몹은 null */
    @Column(nullable = true)
    private Long hp;

    /** 타격저항력(%) — null이면 데이터 없음 */
    @Column(nullable = true)
    private Integer hittingResistance;

    /** 마법저항력(%) — null이면 데이터 없음 */
    @Column(nullable = true)
    private Integer magicResistance;

    /** 속성값 수치 — null이면 데이터 없음 */
    @Column(nullable = true)
    private Integer elementValue;

    /**
     * 속성 종류.
     * WATER/THUNDER/FIRE/WIND/EARTH: 해당 속성
     * NONE: 명시적 무속성 (明속성)
     * null: 데이터 없음 (구형 몹 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private Element element;

    @Builder
    public Monster(String name, String pageUrl, Long hp,
                   Integer hittingResistance, Integer magicResistance,
                   Integer elementValue, Element element) {
        this.name = name;
        this.pageUrl = pageUrl;
        this.hp = hp;
        this.hittingResistance = hittingResistance;
        this.magicResistance = magicResistance;
        this.elementValue = elementValue;
        this.element = element;
    }

    /** 크롤링 재실행 시 수치 업데이트 */
    public void update(Long hp, Integer hittingResistance, Integer magicResistance,
                       Integer elementValue, Element element) {
        this.hp = hp;
        this.hittingResistance = hittingResistance;
        this.magicResistance = magicResistance;
        this.elementValue = elementValue;
        this.element = element;
    }
}
