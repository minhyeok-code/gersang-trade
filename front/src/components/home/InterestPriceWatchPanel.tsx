'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import {
  api,
  getServer,
  getToken,
  parseApiErrorBody,
  type PriceWatchTarget,
  type RitualDto,
  type SetDetailDto,
  type SetSummaryDto,
} from '@/lib/api';
import {
  applyBundleKindToPieces,
  buildRitualMarkOptions,
  initSetPieces,
  type RitualCountOption,
  type RitualMarkOption,
  type SetBundleKind,
  type SetPieceState,
} from '@/lib/setTitle';
import SetPieceConfigurator from '@/components/value-test/SetPieceConfigurator';
import SearchBar from '@/components/common/SearchBar';
import { formatPrice } from '@/lib/formatPrice';
import { BarChart2, Plus, RefreshCw, TrendingUp, X } from 'lucide-react';

const WATCH_LIMIT = 5;
const PRICE_SLOT_COUNT = 5;

function parseWatchlistError(err: unknown): string {
  const body = parseApiErrorBody(err);
  if (body.errorCode === 'WATCH_LIMIT_EXCEEDED') {
    return `관심목록은 최대 ${body.max ?? WATCH_LIMIT}개까지 등록할 수 있습니다.`;
  }
  if (body.errorCode === 'DUPLICATE_WATCH_ITEM') {
    return '이미 등록된 관심 매물입니다.';
  }
  return body.message ?? '요청에 실패했습니다.';
}

interface AddWatchModalProps {
  open: boolean;
  onClose: () => void;
  onAdded: () => void;
  atLimit: boolean;
}

