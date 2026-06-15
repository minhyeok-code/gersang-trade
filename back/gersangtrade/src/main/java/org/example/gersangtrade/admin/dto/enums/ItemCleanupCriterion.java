package org.example.gersangtrade.admin.dto.enums;

/**
 * 크롤링 적재 아이템 중 정리(삭제) 후보를 판별하는 기준.
 * 관리자가 복수 선택하면 OR 조건으로 합산된다.
 */
public enum ItemCleanupCriterion {

    /** 덱 빌더에 매핑되지 않는 장비 (equip_slot=null, 반지 제외) */
    NO_EQUIP_SLOT("덱 착용 슬롯 없음"),

    /** ORB/WING/TITLE — 서비스 덱 슬롯 미지원 부위 */
    UNSUPPORTED_SLOT("미지원 부위(보주/날개/칭호)"),

    /** is_tradeable=false 세트에 소속된 장비 */
    NON_TRADEABLE_SET("비거래 세트 소속"),

    /** equipment_set_pieces에 등록되지 않은 장비 */
    NOT_SET_PIECE("세트 피스 미등록"),

    /** item_stats가 0건 */
    NO_STATS("스탯 없음"),

    /**
     * 거래·덱·세트피스·구함 어디에도 참조되지 않음.
     * 삭제 전 반드시 목록을 검토할 것.
     */
    UNREFERENCED("미참조(거래/덱 없음)");

    private final String label;

    ItemCleanupCriterion(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
