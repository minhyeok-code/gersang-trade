'use client';

import { useEffect, useRef, useState } from 'react';
import { X } from 'lucide-react';
import {
  api,
  getServer,
  type ItemSearchResult,
  type ServerDto,
  type SetDetailDto,
  type SetSummaryDto,
} from '@/lib/api';
import SetPieceConfigurator from '@/components/value-test/SetPieceConfigurator';
import {
  type RitualCountOption,
  type RitualMarkOption,
  type SetBundleKind,
  type SetPieceState,
  validateSetBundleSelection,
} from '@/lib/setTitle';
import { fetchSingleItemRituals, fetchSetRituals } from '@/lib/itemRituals';
import { formatPrice } from '@/lib/formatPrice';

const RADIO_BASE = {
  display: 'flex',
  alignItems: 'center',
  gap: '6px',
  cursor: 'pointer',
  fontSize: '0.875rem',
} as const;

interface CreateListingModalProps {
  onClose: () => void;
  onCreated: () => void;
}

type ListingMode = 'SELL' | 'BUY';
type BundleMode = 'SINGLE' | 'SET';

const EOK = 100_000_000;

function parseEokPrice(value: string) {
  const amount = Number(value);
  if (!Number.isFinite(amount) || amount <= 0) return 0;
  return Math.round(amount * EOK);
}

