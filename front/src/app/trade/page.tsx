'use client';

import { useState, useEffect, useRef, useCallback, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import SearchBar from '@/components/common/SearchBar';
import { api, getSelectedServerName, type ChatRoomSummaryDto, type ListingDto, type WantedDto, type ServerDto, type PublicUserDto, type SetSummaryDto, type SetDetailDto, type RitualDto } from '@/lib/api';
import { parseApiError } from '@/lib/parseApiError';
import { BarChart2, Plus } from 'lucide-react';
import CreateListingModal from '@/components/trade/CreateListingModal';
import { formatPrice } from '@/lib/formatPrice';
import SetPieceConfigurator from '@/components/value-test/SetPieceConfigurator';
import {
  applyBundleKindToPieces,
  buildRitualMarkOptions,
  buildSetSearchFilterTokens,
  initSetPieces,
  type RitualCountOption,
  type RitualMarkOption,
  type SetBundleKind,
  type SetPieceState,
} from '@/lib/setTitle';

// ══════════ 타입 ══════════

type ListingType = 'SELL' | 'BUY';
type SortKey = 'latest' | 'price_asc' | 'price_desc';
type Tab = 'all' | 'sell' | 'buy';

interface TradeItem {
  id: number;
  listingType: ListingType;
  bundleType?: string;
  displayName: string;
  nickname: string;
  gameNickname?: string;
  accessTime?: string;
  server: string;
  grade?: string;
  price: number;
  description?: string;
  status: string;
  createdAt: string;
  sellerId?: number;
}


interface Filters {
  setName: string;
  selectedItemId: number | null;
  selectedItemName: string;
  ritual: string;
  setPieceMarks: string[]; // 세트 피스별 선택 주술 마크
}

// ══════════ 헬퍼 ══════════

function relativeTime(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const min = Math.floor(diff / 60_000);
  if (min < 1) return '방금';
  if (min < 60) return `${min}분 전`;
  const h = Math.floor(min / 60);
  if (h < 24) return `${h}시간 전`;
  const d = Math.floor(h / 24);
  return d === 1 ? '어제' : `${d}일 전`;
}

function mapListing(l: ListingDto): TradeItem {
  const bundle = l.bundles?.[0];
  const displayName = bundle?.displayName
    ?? bundle?.displayTitle
    ?? `#${l.id}`;
  const sellerName = l.seller?.nickname ?? l.sellerName ?? '알 수 없음';
  return {
    id: l.id,
    listingType: 'SELL',
    bundleType: bundle?.bundleType,
    displayName,
    nickname: sellerName,
    gameNickname: l.seller?.gameNickname ?? l.sellerGameNickname,
    accessTime: l.seller?.gameAccessTime ?? l.sellerGameAccessTime,
    server: l.server,
    grade: l.seller?.grade,
    price: l.price,
    description: l.description ?? l.note,
    status: l.status,
    createdAt: l.createdAt,
    sellerId: l.seller?.id ?? l.sellerId,
  };
}

function mapWanted(w: WantedDto): TradeItem {
  const names = w.itemNames?.length
    ? w.itemNames
    : w.itemName
      ? [w.itemName]
      : [];
  const displayName = w.displayTitle
    ?? (w.setName
      ?? (names.length > 0 ? names.join(', ') : `#${w.id}`));
  const buyerName = w.buyer?.nickname ?? w.buyerName ?? '알 수 없음';
  return {
    id: w.id,
    listingType: 'BUY',
    displayName,
    nickname: buyerName,
    gameNickname: w.buyer?.gameNickname,
    accessTime: w.buyer?.gameAccessTime,
    server: w.server,
    grade: w.buyer?.grade,
    price: w.offeredPrice ?? w.price ?? 0,
    description: w.description ?? w.note,
    status: w.status,
    createdAt: w.createdAt,
    sellerId: w.buyer?.id,
  };
}

// ══════════ 사이드바 헬퍼 ══════════

interface RitualOption {
  label: string;
  value: string;
}

// ══════════ 사이드바 ══════════

function FilterSidebar({ filters, onChange, onItemSelect, onReset, onSetPieceMarksChange, onSetSearch }: {
  filters: Filters;
  onChange: (key: keyof Filters, value: string) => void;
  onItemSelect: (id: number | null, name: string) => void;
  onReset: () => void;
  onSetPieceMarksChange: (marks: string[]) => void;
  onSetSearch: () => void;
}) {
  const [searchMode, setSearchMode] = useState<'SINGLE' | 'SET'>('SINGLE');
  const [rituals, setRituals] = useState<{ displayName: string; successMark: string; greatSuccessMark?: string | null }[]>([]);

  // 세트 검색 상태
  const [setQuery, setSetQuery] = useState(filters.setName);
  const [setResults, setSetResults] = useState<SetSummaryDto[]>([]);
  const [setDropOpen, setSetDropOpen] = useState(false);
  const setSearchRef = useRef<HTMLDivElement>(null);

  // 세트 피스·구성 상태
  const [sidebarSetDetail, setSidebarSetDetail] = useState<SetDetailDto | null>(null);
  const [sidebarPieces, setSidebarPieces] = useState<SetPieceState[]>([]);
  const [uniqueRituals, setUniqueRituals] = useState<RitualMarkOption[]>([]);
  const [sidebarBundleKind, setSidebarBundleKind] = useState<SetBundleKind>('FULL');
  const [sidebarRitualCount, setSidebarRitualCount] = useState<RitualCountOption>(0);
  const [sidebarRitualOpt, setSidebarRitualOpt] = useState<RitualMarkOption | null>(null);

  function switchMode(mode: 'SINGLE' | 'SET') {
    setSearchMode(mode);
    if (mode === 'SET') {
      onItemSelect(null, '');
      onChange('ritual', '없음');
    } else {
      onChange('setName', '');
      setSetQuery('');
      setSidebarSetDetail(null);
      setSidebarPieces([]);
      setUniqueRituals([]);
      setSidebarRitualOpt(null);
      setSidebarBundleKind('FULL');
      setSidebarRitualCount(0);
      onSetPieceMarksChange([]);
    }
  }

  function handleSetSearch() {
    if (!sidebarSetDetail) return;
    const tokens = buildSetSearchFilterTokens(
      sidebarSetDetail.name,
      sidebarBundleKind,
      sidebarRitualCount,
      sidebarRitualCount > 0 ? sidebarRitualOpt?.label ?? null : null,
    );
    onSetPieceMarksChange(tokens);
    onSetSearch();
  }

  async function selectSet(s: SetSummaryDto) {
    setSetQuery(s.name);
    onChange('setName', s.name);
    setSetDropOpen(false);
    onSetPieceMarksChange([]);
    setSidebarRitualOpt(null);
    setSidebarRitualCount(0);
    setSidebarBundleKind('FULL');
    setUniqueRituals([]);
    try {
      const detail = await api.getSet(s.id);
      setSidebarSetDetail(detail);
      const perPieceRituals = await Promise.all(
        detail.pieces.map((p) => api.getItemRituals(p.itemId).catch(() => [] as RitualDto[]))
      );
      const ritualMap = new Map(detail.pieces.map((p, i) => [p.itemId, perPieceRituals[i].length > 0]));
      const initial = initSetPieces(detail.pieces, ritualMap);
      setSidebarPieces(applyBundleKindToPieces(initial, 'FULL', 0));
      setUniqueRituals(buildRitualMarkOptions(perPieceRituals));
    } catch {}
  }

  useEffect(() => {
    if (!filters.selectedItemId) {
      setRituals([]);
      return;
    }
    api.getItemRituals(filters.selectedItemId)
      .then((list) => setRituals(list.map((r) => ({
        displayName: r.displayName,
        successMark: r.successMark ?? r.displayName,
        greatSuccessMark: r.greatSuccessMark,
      }))))
      .catch(() => setRituals([]));
  }, [filters.selectedItemId]);

  // 아이템 변경 시 주술 선택 초기화
  const prevItemIdRef = useRef<number | null>(null);
  useEffect(() => {
    if (prevItemIdRef.current !== filters.selectedItemId) {
      prevItemIdRef.current = filters.selectedItemId;
      onChange('ritual', '없음');
    }
  }, [filters.selectedItemId]);

  // 필터 초기화 시 세트 쿼리도 동기화
  useEffect(() => {
    if (!filters.setName) setSetQuery('');
  }, [filters.setName]);

  // 세트명 debounced 검색
  useEffect(() => {
    if (!setQuery.trim()) { setSetResults([]); return; }
    // 이미 세트가 선택된 상태(setQuery === filters.setName)면 검색 안 함
    if (setQuery === filters.setName) return;
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
  }, [setQuery, filters.setName]);

  // 세트 드롭다운 외부 클릭 닫기
  useEffect(() => {
    function close(e: MouseEvent) {
      if (setSearchRef.current && !setSearchRef.current.contains(e.target as Node)) setSetDropOpen(false);
    }
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  // 순서: 없음, 주술(성공), 주술(대성공), ..., 전체
  const ritualOptions: RitualOption[] = [
    { label: '없음', value: '없음' },
    ...rituals.flatMap((r) => [
      { label: r.successMark || r.displayName, value: r.successMark || r.displayName },
      ...(r.greatSuccessMark ? [{ label: `<${r.greatSuccessMark.replace(/[<>]/g, '')}_${(r.successMark ?? r.displayName).replace(/[<>]/g, '')}>`, value: r.greatSuccessMark }] : []),
    ]),
    ...(rituals.length > 0 ? [{ label: '전체', value: '전체' }] : []),
  ];

  return (
    <aside
      style={{ background: 'var(--card)', borderRight: '1px solid var(--border)', width: 270 }}
      className="shrink-0 overflow-y-auto"
    >
      <div className="p-3 space-y-4">
        {/* 단품/세트 토글 */}
        <div className="grid grid-cols-2 gap-1.5">
          {(['SINGLE', 'SET'] as const).map((mode) => (
            <button
              key={mode}
              onClick={() => switchMode(mode)}
              className="rounded-lg py-1.5 text-xs font-medium transition-colors"
              style={{
                background: searchMode === mode ? 'var(--brown)' : 'var(--bg)',
                border: `1px solid ${searchMode === mode ? 'var(--brown)' : 'var(--border)'}`,
                color: searchMode === mode ? 'var(--beige)' : 'var(--text-muted)',
              }}
            >
              {mode === 'SINGLE' ? '단품' : '세트'}
            </button>
          ))}
        </div>

        {/* 단품 검색 */}
        {searchMode === 'SINGLE' && (
          <div>
            <p style={{ color: 'var(--text-muted)' }} className="text-xs mb-2 font-medium uppercase tracking-wide">아이템 검색</p>
            <SearchBar
              size="sm"
              placeholder="아이템명 입력..."
              initialItemId={filters.selectedItemId}
              initialItemName={filters.selectedItemName}
              showSubmitButton
              onSearch={(_, itemId, itemName) => onItemSelect(itemId, itemName)}
            />
          </div>
        )}

        {/* 세트 검색 */}
        {searchMode === 'SET' && (
          <>
            <div>
              <p style={{ color: 'var(--text-muted)' }} className="text-xs mb-2 font-medium uppercase tracking-wide">세트명</p>
              <div className="relative" ref={setSearchRef}>
                <input
                  value={setQuery}
                  onChange={(e) => {
                    setSetQuery(e.target.value);
                    if (!e.target.value) {
                      onChange('setName', '');
                      setSidebarSetDetail(null);
                      setSidebarPieces([]);
                      onSetPieceMarksChange([]);
                    }
                  }}
                  onFocus={() => setResults.length > 0 && setSetDropOpen(true)}
                  placeholder="예: 지국천왕"
                  style={{
                    background: 'var(--bg)',
                    border: `1px solid ${filters.setName ? 'var(--brown)' : 'var(--border)'}`,
                    color: 'var(--text)',
                  }}
                  className="w-full rounded px-3 py-1.5 text-sm placeholder-[var(--text-disabled)] focus:outline-none focus:border-[var(--brown)]"
                />
                {setDropOpen && setResults.length > 0 && (
                  <ul
                    style={{ background: 'var(--card)', border: '1px solid var(--border)', zIndex: 50 }}
                    className="absolute top-full left-0 right-0 mt-0.5 rounded shadow-xl max-h-40 overflow-y-auto"
                  >
                    {setResults.map((s) => (
                      <li
                        key={s.id}
                        onMouseDown={() => selectSet(s)}
                        className="flex items-center justify-between px-3 py-2 text-sm cursor-pointer hover:bg-[var(--bg)]"
                      >
                        <span style={{ color: 'var(--text)' }}>{s.name}</span>
                        <span className="text-xs" style={{ color: 'var(--text-muted)' }}>{s.totalPieces}피스</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>

            {sidebarSetDetail && sidebarPieces.length > 0 && (
              <SetPieceConfigurator
                setName={sidebarSetDetail.name}
                pieces={sidebarPieces}
                uniqueRituals={uniqueRituals}
                bundleKind={sidebarBundleKind}
                ritualCount={sidebarRitualCount}
                ritualMark={sidebarRitualOpt}
                onBundleKindChange={setSidebarBundleKind}
                onRitualCountChange={setSidebarRitualCount}
                onRitualMarkChange={setSidebarRitualOpt}
                onPiecesChange={setSidebarPieces}
              />
            )}

            {/* 세트 검색 버튼 */}
            {sidebarSetDetail && (
              <button
                onClick={handleSetSearch}
                className="w-full rounded-lg py-2 text-sm font-semibold transition-opacity hover:opacity-90"
                style={{ background: 'var(--brown)', color: 'var(--beige)' }}
              >
                검색
              </button>
            )}
          </>
        )}

        {/* 주술 (단품 모드에서만) */}
        {searchMode === 'SINGLE' && (
          <div>
            <p style={{ color: 'var(--text-muted)' }} className="text-xs mb-2 font-medium uppercase tracking-wide">주술</p>
            <div className="flex flex-wrap gap-1">
              {ritualOptions.map((opt) => (
                <button
                  key={opt.label}
                  onClick={() => onChange('ritual', opt.value)}
                  style={{
                    background: filters.ritual === opt.value ? 'var(--brown)' : 'var(--bg)',
                    border: `1px solid ${filters.ritual === opt.value ? 'var(--brown)' : 'var(--border)'}`,
                    color: filters.ritual === opt.value ? 'var(--beige)' : 'var(--text-muted)',
                  }}
                  className="px-2 py-0.5 text-xs rounded transition-colors"
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>
        )}

        <button
          onClick={() => {
            onReset();
            setSetQuery('');
            setSearchMode('SINGLE');
            setSidebarSetDetail(null);
            setSidebarPieces([]);
            setUniqueRituals([]);
            setSidebarRitualOpt(null);
          }}
          style={{ color: 'var(--text-muted)' }}
          className="w-full text-xs py-1 hover:text-[var(--text)] transition-colors"
        >
          필터 초기화
        </button>
      </div>
    </aside>
  );
}

// ══════════ 거래 카드 ══════════

function TradeCard({ item, showBadge, onClick }: {
  item: TradeItem; showBadge: boolean; onClick: () => void;
}) {
  const isSell = item.listingType === 'SELL';
  return (
    <div
      onClick={onClick}
      style={{
        background: 'var(--card)',
        border: '1px solid var(--border)',
        borderLeft: `3px solid ${isSell ? 'var(--sell-border)' : 'var(--buy-border)'}`,
      }}
      className="rounded-lg p-3 cursor-pointer hover:border-[var(--brown)] hover:shadow-md transition-all"
    >
      <div className="flex items-center justify-between mb-1.5">
        <div className="flex items-center gap-2">
          {showBadge && (
            <span
              style={{
                background: isSell ? 'var(--sell-bg)' : 'var(--buy-bg)',
                color: isSell ? 'var(--sell-text)' : 'var(--buy-text)',
                border: `1px solid ${isSell ? 'var(--sell-border)' : 'var(--buy-border)'}`,
              }}
              className="text-xs px-1.5 py-0.5 rounded font-medium"
            >
              {isSell ? '팝니다' : '삽니다'}
            </span>
          )}
        </div>
        <span style={{ color: 'var(--text-muted)' }} className="text-xs">{item.nickname}</span>
      </div>
      <p className="font-serif text-sm font-semibold mb-1" style={{ color: 'var(--text)' }}>
        {item.displayName}
      </p>
      <p className="font-serif text-base font-bold mb-2" style={{ color: 'var(--brown)' }}>
        {formatPrice(item.price)}
      </p>
      <div className="flex items-center justify-between">
        <p className="text-xs truncate mr-2" style={{ color: 'var(--text-muted)' }}>
          {item.gameNickname ?? ''}{item.accessTime ? `  ${item.accessTime}` : ''}
        </p>
        <p className="text-xs shrink-0" style={{ color: 'var(--text-disabled)' }}>{relativeTime(item.createdAt)}</p>
      </div>
    </div>
  );
}

// ══════════ 목록 ══════════

function TradeList({ items, sort, onSortChange, title, showBadge, onCardClick, loading }: {
  items: TradeItem[];
  sort: SortKey;
  onSortChange: (s: SortKey) => void;
  title: string;
  showBadge: boolean;
  onCardClick: (item: TradeItem) => void;
  loading: boolean;
}) {
  const isSell = title === '팝니다';
  const sorted = [...items].sort((a, b) => {
    if (sort === 'price_asc') return a.price - b.price;
    if (sort === 'price_desc') return b.price - a.price;
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
  });

  return (
    <div className="flex flex-col h-full min-h-0">
      <div
        style={{ borderBottom: '1px solid var(--border)', background: 'var(--card)' }}
        className="flex items-center justify-between px-3 py-2 shrink-0"
      >
        <h2
          className="text-sm font-semibold"
          style={{ color: isSell ? 'var(--sell-text)' : 'var(--buy-text)' }}
        >
          {title}
          <span style={{ color: 'var(--text-disabled)' }} className="font-normal text-xs ml-1.5">
            {items.length}건
          </span>
        </h2>
        <select
          value={sort}
          onChange={(e) => onSortChange(e.target.value as SortKey)}
          style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text-muted)' }}
          className="text-xs rounded px-2 py-1 focus:outline-none"
        >
          <option value="latest">최신순</option>
          <option value="price_asc">낮은가</option>
          <option value="price_desc">높은가</option>
        </select>
      </div>
      <div className="flex-1 overflow-y-auto p-2 space-y-2">
        {loading ? (
          <div className="space-y-2 pt-2">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
                className="h-20 rounded animate-pulse" />
            ))}
          </div>
        ) : sorted.length === 0 ? (
          <p style={{ color: 'var(--text-disabled)' }} className="text-center text-sm py-12">
            등록된 거래가 없습니다
          </p>
        ) : (
          sorted.map((item) => (
            <TradeCard key={item.id} item={item} showBadge={showBadge} onClick={() => onCardClick(item)} />
          ))
        )}
      </div>
    </div>
  );
}

// ══════════ 상세 모달 ══════════

function DetailModal({ item, onClose, onDeleted, onChat, chatLoading }: {
  item: TradeItem; onClose: () => void; onDeleted: () => void; onChat: () => void; chatLoading?: boolean;
}) {
  const isSell = item.listingType === 'SELL';
  const [reporting, setReporting] = useState(false);
  const [sellerProfile, setSellerProfile] = useState<PublicUserDto | null>(null);

  useEffect(() => {
    if (item.sellerId) {
      api.getUser(item.sellerId).then(setSellerProfile).catch(() => {});
    }
  }, [item.sellerId]);

  async function handleReport() {
    setReporting(true);
    try {
      await api.createReport({
        targetType: isSell ? 'LISTING' : 'WANTED',
        targetId: item.id,
        reason: 'USER_REPORT',
      });
      alert('신고가 접수되었습니다.');
      onClose();
    } catch {
      alert('신고 중 오류가 발생했습니다.');
    } finally {
      setReporting(false);
    }
  }

  async function handleDelete() {
    if (!confirm('이 리스팅을 삭제하시겠습니까?')) return;
    try {
      if (isSell) await api.deleteListing(item.id);
      else await api.deleteWanted(item.id);
      onDeleted();
    } catch {
      alert('삭제 중 오류가 발생했습니다.');
    }
  }

  return (
    <div
      className="fixed inset-0 flex items-center justify-center z-[500] p-4"
      style={{ background: 'rgba(0,0,0,0.65)' }}
      onClick={onClose}
    >
      <div
        style={{
          background: 'var(--card)',
          border: '1px solid var(--border)',
          boxShadow: '0 24px 60px rgba(0,0,0,0.3)',
          width: 660,
        }}
        className="rounded-xl max-w-full overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 헤더 */}
        <div style={{ borderBottom: '1px solid var(--border)' }} className="flex items-center justify-between px-5 py-3.5">
          <h2 className="font-semibold" style={{ color: 'var(--text)' }}>거래 상세</h2>
          <button onClick={onClose} style={{ color: 'var(--text-muted)' }} className="text-xl hover:text-[var(--text)] transition-colors">×</button>
        </div>

        {/* 본문 */}
        <div className="grid" style={{ gridTemplateColumns: '170px 1fr' }}>
          {/* 프로필 패널 */}
          <div style={{ borderRight: '1px solid var(--border)' }} className="p-5 space-y-4">
            <div className="text-center space-y-2">
              <div
                style={{ background: 'var(--brown)', color: 'var(--beige)' }}
                className="w-16 h-16 rounded-full flex items-center justify-center mx-auto text-2xl font-bold font-serif"
              >
                {item.nickname.charAt(0)}
              </div>
              <div>
                <p className="font-semibold text-sm" style={{ color: 'var(--text)' }}>{item.nickname}</p>
                {item.grade && (
                  <p
                    className="text-xs mt-0.5 px-1.5 py-0.5 rounded inline-block"
                    style={{ background: 'var(--beige)', color: 'var(--brown)' }}
                  >
                    {item.grade}
                  </p>
                )}
              </div>
            </div>
            <div className="space-y-2 text-xs">
              <div className="flex justify-between">
                <span style={{ color: 'var(--text-muted)' }}>서버</span>
                <span style={{ color: 'var(--text)' }}>{item.server}</span>
              </div>
              {item.gameNickname && (
                <div className="flex justify-between gap-1">
                  <span style={{ color: 'var(--text-muted)' }} className="shrink-0">게임닉</span>
                  <span style={{ color: 'var(--text)' }} className="text-right">{item.gameNickname}</span>
                </div>
              )}
              {item.accessTime && (
                <div className="flex justify-between gap-1">
                  <span style={{ color: 'var(--text-muted)' }} className="shrink-0">접속</span>
                  <span style={{ color: 'var(--text)' }} className="text-right">{item.accessTime}</span>
                </div>
              )}
            </div>
            {sellerProfile && (
              <div
                className="grid grid-cols-3 divide-x text-center"
                style={{ background: 'var(--bg)', border: '1px solid var(--border)', borderRadius: 8, overflow: 'hidden' }}
              >
                <div className="flex flex-col items-center py-2 gap-0.5">
                  <span className="text-[10px]" style={{ color: 'var(--text-muted)' }}>거래량</span>
                  <span className="font-bold text-sm font-serif" style={{ color: 'var(--text)' }}>{sellerProfile.tradeCount ?? 0}</span>
                </div>
                <div className="flex flex-col items-center py-2 gap-0.5">
                  <span className="text-[10px]" style={{ color: 'var(--text-muted)' }}>매너</span>
                  <span className="font-bold text-sm font-serif" style={{ color: (sellerProfile.mannerScore ?? 60) >= 70 ? 'var(--brown)' : (sellerProfile.mannerScore ?? 60) >= 40 ? '#b08030' : 'var(--danger)' }}>
                    {sellerProfile.mannerScore ?? 60}
                  </span>
                </div>
                <div className="flex flex-col items-center py-2 gap-0.5">
                  <span className="text-[10px]" style={{ color: 'var(--text-muted)' }}>등급</span>
                  <span className="font-bold text-sm font-serif" style={{ color: 'var(--brown)' }}>
                    {sellerProfile.grade ?? '-'}
                  </span>
                </div>
              </div>
            )}
          </div>

          {/* 거래 정보 */}
          <div className="p-5 space-y-4">
            <div className="flex items-start gap-3">
              <div
                style={{ background: 'var(--bg)', border: '1px solid var(--border)' }}
                className="w-16 h-16 rounded-lg flex items-center justify-center shrink-0 text-3xl"
              >
                ⚔️
              </div>
              <div className="flex-1 min-w-0">
                <span
                  style={{
                    background: isSell ? 'var(--sell-bg)' : 'var(--buy-bg)',
                    color: isSell ? 'var(--sell-text)' : 'var(--buy-text)',
                    border: `1px solid ${isSell ? 'var(--sell-border)' : 'var(--buy-border)'}`,
                  }}
                  className="text-xs px-1.5 py-0.5 rounded font-medium inline-block mb-1"
                >
                  {isSell ? '팝니다' : '삽니다'}
                </span>
                <p className="font-serif font-bold text-base" style={{ color: 'var(--text)' }}>{item.displayName}</p>
                <p className="font-serif text-2xl font-bold mt-1" style={{ color: 'var(--brown)' }}>
                  {formatPrice(item.price)}
                </p>
              </div>
            </div>

            {item.description && (
              <div style={{ borderTop: '1px solid var(--border)' }} className="pt-3">
                <p style={{ color: 'var(--text-muted)' }} className="text-xs mb-1">메모</p>
                <p className="text-sm" style={{ color: 'var(--text)' }}>{item.description}</p>
              </div>
            )}
            <div style={{ color: 'var(--text-disabled)' }} className="text-xs">
              {relativeTime(item.createdAt)} 등록
            </div>
          </div>
        </div>

        {/* 하단 버튼 */}
        <div style={{ borderTop: '1px solid var(--border)' }} className="flex justify-end gap-2 px-5 py-3.5">
          <button
            onClick={handleReport}
            disabled={reporting}
            style={{ border: '1px solid var(--border)', color: 'var(--danger)' }}
            className="px-4 py-2 rounded-lg text-sm hover:bg-red-50 transition-colors disabled:opacity-50"
          >
            신고
          </button>
          <button
            onClick={handleDelete}
            style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
            className="px-4 py-2 rounded-lg text-sm hover:border-[var(--danger)] hover:text-[var(--danger)] transition-colors"
          >
            삭제
          </button>
          <button
            onClick={onChat}
            disabled={chatLoading}
            style={{ background: 'var(--brown)', color: 'var(--beige)' }}
            className="px-5 py-2 rounded-lg text-sm font-semibold hover:bg-[var(--brown-dark)] transition-colors disabled:opacity-50"
          >
            {chatLoading ? '연결 중…' : '채팅하기'}
          </button>
        </div>
      </div>
    </div>
  );
}


// ══════════ 시세 드로어 ══════════

function PriceDrawer({ onClose }: { onClose: () => void }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<{ id: number; name: string; type: string }[]>([]);
  const [selected, setSelected] = useState<{ id: number; name: string } | null>(null);
  const [days, setDays] = useState(10);
  const [history, setHistory] = useState<{ date: string; avgPrice: number; minPrice: number; maxPrice: number; tradeCount: number }[]>([]);
  const [loading, setLoading] = useState(false);
  const [dropOpen, setDropOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!query.trim()) { setResults([]); return; }
    const t = setTimeout(async () => {
      const res = await api.searchItems(query, { limit: 8 });
      setResults(res);
      setDropOpen(res.length > 0);
    }, 300);
    return () => clearTimeout(t);
  }, [query]);

  useEffect(() => {
    if (!selected) return;
    setLoading(true);
    api.getItemPriceHistory(selected.id, days)
      .then((d) => setHistory(d.records))
      .catch(() => setHistory([]))
      .finally(() => setLoading(false));
  }, [selected, days]);

  const avg = history.length ? Math.round(history.reduce((s, r) => s + r.avgPrice, 0) / history.length) : 0;
  const maxP = history.length ? Math.max(...history.map((r) => r.maxPrice)) : 0;
  const minP = history.length ? Math.min(...history.map((r) => r.minPrice)) : 0;
  const total = history.reduce((s, r) => s + r.tradeCount, 0);

  return (
    <div className="fixed inset-0 z-[500] flex" onClick={onClose} style={{ background: 'rgba(0,0,0,0.5)' }}>
      <div
        style={{ background: 'var(--card)', border: '1px solid var(--border)', marginLeft: 'auto', height: '100%', width: 380 }}
        className="flex flex-col overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ borderBottom: '1px solid var(--border)' }} className="flex items-center justify-between px-5 py-4 shrink-0">
          <div className="flex items-center gap-2">
            <BarChart2 style={{ color: 'var(--brown)', width: 18, height: 18 }} />
            <h2 className="font-semibold" style={{ color: 'var(--text)' }}>시세 조회</h2>
          </div>
          <button onClick={onClose} style={{ color: 'var(--text-muted)' }} className="text-xl">×</button>
        </div>

        <div className="p-5 overflow-y-auto flex-1 space-y-4">
          {/* 아이템 검색 */}
          <div className="relative" ref={ref}>
            <label style={{ color: 'var(--text-muted)' }} className="text-xs font-medium uppercase tracking-wide block mb-1.5">아이템명</label>
            <input value={query} onChange={(e) => setQuery(e.target.value)} onFocus={() => results.length > 0 && setDropOpen(true)}
              placeholder="아이템명을 입력하세요"
              style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
              className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]" />
            {dropOpen && results.length > 0 && (
              <ul style={{ background: 'var(--card)', border: '1px solid var(--border)', zIndex: 10 }}
                className="absolute top-full left-0 right-0 mt-0.5 rounded shadow-xl max-h-40 overflow-y-auto">
                {results.map((r) => (
                  <li key={r.id} onMouseDown={() => { setSelected({ id: r.id, name: r.name }); setQuery(r.name); setDropOpen(false); }}
                    className="px-3 py-2 text-sm cursor-pointer hover:bg-[var(--bg)]" style={{ color: 'var(--text)' }}>
                    {r.name}
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* 기간 */}
          <div>
            <label style={{ color: 'var(--text-muted)' }} className="text-xs font-medium uppercase tracking-wide block mb-1.5">기간</label>
            <div className="flex gap-1">
              {[5, 10, 15].map((d) => (
                <button key={d} onClick={() => setDays(d)}
                  style={{ background: days === d ? 'var(--brown)' : 'var(--bg)', border: `1px solid ${days === d ? 'var(--brown)' : 'var(--border)'}`, color: days === d ? 'var(--beige)' : 'var(--text-muted)' }}
                  className="px-4 py-1.5 text-xs rounded transition-colors">
                  {d}일
                </button>
              ))}
            </div>
          </div>

          {/* 결과 */}
          {selected && (
            <div style={{ borderTop: '1px solid var(--border)' }} className="pt-4">
              <p style={{ color: 'var(--text-muted)' }} className="text-xs mb-3">
                {selected.name} 기준 (최근 {days}일)
              </p>
              {loading ? (
                <div style={{ background: 'var(--bg)' }} className="h-32 rounded animate-pulse" />
              ) : history.length === 0 ? (
                <p style={{ color: 'var(--text-disabled)' }} className="text-sm text-center py-6">거래 데이터가 없습니다</p>
              ) : (
                <div className="grid grid-cols-2 gap-2">
                  {[
                    { label: '평균가', value: formatPrice(avg), color: 'var(--brown)' },
                    { label: '최고가', value: formatPrice(maxP), color: 'var(--danger)' },
                    { label: '최저가', value: formatPrice(minP), color: 'var(--buy-text)' },
                    { label: '거래량', value: `${total}건`, color: 'var(--text)' },
                  ].map(({ label, value, color }) => (
                    <div key={label} style={{ background: 'var(--bg)', border: '1px solid var(--border)' }} className="rounded-lg p-3 text-center">
                      <p style={{ color: 'var(--text-muted)' }} className="text-xs mb-1">{label}</p>
                      <p className="font-serif text-base font-bold" style={{ color }}>{value}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ══════════ 메인 ══════════

function TradePageInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const initialQ = searchParams.get('q') ?? '';
  const initialItemId = searchParams.get('itemId') ? Number(searchParams.get('itemId')) : null;
  // itemId가 있으면 아이템 검색으로, 없으면 세트명 텍스트 필터로 초기화
  const [filters, setFilters] = useState<Filters>({
    setName: initialItemId ? '' : initialQ,
    selectedItemId: initialItemId,
    selectedItemName: initialItemId ? initialQ : '',
    ritual: '없음',
    setPieceMarks: [],
  });
  const [activeTab, setActiveTab] = useState<Tab>('all');
  const [sellSort, setSellSort] = useState<SortKey>('latest');
  const [buySort, setBuySort] = useState<SortKey>('latest');
  const [sellItems, setSellItems] = useState<TradeItem[]>([]);
  const [buyItems, setBuyItems] = useState<TradeItem[]>([]);
  const [servers, setServers] = useState<ServerDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selectedItem, setSelectedItem] = useState<TradeItem | null>(null);
  const [chatStarting, setChatStarting] = useState(false);
  const [showPrice, setShowPrice] = useState(false);
  const [showCreate, setShowCreate] = useState(false);

  useEffect(() => {
    api.getServers().then(setServers).catch(() => {});
  }, []);

  const loadListings = useCallback(async (itemId?: number | null) => {
    const serverName = getSelectedServerName(servers);
    if (!serverName) {
      setSellItems([]);
      setBuyItems([]);
      return;
    }

    setLoading(true);
    setError('');
    try {
      const params: Record<string, string> = { server: serverName };
      if (itemId) params.itemId = String(itemId);
      const [listings, wanted] = await Promise.all([
        api.getListings(params).catch(() => [] as ListingDto[]),
        api.getWanted(params).catch(() => [] as WantedDto[]),
      ]);
      setSellItems(listings.map(mapListing));
      setBuyItems(wanted.map(mapWanted));
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }, [servers]);

  useEffect(() => {
    loadListings(filters.selectedItemId);
  }, [loadListings, filters.selectedItemId]);

  useEffect(() => {
    const onServerChanged = () => loadListings(filters.selectedItemId);
    window.addEventListener('server-changed', onServerChanged);
    return () => window.removeEventListener('server-changed', onServerChanged);
  }, [loadListings, filters.selectedItemId]);

  function applyFilters(items: TradeItem[]): TradeItem[] {
    return items.filter((item) => {
      if (filters.setName && !item.displayName.includes(filters.setName)) return false;
      if (filters.ritual !== '없음' && filters.ritual !== '전체' && !item.displayName.includes(filters.ritual)) return false;
      for (const mark of filters.setPieceMarks) {
        if (mark && !item.displayName.includes(mark)) return false;
      }
      return true;
    });
  }

  const filteredSell = applyFilters(sellItems);
  const filteredBuy = applyFilters(buyItems);

  async function handleDirectChat(item: TradeItem) {
    setSelectedItem(null);
    setChatStarting(true);
    try {
      const res = await api.createChatRoom({
        listingId: item.id,
        listingType: item.listingType === 'SELL' ? 'SELL' : 'WANTED',
        initiationType: 'APPLY',
      }) as ChatRoomSummaryDto;
      window.dispatchEvent(new CustomEvent('open-chat-room', { detail: res }));
    } catch (e: unknown) {
      setError(parseApiError(e));
    } finally {
      setChatStarting(false);
    }
  }

  return (
    <div className="flex" style={{ height: 'calc(100vh - 76px)' }}>
      <FilterSidebar
        filters={filters}
        onChange={(key, val) => setFilters((prev) => ({ ...prev, [key]: val }))}
        onItemSelect={(id, name) => setFilters((prev) => ({ ...prev, selectedItemId: id, selectedItemName: name }))}
        onReset={() => {
          setFilters({ setName: '', selectedItemId: null, selectedItemName: '', ritual: '없음', setPieceMarks: [] });
          loadListings();
        }}
        onSetPieceMarksChange={(marks) => setFilters((prev) => ({ ...prev, setPieceMarks: marks }))}
        onSetSearch={() => loadListings()}
      />

      <div className="flex-1 flex flex-col min-w-0 min-h-0">
        {/* 탭 바 */}
        <div
          style={{ background: 'var(--card)', borderBottom: '1px solid var(--border)' }}
          className="flex items-center justify-between px-2 shrink-0 sticky top-[76px] z-[100]"
        >
          <div className="flex">
            {(['all', 'sell', 'buy'] as const).map((tab) => (
              <button key={tab} onClick={() => setActiveTab(tab)}
                style={{
                  borderBottom: activeTab === tab ? '2px solid var(--brown)' : '2px solid transparent',
                  color: activeTab === tab ? 'var(--text)' : 'var(--text-muted)',
                  padding: '12px 16px',
                }}
                className="text-sm font-medium transition-colors">
                {tab === 'all' ? '전체' : tab === 'sell' ? '팝니다' : '삽니다'}
              </button>
            ))}
          </div>
          <div className="flex items-center gap-2 mr-2">
            {error && <span className="text-xs text-red-400">{error}</span>}
            <button onClick={() => setShowPrice(true)}
              style={{ color: 'var(--text-muted)', border: '1px solid var(--border)' }}
              className="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded hover:border-[var(--brown)] hover:text-[var(--brown)] transition-colors">
              <BarChart2 style={{ width: 14, height: 14 }} /> 시세 조회
            </button>
            <button onClick={() => setShowCreate(true)}
              style={{ background: 'var(--brown)', color: 'var(--beige)' }}
              className="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded hover:bg-[var(--brown-dark)] transition-colors">
              <Plus style={{ width: 14, height: 14 }} /> 등록하기
            </button>
          </div>
        </div>

        {/* 목록 */}
        <div className={`flex-1 min-h-0 flex ${activeTab === 'all' ? 'divide-x' : ''}`} style={{ borderColor: 'var(--border)' }}>
          {(activeTab === 'all' || activeTab === 'sell') && (
            <div className={activeTab === 'all' ? 'w-1/2' : 'flex-1'}>
              <TradeList items={filteredSell} sort={sellSort} onSortChange={setSellSort}
                title="팝니다" showBadge={activeTab === 'all'} onCardClick={setSelectedItem} loading={loading} />
            </div>
          )}
          {(activeTab === 'all' || activeTab === 'buy') && (
            <div className={activeTab === 'all' ? 'w-1/2' : 'flex-1'}>
              <TradeList items={filteredBuy} sort={buySort} onSortChange={setBuySort}
                title="삽니다" showBadge={activeTab === 'all'} onCardClick={setSelectedItem} loading={loading} />
            </div>
          )}
        </div>
      </div>

      {selectedItem && (
        <DetailModal
          item={selectedItem}
          onClose={() => setSelectedItem(null)}
          onDeleted={() => {
            const deletedId = selectedItem.id;
            setSelectedItem(null);
            setSellItems((prev) => prev.filter((i) => i.id !== deletedId));
            setBuyItems((prev) => prev.filter((i) => i.id !== deletedId));
            loadListings(filters.selectedItemId);
            router.refresh();
          }}
          onChat={() => handleDirectChat(selectedItem)}
          chatLoading={chatStarting}
        />
      )}
      {showPrice && <PriceDrawer onClose={() => setShowPrice(false)} />}
      {showCreate && (
        <CreateListingModal
          onClose={() => setShowCreate(false)}
          onCreated={() => {
            setShowCreate(false);
            loadListings(filters.selectedItemId);
          }}
        />
      )}
    </div>
  );
}

export default function TradePage() {
  return (
    <Suspense>
      <TradePageInner />
    </Suspense>
  );
}
