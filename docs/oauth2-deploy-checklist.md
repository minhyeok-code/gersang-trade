# 소셜 로그인 배포 환경 설정 체크리스트

Google + Naver OAuth2를 배포 환경(EC2 + 도메인)에서 동작시키기 위해 해야 할 작업 목록.

---

## 전제 조건

- EC2 서버에 도메인이 연결되어 있어야 함 (예: `api.gersang-trade.com`)
- HTTPS 인증서가 적용되어 있어야 함 (Let's Encrypt 등)
  - 쿠키의 `Secure` 속성, OAuth2 Callback URL이 모두 HTTPS를 요구함

---

## 1. Google OAuth2

### 1-1. Google Cloud Console 설정

1. [https://console.cloud.google.com](https://console.cloud.google.com) 접속
2. 기존 프로젝트 선택 (또는 신규 생성)
3. **APIs & Services → OAuth consent screen**
   - User Type: `External`
   - 앱 이름, 지원 이메일 입력
   - Scopes: `email`, `profile` 추가
   - **Publishing status: `In production`으로 변경** (테스트 상태면 등록된 계정만 로그인 가능)
4. **APIs & Services → Credentials → OAuth 2.0 Client IDs** 선택
5. **Authorized redirect URIs**에 운영 URL 추가:
   ```
   https://api.gersang-trade.com/login/oauth2/code/google
   ```
   - 로컬 개발용은 `http://localhost:8080/login/oauth2/code/google` 별도 유지

### 1-2. 확인할 환경변수

| 환경변수 | 설명 |
|---------|------|
| `GOOGLE_CLIENT_ID` | Client ID (Credentials 페이지) |
| `GOOGLE_CLIENT_SECRET` | Client Secret (Credentials 페이지) |

---

## 2. Naver OAuth2

### 2-1. 네이버 개발자 센터 애플리케이션 등록

1. [https://developers.naver.com](https://developers.naver.com) 접속 → **Application → 내 애플리케이션 → 애플리케이션 등록**
2. 애플리케이션 이름 입력 (예: `거상 아이템 거래`)
3. **사용 API**: `네아로 (네이버 아이디로 로그인)` 선택
   - 제공 정보: **이름**, **이메일** 필수 선택
4. **서비스 환경**: PC 웹 선택
5. **서비스 URL**: `https://gersang-trade.com` (프론트엔드 도메인)
6. **Callback URL** 두 개 등록:
   ```
   https://api.gersang-trade.com/login/oauth2/code/naver
   http://localhost:8080/login/oauth2/code/naver
   ```
   - 네이버는 Callback URL이 등록된 것만 허용하므로 로컬 URL도 반드시 추가
7. 등록 완료 후 **Client ID**, **Client Secret** 복사

### 2-2. 확인할 환경변수

| 환경변수 | 설명 |
|---------|------|
| `NAVER_CLIENT_ID` | 애플리케이션 등록 후 발급된 Client ID |
| `NAVER_CLIENT_SECRET` | 애플리케이션 등록 후 발급된 Client Secret |

---

## 3. EC2 서버 환경변수 설정

EC2에서 Spring Boot 실행 시 아래 환경변수를 모두 주입해야 함.  
`/etc/environment` 또는 systemd 서비스 파일의 `Environment=`에 추가하거나, `.env` 파일로 관리.

```bash
# DB
DB_URL=jdbc:mysql://<RDS_ENDPOINT>:3306/gersang-trade-db?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=<DB_USERNAME>
DB_PASSWORD=<DB_PASSWORD>

# JPA / Flyway
DDL_AUTO=validate
FLYWAY_ENABLED=true

# OAuth2 - Google
GOOGLE_CLIENT_ID=<구글_클라이언트_ID>
GOOGLE_CLIENT_SECRET=<구글_클라이언트_시크릿>

# OAuth2 - Naver
NAVER_CLIENT_ID=<네이버_클라이언트_ID>
NAVER_CLIENT_SECRET=<네이버_클라이언트_시크릿>

# JWT
JWT_SECRET=<최소_32자_이상의_랜덤_문자열>

# 인증 성공 후 프론트엔드 리다이렉트 URL
OAUTH2_REDIRECT_URI=https://gersang-trade.com/auth/callback

# 쿠키 Secure 속성 — HTTPS 환경에서는 반드시 true
COOKIE_SECURE=true

# AWS S3
AWS_ACCESS_KEY=<AWS_ACCESS_KEY>
AWS_SECRET_KEY=<AWS_SECRET_KEY>
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=gersang-trade-images
```

---

## 4. 프론트엔드 연동 확인

소셜 로그인 버튼 클릭 시 이동할 URL (백엔드가 자동 생성):

| Provider | 로그인 시작 URL |
|---------|---------------|
| Google | `https://api.gersang-trade.com/oauth2/authorization/google` |
| Naver | `https://api.gersang-trade.com/oauth2/authorization/naver` |

인증 성공 후 백엔드는 아래 URL로 리다이렉트함:
```
https://gersang-trade.com/auth/callback?accessToken=<JWT_AT>
```
- 프론트엔드는 이 `accessToken`을 받아 메모리(또는 localStorage)에 저장
- Refresh Token은 `HttpOnly` 쿠키(`refreshToken`)로 자동 전달됨

---

## 5. CORS 설정 확인

백엔드 CORS 설정에 운영 프론트엔드 도메인이 허용되어 있는지 확인:
- 허용 Origin: `https://gersang-trade.com`
- `allowCredentials: true` — Refresh Token 쿠키 전송에 필요

---

## 6. 배포 전 최종 체크리스트

- [ ] Google Cloud Console — Redirect URI에 운영 URL 추가
- [ ] Google Cloud Console — OAuth consent screen `In production` 상태
- [ ] 네이버 개발자 센터 — 애플리케이션 등록 및 Callback URL 추가
- [ ] EC2 환경변수 전체 설정 (`COOKIE_SECURE=true` 포함)
- [ ] `OAUTH2_REDIRECT_URI`가 운영 프론트엔드 URL로 설정됨
- [ ] HTTPS 인증서 적용 완료
- [ ] 프론트엔드 로그인 버튼이 운영 백엔드 URL로 요청
- [ ] 로그인 → 콜백 → JWT 수신 흐름 운영 환경에서 직접 테스트
