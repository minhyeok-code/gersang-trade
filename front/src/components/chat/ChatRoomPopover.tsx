'use client';

import { useCallback, useEffect, useState } from 'react';
import { api, type ChatRoomSummaryDto } from '@/lib/api';
import ChatRoomStatusBadge from '@/components/chat/ChatRoomStatusBadge';
import { useWs } from '@/lib/useWs';

interface ChatRoomPopoverProps {
  onOpenRoom: (room: ChatRoomSummaryDto) => void;
  onUnreadChange?: (hasUnread: boolean) => void;
}

export default function ChatRoomPopover({ onOpenRoom, onUnreadChange }: ChatRoomPopoverProps) {
  const [rooms, setRooms] = useState<ChatRoomSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadRooms = useCallback(() => {
    api.getChatRooms()
      .then((list) => {
        setRooms(list);
        onUnreadChange?.(list.some((room) => room.hasUnread));
      })
      .catch((e: unknown) => setError(String(e)))
      .finally(() => setLoading(false));
  }, [onUnreadChange]);

  useEffect(() => {
    loadRooms();
  }, [loadRooms]);

  useWs({
    chat_message: () => loadRooms(),
    room_status: () => loadRooms(),
  });

  async function handleMarkAllRead() {
    try {
      await api.markAllChatRoomsRead();
      loadRooms();
    } catch (e: unknown) {
      setError(String(e));
    }
  }

  return (
    <div
      className="absolute right-0 top-full mt-2 w-80 rounded-xl overflow-hidden shadow-xl"
      style={{ background: 'var(--card)', border: '1px solid var(--border)', zIndex: 350 }}
    >
      <div style={{ borderBottom: '1px solid var(--border)' }} className="flex items-center justify-between px-4 py-3">
        <div>
          <h2 className="text-sm font-semibold" style={{ color: 'var(--text)' }}>채팅</h2>
          <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>거래자 / 아이템</p>
        </div>
        <button
          type="button"
          onClick={handleMarkAllRead}
          className="text-xs shrink-0"
          style={{ color: 'var(--brown)' }}
        >
          전체 읽음
        </button>
      </div>

      <div className="max-h-80 overflow-y-auto p-2">
        {loading ? (
          <p className="text-sm text-center py-8" style={{ color: 'var(--text-muted)' }}>불러오는 중...</p>
        ) : error ? (
          <p className="text-xs p-3" style={{ color: 'var(--danger)' }}>{error}</p>
        ) : rooms.length === 0 ? (
          <p className="text-sm text-center py-8" style={{ color: 'var(--text-disabled)' }}>채팅방이 없습니다</p>
        ) : (
          rooms.map((room) => (
            <button
              key={room.id}
              onClick={() => onOpenRoom(room)}
              className="w-full text-left rounded-lg px-3 py-2.5 transition-colors"
              style={{
                background: room.hasUnread ? '#FFF0F0' : 'transparent',
                border: room.hasUnread ? '1px solid var(--danger)' : '1px solid transparent',
              }}
            >
              <div className="flex items-center justify-between gap-3">
                <div className="min-w-0 flex items-center gap-2">
                  {room.hasUnread && (
                    <span
                      className="w-2 h-2 rounded-full shrink-0"
                      style={{ background: 'var(--danger)' }}
                      aria-label="미읽음"
                    />
                  )}
                  <div className="min-w-0">
                    <p
                      className="text-sm font-medium truncate"
                      style={{ color: room.hasUnread ? 'var(--danger)' : 'var(--text)' }}
                    >
                      {room.partnerNickname}
                    </p>
                    <p
                      className="text-xs truncate mt-0.5"
                      style={{ color: room.hasUnread ? 'var(--danger)' : 'var(--text-muted)', opacity: room.hasUnread ? 0.85 : 1 }}
                    >
                      {room.listingDisplayName ?? `${room.listingType} #${room.listingId}`}
                    </p>
                  </div>
                </div>
                <ChatRoomStatusBadge
                  status={room.status}
                  hasUnread={room.hasUnread}
                  myTradeConfirmed={room.myTradeConfirmed}
                  partnerTradeConfirmed={room.partnerTradeConfirmed}
                />
              </div>
            </button>
          ))
        )}
      </div>
    </div>
  );
}
