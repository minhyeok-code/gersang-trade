#!/bin/bash
# Blue-Green 배포 스크립트
# 사용법: ./deploy.sh
# 실행 위치: docker-compose.prod.yml 이 있는 프로젝트 루트
set -e

COMPOSE="docker compose -f docker-compose.prod.yml"
SLOT_FILE=".active-slot"
ACTIVE=$(cat "$SLOT_FILE" 2>/dev/null || echo "blue")
NEXT=$([ "$ACTIVE" = "blue" ] && echo "green" || echo "blue")

echo "[deploy] 현재=$ACTIVE → 다음=$NEXT"

# 1. 새 슬롯 빌드·시작
if [ "$NEXT" = "green" ]; then
    $COMPOSE --profile green up -d --build app-green
else
    $COMPOSE up -d --build app-blue
fi

# 2. 헬스체크 (최대 2분, 5초 간격)
echo "[deploy] app-$NEXT 헬스체크 대기 중..."
for i in $(seq 1 24); do
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' "gersang-trade-app-$NEXT" 2>/dev/null || echo "")
    if [ "$STATUS" = "healthy" ]; then
        echo "[deploy] app-$NEXT healthy 확인"
        break
    fi
    if [ "$i" -eq 24 ]; then
        echo "[error] 헬스체크 실패 (2분 초과) — 롤백"
        $COMPOSE stop "app-$NEXT" 2>/dev/null || true
        exit 1
    fi
    echo "  대기 중... ($i/24, 상태: ${STATUS:-starting})"
    sleep 5
done

# 3. nginx upstream 전환 (무중단 — nginx -s reload는 기존 커넥션 유지)
echo "upstream spring_boot { server app-$NEXT:8080; }" > nginx/upstream.conf
$COMPOSE exec nginx nginx -s reload
echo "[deploy] nginx → app-$NEXT 전환 완료"

# 4. 이전 슬롯 종료
$COMPOSE stop "app-$ACTIVE"
echo "[deploy] app-$ACTIVE 종료"

echo "$NEXT" > "$SLOT_FILE"
echo "[deploy] 완료. active=$NEXT"
