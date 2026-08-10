// 공용 설정·헬퍼 — 모든 k6 스크립트에서 import 한다.
import { check } from 'k6';

// 대상 서버. 기본 localhost:8080 (도커로 앱 띄운 경우 동일 포트).
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 부하 프로파일 — 실행 시 PROFILE 환경변수로 선택. 예) k6 run -e PROFILE=stress ...
// smoke: 기능 확인용 소량, ramp: 기본 상승 부하, stress: 임계점 탐색, spike: 급증 부하
const PROFILES = {
  smoke: [{ duration: '30s', target: 5 }],
  ramp: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 50 },
    { duration: '1m', target: 100 },
    { duration: '30s', target: 0 },
  ],
  stress: [
    { duration: '1m', target: 50 },
    { duration: '2m', target: 200 },
    { duration: '2m', target: 400 },
    { duration: '1m', target: 0 },
  ],
  spike: [
    { duration: '10s', target: 20 },
    { duration: '10s', target: 300 },
    { duration: '30s', target: 300 },
    { duration: '10s', target: 20 },
  ],
};

export function stages() {
  const p = __ENV.PROFILE || 'ramp';
  return PROFILES[p] || PROFILES.ramp;
}

// 합격 기준 — 초과 시 k6 종료코드 비정상(=CI 실패 처리 가능)
export const thresholds = {
  http_req_failed: ['rate<0.01'], // 에러율 1% 미만
  http_req_duration: ['p(95)<800', 'p(99)<2000'], // 응답시간 목표(ms)
};

export const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

export function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

// 쿼리스트링 조립 (k6 goja에는 URLSearchParams가 없어 직접 구성)
export function qs(params) {
  return Object.keys(params)
    .filter((k) => params[k] !== undefined && params[k] !== null && params[k] !== '')
    .map((k) => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`)
    .join('&');
}

export function checkOk(res) {
  check(res, {
    'status is 2xx': (r) => r.status >= 200 && r.status < 300,
  });
}
