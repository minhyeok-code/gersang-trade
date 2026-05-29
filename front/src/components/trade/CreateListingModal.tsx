'use client';

import { useEffect, useRef, useState } from 'react';
import { X } from 'lucide-react';
import { api, getServer, type ItemSearchResult, type ServerDto } from '@/lib/api';

interface CreateListingModalProps {
  onClose: () => void;
  onCreated: () => void;
}

type ListingMode = 'SELL' | 'BUY';

const EOK = 100_000_000;

function parseEokPrice(value: string) {
  const amount = Number(value);
  if (!Number.isFinite(amount) || amount <= 0) return 0;
  return Math.round(amount * EOK);
}

function formatJeon(value: number) {
  return `${value.toLocaleString()} 전`;
}

export default function CreateListingModal({ onClose, onCreated }: CreateListingModalProps) {
  const [listingMode, setListingMode] = useState<ListingMode>('SELL');
  const [servers, setServers] = useState<ServerDto[]>([]);
  const [server, setServer] = useState('');
  const [price, setPrice] = useState('');
  const [note, setNote] = useState('');
  const [quantity, setQuantity] = useState('1');
  const [itemQuery, setItemQuery] = useState('');
  const [itemResults, setItemResults] = useState<ItemSearchResult[]>([]);
  const [selectedItem, setSelectedItem] = useState<ItemSearchResult | null>(null);
  const [dropOpen, setDropOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const searchRef = useRef<HTMLDivElement>(null);
  const priceInJeon = parseEokPrice(price);

  useEffect(() => {
    async function loadInitialServer() {
      try {
        const [list, me] = await Promise.all([
          api.getServers(),
          api.getMe().catch(() => null),
        ]);
        setServers(list);
        const savedServerId = getServer();
        const profileServer = list.find((s) => (
          (me?.serverId != null && s.serverId === me.serverId)
          || (me?.serverName != null && s.name === me.serverName)
          || (me?.server != null && s.name === me.server)
        ));
        const localServer = list.find((s) => String(s.serverId) === savedServerId);
        setServer(profileServer?.name ?? localServer?.name ?? list[0]?.name ?? '');
      } catch {
        setServers([]);
      }
    }
    loadInitialServer();
  }, []);

  useEffect(() => {
    function close(e: MouseEvent) {
      if (searchRef.current && !searchRef.current.contains(e.target as Node)) {
        setDropOpen(false);
      }
    }
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  useEffect(() => {
    if (!itemQuery.trim() || selectedItem) {
      setItemResults([]);
      return;
    }
    const t = setTimeout(async () => {
      try {
        const results = await api.searchItems(itemQuery, { limit: 8 });
        setItemResults(results);
        setDropOpen(results.length > 0);
      } catch {
        setItemResults([]);
      }
    }, 250);
    return () => clearTimeout(t);
  }, [itemQuery, selectedItem]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    if (!selectedItem) {
      setError('아이템을 선택해주세요.');
      return;
    }
    if (!server) {
      setError('서버를 선택해주세요.');
      return;
    }
    if (priceInJeon <= 0) {
      setError('가격을 입력해주세요.');
      return;
    }

    setSubmitting(true);
    try {
      if (listingMode === 'SELL') {
        await api.createListing({
          server,
          price: priceInJeon,
          note: note.trim() || null,
          bundles: [
            {
              bundleType: selectedItem.type === 'EQUIPMENT' ? 'EQUIPMENT_SINGLE' : 'MATERIAL_BUNDLE',
              titleOverride: selectedItem.name,
              lines: [
                {
                  itemId: selectedItem.id,
                  quantity: Number(quantity),
                  sortOrder: 0,
                  equipmentDetail: selectedItem.type === 'EQUIPMENT'
                    ? { enhanceLevel: null, hasRitual: false, rituals: [] }
                    : null,
                },
              ],
            },
          ],
        });
      } else {
        await api.createWanted({
          server,
          offeredPrice: priceInJeon,
          note: note.trim() || null,
          items: [
            {
              itemId: selectedItem.id,
              quantity: Number(quantity),
              sortOrder: 0,
              equipmentCondition: selectedItem.type === 'EQUIPMENT'
                ? { minEnhanceLevel: null, hasRitual: false, ritualConditions: [] }
                : null,
            },
          ],
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
        className="w-full max-w-lg rounded-xl overflow-hidden"
        style={{ background: 'var(--card)', border: '1px solid var(--border)', boxShadow: '0 24px 60px rgba(0,0,0,0.3)' }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ borderBottom: '1px solid var(--border)' }} className="flex items-center justify-between px-5 py-3.5">
          <div>
            <h2 className="font-semibold" style={{ color: 'var(--text)' }}>
              {listingMode === 'SELL' ? '판매 등록' : '구매 희망 등록'}
            </h2>
            <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>
              {listingMode === 'SELL' ? '판매 게시글을 등록합니다.' : '구매 희망 게시글을 등록합니다.'}
            </p>
          </div>
          <button type="button" onClick={onClose} style={{ color: 'var(--text-muted)' }} className="hover:text-[var(--text)]">
            <X style={{ width: 20, height: 20 }} />
          </button>
        </div>

        <div className="p-5 space-y-4">
          <div>
            <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>거래 유형</label>
            <div className="grid grid-cols-2 gap-2">
              {([
                { value: 'SELL' as const, label: '판매' },
                { value: 'BUY' as const, label: '구매' },
              ]).map((option) => {
                const selected = listingMode === option.value;
                return (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => setListingMode(option.value)}
                    className="rounded-lg px-3 py-2 text-sm font-medium transition-colors"
                    style={{
                      background: selected ? 'var(--brown)' : 'var(--bg)',
                      border: `1px solid ${selected ? 'var(--brown)' : 'var(--border)'}`,
                      color: selected ? 'var(--beige)' : 'var(--text-muted)',
                    }}
                  >
                    {option.label}
                  </button>
                );
              })}
            </div>
          </div>

          <div>
            <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>서버</label>
            {servers.length > 0 ? (
              <div className="flex flex-wrap gap-1.5">
                {servers.map((s) => {
                  const selected = server === s.name;
                  return (
                    <button
                      key={s.serverId}
                      type="button"
                      onClick={() => setServer(s.name)}
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
              <input
                value={server}
                onChange={(e) => setServer(e.target.value)}
                required
                className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]"
                style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
              />
            )}
          </div>

          <div>
            <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>아이템</label>
            <div className="relative" ref={searchRef}>
              <input
                value={selectedItem ? selectedItem.name : itemQuery}
                onChange={(e) => { setSelectedItem(null); setItemQuery(e.target.value); }}
                onFocus={() => itemResults.length > 0 && setDropOpen(true)}
                placeholder="아이템명을 검색하세요"
                required
                className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]"
                style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
              />
              {dropOpen && itemResults.length > 0 && (
                <ul
                  className="absolute top-full left-0 right-0 mt-1 rounded shadow-xl max-h-48 overflow-y-auto"
                  style={{ background: 'var(--card)', border: '1px solid var(--border)', zIndex: 520 }}
                >
                  {itemResults.map((item) => (
                    <li
                      key={item.id}
                      onMouseDown={() => {
                        setSelectedItem(item);
                        setItemQuery(item.name);
                        setDropOpen(false);
                      }}
                      className="flex items-center justify-between px-3 py-2 text-sm cursor-pointer hover:bg-[var(--bg)]"
                    >
                      <span style={{ color: 'var(--text)' }}>{item.name}</span>
                      <span className="text-xs ml-2" style={{ color: 'var(--text-muted)' }}>{item.type}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>
                {listingMode === 'SELL' ? '가격' : '제시 가격'}
              </label>
              <input
                type="number"
                step="any"
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                required
                placeholder="억 단위"
                className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]"
                style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
              />
              <p className="text-xs mt-1" style={{ color: priceInJeon > 0 ? 'var(--brown)' : 'var(--text-disabled)' }}>
                {priceInJeon > 0 ? formatJeon(priceInJeon) : '예: 7 입력 시 700,000,000 전'}
              </p>
            </div>
            <div>
              <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>수량</label>
              <input
                type="number"
                min="1"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                required
                className="w-full rounded px-3 py-2 text-sm focus:outline-none focus:border-[var(--brown)]"
                style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
              />
            </div>
          </div>

          <div>
            <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>메모</label>
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              rows={3}
              maxLength={500}
              className="w-full rounded px-3 py-2 text-sm resize-none focus:outline-none focus:border-[var(--brown)]"
              style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)' }}
            />
          </div>

          {error && <p className="text-sm" style={{ color: 'var(--danger)' }}>{error}</p>}
        </div>

        <div style={{ borderTop: '1px solid var(--border)' }} className="flex justify-end gap-2 px-5 py-3.5">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-lg text-sm"
            style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
          >
            취소
          </button>
          <button
            type="submit"
            disabled={submitting}
            className="px-5 py-2 rounded-lg text-sm font-semibold disabled:opacity-50"
            style={{ background: 'var(--brown)', color: 'var(--beige)' }}
          >
            {submitting ? '등록 중...' : listingMode === 'SELL' ? '판매 등록' : '구매 등록'}
          </button>
        </div>
      </form>
    </div>
  );
}
