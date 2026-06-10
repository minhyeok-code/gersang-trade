'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { api, getToken, getServer } from '@/lib/api';
import Link from 'next/link';
import { TrendingUp, Sword, Users, BarChart2, Clock } from 'lucide-react';
import SearchBar from '@/components/common/SearchBar';

interface PriceWatchItem {
  itemId: number;
  itemName: string;
  recentAvg: number;
  prevAvg: number;
  changeRate: number;
}

function formatPrice(price: number): string {
  if (price >= 100_000_000) {
    const v = price / 100_000_000;
    return `${v % 1 === 0 ? v : v.toFixed(1)}억 전`;
  }
  if (price >= 10_000) return `${Math.floor(price / 10_000)}만 전`;
  return `${price.toLocaleString()} 전`;
}

export default function Home() {
  const router = useRouter();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [priceWatch, setPriceWatch] = useState<PriceWatchItem[]>([]);
  const [watchLoading, setWatchLoading] = useState(false);
  const [noServerWarning, setNoServerWarning] = useState(false);

  useEffect(() => {
    const logged = !!getToken();
    setIsLoggedIn(logged);
    if (logged) {
      setWatchLoading(true);
      api.getPriceWatch()
        .then((items) => setPriceWatch(items as PriceWatchItem[]))
        .catch(() => {})
        .finally(() => setWatchLoading(false));
    }
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

        {/* 서버 미선택 경고 */}
        {noServerWarning && (
          <div
            className="mb-3 px-4 py-2 rounded text-sm font-medium"
            style={{ background: 'var(--card)', color: 'var(--brown)', border: '1px solid var(--brown)' }}
          >
            먼저 상단에서 서버를 선택해주세요
          </div>
        )}

        {/* 검색창 */}
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
          {/* 관심목록 시세 */}
          <div
            style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
            className="rounded-xl p-5"
          >
            <div className="flex items-center gap-2 mb-4">
              <TrendingUp style={{ color: 'var(--brown)', width: 18, height: 18 }} />
              <h2 className="font-semibold" style={{ color: 'var(--text)' }}>관심 아이템 시세</h2>
            </div>
            {watchLoading ? (
              <div className="space-y-2">
                {[1, 2, 3].map((i) => (
                  <div key={i} style={{ background: 'var(--bg)' }} className="h-12 rounded animate-pulse" />
                ))}
              </div>
            ) : priceWatch.length === 0 ? (
              <div className="text-center py-8" style={{ color: 'var(--text-muted)' }}>
                <BarChart2 style={{ width: 36, height: 36, margin: '0 auto 8px', opacity: 0.4 }} />
                <p className="text-sm">등록된 관심 아이템이 없습니다</p>
                <Link
                  href="/trade"
                  style={{ color: 'var(--brown)' }}
                  className="text-xs mt-2 inline-block hover:underline"
                >
                  거래 페이지에서 아이템을 검색해보세요 →
                </Link>
              </div>
            ) : (
              <div className="space-y-2">
                {priceWatch.map((item) => (
                  <div
                    key={item.itemId}
                    style={{ background: 'var(--bg)', border: '1px solid var(--border)' }}
                    className="flex items-center justify-between px-4 py-3 rounded-lg"
                  >
                    <span className="text-sm font-medium" style={{ color: 'var(--text)' }}>{item.itemName}</span>
                    <div className="flex items-center gap-3">
                      <span className="font-serif text-sm font-bold" style={{ color: 'var(--brown)' }}>
                        {formatPrice(item.recentAvg)}
                      </span>
                      {item.changeRate !== undefined && (
                        <span
                          className="text-xs font-medium"
                          style={{ color: item.changeRate >= 0 ? '#C24A4A' : '#3A4F6B' }}
                        >
                          {item.changeRate >= 0 ? '+' : ''}{item.changeRate.toFixed(1)}%
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* 스펙업 추천 */}
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
        /* 비로그인 소개 */
        <div className="grid lg:grid-cols-3 gap-5">
          {[
            {
              icon: <TrendingUp style={{ width: 28, height: 28, color: 'var(--brown)' }} />,
              title: '실시간 거래',
              desc: '팝니다·삽니다 등록글을 서버별로 조회하고 채팅으로 바로 거래하세요.',
              href: '/trade',
              label: '거래 보기',
            },
            {
              icon: <BarChart2 style={{ width: 28, height: 28, color: 'var(--brown)' }} />,
              title: '시세 조회',
              desc: '아이템별 최근 5·10·15일 실거래가를 확인하고 적정 가격을 판단하세요.',
              href: '/trade',
              label: '시세 조회',
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
                관심 아이템 시세 알림 · 거래 등록 · 채팅 · 덱 저장
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
