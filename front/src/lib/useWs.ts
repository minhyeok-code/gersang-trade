import { useEffect, useRef } from 'react';
import { connectWs, onWsEvent, type WsHandler } from './wsClient';

/**
 * WebSocket(STOMP) 이벤트 구독 훅.
 * 싱글톤 연결(wsClient)을 공유하므로 여러 컴포넌트가 동시에 사용해도 연결이 끊기지 않는다.
 */
export function useWs(handlers: Record<string, WsHandler>) {
  const handlersRef = useRef(handlers);
  handlersRef.current = handlers;

  useEffect(() => {
    connectWs();

    const unsubs = Object.keys(handlersRef.current).map((event) =>
      onWsEvent(event, (data) => handlersRef.current[event]?.(data))
    );

    return () => unsubs.forEach((unsub) => unsub());
  }, []); // eslint-disable-line react-hooks/exhaustive-deps
}
