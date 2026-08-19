'use client';

import { useEffect, useState } from 'react';
import { api, type HeavenlyKingDto } from '@/lib/api';

type Phase = 'NORMAL' | 'AWAKENED';
type ElKey = 'FIRE' | 'WIND' | 'THUNDER' | 'WATER';

interface ElDef {
  key: ElKey;
  el: string;
  hanja: string;
  king: string;
  color: string;
  badge: string;
  normal: string | null;
  awaken: string | null;
}

/**
 * 홈 사천왕 배너 선택 UI.
 *
 * 4속성 카드 그리드는 항상 노출된다(선택 후에도 유지).
 *   각 카드 = 상단 속성뱃지(nature_baege 아이콘) + 세로 슬롯 2개(일반 / 각성 사천왕 스탠딩 이미지).
 *   스탠딩 이미지는 백엔드 API(getHeavenlyKings)의 normal/awakened.standingImageUrl 에서 내려온다.
 *   이미지가 없거나(로드 실패 포함) API 실패 시 플레이스홀더로 폴백한다.
 *   일반 슬롯을 누르면 해당 속성 · 일반으로, 각성 슬롯을 누르면 해당 속성 · 각성으로 선택되고
 *   선택된 슬롯은 속성 색상으로 강조된다.
 *
 * 선택 시 그리드 아래에 배너 영역이 뜬다(일반/각성 토글 + 하단 배너).
 *   배너는 정적 파일(front/public/banners). 파일명 규칙:
 *     일반 = {천왕}_banner.png / 각성 = {천왕}_banner_awaken.png
 *   아직 없는 파일은 로드 실패 시 자동 폴백한다(각성 미보유 → 일반 배너, 일반까지 없으면 "준비 중").
 *   따라서 이후 규칙에 맞는 파일을 public/banners에 떨구기만 하면 코드 수정 없이 반영된다.
 */
const ELEMENTS: ElDef[] = [
  { key: 'FIRE',    el: '화', hanja: '火', king: '지국천왕', color: '#DE3A2E', badge: '/nature_baege/fire.png',    normal: '/banners/jiguk_banner.png',     awaken: '/banners/jiguk_banner_awaken.png' },
  { key: 'WIND',    el: '풍', hanja: '風', king: '광목천왕', color: '#3FAE3A', badge: '/nature_baege/wind.png',    normal: '/banners/gwangmok_banner.png',  awaken: '/banners/gwangmok_banner_awaken.png' },
  { key: 'THUNDER', el: '뇌', hanja: '雷', king: '증장천왕', color: '#F0C020', badge: '/nature_baege/thunder.png', normal: '/banners/jeungjang_banner.png', awaken: '/banners/jeungjang_banner_awaken.png' },
  { key: 'WATER',   el: '수', hanja: '水', king: '다문천왕', color: '#2596E6', badge: '/nature_baege/water.png',   normal: '/banners/damoon_banner.png',    awaken: '/banners/damoon_banner_awaken.png' },
];

