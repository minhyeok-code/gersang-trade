'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { getToken, getServer } from '@/lib/api';
import Link from 'next/link';
import { Sword, Users, Clock } from 'lucide-react';
import SearchBar from '@/components/common/SearchBar';
import InterestPriceWatchPanel from '@/components/home/InterestPriceWatchPanel';

export default function Home() {
  const router = useRouter();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [noServerWarning, setNoServerWarning] = useState(false);

  useEffect(() => {
    setIsLoggedIn(!!getToken());
    const onAuth = () => setIsLoggedIn(!!getToken());
    window.addEventListener('auth-changed', onAuth);
    return () => window.removeEventListener('auth-changed', onAuth);
  }, []);

  function handleSearch(query: string, itemId: number | null) {
    if (!getServer()) {
      setNoServerWarning(true);
      return;
    }
    setNoServerWarning(false);
    const params = new URLSearchParams();
    if (query) params.set('q', query);
    if (itemId) params.set('itemId', String(itemId));
    router.push(`/trade${params.size ? `?${params}` : ''}`);
  }

  return (
    <div className="py-8">
      {/* 검색 영역 */}
      <div className="mb-8 flex flex-col items-center text-center pt-6">
        <h1 className="font-serif text-3xl font-bold mb-1" style={{ color: 'var(--text)' }}>
          거상인
        </h1>
        <p style={{ color: 'var(--text-muted)' }} className="text-sm mb-6">
          아이템 거래 · 시세 조회 · DPS 계산기
        </p>

        {noServerWarning && (
          <div
            className="mb-3 px-4 py-2 rounded text-sm font-medium"
            style={{ background: 'var(--card)', color: 'var(--brown)', border: '1px solid var(--brown)' }}
          >
            먼저 상단에서 서버를 선택해주세요
          </div>
        )}

        <div className="w-full max-w-xl" onClick={() => setNoServerWarning(false)}>
          <SearchBar
            showSubmitButton
            onSearch={(q, itemId) => handleSearch(q, itemId)}
          />
        </div>

        <div className="flex flex-wrap items-center justify-center gap-3 mt-4">
          <Link
            href="/trade"
            style={{ color: 'var(--text-muted)' }}
            className="text-xs hover:text-[var(--text)] transition-colors"
          >
            전체 거래 보기 →
          </Link>
          <span style={{ color: 'var(--border)' }}>|</span>
          <Link
            href="/deck"
            style={{ color: 'var(--text-muted)' }}
            className="text-xs hover:text-[var(--text)] transition-colors"
          >
            전투 계산기 →
          </Link>
          <span style={{ color: 'var(--border)' }}>|</span>
          <Link
            href="/value-test"
            style={{ color: 'var(--text-muted)' }}
            className="text-xs hover:text-[var(--text)] transition-colors"
          >
            가성비테스트 →
          </Link>
          <span style={{ color: 'var(--border)' }}>|</span>
          <Link
            href="/clear-time"
            style={{ background: 'var(--brown)', color: 'var(--beige)' }}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-medium hover:opacity-90 transition-opacity"
          >
            <Clock style={{ width: 14, height: 14 }} />
            사냥 허브
          </Link>
        </div>
      </div>

      {isLoggedIn ? (
        <div className="grid lg:grid-cols-2 gap-6">
          <InterestPriceWatchPanel />

          <div
            style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
            className="rounded-xl p-5"
          >
            <div className="flex items-center gap-2 mb-4">
              <Sword style={{ color: 'var(--brown)', width: 18, height: 18 }} />
              <h2 className="font-semibold" style={{ color: 'var(--text)' }}>스펙업 추천</h2>
            </div>
            <div className="flex flex-col items-center justify-center py-8" style={{ color: 'var(--text-muted)' }}>
              <Users style={{ width: 36, height: 36, margin: '0 auto 8px', opacity: 0.4 }} />
              <p className="text-sm">덱을 구성하면 스펙업 추천이 제공됩니다</p>
              <Link
                href="/deck"
                style={{ color: 'var(--brown)' }}
                className="text-xs mt-2 inline-block hover:underline"
              >
                전투 계산기에서 덱 설정하기 →
              </Link>
            </div>
          </div>
        </div>
      ) : (
        <div className="grid lg:grid-cols-3 gap-5">
          {[
            {
              icon: <TrendingUpPlaceholder />,
              title: '실시간 거래',
              desc: '팝니다·삽니다 등록글을 서버별로 조회하고 채팅으로 바로 거래하세요.',
              href: '/trade',
              label: '거래 보기',
            },
            {
              icon: <BarChartPlaceholder />,
              title: '시세 조회',
              desc: '관심 매물의 판매·구매·거래완료 시세를 한눈에 확인하세요.',
              href: '/login',
              label: '로그인하기',
            },
            {
              icon: <Sword style={{ width: 28, height: 28, color: 'var(--brown)' }} />,
              title: 'DPS 계산기',
              desc: '덱을 구성하고 몬스터별 DPS를 계산해 최적 스펙업 경로를 찾으세요.',
              href: '/deck',
              label: '계산기 열기',
            },
          ].map((card) => (
            <div
              key={card.title}
              style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
              className="rounded-xl p-6 hover:border-[var(--brown)] hover:shadow-md transition-all"
            >
              <div className="mb-3">{card.icon}</div>
              <h3 className="font-semibold text-lg mb-2" style={{ color: 'var(--text)' }}>{card.title}</h3>
              <p className="text-sm mb-4" style={{ color: 'var(--text-muted)' }}>{card.desc}</p>
              <Link
                href={card.href}
                style={{ background: 'var(--brown)', color: 'var(--beige)' }}
                className="inline-block px-4 py-2 rounded text-sm font-medium hover:bg-[var(--brown-dark)] transition-colors"
              >
                {card.label}
              </Link>
            </div>
          ))}

          <div
            style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
            className="lg:col-span-3 rounded-xl p-6 flex items-center justify-between"
          >
            <div>
              <h3 className="font-semibold" style={{ color: 'var(--text)' }}>로그인하면 더 많은 기능을 사용할 수 있어요</h3>
              <p className="text-sm mt-1" style={{ color: 'var(--text-muted)' }}>
                관심 아이템 시세 · 거래 등록 · 채팅 · 덱 저장
              </p>
            </div>
            <Link
              href="/login"
              style={{ background: 'var(--brown)', color: 'var(--beige)' }}
              className="px-6 py-2.5 rounded font-semibold text-sm hover:bg-[var(--brown-dark)] transition-colors whitespace-nowrap"
            >
              Google / Naver 로그인
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}

function TrendingUpPlaceholder() {
  return (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--brown)" strokeWidth="2">
      <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" />
      <polyline points="17 6 23 6 23 12" />
    </svg>
  );
}

function BarChartPlaceholder() {
  return (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--brown)" strokeWidth="2">
      <line x1="12" y1="20" x2="12" y2="10" />
      <line x1="18" y1="20" x2="18" y2="4" />
      <line x1="6" y1="20" x2="6" y2="16" />
    </svg>
  );
}
