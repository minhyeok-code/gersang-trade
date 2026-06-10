'use client';

import type { DpsValueEvaluationResponseDto, PriceSourceDto } from '@/lib/api';
import { formatEfficiencyPerEok } from '@/lib/valueTest';
import { ChevronDown, ChevronUp, TrendingUp } from 'lucide-react';
import { useState } from 'react';

const PRICE_SOURCE_LABEL: Record<PriceSourceDto, string> = {
  USER_INPUT: '직접 입력',
  TRADE_STAT: '시세',
  MIXED: '혼합',
  MISSING: '가격 없음',
};

interface ValueTestResultPanelProps {
  result: DpsValueEvaluationResponseDto;
  loading?: boolean;
}

export default function ValueTestResultPanel({ result, loading }: ValueTestResultPanelProps) {
  const [showDetail, setShowDetail] = useState(false);

  const finalBefore = result.before.finalDps;
  const finalAfter = result.after.finalDps;
  const finalDelta = result.delta.finalDps;
  const finalRate = result.increaseRate.finalDps;
  const efficiency = result.efficiencyPerEok.finalDps;
  const positive = finalDelta > 0;

  return (
    <div
      style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
      className="rounded-xl p-5 space-y-4"
    >
      <div className="flex items-center gap-2">
        <TrendingUp style={{ color: 'var(--brown)', width: 18, height: 18 }} />
        <h2 className="font-serif text-lg font-semibold" style={{ color: 'var(--text)' }}>
          가성비 결과
        </h2>
        {loading && (
          <span className="text-xs" style={{ color: 'var(--text-muted)' }}>
            계산 중…
          </span>
        )}
      </div>

      {/* 핵심 KPI — final DPS 기준 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <KpiCard label="현재 DPS" value={finalBefore.toLocaleString()} sub="final" />
        <KpiCard label="적용 후 DPS" value={finalAfter.toLocaleString()} sub="final" highlight />
        <KpiCard
          label="증가"
          value={`${positive ? '+' : ''}${finalDelta.toLocaleString()}`}
          sub={`${positive ? '+' : ''}${finalRate.toFixed(2)}%`}
          positive={positive}
        />
        <KpiCard
          label="억당 DPS 증가"
          value={efficiency != null ? `${efficiency > 0 ? '+' : ''}${efficiency.toFixed(2)}%` : '—'}
          sub={result.formattedPrice ? `${result.formattedPrice} 기준` : '가격 없음'}
          highlight
        />
      </div>

      {/* Before → After 바 */}
      <div className="space-y-1">
        <div className="flex justify-between text-xs" style={{ color: 'var(--text-muted)' }}>
          <span>{finalBefore.toLocaleString()}</span>
          <span>{finalAfter.toLocaleString()}</span>
        </div>
        <div className="h-2 rounded-full overflow-hidden" style={{ background: 'var(--bg)' }}>
          <div
            className="h-full rounded-full transition-all"
            style={{
              width: `${finalBefore > 0 ? Math.min(100, (finalAfter / finalBefore) * 100) : 100}%`,
              background: positive ? 'var(--brown)' : 'var(--text-disabled)',
            }}
          />
        </div>
      </div>

      {/* 가격 메타 */}
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <span style={{ color: 'var(--text-muted)' }}>가격</span>
        <span className="font-serif font-bold" style={{ color: 'var(--brown)' }}>
          {result.formattedPrice ?? '—'}
        </span>
        <span
          className="text-xs px-2 py-0.5 rounded"
          style={{ background: 'var(--bg)', color: 'var(--text-muted)' }}
        >
          {PRICE_SOURCE_LABEL[result.priceSource]}
        </span>
        {result.tradeCount != null && (
          <span className="text-xs" style={{ color: 'var(--text-muted)' }}>
            거래 {result.tradeCount}건
          </span>
        )}
        {result.evaluationId != null && (
          <span className="text-xs" style={{ color: 'var(--text-muted)' }}>
            기록 #{result.evaluationId}
          </span>
        )}
      </div>

      {efficiency != null && efficiency <= 0 && (
        <p className="text-xs px-3 py-2 rounded" style={{ background: 'var(--bg)', color: 'var(--text-muted)' }}>
          DPS 증가가 없거나 가격 대비 효율이 낮습니다.
        </p>
      )}

      {/* raw/adjust 상세 */}
      <button
        type="button"
        onClick={() => setShowDetail((v) => !v)}
        className="flex items-center gap-1 text-xs"
        style={{ color: 'var(--text-muted)' }}
      >
        {showDetail ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
        raw / adjust 상세
      </button>
      {showDetail && (
        <div className="grid grid-cols-3 gap-2 text-xs">
          <DetailCol title="raw" before={result.before.raw} after={result.after.raw} delta={result.delta.raw} rate={result.increaseRate.raw} eff={result.efficiencyPerEok.raw} />
          <DetailCol title="adjust" before={result.before.adjust} after={result.after.adjust} delta={result.delta.adjust} rate={result.increaseRate.adjust} eff={result.efficiencyPerEok.adjust} />
          <DetailCol title="final" before={result.before.finalDps} after={result.after.finalDps} delta={result.delta.finalDps} rate={result.increaseRate.finalDps} eff={result.efficiencyPerEok.finalDps} />
        </div>
      )}
    </div>
  );
}

function KpiCard({
  label,
  value,
  sub,
  highlight,
  positive,
}: {
  label: string;
  value: string;
  sub?: string;
  highlight?: boolean;
  positive?: boolean;
}) {
  return (
    <div
      className="rounded-lg p-3"
      style={{
        background: highlight ? 'var(--bg)' : 'transparent',
        border: `1px solid ${highlight ? 'var(--brown)' : 'var(--border)'}`,
      }}
    >
      <div className="text-xs mb-1" style={{ color: 'var(--text-muted)' }}>
        {label}
      </div>
      <div
        className="font-serif text-lg font-bold"
        style={{
          color: positive === false ? 'var(--text-muted)' : positive === true ? 'var(--brown)' : 'var(--text)',
        }}
      >
        {value}
      </div>
      {sub && (
        <div className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>
          {sub}
        </div>
      )}
    </div>
  );
}

function DetailCol({
  title,
  before,
  after,
  delta,
  rate,
  eff,
}: {
  title: string;
  before: number;
  after: number;
  delta: number;
  rate: number;
  eff: number | null;
}) {
  return (
    <div className="rounded p-2" style={{ background: 'var(--bg)' }}>
      <div className="font-medium mb-1" style={{ color: 'var(--brown)' }}>
        {title}
      </div>
      <div style={{ color: 'var(--text-muted)' }}>전 {before.toLocaleString()}</div>
      <div style={{ color: 'var(--text)' }}>후 {after.toLocaleString()}</div>
      <div>Δ {delta >= 0 ? '+' : ''}{delta.toLocaleString()} ({rate.toFixed(2)}%)</div>
      <div>{formatEfficiencyPerEok(eff)}</div>
    </div>
  );
}
