package org.example.gersangtrade.config;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.auth.jwt.JwtTokenizer;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 로컬 개발 전용 테스트 토큰 발급 컨트롤러.
 * local 프로파일에서만 활성화된다. 운영 환경에서는 절대 노출되지 않는다.
 *
 * GET /local/token?email={email} — 해당 이메일 계정의 AT를 발급
 */
@RestController
@RequestMapping("/local")
@RequiredArgsConstructor
@Profile("local")
public class LocalTestAuthController {

    private final UserRepository userRepository;
    private final JwtTokenizer jwtTokenizer;

    /**
     * 테스트용 액세스 토큰 발급.
     * DB에 존재하는 유저의 이메일로 AT를 즉시 발급한다.
     *
     * @param email 발급 대상 유저 이메일
     * @return accessToken
     */
    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> issueTestToken(
            @RequestParam String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 이메일의 유저가 없습니다: " + email));

        String accessToken = jwtTokenizer.createAccessToken(
                user.getId(), user.getRole().name());

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "userId", String.valueOf(user.getId()),
                "role", user.getRole().name()
        ));
    }
}
