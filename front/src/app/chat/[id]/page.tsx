'use client';

import { useState, useEffect, useRef } from 'react';
import { useParams } from 'next/navigation';
import { api, type ChatMessageDto, type ChatRoomDetailDto } from '@/lib/api';

export default function ChatRoomPage() {
  const { id } = useParams<{ id: string }>();
  const roomId = Number(id);
  const [room, setRoom] = useState<ChatRoomDetailDto | null>(null);
  const [messages, setMessages] = useState<ChatMessageDto[]>([]);
  const [content, setContent] = useState('');
  const [error, setError] = useState('');
  const [finalPrice, setFinalPrice] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);

  async function load() {
    try {
      const data = await api.getChatRoom(roomId);
      setRoom(data);
      setMessages(data.messages ?? []);
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    } catch (e: unknown) {
      setError(String(e));
    }
  }

  useEffect(() => { load(); }, [roomId]);

  async function sendMessage(e: React.FormEvent) {
    e.preventDefault();
    if (!content.trim()) return;
    try {
      await api.sendMessage(roomId, content);
      setContent('');
      load();
    } catch (e: unknown) {
      alert(String(e));
    }
  }

  async function posterConfirm() {
    try {
      await api.posterConfirm(roomId, finalPrice ? Number(finalPrice) : undefined);
      load();
    } catch (e: unknown) {
      alert(String(e));
    }
  }

  async function counterpartyConfirm() {
    try {
      await api.counterpartyConfirm(roomId);
      load();
    } catch (e: unknown) {
      alert(String(e));
    }
  }

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
          return (
            <div key={m.id} className="text-sm">
              <span className="text-yellow-400 font-semibold">[{m.senderRole ?? m.senderNickname ?? '?'}]</span>
              <span className="ml-2 text-gray-200">{m.content}</span>
              <span className="ml-2 text-xs text-gray-500">{m.sentAt ?? m.createdAt ?? ''}</span>
            </div>
          );
        })}
        <div ref={bottomRef} />
      </div>

      {/* 메시지 전송 */}
      <form onSubmit={sendMessage} className="flex gap-2">
        <input value={content} onChange={(e) => setContent(e.target.value)}
          placeholder="메시지 입력"
          className="flex-1 bg-gray-800 border border-gray-600 rounded px-3 py-2 text-sm" />
        <button type="submit" className="bg-blue-600 hover:bg-blue-500 px-4 py-2 rounded text-sm">전송</button>
      </form>

      {/* 거래 확정 */}
      <div className="bg-gray-800 rounded p-4 space-y-3">
        <h2 className="font-semibold text-gray-300">거래 확정</h2>
        <div className="flex gap-2 items-center">
          <input type="number" value={finalPrice} onChange={(e) => setFinalPrice(e.target.value)}
            placeholder="최종 가격 (선택)"
            className="bg-gray-700 border border-gray-600 rounded px-3 py-2 text-sm w-40" />
          <button onClick={posterConfirm} className="bg-green-700 hover:bg-green-600 px-4 py-2 rounded text-sm">
            판매자 확정 (POST poster-confirm)
          </button>
        </div>
        <button onClick={counterpartyConfirm} className="bg-purple-700 hover:bg-purple-600 px-4 py-2 rounded text-sm">
          구매자 확정 (POST counterparty-confirm)
        </button>
      </div>
    </div>
  );
}
