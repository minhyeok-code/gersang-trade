package org.example.gersangtrade.user.dto.response;

import org.example.gersangtrade.domain.user.User;
import java.time.LocalDateTime;

/**
 * 사용자 프로필 응답 DTO.
 * 마이페이지 및 타 유저 프로필 조회에서 공통으로 사용한다.
 *
 * @param id               사용자 ID
 * @param nickname         닉네임
 * @param profileImageUrl  프로필 이미지 URL (null: 미설정)
 * @param gameNickname     게임 닉네임 (null: 미설정)
 * @param gameAccessTime   게임 접속 가능 시간대 (null: 미설정)
 * @param grade            현재 등급 표시명 (행상 / 보상 / 객상 / 대상 / 거상)
 * @param gradeStep        호봉 (거상은 null)
 * @param totalExp         누적 EXP
 * @param mannerScore      매너점수 (0~100)
 * @param tradeCount       거래 완료 횟수
 * @param serverId         기본 서버 ID (미설정 시 null)
 * @param serverName       기본 서버명 (미설정 시 null)
 * @param createdAt        가입 일시
 */
public record UserProfileResponse(
        Long id,
        String nickname,
        String profileImageUrl,
        String gameNickname,
        String gameAccessTime,
        String grade,
        Integer gradeStep,
        Long totalExp,
        Integer mannerScore,
        Integer tradeCount,
        Integer serverId,
        String serverName,
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User user) {
        var server = user.getServer();
        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getGameNickname(),
                user.getGameAccessTime(),
                user.getGrade() != null ? user.getGrade().getDisplayName() : null,
                user.getGradeStep(),
                user.getTotalExp(),
                user.getMannerScore(),
                user.getTradeCount(),
                server != null ? server.getServerId() : null,
                server != null ? server.getName() : null,
                user.getCreatedAt()
        );
    }
}
