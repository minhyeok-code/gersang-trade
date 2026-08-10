'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import {
  Map, ChevronDown, ChevronUp, ChevronRight, Check, ShoppingBag, Sword,
  MapPin, Star, Hammer, ScrollText, Coins, Package, Users,
} from 'lucide-react';
import {
  api,
  type GuideSummary,
  type UserGuideDetail,
  type UserGuideStepDto,
} from '@/lib/api';

type Mode = 'loading' | 'none' | 'start' | 'active';

/** 스텝 유형별 fallback 아이콘 (카탈로그 이미지 없을 때). */
function typeIcon(t: string) {
  switch (t) {
    case 'HUNT': return MapPin;
    case 'TRAIT': return Star;
    case 'CRAFT': return Hammer;
    case 'QUEST': return ScrollText;
    case 'SELL': return Coins;
    case 'HIRE_MERCENARY': return Users;
    case 'JOB_CHANGE': return Sword;
    default: return Package;
  }
}

/** 사냥터(HUNT)를 목표로, 그 앞 준비 스텝들을 한 그룹으로 묶는다. */
function groupBySteps(steps: UserGuideStepDto[]): UserGuideStepDto[][] {
  const groups: UserGuideStepDto[][] = [];
  let cur: UserGuideStepDto[] = [];
  for (const s of steps) {
    cur.push(s);
    if (s.stepType === 'HUNT') { groups.push(cur); cur = []; }
  }
  if (cur.length) groups.push(cur);
  return groups;
}

function marketUrlOf(s: UserGuideStepDto): string | null {
  if (s.linkedItemId) return `/trade?itemId=${s.linkedItemId}`;
  if (s.linkedSetId && s.linkedSetTradeable && s.linkedSetName) return `/trade?q=${encodeURIComponent(s.linkedSetName)}`;
  return null;
}

