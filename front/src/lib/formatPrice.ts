const EOK = 100_000_000;

/** 정수 가격(전) 입력란 — 천단위 콤마 */
export function formatPriceInput(value: string): string {
  const digits = value.replace(/\D/g, '');
  if (!digits) return '';
  return Number(digits).toLocaleString('en-US');
}

/** 콤마 포함 입력 문자열 → 숫자(전) */
export function parsePriceInput(value: string): number {
  const digits = value.replace(/\D/g, '');
  if (!digits) return 0;
  return Number(digits);
}

/** 숫자 → 거래가 입력란 초기값 */
export function formatPriceInputFromNumber(price: number | null | undefined): string {
  if (price == null || !Number.isFinite(price)) return '';
  return Math.round(price).toLocaleString('en-US');
}

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
