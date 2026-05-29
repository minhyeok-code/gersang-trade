'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/api';

export default function CreateListingPage() {
  const router = useRouter();
  const [server, setServer] = useState('');
  const [price, setPrice] = useState('');
  const [note, setNote] = useState('');
  const [itemId, setItemId] = useState('');
  const [quantity, setQuantity] = useState('1');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      // 재료 번들 단순 예시 (MATERIAL_BUNDLE)
      const body = {
        server,
        price: Number(price),
        note: note || null,
        bundles: [
          {
            bundleType: 'MATERIAL_BUNDLE',
            titleOverride: null,
            lines: [
              {
                itemId: Number(itemId),
                quantity: Number(quantity),
                sortOrder: 0,
              },
            ],
          },
        ],
      };
      await api.createListing(body);
      router.push('/listings');
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-lg space-y-6">
      <h1 className="text-2xl font-bold text-yellow-400">판매 등록 — POST /api/listings</h1>
      <p className="text-gray-400 text-sm">재료 번들(MATERIAL_BUNDLE) 단순 테스트용입니다.</p>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm text-gray-300 mb-1">서버명 *</label>
          <input value={server} onChange={(e) => setServer(e.target.value)} required
            className="w-full bg-gray-800 border border-gray-600 rounded px-3 py-2 text-sm" />
        </div>
        <div>
          <label className="block text-sm text-gray-300 mb-1">가격 (골드) *</label>
          <input type="number" value={price} onChange={(e) => setPrice(e.target.value)} required min="1"
            className="w-full bg-gray-800 border border-gray-600 rounded px-3 py-2 text-sm" />
        </div>
        <div>
          <label className="block text-sm text-gray-300 mb-1">메모</label>
          <input value={note} onChange={(e) => setNote(e.target.value)}
            className="w-full bg-gray-800 border border-gray-600 rounded px-3 py-2 text-sm" />
        </div>
        <div>
          <label className="block text-sm text-gray-300 mb-1">아이템 ID *</label>
          <input type="number" value={itemId} onChange={(e) => setItemId(e.target.value)} required
            placeholder="아이템 검색에서 확인"
            className="w-full bg-gray-800 border border-gray-600 rounded px-3 py-2 text-sm" />
        </div>
        <div>
          <label className="block text-sm text-gray-300 mb-1">수량</label>
          <input type="number" value={quantity} onChange={(e) => setQuantity(e.target.value)} min="1"
            className="w-full bg-gray-800 border border-gray-600 rounded px-3 py-2 text-sm" />
        </div>

        {error && <p className="text-red-400 text-sm">{error}</p>}

        <div className="flex gap-2">
          <button type="submit" disabled={loading}
            className="bg-yellow-500 text-black px-6 py-2 rounded font-semibold disabled:opacity-50">
            {loading ? '등록 중...' : '등록'}
          </button>
          <button type="button" onClick={() => router.back()}
            className="bg-gray-700 px-6 py-2 rounded">
            취소
          </button>
        </div>
      </form>
    </div>
  );
}
