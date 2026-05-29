package org.example.gersangtrade.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * STOMP CONNECT 프레임에서 핸드쉐이크 때 저장한 userId로 Principal을 설정한다.
 * 이후 convertAndSendToUser(userId, ...) 호출 시 해당 세션으로 라우팅된다.
 */
@Slf4j
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                Long userId = (Long) sessionAttributes.get("userId");
                if (userId != null) {
                    // userId 문자열을 name으로 설정 — convertAndSendToUser(userId.toString(), ...)와 일치
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userId.toString(),
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    accessor.setUser(auth);
                    log.debug("STOMP CONNECT 인증: userId={}", userId);
                }
            }
        }
        return message;
    }
}
