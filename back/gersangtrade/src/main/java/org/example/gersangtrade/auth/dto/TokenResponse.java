package org.example.gersangtrade.auth.dto;

/**
 * 토큰 재발급 응답 DTO.
 * RT는 Cookie로 전달되므로 body에는 AT만 포함한다.
 */
public record TokenResponse(String accessToken) {
}
