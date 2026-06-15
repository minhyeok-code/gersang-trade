package org.example.gersangtrade.auth.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.auth.jwt.JwtTokenizer;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 핸드쉐이크 시 ?token= 쿼리 파라미터의 JWT를 검증하고
 * userId를 세션 속성으로 저장하는 인터셉터.
 *
 * EventSource와 달리 WebSocket 클라이언트도 커스텀 헤더를 설정할 수 없어
 * 쿼리 파라미터를 통한 토큰 전달이 필요하다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenizer jwtTokenizer;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes
    ) {
        try {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            String token = httpRequest.getParameter("token");
            if (token != null && jwtTokenizer.validate(token) && jwtTokenizer.isAccessToken(token)) {
                Long userId = jwtTokenizer.getUserId(token);
                attributes.put("userId", userId);
                log.debug("WebSocket 핸드쉐이크 인증 성공: userId={}", userId);
            } else {
                log.debug("WebSocket 핸드쉐이크 토큰 없음 — 미인증 연결 허용 (구독 시 필터링)");
            }
        }
        } catch (Exception e) {
            log.warn("WebSocket 핸드쉬이크 처리 중 오류 — 미인증 연결 허용: {}", e.getMessage());
        }
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception
    ) {}
}
