'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import { api } from '@/lib/api';

export default function ListingDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [data, setData] = useState<object | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.getListing(Number(id))
      .then((d) => setData(d as object))
      .catch((e: unknown) => setError(String(e)));
  }, [id]);

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-yellow-400">판매 상세 — GET /api/listings/{id}</h1>
      {error && <p className="text-red-400">{error}</p>}
      {data && (
        <pre className="bg-gray-800 rounded p-4 text-xs text-gray-300 overflow-auto max-h-[70vh]">
          {JSON.stringify(data, null, 2)}
        </pre>
      )}
    </div>
  );
}
