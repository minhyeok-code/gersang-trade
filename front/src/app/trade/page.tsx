'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { api, getSelectedServerName, sortChatMessages, type ChatMessageDto, type ChatRoomDetailDto, type ListingDto, type WantedDto, type ServerDto } from '@/lib/api';
import { parseApiError } from '@/lib/parseApiError';
import { isMyMessage, isSystemMessage } from '@/lib/chatUtils';
import { useWs } from '@/lib/useWs';
import type { ChatMessageWsEvent, RoomStatusWsEvent } from '@/lib/wsTypes';
import { Search, BarChart2, Plus, X, ChevronDown, ChevronUp } from 'lucide-react';
import CreateListingModal from '@/components/trade/CreateListingModal';

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

interface SearchItem {
  id: number;
  name: string;
  type: string;
}

interface Filters {
  setName: string;
  selectedItemId: number | null;
  selectedItemName: string;
  ritual: string;
}

// ══════════ 헬퍼 ══════════

function formatPrice(price: number): string {
  if (price >= 100_000_000) {
    const v = price / 100_000_000;
    return `${v % 1 === 0 ? v : v.toFixed(1)}억 전`;
  }
  if (price >= 10_000) return `${Math.floor(price / 10_000)}만 전`;
  return `${price.toLocaleString()} 전`;
}

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
    sellerId: l.seller?.id,
  };
}

