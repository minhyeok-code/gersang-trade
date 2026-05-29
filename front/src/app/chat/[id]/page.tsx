'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { api, sortChatMessages, type ChatMessageDto, type ChatRoomDetailDto } from '@/lib/api';
import { isMyMessage, isSystemMessage } from '@/lib/chatUtils';
import { useWs } from '@/lib/useWs';
import type { ChatMessageWsEvent, RoomStatusWsEvent } from '@/lib/wsTypes';

export default function ChatRoomPage() {
  const { id } = useParams<{ id: string }>();
  const roomId = Number(id);
  const [room, setRoom] = useState<ChatRoomDetailDto | null>(null);
  const [messages, setMessages] = useState<ChatMessageDto[]>([]);
  const [myNickname, setMyNickname] = useState<string | null>(null);
  const [content, setContent] = useState('');
  const [error, setError] = useState('');
  const [finalPrice, setFinalPrice] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);
  const prevCountRef = useRef(0);

  const load = useCallback(async (silent = false) => {
    try {
      const data = await api.getChatRoom(roomId);
      setRoom(data);
      setMessages(sortChatMessages(data.messages ?? []));
      if (!silent) setError('');
    } catch (e: unknown) {
      if (!silent) setError(String(e));
    }
  }, [roomId]);

  useEffect(() => {
    api.getMe().then((u) => setMyNickname(u.nickname)).catch(() => {});
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  useWs({
    chat_message: (data) => {
      const event = data as ChatMessageWsEvent;
      if (event.chatRoomId !== roomId) return;
      setMessages((prev) => {
        if (prev.some((m) => m.id === event.message.id)) return prev;
        return sortChatMessages([...prev, event.message]);
      });
    },
    room_status: (data) => {
      const event = data as RoomStatusWsEvent;
      if (event.chatRoomId !== roomId) return;
      setRoom((prev) =>
        prev
          ? {
              ...prev,
              status: event.status,
              myTradeConfirmed: event.myTradeConfirmed ?? prev.myTradeConfirmed,
              partnerTradeConfirmed: event.partnerTradeConfirmed ?? prev.partnerTradeConfirmed,
            }
          : prev,
      );
      load(true);
    },
  });

  useEffect(() => {
    if (messages.length > prevCountRef.current) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
    prevCountRef.current = messages.length;
  }, [messages]);

  async function sendMessage(e: React.FormEvent) {
    e.preventDefault();
    if (!content.trim()) return;
    try {
      const msg = await api.sendMessage(roomId, content);
      setContent('');
      setMessages((prev) => {
        if (prev.some((m) => m.id === msg.id)) return prev;
        return sortChatMessages([...prev, msg]);
      });
    } catch (e: unknown) {
      alert(String(e));
    }
  }

  async function tradeConfirm() {
    try {
      const summary = await api.confirmTrade(roomId, finalPrice ? Number(finalPrice) : undefined);
      await load();
      if (summary.status === 'CLOSED') {
        setError('이 채팅방에서는 거래가 확정되지 않았습니다. 게시물이 이미 처리되었거나 채팅방이 종료되었습니다.');
      }
    } catch (e: unknown) {
      alert(String(e));
    }
  }

  const isCompleted = room?.status === 'COMPLETED';
  const isClosedOnly = room?.status === 'CLOSED';
  const isTerminated = isCompleted || isClosedOnly;
  const canConfirm = room && !isTerminated && !room.myTradeConfirmed;
  const waitingPartner = room?.myTradeConfirmed && !room?.partnerTradeConfirmed && !isTerminated;
  const needsMyConfirm = room?.partnerTradeConfirmed && !room?.myTradeConfirmed && !isTerminated;

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-yellow-400">채팅방 #{id}</h1>

      {error && <p className="text-red-400 text-sm">{error}</p>}

      {room && (
        <div className="bg-gray-800 rounded p-3 text-sm text-gray-300 flex gap-6">
          <span>상태: <b className="text-white">{String(room.status ?? '-')}</b></span>
          <span>등록글: {String(room.listingId ?? '-')}</span>
        </div>
      )}

      {/* 메시지 목록 */}
      <div className="bg-gray-900 rounded p-4 h-64 overflow-y-auto space-y-2">
        {messages.length === 0 && <p className="text-gray-500 text-sm">메시지 없음</p>}
        {messages.map((m) => {
          const system = isSystemMessage(m);
          const mine = isMyMessage(m, myNickname);
          return (
            <div key={m.id} className={`text-sm ${system ? '' : mine ? 'text-right' : ''}`}>
              {!system && (
                <span className="text-yellow-400 font-semibold">
                  [{mine ? '나' : m.senderNickname ?? '?'}]
                </span>
              )}
              <span className={`ml-2 ${system ? 'text-gray-500' : 'text-gray-200'}`}>{m.content}</span>
              <span className="ml-2 text-xs text-gray-500">{m.sentAt ?? m.createdAt ?? ''}</span>
            </div>
          );
        })}
        <div ref={bottomRef} />
      </div>

      {/* 메시지 전송 — 거래 완료 시 숨김 */}
      {!isTerminated && (
        <form onSubmit={sendMessage} className="flex gap-2">
          <input value={content} onChange={(e) => setContent(e.target.value)}
            placeholder="메시지 입력"
            className="flex-1 bg-gray-800 border border-gray-600 rounded px-3 py-2 text-sm" />
          <button type="submit" className="bg-blue-600 hover:bg-blue-500 px-4 py-2 rounded text-sm">전송</button>
        </form>
      )}

      {/* 거래 확정 */}
      <div className="bg-gray-800 rounded p-4 space-y-3">
        <h2 className="font-semibold text-gray-300">거래 확정</h2>
        {isTerminated ? (
          <p className="text-sm text-gray-400">
            {isCompleted
              ? '거래가 확정되었습니다.'
              : '채팅방이 종료되었습니다. 이 방에서는 거래가 확정되지 않았을 수 있습니다.'}
          </p>
        ) : (
          <div className="space-y-2">
            {needsMyConfirm && (
              <p className="text-sm text-yellow-300">상대방이 거래완료를 확인했습니다. 확인해주세요.</p>
            )}
          <div className="flex gap-2 items-center flex-wrap">
            <input type="number" value={finalPrice} onChange={(e) => setFinalPrice(e.target.value)}
              placeholder="최종 가격 (선택)"
              className="bg-gray-700 border border-gray-600 rounded px-3 py-2 text-sm w-40" />
            {canConfirm && (
              <button onClick={tradeConfirm} className="bg-green-700 hover:bg-green-600 px-4 py-2 rounded text-sm">
                {needsMyConfirm ? '거래 완료 확인' : '거래 완료'}
              </button>
            )}
            {waitingPartner && (
              <span className="text-sm text-gray-400">상대방 확인 대기 중…</span>
            )}
          </div>
          </div>
        )}
      </div>
    </div>
  );
}
