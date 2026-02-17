import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
  scenarios: {
    today_load_test: {
      executor: 'constant-vus',
      vus: 50,           // 동시 사용자 수
      duration: '20s',   // 테스트 시간
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% 요청이 500ms 이하
    http_req_failed: ['rate<0.01'],   // 에러율 1% 미만
  },
};

const BASE_URL = 'http://localhost:8080';
const TOKEN = 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzcxMzI4MDA4LCJleHAiOjE3NzEzMzE2MDh9.-GPUwahixdCBDR5W8HNMjM_Lz6JPZGZy9F6n5d3cHwo';

export default function () {
  http.get(`${BASE_URL}/api/attendance/today`, {
    headers: {
      Authorization: TOKEN,
    },
  });

  sleep(1);
}
