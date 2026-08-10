// 혼합 시나리오 — 실사용자 흐름을 모사해 listing/calculator/dps를 가중치로 섞어 호출한다.
// 각 엔드포인트를 독립 scenario로 분리해 개별 지표(tag=name)로 Grafana에서 나눠 볼 수 있다.
import http from 'k6/http';
import { sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { BASE_URL, thresholds, checkOk, pick, qs, JSON_HEADERS } from './lib/common.js';

const calcPayloads = new SharedArray('calculator', () =>
  JSON.parse(open('../payloads/calculator.json'))
);
const dpsPayloads = new SharedArray('dps', () =>
  JSON.parse(open('../payloads/dps.json'))
);

// 트래픽 비중 가정: 목록 브라우징이 가장 많고, 계산기는 그보다 적게 호출된다.
export const options = {
  scenarios: {
    listing: {
      executor: 'ramping-vus',
      exec: 'listing',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 60 },
        { duration: '2m', target: 60 },
        { duration: '30s', target: 0 },
      ],
    },
    calculator: {
      executor: 'ramping-vus',
      exec: 'calculator',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 20 },
        { duration: '2m', target: 20 },
        { duration: '30s', target: 0 },
      ],
    },
    dps: {
      executor: 'ramping-vus',
      exec: 'dps',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 15 },
        { duration: '2m', target: 15 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds,
};

const bundleTypes = ['MATERIAL_BUNDLE', 'EQUIPMENT_SINGLE', 'EQUIPMENT_SET'];

export function listing() {
  const params = { page: Math.floor(Math.random() * 5), size: 20 };
  if (Math.random() < 0.5) params.bundleType = pick(bundleTypes);
  const res = http.get(`${BASE_URL}/api/listings?${qs(params)}`, { tags: { name: 'listings' } });
  checkOk(res);
  sleep(1);
}

export function calculator() {
  const res = http.post(`${BASE_URL}/api/calculator`, JSON.stringify(pick(calcPayloads)), {
    ...JSON_HEADERS,
    tags: { name: 'calculator' },
  });
  checkOk(res);
  sleep(1);
}

export function dps() {
  const res = http.post(`${BASE_URL}/api/calculator/dps`, JSON.stringify(pick(dpsPayloads)), {
    ...JSON_HEADERS,
    tags: { name: 'dps' },
  });
  checkOk(res);
  sleep(1);
}