/** 덱 주력 사천왕 기반 개인화 육성 가이드 — 사냥터 단위 행 정렬. */
export default function GuideChecklistPanel() {
  const [mode, setMode] = useState<Mode>('loading');
  const [recommended, setRecommended] = useState<GuideSummary | null>(null);
  const [detail, setDetail] = useState<UserGuideDetail | null>(null);
  const [collapsed, setCollapsed] = useState(false);
  const [busy, setBusy] = useState(false);

  const bootstrap = useCallback(async () => {
    try {
      const [mine, rec] = await Promise.all([api.getMyGuides(), api.getRecommendedGuides()]);
      if (mine.length > 0) {
        const match = mine.find((m) => rec.some((r) => r.id === m.sourceGuideId)) ?? mine[0];
        setDetail(await api.getUserGuide(match.id));
        setMode('active');
      } else if (rec.length > 0) {
        setRecommended(rec[0]);
        setMode('start');
      } else {
        setMode('none');
      }
    } catch {
      setMode('none');
    }
  }, []);

  useEffect(() => { bootstrap(); }, [bootstrap]);

  async function start() {
    if (!recommended || busy) return;
    setBusy(true);
    try {
      setDetail(await api.startGuide(recommended.id));
      setMode('active');
    } finally { setBusy(false); }
  }

  async function toggle(step: UserGuideStepDto) {
    if (!detail) return;
    const next = !step.checked;
    setDetail({ ...detail, steps: detail.steps.map((s) => (s.id === step.id ? { ...s, checked: next } : s)) });
    try {
      if (next) await api.checkGuideStep(step.id);
      else await api.uncheckGuideStep(step.id);
    } catch {
      setDetail({ ...detail, steps: detail.steps.map((s) => (s.id === step.id ? { ...s, checked: !next } : s)) });
    }
  }

  if (mode === 'loading') {
    return <Panel><div className="py-6 text-center text-sm" style={{ color: 'var(--text-muted)' }}>가이드 불러오는 중…</div></Panel>;
  }

  if (mode === 'none') {
    return (
      <Panel>
        <div className="flex flex-col items-center justify-center py-8 text-center" style={{ color: 'var(--text-muted)' }}>
          <Sword style={{ width: 34, height: 34, opacity: 0.4, marginBottom: 8 }} />
          <p className="text-sm">덱에 사천왕을 편성하면 맞는 육성 가이드가 여기 떠요</p>
          <Link href="/deck" className="text-xs mt-2 inline-block hover:underline" style={{ color: 'var(--brown)' }}>
            전투 계산기에서 덱 설정하기 →
          </Link>
        </div>
      </Panel>
    );
  }

  if (mode === 'start' && recommended) {
    return (
      <Panel>
        <div className="flex items-center gap-3">
          <Avatar url={recommended.targetMercenaryImageUrl} />
          <div className="flex-1 min-w-0">
            <p className="font-semibold" style={{ color: 'var(--text)' }}>{recommended.title}</p>
            <p className="text-xs" style={{ color: 'var(--text-muted)' }}>내 덱 주력에 맞는 육성 로드맵이에요</p>
          </div>
          <button onClick={start} disabled={busy}
            className="text-sm font-medium px-4 py-2 rounded whitespace-nowrap disabled:opacity-50"
            style={{ background: 'var(--brown)', color: 'var(--beige)' }}>
            {busy ? '시작 중…' : '가이드 시작'}
          </button>
        </div>
      </Panel>
    );
  }

  if (!detail) return null;

  const total = detail.steps.length;
  const done = detail.steps.filter((s) => s.checked).length;
  const pct = total > 0 ? Math.round((done / total) * 100) : 0;
  const groups = groupBySteps(detail.steps);

  return (
    <Panel>
      <div className="flex items-center gap-3 mb-3">
        <Avatar url={detail.targetMercenaryImageUrl} />
        <div className="flex-1 min-w-0">
          <p className="font-semibold truncate" style={{ color: 'var(--text)' }}>{detail.title}</p>
          <p className="text-xs" style={{ color: 'var(--text-muted)' }}>{done} / {total} 완료 · {pct}%</p>
        </div>
        <button onClick={() => setCollapsed((c) => !c)} className="p-1" style={{ color: 'var(--text-muted)' }} aria-label="접기/펼치기">
          {collapsed ? <ChevronDown style={{ width: 18, height: 18 }} /> : <ChevronUp style={{ width: 18, height: 18 }} />}
        </button>
      </div>

      <div className="h-2 rounded-full overflow-hidden mb-1" style={{ background: 'var(--beige)' }}>
        <div className="h-full rounded-full transition-all" style={{ width: `${pct}%`, background: 'var(--brown)' }} />
      </div>

      {!collapsed && (
        <div className="mt-3 space-y-2">
          {groups.map((group, gi) => (
            <div
              key={gi}
              className="flex items-start gap-1.5 overflow-x-auto pb-2 pt-1"
              style={{ borderTop: gi > 0 ? '1px dashed var(--border)' : undefined }}
            >
              {group.map((s, i) => {
                const isGoal = s.stepType === 'HUNT' && i === group.length - 1;
                return (
                  <div key={s.id} className="flex items-start">
                    {i > 0 && <ChevronRight className="flex-none mt-5" style={{ width: 14, height: 14, color: 'var(--border)' }} />}
                    <Tile s={s} isGoal={isGoal} onToggle={() => toggle(s)} />
                  </div>
                );
              })}
            </div>
          ))}
        </div>
      )}
    </Panel>
  );
}

