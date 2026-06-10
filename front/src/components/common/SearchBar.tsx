'use client';

import { useState, useEffect, useRef } from 'react';
import { api } from '@/lib/api';
import { Search, X } from 'lucide-react';

interface SearchItem {
  id: number;
  name: string;
  type: string;
}

interface SearchBarProps {
  placeholder?: string;
  initialValue?: string;
  initialItemId?: number | null;
  initialItemName?: string;
  onSearch: (query: string, itemId: number | null, itemName: string, itemType?: string) => void;
  showSubmitButton?: boolean;
  size?: 'sm' | 'md';
}

export default function SearchBar({
  placeholder = '아이템명, 세트명으로 검색...',
  initialValue = '',
  initialItemId = null,
  initialItemName = '',
  onSearch,
  showSubmitButton = false,
  size = 'md',
}: SearchBarProps) {
  const [query, setQuery] = useState(initialItemId ? initialItemName : initialValue);
  const [selectedId, setSelectedId] = useState<number | null>(initialItemId);
  const [results, setResults] = useState<SearchItem[]>([]);
  const [dropOpen, setDropOpen] = useState(false);
  const [searching, setSearching] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const isSm = size === 'sm';
  const inputPy = isSm ? 'py-1.5' : 'py-2.5';
  const inputPl = isSm ? 'pl-3' : 'pl-9';

  useEffect(() => {
    function handleOutsideClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setDropOpen(false);
    }
    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, []);

  useEffect(() => {
    // 아이템이 선택된 상태에서는 드롭다운 검색 안 함
    if (selectedId) { setResults([]); setDropOpen(false); return; }
    if (!query.trim()) { setResults([]); setDropOpen(false); return; }
    const t = setTimeout(async () => {
      setSearching(true);
      try {
        const res = await api.searchItems(query, { limit: 10 });
        setResults(res);
        setDropOpen(res.length > 0);
      } catch {
        setResults([]);
      } finally {
        setSearching(false);
      }
    }, 300);
    return () => clearTimeout(t);
  }, [query, selectedId]);

  function handleSelect(item: SearchItem) {
    setSelectedId(item.id);
    setQuery(item.name);
    setDropOpen(false);
    onSearch(item.name, item.id, item.name, item.type);
  }

  function handleClear() {
    setSelectedId(null);
    setQuery('');
    onSearch('', null, '');
  }

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    // 입력을 바꾸면 선택 해제
    if (selectedId) setSelectedId(null);
    setQuery(e.target.value);
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setDropOpen(false);
    onSearch(query.trim(), selectedId, query.trim());
  }

  const isSelected = !!selectedId;

  return (
    <form onSubmit={handleSubmit} className="flex gap-2 w-full">
      <div className="relative flex-1" ref={ref}>
        {!isSm && (
          <Search
            style={{
              position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)',
              color: 'var(--text-disabled)', width: 16, height: 16,
            }}
          />
        )}
        <input
          value={query}
          onChange={handleChange}
          onFocus={() => results.length > 0 && setDropOpen(true)}
          placeholder={placeholder}
          className={`w-full rounded-lg ${inputPl} ${isSelected ? 'pr-7' : 'pr-4'} ${inputPy} text-sm focus:outline-none focus:border-[var(--brown)] placeholder-[var(--text-disabled)]`}
          style={{
            background: 'var(--card)',
            border: `1px solid ${isSelected ? 'var(--brown)' : 'var(--border)'}`,
            color: 'var(--text)',
          }}
        />
        {/* 선택됐을 때 X 버튼 */}
        {isSelected && (
          <button
            type="button"
            onClick={handleClear}
            style={{ color: 'var(--text-muted)', position: 'absolute', right: 6, top: '50%', transform: 'translateY(-50%)' }}
            className="hover:text-red-400 transition-colors"
          >
            <X style={{ width: 13, height: 13 }} />
          </button>
        )}
        {searching && !isSelected && (
          <span style={{ color: 'var(--text-muted)' }} className="absolute right-3 top-1/2 -translate-y-1/2 text-xs">
            검색 중...
          </span>
        )}
        {dropOpen && (
          <ul
            style={{ background: 'var(--card)', border: '1px solid var(--border)', zIndex: 50 }}
            className="absolute top-full left-0 right-0 mt-0.5 rounded shadow-xl max-h-48 overflow-y-auto"
          >
            {results.map((item) => (
              <li
                key={item.id}
                onMouseDown={() => handleSelect(item)}
                className="flex items-center px-3 py-2 text-sm cursor-pointer hover:bg-[var(--bg)]"
              >
                <span style={{ color: 'var(--text)' }}>{item.name}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
      {showSubmitButton && (
        <button
          type="submit"
          className={`${isSm ? 'px-3 py-1.5' : 'px-5 py-2.5'} rounded-lg font-semibold text-sm transition-opacity hover:opacity-90 shrink-0`}
          style={{ background: 'var(--brown)', color: 'var(--beige)' }}
        >
          검색
        </button>
      )}
    </form>
  );
}
