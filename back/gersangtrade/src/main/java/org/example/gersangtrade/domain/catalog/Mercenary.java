package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.Nation;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.common.BaseEntity;

import java.time.LocalDateTime;

/**
 * 용병 엔티티.
 * gerniverse 목록·상세 페이지 크롤링으로 적재된다.
 *
 * <p>ListTasklet(Step 3)에서 name만 저장되고, DetailWriter(Step 4)에서
 * category / nation / nature / stats / materials / imageUrl / crawledAt 이 채워진다.
 *
 * <p>능력치(스탯)는 MercenaryStat을 통해 StatType별로 저장된다.
 * 고용 재료는 MercenaryMaterial을 통해 연결된다.
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

    /** 용병 풀네임 — 예: "각성 군다리명왕", "채염사천왕" */
    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    /**
     * gerniverse 내부 식별 키 — 예: "gakGwangmok".
     * ListTasklet 단계에서는 null. DetailWriter 파싱 후 채워진다.
     */
    @Column(name = "mercenary_key", length = 100, unique = true)
    private String key;

    /** 용병 카테고리 — 예: 각성사천왕, 명왕, 전설장수. gerniverse 배지에서 파싱한다 */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private MercenaryCategory category;

    /** 출신 국가 */
    @Enumerated(EnumType.STRING)
    @Column(name = "nation", length = 20)
    private Nation nation;

    /** 속성 (fire / water / thunder / air / earth / 무속성) */
    @Enumerated(EnumType.STRING)
    @Column(name = "nature", length = 20)
    private Nature nature;

    /**
     * 속성값 (natureValue).
     * 속성 추가 데미지 공식 n% = (3x - y) / 2에서 x 구성 요소.
     * MercenaryStat(ELEMENT_VALUE)에도 저장되지만, 계산기 접근 편의를 위해 여기서도 유지한다.
     * 무속성 용병은 null.
     */
    @Column(name = "nature_value")
    private Integer natureValue;

    /** 출시 예정 여부 — true이면 크롤링 대상에서 제외된다 */
    @Column(name = "is_coming_soon", nullable = false)
    private boolean comingSoon = false;

    /**
     * gerniverse에서 수집한 이미지 S3 URL.
     * DetailWriter 처리 전에는 null.
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * 상세 크롤링 완료 시각.
     * ListTasklet 직후에는 null. DetailWriter 처리 완료 후 설정된다.
     * null이면 DetailReader가 처리 대상으로 선택한다.
     */
    @Column(name = "crawled_at")
    private LocalDateTime crawledAt;

    @Builder
    public Mercenary(String name, String key, MercenaryCategory category,
                     Nation nation, Nature nature, Integer natureValue,
                     boolean comingSoon, String imageUrl) {
        this.name = name;
        this.key = key;
        this.category = category;
        this.nation = nation;
        this.nature = nature;
        this.natureValue = natureValue;
        this.comingSoon = comingSoon;
        this.imageUrl = imageUrl;
    }

    /**
     * 크롤링 상세 파싱 후 스펙 정보 업데이트.
     * imageUrl은 null이면 기존 값을 유지한다 (S3 업로드 실패로 null이 덮어쓰이는 것을 방지).
     * key는 null/공백이면 기존 값을 유지한다.
     */
    public void updateSpec(String key, MercenaryCategory category, Nation nation,
                           Nature nature, Integer natureValue, boolean comingSoon,
                           String imageUrl, LocalDateTime crawledAt) {
        if (key != null && !key.isBlank()) {
            this.key = key;
        }
        if (category != null) {
            this.category = category;
        }
        if (nation != null) {
            this.nation = nation;
        }
        if (nature != null) {
            this.nature = nature;
        }
        this.natureValue = natureValue;
        this.comingSoon = comingSoon;
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
        this.crawledAt = crawledAt;
    }
}
