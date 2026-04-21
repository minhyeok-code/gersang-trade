package org.example.gersangtrade.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 채팅 메시지 전송 요청 DTO.
 */
public record ChatMessageSendRequest(

        /** 전송할 메시지 내용 (최대 1,000자) */
        @NotBlank(message = "메시지 내용은 비어있을 수 없습니다.")
        @Size(max = 1000, message = "메시지는 최대 1,000자까지 입력할 수 있습니다.")
        String content
) {}
