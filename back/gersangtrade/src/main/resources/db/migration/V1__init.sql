-- ============================================================
-- V1__init.sql — 전체 스키마 초기화
-- 엔티티 기반으로 작성. 외래키 의존 순서에 따라 테이블 생성.
-- 모든 테이블명 복수형.
-- ============================================================

-- ============================================================
-- 1. 카탈로그 기반 (참조 없음 또는 자기 참조만)
-- ============================================================

CREATE TABLE servers (
    server_id  INT          NOT NULL,
    name       VARCHAR(20)  NOT NULL,
    is_active  BOOLEAN      NOT NULL,
    PRIMARY KEY (server_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE equipment_sets (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL,
    total_pieces INT          NOT NULL,
    is_tradeable BOOLEAN      NOT NULL DEFAULT TRUE,
    enhancement  INT,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE set_granted_skills (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    skill_key             VARCHAR(100) NOT NULL,
    skill_name            VARCHAR(100) NOT NULL,
    skill_behavior_type   VARCHAR(20),
    stat_source           VARCHAR(20),
    trigger_source        VARCHAR(20),
    trigger_every_n       INT,
    trigger_base_skill_key VARCHAR(100),
    note                  TEXT,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rituals (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    display_name        VARCHAR(50) NOT NULL,
    ritual_type         VARCHAR(20) NOT NULL,
    success_mark        VARCHAR(20) NOT NULL,
    great_success_mark  VARCHAR(20),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE spirits (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    name                 VARCHAR(100) NOT NULL,
    nature               VARCHAR(20)  NOT NULL,
    grade                VARCHAR(20)  NOT NULL,
    acquire_condition    TEXT,
    special_effect_note  TEXT,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    UNIQUE KEY uq_spirits_nature_grade (nature, grade),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE deck_buff_sources (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    source_type VARCHAR(20) NOT NULL,
    source_id   BIGINT      NOT NULL,
    name        VARCHAR(50) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE gonmyeong_level_stats (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    level     INT         NOT NULL,
    stat_type VARCHAR(30) NOT NULL,
    value     INT         NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_gonmyeong_level_stats (level, stat_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE gaho_level_stats (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    level     INT         NOT NULL,
    stat_type VARCHAR(30) NOT NULL,
    value     INT         NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_gaho_level_stats (level, stat_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 2. 아이템 (Item 기반 테이블)
-- ============================================================

CREATE TABLE items (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(100) NOT NULL,
    type           VARCHAR(20)  NOT NULL,
    trade_category VARCHAR(50),
    image_url      VARCHAR(500),
    item_key       VARCHAR(100),
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_items_item_key (item_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE material_items (
    item_id         BIGINT      NOT NULL,
    stack_unit_name VARCHAR(20),
    PRIMARY KEY (item_id),
    CONSTRAINT fk_material_items_item FOREIGN KEY (item_id) REFERENCES items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item_skills (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    skill_name          VARCHAR(100) NOT NULL,
    skill_key           VARCHAR(100),
    skill_behavior_type VARCHAR(20),
    replaces_base_skill BOOLEAN      NOT NULL DEFAULT FALSE,
    trigger_every_n     INT,
    trigger_base_skill_key VARCHAR(100),
    note                TEXT,
    PRIMARY KEY (id),
    CONSTRAINT uq_item_skills_skill_name UNIQUE (skill_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item_skill_mappings (
    id       BIGINT NOT NULL AUTO_INCREMENT,
    item_id  BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_item_skill_mappings UNIQUE (item_id, skill_id),
    CONSTRAINT fk_ism_item  FOREIGN KEY (item_id)  REFERENCES items (id),
    CONSTRAINT fk_ism_skill FOREIGN KEY (skill_id) REFERENCES item_skills (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

-- ============================================================
-- 3. 용병
-- ============================================================

CREATE TABLE mercenaries (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    mercenary_key   VARCHAR(100),
    category        VARCHAR(50),
    nation          VARCHAR(20),
    nature          VARCHAR(20),
    nature_value    INT,
    mercenary_type  VARCHAR(30),
    is_coming_soon  BOOLEAN      NOT NULL DEFAULT FALSE,
    image_url       VARCHAR(500),
    crawled_at      DATETIME(6),
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_mercenaries_name (name),
    UNIQUE KEY uq_mercenaries_key  (mercenary_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mercenary_stats (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    mercenary_id  BIGINT      NOT NULL,
    stat_key      VARCHAR(30) NOT NULL,
    stat_value    INT         NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_mercenary_stats_merc_stat (mercenary_id, stat_key),
    CONSTRAINT fk_mercenary_stats_merc FOREIGN KEY (mercenary_id) REFERENCES mercenaries (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mercenary_materials (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    mercenary_id      BIGINT NOT NULL,
    item_id           BIGINT NOT NULL,
    required_quantity INT    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_merc_materials_merc FOREIGN KEY (mercenary_id) REFERENCES mercenaries (id),
    CONSTRAINT fk_merc_materials_item FOREIGN KEY (item_id)      REFERENCES items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mercenary_skills (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    mercenary_id  BIGINT       NOT NULL,
    skill_name    VARCHAR(100) NOT NULL,
    skill_key     VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT uq_mercenary_skills_merc_skill UNIQUE (mercenary_id, skill_name),
    CONSTRAINT fk_mercenary_skills_merc FOREIGN KEY (mercenary_id) REFERENCES mercenaries (id)
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

CREATE TABLE mercenary_characteristics (
    id                           BIGINT       NOT NULL AUTO_INCREMENT,
    mercenary_id                 BIGINT       NOT NULL,
    characteristic_key           VARCHAR(100) NOT NULL,
    name                         VARCHAR(50)  NOT NULL,
    point                        INT,
    description                  VARCHAR(500),
    required_characteristic_key  VARCHAR(100),
    apply_type                   VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_merc_char_key (characteristic_key),
    CONSTRAINT fk_merc_char_merc FOREIGN KEY (mercenary_id) REFERENCES mercenaries (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mercenary_characteristic_levels (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    characteristic_id  BIGINT       NOT NULL,
    label              VARCHAR(100) NOT NULL,
    level              INT          NOT NULL,
    amount             VARCHAR(20)  NOT NULL,
    amount_value       FLOAT,
    stat_type          VARCHAR(30),
    PRIMARY KEY (id),
    UNIQUE KEY uq_merc_char_level_label (characteristic_id, label, level),
    CONSTRAINT fk_merc_char_level_char FOREIGN KEY (characteristic_id) REFERENCES mercenary_characteristics (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE player_character_details (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    mercenary_id  BIGINT      NOT NULL,
    nation        VARCHAR(20) NOT NULL,
    job_type      VARCHAR(10) NOT NULL,
    gender        VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_player_char_merc (mercenary_id),
    CONSTRAINT fk_player_char_merc FOREIGN KEY (mercenary_id) REFERENCES mercenaries (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 4. 장비 세트 관련
-- ============================================================

CREATE TABLE equipment_items (
    item_id          BIGINT      NOT NULL,
    equipment_kind   VARCHAR(20) NOT NULL,
    slot             VARCHAR(20) NOT NULL,
    set_id           BIGINT,
    ritual_applicable BOOLEAN    NOT NULL DEFAULT FALSE,
    equip_slot       VARCHAR(30),
    mercenary_id     BIGINT,
    enhancement      INT,
    has_slot_option  BOOLEAN     NOT NULL DEFAULT FALSE,
    is_sain_sword    BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (item_id),
    CONSTRAINT fk_equipment_items_item FOREIGN KEY (item_id)      REFERENCES items (id),
    CONSTRAINT fk_equipment_items_set  FOREIGN KEY (set_id)       REFERENCES equipment_sets (id),
    CONSTRAINT fk_equipment_items_merc FOREIGN KEY (mercenary_id) REFERENCES mercenaries (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE equipment_set_pieces (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    set_id              BIGINT      NOT NULL,
    equipment_item_id   BIGINT      NOT NULL,
    slot                VARCHAR(20) NOT NULL,
    piece_count         INT         NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_set_piece_slot (set_id, slot),
    CONSTRAINT fk_esp_set  FOREIGN KEY (set_id)            REFERENCES equipment_sets (id),
    CONSTRAINT fk_esp_item FOREIGN KEY (equipment_item_id) REFERENCES equipment_items (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE equipment_set_effects (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    equipment_set_id BIGINT      NOT NULL,
    required_pieces  INT         NOT NULL,
    stat_type        VARCHAR(30) NOT NULL,
    stat_value       INT         NOT NULL,
    stat_unit        VARCHAR(10) NOT NULL,
    element          VARCHAR(20) NOT NULL DEFAULT 'NONE',
    scope            VARCHAR(10) NOT NULL DEFAULT 'SELF',
    PRIMARY KEY (id),
    CONSTRAINT uq_set_effects_set_pieces_stat_element_scope
        UNIQUE (equipment_set_id, required_pieces, stat_type, element, scope),
    CONSTRAINT fk_set_effects_set FOREIGN KEY (equipment_set_id) REFERENCES equipment_sets (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE equipment_set_skill_effects (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    set_id              BIGINT NOT NULL,
    required_pieces     INT    NOT NULL,
    enhancement         INT,
    set_granted_skill_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_set_skill_effect UNIQUE (set_id, required_pieces, enhancement),
    CONSTRAINT fk_set_skill_effect_set   FOREIGN KEY (set_id)               REFERENCES equipment_sets (id),
    CONSTRAINT fk_set_skill_effect_skill FOREIGN KEY (set_granted_skill_id) REFERENCES set_granted_skills (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item_stats (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    item_id   BIGINT      NOT NULL,
    stat_type VARCHAR(30) NOT NULL,
    element   VARCHAR(20) NOT NULL DEFAULT 'NONE',
    value     INT         NOT NULL,
    stat_unit VARCHAR(10) NOT NULL,
    scope     VARCHAR(30) NOT NULL DEFAULT 'SELF',
    PRIMARY KEY (id),
    CONSTRAINT uq_item_stats_item_stat_element_scope UNIQUE (item_id, stat_type, element, scope),
    CONSTRAINT fk_item_stats_item FOREIGN KEY (item_id) REFERENCES items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item_mercenary_restrictions (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    item_id       BIGINT      NOT NULL,
    mercenary_id  BIGINT,
    category      VARCHAR(50),
    PRIMARY KEY (id),
    CONSTRAINT fk_imr_item FOREIGN KEY (item_id)      REFERENCES items (id),
    CONSTRAINT fk_imr_merc FOREIGN KEY (mercenary_id) REFERENCES mercenaries (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 5. 주술 관련
-- ============================================================

CREATE TABLE ritual_stats (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    ritual_id BIGINT      NOT NULL,
    outcome   VARCHAR(20) NOT NULL,
    stat_type VARCHAR(30) NOT NULL,
    stat_value INT        NOT NULL,
    stat_unit VARCHAR(10) NOT NULL,
    element   VARCHAR(20) NOT NULL DEFAULT 'NONE',
    PRIMARY KEY (id),
    UNIQUE KEY uq_ritual_stats (ritual_id, outcome, stat_type, element),
    CONSTRAINT fk_ritual_stats_ritual FOREIGN KEY (ritual_id) REFERENCES rituals (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ritual_applicabilities (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    ritual_id           BIGINT NOT NULL,
    equipment_item_id   BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ritual_applicabilities (ritual_id, equipment_item_id),
    CONSTRAINT fk_ra_ritual FOREIGN KEY (ritual_id)         REFERENCES rituals (id),
    CONSTRAINT fk_ra_equip  FOREIGN KEY (equipment_item_id) REFERENCES equipment_items (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ritual_set_effects (
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    ritual_id              BIGINT      NOT NULL,
    equipment_set_id       BIGINT      NOT NULL,
    outcome                VARCHAR(20) NOT NULL,
    required_ritual_pieces INT         NOT NULL,
    stat_type              VARCHAR(30) NOT NULL,
    stat_value             INT         NOT NULL,
    stat_unit              VARCHAR(10) NOT NULL,
    element                VARCHAR(20) NOT NULL DEFAULT 'NONE',
    PRIMARY KEY (id),
    UNIQUE KEY uq_ritual_set_effects (ritual_id, outcome, equipment_set_id, required_ritual_pieces, stat_type),
    CONSTRAINT fk_rse_ritual FOREIGN KEY (ritual_id)        REFERENCES rituals (id),
    CONSTRAINT fk_rse_set    FOREIGN KEY (equipment_set_id) REFERENCES equipment_sets (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 6. 보석
-- ============================================================

CREATE TABLE gems (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    gem_grade  VARCHAR(20)  NOT NULL,
    ritual_id  BIGINT,
    image_url  VARCHAR(500),
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_gems_name_grade_ritual UNIQUE (name, gem_grade, ritual_id),
    CONSTRAINT fk_gems_ritual FOREIGN KEY (ritual_id) REFERENCES rituals (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 7. 정령
-- ============================================================

CREATE TABLE spirit_buffs (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    spirit_id  BIGINT      NOT NULL,
    stat_type  VARCHAR(30) NOT NULL,
    element    VARCHAR(20) NOT NULL,
    stat_unit  VARCHAR(20) NOT NULL,
    value      FLOAT       NOT NULL,
    target     VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_spirit_buffs_spirit FOREIGN KEY (spirit_id) REFERENCES spirits (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 8. 전설장수
-- ============================================================

CREATE TABLE legend_generals (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    mercenary_id  BIGINT      NOT NULL,
    type          VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_legend_generals_merc (mercenary_id),
    CONSTRAINT fk_legend_generals_merc FOREIGN KEY (mercenary_id) REFERENCES mercenaries (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE legend_general_passives (
    id                    BIGINT      NOT NULL AUTO_INCREMENT,
    legend_general_id     BIGINT      NOT NULL,
    stat_type             VARCHAR(30) NOT NULL,
    element               VARCHAR(10) NOT NULL,
    value_type            VARCHAR(15) NOT NULL,
    target                VARCHAR(10) NOT NULL,
    start_level           INT,
    start_value           FLOAT,
    increment_per_levels  INT,
    increment_value       FLOAT,
    max_value             FLOAT,
    PRIMARY KEY (id),
    CONSTRAINT fk_lgp_legend_generals FOREIGN KEY (legend_general_id) REFERENCES legend_generals (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE legend_general_characteristics (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    legend_general_id       BIGINT      NOT NULL,
    characteristic_index    INT         NOT NULL,
    name                    VARCHAR(50),
    level                   INT         NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_lgc_gen_index_level (legend_general_id, characteristic_index, level),
    CONSTRAINT fk_lgc_legend_generals FOREIGN KEY (legend_general_id) REFERENCES legend_generals (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE characteristic_effects (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    characteristic_id  BIGINT      NOT NULL,
    stat_type          VARCHAR(30) NOT NULL,
    element            VARCHAR(10) NOT NULL,
    value_type         VARCHAR(15) NOT NULL,
    value              FLOAT       NOT NULL,
    target             VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ce_characteristic FOREIGN KEY (characteristic_id) REFERENCES legend_general_characteristics (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 9. 스킬 계수
-- ============================================================

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

-- ============================================================
-- 10. 덱 버프 소스
-- ============================================================

CREATE TABLE deck_buffs (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    source_id   BIGINT      NOT NULL,
    stat_type   VARCHAR(30) NOT NULL,
    element     VARCHAR(10) NOT NULL,
    value_type  VARCHAR(15) NOT NULL,
    value       FLOAT       NOT NULL,
    target      VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_deck_buffs_source FOREIGN KEY (source_id) REFERENCES deck_buff_sources (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 11. 몬스터
-- ============================================================

CREATE TABLE monsters (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    name                VARCHAR(100) NOT NULL,
    image_url           VARCHAR(500),
    hp                  BIGINT,
    hitting_resistance  INT,
    magic_resistance    INT,
    element_value       INT,
    element             VARCHAR(20),
    hidden              BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uq_monsters_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 12. 사용자
-- ============================================================

CREATE TABLE users (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    oauth_provider   VARCHAR(20)  NOT NULL,
    oauth_id         VARCHAR(255) NOT NULL,
    nickname         VARCHAR(50)  NOT NULL,
    email            VARCHAR(100) NOT NULL,
    role             VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    blocked_until    DATETIME(6),
    block_reason     VARCHAR(500),
    grade            VARCHAR(20)  NOT NULL,
    grade_step       INT,
    total_exp        BIGINT       NOT NULL DEFAULT 0,
    manner_score     INT          NOT NULL DEFAULT 60,
    trade_count      INT          NOT NULL DEFAULT 0,
    server_id        INT,
    game_nickname    VARCHAR(50),
    game_access_time VARCHAR(100),
    profile_image_url VARCHAR(500),
    deleted_at       DATETIME(6),
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_oauth_provider_id UNIQUE (oauth_provider, oauth_id),
    CONSTRAINT fk_users_server FOREIGN KEY (server_id) REFERENCES servers (server_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refresh_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token       VARCHAR(512) NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_tokens_user_id (user_id),
    KEY idx_refresh_tokens_user_id (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_watch_targets (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    target_type  VARCHAR(10)  NOT NULL,
    watch_key    VARCHAR(255) NOT NULL,
    item_id      BIGINT,
    set_id       BIGINT,
    composition  VARCHAR(20),
    ritual_count INT,
    ritual_mark  VARCHAR(20),
    sort_order   INT          NOT NULL DEFAULT 0,
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_uwt_user_watch_key (user_id, watch_key),
    KEY idx_uwt_user_id (user_id),
    CONSTRAINT fk_uwt_user FOREIGN KEY (user_id) REFERENCES users (id)  ON DELETE CASCADE,
    CONSTRAINT fk_uwt_item FOREIGN KEY (item_id)  REFERENCES items (id)  ON DELETE SET NULL,
    CONSTRAINT fk_uwt_set  FOREIGN KEY (set_id)   REFERENCES equipment_sets (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 13. 덱
-- ============================================================

CREATE TABLE user_decks (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    user_id             BIGINT       NOT NULL,
    name                VARCHAR(50)  NOT NULL,
    is_active           BOOLEAN      NOT NULL DEFAULT FALSE,
    spirit_1_id         BIGINT,
    spirit_2_id         BIGINT,
    jinbeop_source_id   BIGINT,
    cheungjin_source_id BIGINT,
    attr_x_value        INT,
    total_res_down      INT,
    gonmyeong_level     INT,
    gaho_level          INT,
    created_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_decks_user      FOREIGN KEY (user_id)             REFERENCES users (id),
    CONSTRAINT fk_user_decks_spirit1   FOREIGN KEY (spirit_1_id)         REFERENCES spirits (id),
    CONSTRAINT fk_user_decks_spirit2   FOREIGN KEY (spirit_2_id)         REFERENCES spirits (id),
    CONSTRAINT fk_user_decks_jinbeop   FOREIGN KEY (jinbeop_source_id)   REFERENCES deck_buff_sources (id),
    CONSTRAINT fk_user_decks_cheungjin FOREIGN KEY (cheungjin_source_id) REFERENCES deck_buff_sources (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_deck_members (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    deck_id       BIGINT      NOT NULL,
    mercenary_id  BIGINT      NOT NULL,
    level         INT         NOT NULL DEFAULT 250,
    bonus_target  VARCHAR(20) NOT NULL,
    bonus_amount  INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_deck_members (deck_id, mercenary_id),
    CONSTRAINT fk_udm_deck FOREIGN KEY (deck_id)      REFERENCES user_decks (id),
    CONSTRAINT fk_udm_merc FOREIGN KEY (mercenary_id) REFERENCES mercenaries (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_deck_member_slots (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    deck_member_id      BIGINT      NOT NULL,
    equipment_item_id   BIGINT      NOT NULL,
    slot                VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_udms_member_slot (deck_member_id, slot),
    CONSTRAINT fk_udms_member FOREIGN KEY (deck_member_id)    REFERENCES user_deck_members (id),
    CONSTRAINT fk_udms_equip  FOREIGN KEY (equipment_item_id) REFERENCES equipment_items (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_deck_member_slot_rituals (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    deck_member_slot_id BIGINT      NOT NULL,
    ritual_id           BIGINT      NOT NULL,
    outcome             VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_udmsr_slot (deck_member_slot_id),
    CONSTRAINT fk_udmsr_slot   FOREIGN KEY (deck_member_slot_id) REFERENCES user_deck_member_slots (id),
    CONSTRAINT fk_udmsr_ritual FOREIGN KEY (ritual_id)           REFERENCES rituals (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_deck_member_equips (
    id                 BIGINT   NOT NULL AUTO_INCREMENT,
    deck_member_id     BIGINT   NOT NULL,
    equipment_set_id   BIGINT,
    equipment_item_id  BIGINT,
    enhance_level      INT,
    set_piece_count    INT,
    has_affinity       BOOLEAN  NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uq_udme_member (deck_member_id),
    CONSTRAINT fk_udme_member FOREIGN KEY (deck_member_id)    REFERENCES user_deck_members (id),
    CONSTRAINT fk_udme_set    FOREIGN KEY (equipment_set_id)  REFERENCES equipment_sets (id),
    CONSTRAINT fk_udme_equip  FOREIGN KEY (equipment_item_id) REFERENCES equipment_items (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_deck_member_characteristics (
    id                 BIGINT NOT NULL AUTO_INCREMENT,
    deck_member_id     BIGINT NOT NULL,
    characteristic_id  BIGINT NOT NULL,
    selected_level     INT    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_udmc (deck_member_id, characteristic_id),
    CONSTRAINT fk_udmc_member FOREIGN KEY (deck_member_id)    REFERENCES user_deck_members (id),
    CONSTRAINT fk_udmc_char   FOREIGN KEY (characteristic_id) REFERENCES mercenary_characteristics (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 14. 거래 등록
-- ============================================================

CREATE TABLE trade_listings (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    seller_id   BIGINT       NOT NULL,
    server      VARCHAR(30)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    price       BIGINT       NOT NULL,
    note        VARCHAR(500),
    hidden      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at  DATETIME(6),
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_trade_listings_seller FOREIGN KEY (seller_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE listing_bundles (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    listing_id       BIGINT       NOT NULL,
    bundle_type      VARCHAR(30)  NOT NULL,
    equipment_set_id BIGINT,
    title_override   VARCHAR(200),
    PRIMARY KEY (id),
    CONSTRAINT fk_listing_bundles_listing FOREIGN KEY (listing_id)       REFERENCES trade_listings (id),
    CONSTRAINT fk_listing_bundles_set     FOREIGN KEY (equipment_set_id) REFERENCES equipment_sets (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bundle_lines (
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    bundle_id             BIGINT NOT NULL,
    item_id               BIGINT NOT NULL,
    equipment_set_piece_id BIGINT,
    quantity              INT    NOT NULL,
    sort_order            INT    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_bundle_lines_bundle FOREIGN KEY (bundle_id)              REFERENCES listing_bundles (id),
    CONSTRAINT fk_bundle_lines_item   FOREIGN KEY (item_id)                REFERENCES items (id),
    CONSTRAINT fk_bundle_lines_piece  FOREIGN KEY (equipment_set_piece_id) REFERENCES equipment_set_pieces (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bundle_equipment_details (
    bundle_line_id           BIGINT      NOT NULL,
    equipment_item_id        BIGINT      NOT NULL,
    equipment_kind_snapshot  VARCHAR(20) NOT NULL,
    enhance_level            INT,
    has_ritual               BOOLEAN     NOT NULL DEFAULT FALSE,
    gem_id                   BIGINT,
    PRIMARY KEY (bundle_line_id),
    CONSTRAINT fk_bed_bundle_line FOREIGN KEY (bundle_line_id)  REFERENCES bundle_lines (id),
    CONSTRAINT fk_bed_equip_item  FOREIGN KEY (equipment_item_id) REFERENCES equipment_items (item_id),
    CONSTRAINT fk_bed_gem         FOREIGN KEY (gem_id)           REFERENCES gems (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bundle_equipment_rituals (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    bundle_line_id       BIGINT      NOT NULL,
    ritual_id            BIGINT      NOT NULL,
    outcome              VARCHAR(20) NOT NULL,
    applied_mark_snapshot VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_bundle_equipment_rituals_line_ritual UNIQUE (bundle_line_id, ritual_id),
    CONSTRAINT fk_ber_bundle_line FOREIGN KEY (bundle_line_id) REFERENCES bundle_lines (id),
    CONSTRAINT fk_ber_ritual      FOREIGN KEY (ritual_id)      REFERENCES rituals (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 15. 구매 희망
-- ============================================================

CREATE TABLE wanted_listings (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    buyer_id       BIGINT      NOT NULL,
    server         VARCHAR(30) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    offered_price  BIGINT      NOT NULL,
    note           VARCHAR(500),
    hidden         BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at     DATETIME(6),
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_wanted_listings_buyer FOREIGN KEY (buyer_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wanted_items (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    wanted_listing_id BIGINT NOT NULL,
    item_id           BIGINT NOT NULL,
    quantity          INT    NOT NULL,
    sort_order        INT    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_wanted_items_listing FOREIGN KEY (wanted_listing_id) REFERENCES wanted_listings (id),
    CONSTRAINT fk_wanted_items_item    FOREIGN KEY (item_id)           REFERENCES items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wanted_equipment_conditions (
    wanted_item_id    BIGINT  NOT NULL,
    min_enhance_level INT,
    has_ritual        BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (wanted_item_id),
    CONSTRAINT fk_wec_wanted_item FOREIGN KEY (wanted_item_id) REFERENCES wanted_items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wanted_ritual_conditions (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    wanted_item_id    BIGINT      NOT NULL,
    ritual_id         BIGINT      NOT NULL,
    preferred_outcome VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_wanted_ritual_conditions (wanted_item_id, ritual_id),
    CONSTRAINT fk_wrc_wanted_item FOREIGN KEY (wanted_item_id) REFERENCES wanted_items (id),
    CONSTRAINT fk_wrc_ritual      FOREIGN KEY (ritual_id)      REFERENCES rituals (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 16. 거래 신청 및 확정
-- ============================================================

CREATE TABLE trade_applications (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    listing_id   BIGINT       NOT NULL,
    buyer_id     BIGINT       NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    message      VARCHAR(500),
    responded_at DATETIME(6),
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_trade_app_listing FOREIGN KEY (listing_id) REFERENCES trade_listings (id),
    CONSTRAINT fk_trade_app_buyer   FOREIGN KEY (buyer_id)   REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trade_confirmeds (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    chat_room_id        BIGINT,
    seller_id           BIGINT,
    buyer_id            BIGINT,
    listing_type        VARCHAR(10)  NOT NULL,
    server_snapshot     VARCHAR(30)  NOT NULL,
    confirmed_price     BIGINT       NOT NULL,
    stat_key_snapshot   VARCHAR(255) NOT NULL,
    confirmed_at        DATETIME(6)  NOT NULL,
    cancelled           BOOLEAN      NOT NULL DEFAULT FALSE,
    cancelled_at        DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_tc_statkey_server_confirmed (stat_key_snapshot(64), server_snapshot, confirmed_at DESC),
    CONSTRAINT fk_tc_seller FOREIGN KEY (seller_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_tc_buyer  FOREIGN KEY (buyer_id)  REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trade_reviews (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    trade_confirmed_id BIGINT,
    reviewer_id        BIGINT      NOT NULL,
    target_id          BIGINT      NOT NULL,
    rating             VARCHAR(10),
    reveal_at          DATETIME(6),
    is_published       BOOLEAN     NOT NULL DEFAULT FALSE,
    submitted_at       DATETIME(6),
    created_at         DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_trade_reviews_confirmed_reviewer UNIQUE (trade_confirmed_id, reviewer_id),
    KEY idx_trade_reviews_reveal_at (reveal_at, is_published),
    CONSTRAINT fk_tr_confirmed FOREIGN KEY (trade_confirmed_id) REFERENCES trade_confirmeds (id),
    CONSTRAINT fk_tr_reviewer  FOREIGN KEY (reviewer_id)        REFERENCES users (id),
    CONSTRAINT fk_tr_target    FOREIGN KEY (target_id)          REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trade_stat_dailies (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    stat_date    DATE        NOT NULL,
    server_id    INT         NOT NULL,
    stat_key     VARCHAR(255) NOT NULL,
    trade_count  INT         NOT NULL,
    quantity_sum BIGINT      NOT NULL,
    price_sum    BIGINT      NOT NULL,
    price_min    BIGINT      NOT NULL,
    price_max    BIGINT      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_trade_stat_dailies_date_key_server UNIQUE (stat_date, stat_key, server_id),
    CONSTRAINT fk_tsd_server FOREIGN KEY (server_id) REFERENCES servers (server_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trade_stat_monthlies (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    stat_month   VARCHAR(7)  NOT NULL,
    server_id    INT         NOT NULL,
    stat_key     VARCHAR(255) NOT NULL,
    avg_price    BIGINT      NOT NULL,
    trade_count  INT         NOT NULL,
    quantity_sum BIGINT      NOT NULL,
    price_min    BIGINT,
    price_max    BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uq_trade_stat_monthlies_month_key_server UNIQUE (stat_month, stat_key, server_id),
    CONSTRAINT fk_tsm_server FOREIGN KEY (server_id) REFERENCES servers (server_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE value_metric_monthlies (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    item_id         BIGINT      NOT NULL,
    month           VARCHAR(7)  NOT NULL,
    stat_type       VARCHAR(30) NOT NULL,
    element         VARCHAR(20) NOT NULL,
    stat_value      INT         NOT NULL,
    avg_price       BIGINT      NOT NULL,
    value_for_money DOUBLE      NOT NULL,
    trade_count     INT         NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_value_metric_monthlies (month, item_id, stat_type, element),
    CONSTRAINT fk_vmm_item FOREIGN KEY (item_id) REFERENCES items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 17. 채팅
-- ============================================================

CREATE TABLE chat_rooms (
    id                         BIGINT       NOT NULL AUTO_INCREMENT,
    poster_id                  BIGINT       NOT NULL,
    counterparty_id            BIGINT       NOT NULL,
    listing_type               VARCHAR(10)  NOT NULL,
    listing_id                 BIGINT       NOT NULL,
    initiation_type            VARCHAR(15)  NOT NULL,
    status                     VARCHAR(20)  NOT NULL,
    final_price                BIGINT,
    poster_confirmed_at        DATETIME(6),
    counterparty_confirmed_at  DATETIME(6),
    completed_at               DATETIME(6),
    poster_last_read_at        DATETIME(6),
    counterparty_last_read_at  DATETIME(6),
    created_at                 DATETIME(6)  NOT NULL,
    updated_at                 DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_chat_rooms_listing_counterparty (listing_type, listing_id, counterparty_id),
    CONSTRAINT fk_chat_rooms_poster       FOREIGN KEY (poster_id)       REFERENCES users (id),
    CONSTRAINT fk_chat_rooms_counterparty FOREIGN KEY (counterparty_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- trade_confirmeds의 chat_room_id FK는 chat_rooms 생성 후 추가
ALTER TABLE trade_confirmeds
    ADD CONSTRAINT fk_tc_chat_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (id) ON DELETE SET NULL;

CREATE TABLE chat_messages (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    chat_room_id BIGINT        NOT NULL,
    sender_id    BIGINT,
    content      VARCHAR(1000) NOT NULL,
    message_type VARCHAR(10)   NOT NULL,
    flagged      BOOLEAN       NOT NULL DEFAULT FALSE,
    flag_reason  VARCHAR(500),
    hidden       BOOLEAN       NOT NULL DEFAULT FALSE,
    archived_at  DATETIME(6),
    sent_at      DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_chat_messages_room_sent (chat_room_id, sent_at),
    KEY idx_chat_messages_archived  (archived_at),
    CONSTRAINT fk_chat_messages_room   FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (id),
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id)    REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 18. 알림 / 신고
-- ============================================================

CREATE TABLE notifications (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    type         VARCHAR(30)  NOT NULL,
    chat_room_id BIGINT,
    message      VARCHAR(500) NOT NULL,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_user_read (user_id, is_read),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reports (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    reporter_id     BIGINT,
    reporter_type   VARCHAR(10)   NOT NULL,
    target_type     VARCHAR(30)   NOT NULL,
    target_id       BIGINT        NOT NULL,
    reason_category VARCHAR(30)   NOT NULL,
    description     VARCHAR(1000) NOT NULL,
    evidence_url    VARCHAR(500),
    chat_room_id    BIGINT,
    status          VARCHAR(20)   NOT NULL,
    admin_note      VARCHAR(500),
    processed_by    BIGINT,
    processed_at    DATETIME(6),
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reports_reporter     FOREIGN KEY (reporter_id)  REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_reports_processed_by FOREIGN KEY (processed_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE keyword_blacklists (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    pattern     VARCHAR(200) NOT NULL,
    is_regex    BOOLEAN      NOT NULL,
    description VARCHAR(200),
    is_active   BOOLEAN      NOT NULL,
    created_by  BIGINT       NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_keyword_blacklists_active (is_active),
    CONSTRAINT fk_keyword_blacklists_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 19. 가성비 계산기
-- ============================================================

CREATE TABLE deck_snapshots (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    content_json TEXT        NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_deck_snapshots_content_hash (content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dps_value_evaluations (
    id                        BIGINT       NOT NULL AUTO_INCREMENT,
    user_id                   BIGINT       NOT NULL,
    deck_id                   BIGINT       NOT NULL,
    monster_id                BIGINT       NOT NULL,
    baseline_deck_snapshot_id BIGINT,
    scenario_deck_snapshot_id BIGINT,
    request_json              TEXT,
    candidate_type            VARCHAR(20)  NOT NULL,
    candidate_ref             BIGINT       NOT NULL,
    mercenary_mode            VARCHAR(10),
    affected_member_id        BIGINT,
    server_id                 INT,
    price                     BIGINT,
    price_source              VARCHAR(15)  NOT NULL,
    price_json                TEXT,
    raw_dps_before            BIGINT       NOT NULL,
    raw_dps_after             BIGINT       NOT NULL,
    adjust_dps_before         BIGINT       NOT NULL,
    adjust_dps_after          BIGINT       NOT NULL,
    final_dps_before          BIGINT       NOT NULL,
    final_dps_after           BIGINT       NOT NULL,
    raw_dps_delta             BIGINT       NOT NULL,
    raw_dps_increase_rate     DOUBLE       NOT NULL,
    adjust_dps_delta          BIGINT       NOT NULL,
    adjust_dps_increase_rate  DOUBLE       NOT NULL,
    final_dps_delta           BIGINT       NOT NULL,
    final_dps_increase_rate   DOUBLE       NOT NULL,
    efficiency_per_eok_raw    DOUBLE,
    efficiency_per_eok_adjust DOUBLE,
    efficiency_per_eok_final  DOUBLE,
    evaluation_hash           VARCHAR(64)  NOT NULL,
    created_at                DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_dps_eval_user_hash UNIQUE (user_id, evaluation_hash),
    CONSTRAINT fk_dps_eval_user              FOREIGN KEY (user_id)                   REFERENCES users (id),
    CONSTRAINT fk_dps_eval_monster           FOREIGN KEY (monster_id)                REFERENCES monsters (id),
    CONSTRAINT fk_dps_eval_baseline_snapshot FOREIGN KEY (baseline_deck_snapshot_id) REFERENCES deck_snapshots (id),
    CONSTRAINT fk_dps_eval_scenario_snapshot FOREIGN KEY (scenario_deck_snapshot_id) REFERENCES deck_snapshots (id),
    CONSTRAINT fk_dps_eval_server            FOREIGN KEY (server_id)                 REFERENCES servers (server_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 20. 사냥 허브 (클리어타임)
-- ============================================================

CREATE TABLE user_clear_times (
    id                       BIGINT      NOT NULL AUTO_INCREMENT,
    user_id                  BIGINT      NOT NULL,
    monster_id               BIGINT      NOT NULL,
    deck_id                  BIGINT      NOT NULL,
    deck_snapshot_id         BIGINT      NOT NULL,
    total_resist_pierce      INT,
    total_element_pierce     INT,
    raw_dps                  BIGINT,
    adjust_dps               BIGINT,
    final_dps                BIGINT      NOT NULL,
    resist_after_debuff      INT,
    effective_monster_element INT,
    resist_pass_rate         DOUBLE,
    clear_time_seconds       INT         NOT NULL,
    is_public                BOOLEAN     NOT NULL DEFAULT TRUE,
    status                   VARCHAR(20) NOT NULL,
    exp_granted              BOOLEAN     NOT NULL,
    recorded_at              DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_uct_user          FOREIGN KEY (user_id)         REFERENCES users (id),
    CONSTRAINT fk_uct_monster       FOREIGN KEY (monster_id)      REFERENCES monsters (id),
    CONSTRAINT fk_uct_deck_snapshot FOREIGN KEY (deck_snapshot_id) REFERENCES deck_snapshots (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
