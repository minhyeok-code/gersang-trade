'use client';

import { useEffect, useState } from 'react';
import { api, type NotificationDto, type PendingReviewDto } from '@/lib/api';
import { useWs } from '@/lib/useWs';

type RatingValue = 'GOOD' | 'NEUTRAL' | 'BAD';

const RATING_OPTIONS: { value: RatingValue; emoji: string; label: string; sub: string }[] = [
  { value: 'GOOD',    emoji: '👍', label: '좋아요',    sub: 'EXP +15, 매너점수 +2' },
  { value: 'NEUTRAL', emoji: '😐', label: '보통이에요', sub: '변화 없음' },
  { value: 'BAD',     emoji: '👎', label: '나빠요',    sub: 'EXP -20, 매너점수 -3' },
];

interface NotificationPopoverProps {
  onUnreadChange?: (count: number) => void;
}

function isUnread(notification: NotificationDto) {
  return (notification.isRead === false || notification.read === false)
    && notification.type !== 'CHAT_MESSAGE';
}

function getTitle(notification: NotificationDto) {
  return notification.title ?? notification.message ?? notification.content ?? '알림';
}

function getContent(notification: NotificationDto) {
  return notification.content ?? notification.message ?? notification.title ?? '';
}

export default function NotificationPopover({ onUnreadChange }: NotificationPopoverProps) {
  const [notifications, setNotifications] = useState<NotificationDto[]>([]);
  const [selected, setSelected] = useState<NotificationDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // 평가 폼 상태
  const [pendingReview, setPendingReview] = useState<PendingReviewDto | null>(null);
  const [selectedRating, setSelectedRating] = useState<RatingValue>('GOOD');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    onUnreadChange?.(notifications.filter(isUnread).length);
  }, [notifications, onUnreadChange]);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const list = await api.getNotifications();
      const visible = list.filter((n) => n.type !== 'CHAT_MESSAGE');
      setNotifications(visible);
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  useWs({
    notification: (data) => {
      const incoming = data as NotificationDto;
      if (incoming.type === 'CHAT_MESSAGE') return;
      setNotifications((prev) => {
        if (prev.some((n) => n.id === incoming.id)) return prev;
        return [incoming, ...prev];
      });
    },
  });

  async function handleSelect(notification: NotificationDto) {
    if (isUnread(notification)) {
      api.markRead(notification.id)
        .then(() => {
          setNotifications((prev) =>
            prev.map((n) => (n.id === notification.id ? { ...n, isRead: true, read: true } : n))
          );
        })
        .catch(() => {});
    }

    if (notification.type === 'REVIEW_REQUESTED') {
      // pending reviews 조회 후 이 알림의 chatRoomId와 매핑
      try {
        const pending = await api.getPendingReviews();
        const matched = notification.chatRoomId != null
          ? pending.find((r) => r.chatRoomId === notification.chatRoomId)
          : pending[0];
        if (matched) {
          setPendingReview(matched);
          setSelectedRating('GOOD');
          return;
        }
      } catch {
        // 조회 실패 시 일반 텍스트 모달로 폴백
      }
    }

    setSelected(notification);
  }

  async function handleSubmitReview() {
    if (!pendingReview) return;
    setSubmitting(true);
    try {
      await api.submitReview(pendingReview.reviewId, { rating: selectedRating });
      setPendingReview(null);
    } catch {
      alert('평가 제출 중 오류가 발생했습니다.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleMarkAllRead() {
    try {
      await api.markAllRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true, read: true })));
    } catch (e: unknown) {
      setError(String(e));
    }
  }

  return (
    <>
      <div
        className="absolute right-0 top-full mt-2 w-80 rounded-xl overflow-hidden shadow-xl"
        style={{ background: 'var(--card)', border: '1px solid var(--border)', zIndex: 350 }}
      >
        <div style={{ borderBottom: '1px solid var(--border)' }} className="flex items-center justify-between px-4 py-3">
          <h2 className="text-sm font-semibold" style={{ color: 'var(--text)' }}>알림</h2>
          <button onClick={handleMarkAllRead} className="text-xs" style={{ color: 'var(--brown)' }}>
            전체 읽음
          </button>
        </div>

        <div className="max-h-80 overflow-y-auto p-2">
          {loading ? (
            <p className="text-sm text-center py-8" style={{ color: 'var(--text-muted)' }}>불러오는 중...</p>
          ) : error ? (
            <p className="text-xs p-3" style={{ color: 'var(--danger)' }}>{error}</p>
          ) : notifications.length === 0 ? (
            <p className="text-sm text-center py-8" style={{ color: 'var(--text-disabled)' }}>알림이 없습니다</p>
          ) : (
            notifications.map((notification) => (
              <button
                key={notification.id}
                onClick={() => handleSelect(notification)}
                className="w-full text-left rounded-lg px-3 py-2.5 hover:bg-[var(--bg)] transition-colors"
              >
                <div className="flex items-start gap-2">
                  {isUnread(notification) && (
                    <span className="mt-1.5 w-1.5 h-1.5 rounded-full shrink-0" style={{ background: 'var(--danger)' }} />
                  )}
                  <div className="min-w-0">
                    <p className="text-sm truncate" style={{ color: 'var(--text)' }}>{getTitle(notification)}</p>
                    <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>
                      {notification.type}
                    </p>
                  </div>
                </div>
              </button>
            ))
          )}
        </div>
      </div>

      {/* 평가 폼 모달 */}
      {pendingReview && (
        <div
          className="fixed inset-0 z-[520] flex items-center justify-center p-4"
          style={{ background: 'rgba(0,0,0,0.65)' }}
          onClick={() => setPendingReview(null)}
        >
          <div
            className="w-full max-w-sm rounded-xl overflow-hidden"
            style={{ background: 'var(--card)', border: '1px solid var(--border)', boxShadow: '0 24px 60px rgba(0,0,0,0.3)' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ borderBottom: '1px solid var(--border)' }} className="flex items-center justify-between px-5 py-3.5">
              <h2 className="font-semibold text-sm" style={{ color: 'var(--text)' }}>
                {pendingReview.counterpartyNickname}님 거래 평가
              </h2>
              <button onClick={() => setPendingReview(null)} className="text-xl" style={{ color: 'var(--text-muted)' }}>×</button>
            </div>
            <div className="p-5 space-y-4">
              <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
                마감: {new Date(pendingReview.revealAt).toLocaleDateString()}
              </p>
              <div className="flex flex-col gap-2">
                {RATING_OPTIONS.map((opt) => (
                  <button
                    key={opt.value}
                    onClick={() => setSelectedRating(opt.value)}
                    className="flex items-center gap-3 px-4 py-3 rounded-lg text-left transition-colors"
                    style={{
                      background: selectedRating === opt.value ? 'var(--brown)' : 'var(--bg)',
                      border: `1px solid ${selectedRating === opt.value ? 'var(--brown)' : 'var(--border)'}`,
                      color: selectedRating === opt.value ? 'var(--beige)' : 'var(--text)',
                    }}
                  >
                    <span className="text-xl">{opt.emoji}</span>
                    <div>
                      <p className="text-sm font-semibold">{opt.label}</p>
                      <p className="text-xs mt-0.5" style={{ color: selectedRating === opt.value ? 'var(--beige)' : 'var(--text-muted)' }}>
                        {opt.sub}
                      </p>
                    </div>
                  </button>
                ))}
              </div>
              <button
                onClick={handleSubmitReview}
                disabled={submitting}
                className="w-full py-2.5 rounded-lg text-sm font-semibold disabled:opacity-50"
                style={{ background: 'var(--brown)', color: 'var(--beige)' }}
              >
                {submitting ? '제출 중...' : '평가 등록'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 일반 알림 상세 모달 */}
      {selected && (
        <div
          className="fixed inset-0 z-[520] flex items-center justify-center p-4"
          style={{ background: 'rgba(0,0,0,0.65)' }}
          onClick={() => setSelected(null)}
        >
          <div
            className="w-full max-w-md rounded-xl overflow-hidden"
            style={{ background: 'var(--card)', border: '1px solid var(--border)', boxShadow: '0 24px 60px rgba(0,0,0,0.3)' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ borderBottom: '1px solid var(--border)' }} className="flex items-center justify-between px-5 py-3.5">
              <h2 className="font-semibold" style={{ color: 'var(--text)' }}>{getTitle(selected)}</h2>
              <button onClick={() => setSelected(null)} className="text-xl" style={{ color: 'var(--text-muted)' }}>×</button>
            </div>
            <div className="p-5 space-y-3">
              <p className="text-sm whitespace-pre-wrap leading-6" style={{ color: 'var(--text)' }}>{getContent(selected)}</p>
              <p className="text-xs" style={{ color: 'var(--text-disabled)' }}>{new Date(selected.createdAt).toLocaleString()}</p>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
