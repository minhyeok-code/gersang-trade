package org.example.gersangtrade.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.auth.security.CustomOAuth2UserDetails;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.Role;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * OAuth2 로그인 흐름에서 사용자 정보를 처리하는 서비스.
 *
 * 처리 순서:
 *   1. OAuth2 제공자(Google/Naver)에서 사용자 정보 조회 (super.loadUser)
 *   2. DB에서 기존 사용자 조회
 *   3. 없으면 신규 가입 처리, 있으면 차단 여부 확인
 *   4. CustomOAuth2UserDetails 반환 → Spring Security 인증 컨텍스트에 저장됨
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 제공자로부터 사용자 정보 수신
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 제공자별 사용자 정보 추출
        OAuthUserInfo userInfo = extractUserInfo(registrationId, attributes);

        User user = userRepository
                .findByOauthProviderAndOauthId(registrationId, userInfo.oauthId())
                .map(existing -> validateAndReturn(existing))
                .orElseGet(() -> registerNewUser(registrationId, userInfo));

        log.debug("OAuth2 로그인 처리 완료: userId={}, provider={}", user.getId(), registrationId);
        return new CustomOAuth2UserDetails(user, attributes);
    }

    /** 제공자별 사용자 정보 파싱 */
    @SuppressWarnings("unchecked")
    private OAuthUserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> new OAuthUserInfo(
                    (String) attributes.get("sub"),
                    (String) attributes.get("email"),
                    (String) attributes.get("name")
            );
            case "naver" -> {
                // 네이버 응답 구조: { "resultcode": "00", "response": { "id": "...", "email": "...", "name": "..." } }
                // application.yml에서 user-name-attribute=response 설정으로 attributes에 "response" 키로 진입
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                yield new OAuthUserInfo(
                        (String) response.get("id"),
                        (String) response.get("email"),
                        (String) response.get("name")
                );
            }
            // Kakao는 MVP 범위 외 — 추후 확장 시 추가
            default -> throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"),
                    "지원하지 않는 OAuth2 제공자: " + registrationId
            );
        };
    }

    /** 탈퇴/차단 여부 검사 후 기존 사용자 반환 */
    private User validateAndReturn(User user) {
        // 소프트 딜리트된 계정 — 탈퇴 후 재로그인 시도 시 신규 가입과 동일하게 처리
        if (user.getDeletedAt() != null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_deleted"),
                    "탈퇴된 계정입니다. 새로 가입해 주세요."
            );
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_blocked"),
                    "차단된 사용자입니다."
            );
        }
        return user;
    }

    /** 신규 사용자 가입 처리 */
    private User registerNewUser(String provider, OAuthUserInfo info) {
        User newUser = User.builder()
                .oauthProvider(provider)
                .oauthId(info.oauthId())
                .email(info.email())
                .nickname(info.nickname())
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        User saved = userRepository.save(newUser);
        log.info("신규 사용자 가입: userId={}, provider={}, email={}", saved.getId(), provider, info.email());
        return saved;
    }

    /** OAuth2 제공자에서 추출한 사용자 정보 */
    private record OAuthUserInfo(String oauthId, String email, String nickname) {}
}
