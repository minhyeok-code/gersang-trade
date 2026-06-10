'use client';

/** 기본값 → 디버프 후 값 표시 (동일하면 화살표 생략) */
export function StatWithDebuff({ label, base, debuffed, className = 'text-[10px]' }: {
  label: string;
  base?: number | null;
  debuffed?: number | null;
  className?: string;
}) {
  if (base == null && debuffed == null) return null;
  const showDebuff = debuffed != null && debuffed !== base;
  return (
    <p className={`${className} leading-snug`} style={{ color: 'var(--text-muted)' }}>
      {label}:{' '}
      <span style={{ color: 'var(--text)' }}>{base ?? '-'}</span>
      {showDebuff && (
        <>
          {' → '}
          <span style={{ color: 'var(--brown)' }}>{debuffed}</span>
        </>
      )}
    </p>
  );
}
