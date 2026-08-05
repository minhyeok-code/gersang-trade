-- 가이드(육성 로드맵) 도메인 테이블.
-- 원본(guides/guide_steps, 관리자 작성) + 유저 비공개 사본(user_guides/user_guide_steps).
-- 사본은 clone-on-start로 생성되며 진행도는 user_guide_steps.checked_at으로 관리한다.

-- ── 원본 ──────────────────────────────────────────────────────────────────────

CREATE TABLE `guides` (
  `id`                   bigint       NOT NULL AUTO_INCREMENT,
  `created_at`           datetime(6)  NOT NULL,
  `updated_at`           datetime(6)  NOT NULL,
  `title`                varchar(100) NOT NULL,
  `target_mercenary_id`  bigint       DEFAULT NULL,
  `phase`                varchar(20)  NOT NULL,
  `version`              varchar(30)  NOT NULL,
  `author`               varchar(100) DEFAULT NULL,
  `published`            bit(1)       NOT NULL DEFAULT b'0',
  `next_guide_id`        bigint       DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_guides_target_mercenary` (`target_mercenary_id`),
  CONSTRAINT `fk_guides_target_mercenary` FOREIGN KEY (`target_mercenary_id`) REFERENCES `mercenaries` (`id`),
  CONSTRAINT `fk_guides_next_guide` FOREIGN KEY (`next_guide_id`) REFERENCES `guides` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `guide_steps` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT,
  `guide_id`              bigint       NOT NULL,
  `step_order`            int          NOT NULL,
  `step_type`             varchar(30)  NOT NULL,
  `label`                 varchar(100) NOT NULL,
  `note`                  varchar(255) DEFAULT NULL,
  `linked_item_id`        bigint       DEFAULT NULL,
  `linked_set_id`         bigint       DEFAULT NULL,
  `linked_mercenary_id`   bigint       DEFAULT NULL,
  `icon_url`              varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_guide_step_order` (`guide_id`, `step_order`),
  KEY `idx_guide_steps_item` (`linked_item_id`),
  KEY `idx_guide_steps_set` (`linked_set_id`),
  KEY `idx_guide_steps_mercenary` (`linked_mercenary_id`),
  CONSTRAINT `fk_guide_steps_guide` FOREIGN KEY (`guide_id`) REFERENCES `guides` (`id`),
  CONSTRAINT `fk_guide_steps_item` FOREIGN KEY (`linked_item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_guide_steps_set` FOREIGN KEY (`linked_set_id`) REFERENCES `equipment_sets` (`id`),
  CONSTRAINT `fk_guide_steps_mercenary` FOREIGN KEY (`linked_mercenary_id`) REFERENCES `mercenaries` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ── 유저 사본 ─────────────────────────────────────────────────────────────────

CREATE TABLE `user_guides` (
  `id`                   bigint       NOT NULL AUTO_INCREMENT,
  `created_at`           datetime(6)  NOT NULL,
  `updated_at`           datetime(6)  NOT NULL,
  `user_id`              bigint       NOT NULL,
  `source_guide_id`      bigint       DEFAULT NULL,
  `source_version`       varchar(30)  NOT NULL,
  `title`                varchar(100) NOT NULL,
  `target_mercenary_id`  bigint       DEFAULT NULL,
  `deleted_at`           datetime(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_guides_user` (`user_id`),
  KEY `idx_user_guides_user_source` (`user_id`, `source_guide_id`, `deleted_at`),
  CONSTRAINT `fk_user_guides_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_user_guides_source_guide` FOREIGN KEY (`source_guide_id`) REFERENCES `guides` (`id`),
  CONSTRAINT `fk_user_guides_target_mercenary` FOREIGN KEY (`target_mercenary_id`) REFERENCES `mercenaries` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_guide_steps` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT,
  `user_guide_id`         bigint       NOT NULL,
  `step_order`            int          NOT NULL,
  `step_type`             varchar(30)  NOT NULL,
  `label`                 varchar(100) NOT NULL,
  `note`                  varchar(255) DEFAULT NULL,
  `linked_item_id`        bigint       DEFAULT NULL,
  `linked_set_id`         bigint       DEFAULT NULL,
  `linked_mercenary_id`   bigint       DEFAULT NULL,
  `icon_url`              varchar(500) DEFAULT NULL,
  `is_custom`             bit(1)       NOT NULL DEFAULT b'0',
  `checked_at`            datetime(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_guide_steps_guide` (`user_guide_id`),
  KEY `idx_user_guide_steps_item` (`linked_item_id`),
  KEY `idx_user_guide_steps_set` (`linked_set_id`),
  KEY `idx_user_guide_steps_mercenary` (`linked_mercenary_id`),
  CONSTRAINT `fk_user_guide_steps_user_guide` FOREIGN KEY (`user_guide_id`) REFERENCES `user_guides` (`id`),
  CONSTRAINT `fk_user_guide_steps_item` FOREIGN KEY (`linked_item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_user_guide_steps_set` FOREIGN KEY (`linked_set_id`) REFERENCES `equipment_sets` (`id`),
  CONSTRAINT `fk_user_guide_steps_mercenary` FOREIGN KEY (`linked_mercenary_id`) REFERENCES `mercenaries` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
