// DPS 계산 부하테스트 — POST /api/calculator/dps (가장 무거운 연산 + N+1 의심 경로)
import http from 'k6/http';
import { sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { BASE_URL, stages, thresholds, checkOk, pick, JSON_HEADERS } from './lib/common.js';

export const options = { stages: stages(), thresholds };

const payloads = new SharedArray('dps', () =>
  JSON.parse(open('../payloads/dps.json'))
);

export default function () {
  const body = JSON.stringify(pick(payloads));
  const res = http.post(`${BASE_URL}/api/calculator/dps`, body, {
    ...JSON_HEADERS,
    tags: { name: 'dps' },
  });
  checkOk(res);
  sleep(1);
}
