'use client';

import { useState, useEffect } from 'react';
import { api, setServer, type UserDto, type GradeDto, type ServerDto } from '@/lib/api';
import Link from 'next/link';
import { formatPrice } from '@/lib/formatPrice';
import { Edit2, Save, X, ChevronRight } from 'lucide-react';

function relativeTime(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const d = Math.floor(diff / 86_400_000);
  if (d < 1) return '오늘';
  if (d < 2) return '어제';
  return `${d}일 전`;
}

/** 등급·호봉 표시 — 예: "행상 1 패" */
function formatGradeStep(grade: GradeDto): string {
  if (!grade.gradeStep || !grade.stepUnit) return grade.grade;
  return `${grade.grade} ${grade.gradeStep} ${grade.stepUnit}`;
}

export default function ProfilePage() {
  const [user, setUser] = useState<UserDto | null>(null);
  const [grade, setGrade] = useState<GradeDto | null>(null);
  const [listings, setListings] = useState<unknown[]>([]);
  const [listingsPage, setListingsPage] = useState(0);
  const [reviews, setReviews] = useState<unknown[]>([]);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(false);
  const [servers, setServers] = useState<ServerDto[]>([]);
  const [form, setForm] = useState({ nickname: '', gameNickname: '', gameAccessTime: '', serverId: null as number | null });
  const [saving, setSaving] = useState(false);

  // 리뷰 제출 모달
  const [reviewTarget, setReviewTarget] = useState<{ id: number; counterparty: string } | null>(null);
  const [reviewRating, setReviewRating] = useState<'GOOD' | 'NEUTRAL' | 'BAD'>('GOOD');

  useEffect(() => {
    api.getMe()
      .then((u) => {
        setUser(u);
        setForm({
          nickname: u.nickname,
          gameNickname: u.gameNickname ?? '',
          gameAccessTime: u.gameAccessTime ?? '',
          serverId: u.serverId ?? null,
        });
      })
      .catch((e: unknown) => setError(String(e)));

    api.getServers().then(setServers).catch(() => {});
    api.getMyGrade().then(setGrade).catch(() => {});
    api.getMyTrades().then(setListings).catch(() => {});
    api.getReceivedReviews().then(setReviews).catch(() => {});
  }, []);

  async function handleSave() {
    setSaving(true);
    try {
      const { serverId, ...profileFields } = form;
      const updated = await api.updateMe(profileFields);
      if (serverId != null) {
        await api.updateMyServer(serverId);
        setServer(String(serverId));
      }
      const serverName = servers.find((s) => s.serverId === serverId)?.name;
      setUser({ ...updated, serverId: serverId ?? undefined, serverName });
      setEditing(false);
    } catch {
      alert('저장 중 오류가 발생했습니다.');
    } finally {
      setSaving(false);
    }
  }

  function handleCancelEdit() {
    if (!user) return;
    setForm({
      nickname: user.nickname,
      gameNickname: user.gameNickname ?? '',
      gameAccessTime: user.gameAccessTime ?? '',
      serverId: user.serverId ?? null,
    });
    setEditing(false);
  }

  const selectedServerName =
    servers.find((s) => s.serverId === form.serverId)?.name
    ?? user?.serverName
    ?? user?.server
    ?? null;

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

  const gradeDisplayName = grade?.grade ?? user.grade ?? '-';
  const initials = user.nickname?.slice(0, 2).toUpperCase() ?? '?';
  const hasStepProgress = grade != null && grade.expPerStep > 0 && grade.gradeStep != null;
  const stepProgressPercent = hasStepProgress
    ? Math.min(100, Math.round((grade.stepProgressExp / grade.expPerStep) * 100))
    : 0;

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
            {gradeDisplayName !== '-' && (
              <span
                style={{ background: 'var(--beige)', color: 'var(--brown)', border: '1px solid var(--brown)' }}
                className="text-xs font-medium px-2 py-0.5 rounded-full mt-2 inline-block"
              >
                {gradeDisplayName}
              </span>
            )}
          </div>

          {/* 등급 경험치 */}
          {grade && (
            <div style={{ background: 'var(--bg)', borderRadius: 8, padding: '12px 14px' }}>
              <div className="flex justify-between text-xs mb-2" style={{ color: 'var(--text-muted)' }}>
                <span>{formatGradeStep(grade)}</span>
                {hasStepProgress ? (
                  <span>{grade.stepProgressExp} / {grade.expPerStep}</span>
                ) : (
                  <span>{grade.totalExp.toLocaleString()} EXP</span>
                )}
              </div>
              {hasStepProgress && (
                <div
                  style={{ background: 'var(--border)', borderRadius: 999, height: 8, overflow: 'hidden' }}
                  role="progressbar"
                  aria-valuenow={grade.stepProgressExp}
                  aria-valuemin={0}
                  aria-valuemax={grade.expPerStep}
                >
                  <div
                    style={{
                      background: 'var(--brown)',
                      borderRadius: 999,
                      height: '100%',
                      width: `${stepProgressPercent}%`,
                      transition: 'width 0.4s ease',
                    }}
                  />
                </div>
              )}
            </div>
          )}

          {/* 매너점수 바 */}
          {user.mannerScore != null && (
            <div style={{ background: 'var(--bg)', borderRadius: 8, padding: '12px 14px' }}>
              <div className="flex justify-between text-xs mb-2" style={{ color: 'var(--text-muted)' }}>
                <span>매너점수</span>
                <span>{user.mannerScore}점</span>
              </div>
              <div
                style={{ background: 'var(--border)', borderRadius: 999, height: 8, overflow: 'hidden' }}
                role="progressbar"
                aria-valuenow={user.mannerScore}
                aria-valuemin={0}
                aria-valuemax={100}
              >
                <div
                  style={{
                    background: user.mannerScore >= 70 ? 'var(--brown)' : user.mannerScore >= 40 ? '#b08030' : 'var(--danger)',
                    borderRadius: 999,
                    height: '100%',
                    width: `${user.mannerScore}%`,
                    transition: 'width 0.4s ease',
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
                  <button onClick={handleCancelEdit}
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
                { label: '닉네임', key: 'nickname' as const, value: form.nickname, placeholder: '닉네임 입력' },
                { label: '게임 닉네임', key: 'gameNickname' as const, value: form.gameNickname, placeholder: '게임 내 닉네임' },
                { label: '접속 가능 시간', key: 'gameAccessTime' as const, value: form.gameAccessTime, placeholder: '예: 21시~24시' },
              ].map(({ label, key, value, placeholder }) => (
                <div key={label}>
                  <label style={{ color: 'var(--text-muted)' }} className="text-xs font-medium block mb-1">{label}</label>
                  {editing && key ? (
                    <input
                      value={form[key]}
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

              <div>
                <label style={{ color: 'var(--text-muted)' }} className="text-xs font-medium block mb-1">서버</label>
                {editing ? (
                  servers.length > 0 ? (
                    <div className="flex flex-wrap gap-1.5">
                      {servers.map((s) => {
                        const selected = form.serverId === s.serverId;
                        return (
                          <button
                            key={s.serverId}
                            type="button"
                            onClick={() => setForm((prev) => ({ ...prev, serverId: s.serverId }))}
                            className="px-3 py-1.5 rounded text-xs transition-colors"
                            style={{
                              background: selected ? 'var(--brown)' : 'var(--bg)',
                              border: `1px solid ${selected ? 'var(--brown)' : 'var(--border)'}`,
                              color: selected ? 'var(--beige)' : 'var(--text-muted)',
                            }}
                          >
                            {s.name}
                          </button>
                        );
                      })}
                    </div>
                  ) : (
                    <p style={{ color: 'var(--text-disabled)' }}>서버 목록을 불러오는 중...</p>
                  )
                ) : (
                  <p style={{ color: selectedServerName ? 'var(--text)' : 'var(--text-disabled)' }}>
                    {selectedServerName || '미설정'}
                  </p>
                )}
              </div>
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
            ) : (() => {
              const PAGE_SIZE = 5;
              const totalPages = Math.ceil(listings.length / PAGE_SIZE);
              const page = Math.min(listingsPage, totalPages - 1);
              const pageItems = listings.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);
              return (
                <>
                  <div className="space-y-2">
                    {pageItems.map((item: unknown) => {
                      const r = item as Record<string, unknown>;
                      const displayName = String(r.displayName ?? '-');
                      const role = String(r.role ?? '판매');
                      const isSell = role === '판매';
                      return (
                        <div key={String(r.tradeId)}
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
                              {role}
                            </span>
                            <span className="text-sm font-medium truncate" style={{ color: 'var(--text)' }}>
                              {displayName}
                            </span>
                          </div>
                          <div className="flex items-center gap-3 shrink-0">
                            <span className="font-serif text-sm font-bold" style={{ color: 'var(--brown)' }}>
                              {r.confirmedPrice ? formatPrice(Number(r.confirmedPrice)) : '-'}
                            </span>
                            <span style={{ color: 'var(--text-disabled)' }} className="text-xs">
                              {r.confirmedAt ? relativeTime(String(r.confirmedAt)) : ''}
                            </span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                  {totalPages > 1 && (
                    <div className="flex items-center justify-center gap-2 mt-4">
                      <button
                        onClick={() => setListingsPage((p) => Math.max(0, p - 1))}
                        disabled={page === 0}
                        className="px-3 py-1 rounded text-xs disabled:opacity-30"
                        style={{ background: 'var(--bg)', color: 'var(--text)', border: '1px solid var(--border)' }}
                      >
                        이전
                      </button>
                      <span className="text-xs" style={{ color: 'var(--text-muted)' }}>
                        {page + 1} / {totalPages}
                      </span>
                      <button
                        onClick={() => setListingsPage((p) => Math.min(totalPages - 1, p + 1))}
                        disabled={page === totalPages - 1}
                        className="px-3 py-1 rounded text-xs disabled:opacity-30"
                        style={{ background: 'var(--bg)', color: 'var(--text)', border: '1px solid var(--border)' }}
                      >
                        다음
                      </button>
                    </div>
                  )}
                </>
              );
            })()}
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
