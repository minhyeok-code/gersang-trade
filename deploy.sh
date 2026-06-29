#!/bin/bash
# 로컬 → EC2 배포 스크립트
# 사용법: ./deploy.sh
# 전제: EC2에 코드가 git clone 되어 있고 .env 파일이 존재해야 함

# ── 접속 정보 (최초 1회 설정) ───────────────────────────────────────────────
EC2_USER=ec2-user                        # Amazon Linux: ec2-user / Ubuntu: ubuntu
EC2_HOST=                                # EC2 퍼블릭 IP 또는 도메인 (Elastic IP 권장)
SSH_KEY=~/.ssh/gersang-trade.pem        # 발급받은 키 페어 경로
REMOTE_DIR=/home/ec2-user/gersangTrade  # EC2 내 프로젝트 경로
# ─────────────────────────────────────────────────────────────────────────────

if [ -z "$EC2_HOST" ]; then
    echo "[error] EC2_HOST를 설정하세요 (deploy.sh 상단)"
    exit 1
fi

echo "[deploy] EC2($EC2_HOST)에 접속합니다..."

ssh -i "$SSH_KEY" "$EC2_USER@$EC2_HOST" bash << EOF
set -e
cd $REMOTE_DIR

# 1. 최신 코드 반영
echo "[원격] git pull..."
git pull origin main

# 2. 현재/다음 슬롯 결정
ACTIVE=\$(cat .active-slot 2>/dev/null || echo "blue")
NEXT=\$([ "\$ACTIVE" = "blue" ] && echo "green" || echo "blue")
echo "[원격] 현재=\$ACTIVE → 다음=\$NEXT"

COMPOSE="docker compose -f docker-compose.prod.yml"

# 3. 새 슬롯 빌드·시작
if [ "\$NEXT" = "green" ]; then
    \$COMPOSE --profile green up -d --build app-green
else
    \$COMPOSE up -d --build app-blue
fi

# 4. 헬스체크 (최대 2분, 5초 간격)
echo "[원격] app-\$NEXT 헬스체크 대기 중..."
for i in \$(seq 1 24); do
    STATUS=\$(docker inspect --format='{{.State.Health.Status}}' "gersang-trade-app-\$NEXT" 2>/dev/null || echo "")
    if [ "\$STATUS" = "healthy" ]; then
        echo "[원격] app-\$NEXT healthy 확인"
        break
    fi
    if [ "\$i" -eq 24 ]; then
        echo "[error] 헬스체크 실패 — 롤백"
        \$COMPOSE stop "app-\$NEXT" 2>/dev/null || true
        exit 1
    fi
    echo "  대기 중... (\$i/24, 상태: \${STATUS:-starting})"
    sleep 5
done

# 5. nginx upstream 전환 (무중단)
echo "upstream spring_boot { server app-\$NEXT:8080; }" > nginx/upstream.conf
\$COMPOSE exec nginx nginx -s reload
echo "[원격] nginx → app-\$NEXT 전환 완료"

# 6. 이전 슬롯 종료
\$COMPOSE stop "app-\$ACTIVE"

echo "\$NEXT" > .active-slot
echo "[원격] 배포 완료. active=\$NEXT"
EOF

echo "[deploy] 완료"
