/** @deprecated wsClient 직접 import 권장 — WebSocket(STOMP) 호환 re-export */
export {
  connectWs as connectSse,
  disconnectWs as disconnectSse,
  reconnectWs as reconnectSse,
  onWsEvent as onSseEvent,
} from './wsClient';
