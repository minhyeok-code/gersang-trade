'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { api, type PublicUserDto } from '@/lib/api';

function formatGrade(grade?: string, gradeStep?: number): string {
  if (!grade) return '-';
  if (gradeStep == null) return grade;
  return `${grade} ${gradeStep}`;
}

function mannerLabel(score: number): string {
  if (score >= 80) return '매우 좋음';
  if (score >= 60) return '좋음';
  if (score >= 40) return '보통';
  if (score >= 20) return '나쁨';
  return '매우 나쁨';
}

function mannerColor(score: number): string {
  if (score >= 70) return 'var(--brown)';
  if (score >= 40) return '#b08030';
  return 'var(--danger)';
}

export default function PublicProfilePage() {
  const params = useParams();
  const userId = Number(params.userId);

  const [user, setUser] = useState<PublicUserDto | null>(null);
  const [reviews, setReviews] = useState<unknown[]>([]);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!userId) return;
    api.getUser(userId).then(setUser).catch((e: unknown) => setError(String(e)));
    api.getUserReviews(userId).then(setReviews).catch(() => {});
  }, [userId]);

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-[calc(100vh-76px)]" style={{ background: 'var(--bg)' }}>
        <p style={{ color: 'var(--danger)' }}>{error}</p>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="flex items-center justify-center min-h-[calc(100vh-76px)]" style={{ background: 'var(--bg)' }}>
        <p style={{ color: 'var(--text-muted)' }} className="text-sm">불러오는 중...</p>
      </div>
    );
  }

  const score = user.mannerScore ?? 60;

  return (
    <div className="max-w-lg mx-auto px-4 py-8 space-y-4" style={{ background: 'var(--bg)' }}>
      {/* 기본 정보 */}
      <div style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-5">
        <div className="flex items-center gap-4 mb-5">
          <div
            className="w-14 h-14 rounded-full flex items-center justify-center text-xl font-bold shrink-0"
            style={{ background: 'var(--beige)', color: 'var(--brown)' }}
          >
            {user.nickname.slice(0, 2).toUpperCase()}
          </div>
          <div>
            <p className="font-semibold text-lg" style={{ color: 'var(--text)' }}>{user.nickname}</p>
            {user.gameNickname && (
              <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>게임: {user.gameNickname}</p>
            )}
          </div>
        </div>

        {/* md 4-4절 형식: 거래량 / 매너점수 / 등급 */}
        <div
          className="grid grid-cols-3 divide-x"
          style={{ background: 'var(--bg)', border: '1px solid var(--border)', borderRadius: 8, overflow: 'hidden' }}
        >
          <div className="flex flex-col items-center py-4 gap-1">
            <span className="text-xs" style={{ color: 'var(--text-muted)' }}>거래량</span>
            <span className="font-bold font-serif text-lg" style={{ color: 'var(--text)' }}>
              {user.tradeCount ?? 0}건
            </span>
          </div>
          <div className="flex flex-col items-center py-4 gap-1">
            <span className="text-xs" style={{ color: 'var(--text-muted)' }}>매너점수</span>
            <span className="font-bold font-serif text-lg" style={{ color: mannerColor(score) }}>
              {score}점
            </span>
            <span className="text-xs" style={{ color: mannerColor(score) }}>{mannerLabel(score)}</span>
          </div>
          <div className="flex flex-col items-center py-4 gap-1">
            <span className="text-xs" style={{ color: 'var(--text-muted)' }}>등급</span>
            <span className="font-bold font-serif text-lg" style={{ color: 'var(--brown)' }}>
              {formatGrade(user.grade, user.gradeStep)}
            </span>
          </div>
        </div>

        {user.gameAccessTime && (
          <p className="text-xs mt-3" style={{ color: 'var(--text-muted)' }}>
            접속 가능 시간: {user.gameAccessTime}
          </p>
        )}
      </div>

      {/* 받은 리뷰 */}
      <div style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-5">
        <h2 className="font-semibold mb-4 text-sm" style={{ color: 'var(--text)' }}>
          받은 평가
          <span className="font-normal text-xs ml-2" style={{ color: 'var(--text-muted)' }}>{reviews.length}건</span>
        </h2>
        {reviews.length === 0 ? (
          <p className="text-sm text-center py-4" style={{ color: 'var(--text-disabled)' }}>아직 받은 평가가 없습니다</p>
        ) : (
          <div className="space-y-2">
            {(reviews as Array<Record<string, unknown>>).map((r) => {
              const rating = String(r.rating ?? '');
              const color = rating === 'GOOD' ? 'var(--brown)' : rating === 'BAD' ? 'var(--danger)' : 'var(--text-muted)';
              const label = rating === 'GOOD' ? '👍 좋아요' : rating === 'BAD' ? '👎 나빠요' : '😐 보통이에요';
              return (
                <div
                  key={String(r.id)}
                  className="flex items-center justify-between px-3 py-2 rounded-lg"
                  style={{ background: 'var(--bg)', border: '1px solid var(--border)' }}
                >
                  <span className="text-sm font-medium" style={{ color }}>{label}</span>
                  <span className="text-xs" style={{ color: 'var(--text-disabled)' }}>
                    {r.reviewerNickname ? String(r.reviewerNickname) : ''}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
