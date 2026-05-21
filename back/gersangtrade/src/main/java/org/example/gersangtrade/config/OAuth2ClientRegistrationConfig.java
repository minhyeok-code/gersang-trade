package org.example.gersangtrade.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

import java.util.ArrayList;
import java.util.List;

/**
 * OAuth2 클라이언트 등록 설정.
 *
 * <p>Spring Boot 자동 설정(application.yml의 registration 섹션) 대신 직접 빈을 생성한다.
 * 이유: 환경변수가 설정되지 않은 provider의 client-id가 비어있으면 Spring Boot가
 * 시작 시 {@code IllegalStateException}을 던지기 때문.
 * 빈을 직접 정의하면 Spring Boot의 {@code OAuth2ClientAutoConfiguration}이 자동으로 back-off한다.
 *
 * <p>env var가 비어있는 provider는 등록에서 제외된다.
 * 최소 1개의 provider가 등록되어야 앱이 정상 동작한다 (둘 다 없으면 경고 로그).
 */
@Slf4j
@Configuration
public class OAuth2ClientRegistrationConfig {

    @Value("${oauth2.google.client-id:}")
    private String googleClientId;

    @Value("${oauth2.google.client-secret:}")
    private String googleClientSecret;

    @Value("${oauth2.naver.client-id:}")
    private String naverClientId;

    @Value("${oauth2.naver.client-secret:}")
    private String naverClientSecret;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();

        if (isConfigured(googleClientId, googleClientSecret)) {
            registrations.add(googleRegistration());
            log.info("OAuth2 provider 등록: Google");
        } else {
            log.warn("OAuth2 Google provider 비활성화 — GOOGLE_CLIENT_ID/SECRET 환경변수 미설정");
        }

        if (isConfigured(naverClientId, naverClientSecret)) {
            registrations.add(naverRegistration());
            log.info("OAuth2 provider 등록: Naver");
        } else {
            log.warn("OAuth2 Naver provider 비활성화 — NAVER_CLIENT_ID/SECRET 환경변수 미설정");
        }

        if (registrations.isEmpty()) {
            log.warn("OAuth2 provider가 하나도 등록되지 않았습니다. 소셜 로그인이 동작하지 않습니다.");
            // InMemoryClientRegistrationRepository는 빈 리스트 허용 안 함 → 람다로 빈 Iterable 반환
            // 실제 OAuth2 요청 시 UnknownRegistrationException이 발생하며 로그인 불가 상태임을 명확히 함
            return registrationId -> null;
        }

        return new InMemoryClientRegistrationRepository(registrations);
    }

    private boolean isConfigured(String clientId, String clientSecret) {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    /** Google OAuth2 — OIDC 기반 (Spring Security 공식 지원) */
    private ClientRegistration googleRegistration() {
        return ClientRegistration.withRegistrationId("google")
                .clientId(googleClientId)
                .clientSecret(googleClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "email", "profile")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Google")
                .build();
    }

    /**
     * Naver OAuth2 — Spring Security 기본 provider 미지원으로 직접 엔드포인트 지정.
     * user-name-attribute=response: 네이버 응답이 {"response": {"id":...}} 구조로 감싸져 있음.
     */
    private ClientRegistration naverRegistration() {
        return ClientRegistration.withRegistrationId("naver")
                .clientId(naverClientId)
                .clientSecret(naverClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("name", "email")
                .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                .tokenUri("https://nid.naver.com/oauth2.0/token")
                .userInfoUri("https://openapi.naver.com/v1/nid/me")
                .userNameAttributeName("response")
                .clientName("Naver")
                .build();
    }
}
