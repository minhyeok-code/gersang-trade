-- 가성비 평가: 평가 당시 덱(baseline) 스냅샷 + 재평가용 요청 JSON
ALTER TABLE dps_value_evaluations
    ADD COLUMN baseline_deck_snapshot_id BIGINT NULL,
    ADD COLUMN request_json TEXT NULL,
    ADD CONSTRAINT fk_dps_eval_baseline_snapshot
        FOREIGN KEY (baseline_deck_snapshot_id) REFERENCES deck_snapshots (id);
