import { CHAT_UNREAD_VISUAL, getChatRoomStatusVisual } from '@/lib/chatLabels';

interface ChatRoomStatusBadgeProps {
  status: string | undefined;
  hasUnread?: boolean;
  myTradeConfirmed?: boolean;
  partnerTradeConfirmed?: boolean;
}

/** 채팅방 목록·상세에서 상태를 색상 배지로 표시 */
export default function ChatRoomStatusBadge({
  status,
  hasUnread,
  myTradeConfirmed,
  partnerTradeConfirmed,
}: ChatRoomStatusBadgeProps) {
  const visual = hasUnread
    ? CHAT_UNREAD_VISUAL
    : getChatRoomStatusVisual(status, { myTradeConfirmed, partnerTradeConfirmed });

  if (!visual.label) return null;

  return (
    <span
      className="text-[11px] shrink-0 font-semibold px-2 py-0.5 rounded-full leading-tight"
      style={{
        background: visual.background,
        color: visual.color,
        border: `1px solid ${visual.border}`,
      }}
    >
      {visual.label}
    </span>
  );
}