export default function HeavenlyKingBanner() {
  const [selected, setSelected] = useState<number | null>(null);
  const [phase, setPhase] = useState<Phase>('NORMAL');
  // 로드 실패한 배너 이미지 URL 모음 → 존재하지 않는 배너를 자동으로 폴백 처리
  const [failed, setFailed] = useState<Set<string>>(new Set());
  // 속성별 사천왕 정보(스탠딩 이미지 소스) — element 키로 조회
  const [kings, setKings] = useState<Record<string, HeavenlyKingDto>>({});
  // 로드 실패한 스탠딩 이미지 URL 모음 → 플레이스홀더로 폴백 처리
  const [failedStanding, setFailedStanding] = useState<Set<string>>(new Set());

  const active = selected !== null ? ELEMENTS[selected] : null;

  // 사천왕 스탠딩 이미지 로드. 실패 시 조용히 무시하고 플레이스홀더로 폴백한다.
  useEffect(() => {
    api
      .getHeavenlyKings()
      .then((list) => {
        const map: Record<string, HeavenlyKingDto> = {};
        for (const k of list) map[k.element] = k;
        setKings(map);
      })
      .catch(() => {
        /* 폴백: 플레이스홀더 그대로 사용 */
      });
  }, []);

  function pickSlot(i: number, p: Phase) {
    setSelected(i);
    setPhase(p);
  }

  // 카드 슬롯에 표시할 사천왕 스탠딩 이미지 URL. 없거나 실패한 URL이면 null(→ 플레이스홀더).
  function standingUrl(el: ElDef, p: Phase): string | null {
    const king = kings[el.key];
    const u = p === 'AWAKENED' ? king?.awakened?.standingImageUrl : king?.normal?.standingImageUrl;
    return u && !failedStanding.has(u) ? u : null;
  }
  function markStandingFailed(url: string) {
    setFailedStanding((prev) => new Set(prev).add(url));
  }

  // 표시할 배너 URL 결정: 각성 → (없으면) 일반 → (없으면) null(플레이스홀더)
  function resolveSrc(el: ElDef, p: Phase): string | null {
    const chain = p === 'AWAKENED' ? [el.awaken, el.normal] : [el.normal];
    for (const u of chain) {
      if (u && !failed.has(u)) return u;
    }
    return null;
  }

  const src = active ? resolveSrc(active, phase) : null;
  const usingFallback = !!active && phase === 'AWAKENED' && !!src && src === active.normal;

  return (
    <section>
      <h2 className="font-serif text-lg font-semibold mb-1" style={{ color: 'var(--text)' }}>
        사천왕 육성 루트
      </h2>
      <p className="text-sm mb-4" style={{ color: 'var(--text-muted)' }}>
        속성을 선택하면 일반 · 각성 배너를 볼 수 있어요
      </p>

      {/* 4속성 카드 그리드 (상시 노출 · 속성뱃지 + 일반/각성 스탠딩 슬롯) */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {ELEMENTS.map((e, i) => (
          <div
            key={e.key}
            className="relative rounded-xl pt-7 px-3 pb-3"
            style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
          >
            {/* 속성뱃지 */}
            <div
              className="absolute left-1/2 -translate-x-1/2 -top-4 w-10 h-10 rounded-full grid place-items-center overflow-hidden"
              style={{ background: 'var(--card)', border: '3px solid var(--bg)' }}
              title={`${e.el}(${e.hanja}) 속성`}
            >
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={e.badge} alt="" className="w-full h-full object-contain p-0.5" />
            </div>
            <div className="flex gap-2">
              <StandingSlot
                url={standingUrl(e, 'NORMAL')}
                color={e.color}
                label="일반"
                selected={selected === i && phase === 'NORMAL'}
                onClick={() => pickSlot(i, 'NORMAL')}
                onErr={markStandingFailed}
              />
              <StandingSlot
                url={standingUrl(e, 'AWAKENED')}
                color={e.color}
                label="각성"
                awakened
                selected={selected === i && phase === 'AWAKENED'}
                onClick={() => pickSlot(i, 'AWAKENED')}
                onErr={markStandingFailed}
              />
            </div>
          </div>
        ))}
      </div>

      {/* 선택 시: 일반/각성 토글 + 하단 배너 */}
      {active && (
        <div className="fade-in mt-6">
          <div className="flex items-center justify-between gap-3 mb-3 flex-wrap">
            <div className="flex items-center gap-2">
              <span className="text-base font-semibold" style={{ color: active.color }}>
                {active.king}
              </span>
              <span className="text-xs" style={{ color: 'var(--text-muted)' }}>
                {active.el}({active.hanja}) 속성
              </span>
            </div>
            <PhaseToggle phase={phase} onChange={setPhase} color={active.color} />
          </div>

          <div
            className="relative w-full overflow-hidden rounded-xl"
            style={{
              border: '1px solid var(--border)',
              aspectRatio: '1942 / 809',
              background: 'var(--beige)',
            }}
          >
            {src ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                key={`${active.key}-${src}`}
                src={src}
                alt={`${active.king} ${phase === 'AWAKENED' ? '각성' : '일반'} 배너`}
                className="w-full h-full object-cover fade-in"
                onError={() => setFailed((prev) => new Set(prev).add(src))}
              />
            ) : (
              <div className="w-full h-full grid place-items-center">
                <span className="text-sm" style={{ color: 'var(--text-muted)' }}>
                  {active.king} 배너 준비 중
                </span>
              </div>
            )}
          </div>

          {usingFallback && (
            <p className="text-xs mt-2" style={{ color: 'var(--text-muted)' }}>
              ※ 각성 배너 이미지가 아직 없어 임시로 일반 배너를 표시하고 있어요.
            </p>
          )}
        </div>
      )}
    </section>
  );
}

