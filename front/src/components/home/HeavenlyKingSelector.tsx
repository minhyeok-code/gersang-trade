'use client';

import { useEffect, useState } from 'react';
import { api, type HeavenlyKingDto, type HeavenlyKingVariantDto } from '@/lib/api';

/** 속성별 강조색·이모지 (거상 메달 규칙: 화=빨강 / 풍=초록 / 뇌=노랑 / 수=파랑). */
const ELEMENT_META: Record<string, { color: string; emoji: string }> = {
  FIRE: { color: '#DE3A2E', emoji: '🔥' },
  WIND: { color: '#3FAE3A', emoji: '🍃' },
  THUNDER: { color: '#F0C020', emoji: '⚡' },
  WATER: { color: '#2596E6', emoji: '💧' },
};

type Phase = 'NORMAL' | 'AWAKENED';
type Selection = { element: string; phase: Phase; id: number } | null;

/**
 * 홈 사천왕 선택 UI.
 * 속성 4종 카드 = 요소 배지 + 일반/각성 전신 스탠딩 2컷(텍스트 없음, 이미지가 속성·천왕·단계를 표현).
 * 스탠딩 이미지는 백엔드가 S3 mercenary-standing/{id}.png 규칙으로 URL을 조합해 내려준다.
 * 배경 테마는 기존(라이트)을 그대로 사용한다.
 */
export default function HeavenlyKingSelector() {
  const [kings, setKings] = useState<HeavenlyKingDto[] | null>(null);
  const [error, setError] = useState(false);
  const [sel, setSel] = useState<Selection>(null);
  const [failed, setFailed] = useState<Set<number>>(new Set());

  useEffect(() => {
    api.getHeavenlyKings().then(setKings).catch(() => setError(true));
  }, []);

  function pick(element: string, phase: Phase, v: HeavenlyKingVariantDto | null) {
    if (!v) return;
    setSel({ element, phase, id: v.id });
  }
  function markFailed(id: number) {
    setFailed((prev) => new Set(prev).add(id));
  }

  // 조용히 숨김 — 콜드스타트 앵커라 로드 실패 시 노출하지 않는다.
  if (error) return null;

  const cards: (HeavenlyKingDto | null)[] = kings ?? [null, null, null, null];

  return (
    <section>
      <h2 className="font-serif text-lg font-semibold mb-3" style={{ color: 'var(--text)' }}>
        사천왕 육성 루트
      </h2>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {cards.map((k, i) => {
          const meta = (k && ELEMENT_META[k.element]) || { color: 'var(--brown)', emoji: '◆' };
          return (
            <div
              key={k?.element ?? i}
              className="relative rounded-xl pt-7 px-3 pb-3"
              style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
            >
              {/* 요소 배지 (추후 실제 거상 메달 이미지로 교체 예정) */}
              <div
                className="absolute left-1/2 -translate-x-1/2 -top-4 w-10 h-10 rounded-full grid place-items-center text-lg"
                style={{ background: meta.color, color: '#fff', border: '3px solid var(--bg)' }}
              >
                {meta.emoji}
              </div>
              <div className="flex gap-2">
                <Slot
                  v={k?.normal ?? null}
                  color={meta.color}
                  selected={!!k?.normal && sel?.id === k.normal.id}
                  failed={failed}
                  onErr={markFailed}
                  onClick={() => k && pick(k.element, 'NORMAL', k.normal)}
                />
                <Slot
                  v={k?.awakened ?? null}
                  color={meta.color}
                  selected={!!k?.awakened && sel?.id === k.awakened.id}
                  failed={failed}
                  onErr={markFailed}
                  onClick={() => k && pick(k.element, 'AWAKENED', k.awakened)}
                  awakened
                />
              </div>
            </div>
          );
        })}
      </div>
      {sel && (
        <p className="text-xs mt-4 text-center" style={{ color: 'var(--text-muted)' }}>
          선택됨 · 목표 몬스터 · 루트 목록은 준비 중입니다
        </p>
      )}
    </section>
  );
}

function Slot({
  v,
  color,
  selected,
  failed,
  onErr,
  onClick,
  awakened = false,
}: {
  v: HeavenlyKingVariantDto | null;
  color: string;
  selected: boolean;
  failed: Set<number>;
  onErr: (id: number) => void;
  onClick: () => void;
  awakened?: boolean;
}) {
  const showImg = !!v?.standingImageUrl && !failed.has(v.id);
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={!v}
      className="flex-1 rounded-lg overflow-hidden transition-all"
      style={{
        border: `2px solid ${selected ? color : 'var(--border)'}`,
        opacity: v ? 1 : 0.45,
        cursor: v ? 'pointer' : 'default',
      }}
    >
      <div className="w-full grid place-items-center" style={{ aspectRatio: '3 / 4', background: 'var(--beige)' }}>
        {showImg ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={v!.standingImageUrl!}
            alt=""
            className="w-full h-full object-cover"
            onError={() => onErr(v!.id)}
          />
        ) : (
          <span className="text-3xl" style={{ color: 'var(--text-muted)' }}>
            {awakened ? '✨' : '🧍'}
          </span>
        )}
      </div>
    </button>
  );
}
