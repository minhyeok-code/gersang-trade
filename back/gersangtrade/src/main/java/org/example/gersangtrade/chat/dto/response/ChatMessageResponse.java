package org.example.gersangtrade.chat.dto.response;

import org.example.gersangtrade.domain.chat.ChatMessage;
import org.example.gersangtrade.domain.chat.enums.ChatMessageType;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 응답 DTO.
 * hidden=true인 메시지는 content를 "[삭제된 메시지입니다]"로 대체한다.
 */
public record ChatMessageResponse(

        Long id,
        String senderNickname,
        String content,
        ChatMessageType messageType,
        boolean flagged,
        LocalDateTime sentAt
) {
    /** 사용자용 — hidden 메시지는 내용 마스킹 처리 */
    public static ChatMessageResponse of(ChatMessage msg) {
        String displayContent = msg.isHidden() ? "[삭제된 메시지입니다]" : msg.getContent();
        String senderNickname = (msg.getSender() != null) ? msg.getSender().getNickname() : "시스템";

        return new ChatMessageResponse(
                msg.getId(),
                senderNickname,
                displayContent,
                msg.getMessageType(),
                false, // 사용자에게 flagged 여부 노출 안 함
                msg.getSentAt()
        );
    }

    /** 관리자용 — hidden 마스킹 없이 flagged 여부 포함 */
    public static ChatMessageResponse ofForAdmin(ChatMessage msg) {
        String senderNickname = (msg.getSender() != null) ? msg.getSender().getNickname() : "시스템";

        return new ChatMessageResponse(
                msg.getId(),
                senderNickname,
                msg.getContent(),
                msg.getMessageType(),
                msg.isFlagged(),
                msg.getSentAt()
        );
    }
}
