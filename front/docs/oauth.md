📢 [거상인] 소셜 로그인 연동 관련 프론트엔드 작업 요청
현재 구글 소셜 로그인 성공 후 백엔드 처리(JWT 토큰 발급 및 리다이렉트)는 정상적으로 완료되고 있습니다.

다만, 백엔드가 세션을 쓰지 않는 JWT(Stateless) 방식을 사용하기 때문에, 로그인 완료 후 API를 호출할 때 프론트엔드에서 토큰을 헤더에 실어 보내주어야 로그인이 유지됩니다. 정상적인 서비스 이용을 위해 아래 2가지 작업을 프론트엔드에 적용해 주세요.

1. 리다이렉트 페이지에서 accessToken 추출 및 저장
구글 로그인 성공 후 백엔드가 아래 주소처럼 URL 파라미터에 accessToken을 붙여서 프론트엔드로 리다이렉트 시켜줍니다.

http://localhost:3000/auth/callback?accessToken=eyJhbGci...

/auth/callback 주소를 처리하는 컴포넌트(또는 페이지)에서 URL의 accessToken을 추출하여 브라우저 저장소(localStorage 또는 Cookie)에 저장해 주세요.

[예시 코드 (React 기준)]

JavaScript
import { useEffect } from 'react';

function AuthCallback() {
    useEffect(() => {
        // 1. URL에서 토큰 추출
        const params = new URLSearchParams(window.location.search);
        const token = params.get('accessToken');

        if (token) {
            // 2. 로컬 스토리지에 토큰 저장
            localStorage.setItem('accessToken', token);
            
            // 3. 로그인 완료 후 메인 페이지 또는 기존 페이지로 리다이렉트
            window.location.href = '/'; 
        }
    }, []);

    return <div>로그인 처리 중입니다...</div>;
}
2. 백엔드 API 요청 시 Authorization 헤더 추가
이후 백엔드로 보내는 모든 API 요청(예: GET /api/servers)을 보낼 때, 저장해 둔 토큰을 Authorization 헤더에 Bearer  형태로 포함해서 보내주어야 합니다. 그렇지 않으면 백엔드에서 인증되지 않은 익명 사용자로 처리됩니다.

[예시 코드 (Axios 인터셉터 기준)]

JavaScript
import axios from 'axios';

// API 인스턴스 생성
const api = axios.create({
    baseURL: 'http://localhost:8080'
});

// 요청 인터셉터를 통해 모든 요청에 자동으로 토큰 삽입
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
}, (error) => {
    return Promise.reject(error);
});

// 호출 예시
// api.get('/api/servers').then(res => console.log(res.data));
참고사항: 기존 브라우저에 남아있는 JSESSIONID 쿠키는 백엔드에서 세션을 사용하지 않으므로 로그인 유지에 영향을 주지 않습니다. 위 두 가지만 적용하면 로그인 상태 변경 및 API 호출이 정상 작동합니다.