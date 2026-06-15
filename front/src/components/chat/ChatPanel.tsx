'use client';



import { useCallback, useEffect, useRef, useState } from 'react';

import { X } from 'lucide-react';

import { api, sortChatMessages, type ChatMessageDto, type ChatRoomDetailDto, type ChatRoomSummaryDto } from '@/lib/api';
import { formatPriceInput, formatPriceInputFromNumber, parsePriceInput } from '@/lib/formatPrice';
import { parseApiError } from '@/lib/parseApiError';

import { isMyMessage, isSystemMessage, resolvePartnerNickname } from '@/lib/chatUtils';

import { useWs } from '@/lib/useWs';
import type { ChatMessageWsEvent, RoomStatusWsEvent } from '@/lib/wsTypes';



interface ChatPanelProps {

  room: ChatRoomSummaryDto;

  onClose: () => void;

}



export default function ChatPanel({ room, onClose }: ChatPanelProps) {

  const [detail, setDetail] = useState<ChatRoomDetailDto | null>(null);

  const [messages, setMessages] = useState<ChatMessageDto[]>([]);

  const [myNickname, setMyNickname] = useState<string | null>(null);

  const [content, setContent] = useState('');

  const [finalPrice, setFinalPrice] = useState('');

  const [error, setError] = useState('');

  const [sending, setSending] = useState(false);
  const [confirming, setConfirming] = useState(false);

  const bottomRef = useRef<HTMLDivElement>(null);

  const prevCountRef = useRef(0);
  const priceInitializedRef = useRef(false);



  useEffect(() => {

    api.getMe().then((u) => setMyNickname(u.nickname)).catch(() => {});

  }, []);



  const load = useCallback(async (silent = false) => {

    try {

      const data = await api.getChatRoom(room.id);

      setDetail(data);

      setMessages(sortChatMessages(data.messages ?? []));

      const defaultPrice = data.finalPrice ?? data.listingPrice ?? room.listingPrice;

      if (!priceInitializedRef.current && defaultPrice != null) {
        setFinalPrice(formatPriceInputFromNumber(defaultPrice));
        priceInitializedRef.current = true;
      }

      if (!silent) setError('');

    } catch (e: unknown) {

      if (!silent) setError(String(e));

    }

  }, [room.id, room.listingPrice]);



  useEffect(() => {
    priceInitializedRef.current = false;
    load();
    api.markChatRoomRead(room.id).catch(() => {});
  }, [load, room.id]);



  useWs({
    chat_message: (data) => {
      const event = data as ChatMessageWsEvent;
      if (event.chatRoomId !== room.id) return;
      setMessages((prev) => {
        if (prev.some((m) => m.id === event.message.id)) return prev;
        return sortChatMessages([...prev, event.message]);
      });
      api.markChatRoomRead(room.id).catch(() => {});
    },

    room_status: (data) => {
      const event = data as RoomStatusWsEvent;

      if (event.chatRoomId !== room.id) return;

      setDetail((prev) =>
        prev
          ? {
              ...prev,
              status: event.status,
              myTradeConfirmed: event.myTradeConfirmed ?? prev.myTradeConfirmed,
              partnerTradeConfirmed: event.partnerTradeConfirmed ?? prev.partnerTradeConfirmed,
            }
          : prev,
      );

    },

  });



  useEffect(() => {

    if (messages.length > prevCountRef.current) {

      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });

    }

    prevCountRef.current = messages.length;

  }, [messages]);

  const isCompleted = detail?.status === 'COMPLETED';
  const isClosedOnly = detail?.status === 'CLOSED';
  const isTerminated = isCompleted || isClosedOnly;
  const canConfirm = detail && !isTerminated && !detail.myTradeConfirmed;
  const waitingPartner = detail?.myTradeConfirmed && !detail?.partnerTradeConfirmed && !isTerminated;
  const needsMyConfirm = detail?.partnerTradeConfirmed && !detail?.myTradeConfirmed && !isTerminated;
  const bothConfirmed = detail?.myTradeConfirmed && detail?.partnerTradeConfirmed && !isTerminated;

  useEffect(() => {
    if (!waitingPartner && !bothConfirmed && !needsMyConfirm) return;
    const timer = setInterval(() => load(true), 3000);
    return () => clearInterval(timer);
  }, [waitingPartner, bothConfirmed, needsMyConfirm, load]);

  async function handleSend() {

    const text = content.trim();

    if (!text) return;

    setSending(true);

    setContent('');

    try {

      const msg = await api.sendMessage(room.id, text);

      setMessages((prev) => {

        if (prev.some((m) => m.id === msg.id)) return prev;

        return sortChatMessages([...prev, msg]);

      });

    } catch (e: unknown) {

      setError(String(e));

      setContent(text);

    } finally {

      setSending(false);

    }

  }

  async function handleTradeConfirm() {
    if (confirming) return;
    setError('');
    setConfirming(true);
    try {
      const parsedPrice = parsePriceInput(finalPrice);
      const summary = await api.confirmTrade(room.id, parsedPrice > 0 ? parsedPrice : undefined);
      try {
        await load(true);
      } catch (loadError: unknown) {
        setError(parseApiError(loadError));
      }
      setDetail((prev) =>
        prev
          ? {
              ...prev,
              status: summary.status ?? prev.status,
              myTradeConfirmed: summary.myTradeConfirmed ?? prev.myTradeConfirmed,
              partnerTradeConfirmed: summary.partnerTradeConfirmed ?? prev.partnerTradeConfirmed,
            }
          : prev,
      );
      // CLOSED는 HTTP 200이지만 거래 확정(COMPLETED)이 아님 — stale 종료 등
      const status = (summary as ChatRoomSummaryDto | undefined)?.status ?? detail?.status;
      if (status === 'CLOSED') {
        setError('이 채팅방에서는 거래가 확정되지 않았습니다. 게시물이 이미 처리되었거나 채팅방이 종료되었습니다.');
      }
    } catch (e: unknown) {
      setError(parseApiError(e));
    } finally {
      setConfirming(false);
    }
  }

  const title = detail?.listingDisplayName ?? room.listingDisplayName ?? `${room.listingType} #${room.listingId}`;

  const partnerNickname = resolvePartnerNickname(detail, room, myNickname);



  return (

    <div

      className="fixed right-6 bottom-6 z-[450] w-[380px] max-w-[calc(100vw-48px)] h-[560px] rounded-xl overflow-hidden shadow-2xl flex flex-col"

      style={{ background: 'var(--card)', border: '1px solid var(--border)' }}

    >

      <div style={{ borderBottom: '1px solid var(--border)' }} className="flex items-center justify-between px-4 py-3 shrink-0">

        <div className="min-w-0">

          <h2 className="text-sm font-semibold truncate" style={{ color: 'var(--text)' }}>

            {partnerNickname}

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

            const mine = isMyMessage(message, myNickname);

            return (

              <div key={message.id} className={system ? 'flex justify-center' : mine ? 'flex justify-end' : 'flex justify-start'}>

                {system ? (

                  <span className="text-xs px-3 py-1 rounded-full" style={{ background: 'var(--card)', color: 'var(--text-muted)' }}>

                    {message.content}

                  </span>

                ) : (

                  <div

                    className="text-sm px-3 py-2 rounded-2xl max-w-[75%]"

                    style={{

                      background: mine ? 'var(--brown)' : 'var(--card)',

                      color: mine ? 'var(--beige)' : 'var(--text)',

                      border: mine ? '1px solid var(--brown)' : '1px solid var(--border)',

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



      {isTerminated ? (

        <div style={{ borderTop: '1px solid var(--border)' }} className="px-4 py-3 shrink-0">

          <p className="text-xs text-center" style={{ color: 'var(--text-muted)' }}>
            {isCompleted
              ? '거래가 확정되었습니다.'
              : '채팅방이 종료되었습니다. 이 방에서는 거래가 확정되지 않았을 수 있습니다.'}
          </p>

        </div>

      ) : (

        <div style={{ borderTop: '1px solid var(--border)' }} className="p-3 space-y-2 shrink-0">

          <div className="flex flex-col gap-1.5">
            {needsMyConfirm && (
              <p className="text-xs px-1" style={{ color: 'var(--brown)' }}>
                상대방이 거래완료를 확인했습니다. 아래 버튼으로 확인해주세요.
              </p>
            )}

          <div className="flex gap-2 items-center">

            <label className="text-xs shrink-0" style={{ color: 'var(--text-muted)' }}>거래가</label>

            <input

              type="text"

              inputMode="numeric"

              value={finalPrice}

              onChange={(e) => setFinalPrice(formatPriceInput(e.target.value))}

              placeholder="가격 입력"

              className="flex-1 min-w-0 rounded px-2 py-1.5 text-xs focus:outline-none focus:border-[var(--brown)]"

              style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}

            />

            {canConfirm && (

              <button

                onClick={handleTradeConfirm}

                disabled={confirming}

                className="flex-1 px-2.5 py-1.5 rounded text-xs font-semibold disabled:opacity-50"

                style={{ background: 'var(--brown)', color: 'var(--beige)' }}

              >

                {confirming ? '처리 중…' : needsMyConfirm ? '거래 완료 확인' : '거래 완료'}

              </button>

            )}

            {waitingPartner && (

              <p className="flex-1 text-xs text-right" style={{ color: 'var(--text-muted)' }}>

                상대방 확인 대기 중…

              </p>

            )}

            {bothConfirmed && (

              <p className="flex-1 text-xs text-right" style={{ color: 'var(--text-muted)' }}>

                거래 확정 처리 중…

              </p>

            )}

          </div>
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

      )}

    </div>

  );

}