export default function CreateListingModal({ onClose, onCreated }: CreateListingModalProps) {
  const [listingMode, setListingMode] = useState<ListingMode>('SELL');
  const [bundleMode, setBundleMode] = useState<BundleMode>('SINGLE');
  const [servers, setServers] = useState<ServerDto[]>([]);
  const [server, setServer] = useState('');
  const [price, setPrice] = useState('');
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  // 단품 상태
  const [itemQuery, setItemQuery] = useState('');
  const [itemResults, setItemResults] = useState<ItemSearchResult[]>([]);
  const [selectedItem, setSelectedItem] = useState<ItemSearchResult | null>(null);
  const [itemDropOpen, setItemDropOpen] = useState(false);
  const itemSearchRef = useRef<HTMLDivElement>(null);
  const [singleRitualOptions, setSingleRitualOptions] = useState<RitualMarkOption[]>([]);
  const [singleRitualMark, setSingleRitualMark] = useState<RitualMarkOption | null>(null);

  // 세트 상태
  const [setQuery, setSetQuery] = useState('');
  const [setResults, setSetResults] = useState<SetSummaryDto[]>([]);
  const [selectedSet, setSelectedSet] = useState<SetDetailDto | null>(null);
  const [setDropOpen, setSetDropOpen] = useState(false);
  const [pieces, setPieces] = useState<SetPieceState[]>([]);
  const [uniqueRituals, setUniqueRituals] = useState<RitualMarkOption[]>([]);
  const [ritualMark, setRitualMark] = useState<RitualMarkOption | null>(null);
  const [bundleKind, setBundleKind] = useState<SetBundleKind>('FULL');
  const [ritualCount, setRitualCount] = useState<RitualCountOption>(0);
  const setSearchRef = useRef<HTMLDivElement>(null);

  const priceInJeon = parseEokPrice(price);
  const isSetMode = bundleMode === 'SET';

  // 서버 초기 로드
  useEffect(() => {
    async function load() {
      try {
        const [list, me] = await Promise.all([api.getServers(), api.getMe().catch(() => null)]);
        setServers(list);
        const savedId = getServer();
        const fromProfile = list.find((s) =>
          (me?.serverId != null && s.serverId === me.serverId) ||
          (me?.serverName != null && s.name === me.serverName) ||
          (me?.server != null && s.name === me.server)
        );
        const fromLocal = list.find((s) => String(s.serverId) === savedId);
        setServer(fromProfile?.name ?? fromLocal?.name ?? list[0]?.name ?? '');
      } catch {
        setServers([]);
      }
    }
    load();
  }, []);

  // 드롭다운 외부 클릭 닫기
  useEffect(() => {
    function close(e: MouseEvent) {
      if (itemSearchRef.current && !itemSearchRef.current.contains(e.target as Node)) setItemDropOpen(false);
      if (setSearchRef.current && !setSearchRef.current.contains(e.target as Node)) setSetDropOpen(false);
    }
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  // 단품 아이템 검색
  useEffect(() => {
    if (!itemQuery.trim() || selectedItem) { setItemResults([]); return; }
    const t = setTimeout(async () => {
      try {
        const results = await api.searchItems(itemQuery, { limit: 8 });
        setItemResults(results);
        setItemDropOpen(results.length > 0);
      } catch {
        setItemResults([]);
      }
    }, 250);
    return () => clearTimeout(t);
  }, [itemQuery, selectedItem]);

  // 단품 장비 선택 시 주술 목록 로드
  useEffect(() => {
    setSingleRitualMark(null);
    setSingleRitualOptions([]);
    if (!selectedItem || selectedItem.type !== 'EQUIPMENT') return;
    fetchSingleItemRituals(selectedItem.id).then(setSingleRitualOptions);
  }, [selectedItem]);

  // 세트명 검색
  useEffect(() => {
    if (!setQuery.trim() || selectedSet) { setSetResults([]); return; }
    const t = setTimeout(async () => {
      try {
        const res = await api.getSets(setQuery);
        setSetResults(res.content ?? []);
        setSetDropOpen((res.content ?? []).length > 0);
      } catch {
        setSetResults([]);
      }
    }, 250);
    return () => clearTimeout(t);
  }, [setQuery, selectedSet]);

  // 세트 선택 시 피스 초기화 + 주술 일괄 조회
  useEffect(() => {
    if (!selectedSet) {
      setPieces([]);
      setUniqueRituals([]);
      setRitualMark(null);
      setBundleKind('FULL');
      setRitualCount(0);
      return;
    }
    setRitualMark(null);
    setBundleKind('FULL');
    setRitualCount(0);
    fetchSetRituals(selectedSet.pieces).then(({ initialPieces, uniqueRituals }) => {
      setPieces(initialPieces);
      setUniqueRituals(uniqueRituals);
    });
  }, [selectedSet]);

  // 모드 전환 시 상태 초기화
  useEffect(() => {
    if (isSetMode) {
      // 단품 주술 상태는 유지 — 단품으로 복귀 시 재사용
    } else {
      setSelectedSet(null);
      setSetQuery('');
      setUniqueRituals([]);
      setRitualMark(null);
      setBundleKind('FULL');
      setRitualCount(0);
      // 단품으로 복귀 시 selectedItem이 있는데 주술 옵션이 없으면 재로드
      if (selectedItem?.type === 'EQUIPMENT') {
        fetchSingleItemRituals(selectedItem.id).then(setSingleRitualOptions);
      }
    }
  }, [isSetMode]); // eslint-disable-line react-hooks/exhaustive-deps

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    if (!server) { setError('서버를 선택해주세요.'); return; }
    if (priceInJeon <= 0) { setError('가격을 입력해주세요.'); return; }

    setSubmitting(true);
    try {
      if (isSetMode) {
        // 세트 등록 (판매/구매 공통)
        if (!selectedSet || pieces.length === 0) { setError('세트를 선택해주세요.'); setSubmitting(false); return; }
        const validationError = validateSetBundleSelection(bundleKind, ritualCount, ritualMark, pieces);
        if (validationError) { setError(validationError); setSubmitting(false); return; }
        const includedPieces = pieces.filter((p) => p.included);

        if (listingMode === 'SELL') {
          await api.createListing({
            server,
            price: priceInJeon,
            note: note.trim() || null,
            bundles: [{
              bundleType: 'EQUIPMENT_SET',
              titleOverride: null,
              lines: includedPieces.map((p, i) => ({
                itemId: p.itemId,
                quantity: 1,
                sortOrder: i,
                equipmentDetail: {
                  enhanceLevel: 0,
                  hasRitual: p.hasRitual && ritualMark != null,
                  rituals: (p.hasRitual && ritualMark) ? [{ ritualId: ritualMark.ritualId, outcome: ritualMark.outcome }] : [],
                },
              })),
            }],
          });
        } else {
          await api.createWanted({
            server,
            offeredPrice: priceInJeon,
            note: note.trim() || null,
            setName: selectedSet.name,
            items: includedPieces.map((p, i) => ({
              itemId: p.itemId,
              quantity: 1,
              sortOrder: i,
              equipmentCondition: {
                minEnhanceLevel: null,
                hasRitual: p.hasRitual && ritualMark != null,
                ritualConditions: (p.hasRitual && ritualMark)
                  ? [{ ritualId: ritualMark.ritualId, preferredOutcome: ritualMark.outcome }]
                  : [],
              },
            })),
          });
        }
      } else if (listingMode === 'BUY') {
        if (!selectedItem) { setError('아이템을 선택해주세요.'); setSubmitting(false); return; }
        await api.createWanted({
          server,
          offeredPrice: priceInJeon,
          note: note.trim() || null,
          items: [{
            itemId: selectedItem.id,
            quantity: 1,
            sortOrder: 0,
            equipmentCondition: selectedItem.type === 'EQUIPMENT'
              ? {
                  minEnhanceLevel: null,
                  hasRitual: singleRitualMark !== null,
                  ritualConditions: singleRitualMark
                    ? [{ ritualId: singleRitualMark.ritualId, preferredOutcome: singleRitualMark.outcome }]
                    : [],
                }
              : null,
          }],
        });
      } else {
        if (!selectedItem) { setError('아이템을 선택해주세요.'); setSubmitting(false); return; }
        await api.createListing({
          server,
          price: priceInJeon,
          note: note.trim() || null,
          bundles: [{
            bundleType: selectedItem.type === 'EQUIPMENT' ? 'EQUIPMENT_SINGLE' : 'MATERIAL_BUNDLE',
            // 장비 단품은 주술·아이템명으로 서버가 표시 제목을 계산한다
            titleOverride: selectedItem.type === 'EQUIPMENT' ? null : selectedItem.name,
            lines: [{
              itemId: selectedItem.id,
              quantity: 1,
              sortOrder: 0,
              equipmentDetail: selectedItem.type === 'EQUIPMENT'
                ? {
                    enhanceLevel: null,
                    hasRitual: singleRitualMark !== null,
                    rituals: singleRitualMark
                      ? [{ ritualId: singleRitualMark.ritualId, outcome: singleRitualMark.outcome }]
                      : [],
                  }
                : null,
            }],
          }],
        });
      }
      onCreated();
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-[500] flex items-center justify-center p-4"
      style={{ background: 'rgba(0,0,0,0.65)' }}
      onClick={onClose}
    >
      <form
        onSubmit={handleSubmit}
        className={`w-full ${isSetMode ? 'max-w-2xl' : 'max-w-lg'} rounded-xl overflow-hidden flex flex-col`}
        style={{
          background: 'var(--card)',
          border: '1px solid var(--border)',
          boxShadow: '0 24px 60px rgba(0,0,0,0.3)',
          maxHeight: '90vh',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* 헤더 */}
        <div style={{ borderBottom: '1px solid var(--border)' }} className="flex items-center justify-between px-5 py-3.5 shrink-0">
          <h2 className="font-semibold" style={{ color: 'var(--text)' }}>
            {listingMode === 'SELL' ? '판매 등록' : '구매 희망 등록'}
          </h2>
          <button type="button" onClick={onClose} style={{ color: 'var(--text-muted)' }} className="hover:text-[var(--text)]">
            <X style={{ width: 20, height: 20 }} />
          </button>
        </div>

        {/* 본문 */}
        <div className="overflow-y-auto flex-1 p-5 space-y-4">

          {/* 거래 유형 */}
          <div>
            <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>거래 유형</label>
            <div className="grid grid-cols-2 gap-2">
              {([{ value: 'SELL' as const, label: '판매' }, { value: 'BUY' as const, label: '구매' }]).map((opt) => (
                <button key={opt.value} type="button" onClick={() => setListingMode(opt.value)}
                  className="rounded-lg px-3 py-2 text-sm font-medium transition-colors"
                  style={{
                    background: listingMode === opt.value ? 'var(--brown)' : 'var(--bg)',
                    border: `1px solid ${listingMode === opt.value ? 'var(--brown)' : 'var(--border)'}`,
                    color: listingMode === opt.value ? 'var(--beige)' : 'var(--text-muted)',
                  }}>
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {/* 아이템 유형 */}
          <div>
            <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>아이템 유형</label>
            <div className="grid grid-cols-2 gap-2">
              {([{ value: 'SINGLE' as const, label: '단품' }, { value: 'SET' as const, label: '세트' }]).map((opt) => (
                <button key={opt.value} type="button" onClick={() => setBundleMode(opt.value)}
                  className="rounded-lg px-3 py-2 text-sm font-medium transition-colors"
                  style={{
                    background: bundleMode === opt.value ? 'var(--brown)' : 'var(--bg)',
                    border: `1px solid ${bundleMode === opt.value ? 'var(--brown)' : 'var(--border)'}`,
                    color: bundleMode === opt.value ? 'var(--beige)' : 'var(--text-muted)',
                  }}>
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {/* 서버 */}
          <div>
            <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>서버</label>
            {servers.length > 0 ? (
              <div className="flex flex-wrap gap-1.5">
                {servers.map((s) => (
                  <button key={s.serverId} type="button" onClick={() => setServer(s.name)}
                    className="px-3 py-1.5 rounded text-xs transition-colors"
                    style={{
                      background: server === s.name ? 'var(--brown)' : 'var(--bg)',
                      border: `1px solid ${server === s.name ? 'var(--brown)' : 'var(--border)'}`,
                      color: server === s.name ? 'var(--beige)' : 'var(--text-muted)',
                    }}>
                    {s.name}
                  </button>
                ))}
              </div>
            ) : (
              <input value={server} onChange={(e) => setServer(e.target.value)} required
                className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]"
                style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
              />
            )}
          </div>

          {/* 단품 아이템 선택 */}
          {!isSetMode && (
            <div>
              <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>아이템</label>
              <div className="relative" ref={itemSearchRef}>
                <input
                  value={selectedItem ? selectedItem.name : itemQuery}
                  onChange={(e) => { setSelectedItem(null); setItemQuery(e.target.value); }}
                  onFocus={() => itemResults.length > 0 && setItemDropOpen(true)}
                  placeholder="아이템명을 검색하세요"
                  className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]"
                  style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
                />
                {itemDropOpen && itemResults.length > 0 && (
                  <ul className="absolute top-full left-0 right-0 mt-1 rounded shadow-xl max-h-48 overflow-y-auto"
                    style={{ background: 'var(--card)', border: '1px solid var(--border)', zIndex: 520 }}>
                    {itemResults.map((item) => (
                      <li key={item.id}
                        onMouseDown={() => { setSelectedItem(item); setItemQuery(item.name); setItemDropOpen(false); }}
                        className="px-3 py-2 text-sm cursor-pointer hover:bg-[var(--bg)]"
                        style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ color: 'var(--text)', flex: 1 }}>{item.name}</span>
                        <span className="text-[10px] px-1.5 py-0.5 rounded shrink-0" style={{
                          background: item.type === 'EQUIPMENT' ? 'var(--brown)' : 'var(--border)',
                          color: item.type === 'EQUIPMENT' ? 'var(--beige)' : 'var(--text-muted)',
                        }}>
                          {item.type === 'EQUIPMENT' ? '장비' : '재료'}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          )}

          {/* 단품 장비 주술 선택 */}
          {!isSetMode && selectedItem?.type === 'EQUIPMENT' && (
            <div>
              <label className="block text-xs mb-2 font-medium" style={{ color: 'var(--text-muted)' }}>주술</label>
              <div className="flex flex-wrap gap-x-4 gap-y-2">
                <label style={RADIO_BASE}>
                  <input
                    type="radio"
                    name="single-ritual"
                    checked={singleRitualMark === null}
                    onChange={() => setSingleRitualMark(null)}
                    style={{ accentColor: 'var(--brown)' }}
                  />
                  <span style={{ color: 'var(--text)' }}>없음</span>
                </label>
                {singleRitualOptions.map((r) => (
                  <label key={r.label} style={RADIO_BASE}>
                    <input
                      type="radio"
                      name="single-ritual"
                      checked={singleRitualMark?.label === r.label}
                      onChange={() => setSingleRitualMark(r)}
                      style={{ accentColor: 'var(--brown)' }}
                    />
                    <span style={{ color: 'var(--text)' }}>{r.label}</span>
                  </label>
                ))}
              </div>
            </div>
          )}

          {/* 세트 선택 + 피스 설정 */}
          {isSetMode && (
            <>
              <div>
                <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>세트명</label>
                <div className="relative" ref={setSearchRef}>
                  <input
                    value={selectedSet ? selectedSet.name : setQuery}
                    onChange={(e) => { setSelectedSet(null); setSetQuery(e.target.value); }}
                    onFocus={() => setResults.length > 0 && setSetDropOpen(true)}
                    placeholder="세트명을 검색하세요"
                    className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]"
                    style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
                  />
                  {setDropOpen && setResults.length > 0 && (
                    <ul className="absolute top-full left-0 right-0 mt-1 rounded shadow-xl max-h-48 overflow-y-auto"
                      style={{ background: 'var(--card)', border: '1px solid var(--border)', zIndex: 520 }}>
                      {setResults.map((s) => (
                        <li key={s.id}
                          onMouseDown={async () => {
                            const detail = await api.getSet(s.id);
                            setSelectedSet(detail);
                            setSetQuery(s.name);
                            setSetDropOpen(false);
                          }}
                          className="flex items-center justify-between px-3 py-2 text-sm cursor-pointer hover:bg-[var(--bg)]">
                          <span style={{ color: 'var(--text)' }}>{s.name}</span>
                          <span className="text-xs" style={{ color: 'var(--text-muted)' }}>{s.totalPieces}피스</span>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>

              {selectedSet && pieces.length > 0 && (
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
            </>
          )}

          {/* 가격 */}
          <div>
            <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>
              {listingMode === 'SELL' ? '가격' : '제시 가격'}
            </label>
            <input type="number" step="any" value={price} onChange={(e) => setPrice(e.target.value)} required
              placeholder="억 단위"
              className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]"
              style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
            />
            <p className="text-xs mt-1" style={{ color: priceInJeon > 0 ? 'var(--brown)' : 'var(--text-disabled)' }}>
              {priceInJeon > 0 ? formatPrice(priceInJeon) : '예: 7 입력 시 7억'}
            </p>
          </div>

          {/* 메모 */}
          <div>
            <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>메모</label>
            <textarea value={note} onChange={(e) => setNote(e.target.value)} rows={3} maxLength={500}
              className="w-full rounded px-3 py-2 text-sm resize-none focus:outline-none focus:border-[var(--brown)]"
              style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
            />
          </div>

          {error && <p className="text-sm" style={{ color: 'var(--danger)' }}>{error}</p>}
        </div>

        {/* 푸터 */}
        <div style={{ borderTop: '1px solid var(--border)' }} className="flex justify-end gap-2 px-5 py-3.5 shrink-0">
          <button type="button" onClick={onClose} className="px-4 py-2 rounded-lg text-sm"
            style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}>
            취소
          </button>
          <button type="submit" disabled={submitting} className="px-5 py-2 rounded-lg text-sm font-semibold disabled:opacity-50"
            style={{ background: 'var(--brown)', color: 'var(--beige)' }}>
            {submitting ? '등록 중...' : listingMode === 'SELL' ? '판매 등록' : '구매 등록'}
          </button>
        </div>
      </form>
    </div>
  );
}
