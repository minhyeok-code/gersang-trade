-- 관심 아이템 시세 조회 최적화: statKeySnapshot + serverSnapshot + confirmedAt 복합 인덱스
-- SET watchKey는 30자를 초과할 수 있으므로 prefix(64) 적용
CREATE INDEX idx_tc_statkey_server_confirmed
    ON trade_confirmed (stat_key_snapshot(64), server_snapshot, confirmed_at DESC);