function AddWatchModal({ open, onClose, onAdded, atLimit }: AddWatchModalProps) {
  const [mode, setMode] = useState<'ITEM' | 'SET'>('ITEM');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  // ITEM
  const [itemId, setItemId] = useState<number | null>(null);
  const [itemName, setItemName] = useState('');
  const [itemType, setItemType] = useState<string>('');
  const [itemRitualOptions, setItemRitualOptions] = useState<RitualMarkOption[]>([]);
  const [itemRitualMark, setItemRitualMark] = useState<string | null>(null);

  // SET
  const [setQuery, setSetQuery] = useState('');
  const [setResults, setSetResults] = useState<SetSummaryDto[]>([]);
  const [setDropOpen, setSetDropOpen] = useState(false);
  const [selectedSet, setSelectedSet] = useState<SetDetailDto | null>(null);
  const [pieces, setPieces] = useState<SetPieceState[]>([]);
  const [uniqueRituals, setUniqueRituals] = useState<RitualMarkOption[]>([]);
  const [bundleKind, setBundleKind] = useState<SetBundleKind>('FULL');
  const [ritualCount, setRitualCount] = useState<RitualCountOption>(0);
  const [ritualMark, setRitualMark] = useState<RitualMarkOption | null>(null);

  const setSearchRef = useRef<HTMLDivElement>(null);

  function resetForm() {
    setError('');
    setItemId(null);
    setItemName('');
    setItemType('');
    setItemRitualOptions([]);
    setItemRitualMark(null);
    setSetQuery('');
    setSetResults([]);
    setSelectedSet(null);
    setPieces([]);
    setUniqueRituals([]);
    setBundleKind('FULL');
    setRitualCount(0);
    setRitualMark(null);
  }

  useEffect(() => {
    if (!open) return;
    resetForm();
    setMode('ITEM');
  }, [open]);

  useEffect(() => {
    if (!itemId || itemType === 'MATERIAL') {
      setItemRitualOptions([]);
      setItemRitualMark(null);
      return;
    }
    api.getItemRituals(itemId)
      .then((rituals) => setItemRitualOptions(buildRitualMarkOptions([rituals])))
      .catch(() => setItemRitualOptions([]));
  }, [itemId, itemType]);

  useEffect(() => {
    if (!setQuery.trim() || (selectedSet && setQuery === selectedSet.name)) {
      setSetResults([]);
      return;
    }
    const t = setTimeout(() => {
      api.getSets(setQuery)
        .then((res) => setSetResults(res.content ?? []))
        .catch(() => setSetResults([]));
    }, 250);
    return () => clearTimeout(t);
  }, [setQuery, selectedSet]);

  useEffect(() => {
    function close(e: MouseEvent) {
      if (setSearchRef.current && !setSearchRef.current.contains(e.target as Node)) {
        setSetDropOpen(false);
      }
    }
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  async function selectSet(s: SetSummaryDto) {
    setSetQuery(s.name);
    setSetDropOpen(false);
    setRitualMark(null);
    setRitualCount(0);
    setBundleKind('FULL');
    try {
      const detail = await api.getSet(s.id);
      setSelectedSet(detail);
      const perPieceRituals = await Promise.all(
        detail.pieces.map((p) => api.getItemRituals(p.itemId).catch(() => [] as RitualDto[])),
      );
      const ritualMap = new Map(detail.pieces.map((p, i) => [p.itemId, perPieceRituals[i].length > 0]));
      const initial = initSetPieces(detail.pieces, ritualMap);
      setPieces(applyBundleKindToPieces(initial, 'FULL', 0));
      setUniqueRituals(buildRitualMarkOptions(perPieceRituals));
    } catch {
      setError('세트 정보를 불러오지 못했습니다.');
    }
  }

  async function handleSubmit() {
    if (atLimit) return;
    setError('');
    setSubmitting(true);
    try {
      if (mode === 'ITEM') {
        if (!itemId) {
          setError('아이템을 선택해주세요.');
          return;
        }
        await api.addWatchTarget({
          targetType: 'ITEM',
          itemId,
          ritualMark: itemRitualMark,
        });
      } else {
        if (!selectedSet) {
          setError('세트를 선택해주세요.');
          return;
        }
        // 반지 단독(BANSSANG)만 주술 불가 — 풀반쌍(FULL_BANSSANG)은 주술·피스 수 구분 필요
        const isRingOnlyBanssang = bundleKind === 'BANSSANG';
        if (!isRingOnlyBanssang && ritualMark && ritualCount === 0) {
          setError('주술을 선택했으면 주술 피스 수(2·3·5)도 선택해주세요.');
          return;
        }
        await api.addWatchTarget({
          targetType: 'SET',
          setId: selectedSet.id,
          composition: bundleKind,
          ritualCount: isRingOnlyBanssang ? 0 : ritualCount,
          ritualMark: isRingOnlyBanssang ? null : ritualCount > 0 ? ritualMark?.label ?? null : null,
        });
      }
      onAdded();
      onClose();
    } catch (err) {
      setError(parseWatchlistError(err));
    } finally {
      setSubmitting(false);
    }
  }

  function handleItemSearch(_q: string, id: number | null, name: string, type?: string) {
    setItemId(id);
    setItemName(name);
    setItemType(type ?? '');
    setItemRitualMark(null);
  }

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(0,0,0,0.45)' }}
      onClick={onClose}
    >
      <div
        className="w-full max-w-2xl min-h-[75vh] max-h-[92vh] rounded-xl p-6 shadow-lg flex flex-col overflow-hidden"
        style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold" style={{ color: 'var(--text)' }}>관심 매물 등록</h3>
          <button type="button" onClick={onClose} className="p-1 rounded hover:opacity-70" aria-label="닫기">
            <X style={{ width: 18, height: 18, color: 'var(--text-muted)' }} />
          </button>
        </div>

        <div className="flex gap-2 mb-4 shrink-0">
          {(['ITEM', 'SET'] as const).map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => setMode(t)}
              className="flex-1 py-2 text-xs font-medium rounded transition-colors"
              style={{
                background: mode === t ? 'var(--brown)' : 'var(--bg)',
                color: mode === t ? 'var(--beige)' : 'var(--text-muted)',
                border: `1px solid ${mode === t ? 'var(--brown)' : 'var(--border)'}`,
              }}
            >
              {t === 'ITEM' ? '단품·재료' : '세트'}
            </button>
          ))}
        </div>

        <div className="flex-1 min-h-0 overflow-y-auto pr-1">
        {mode === 'ITEM' ? (
          <div className="space-y-3">
            <SearchBar
              size="sm"
              placeholder="아이템명 검색..."
              onSearch={handleItemSearch}
            />
            {itemId && itemType !== 'MATERIAL' && (
              <div>
                <label className="text-xs block mb-2" style={{ color: 'var(--text-muted)' }}>주술</label>
                <div className="flex flex-wrap gap-x-4 gap-y-2">
                  <label style={{ display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer', fontSize: '0.875rem' }}>
                    <input
                      type="radio"
                      name="watch-ritual"
                      checked={itemRitualMark === null}
                      onChange={() => setItemRitualMark(null)}
                      style={{ accentColor: 'var(--brown)' }}
                    />
                    <span style={{ color: 'var(--text)' }}>없음</span>
                  </label>
                  {itemRitualOptions.map((o) => (
                    <label key={o.label} style={{ display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer', fontSize: '0.875rem' }}>
                      <input
                        type="radio"
                        name="watch-ritual"
                        checked={itemRitualMark === o.label}
                        onChange={() => setItemRitualMark(o.label)}
                        style={{ accentColor: 'var(--brown)' }}
                      />
                      <span style={{ color: 'var(--text)' }}>{o.label}</span>
                    </label>
                  ))}
                </div>
                {itemRitualOptions.length === 0 && (
                  <p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>이 아이템에는 적용 가능한 주술이 없습니다.</p>
                )}
              </div>
            )}
            {itemId && (
              <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
                선택: {itemName}
              </p>
            )}
          </div>
        ) : (
          <div className="space-y-3">
            <div ref={setSearchRef} className="relative">
              <input
                value={setQuery}
                onChange={(e) => {
                  setSetQuery(e.target.value);
                  setSelectedSet(null);
                  setSetDropOpen(true);
                }}
                onFocus={() => setSetResults.length > 0 && setSetDropOpen(true)}
                placeholder="세트명 검색..."
                className="w-full text-sm rounded px-3 py-2"
                style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
              />
              {setDropOpen && setResults.length > 0 && (
                <ul
                  className="absolute z-10 w-full mt-1 rounded shadow-lg max-h-40 overflow-y-auto"
                  style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
                >
                  {setResults.map((s) => (
                    <li key={s.id}>
                      <button
                        type="button"
                        className="w-full text-left px-3 py-2 text-sm hover:opacity-80"
                        style={{ color: 'var(--text)' }}
                        onClick={() => selectSet(s)}
                      >
                        {s.name}
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
            {selectedSet && (
              <SetPieceConfigurator
                setName={selectedSet.name}
                pieces={pieces}
                uniqueRituals={uniqueRituals}
                bundleKind={bundleKind}
                ritualCount={ritualCount}
                ritualMark={ritualMark}
                onBundleKindChange={setBundleKind}
                onRitualCountChange={setRitualCount}
                onRitualMarkChange={setRitualMark}
                onPiecesChange={setPieces}
              />
            )}
          </div>
        )}

        {error && (
          <p className="text-xs mt-3" style={{ color: '#C24A4A' }}>{error}</p>
        )}
        </div>

        <div className="flex gap-2 mt-5 shrink-0 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 py-2 text-sm rounded"
            style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={submitting || atLimit}
            className="flex-1 py-2 text-sm font-medium rounded disabled:opacity-50"
            style={{ background: 'var(--brown)', color: 'var(--beige)' }}
          >
            {submitting ? '등록 중...' : '등록'}
          </button>
        </div>
      </div>
    </div>
  );
}

interface PriceSlot {
  price: number | null;
}

interface PriceTableRowData {
  label: string;
  avg: number | null;
  count: number;
  slots: PriceSlot[];
  showAvg: boolean;
}

function buildPriceSlots(
  items: Array<{ price: number }>,
  limit = PRICE_SLOT_COUNT,
): PriceSlot[] {
  const slots: PriceSlot[] = items.slice(0, limit).map((item) => ({ price: item.price }));
  while (slots.length < limit) {
    slots.push({ price: null });
  }
  return slots;
}

function buildPriceRows(target: PriceWatchTarget): PriceTableRowData[] {
  return [
    {
      label: '판매',
      avg: target.sell.avgPrice,
      count: target.sell.count,
      showAvg: true,
      slots: buildPriceSlots(target.sell.listings.map((l) => ({ price: l.price }))),
    },
    {
      label: '구매',
      avg: target.buy.avgPrice,
      count: target.buy.count,
      showAvg: true,
      slots: buildPriceSlots(target.buy.listings.map((l) => ({ price: l.offeredPrice }))),
    },
    {
      label: '거래완료',
      avg: null,
      count: target.completed.count,
      showAvg: false,
      slots: buildPriceSlots(target.completed.trades.map((t) => ({ price: t.confirmedPrice }))),
    },
  ];
}

function PriceTable({ target }: { target: PriceWatchTarget }) {
  const rows = buildPriceRows(target);

  const thStyle = { color: 'var(--text-muted)', borderColor: 'var(--border)' } as const;
  const tdStyle = { color: 'var(--text)', borderColor: 'var(--border)' } as const;

  return (
    <div className="overflow-x-auto rounded-lg" style={{ border: '1px solid var(--border)' }}>
      <table className="w-full min-w-[32rem] text-xs border-collapse">
        <thead>
          <tr style={{ background: 'var(--bg)' }}>
            <th className="px-3 py-2 text-left font-medium border-b w-20" style={thStyle}>구분</th>
            <th className="px-2 py-2 text-right font-medium border-b w-24" style={thStyle}>평균</th>
            <th className="px-2 py-2 text-center font-medium border-b w-12" style={thStyle}>건수</th>
            {Array.from({ length: PRICE_SLOT_COUNT }, (_, i) => (
              <th key={i} className="px-2 py-2 text-center font-medium border-b" style={thStyle}>
                #{i + 1}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.label}>
              <td className="px-3 py-2.5 font-medium border-b whitespace-nowrap" style={tdStyle}>{row.label}</td>
              <td
                className="px-2 py-2.5 text-right font-semibold border-b whitespace-nowrap"
                style={{ ...tdStyle, color: row.showAvg && row.count > 0 ? 'var(--brown)' : 'var(--text-muted)' }}
              >
                {row.showAvg ? formatPrice(row.avg) : '—'}
              </td>
              <td className="px-2 py-2.5 text-center border-b" style={{ color: 'var(--text-muted)', borderColor: 'var(--border)' }}>
                {row.count}
              </td>
              {row.slots.map((slot, i) => (
                <td key={i} className="px-2 py-2.5 text-center border-b" style={tdStyle}>
                  {slot.price != null ? (
                    <span className="font-semibold" style={{ color: 'var(--brown)' }}>{formatPrice(slot.price)}</span>
                  ) : (
                    <span style={{ color: 'var(--text-muted)' }}>—</span>
                  )}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function InterestPriceWatchPanel() {
  const [targets, setTargets] = useState<PriceWatchTarget[]>([]);
  const [loading, setLoading] = useState(true);
  const [serverName, setServerName] = useState<string | null>(null);
  const [noServer, setNoServer] = useState(false);
  const [addOpen, setAddOpen] = useState(false);
  const [removingId, setRemovingId] = useState<number | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [selectedEntryId, setSelectedEntryId] = useState<number | null>(null);

  const load = useCallback(async () => {
    const serverId = Number(getServer());
    if (!serverId) {
      setNoServer(true);
      setTargets([]);
      setServerName(null);
      setLoading(false);
      return;
    }
    setNoServer(false);
    setLoading(true);
    try {
      const res = await api.getPriceWatch(serverId);
      setTargets(res.targets);
      setServerName(res.serverName);
      // 첫 로드 또는 선택 항목이 사라진 경우 첫 번째 항목 자동 선택
      setSelectedEntryId((prev) => {
        const ids = res.targets.map((t) => t.entryId);
        if (prev !== null && ids.includes(prev)) return prev;
        return ids[0] ?? null;
      });
    } catch {
      setTargets([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!getToken()) {
      setTargets([]);
      setLoading(false);
      return;
    }
    load();
    const onServer = () => load();
    const onAuth = () => {
      if (getToken()) load();
      else {
        setTargets([]);
        setLoading(false);
      }
    };
    const onVisible = () => {
      if (document.visibilityState === 'visible' && getToken()) load();
    };
    window.addEventListener('server-changed', onServer);
    window.addEventListener('auth-changed', onAuth);
    document.addEventListener('visibilitychange', onVisible);
    return () => {
      window.removeEventListener('server-changed', onServer);
      window.removeEventListener('auth-changed', onAuth);
      document.removeEventListener('visibilitychange', onVisible);
    };
  }, [load]);

  async function handleRefresh() {
    setRefreshing(true);
    try {
      await load();
    } finally {
      setRefreshing(false);
    }
  }

  async function handleRemove(entryId: number) {
    setRemovingId(entryId);
    try {
      await api.removeWatchTarget(entryId);
      setTargets((prev) => {
        const next = prev.filter((t) => t.entryId !== entryId);
        // 삭제된 항목이 선택 중이었으면 첫 번째 항목으로 이동
        setSelectedEntryId((sel) => {
          if (sel !== entryId) return sel;
          return next[0]?.entryId ?? null;
        });
        return next;
      });
    } catch {
      await load();
    } finally {
      setRemovingId(null);
    }
  }

  const atLimit = targets.length >= WATCH_LIMIT;

  return (
    <div
      style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
      className="rounded-xl p-5"
    >
      <div className="flex items-center justify-between gap-2 mb-4">
        <div className="flex items-center gap-2 min-w-0">
          <TrendingUp style={{ color: 'var(--brown)', width: 18, height: 18 }} className="shrink-0" />
          <h2 className="font-semibold truncate" style={{ color: 'var(--text)' }}>관심 아이템 시세</h2>
          {serverName && (
            <span className="text-xs shrink-0" style={{ color: 'var(--text-muted)' }}>
              · {serverName}
            </span>
          )}
        </div>
        <div className="flex items-center gap-1.5 shrink-0">
          <button
            type="button"
            onClick={handleRefresh}
            disabled={loading || refreshing || noServer || !getToken()}
            className="p-1.5 rounded disabled:opacity-40 hover:opacity-70"
            aria-label="시세 새로고침"
            title="시세 새로고침"
          >
            <RefreshCw
              style={{ width: 16, height: 16, color: 'var(--text-muted)' }}
              className={refreshing ? 'animate-spin' : undefined}
            />
          </button>
          <button
            type="button"
            onClick={() => setAddOpen(true)}
            disabled={atLimit || noServer}
            className="flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium rounded disabled:opacity-40"
            style={{ background: 'var(--brown)', color: 'var(--beige)' }}
            title={atLimit ? `최대 ${WATCH_LIMIT}개` : noServer ? '서버를 선택하세요' : '관심 매물 추가'}
          >
            <Plus style={{ width: 14, height: 14 }} />
            추가
          </button>
        </div>
      </div>

      {atLimit && (
        <p className="text-xs mb-3" style={{ color: 'var(--text-muted)' }}>
          관심목록 {targets.length}/{WATCH_LIMIT} — 삭제 후 추가할 수 있습니다.
        </p>
      )}

      {noServer ? (
        <div className="text-center py-8" style={{ color: 'var(--text-muted)' }}>
          <BarChart2 style={{ width: 36, height: 36, margin: '0 auto 8px', opacity: 0.4 }} />
          <p className="text-sm">상단에서 서버를 선택하면 시세가 표시됩니다</p>
        </div>
      ) : loading ? (
        <div className="space-y-2">
          {[1, 2, 3].map((i) => (
            <div key={i} style={{ background: 'var(--bg)' }} className="h-36 rounded animate-pulse" />
          ))}
        </div>
      ) : targets.length === 0 ? (
        <div className="text-center py-8" style={{ color: 'var(--text-muted)' }}>
          <BarChart2 style={{ width: 36, height: 36, margin: '0 auto 8px', opacity: 0.4 }} />
          <p className="text-sm">등록된 관심 매물이 없습니다</p>
          <button
            type="button"
            onClick={() => setAddOpen(true)}
            className="text-xs mt-2 hover:underline"
            style={{ color: 'var(--brown)' }}
          >
            + 관심 매물 추가하기
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {/* 관심 매물 선택 — 선택 항목은 전체 표기, 나머지는 한 줄 말줄임 */}
          <div className="space-y-1.5" role="radiogroup" aria-label="관심 매물 선택">
            {targets.map((target) => {
              const selected = selectedEntryId === target.entryId;
              return (
                <label
                  key={target.entryId}
                  className="flex items-start gap-2.5 rounded-lg px-3 py-2.5 cursor-pointer transition-colors"
                  style={{
                    border: `1px solid ${selected ? 'var(--brown)' : 'var(--border)'}`,
                    background: selected ? 'var(--bg)' : 'transparent',
                  }}
                >
                  <input
                    type="radio"
                    name="watch-target"
                    checked={selected}
                    onChange={() => setSelectedEntryId(target.entryId)}
                    className="mt-0.5 shrink-0"
                    style={{ accentColor: 'var(--brown)' }}
                  />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start gap-2">
                      <span
                        className="shrink-0 text-[0.65rem] px-1.5 py-0.5 rounded"
                        style={{
                          background: selected ? 'var(--brown)' : 'var(--border)',
                          color: selected ? 'var(--beige)' : 'var(--text-muted)',
                        }}
                      >
                        {target.targetType === 'SET' ? '세트' : '단품'}
                      </span>
                      <span
                        className={selected ? 'text-sm font-medium break-words leading-snug' : 'text-sm truncate'}
                        style={{ color: selected ? 'var(--text)' : 'var(--text-muted)' }}
                        title={!selected ? target.displayLabel : undefined}
                      >
                        {target.displayLabel}
                      </span>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={(e) => { e.preventDefault(); handleRemove(target.entryId); }}
                    disabled={removingId === target.entryId}
                    className="shrink-0 p-0.5 rounded hover:opacity-70 disabled:opacity-40"
                    style={{ color: 'var(--text-muted)', lineHeight: 1 }}
                    aria-label="삭제"
                  >
                    <X style={{ width: 14, height: 14 }} />
                  </button>
                </label>
              );
            })}
          </div>

          {/* 선택된 아이템의 가격 표 */}
          {(() => {
            const selected = targets.find((t) => t.entryId === selectedEntryId);
            return selected ? <PriceTable target={selected} /> : null;
          })()}
        </div>
      )}

      <AddWatchModal
        open={addOpen}
        onClose={() => setAddOpen(false)}
        onAdded={load}
        atLimit={atLimit}
      />
    </div>
  );
}
