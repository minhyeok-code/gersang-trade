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
 * <p>UNIQUE 제약: name — 몬스터 이름을 전역 upsert 키로 사용.
 */
@Entity
@Table(
        name = "monsters",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_monsters_name",
                columnNames = {"name"}
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

    /**
     * 관리자가 S3에 직접 업로드한 이미지 URL.
     * 업로드 전에는 null.
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

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

    /**
     * 공개 노출 숨김 여부.
     * true이면 공개 API에서 제외된다.
     *
     * <p>자동 숨김 조건 (applyHiddenRule):
     * <ul>
     *   <li>elementValue가 null인 경우 (속성값 없는 몬스터)</li>
     *   <li>이름에 (明) 또는 （明） 가 포함된 경우 (명속성 무속성 몬스터)</li>
     * </ul>
     * 관리자가 수동으로 토글할 수 있다.
     */
    @Column(nullable = false)
    private boolean hidden = true;

    @Builder
    public Monster(String name, Long hp,
                   Integer hittingResistance, Integer magicResistance,
                   Integer elementValue, Element element) {
        this.name = name;
        this.hp = hp;
        this.hittingResistance = hittingResistance;
        this.magicResistance = magicResistance;
        this.elementValue = elementValue;
        this.element = element;
        this.hidden = computeHidden(name, elementValue);
    }

    /** 관리자 직접 업로드 후 S3 URL 저장 */
    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /** 관리자 이름 수정 */
    public void updateName(String name) {
        this.name = name;
    }

    /** 크롤링 재실행 시 수치 업데이트 + 숨김 여부 자동 재계산 */
    public void update(Long hp, Integer hittingResistance, Integer magicResistance,
                       Integer elementValue, Element element) {
        this.hp = hp;
        this.hittingResistance = hittingResistance;
        this.magicResistance = magicResistance;
        this.elementValue = elementValue;
        this.element = element;
        this.hidden = computeHidden(this.name, elementValue);
    }

    /** 관리자 수동 노출 여부 설정 */
    public void updateHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * 이름·속성값 기반 자동 숨김 여부 계산.
     * elementValue 없거나 明속성 몬스터이면 true (숨김).
     */
    public static boolean computeHidden(String name, Integer elementValue) {
        if (elementValue == null) return true;
        // 반각/전각 괄호 모두 처리: (明), (明）, （明), （明）
        return name != null && name.matches(".*[（(]明[）)].*");
    }
}
