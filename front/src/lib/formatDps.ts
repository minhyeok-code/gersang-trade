/** DPS 숫자 축약 표기 */
export function formatDps(dps: number | null | undefined): string {
  if (dps == null || !Number.isFinite(dps)) return '-';
  if (dps >= 1_000_000) return `${(dps / 1_000_000).toFixed(2)}M`;
  if (dps >= 1_000) return `${(dps / 1_000).toFixed(1)}K`;
  return dps.toLocaleString();
}
