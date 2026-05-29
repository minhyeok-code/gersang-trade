/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [
      // Windows에서 localhost가 ::1(IPv6)로 resolve되면 백엔드 연결 실패(ECONNREFUSED)가 날 수 있어 127.0.0.1 고정
      {
        source: '/api/:path*',
        destination: 'http://127.0.0.1:8080/api/:path*',
      },
      // /auth/callback 은 Next.js 페이지 — /auth/* 전체 프록시 시 콜백 페이지가 백엔드로 넘어가 토큰 저장 실패
      {
        source: '/auth/refresh',
        destination: 'http://127.0.0.1:8080/auth/refresh',
      },
      {
        source: '/auth/logout',
        destination: 'http://127.0.0.1:8080/auth/logout',
      },
      {
        source: '/admin/:path*',
        destination: 'http://127.0.0.1:8080/admin/:path*',
      },
      {
        source: '/oauth2/:path*',
        destination: 'http://127.0.0.1:8080/oauth2/:path*',
      },
      // Google/Naver OAuth 코드 콜백 — 로그인 시작을 :3000 프록시로 할 때 redirect_uri가 localhost:3000 기준
      {
        source: '/login/oauth2/code/:path*',
        destination: 'http://127.0.0.1:8080/login/oauth2/code/:path*',
      },
    ];
  },
};

module.exports = nextConfig;
