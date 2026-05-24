'use client';

import { useState, useEffect } from 'react';
import { api } from '@/lib/api';

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<unknown[]>([]);
  const [error, setError] = useState('');
  const [sseLog, setSseLog] = useState<string[]>([]);

  useEffect(() => {
    api.getNotifications()
      .then(setNotifications)
      .catch((e: unknown) => setError(String(e)));
  }, []);

  async function markAllRead() {
    try {
      await api.markAllRead();
      api.getNotifications().then(setNotifications);
    } catch (e: unknown) {
      alert(String(e));
    }
  }

  function connectSSE() {
    const token = localStorage.getItem('accessToken');
    if (!token) { alert('로그인 필요'); return; }

    const url = `http://localhost:8080/api/notifications/subscribe`;
    setSseLog((prev) => [...prev, `[${new Date().toLocaleTimeString()}] SSE 연결 중...`]);

    const eventSource = new EventSource(url + `?token=${encodeURIComponent(token)}`);
    eventSource.onopen = () => setSseLog((prev) => [...prev, `[${new Date().toLocaleTimeString()}] 연결됨`]);
    eventSource.onmessage = (e) => setSseLog((prev) => [...prev, `[${new Date().toLocaleTimeString()}] ${e.data}`]);
    eventSource.onerror = () => {
      setSseLog((prev) => [...prev, `[${new Date().toLocaleTimeString()}] 연결 오류/종료`]);
      eventSource.close();
    };

    setTimeout(() => { eventSource.close(); setSseLog((prev) => [...prev, '연결 닫힘 (30초)']); }, 30000);
  }

  return (
    <div className="max-w-5xl mx-auto p-4 space-y-6">
      <h1 className="text-2xl font-bold text-yellow-400">알림</h1>

      {error && <p className="text-red-400 text-sm">{error}</p>}

      <div className="flex gap-2">
        <button onClick={markAllRead} className="bg-blue-700 hover:bg-blue-600 px-4 py-2 rounded text-sm">
          전체 읽음 (PATCH /api/notifications/read-all)
        </button>
        <button onClick={connectSSE} className="bg-purple-700 hover:bg-purple-600 px-4 py-2 rounded text-sm">
          SSE 구독 테스트 (GET /api/notifications/subscribe)
        </button>
      </div>

      {sseLog.length > 0 && (
        <div className="bg-gray-900 rounded p-4">
          <h2 className="font-semibold mb-2 text-gray-300">SSE 로그</h2>
          <div className="space-y-1 text-xs text-gray-300 font-mono max-h-40 overflow-y-auto">
            {sseLog.map((log, i) => <div key={i}>{log}</div>)}
          </div>
        </div>
      )}

      <div className="bg-gray-800 rounded p-4">
        <h2 className="font-semibold mb-2 text-gray-300">알림 목록 — GET /api/notifications ({notifications.length}개)</h2>
        {notifications.length === 0 ? (
          <p className="text-gray-500 text-sm">알림 없음</p>
        ) : (
          <div className="space-y-2">
            {notifications.map((n: unknown) => {
              const r = n as Record<string, unknown>;
              return (
                <div key={String(r.id)} className={`p-2 rounded text-sm ${r.read ? 'bg-gray-700 text-gray-400' : 'bg-gray-600 text-white'}`}>
                  <span className="font-semibold text-yellow-400 mr-2">{String(r.type ?? '-')}</span>
                  {String(r.message ?? r.content ?? '-')}
                  <span className="ml-2 text-xs text-gray-500">{String(r.createdAt ?? '')}</span>
                  {!r.read && <span className="ml-2 text-xs bg-red-600 px-1 rounded">미읽음</span>}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
