package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.StatType;

/**
 * 용병 특성 레벨별 수치 엔티티.
 * 한 특성(MercenaryCharacteristic)에 label × level 조합으로 행이 생성된다.
 *
 * <p>예: "광풍" 특성의 "풍극진멸 데미지" label, 레벨 1~5.
 * <p>각성 특성(level: null)은 이 테이블에 행이 생성되지 않는다.
 *
 * <p>amountValue: 계산기에서 사용하는 파싱된 Float 수치.
 *   "20%" → 20.0, "500" → 500.0. 파싱 불가 시 null.
 * <p>statType: label → StatType 자동 매핑. 알 수 없는 label은 null(관리자 수동 보정).
 */
@Entity
@Table(
        name = "mercenary_characteristic_levels",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_mercenary_characteristic_levels",
                columnNames = {"characteristic_id", "label", "level"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MercenaryCharacteristicLevel {

    /** 특성 레벨 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 수치가 속한 특성 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "characteristic_id", nullable = false)
    private MercenaryCharacteristic characteristic;

    /**
     * 수치 항목명 — 예: "풍극진멸 데미지", "타격저항력", "습격 피해량".
     * gerniverse 원본 텍스트 그대로 저장한다.
     */
    @Column(name = "label", nullable = false, length = 100)
    private String label;

    /**
     * 레벨 (1부터 시작).
     * gerniverse amount 배열의 인덱스 + 1.
     * 각성 사천왕·명왕·주인공: 최대 5, 전설장수: 최대 10.
     */
    @Column(name = "level", nullable = false)
    private Integer level;

    /**
     * 원본 수치 문자열 — 예: "20%", "500".
     * 단위 정보 보존을 위해 그대로 저장한다.
     */
    @Column(name = "amount", nullable = false, length = 20)
    private String amount;

    /**
     * 파싱된 Float 수치 — 예: 20.0, 500.0.
     * "20%" → 20.0, "500" → 500.0.
     * 파싱 불가 시 null. 계산기 합산에서 사용된다.
     */
    @Column(name = "amount_value")
    private Float amountValue;

    /**
     * 계산기 합산 대상 스탯 종류.
     * label 자동 매핑 결과. 알 수 없는 label은 null(관리자 수동 보정 대상).
     * null이면 calculateTotalStats에서 skip된다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", length = 30)
    private StatType statType;

    @Builder
    public MercenaryCharacteristicLevel(MercenaryCharacteristic characteristic,
                                        String label, Integer level,
                                        String amount, Float amountValue,
                                        StatType statType) {
        this.characteristic = characteristic;
        this.label = label;
        this.level = level;
        this.amount = amount;
        this.amountValue = amountValue;
        this.statType = statType;
    }

    /** 관리자 수동 수정 — 수치 문자열·파싱값·스탯타입 갱신 */
    public void update(String amount, Float amountValue, StatType statType) {
        this.amount = amount;
        this.amountValue = amountValue;
        this.statType = statType;
    }
}
