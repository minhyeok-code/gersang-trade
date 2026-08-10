// 가성비 계산 부하테스트 — POST /api/calculator (연산 + DB 다중 조회 혼합)
import http from 'k6/http';
import { sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { BASE_URL, stages, thresholds, checkOk, pick, JSON_HEADERS } from './lib/common.js';

export const options = { stages: stages(), thresholds };

// 요청 payload는 init 단계에서 1회 로드해 VU들이 공유(SharedArray)한다.
const payloads = new SharedArray('calculator', () =>
  JSON.parse(open('../payloads/calculator.json'))
);

export default function () {
  const body = JSON.stringify(pick(payloads));
  const res = http.post(`${BASE_URL}/api/calculator`, body, {
    ...JSON_HEADERS,
    tags: { name: 'calculator' },
  });
  checkOk(res);
  sleep(1);
}
