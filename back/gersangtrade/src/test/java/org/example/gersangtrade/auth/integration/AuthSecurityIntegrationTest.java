package org.example.gersangtrade.auth.integration;

import jakarta.servlet.http.Cookie;
import org.example.gersangtrade.admin.controller.CrawlerAdminController;
import org.example.gersangtrade.auth.controller.AuthController;
import org.example.gersangtrade.auth.handler.OAuth2LoginFailureHandler;
import org.example.gersangtrade.auth.handler.OAuth2LoginSuccessHandler;
import org.example.gersangtrade.auth.jwt.JwtTokenizer;
import org.example.gersangtrade.auth.service.AuthService;
import org.example.gersangtrade.auth.service.CustomOAuth2UserService;
import org.example.gersangtrade.auth.service.CustomOidcUserService;
import org.example.gersangtrade.config.OAuth2ClientRegistrationConfig;
import org.example.gersangtrade.config.SecurityConfig;
import org.example.gersangtrade.auth.service.AuthService.RefreshResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 인증·접근제어 통합 테스트 (MockMvc 슬라이스).
 *
 * 검증 대상:
 *   - POST /auth/refresh  : RT 쿠키로 새 AT 반환 및 RT 쿠키 갱신
 *   - POST /auth/logout   : 204 + RT 쿠키 만료(Max-Age=0)
 *   - POST /admin/**      : 비인증 → 401 / USER 권한 → 403 / ADMIN 권한 → 200
 *
 * application.yml의 Google OAuth2 자격증명(client-id/secret)이 빈 문자열 기본값이면
 * OAuth2ClientProperties 검증이 실패하므로 @TestPropertySource로 테스트용 더미 값을 주입한다.
 */
@WebMvcTest(controllers = {AuthController.class, CrawlerAdminController.class})
@Import({SecurityConfig.class, OAuth2ClientRegistrationConfig.class})
@TestPropertySource(properties = {
        // OAuth2ClientRegistrationConfig가 읽는 프로퍼티 경로로 변경
        "oauth2.google.client-id=test-google-client-id",
        "oauth2.google.client-secret=test-google-client-secret"
})
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenizer jwtTokenizer;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private CustomOidcUserService customOidcUserService;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @MockitoBean
    private JobLauncher jobLauncher;

    @MockitoBean(name = "masterDataJob")
    private Job masterDataJob;

    @MockitoBean(name = "itemDataJob")
    private Job itemDataJob;

    @MockitoBean(name = "materialDataJob")
    private Job materialDataJob;

    @MockitoBean(name = "mercenaryDataJob")
    private Job mercenaryDataJob;

    @MockitoBean(name = "setDataJob")
    private Job setDataJob;

    @MockitoBean(name = "ritualDataJob")
    private Job ritualDataJob;

    @MockitoBean(name = "monsterDataJob")
    private Job monsterDataJob;

    @MockitoBean(name = "exclusiveEquipmentDataJob")
    private Job exclusiveEquipmentDataJob;

    @Test
    @DisplayName("refresh_RT쿠키존재_새AT반환및RT쿠키갱신")
    void refresh_RT쿠키존재_새AT반환및RT쿠키갱신() throws Exception {
        when(authService.refresh("old-rt")).thenReturn(new RefreshResult("new-at", "new-rt"));
        when(jwtTokenizer.getRefreshTokenExpiryMs()).thenReturn(604_800_000L);

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "old-rt"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-at"))
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=new-rt")));
    }

    @Test
    @DisplayName("logout_RT쿠키존재_204및RT쿠키만료")
    void logout_RT쿠키존재_204및RT쿠키만료() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refreshToken", "old-rt"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    @DisplayName("admin엔드포인트_비인증요청_401반환")
    void admin엔드포인트_비인증요청_401반환() throws Exception {
        mockMvc.perform(post("/admin/crawler/master"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("admin엔드포인트_USER권한_403반환")
    void admin엔드포인트_USER권한_403반환() throws Exception {
        mockMvc.perform(post("/admin/crawler/master")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin엔드포인트_ADMIN권한_200반환")
    void admin엔드포인트_ADMIN권한_200반환() throws Exception {
        JobExecution execution = mock(JobExecution.class);
        when(execution.getId()).thenReturn(100L);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(execution);

        mockMvc.perform(post("/admin/crawler/master")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
