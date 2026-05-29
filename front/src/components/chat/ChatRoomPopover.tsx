'use client';

import { useCallback, useEffect, useState } from 'react';
import { api, type ChatRoomSummaryDto } from '@/lib/api';
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
    notification: (data) => {
      const n = data as { type?: string };
      if (n.type === 'CHAT_OPENED') loadRooms();
    },
  });

  return (
    <div
      className="absolute right-0 top-full mt-2 w-80 rounded-xl overflow-hidden shadow-xl"
      style={{ background: 'var(--card)', border: '1px solid var(--border)', zIndex: 350 }}
    >
      <div style={{ borderBottom: '1px solid var(--border)' }} className="px-4 py-3">
        <h2 className="text-sm font-semibold" style={{ color: 'var(--text)' }}>채팅</h2>
        <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>거래자 / 아이템</p>
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
              className="w-full text-left rounded-lg px-3 py-2.5 hover:bg-[var(--bg)] transition-colors"
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
                    <p className="text-sm font-medium truncate" style={{ color: 'var(--text)' }}>
                      {room.partnerNickname}
                    </p>
                    <p className="text-xs truncate mt-0.5" style={{ color: 'var(--text-muted)' }}>
                      {room.listingDisplayName ?? `${room.listingType} #${room.listingId}`}
                    </p>
                  </div>
                </div>
                <span className="text-[11px] shrink-0" style={{ color: 'var(--text-disabled)' }}>
                  {room.status}
                </span>
              </div>
            </button>
          ))
        )}
      </div>
    </div>
  );
}
