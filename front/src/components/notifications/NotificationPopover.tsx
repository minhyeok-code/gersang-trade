'use client';

import { useEffect, useState } from 'react';
import { api, type NotificationDto } from '@/lib/api';

interface NotificationPopoverProps {
  onUnreadChange?: (count: number) => void;
}

function isUnread(notification: NotificationDto) {
  return notification.isRead === false || notification.read === false;
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

  async function load() {
    setLoading(true);
    setError('');
    try {
      const list = await api.getNotifications();
      setNotifications(list);
      onUnreadChange?.(list.filter(isUnread).length);
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function handleSelect(notification: NotificationDto) {
    setSelected(notification);
    if (isUnread(notification)) {
      api.markRead(notification.id)
        .then(() => {
          setNotifications((prev) => {
            const next = prev.map((n) => n.id === notification.id ? { ...n, isRead: true, read: true } : n);
            onUnreadChange?.(next.filter(isUnread).length);
            return next;
          });
        })
        .catch(() => {});
    }
  }

  async function handleMarkAllRead() {
    try {
      await api.markAllRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true, read: true })));
      onUnreadChange?.(0);
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
