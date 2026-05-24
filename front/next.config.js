/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [
      // Windows에서 localhost가 ::1(IPv6)로 resolve되면 백엔드 연결 실패(ECONNREFUSED)가 날 수 있어 127.0.0.1 고정
      {
        source: '/api/:path*',
        destination: 'http://127.0.0.1:8080/api/:path*',
      },
      {
        source: '/auth/:path*',
        destination: 'http://127.0.0.1:8080/auth/:path*',
      },
      {
        source: '/admin/:path*',
        destination: 'http://127.0.0.1:8080/admin/:path*',
      },
      {
        source: '/oauth2/:path*',
        destination: 'http://127.0.0.1:8080/oauth2/:path*',
      },
    ];
  },
};

module.exports = nextConfig;
