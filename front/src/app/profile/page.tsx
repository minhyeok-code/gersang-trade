'use client';

import { useState, useEffect } from 'react';
import { api, type UserDto, type GradeDto } from '@/lib/api';
import Link from 'next/link';
import { Edit2, Save, X, ChevronRight } from 'lucide-react';

function formatPrice(price: number): string {
  if (price >= 100_000_000) return `${(price / 100_000_000).toFixed(1)}억 전`;
  if (price >= 10_000) return `${Math.floor(price / 10_000)}만 전`;
  return `${price.toLocaleString()} 전`;
}

function relativeTime(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const d = Math.floor(diff / 86_400_000);
  if (d < 1) return '오늘';
  if (d < 2) return '어제';
  return `${d}일 전`;
}

const GRADE_LABELS: Record<string, string> = {
  BRONZE: '브론즈',
  SILVER: '실버',
  GOLD: '골드',
  PLATINUM: '플래티넘',
  DIAMOND: '다이아',
};

export default function ProfilePage() {
  const [user, setUser] = useState<UserDto | null>(null);
  const [grade, setGrade] = useState<GradeDto | null>(null);
  const [listings, setListings] = useState<unknown[]>([]);
  const [reviews, setReviews] = useState<unknown[]>([]);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ nickname: '', gameNickname: '', gameAccessTime: '' });
  const [saving, setSaving] = useState(false);

  // 리뷰 제출 모달
  const [reviewTarget, setReviewTarget] = useState<{ id: number; counterparty: string } | null>(null);
  const [reviewRating, setReviewRating] = useState<'GOOD' | 'NEUTRAL' | 'BAD'>('GOOD');

  useEffect(() => {
    api.getMe()
      .then((u) => {
        setUser(u);
        setForm({ nickname: u.nickname, gameNickname: u.gameNickname ?? '', gameAccessTime: u.gameAccessTime ?? '' });
      })
      .catch((e: unknown) => setError(String(e)));

    api.getMyGrade().then(setGrade).catch(() => {});
    api.getMyListings().then(setListings).catch(() => {});
    api.getReceivedReviews().then(setReviews).catch(() => {});
  }, []);

  async function handleSave() {
    setSaving(true);
    try {
      const updated = await api.updateMe(form);
      setUser(updated);
      setEditing(false);
    } catch {
      alert('저장 중 오류가 발생했습니다.');
    } finally {
      setSaving(false);
    }
  }

  async function handleSubmitReview() {
    if (!reviewTarget) return;
    try {
      await api.submitReview(reviewTarget.id, { rating: reviewRating });
      setReviewTarget(null);
      api.getReceivedReviews().then(setReviews);
      alert('리뷰가 등록되었습니다.');
    } catch {
      alert('리뷰 등록 중 오류가 발생했습니다.');
    }
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-[calc(100vh-76px)]" style={{ background: 'var(--bg)' }}>
        <div className="text-center">
          <p style={{ color: 'var(--danger)' }} className="mb-4">{error}</p>
          <Link href="/login" style={{ background: 'var(--brown)', color: 'var(--beige)' }} className="px-5 py-2 rounded text-sm font-semibold">
            로그인하기
          </Link>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="py-8">
        <div className="grid lg:grid-cols-3 gap-6">
          {[1, 2, 3].map((i) => (
            <div key={i} style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
              className="h-48 rounded-xl animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  const gradeLabel = GRADE_LABELS[user.grade ?? ''] ?? (user.grade ?? '-');
  const initials = user.nickname?.slice(0, 2).toUpperCase() ?? '?';

  return (
    <div className="max-w-[1200px] mx-auto px-4 py-8">
      <div className="grid lg:grid-cols-3 gap-6">
        {/* 프로필 카드 */}
        <div style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-6 space-y-5">
          <div className="text-center">
            <div
              style={{ background: 'var(--brown)', color: 'var(--beige)' }}
              className="w-20 h-20 rounded-full flex items-center justify-center mx-auto text-3xl font-serif font-bold mb-3"
            >
              {initials}
            </div>
            <h1 className="font-semibold text-xl" style={{ color: 'var(--text)' }}>{user.nickname}</h1>
            {user.grade && (
              <span
                style={{ background: 'var(--beige)', color: 'var(--brown)', border: '1px solid var(--brown)' }}
                className="text-xs font-medium px-2 py-0.5 rounded-full mt-2 inline-block"
              >
                {gradeLabel}
              </span>
            )}
          </div>

          {/* 등급 경험치 */}
          {grade && (
            <div style={{ background: 'var(--bg)', borderRadius: 8, padding: '12px 14px' }}>
              <div className="flex justify-between text-xs mb-2" style={{ color: 'var(--text-muted)' }}>
                <span>{gradeLabel} {grade.gradeStep}단계</span>
                <span>{grade.totalExp} / {grade.nextLevelExp} EXP</span>
              </div>
              <div style={{ background: 'var(--border)', borderRadius: 4, height: 6 }}>
                <div
                  style={{
                    background: 'var(--brown)',
                    borderRadius: 4,
                    height: 6,
                    width: `${Math.min(100, Math.round(grade.totalExp / grade.nextLevelExp * 100))}%`,
                    transition: 'width 0.5s',
                  }}
                />
              </div>
            </div>
          )}

          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span style={{ color: 'var(--text-muted)' }}>거래 수</span>
              <span style={{ color: 'var(--text)' }} className="font-medium">{user.tradeCount ?? 0}건</span>
            </div>
            {user.createdAt && (
              <div className="flex justify-between">
                <span style={{ color: 'var(--text-muted)' }}>가입일</span>
                <span style={{ color: 'var(--text)' }}>{user.createdAt.slice(0, 10)}</span>
              </div>
            )}
          </div>

          <Link href="/deck"
            style={{ background: 'var(--brown)', color: 'var(--beige)' }}
            className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded text-sm font-semibold hover:bg-[var(--brown-dark)] transition-colors">
            내 덱 구성하기
            <ChevronRight style={{ width: 16, height: 16 }} />
          </Link>
        </div>

        {/* 우측 상세 */}
        <div className="lg:col-span-2 space-y-5">
          {/* 프로필 정보 수정 */}
          <div style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-5">
            <div className="flex items-center justify-between mb-4">
              <h2 className="font-semibold" style={{ color: 'var(--text)' }}>프로필 정보</h2>
              {!editing ? (
                <button onClick={() => setEditing(true)}
                  style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
                  className="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded hover:border-[var(--brown)] hover:text-[var(--brown)] transition-colors">
                  <Edit2 style={{ width: 12, height: 12 }} /> 수정
                </button>
              ) : (
                <div className="flex gap-2">
                  <button onClick={() => setEditing(false)}
                    style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
                    className="flex items-center gap-1 text-xs px-3 py-1.5 rounded hover:text-[var(--danger)] transition-colors">
                    <X style={{ width: 12, height: 12 }} /> 취소
                  </button>
                  <button onClick={handleSave} disabled={saving}
                    style={{ background: 'var(--brown)', color: 'var(--beige)' }}
                    className="flex items-center gap-1 text-xs px-3 py-1.5 rounded hover:bg-[var(--brown-dark)] transition-colors disabled:opacity-50">
                    <Save style={{ width: 12, height: 12 }} /> 저장
                  </button>
                </div>
              )}
            </div>

            <div className="grid sm:grid-cols-2 gap-4 text-sm">
              {[
                { label: '닉네임', key: 'nickname', value: form.nickname, placeholder: '닉네임 입력' },
                { label: '이메일', key: null, value: user.email, placeholder: '' },
                { label: '게임 닉네임', key: 'gameNickname', value: form.gameNickname, placeholder: '게임 내 닉네임' },
                { label: '접속 가능 시간', key: 'gameAccessTime', value: form.gameAccessTime, placeholder: '예: 21시~24시' },
              ].map(({ label, key, value, placeholder }) => (
                <div key={label}>
                  <label style={{ color: 'var(--text-muted)' }} className="text-xs font-medium block mb-1">{label}</label>
                  {editing && key ? (
                    <input
                      value={form[key as keyof typeof form]}
                      onChange={(e) => setForm((prev) => ({ ...prev, [key]: e.target.value }))}
                      placeholder={placeholder}
                      style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
                      className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]"
                    />
                  ) : (
                    <p style={{ color: value ? 'var(--text)' : 'var(--text-disabled)' }}>
                      {value || '미설정'}
                    </p>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* 내 판매 리스팅 */}
          <div style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-5">
            <h2 className="font-semibold mb-4" style={{ color: 'var(--text)' }}>
              최근 거래 내역
              <span style={{ color: 'var(--text-muted)' }} className="font-normal text-xs ml-2">{listings.length}건</span>
            </h2>
            {listings.length === 0 ? (
              <p style={{ color: 'var(--text-disabled)' }} className="text-sm text-center py-6">등록된 거래가 없습니다</p>
            ) : (
              <div className="space-y-2">
                {listings.slice(0, 8).map((item: unknown) => {
                  const r = item as Record<string, unknown>;
                  const isSell = String(r.listingType ?? r.type ?? 'SELL') !== 'BUY';
                  return (
                    <div key={String(r.id)}
                      style={{ background: 'var(--bg)', border: '1px solid var(--border)' }}
                      className="flex items-center justify-between px-4 py-3 rounded-lg">
                      <div className="flex items-center gap-3 min-w-0">
                        <span
                          style={{
                            background: isSell ? 'var(--sell-bg)' : 'var(--buy-bg)',
                            color: isSell ? 'var(--sell-text)' : 'var(--buy-text)',
                            border: `1px solid ${isSell ? 'var(--sell-border)' : 'var(--buy-border)'}`,
                          }}
                          className="text-xs px-1.5 py-0.5 rounded font-medium shrink-0"
                        >
                          {isSell ? '판매' : '구매'}
                        </span>
                        <span className="text-sm font-medium truncate" style={{ color: 'var(--text)' }}>
                          {String(r.displayName ?? r.title ?? '-')}
                        </span>
                      </div>
                      <div className="flex items-center gap-3 shrink-0">
                        <span className="font-serif text-sm font-bold" style={{ color: 'var(--brown)' }}>
                          {r.price ? formatPrice(Number(r.price)) : '-'}
                        </span>
                        <span style={{ color: 'var(--text-disabled)' }} className="text-xs">
                          {r.createdAt ? relativeTime(String(r.createdAt)) : ''}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* 받은 리뷰 */}
          <div style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-5">
            <h2 className="font-semibold mb-4" style={{ color: 'var(--text)' }}>
              받은 리뷰
              <span style={{ color: 'var(--text-muted)' }} className="font-normal text-xs ml-2">{reviews.length}건</span>
            </h2>
            {reviews.length === 0 ? (
              <p style={{ color: 'var(--text-disabled)' }} className="text-sm text-center py-6">아직 받은 리뷰가 없습니다</p>
            ) : (
              <div className="space-y-2">
                {reviews.map((rev: unknown) => {
                  const r = rev as Record<string, unknown>;
                  const rating = String(r.rating ?? '');
                  const color = rating === 'GOOD' ? 'var(--sell-text)' : rating === 'BAD' ? 'var(--danger)' : 'var(--text-muted)';
                  const label = rating === 'GOOD' ? '👍 좋아요' : rating === 'BAD' ? '👎 나빠요' : '😐 보통';
                  return (
                    <div key={String(r.id)}
                      style={{ background: 'var(--bg)', border: '1px solid var(--border)' }}
                      className="flex items-center gap-3 px-4 py-3 rounded-lg">
                      <span className="text-sm font-medium" style={{ color }}>{label}</span>
                      {r.comment != null && <span className="text-sm flex-1" style={{ color: 'var(--text)' }}>{String(r.comment)}</span>}
                      <span style={{ color: 'var(--text-disabled)' }} className="text-xs shrink-0">
                        {r.createdAt ? relativeTime(String(r.createdAt)) : ''}
                      </span>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 리뷰 제출 모달 */}
      {reviewTarget && (
        <div className="fixed inset-0 flex items-center justify-center z-[500]" style={{ background: 'rgba(0,0,0,0.65)' }}>
          <div style={{ background: 'var(--card)', border: '1px solid var(--border)', width: 360 }} className="rounded-xl p-6 space-y-4">
            <h2 className="font-semibold" style={{ color: 'var(--text)' }}>
              {reviewTarget.counterparty}님에게 리뷰 남기기
            </h2>
            <div className="flex gap-2">
              {(['GOOD', 'NEUTRAL', 'BAD'] as const).map((r) => {
                const label = r === 'GOOD' ? '👍 좋아요' : r === 'BAD' ? '👎 나빠요' : '😐 보통';
                return (
                  <button key={r} onClick={() => setReviewRating(r)}
                    style={{
                      flex: 1,
                      background: reviewRating === r ? 'var(--brown)' : 'var(--bg)',
                      border: `1px solid ${reviewRating === r ? 'var(--brown)' : 'var(--border)'}`,
                      color: reviewRating === r ? 'var(--beige)' : 'var(--text-muted)',
                    }}
                    className="py-2 text-xs rounded transition-colors">
                    {label}
                  </button>
                );
              })}
            </div>
            <div className="flex gap-2">
              <button onClick={() => setReviewTarget(null)}
                style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
                className="flex-1 py-2 rounded text-sm">취소</button>
              <button onClick={handleSubmitReview}
                style={{ background: 'var(--brown)', color: 'var(--beige)' }}
                className="flex-1 py-2 rounded text-sm font-semibold hover:bg-[var(--brown-dark)]">등록</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
