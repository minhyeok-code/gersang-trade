package org.example.gersangtrade.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.auth.security.CustomOidcUserDetails;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.Role;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Google OIDC 로그인 처리 서비스.
 * Google은 OIDC 프로토콜을 사용하므로 DefaultOAuth2UserService가 아닌 OidcUserService를 상속한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Transactional
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String oauthId = oidcUser.getAttribute("sub");
        String email = oidcUser.getAttribute("email");
        String nickname = oidcUser.getAttribute("name");
        String profileImageUrl = oidcUser.getAttribute("picture");

        User user = userRepository
                .findByOauthProviderAndOauthId("google", oauthId)
                .map(this::validateAndReturn)
                .orElseGet(() -> registerNewUser(oauthId, email, nickname, profileImageUrl));

        log.debug("Google OIDC 로그인 처리 완료: userId={}", user.getId());
        return new CustomOidcUserDetails(user, oidcUser);
    }

    private User validateAndReturn(User user) {
        if (user.getDeletedAt() != null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_deleted"), "탈퇴된 계정입니다. 새로 가입해 주세요.");
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_blocked"), "차단된 사용자입니다.");
        }
        return user;
    }

    private User registerNewUser(String oauthId, String email, String nickname, String profileImageUrl) {
        User newUser = User.builder()
                .oauthProvider("google")
                .oauthId(oauthId)
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        User saved = userRepository.save(newUser);
        log.info("Google 신규 사용자 가입: userId={}, email={}", saved.getId(), email);
        return saved;
    }
}