function Tile({ s, isGoal, onToggle }: { s: UserGuideStepDto; isGoal: boolean; onToggle: () => void }) {
  const Icon = typeIcon(s.stepType);
  const label = s.stepType === 'HUNT' && s.note ? s.note : s.label;
  const market = marketUrlOf(s);
  return (
    <div className="flex flex-col items-center flex-none" style={{ width: 74 }}>
      <button
        onClick={onToggle}
        className="relative rounded-lg overflow-hidden flex items-center justify-center"
        style={{
          width: 54, height: 54,
          border: `2px solid ${isGoal ? 'var(--brown)' : s.checked ? 'var(--brown)' : 'var(--border)'}`,
          background: 'var(--beige)', opacity: s.checked ? 0.5 : 1,
        }}
        aria-label={`${label} 완료 체크`}
      >
        {s.setPieceIcons && s.setPieceIcons.length > 0 ? (
          <SetCollage icons={s.setPieceIcons} />
        ) : s.displayIconUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={s.displayIconUrl} alt="" className="w-full h-full object-cover" />
        ) : (
          <Icon style={{ width: 22, height: 22, color: isGoal ? 'var(--brown)' : 'var(--text-muted)' }} />
        )}
        {s.checked && (
          <span
            className="absolute inset-0 flex items-center justify-center"
            style={{ background: 'rgba(139,107,74,0.35)' }}
          >
            <Check style={{ width: 22, height: 22, color: 'var(--beige)' }} />
          </span>
        )}
      </button>
      <span
        className="text-[11px] text-center mt-1 leading-tight"
        style={{
          color: isGoal ? 'var(--brown)' : s.checked ? 'var(--text-muted)' : 'var(--text)',
          fontWeight: isGoal ? 600 : 400,
          maxWidth: 74, maxHeight: 28, overflow: 'hidden',
        }}
      >
        {label}
      </span>
      {market && (
        <Link
          href={market}
          onClick={(e) => e.stopPropagation()}
          className="inline-flex items-center gap-0.5 text-[10px] mt-0.5 px-1 py-0.5 rounded"
          style={{ color: 'var(--brown)', border: '1px solid var(--border)' }}
        >
          <ShoppingBag style={{ width: 9, height: 9 }} /> 매물
        </Link>
      )}
    </div>
  );
}

/**
 * 세트 피스 이미지 콜라주 — 게임에 세트 단일 이미지가 없어 피스들을 격자로 보여준다.
 * 유효 피스 수 2/3/5 기준: 2=좌우, 3=위1·아래2, 5=위3·아래2.
 */
function SetCollage({ icons }: { icons: string[] }) {
  const list = icons.slice(0, 5);
  const n = list.length;
  if (n <= 1) {
    // eslint-disable-next-line @next/next/no-img-element
    return <img src={list[0]} alt="" className="w-full h-full object-cover" />;
  }
  const cols = n <= 3 ? 2 : 3;
  const rows = n === 2 ? 1 : 2;
  return (
    <div
      className="w-full h-full"
      style={{ display: 'grid', gridTemplateColumns: `repeat(${cols}, 1fr)`, gridTemplateRows: `repeat(${rows}, 1fr)`, gap: 1, background: 'var(--border)' }}
    >
      {list.map((u, i) => (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          key={i}
          src={u}
          alt=""
          className="w-full h-full object-cover"
          style={n === 3 && i === 0 ? { gridColumn: '1 / -1' } : undefined}
        />
      ))}
    </div>
  );
}

function Panel({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-5">
      <div className="flex items-center gap-2 mb-3">
        <Map style={{ color: 'var(--brown)', width: 18, height: 18 }} />
        <h2 className="font-semibold" style={{ color: 'var(--text)' }}>내 육성 가이드</h2>
      </div>
      {children}
    </div>
  );
}

function Avatar({ url }: { url: string | null | undefined }) {
  return (
    <div className="flex-none rounded-lg overflow-hidden flex items-center justify-center"
      style={{ width: 44, height: 44, background: 'var(--beige)' }}>
      {url ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={url} alt="" className="w-full h-full object-cover" />
      ) : (
        <Sword style={{ width: 20, height: 20, color: 'var(--brown)' }} />
      )}
    </div>
  );
}
