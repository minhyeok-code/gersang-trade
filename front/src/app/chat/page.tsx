'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { api } from '@/lib/api';

export default function ChatPage() {
  const [rooms, setRooms] = useState<unknown[]>([]);
  const [error, setError] = useState('');

  // 채팅방 생성 폼
  const [listingId, setListingId] = useState('');
  const [listingType, setListingType] = useState('SELL');
  const [creating, setCreating] = useState(false);

  async function load() {
    try {
      const data = await api.getChatRooms();
      setRooms(data);
    } catch (e: unknown) {
      setError(String(e));
    }
  }

  useEffect(() => { load(); }, []);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setCreating(true);
    try {
      await api.createChatRoom({ listingId: Number(listingId), listingType });
      load();
      setListingId('');
    } catch (e: unknown) {
      alert(String(e));
    } finally {
      setCreating(false);
    }
  }

  return (
    <div className="max-w-5xl mx-auto p-4 space-y-6">
      <h1 className="text-2xl font-bold text-yellow-400">채팅방</h1>

      <div className="bg-gray-800 rounded p-4">
        <h2 className="font-semibold mb-3">채팅방 생성 — POST /api/chat-rooms</h2>
        <form onSubmit={handleCreate} className="flex gap-2 flex-wrap">
          <input type="number" value={listingId} onChange={(e) => setListingId(e.target.value)}
            placeholder="등록글 ID" required
            className="bg-gray-700 border border-gray-600 rounded px-3 py-2 text-sm w-36" />
          <select value={listingType} onChange={(e) => setListingType(e.target.value)}
            className="bg-gray-700 border border-gray-600 rounded px-3 py-2 text-sm">
            <option value="SELL">SELL</option>
            <option value="WANTED">WANTED</option>
          </select>
          <button type="submit" disabled={creating}
            className="bg-yellow-500 text-black px-4 py-2 rounded text-sm font-semibold">
            생성
          </button>
        </form>
      </div>

      {error && <p className="text-red-400 text-sm">{error}</p>}

      <div>
        <h2 className="font-semibold mb-2 text-gray-300">내 채팅방 — GET /api/chat-rooms</h2>
        <div className="space-y-2">
          {rooms.length === 0 && <p className="text-gray-500 text-sm">채팅방이 없습니다.</p>}
          {rooms.map((room: unknown) => {
            const r = room as Record<string, unknown>;
            return (
              <Link key={String(r.id)} href={`/chat/${r.id}`}
                className="block bg-gray-800 rounded p-3 hover:bg-gray-700">
                <div className="flex items-center gap-4">
                  <span className="font-semibold text-white">방 #{String(r.id)}</span>
                  <span className="text-xs text-gray-400">상태: {String(r.status ?? '-')}</span>
                  <span className="text-xs text-gray-400">
                    {r.listingDisplayName ? String(r.listingDisplayName) : `등록글 #${String(r.listingId ?? '-')}`}
                  </span>
                </div>
              </Link>
            );
          })}
        </div>
      </div>
    </div>
  );
}
