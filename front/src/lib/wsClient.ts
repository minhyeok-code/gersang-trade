import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getToken } from './api';

export type WsHandler = (data: unknown) => void;

/** STOMP 구독 이벤트 키 */
export type WsEventType = 'chat_message' | 'room_status' | 'notification';

/** 이벤트별 구독자 목록 */
const handlers = new Map<string, Set<WsHandler>>();

let client: Client | null = null;

function buildHttpUrl(token: string): string {
  // SockJS는 http:// URL을 사용 (내부적으로 WebSocket 업그레이드 처리)
  const base = (process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080').replace(/\/$/, '');
  return `${base}/ws?token=${encodeURIComponent(token)}`;
}

function dispatch(event: string, raw: string) {
  try {
    const data = JSON.parse(raw);
    handlers.get(event)?.forEach((h) => h(data));
  } catch {
    /* JSON 파싱 실패 무시 */
  }
}

function subscribeUserQueues(stomp: Client) {
  stomp.subscribe('/user/queue/chat-message', (frame) => {
    dispatch('chat_message', frame.body);
  });
  stomp.subscribe('/user/queue/room-status', (frame) => {
    dispatch('room_status', frame.body);
  });
  stomp.subscribe('/user/queue/notification', (frame) => {
    dispatch('notification', frame.body);
  });
}

/** WebSocket(STOMP) 연결 — 로그인 상태에서만 호출 */
export function connectWs() {
  const token = getToken();
  if (!token) return;

  if (client?.active) return;

  if (client) {
    client.deactivate();
    client = null;
  }

  // SockJS: native WebSocket 실패 시 HTTP 롱폴링으로 자동 폴백
  client = new Client({
    webSocketFactory: () => new SockJS(buildHttpUrl(token)),
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      subscribeUserQueues(client!);
    },
    onStompError: (frame) => {
      console.warn('[WS] STOMP error:', frame.headers.message ?? frame.body);
    },
    onWebSocketClose: () => {
      /* @stomp/stompjs reconnectDelay로 자동 재연결 */
    },
  });

  client.activate();
}

/** 로그아웃·토큰 삭제 시 WebSocket 연결 종료 */
export function disconnectWs() {
  if (client) {
    client.deactivate();
    client = null;
  }
}

/** 로그인·토큰 갱신 후 재연결 */
export function reconnectWs() {
  disconnectWs();
  connectWs();
}

/** WebSocket 이벤트 구독 — 반환값으로 구독 해제 */
export function onWsEvent(event: WsEventType | string, handler: WsHandler): () => void {
  if (!handlers.has(event)) handlers.set(event, new Set());
  handlers.get(event)!.add(handler);

  if (getToken()) connectWs();

  return () => {
    handlers.get(event)?.delete(handler);
  };
}
