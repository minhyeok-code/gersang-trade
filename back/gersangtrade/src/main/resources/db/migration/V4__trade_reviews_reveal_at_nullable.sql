-- 거래 평가: reveal_at은 생성 직후 null 허용(2일 경과 배치 설정) 또는 생성 시 예정 시각 저장
ALTER TABLE trade_reviews
    MODIFY COLUMN reveal_at DATETIME(6) NULL;
