import http from 'k6/http';
import {sleep} from 'k6';

export const options = {
    scenarios: {
        profile_load_test: {
            executor: 'constant-vus',
            vus: 50,
            duration: '20s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = 'http://localhost:8080';
const USER_STORE_ID = 3;
const TOKEN = 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzcxMzMxNzU1LCJleHAiOjE3NzEzMzUzNTV9.IvBrZDnCF1oidWb3f141yQABb6p98WBH8MgXYVmi1nA';

export default function () {
    http.get(
        `${BASE_URL}/api/administration-staff/employees/${USER_STORE_ID}/profile`,
        {
            headers: {
                Authorization: TOKEN,
            },
        }
    );

    sleep(1);
}
