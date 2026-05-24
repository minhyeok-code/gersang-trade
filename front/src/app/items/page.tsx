'use client';

import { useState } from 'react';
import { api } from '@/lib/api';

export default function ItemsPage() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<unknown[]>([]);
  const [rituals, setRituals] = useState<unknown[]>([]);
  const [priceHistory, setPriceHistory] = useState<object | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function search() {
    if (!query.trim()) return;
    setError('');
    setLoading(true);
    try {
      const data = await api.searchItems(query, { limit: 20 });
      setResults(data);
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }

  async function loadRituals(itemId: number) {
    setSelectedId(itemId);
    try {
      const data = await api.getItemRituals(itemId);
      setRituals(data);
    } catch (e: unknown) {
      setRituals([]);
    }
  }

  async function loadPriceHistory(itemId: number) {
    try {
      const data = await api.getItemPriceHistory(itemId);
      setPriceHistory(data as object);
    } catch (e: unknown) {
      setPriceHistory({ error: String(e) });
    }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-yellow-400">아이템 검색 — GET /api/items/search</h1>

      <div className="flex gap-2">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && search()}
          placeholder="아이템명 입력 (예: 마검)"
          className="flex-1 bg-gray-800 border border-gray-600 rounded px-3 py-2 text-sm"
        />
        <button onClick={search} className="bg-yellow-500 text-black px-4 py-2 rounded text-sm font-semibold">
          검색
        </button>
      </div>

      {error && <p className="text-red-400 text-sm">{error}</p>}
      {loading && <p className="text-gray-400 text-sm">검색 중...</p>}

      {results.length > 0 && (
        <div>
          <p className="text-gray-400 text-sm mb-2">결과 {results.length}개</p>
          <div className="space-y-2">
            {results.map((item: unknown) => {
              const r = item as Record<string, unknown>;
              return (
                <div key={String(r.id)} className="bg-gray-800 rounded p-3 flex items-center gap-4">
                  <div className="flex-1">
                    <span className="font-semibold text-white">{String(r.name)}</span>
                    <span className="ml-2 text-xs text-gray-400">{String(r.type)} / {String(r.equipmentKind ?? '-')}</span>
                  </div>
                  <button
                    onClick={() => loadRituals(Number(r.id))}
                    className="text-xs bg-blue-700 hover:bg-blue-600 px-2 py-1 rounded"
                  >
                    주술 조회
                  </button>
                  <button
                    onClick={() => loadPriceHistory(Number(r.id))}
                    className="text-xs bg-green-700 hover:bg-green-600 px-2 py-1 rounded"
                  >
                    시세 조회
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {selectedId !== null && rituals.length >= 0 && (
        <div className="bg-gray-800 rounded p-4">
          <h2 className="font-semibold mb-2">아이템 #{selectedId} 주술 목록 — GET /api/items/{'{id}'}/rituals</h2>
          {rituals.length === 0 ? (
            <p className="text-gray-400 text-sm">주술 없음</p>
          ) : (
            <pre className="text-xs text-gray-300 overflow-auto max-h-48">{JSON.stringify(rituals, null, 2)}</pre>
          )}
        </div>
      )}

      {priceHistory && (
        <div className="bg-gray-800 rounded p-4">
          <h2 className="font-semibold mb-2">시세 히스토리 — GET /api/items/{'{id}'}/price-history</h2>
          <pre className="text-xs text-gray-300 overflow-auto max-h-48">{JSON.stringify(priceHistory, null, 2)}</pre>
        </div>
      )}
    </div>
  );
}
