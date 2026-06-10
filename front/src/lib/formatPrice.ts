const EOK = 100_000_000;

/** 억 단위 가격 표시 (예: 2,000,000,000 → 20억, 2,620,000,000 → 26.2억) */
export function formatPrice(price: number | null | undefined): string {
  if (price == null || !Number.isFinite(price)) return '—';

  const eok = Math.round(price) / EOK;
  const rounded = Math.round(eok * 100) / 100;

  if (rounded === 0) return '0억';

  if (Number.isInteger(rounded)) {
    return `${rounded.toLocaleString('en-US')}억`;
  }

  const fixed = rounded.toFixed(2).replace(/\.?0+$/, '');
  const [intPart, decPart] = fixed.split('.');
  const formattedInt = Number(intPart).toLocaleString('en-US');
  return decPart ? `${formattedInt}.${decPart}억` : `${formattedInt}억`;
}
