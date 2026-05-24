package org.example.gersangtrade.user.dto.response;

import org.example.gersangtrade.domain.user.User;

/**
 * 공개 사용자 프로필 응답 DTO.
 * 이메일·OAuth 정보 등 민감 필드를 제외한 공개 정보만 포함한다.
 *
 * @param id              사용자 ID
 * @param nickname        닉네임
 * @param profileImageUrl 프로필 이미지 URL (null: 미설정)
 * @param gameNickname    게임 닉네임 (null: 미설정)
 * @param gameAccessTime  게임 접속 가능 시간대 (null: 미설정)
 * @param grade           현재 등급 표시명 (행상 / 보상 / 객상 / 대상 / 거상)
 * @param gradeStep       호봉 (거상은 null)
 * @param tradeCount      거래 완료 횟수
 * @param mannerScore     매너점수 (0~100)
 */
public record PublicUserProfileResponse(
        Long id,
        String nickname,
        String profileImageUrl,
        String gameNickname,
        String gameAccessTime,
        String grade,
        Integer gradeStep,
        Integer tradeCount,
        Integer mannerScore
) {
    public static PublicUserProfileResponse from(User user) {
        return new PublicUserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getGameNickname(),
                user.getGameAccessTime(),
                user.getGrade() != null ? user.getGrade().getDisplayName() : null,
                user.getGradeStep(),
                user.getTradeCount(),
                user.getMannerScore()
        );
    }
}
