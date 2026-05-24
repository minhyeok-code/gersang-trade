'use client';

import { useEffect, useRef, useState } from 'react';
import { X } from 'lucide-react';
import { api, type ChatMessageDto, type ChatRoomDetailDto, type ChatRoomSummaryDto } from '@/lib/api';

interface ChatPanelProps {
  room: ChatRoomSummaryDto;
  onClose: () => void;
}

function isSystemMessage(message: ChatMessageDto) {
  return message.messageType === 'SYSTEM' || message.type === 'SYSTEM' || message.senderNickname === '시스템';
}

export default function ChatPanel({ room, onClose }: ChatPanelProps) {
  const [detail, setDetail] = useState<ChatRoomDetailDto | null>(null);
  const [messages, setMessages] = useState<ChatMessageDto[]>([]);
  const [content, setContent] = useState('');
  const [finalPrice, setFinalPrice] = useState('');
  const [error, setError] = useState('');
  const [sending, setSending] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  async function load() {
    try {
      const data = await api.getChatRoom(room.id);
      setDetail(data);
      setMessages(data.messages ?? []);
    } catch (e: unknown) {
      setError(String(e));
    }
  }

  useEffect(() => { load(); }, [room.id]);
  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages]);

  async function handleSend() {
    const text = content.trim();
    if (!text) return;
    setSending(true);
    setContent('');
    try {
      await api.sendMessage(room.id, text);
      await load();
    } catch (e: unknown) {
      setError(String(e));
      setContent(text);
    } finally {
      setSending(false);
    }
  }

  async function handlePosterConfirm() {
    try {
      await api.posterConfirm(room.id, finalPrice ? Number(finalPrice) : undefined);
      await load();
    } catch (e: unknown) {
      setError(String(e));
    }
  }

  async function handleCounterpartyConfirm() {
    try {
      await api.counterpartyConfirm(room.id);
      await load();
    } catch (e: unknown) {
      setError(String(e));
    }
  }

  const title = detail?.listingDisplayName ?? room.listingDisplayName ?? `${room.listingType} #${room.listingId}`;

  return (
    <div
      className="fixed right-6 bottom-6 z-[450] w-[380px] max-w-[calc(100vw-48px)] h-[560px] rounded-xl overflow-hidden shadow-2xl flex flex-col"
      style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
    >
      <div style={{ borderBottom: '1px solid var(--border)' }} className="flex items-center justify-between px-4 py-3 shrink-0">
        <div className="min-w-0">
          <h2 className="text-sm font-semibold truncate" style={{ color: 'var(--text)' }}>
            {detail?.partnerNickname ?? room.partnerNickname}
          </h2>
          <p className="text-xs truncate" style={{ color: 'var(--text-muted)' }}>{title}</p>
        </div>
        <button onClick={onClose} style={{ color: 'var(--text-muted)' }} className="hover:text-[var(--text)]">
          <X style={{ width: 18, height: 18 }} />
        </button>
      </div>

      {error && (
        <div className="px-4 py-2 text-xs shrink-0" style={{ color: 'var(--danger)', borderBottom: '1px solid var(--border)' }}>
          {error}
        </div>
      )}

      <div className="flex-1 overflow-y-auto p-4 space-y-2" style={{ background: 'var(--bg)' }}>
        {messages.length === 0 ? (
          <p className="text-sm text-center py-8" style={{ color: 'var(--text-disabled)' }}>메시지가 없습니다</p>
        ) : (
          messages.map((message) => {
            const system = isSystemMessage(message);
            const isPartner = message.senderNickname === room.partnerNickname;
            return (
              <div key={message.id} className={system ? 'flex justify-center' : isPartner ? 'flex justify-start' : 'flex justify-end'}>
                {system ? (
                  <span className="text-xs px-3 py-1 rounded-full" style={{ background: 'var(--card)', color: 'var(--text-muted)' }}>
                    {message.content}
                  </span>
                ) : (
                  <div
                    className="text-sm px-3 py-2 rounded-2xl max-w-[75%]"
                    style={{
                      background: isPartner ? 'var(--card)' : 'var(--brown)',
                      color: isPartner ? 'var(--text)' : 'var(--beige)',
                      border: isPartner ? '1px solid var(--border)' : '1px solid var(--brown)',
                    }}
                  >
                    {message.content}
                  </div>
                )}
              </div>
            );
          })
        )}
        <div ref={bottomRef} />
      </div>

      <div style={{ borderTop: '1px solid var(--border)' }} className="p-3 space-y-2 shrink-0">
        <div className="flex gap-2">
          <input
            type="number"
            value={finalPrice}
            onChange={(e) => setFinalPrice(e.target.value)}
            placeholder="최종가"
            className="w-24 rounded px-2 py-1.5 text-xs focus:outline-none focus:border-[var(--brown)]"
            style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
          />
          <button onClick={handlePosterConfirm} className="px-2.5 py-1.5 rounded text-xs" style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}>
            거래완료 요청
          </button>
          <button onClick={handleCounterpartyConfirm} className="px-2.5 py-1.5 rounded text-xs" style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}>
            완료 확인
          </button>
        </div>
        <div className="flex gap-2">
          <input
            value={content}
            onChange={(e) => setContent(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSend();
              }
            }}
            placeholder="메시지 입력..."
            className="flex-1 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]"
            style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
          />
          <button
            onClick={handleSend}
            disabled={!content.trim() || sending}
            className="px-4 py-2 rounded-lg text-sm font-semibold disabled:opacity-50"
            style={{ background: 'var(--brown)', color: 'var(--beige)' }}
          >
            전송
          </button>
        </div>
      </div>
    </div>
  );
}
