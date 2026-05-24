'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { api } from '@/lib/api';

export default function ListingsPage() {
  const [listings, setListings] = useState<unknown[]>([]);
  const [error, setError] = useState('');
  const [server, setServer] = useState('');
  const [keyword, setKeyword] = useState('');

  async function load() {
    setError('');
    try {
      const params: Record<string, string> = {};
      if (server) params.server = server;
      if (keyword) params.keyword = keyword;
      const data = await api.getListings(params);
      setListings(data);
    } catch (e: unknown) {
      setError(String(e));
    }
  }

  useEffect(() => { load(); }, []);

  async function handleDelete(id: number) {
    if (!confirm('취소하시겠습니까?')) return;
    try {
      await api.deleteListing(id);
      load();
    } catch (e: unknown) {
      alert(String(e));
    }
  }

  return (
    <div className="max-w-5xl mx-auto p-4 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-yellow-400">판매 등록글 — GET /api/listings</h1>
        <Link href="/listings/create" className="bg-yellow-500 text-black px-4 py-2 rounded text-sm font-semibold">
          + 새 등록
        </Link>
      </div>

      <div className="flex gap-2 flex-wrap">
        <input value={server} onChange={(e) => setServer(e.target.value)} placeholder="서버명" className="bg-gray-800 border border-gray-600 rounded px-3 py-2 text-sm w-32" />
        <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="키워드" className="bg-gray-800 border border-gray-600 rounded px-3 py-2 text-sm w-40" />
        <button onClick={load} className="bg-blue-600 hover:bg-blue-500 px-4 py-2 rounded text-sm">검색</button>
      </div>

      {error && <p className="text-red-400 text-sm">{error}</p>}

      <div className="space-y-2">
        {listings.length === 0 && <p className="text-gray-500 text-sm">등록글이 없습니다.</p>}
        {listings.map((item: unknown) => {
          const r = item as Record<string, unknown>;
          return (
            <div key={String(r.id)} className="bg-gray-800 rounded p-3 flex items-center gap-4">
              <div className="flex-1">
                <Link href={`/listings/${r.id}`} className="font-semibold text-white hover:text-yellow-400">
                  {String(r.title ?? r.id)}
                </Link>
                <div className="text-xs text-gray-400 mt-1">
                  서버: {String(r.server ?? '-')} / 상태: {String(r.status ?? '-')} / 가격: {String(r.price ?? '-')}
                </div>
              </div>
              <button onClick={() => handleDelete(Number(r.id))} className="text-xs text-red-400 hover:text-red-300">취소</button>
            </div>
          );
        })}
      </div>
    </div>
  );
}
