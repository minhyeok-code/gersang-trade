'use client';

import { Suspense, useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { ArrowLeft, Clock, Search, Users } from 'lucide-react';
import {
  api,
  getToken,
  type HuntMonsterSummaryDto,
  type HuntPublicRecordDto,
  type HuntSnapshotDto,
  type MonsterDto,
  type MyClearTimeDto,
} from '@/lib/api';
import { parseApiError } from '@/lib/parseApiError';
import { DeckSnapshotViewer } from '@/components/hunt/DeckSnapshotViewer';
import { DpsBreakdown } from '@/components/hunt/DpsBreakdown';
import type { SnapshotDpsMeta } from '@/components/hunt/DeckSnapshotViewer';

type Tab = 'monsters' | 'mine';

function relativeTime(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const min = Math.floor(diff / 60_000);
  if (min < 1) return '방금';
  if (min < 60) return `${min}분 전`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr}시간 전`;
  const day = Math.floor(hr / 24);
  if (day < 30) return `${day}일 전`;
  return new Date(dateStr).toLocaleDateString('ko-KR');
}

function ClearTimePageInner() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const monsterParam = searchParams.get('monster');
  const snapshotParam = searchParams.get('snapshot');
  const selectedMonsterId = monsterParam ? Number(monsterParam) : null;
  const selectedSnapshotId = snapshotParam ? Number(snapshotParam) : null;

  const [tab, setTab] = useState<Tab>('monsters');
  const [monsters, setMonsters] = useState<HuntMonsterSummaryDto[]>([]);
  const [myRecords, setMyRecords] = useState<MyClearTimeDto[]>([]);
  const [records, setRecords] = useState<HuntPublicRecordDto[]>([]);
  const [snapshot, setSnapshot] = useState<HuntSnapshotDto | null>(null);
  const [snapshotMeta, setSnapshotMeta] = useState<{
    monsterId?: number;
    clearTimeSeconds?: number;
    totalResistPierce?: number | null;
    totalElementPierce?: number | null;
    rawDps?: number | null;
    adjustDps?: number | null;
    finalDps?: number | null;
    resistAfterDebuff?: number | null;
    effectiveMonsterElement?: number | null;
    resistPassRate?: number | null;
    authorNickname?: string;
    monsterName?: string;
  } | null>(null);

  const [loading, setLoading] = useState(true);
  const [recordsLoading, setRecordsLoading] = useState(false);
  const [snapshotLoading, setSnapshotLoading] = useState(false);
  const [recordsError, setRecordsError] = useState<string | null>(null);
  const [snapshotError, setSnapshotError] = useState<string | null>(null);
  const [allMonsters, setAllMonsters] = useState<MonsterDto[]>([]);
  const [monsterQuery, setMonsterQuery] = useState('');
  const [showMonsterSuggest, setShowMonsterSuggest] = useState(false);

  const normalizedMonsterQuery = monsterQuery.trim().toLowerCase();
  const filteredMonsters = useMemo(
    () => monsters.filter((m) =>
      !normalizedMonsterQuery || m.monsterName.toLowerCase().includes(normalizedMonsterQuery),
    ),
    [monsters, normalizedMonsterQuery],
  );
  const monsterSuggestions = useMemo(
    () => allMonsters
      .filter((m) => !normalizedMonsterQuery || m.name.toLowerCase().includes(normalizedMonsterQuery))
      .slice(0, 10),
    [allMonsters, normalizedMonsterQuery],
  );

  const snapshotDpsMeta = useMemo((): SnapshotDpsMeta | null => {
    if (!snapshotMeta) return null;
    const monster = snapshotMeta.monsterId != null
      ? allMonsters.find((m) => m.id === snapshotMeta.monsterId) ?? null
      : null;
    const resistanceType = snapshot?.content.dpsContext.resistanceType ?? 'HITTING';
    const baseResist = monster
      ? (resistanceType === 'MAGIC'
        ? monster.magicResistance
        : (monster.hittingResistance ?? monster.resistance))
      : null;
    const resistAfterDebuff = snapshotMeta.resistAfterDebuff
      ?? (baseResist != null && snapshotMeta.totalResistPierce != null
        ? baseResist - snapshotMeta.totalResistPierce
        : null);
    const effectiveMonsterElement = snapshotMeta.effectiveMonsterElement
      ?? (monster?.elementValue != null && snapshotMeta.totalElementPierce != null
        ? Math.max(0, monster.elementValue - snapshotMeta.totalElementPierce)
        : null);
    return {
      rawDps: snapshotMeta.rawDps,
      adjustDps: snapshotMeta.adjustDps,
      finalDps: snapshotMeta.finalDps,
      totalResistPierce: snapshotMeta.totalResistPierce,
      totalElementPierce: snapshotMeta.totalElementPierce,
      monster,
      monsterName: snapshotMeta.monsterName,
      resistAfterDebuff,
      effectiveMonsterElement,
      resistPassRate: snapshotMeta.resistPassRate,
    };
  }, [snapshotMeta, allMonsters, snapshot?.content.dpsContext.resistanceType]);

  const loadBase = useCallback(async () => {
    if (!getToken()) {
      router.replace('/login');
      return;
    }
    setLoading(true);
    try {
      const [monsterList, mine, catalog] = await Promise.all([
        api.getHuntMonsters(),
        api.getMyClearTimes(),
        api.getMonsters().catch(() => [] as MonsterDto[]),
      ]);
      setMonsters(monsterList);
      setMyRecords(mine);
      setAllMonsters(catalog);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [router]);

  useEffect(() => {
    loadBase();
  }, [loadBase]);

  useEffect(() => {
    if (!selectedMonsterId) {
      setRecords([]);
      setRecordsError(null);
      return;
    }
    setRecordsLoading(true);
    setRecordsError(null);
    api.getHuntMonsterRecords(selectedMonsterId)
      .then(setRecords)
      .catch((err) => {
        setRecords([]);
        setRecordsError(parseApiError(err));
      })
      .finally(() => setRecordsLoading(false));
  }, [selectedMonsterId]);

  useEffect(() => {
    if (!selectedSnapshotId) {
      setSnapshot(null);
      setSnapshotMeta(null);
      setSnapshotError(null);
      return;
    }
    setSnapshotLoading(true);
    setSnapshotError(null);
    api.getHuntSnapshot(selectedSnapshotId)
      .then((res) => {
        setSnapshot(res);
        const fromPublic = records.find((r) => r.deckSnapshotId === selectedSnapshotId);
        const fromMine = myRecords.find((r) => r.deckSnapshotId === selectedSnapshotId);
        const source = fromPublic ?? fromMine;
        setSnapshotMeta({
          monsterId: source?.monsterId,
          clearTimeSeconds: source?.clearTimeSeconds,
          totalResistPierce: source?.totalResistPierce,
          totalElementPierce: source?.totalElementPierce,
          rawDps: source?.rawDps,
          adjustDps: source?.adjustDps,
          finalDps: source?.finalDps,
          resistAfterDebuff: source?.resistAfterDebuff,
          effectiveMonsterElement: source?.effectiveMonsterElement,
          resistPassRate: source?.resistPassRate,
          authorNickname: fromPublic?.authorNickname,
          monsterName: source?.monsterName,
        });
      })
      .catch((err) => {
        setSnapshot(null);
        setSnapshotMeta(null);
        setSnapshotError(parseApiError(err));
      })
      .finally(() => setSnapshotLoading(false));
  }, [selectedSnapshotId, records, myRecords]);

  function selectMonster(monsterId: number, monsterName?: string) {
    const params = new URLSearchParams(searchParams.toString());
    params.set('monster', String(monsterId));
    params.delete('snapshot');
    router.push(`/clear-time?${params}`);
    setTab('monsters');
    if (monsterName) setMonsterQuery(monsterName);
    setShowMonsterSuggest(false);
  }

  function openSnapshot(snapshotId: number, monsterId?: number) {
    const params = new URLSearchParams(searchParams.toString());
    if (monsterId) params.set('monster', String(monsterId));
    params.set('snapshot', String(snapshotId));
    router.push(`/clear-time?${params}`);
  }

  function closeSnapshot() {
    const params = new URLSearchParams(searchParams.toString());
    params.delete('snapshot');
    router.push(`/clear-time?${params}`);
  }

  const selectedMonster = monsters.find((m) => m.monsterId === selectedMonsterId) ?? null;
  const selectedMonsterName = selectedMonster?.monsterName
    ?? allMonsters.find((m) => m.id === selectedMonsterId)?.name
    ?? (selectedMonsterId ? `몬스터 #${selectedMonsterId}` : null);

  if (loading) {
    return (
      <div className="py-12 text-center text-sm" style={{ color: 'var(--text-muted)' }}>
        사냥 허브 불러오는 중...
      </div>
    );
  }

  return (
    <div className="py-6">
      <div className="mb-6">
        <Link
          href="/deck"
          className="inline-flex items-center gap-1 text-xs mb-2 hover:underline"
          style={{ color: 'var(--text-muted)' }}
        >
          <ArrowLeft style={{ width: 14, height: 14 }} />
          전투 계산기
        </Link>
        <h1 className="font-serif text-2xl font-bold" style={{ color: 'var(--text)' }}>
          사냥 허브
        </h1>
        <p className="text-sm mt-1" style={{ color: 'var(--text-muted)' }}>
          클리어타임·덱 스냅샷을 공유하고 다른 유저의 빌드를 참고하세요.
        </p>
      </div>

      <div className="grid lg:grid-cols-[280px_1fr] gap-5">
        {/* 사이드바 */}
        <aside className="space-y-3">
          <div
            style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
            className="rounded-xl p-1 flex"
          >
            {(['monsters', 'mine'] as Tab[]).map((key) => (
              <button
                key={key}
                type="button"
                onClick={() => setTab(key)}
                className="flex-1 rounded-lg py-2 text-xs font-medium transition-colors"
                style={{
                  background: tab === key ? 'var(--brown)' : 'transparent',
                  color: tab === key ? '#fff' : 'var(--text-muted)',
                }}
              >
                {key === 'monsters' ? '몬스터' : '내 기록'}
              </button>
            ))}
          </div>

          {tab === 'monsters' && (
            <div
              style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
              className="rounded-xl p-3"
            >
              <div className="relative">
                <Search
                  style={{ position: 'absolute', left: 10, top: 10, width: 14, height: 14, color: 'var(--text-muted)' }}
                />
                <input
                  value={monsterQuery}
                  onFocus={() => setShowMonsterSuggest(true)}
                  onChange={(e) => {
                    setMonsterQuery(e.target.value);
                    setShowMonsterSuggest(true);
                  }}
                  onBlur={() => setTimeout(() => setShowMonsterSuggest(false), 120)}
                  placeholder="몬스터 검색..."
                  style={{
                    background: 'var(--bg)',
                    border: '1px solid var(--border)',
                    color: 'var(--text)',
                    paddingLeft: 32,
                  }}
                  className="w-full rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-[var(--brown)]"
                />
                {showMonsterSuggest && normalizedMonsterQuery && (
                  <div
                    style={{
                      background: 'var(--card)',
                      border: '1px solid var(--border)',
                      boxShadow: '0 6px 16px rgba(0,0,0,0.12)',
                    }}
                    className="absolute z-20 left-0 right-0 mt-1 rounded-lg max-h-48 overflow-y-auto"
                  >
                    {monsterSuggestions.length === 0 ? (
                      <p className="text-[11px] px-3 py-2" style={{ color: 'var(--text-disabled)' }}>
                        검색 결과 없음
                      </p>
                    ) : (
                      monsterSuggestions.map((m) => {
                        const hunt = monsters.find((h) => h.monsterId === m.id);
                        return (
                          <button
                            key={m.id}
                            type="button"
                            onMouseDown={(e) => e.preventDefault()}
                            onClick={() => selectMonster(m.id, m.name)}
                            style={{ borderBottom: '1px solid var(--border)', width: '100%', textAlign: 'left' }}
                            className="px-3 py-2 hover:bg-[var(--bg)] transition-colors"
                          >
                            <p className="text-xs font-medium truncate" style={{ color: 'var(--text)' }}>
                              {m.name}
                            </p>
                            <p className="text-[10px] mt-0.5" style={{ color: 'var(--text-muted)' }}>
                              {hunt ? `공개 기록 ${hunt.publicRecordCount}건` : '공개 기록 없음'}
                            </p>
                          </button>
                        );
                      })
                    )}
                  </div>
                )}
              </div>
            </div>
          )}

          <div
            style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
            className="rounded-xl overflow-hidden max-h-[calc(100vh-300px)] overflow-y-auto"
          >
            {tab === 'monsters' ? (
              monsters.length === 0 ? (
                <p className="text-xs p-4 text-center" style={{ color: 'var(--text-muted)' }}>
                  공개 기록이 있는 몬스터가 없습니다.
                </p>
              ) : filteredMonsters.length === 0 ? (
                <p className="text-xs p-4 text-center" style={{ color: 'var(--text-muted)' }}>
                  검색 결과가 없습니다.
                </p>
              ) : (
                filteredMonsters.map((m) => {
                  const active = m.monsterId === selectedMonsterId;
                  return (
                    <button
                      key={m.monsterId}
                      type="button"
                      onClick={() => selectMonster(m.monsterId, m.monsterName)}
                      className="w-full text-left px-4 py-3 transition-colors border-b last:border-b-0"
                      style={{
                        borderColor: 'var(--border)',
                        background: active ? 'var(--bg)' : 'transparent',
                      }}
                    >
                      <p className="text-sm font-medium truncate" style={{ color: 'var(--text)' }}>
                        {m.monsterName}
                      </p>
                      <p className="text-[11px] mt-0.5" style={{ color: 'var(--text-muted)' }}>
                        공개 기록 {m.publicRecordCount}건
                      </p>
                    </button>
                  );
                })
              )
            ) : (
              myRecords.length === 0 ? (
                <p className="text-xs p-4 text-center" style={{ color: 'var(--text-muted)' }}>
                  등록한 클리어타임이 없습니다.
                  <Link href="/deck" className="block mt-2" style={{ color: 'var(--brown)' }}>
                    덱 페이지에서 기록하기 →
                  </Link>
                </p>
              ) : (
                myRecords.map((r) => {
                  const active = r.deckSnapshotId === selectedSnapshotId;
                  return (
                    <button
                      key={r.id}
                      type="button"
                      onClick={() => openSnapshot(r.deckSnapshotId, r.monsterId)}
                      className="w-full text-left px-4 py-3 transition-colors border-b last:border-b-0"
                      style={{
                        borderColor: 'var(--border)',
                        background: active ? 'var(--bg)' : 'transparent',
                      }}
                    >
                      <p className="text-sm font-medium truncate" style={{ color: 'var(--text)' }}>
                        {r.monsterName}
                      </p>
                      <p className="text-[11px] mt-0.5" style={{ color: 'var(--text-muted)' }}>
                        {r.clearTimeSeconds}초 · {relativeTime(r.recordedAt)}
                      </p>
                      <DpsBreakdown
                        rawDps={r.rawDps}
                        adjustDps={r.adjustDps}
                        finalDps={r.finalDps}
                        className="mt-1"
                      />
                    </button>
                  );
                })
              )
            )}
          </div>
        </aside>

        {/* 메인 영역 */}
        <section className="min-w-0">
          {selectedSnapshotId ? (
            <div className="space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <button
                  type="button"
                  onClick={closeSnapshot}
                  className="text-xs inline-flex items-center gap-1 hover:underline"
                  style={{ color: 'var(--text-muted)' }}
                >
                  <ArrowLeft style={{ width: 14, height: 14 }} />
                  목록으로
                </button>
                {snapshotMeta && (
                  <div className="flex flex-wrap items-center gap-3 text-xs" style={{ color: 'var(--text-muted)' }}>
                    {snapshotMeta.monsterName && <span>{snapshotMeta.monsterName}</span>}
                    {snapshotMeta.clearTimeSeconds != null && (
                      <span className="inline-flex items-center gap-1">
                        <Clock style={{ width: 12, height: 12 }} />
                        {snapshotMeta.clearTimeSeconds}초
                      </span>
                    )}
                    {snapshotMeta.authorNickname && (
                      <span className="inline-flex items-center gap-1">
                        <Users style={{ width: 12, height: 12 }} />
                        {snapshotMeta.authorNickname}
                      </span>
                    )}
                  </div>
                )}
              </div>

              {snapshotLoading ? (
                <p className="text-sm py-8 text-center" style={{ color: 'var(--text-muted)' }}>스냅샷 불러오는 중...</p>
              ) : snapshotError ? (
                <ErrorBox message={snapshotError} />
              ) : snapshot ? (
                <DeckSnapshotViewer
                  content={snapshot.content}
                  dpsMeta={snapshotDpsMeta}
                />
              ) : null}
            </div>
          ) : tab === 'mine' ? (
            <div
              style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
              className="rounded-xl p-6 text-center"
            >
              <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
                왼쪽에서 내 기록을 선택하면 덱 스냅샷을 볼 수 있습니다.
              </p>
            </div>
          ) : !selectedMonsterId ? (
            <div
              style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
              className="rounded-xl p-6 text-center"
            >
              <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
                몬스터를 선택하면 공개 클리어타임 랭킹이 표시됩니다.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              <div
                style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
                className="rounded-xl px-4 py-3"
              >
                <h2 className="font-semibold" style={{ color: 'var(--text)' }}>
                  {selectedMonsterName}
                </h2>
                <p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>
                  클리어타임 빠른 순 · 공개 기록만 표시
                </p>
              </div>

              {recordsLoading ? (
                <p className="text-sm py-8 text-center" style={{ color: 'var(--text-muted)' }}>랭킹 불러오는 중...</p>
              ) : recordsError ? (
                <ErrorBox message={recordsError} />
              ) : records.length === 0 ? (
                <div
                  style={{ background: 'var(--card)', border: '1px solid var(--border)', color: 'var(--text-muted)' }}
                  className="rounded-xl p-6 text-center text-sm"
                >
                  이 몬스터의 공개 기록이 없습니다.
                </div>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-2">
                  {records.map((r, idx) => (
                    <ClearTimeRecordCard
                      key={r.id}
                      rank={idx + 1}
                      authorNickname={r.authorNickname}
                      recordedAt={r.recordedAt}
                      clearTimeSeconds={r.clearTimeSeconds}
                      rawDps={r.rawDps}
                      adjustDps={r.adjustDps}
                      finalDps={r.finalDps}
                      onClick={() => openSnapshot(r.deckSnapshotId, r.monsterId)}
                    />
                  ))}
                </div>
              )}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

function ClearTimeRecordCard({
  rank,
  authorNickname,
  recordedAt,
  clearTimeSeconds,
  rawDps,
  adjustDps,
  finalDps,
  onClick,
}: {
  rank: number;
  authorNickname: string;
  recordedAt: string;
  clearTimeSeconds: number;
  rawDps?: number | null;
  adjustDps?: number | null;
  finalDps?: number | null;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="rounded-xl p-3 text-left flex flex-col h-full hover:border-[var(--brown)] transition-colors"
      style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
    >
      <div className="flex items-center justify-between gap-1 mb-1">
        <span
          className="text-[15px] font-bold tabular-nums"
          style={{ color: rank <= 3 ? 'var(--brown)' : 'var(--text-muted)' }}
        >
          {rank}
        </span>
        <span className="text-[15px] font-semibold tabular-nums" style={{ color: 'var(--brown)' }}>
          {clearTimeSeconds}초
        </span>
      </div>
      <p className="text-[15px] font-medium truncate" style={{ color: 'var(--text)' }} title={authorNickname}>
        {authorNickname}
      </p>
      <p className="text-[13px] mt-0.5 truncate" style={{ color: 'var(--text-muted)' }}>
        {relativeTime(recordedAt)}
      </p>
      <DpsBreakdown
        layout="stack"
        stackTextClass="text-[13px]"
        className="mt-1.5"
        rawDps={rawDps}
        adjustDps={adjustDps}
        finalDps={finalDps}
      />
    </button>
  );
}

function ErrorBox({ message }: { message: string }) {
  return (
    <div
      style={{ background: 'var(--card)', border: '1px solid #C24A4A', color: '#C24A4A' }}
      className="rounded-xl p-6 text-center text-sm"
    >
      {message}
    </div>
  );
}

export default function ClearTimePage() {
  return (
    <Suspense fallback={
      <div className="py-12 text-center text-sm" style={{ color: 'var(--text-muted)' }}>
        사냥 허브 불러오는 중...
      </div>
    }>
      <ClearTimePageInner />
    </Suspense>
  );
}
