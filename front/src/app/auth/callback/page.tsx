'use client';

import { useEffect } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { setToken } from '@/lib/api';
import { Suspense } from 'react';

function CallbackInner() {
  const params = useSearchParams();
  const router = useRouter();

  useEffect(() => {
    const token = params.get('accessToken');
    if (token) {
      setToken(token);
      window.location.href = '/profile';
    } else {
      window.location.href = '/';
    }
  }, [params, router]);

  return <p className="text-center mt-20 text-gray-400">로그인 처리 중...</p>;
}

export default function AuthCallback() {
  return (
    <Suspense fallback={<p className="text-center mt-20 text-gray-400">로딩 중...</p>}>
      <CallbackInner />
    </Suspense>
  );
}
