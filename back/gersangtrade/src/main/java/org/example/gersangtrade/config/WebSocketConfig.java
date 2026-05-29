package org.example.gersangtrade.config;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.auth.filter.JwtHandshakeInterceptor;
import org.example.gersangtrade.auth.jwt.JwtTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenizer jwtTokenizer;
    private final WebSocketAuthChannelInterceptor authChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 인메모리 브로커 — /topic(브로드캐스트), /queue(개인)
        config.enableSimpleBroker("/topic", "/queue");
        // 클라이언트 → 서버 메시지 prefix (현재 미사용, 구독 방향만 사용)
        config.setApplicationDestinationPrefixes("/app");
        // 사용자 전용 메시지 prefix (/user/{userId}/queue/...)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 엔드포인트 — native WebSocket + SockJS 폴백 모두 등록
        // SockJS 폴백: 브라우저 환경에 따라 HTTP 롱폴링으로 자동 전환
        registry.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor())
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // CONNECT 프레임에서 세션 속성의 userId를 STOMP principal로 설정
        registration.interceptors(authChannelInterceptor);
    }

    @Bean
    public JwtHandshakeInterceptor jwtHandshakeInterceptor() {
        return new JwtHandshakeInterceptor(jwtTokenizer);
    }
}
