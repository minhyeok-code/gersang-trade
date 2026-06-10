'use client';

import { formatDps } from '@/lib/formatDps';

export interface DpsBreakdownValues {
  rawDps?: number | null;
  adjustDps?: number | null;
  finalDps?: number | null;
}

export function DpsBreakdown({
  rawDps,
  adjustDps,
  finalDps,
  layout = 'inline',
  className = '',
  stackTextClass = 'text-[11px]',
}: DpsBreakdownValues & {
  layout?: 'inline' | 'stack';
  className?: string;
  stackTextClass?: string;
}) {
  const items = [
    { label: 'Raw', value: rawDps },
    { label: 'Adjust', value: adjustDps },
    { label: 'Final', value: finalDps },
  ];

  if (layout === 'stack') {
    return (
      <div className={`space-y-1 ${className}`}>
        {items.map((item) => (
          <div key={item.label} className={`flex items-center justify-between gap-3 ${stackTextClass}`}>
            <span style={{ color: 'var(--text-muted)' }}>{item.label}</span>
            <span className="font-semibold tabular-nums" style={{ color: 'var(--brown)' }}>
              {item.value != null ? formatDps(item.value) : '-'}
            </span>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className={`flex flex-wrap gap-x-2 gap-y-0.5 text-[11px] ${className}`}>
      {items.map((item, idx) => (
        <span key={item.label} style={{ color: 'var(--text-muted)' }}>
          {idx > 0 && <span className="mr-2" style={{ color: 'var(--border)' }}>·</span>}
          <span>{item.label} </span>
          <span className="font-semibold tabular-nums" style={{ color: 'var(--brown)' }}>
            {item.value != null ? formatDps(item.value) : '-'}
          </span>
        </span>
      ))}
    </div>
  );
}
