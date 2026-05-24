'use client';

import { useState, useEffect } from 'react';
import { api } from '@/lib/api';

export default function WantedPage() {
  const [items, setItems] = useState<unknown[]>([]);
  const [error, setError] = useState('');
  const [detail, setDetail] = useState<object | null>(null);

  useEffect(() => {
    api.getWanted()
      .then(setItems)
      .catch((e: unknown) => setError(String(e)));
  }, []);

  async function loadDetail(id: number) {
    try {
      const d = await api.getWantedDetail(id);
      setDetail(d as object);
    } catch (e: unknown) {
      setDetail({ error: String(e) });
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('취소하시겠습니까?')) return;
    try {
      await api.deleteWanted(id);
      setItems((prev) => (prev as Record<string, unknown>[]).filter((r) => r.id !== id));
    } catch (e: unknown) {
      alert(String(e));
    }
  }

  return (
    <div className="max-w-5xl mx-auto p-4 space-y-6">
      <h1 className="text-2xl font-bold text-yellow-400">구매 희망 목록 — GET /api/wanted</h1>

      {error && <p className="text-red-400 text-sm">{error}</p>}

      <div className="space-y-2">
        {items.length === 0 && <p className="text-gray-500 text-sm">구매 희망 등록글이 없습니다.</p>}
        {items.map((item: unknown) => {
          const r = item as Record<string, unknown>;
          return (
            <div key={String(r.id)} className="bg-gray-800 rounded p-3 flex items-center gap-4">
              <div className="flex-1">
                <span className="font-semibold text-white">{String(r.title ?? r.id)}</span>
                <div className="text-xs text-gray-400 mt-1">
                  서버: {String(r.server ?? '-')} / 상태: {String(r.status ?? '-')} / 예산: {String(r.budget ?? '-')}
                </div>
              </div>
              <button onClick={() => loadDetail(Number(r.id))} className="text-xs bg-blue-700 px-2 py-1 rounded">상세</button>
              <button onClick={() => handleDelete(Number(r.id))} className="text-xs text-red-400">취소</button>
            </div>
          );
        })}
      </div>

      {detail && (
        <div className="bg-gray-800 rounded p-4">
          <h2 className="font-semibold mb-2">상세 — GET /api/wanted/{'{id}'}</h2>
          <pre className="text-xs text-gray-300 overflow-auto max-h-64">{JSON.stringify(detail, null, 2)}</pre>
        </div>
      )}
    </div>
  );
}
