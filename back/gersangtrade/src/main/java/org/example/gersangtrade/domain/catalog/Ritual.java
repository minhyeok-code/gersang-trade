package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gersangtrade.domain.catalog.enums.RitualType;

/**
 * 주술(Ritual) 정의 엔티티.
 * 장비에 적용 가능한 주술의 종류와 성공/대성공 시 표기 마크를 정의한다.
 * 예: "XX주술" — 성공 시 "00", 대성공 시 "**"
 * 실제 적용 가능 장비 목록은 RitualApplicability를 통해 조회한다.
 */
@Entity
@Table(name = "rituals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ritual {

    /** 주술 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 주술 명칭 — 예: "XX주술", "OO주술" */
    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    /** 주술 적용 대상 유형 — WEAPON: 무기 주술, ARMOR: 방어구 주술 */
    @Enumerated(EnumType.STRING)
    @Column(name = "ritual_type", nullable = false, length = 20)
    private RitualType ritualType;

    /** 주술 성공 시 장비에 표시되는 마크 — 예: "00" */
    @Column(name = "success_mark", nullable = false, length = 20)
    private String successMark;

    /** 주술 대성공 시 장비에 표시되는 마크 — 예: "&lt;북두칠성&gt;". 대성공 없는 주술은 null */
    @Column(name = "great_success_mark", nullable = true, length = 20)
    private String greatSuccessMark;

    @Builder
    public Ritual(String displayName, RitualType ritualType,
                  String successMark, String greatSuccessMark) {
        this.displayName = displayName;
        this.ritualType = ritualType;
        this.successMark = successMark;
        this.greatSuccessMark = greatSuccessMark;
    }

    /** 성공/대성공 마크 및 주술 타입 업데이트 — 크롤러 재실행 시 보정용 */
    public void updateMarks(RitualType ritualType, String successMark, String greatSuccessMark) {
        this.ritualType = ritualType;
        this.successMark = successMark;
        this.greatSuccessMark = greatSuccessMark;
    }
}
