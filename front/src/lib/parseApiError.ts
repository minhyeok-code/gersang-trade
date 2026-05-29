/** API fetch 오류 문자열에서 message 필드를 추출한다 */
export function parseApiError(error: unknown): string {
  const raw = String(error);
  const jsonStart = raw.indexOf('{');
  if (jsonStart >= 0) {
    try {
      const body = JSON.parse(raw.slice(jsonStart)) as { message?: string; fieldErrors?: Record<string, string> };
      if (body.message) return body.message;
      if (body.fieldErrors) {
        return Object.values(body.fieldErrors).join(', ');
      }
    } catch {
      /* fall through */
    }
  }
  return raw;
}