function mapWanted(w: WantedDto): TradeItem {
  const names = w.itemNames?.length
    ? w.itemNames
    : w.itemName
      ? [w.itemName]
      : [];
  const displayName = w.setName
    ? `${w.setName} 구매 희망`
    : names.length > 0
      ? `${names.join(', ')} 구매 희망`
      : `구매희망 #${w.id}`;
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

const RITUALS = ['전체', '없음', '천추', '북두', '천기', '개양', '천강', '자미', '탐랑', '거문', '녹존'];

// ══════════ 사이드바 ══════════

function FilterSidebar({ filters, onChange, onItemSelect, onReset }: {
  filters: Filters;
  onChange: (key: keyof Filters, value: string) => void;
  onItemSelect: (id: number | null, name: string) => void;
  onReset: () => void;
}) {
  const [itemQuery, setItemQuery] = useState('');
  const [itemResults, setItemResults] = useState<SearchItem[]>([]);
  const [searching, setSearching] = useState(false);
  const [dropOpen, setDropOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function close(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setDropOpen(false);
    }
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  useEffect(() => {
    if (!itemQuery.trim()) { setItemResults([]); setDropOpen(false); return; }
    const t = setTimeout(async () => {
      setSearching(true);
      try {
        const res = await api.searchItems(itemQuery, { limit: 10 });
        setItemResults(res);
        setDropOpen(res.length > 0);
      } catch { setItemResults([]); }
      finally { setSearching(false); }
    }, 300);
    return () => clearTimeout(t);
  }, [itemQuery]);

  return (
    <aside
      style={{ background: 'var(--card)', borderRight: '1px solid var(--border)', width: 240 }}
      className="shrink-0 overflow-y-auto"
    >
      <div className="p-3 space-y-4">
        {/* 아이템 검색 */}
        <div>
          <p style={{ color: 'var(--text-muted)' }} className="text-xs mb-2 font-medium uppercase tracking-wide">아이템 검색</p>
          {filters.selectedItemId ? (
            <div
              style={{ background: 'var(--bg)', border: '1px solid var(--brown)' }}
              className="flex items-center justify-between px-3 py-2 rounded"
            >
              <span className="text-sm font-medium truncate mr-2" style={{ color: 'var(--text)' }}>
                {filters.selectedItemName}
              </span>
              <button
                onClick={() => { onItemSelect(null, ''); setItemQuery(''); }}
                style={{ color: 'var(--text-muted)' }}
                className="hover:text-red-400 shrink-0 transition-colors"
              >
                <X style={{ width: 14, height: 14 }} />
              </button>
            </div>
          ) : (
            <div className="relative" ref={ref}>
              <input
                value={itemQuery}
                onChange={(e) => setItemQuery(e.target.value)}
                onFocus={() => itemResults.length > 0 && setDropOpen(true)}
                placeholder="아이템명 입력..."
                style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
                className="w-full rounded px-3 py-1.5 text-sm placeholder-[var(--text-disabled)] focus:outline-none focus:border-[var(--brown)]"
              />
              {searching && (
                <span style={{ color: 'var(--text-muted)' }} className="absolute right-2 top-1/2 -translate-y-1/2 text-xs">검색 중...</span>
              )}
              {dropOpen && (
                <ul
                  style={{ background: 'var(--card)', border: '1px solid var(--border)', zIndex: 20 }}
                  className="absolute top-full left-0 right-0 mt-0.5 rounded shadow-xl max-h-48 overflow-y-auto"
                >
                  {itemResults.map((item) => (
                    <li
                      key={item.id}
                      onMouseDown={() => { onItemSelect(item.id, item.name); setItemQuery(''); setDropOpen(false); }}
                      className="flex items-center justify-between px-3 py-2 text-sm cursor-pointer hover:bg-[var(--bg)]"
                    >
                      <span style={{ color: 'var(--text)' }}>{item.name}</span>
                      <span style={{ color: 'var(--text-muted)' }} className="text-xs ml-2 shrink-0">{item.type}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </div>

        {/* 세트명 */}
        <div>
          <p style={{ color: 'var(--text-muted)' }} className="text-xs mb-2 font-medium uppercase tracking-wide">세트명</p>
          <input
            value={filters.setName}
            onChange={(e) => onChange('setName', e.target.value)}
            placeholder="예: 지국천왕"
            style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
            className="w-full rounded px-3 py-1.5 text-sm placeholder-[var(--text-disabled)] focus:outline-none focus:border-[var(--brown)]"
          />
        </div>

        {/* 주술 */}
        <div>
          <p style={{ color: 'var(--text-muted)' }} className="text-xs mb-2 font-medium uppercase tracking-wide">주술</p>
          <div className="flex flex-wrap gap-1">
            {RITUALS.map((r) => (
              <button
                key={r}
                onClick={() => onChange('ritual', r)}
                style={{
                  background: filters.ritual === r ? 'var(--brown)' : 'var(--bg)',
                  border: `1px solid ${filters.ritual === r ? 'var(--brown)' : 'var(--border)'}`,
                  color: filters.ritual === r ? 'var(--beige)' : 'var(--text-muted)',
                }}
                className="px-2 py-0.5 text-xs rounded transition-colors"
              >
                {r}
              </button>
            ))}
          </div>
        </div>

        <button
          onClick={onReset}
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

function DetailModal({ item, onClose, onChat }: {
  item: TradeItem; onClose: () => void; onChat: () => void;
}) {
  const isSell = item.listingType === 'SELL';
  const [reporting, setReporting] = useState(false);

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
      onClose();
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
            style={{ background: 'var(--brown)', color: 'var(--beige)' }}
            className="px-5 py-2 rounded-lg text-sm font-semibold hover:bg-[var(--brown-dark)] transition-colors"
          >
            채팅하기
          </button>
        </div>
      </div>
    </div>
  );
}

// ══════════ 채팅 모달 ══════════

interface ChatMsg { id: number; content: string; type: 'TEXT' | 'SYSTEM'; mine: boolean; }

function mapApiMessage(message: ChatMessageDto, myNickname: string | null): ChatMsg {
  const system = isSystemMessage(message);
  return {
    id: message.id,
    content: message.content,
    type: system ? 'SYSTEM' : 'TEXT',
    mine: !system && isMyMessage(message, myNickname),
  };
}

function ChatModal({ item, onClose }: { item: TradeItem; onClose: () => void }) {
  const [msgs, setMsgs] = useState<ChatMsg[]>([]);
  const [detail, setDetail] = useState<ChatRoomDetailDto | null>(null);
  const [input, setInput] = useState('');
  const [finalPrice, setFinalPrice] = useState('');
  const [started, setStarted] = useState(false);
  const [roomId, setRoomId] = useState<number | null>(null);
  const [myNickname, setMyNickname] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);
  const prevCountRef = useRef(0);

  useEffect(() => {
    api.getMe().then((u) => setMyNickname(u.nickname)).catch(() => {});
  }, []);

  const loadMessages = useCallback(async () => {
    if (!roomId || roomId <= 0) return;
    try {
      const data = await api.getChatRoom(roomId);
      setDetail(data);
      setMsgs(sortChatMessages(data.messages ?? []).map((m) => mapApiMessage(m, myNickname)));
    } catch {
      // 초기 로드 실패는 UI 유지
    }
  }, [roomId, myNickname]);

  useEffect(() => {
    if (!started || !roomId || roomId <= 0 || myNickname == null) return;
    loadMessages();
  }, [started, roomId, myNickname, loadMessages]);

  const myNicknameRef = useRef<string | null>(null);
  myNicknameRef.current = myNickname;

  const isCompleted = detail?.status === 'COMPLETED';
  const isClosedOnly = detail?.status === 'CLOSED';
  const isTerminated = isCompleted || isClosedOnly;
  const canConfirm = detail && !isTerminated && !detail.myTradeConfirmed;
  const waitingPartner = detail?.myTradeConfirmed && !detail?.partnerTradeConfirmed && !isTerminated;
  const needsMyConfirm = detail?.partnerTradeConfirmed && !detail?.myTradeConfirmed && !isTerminated;
  const bothConfirmed = detail?.myTradeConfirmed && detail?.partnerTradeConfirmed && !isTerminated;

  useWs({
    chat_message: (data) => {
      const event = data as ChatMessageWsEvent;
      if (!roomId || event.chatRoomId !== roomId) return;
      const mapped = mapApiMessage(event.message, myNicknameRef.current);
      setMsgs((prev) => (prev.some((m) => m.id === mapped.id) ? prev : [...prev, mapped]));
    },
    room_status: (data) => {
      const event = data as RoomStatusWsEvent;
      if (!roomId || event.chatRoomId !== roomId) return;
      setDetail((prev) =>
        prev
          ? {
              ...prev,
              status: event.status,
              myTradeConfirmed: event.myTradeConfirmed ?? prev.myTradeConfirmed,
              partnerTradeConfirmed: event.partnerTradeConfirmed ?? prev.partnerTradeConfirmed,
            }
          : prev,
      );
    },
  });

  useEffect(() => {
    if (!waitingPartner && !needsMyConfirm && !bothConfirmed) return;
    const timer = setInterval(() => loadMessages(), 3000);
    return () => clearInterval(timer);
  }, [waitingPartner, needsMyConfirm, bothConfirmed, loadMessages]);

  async function handleTradeConfirm() {
    if (confirming || !roomId || roomId <= 0) return;
    setError('');
    setConfirming(true);
    try {
      const summary = await api.confirmTrade(roomId, finalPrice ? Number(finalPrice) : undefined);
      await loadMessages();
      setDetail((prev) =>
        prev
          ? {
              ...prev,
              status: (summary as ChatRoomDetailDto | undefined)?.status ?? prev.status,
              myTradeConfirmed: (summary as ChatRoomDetailDto | undefined)?.myTradeConfirmed ?? prev.myTradeConfirmed,
              partnerTradeConfirmed: (summary as ChatRoomDetailDto | undefined)?.partnerTradeConfirmed ?? prev.partnerTradeConfirmed,
            }
          : prev,
      );
      const status = (summary as ChatRoomDetailDto | undefined)?.status ?? detail?.status;
      if (status === 'CLOSED') {
        setError('이 채팅방에서는 거래가 확정되지 않았습니다. 게시물이 이미 처리되었거나 채팅방이 종료되었습니다.');
      }
    } catch (e: unknown) {
      setError(parseApiError(e));
    } finally {
      setConfirming(false);
    }
  }

  useEffect(() => {
    if (msgs.length > prevCountRef.current) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
    prevCountRef.current = msgs.length;
  }, [msgs]);

  async function handleStart(type: 'APPLY' | 'NEGOTIATE') {
    setLoading(true);
    try {
      const res = await api.createChatRoom({
        listingId: item.id,
        listingType: item.listingType,
        initiationType: type,
      }) as { id?: number; chatRoomId?: number };
      const id = Number(res.id ?? res.chatRoomId ?? 0);
      setRoomId(id);
      setStarted(true);
      if (id > 0) {
        const data = await api.getChatRoom(id);
        const me = await api.getMe().catch(() => null);
        const nickname = me?.nickname ?? myNickname;
        if (nickname) setMyNickname(nickname);
        setDetail(data);
        setMsgs(sortChatMessages(data.messages ?? []).map((m) => mapApiMessage(m, nickname ?? null)));
      }
    } catch {
      setRoomId(-1);
      setStarted(true);
    } finally {
      setLoading(false);
    }
  }

  async function handleSend() {
    const content = input.trim();
    if (!content || !roomId || roomId <= 0) return;
    setInput('');
    try {
      const msg = await api.sendMessage(roomId, content);
      setMsgs((prev) => {
        const mapped = mapApiMessage(msg, myNickname);
        if (prev.some((m) => m.id === mapped.id)) return prev;
        return [...prev, mapped];
      });
    } catch {
      setInput(content);
    }
  }

  return (
    <div className="fixed inset-0 flex items-center justify-center z-[500] p-4" style={{ background: 'rgba(0,0,0,0.65)' }} onClick={onClose}>
      <div
        style={{ background: 'var(--card)', border: '1px solid var(--border)', boxShadow: '0 24px 60px rgba(0,0,0,0.3)' }}
        className="rounded-xl w-full max-w-md flex flex-col h-[580px]"
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ borderBottom: '1px solid var(--border)' }} className="flex items-center justify-between px-4 py-3 shrink-0">
          <h2 className="font-semibold text-sm" style={{ color: 'var(--text)' }}>채팅</h2>
          <button onClick={onClose} style={{ color: 'var(--text-muted)' }} className="text-xl hover:text-[var(--text)]">×</button>
        </div>
        <div style={{ background: 'var(--bg)', borderBottom: '1px solid var(--border)' }} className="flex items-center gap-3 px-4 py-2.5 shrink-0">
          <div style={{ background: 'var(--beige)', fontSize: 20 }} className="w-9 h-9 rounded-lg flex items-center justify-center shrink-0">⚔️</div>
          <div className="min-w-0">
            <p className="text-xs font-semibold truncate" style={{ color: 'var(--text)' }}>{item.displayName}</p>
            <p className="text-xs font-serif font-bold" style={{ color: 'var(--brown)' }}>{formatPrice(item.price)}</p>
          </div>
        </div>

        {!started ? (
          <div className="flex-1 flex flex-col items-center justify-center gap-4 p-6">
            <p className="text-sm text-center" style={{ color: 'var(--text-muted)' }}>
              <span className="font-medium" style={{ color: 'var(--text)' }}>{item.nickname}</span>님과 어떻게 연락할까요?
            </p>
            <div className="flex gap-3">
              <button onClick={() => handleStart('NEGOTIATE')} disabled={loading}
                style={{ border: '1px solid var(--border)', color: 'var(--text)' }}
                className="px-5 py-2.5 rounded-lg text-sm hover:bg-[var(--bg)] transition-colors disabled:opacity-50">
                흥정하기
              </button>
              <button onClick={() => handleStart('APPLY')} disabled={loading}
                style={{ background: 'var(--brown)', color: 'var(--beige)' }}
                className="px-5 py-2.5 rounded-lg text-sm font-semibold hover:bg-[var(--brown-dark)] transition-colors disabled:opacity-50">
                거래신청
              </button>
            </div>
          </div>
        ) : (
          <>
            <div className="flex-1 overflow-y-auto p-4 space-y-2">
              {msgs.map((msg) => (
                <div key={msg.id} className={msg.type === 'SYSTEM' ? 'flex justify-center' : msg.mine ? 'flex justify-end' : 'flex justify-start'}>
                  {msg.type === 'SYSTEM' ? (
                    <span style={{ background: 'var(--bg)', color: 'var(--text-muted)' }} className="text-xs px-3 py-1 rounded-full">{msg.content}</span>
                  ) : msg.mine ? (
                    <div style={{ background: 'var(--brown)', color: 'var(--beige)' }} className="text-sm px-3 py-2 rounded-2xl rounded-tr-sm max-w-[75%]">{msg.content}</div>
                  ) : (
                    <div style={{ background: 'var(--bg)', color: 'var(--text)', border: '1px solid var(--border)' }} className="text-sm px-3 py-2 rounded-2xl rounded-tl-sm max-w-[75%]">{msg.content}</div>
                  )}
                </div>
              ))}
              <div ref={bottomRef} />
            </div>
            {error && (
              <div className="px-4 py-2 text-xs shrink-0" style={{ color: 'var(--danger)', borderTop: '1px solid var(--border)' }}>
                {error}
              </div>
            )}
            {isTerminated ? (
              <div style={{ borderTop: '1px solid var(--border)' }} className="px-4 py-3 shrink-0">
                <p className="text-xs text-center" style={{ color: 'var(--text-muted)' }}>
                  {isCompleted ? '거래가 확정되었습니다.' : '채팅방이 종료되었습니다.'}
                </p>
              </div>
            ) : (
              <div style={{ borderTop: '1px solid var(--border)' }} className="p-3 space-y-2 shrink-0">
                {detail && (
                  <div className="flex flex-col gap-1.5">
                    {needsMyConfirm && (
                      <p className="text-xs px-1" style={{ color: 'var(--brown)' }}>
                        상대방이 거래완료를 확인했습니다. 아래 버튼으로 확인해주세요.
                      </p>
                    )}
                    <div className="flex gap-2 items-center">
                      <input
                        type="number"
                        value={finalPrice}
                        onChange={(e) => setFinalPrice(e.target.value)}
                        placeholder="최종가 (선택)"
                        className="w-28 rounded px-2 py-1.5 text-xs focus:outline-none focus:border-[var(--brown)]"
                        style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
                      />
                      {canConfirm && (
                        <button
                          onClick={handleTradeConfirm}
                          disabled={confirming}
                          className="flex-1 px-2.5 py-1.5 rounded text-xs font-semibold disabled:opacity-50"
                          style={{ background: 'var(--brown)', color: 'var(--beige)' }}
                        >
                          {confirming ? '처리 중…' : needsMyConfirm ? '거래 완료 확인' : '거래 완료'}
                        </button>
                      )}
                      {waitingPartner && (
                        <p className="flex-1 text-xs text-right" style={{ color: 'var(--text-muted)' }}>
                          상대방 확인 대기 중…
                        </p>
                      )}
                    </div>
                  </div>
                )}
                <div className="flex gap-2">
                  <input value={input} onChange={(e) => setInput(e.target.value)}
                    onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); } }}
                    placeholder="메시지 입력..."
                    style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
                    className="flex-1 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]" />
                  <button onClick={handleSend} disabled={!input.trim()}
                    style={{ background: input.trim() ? 'var(--brown)' : 'var(--border)', color: input.trim() ? 'var(--beige)' : 'var(--text-disabled)' }}
                    className="px-4 py-2 rounded-lg text-sm font-semibold transition-colors">
                    전송
                  </button>
                </div>
              </div>
            )}
          </>
        )}
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
                    <span style={{ color: 'var(--text-muted)' }} className="text-xs ml-2">{r.type}</span>
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

export default function TradePage() {
  const [filters, setFilters] = useState<Filters>({ setName: '', selectedItemId: null, selectedItemName: '', ritual: '전체' });
  const [activeTab, setActiveTab] = useState<Tab>('all');
  const [sellSort, setSellSort] = useState<SortKey>('latest');
  const [buySort, setBuySort] = useState<SortKey>('latest');
  const [sellItems, setSellItems] = useState<TradeItem[]>([]);
  const [buyItems, setBuyItems] = useState<TradeItem[]>([]);
  const [servers, setServers] = useState<ServerDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selectedItem, setSelectedItem] = useState<TradeItem | null>(null);
  const [chatItem, setChatItem] = useState<TradeItem | null>(null);
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
      if (filters.ritual !== '전체' && filters.ritual !== '없음' && !item.displayName.includes(filters.ritual)) return false;
      return true;
    });
  }

  const filteredSell = applyFilters(sellItems);
  const filteredBuy = applyFilters(buyItems);

  return (
    <div className="flex" style={{ height: 'calc(100vh - 76px)' }}>
      <FilterSidebar
        filters={filters}
        onChange={(key, val) => setFilters((prev) => ({ ...prev, [key]: val }))}
        onItemSelect={(id, name) => setFilters((prev) => ({ ...prev, selectedItemId: id, selectedItemName: name }))}
        onReset={() => {
          setFilters({ setName: '', selectedItemId: null, selectedItemName: '', ritual: '전체' });
          loadListings();
        }}
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

      {selectedItem && <DetailModal item={selectedItem} onClose={() => setSelectedItem(null)} onChat={() => { setChatItem(selectedItem); setSelectedItem(null); }} />}
      {chatItem && <ChatModal item={chatItem} onClose={() => setChatItem(null)} />}
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
