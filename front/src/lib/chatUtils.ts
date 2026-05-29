import type { ChatMessageDto, ChatRoomDetailDto, ChatRoomSummaryDto } from './api';

/** 채팅방 상세·요약에서 상대방 닉네임을 구한다 */
export function resolvePartnerNickname(
  detail: ChatRoomDetailDto | null | undefined,
  room: ChatRoomSummaryDto | null | undefined,
  myNickname: string | null | undefined,
): string {
  if (detail && myNickname) {
    if (detail.posterNickname === myNickname) return detail.counterpartyNickname;
    if (detail.counterpartyNickname === myNickname) return detail.posterNickname;
  }
  return detail?.partnerNickname ?? room?.partnerNickname ?? '';
}

/** 내가 보낸 메시지인지 판별 (닉네임 기준) */
export function isMyMessage(message: ChatMessageDto, myNickname: string | null | undefined): boolean {
  if (!myNickname || !message.senderNickname) return false;
  return message.senderNickname === myNickname;
}

export function isSystemMessage(message: ChatMessageDto): boolean {
  return message.messageType === 'SYSTEM' || message.type === 'SYSTEM' || message.senderNickname === '시스템';
}
