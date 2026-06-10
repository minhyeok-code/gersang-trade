/** 채팅방 상태 — 사용자-facing 한글 라벨 */
export function formatChatRoomStatus(status: string | undefined): string {
  switch (status) {
    case 'OPEN':
      return '진행 중';
    case 'AWAITING_PARTNER':
      return '확인 대기';
    case 'COMPLETED':
      return '거래 완료';
    case 'CLOSED':
      return '종료';
    default:
      return status ?? '';
  }
}

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