/** 카드 안 세로 슬롯 — 일반/각성 사천왕 스탠딩 이미지(없으면 플레이스홀더). 선택 시 속성 색상 강조. */
function StandingSlot({
  url,
  color,
  label,
  selected,
  awakened = false,
  onClick,
  onErr,
}: {
  url: string | null;
  color: string;
  label: string;
  selected: boolean;
  awakened?: boolean;
  onClick: () => void;
  onErr: (url: string) => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      title={label}
      aria-pressed={selected}
      className="flex-1 rounded-lg overflow-hidden transition-all hover:brightness-95"
      style={{
        border: `2px solid ${selected ? color : 'var(--border)'}`,
        boxShadow: selected ? `0 0 0 2px ${color}55` : 'none',
      }}
    >
      <div className="relative w-full grid place-items-center" style={{ aspectRatio: '3 / 4', background: 'var(--header-bg)' }}>
        {url ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={url}
            alt=""
            className="w-full h-full object-cover"
            style={{ objectPosition: 'top center' }}
            onError={() => onErr(url)}
          />
        ) : (
          <span className="text-3xl" style={{ color: 'var(--text-muted)' }}>
            {awakened ? '✨' : '🧍'}
          </span>
        )}
        <span
          className="absolute left-1 bottom-1 px-1.5 py-0.5 rounded text-[10px] font-semibold leading-none"
          style={{ background: selected ? color : `${color}E6`, color: '#fff' }}
        >
          {label}
        </span>
      </div>
    </button>
  );
}

function PhaseToggle({
  phase,
  onChange,
  color,
}: {
  phase: Phase;
  onChange: (p: Phase) => void;
  color: string;
}) {
  const isAwk = phase === 'AWAKENED';
  return (
    <div
      className="relative flex select-none rounded-full p-1"
      style={{ background: 'var(--beige)', border: '1px solid var(--border)' }}
      role="tablist"
      aria-label="일반/각성 선택"
    >
      <span
        aria-hidden
        className="absolute top-1 bottom-1 rounded-full transition-transform duration-200 ease-out"
        style={{
          width: 'calc(50% - 4px)',
          left: 4,
          background: color,
          transform: isAwk ? 'translateX(100%)' : 'translateX(0)',
          boxShadow: `0 2px 8px ${color}66`,
        }}
      />
      <button
        role="tab"
        aria-selected={!isAwk}
        onClick={() => onChange('NORMAL')}
        className="relative z-10 w-[74px] py-1.5 text-sm font-semibold rounded-full transition-colors"
        style={{ color: isAwk ? 'var(--text-muted)' : '#fff' }}
      >
        일반
      </button>
      <button
        role="tab"
        aria-selected={isAwk}
        onClick={() => onChange('AWAKENED')}
        className="relative z-10 w-[74px] py-1.5 text-sm font-semibold rounded-full transition-colors"
        style={{ color: isAwk ? '#fff' : 'var(--text-muted)' }}
      >
        각성
      </button>
    </div>
  );
}
