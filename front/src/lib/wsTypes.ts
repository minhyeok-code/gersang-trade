import type { ChatMessageDto, NotificationDto } from './api';

/** 백엔드 ChatMessageSseEvent — STOMP /user/queue/chat-message 페이로드 */
export interface ChatMessageWsEvent {
  chatRoomId: number;
  message: ChatMessageDto;
}

/** 백엔드 RoomStatusSseEvent — STOMP /user/queue/room-status 페이로드 */
export interface RoomStatusWsEvent {
  chatRoomId: number;
  status: string;
  myTradeConfirmed?: boolean;
  partnerTradeConfirmed?: boolean;
}

export type { NotificationDto as NotificationWsEvent };
