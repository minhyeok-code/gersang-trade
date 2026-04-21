package org.example.gersangtrade.domain.report.enums;

/**
 * 신고 접수 주체 유형.
 * 사용자가 직접 신고한 경우(USER)와 서버 자동 감지(SYSTEM)를 구분한다.
 * SYSTEM 신고의 경우 reporter_id는 null로 저장된다.
 */
public enum ReporterType {

    /** 사용자가 직접 신고 버튼을 눌러 접수한 신고 */
    USER,

    /** 채팅 메시지 키워드/패턴 자동 감지로 서버가 생성한 신고 */
    SYSTEM
}
