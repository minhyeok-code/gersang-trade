export interface ChatRoomStatusVisual {
  label: string;
  background: string;
  color: string;
  border: string;
}

const CHAT_STATUS_VISUAL: Record<string, ChatRoomStatusVisual> = {
  OPEN: {
    label: '진행 중',
    background: 'var(--beige)',
    color: 'var(--brown)',
    border: 'var(--border)',
  },
  AWAITING_PARTNER: {
    label: '확인 대기',
    background: '#F5EDD8',
    color: '#8B6914',
    border: '#D4A84B',
  },
  COMPLETED: {
    label: '거래 완료',
    background: 'var(--sell-bg)',
    color: 'var(--sell-text)',
    border: 'var(--sell-border)',
  },
  CLOSED: {
    label: '종료',
    background: 'var(--bg)',
    color: 'var(--text-muted)',
    border: 'var(--border)',
  },
};

/** 채팅방 상태 — 사용자-facing 한글 라벨 */
export function formatChatRoomStatus(status: string | undefined): string {
  return getChatRoomStatusVisual(status).label;
}

/**
 * 채팅방 상태 배지용 라벨·색상.
 * AWAITING_PARTNER는 내/상대 확인 여부로 라벨을 나눈다.
 */
export function getChatRoomStatusVisual(
  status: string | undefined,
  options?: { myTradeConfirmed?: boolean; partnerTradeConfirmed?: boolean },
): ChatRoomStatusVisual {
  if (status === 'AWAITING_PARTNER') {
    if (options?.myTradeConfirmed && !options?.partnerTradeConfirmed) {
      return {
        label: '상대 확인 중',
        background: 'var(--buy-bg)',
        color: 'var(--buy-text)',
        border: 'var(--buy-border)',
      };
    }
    if (!options?.myTradeConfirmed && options?.partnerTradeConfirmed) {
      return {
        label: '완료 대기',
        background: '#FFF0E8',
        color: '#9A4A2A',
        border: '#E0A080',
      };
    }
  }
  return CHAT_STATUS_VISUAL[status ?? ''] ?? {
    label: status ?? '',
    background: 'var(--bg)',
    color: 'var(--text-muted)',
    border: 'var(--border)',
  };
}

export const CHAT_UNREAD_VISUAL: ChatRoomStatusVisual = {
  label: '미읽음',
  background: '#FFF0F0',
  color: 'var(--danger)',
  border: 'var(--danger)',
};

/** 알림 유형 — 사용자-facing 한글 라벨 (기술 enum 숨김) */
export function formatNotificationType(type: string | undefined): string {
  switch (type) {
    case 'TRADE_COMPLETED':
      return '거래 완료';
    case 'REVIEW_REQUESTED':
      return '거래 평가';
    case 'LISTING_SOLD':
      return '판매 완료';
    case 'REPORT_RESOLVED':
      return '신고 처리';
    default:
      return '';
  }
}
