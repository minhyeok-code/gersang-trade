package org.example.gersangtrade.user.dto.request;

/**
 * 사용자 프로필 수정 요청 DTO.
 * 모든 필드는 선택(null 가능) — null이면 해당 필드를 변경하지 않는다.
 *
 * @param nickname         서비스 닉네임 (null이면 미변경)
 * @param gameNickname     게임 내 닉네임 (null이면 미변경)
 * @param gameAccessTime   게임 접속 가능 시간대 (null이면 미변경)
 * @param profileImageUrl  프로필 이미지 URL (null이면 미변경, 빈 문자열이면 삭제)
 */
public record UserProfileUpdateRequest(
        String nickname,
        String gameNickname,
        String gameAccessTime,
        String profileImageUrl
) {
}
