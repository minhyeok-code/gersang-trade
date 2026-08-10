// 거래글 목록 부하테스트 — GET /api/listings (DB 읽기 위주, 세트명 생성 로직 포함)
import http from 'k6/http';
import { sleep } from 'k6';
import { BASE_URL, stages, thresholds, checkOk, pick, qs } from './lib/common.js';

export const options = { stages: stages(), thresholds };

const bundleTypes = ['MATERIAL_BUNDLE', 'EQUIPMENT_SINGLE', 'EQUIPMENT_SET'];
const keywords = ['', '', '', '세트', '주술', '보석']; // 빈 문자열 비중을 높여 전체 조회도 섞음

export default function () {
  // 필터 조합을 랜덤하게 섞어 다양한 쿼리 경로를 태운다.
  const params = {
    page: Math.floor(Math.random() * 5),
    size: 20,
  };
  if (Math.random() < 0.5) params.bundleType = pick(bundleTypes);
  const kw = pick(keywords);
  if (kw) params.keyword = kw;

  const res = http.get(`${BASE_URL}/api/listings?${qs(params)}`, { tags: { name: 'listings' } });
  checkOk(res);

  sleep(1); // 실사용자 think-time 모사
}
