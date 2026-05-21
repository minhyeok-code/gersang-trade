package org.example.gersangtrade.user.dto.request;

/**
 * 사용자 기본 서버 변경 요청 DTO.
 *
 * @param serverId 선택할 서버 ID (1~13). null이면 서버 선택 해제.
 */
public record UserServerUpdateRequest(Integer serverId) {
}
