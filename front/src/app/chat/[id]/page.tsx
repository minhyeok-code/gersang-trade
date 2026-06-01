'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { api, sortChatMessages, type ChatMessageDto, type ChatRoomDetailDto, type PublicUserDto } from '@/lib/api';
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
  const [partnerProfile, setPartnerProfile] = useState<PublicUserDto | null>(null);
  const [showProfile, setShowProfile] = useState(false);
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
        <div className="bg-gray-800 rounded p-3 text-sm text-gray-300 flex gap-6 items-center">
          <span>상태: <b className="text-white">{String(room.status ?? '-')}</b></span>
          <span>등록글: {String(room.listingId ?? '-')}</span>
          {room.partnerNickname && (
            <button
              className="ml-auto text-yellow-400 font-semibold hover:underline text-sm"
              onClick={async () => {
                if (room.partnerId && !partnerProfile) {
                  const profile = await api.getUser(room.partnerId).catch(() => null);
                  setPartnerProfile(profile);
                }
                setShowProfile(true);
              }}
            >
              {room.partnerNickname} 프로필 보기
            </button>
          )}
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
      {/* 상대방 프로필 모달 */}
      {showProfile && (
        <div
          className="fixed inset-0 z-[500] flex items-center justify-center p-4"
          style={{ background: 'rgba(0,0,0,0.65)' }}
          onClick={() => setShowProfile(false)}
        >
          <div
            className="w-full max-w-sm rounded-xl overflow-hidden"
            style={{ background: 'var(--card)', border: '1px solid var(--border)', boxShadow: '0 24px 60px rgba(0,0,0,0.3)' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ borderBottom: '1px solid var(--border)' }} className="flex items-center justify-between px-5 py-3.5">
              <h2 className="font-semibold text-sm" style={{ color: 'var(--text)' }}>
                {room?.partnerNickname} 프로필
              </h2>
              <button onClick={() => setShowProfile(false)} className="text-xl" style={{ color: 'var(--text-muted)' }}>×</button>
            </div>

            {partnerProfile ? (
              <div className="p-5 space-y-4">
                {/* 아바타 + 닉네임 */}
                <div className="flex items-center gap-3">
                  <div
                    className="w-12 h-12 rounded-full flex items-center justify-center text-lg font-bold shrink-0"
                    style={{ background: 'var(--beige)', color: 'var(--brown)' }}
                  >
                    {partnerProfile.nickname.slice(0, 2).toUpperCase()}
                  </div>
                  <div>
                    <p className="font-semibold" style={{ color: 'var(--text)' }}>{partnerProfile.nickname}</p>
                    {partnerProfile.gameNickname && (
                      <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>게임: {partnerProfile.gameNickname}</p>
                    )}
                  </div>
                </div>

                {/* 거래량 / 매너점수 / 등급 */}
                <div
                  className="grid grid-cols-3 divide-x text-center"
                  style={{ background: 'var(--bg)', border: '1px solid var(--border)', borderRadius: 8, overflow: 'hidden' }}
                >
                  <div className="flex flex-col items-center py-3 gap-0.5">
                    <span className="text-xs" style={{ color: 'var(--text-muted)' }}>거래량</span>
                    <span className="font-bold font-serif text-base" style={{ color: 'var(--text)' }}>
                      {partnerProfile.tradeCount ?? 0}건
                    </span>
                  </div>
                  <div className="flex flex-col items-center py-3 gap-0.5">
                    <span className="text-xs" style={{ color: 'var(--text-muted)' }}>매너점수</span>
                    {(() => {
                      const score = partnerProfile.mannerScore ?? 60;
                      const color = score >= 70 ? 'var(--brown)' : score >= 40 ? '#b08030' : 'var(--danger)';
                      return (
                        <span className="font-bold font-serif text-base" style={{ color }}>{score}점</span>
                      );
                    })()}
                  </div>
                  <div className="flex flex-col items-center py-3 gap-0.5">
                    <span className="text-xs" style={{ color: 'var(--text-muted)' }}>등급</span>
                    <span className="font-bold font-serif text-base" style={{ color: 'var(--brown)' }}>
                      {partnerProfile.grade ?? '-'}
                    </span>
                  </div>
                </div>

                {partnerProfile.gameAccessTime && (
                  <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
                    접속 가능 시간: {partnerProfile.gameAccessTime}
                  </p>
                )}
              </div>
            ) : (
              <p className="text-sm text-center py-8" style={{ color: 'var(--text-muted)' }}>불러오는 중...</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
