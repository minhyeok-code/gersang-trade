package org.example.gersangtrade.auth.service;

import org.example.gersangtrade.auth.jwt.JwtTokenizer;
import org.example.gersangtrade.domain.user.RefreshToken;
import org.example.gersangtrade.domain.user.RefreshTokenRepository;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.Role;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtTokenizer jwtTokenizer;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("issueRefreshToken_기존토큰존재_토큰회전후반환")
    void issueRefreshToken_기존토큰존재_토큰회전후반환() {
        RefreshToken existing = RefreshToken.builder()
                .userId(1L)
                .token("old-rt")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(jwtTokenizer.createRefreshToken(1L)).thenReturn("new-rt");
        when(jwtTokenizer.getRefreshTokenExpiryMs()).thenReturn(604_800_000L);
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

        String result = authService.issueRefreshToken(1L);

        assertThat(result).isEqualTo("new-rt");
        assertThat(existing.getToken()).isEqualTo("new-rt");
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("issueRefreshToken_기존토큰없음_새토큰저장")
    void issueRefreshToken_기존토큰없음_새토큰저장() {
        when(jwtTokenizer.createRefreshToken(1L)).thenReturn("new-rt");
        when(jwtTokenizer.getRefreshTokenExpiryMs()).thenReturn(604_800_000L);
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.empty());

        String result = authService.issueRefreshToken(1L);

        assertThat(result).isEqualTo("new-rt");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("refresh_유효하지않은토큰_예외발생")
    void refresh_유효하지않은토큰_예외발생() {
        when(jwtTokenizer.validate("bad-rt")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("bad-rt"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("유효하지 않은 리프레시 토큰");
    }

    @Test
    @DisplayName("refresh_DB에토큰없음_탈취의심처리후예외발생")
    void refresh_DB에토큰없음_탈취의심처리후예외발생() {
        when(jwtTokenizer.validate("stolen-rt")).thenReturn(true);
        when(refreshTokenRepository.findByToken("stolen-rt")).thenReturn(Optional.empty());
        when(jwtTokenizer.getUserId("stolen-rt")).thenReturn(1L);

        assertThatThrownBy(() -> authService.refresh("stolen-rt"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("이미 사용되었거나 만료된 리프레시 토큰");

        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    @Test
    @DisplayName("refresh_정상_새ATRT반환및토큰회전")
    void refresh_정상_새ATRT반환및토큰회전() {
        RefreshToken stored = RefreshToken.builder()
                .userId(1L)
                .token("old-rt")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        User user = User.builder()
                .oauthProvider("google")
                .oauthId("google-1")
                .nickname("tester")
                .email("tester@example.com")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(jwtTokenizer.validate("old-rt")).thenReturn(true);
        when(refreshTokenRepository.findByToken("old-rt")).thenReturn(Optional.of(stored));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenizer.createAccessToken(1L, "USER")).thenReturn("new-at");
        when(jwtTokenizer.createRefreshToken(1L)).thenReturn("new-rt");
        when(jwtTokenizer.getRefreshTokenExpiryMs()).thenReturn(604_800_000L);

        AuthService.RefreshResult result = authService.refresh("old-rt");

        assertThat(result.accessToken()).isEqualTo("new-at");
        assertThat(result.refreshToken()).isEqualTo("new-rt");
        assertThat(stored.getToken()).isEqualTo("new-rt");
    }

    @Test
    @DisplayName("logout_토큰존재_삭제호출")
    void logout_토큰존재_삭제호출() {
        RefreshToken stored = RefreshToken.builder()
                .userId(1L)
                .token("rt")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        when(refreshTokenRepository.findByToken("rt")).thenReturn(Optional.of(stored));

        authService.logout("rt");

        verify(refreshTokenRepository).delete(stored);
    }
}
