CREATE TABLE user_watch_targets (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    target_type   VARCHAR(10)  NOT NULL COMMENT 'ITEM | SET',
    watch_key     VARCHAR(255) NOT NULL,
    item_id       BIGINT,
    set_id        BIGINT,
    composition   VARCHAR(20)  COMMENT 'SetComposition enum: GAMTU | BYEON | BANSSANG | FULL | FULL_BANSSANG',
    ritual_count  INT,
    ritual_mark   VARCHAR(10),
    sort_order    INT          NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_uwt_user_watch_key (user_id, watch_key),
    KEY idx_uwt_user_id (user_id),
    CONSTRAINT fk_uwt_user   FOREIGN KEY (user_id) REFERENCES users (id)            ON DELETE CASCADE,
    CONSTRAINT fk_uwt_item   FOREIGN KEY (item_id) REFERENCES items (id)            ON DELETE SET NULL,
    CONSTRAINT fk_uwt_set    FOREIGN KEY (set_id)  REFERENCES equipment_sets (id)   ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
