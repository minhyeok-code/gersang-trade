/** @type {import('next').NextConfig} */

// 로컬 개발: 127.0.0.1:8080 (IPv6 resolve 문제 회피)
// Docker 컨테이너: http://app:8080 (내부 서비스명)
const BACKEND_URL = process.env.BACKEND_URL || 'http://127.0.0.1:8080';

const nextConfig = {
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${BACKEND_URL}/api/:path*`,
      },
      {
        source: '/ws/:path*',
        destination: `${BACKEND_URL}/ws/:path*`,
      },
      {
        source: '/ws',
        destination: `${BACKEND_URL}/ws`,
      },
      // /auth/callback 은 Next.js 페이지 — /auth/* 전체 프록시 시 콜백 페이지가 백엔드로 넘어가 토큰 저장 실패
      {
        source: '/auth/refresh',
        destination: `${BACKEND_URL}/auth/refresh`,
      },
      {
        source: '/auth/logout',
        destination: `${BACKEND_URL}/auth/logout`,
      },
      {
        source: '/admin/:path*',
        destination: `${BACKEND_URL}/admin/:path*`,
      },
      {
        source: '/oauth2/:path*',
        destination: `${BACKEND_URL}/oauth2/:path*`,
      },
      {
        source: '/login/oauth2/code/:path*',
        destination: `${BACKEND_URL}/login/oauth2/code/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
