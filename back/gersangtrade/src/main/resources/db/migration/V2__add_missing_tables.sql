-- ============================================================
-- V2__add_missing_tables.sql — V1에 누락된 테이블 4개 추가
-- ============================================================

CREATE TABLE item_skill_effects (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    skill_id   BIGINT      NOT NULL,
    stat_key   VARCHAR(30) NOT NULL,
    stat_value INT         NOT NULL,
    value_type VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_item_skill_effect_skill_stat UNIQUE (skill_id, stat_key),
    CONSTRAINT fk_ise_skill FOREIGN KEY (skill_id) REFERENCES item_skills (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mercenary_skill_effects (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    skill_id   BIGINT      NOT NULL,
    stat_key   VARCHAR(30) NOT NULL,
    stat_value INT         NOT NULL,
    value_type VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_skill_effect_skill_stat UNIQUE (skill_id, stat_key),
    CONSTRAINT fk_mse_skill FOREIGN KEY (skill_id) REFERENCES mercenary_skills (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE skill_coefficients (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    mercenary_skill_id   BIGINT,
    item_skill_id        BIGINT,
    set_granted_skill_id BIGINT,
    row_id               VARCHAR(100),
    coef_str             FLOAT        NOT NULL,
    coef_dex             FLOAT        NOT NULL,
    coef_vit             FLOAT        NOT NULL,
    coef_int             FLOAT        NOT NULL,
    coef_atk             FLOAT        NOT NULL,
    coef_lvl             FLOAT        NOT NULL,
    hit_count            INT          NOT NULL,
    damage_range_factor  FLOAT        NOT NULL,
    skill_type           VARCHAR(20),
    casts_per_second     FLOAT,
    tick_interval_ms     INT,
    confidence           VARCHAR(20),
    measurement_note     TEXT,
    PRIMARY KEY (id),
    CONSTRAINT uq_skill_coefficients_row_id UNIQUE (row_id),
    CONSTRAINT chk_skill_coef_one_skill
        CHECK (mercenary_skill_id IS NOT NULL OR item_skill_id IS NOT NULL OR set_granted_skill_id IS NOT NULL),
    CONSTRAINT fk_sc_merc_skill  FOREIGN KEY (mercenary_skill_id)   REFERENCES mercenary_skills (id),
    CONSTRAINT fk_sc_item_skill  FOREIGN KEY (item_skill_id)        REFERENCES item_skills (id),
    CONSTRAINT fk_sc_set_skill   FOREIGN KEY (set_granted_skill_id) REFERENCES set_granted_skills (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_clear_times (
    id                        BIGINT      NOT NULL AUTO_INCREMENT,
    user_id                   BIGINT      NOT NULL,
    monster_id                BIGINT      NOT NULL,
    deck_id                   BIGINT      NOT NULL,
    deck_snapshot_id          BIGINT      NOT NULL,
    total_resist_pierce       INT,
    total_element_pierce      INT,
    raw_dps                   BIGINT,
    adjust_dps                BIGINT,
    final_dps                 BIGINT      NOT NULL,
    resist_after_debuff       INT,
    effective_monster_element INT,
    resist_pass_rate          DOUBLE,
    clear_time_seconds        INT         NOT NULL,
    is_public                 BOOLEAN     NOT NULL DEFAULT TRUE,
    status                    VARCHAR(20) NOT NULL,
    exp_granted               BOOLEAN     NOT NULL,
    recorded_at               DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_uct_user          FOREIGN KEY (user_id)          REFERENCES users (id),
    CONSTRAINT fk_uct_monster       FOREIGN KEY (monster_id)       REFERENCES monsters (id),
    CONSTRAINT fk_uct_deck_snapshot FOREIGN KEY (deck_snapshot_id) REFERENCES deck_snapshots (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
