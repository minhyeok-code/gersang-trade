
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_EXECUTION` (
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `VERSION` bigint DEFAULT NULL,
  `JOB_INSTANCE_ID` bigint NOT NULL,
  `CREATE_TIME` datetime(6) NOT NULL,
  `START_TIME` datetime(6) DEFAULT NULL,
  `END_TIME` datetime(6) DEFAULT NULL,
  `STATUS` varchar(10) DEFAULT NULL,
  `EXIT_CODE` varchar(2500) DEFAULT NULL,
  `EXIT_MESSAGE` varchar(2500) DEFAULT NULL,
  `LAST_UPDATED` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`JOB_EXECUTION_ID`),
  KEY `JOB_INST_EXEC_FK` (`JOB_INSTANCE_ID`),
  CONSTRAINT `JOB_INST_EXEC_FK` FOREIGN KEY (`JOB_INSTANCE_ID`) REFERENCES `BATCH_JOB_INSTANCE` (`JOB_INSTANCE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_EXECUTION_CONTEXT` (
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `SHORT_CONTEXT` varchar(2500) NOT NULL,
  `SERIALIZED_CONTEXT` text,
  PRIMARY KEY (`JOB_EXECUTION_ID`),
  CONSTRAINT `JOB_EXEC_CTX_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_EXECUTION_PARAMS` (
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `PARAMETER_NAME` varchar(100) NOT NULL,
  `PARAMETER_TYPE` varchar(100) NOT NULL,
  `PARAMETER_VALUE` varchar(2500) DEFAULT NULL,
  `IDENTIFYING` char(1) NOT NULL,
  KEY `JOB_EXEC_PARAMS_FK` (`JOB_EXECUTION_ID`),
  CONSTRAINT `JOB_EXEC_PARAMS_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_EXECUTION_SEQ` (
  `ID` bigint NOT NULL,
  `UNIQUE_KEY` char(1) NOT NULL,
  UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_INSTANCE` (
  `JOB_INSTANCE_ID` bigint NOT NULL,
  `VERSION` bigint DEFAULT NULL,
  `JOB_NAME` varchar(100) NOT NULL,
  `JOB_KEY` varchar(32) NOT NULL,
  PRIMARY KEY (`JOB_INSTANCE_ID`),
  UNIQUE KEY `JOB_INST_UN` (`JOB_NAME`,`JOB_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_SEQ` (
  `ID` bigint NOT NULL,
  `UNIQUE_KEY` char(1) NOT NULL,
  UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_STEP_EXECUTION` (
  `STEP_EXECUTION_ID` bigint NOT NULL,
  `VERSION` bigint NOT NULL,
  `STEP_NAME` varchar(100) NOT NULL,
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `CREATE_TIME` datetime(6) NOT NULL,
  `START_TIME` datetime(6) DEFAULT NULL,
  `END_TIME` datetime(6) DEFAULT NULL,
  `STATUS` varchar(10) DEFAULT NULL,
  `COMMIT_COUNT` bigint DEFAULT NULL,
  `READ_COUNT` bigint DEFAULT NULL,
  `FILTER_COUNT` bigint DEFAULT NULL,
  `WRITE_COUNT` bigint DEFAULT NULL,
  `READ_SKIP_COUNT` bigint DEFAULT NULL,
  `WRITE_SKIP_COUNT` bigint DEFAULT NULL,
  `PROCESS_SKIP_COUNT` bigint DEFAULT NULL,
  `ROLLBACK_COUNT` bigint DEFAULT NULL,
  `EXIT_CODE` varchar(2500) DEFAULT NULL,
  `EXIT_MESSAGE` varchar(2500) DEFAULT NULL,
  `LAST_UPDATED` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`STEP_EXECUTION_ID`),
  KEY `JOB_EXEC_STEP_FK` (`JOB_EXECUTION_ID`),
  CONSTRAINT `JOB_EXEC_STEP_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_STEP_EXECUTION_CONTEXT` (
  `STEP_EXECUTION_ID` bigint NOT NULL,
  `SHORT_CONTEXT` varchar(2500) NOT NULL,
  `SERIALIZED_CONTEXT` text,
  PRIMARY KEY (`STEP_EXECUTION_ID`),
  CONSTRAINT `STEP_EXEC_CTX_FK` FOREIGN KEY (`STEP_EXECUTION_ID`) REFERENCES `BATCH_STEP_EXECUTION` (`STEP_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_STEP_EXECUTION_SEQ` (
  `ID` bigint NOT NULL,
  `UNIQUE_KEY` char(1) NOT NULL,
  UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bundle_equipment_details` (
  `bundle_line_id` bigint NOT NULL,
  `enhance_level` int DEFAULT NULL,
  `equipment_kind_snapshot` enum('APPEARANCE','NORMAL') NOT NULL,
  `has_ritual` bit(1) NOT NULL,
  `equipment_item_id` bigint NOT NULL,
  `gem_id` bigint DEFAULT NULL,
  PRIMARY KEY (`bundle_line_id`),
  KEY `FKa7skat3w8vrnedcfoceoj98gl` (`equipment_item_id`),
  KEY `FKoyni0fkvnu4vkbmt2lcq862n7` (`gem_id`),
  CONSTRAINT `FKa7skat3w8vrnedcfoceoj98gl` FOREIGN KEY (`equipment_item_id`) REFERENCES `equipment_items` (`item_id`),
  CONSTRAINT `FKh3jc5owvvk5jxg0ofpt7kf7w0` FOREIGN KEY (`bundle_line_id`) REFERENCES `bundle_lines` (`id`),
  CONSTRAINT `FKoyni0fkvnu4vkbmt2lcq862n7` FOREIGN KEY (`gem_id`) REFERENCES `gems` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bundle_equipment_rituals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `applied_mark_snapshot` varchar(20) NOT NULL,
  `outcome` enum('GREAT_SUCCESS','SUCCESS') NOT NULL,
  `bundle_line_id` bigint NOT NULL,
  `ritual_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_bundle_equipment_rituals_line_ritual` (`bundle_line_id`,`ritual_id`),
  KEY `FKlkgsaap0vstlug3j793unirmq` (`ritual_id`),
  CONSTRAINT `FKdfn7s0xwwyb16pos6fa9mnry3` FOREIGN KEY (`bundle_line_id`) REFERENCES `bundle_lines` (`id`),
  CONSTRAINT `FKlkgsaap0vstlug3j793unirmq` FOREIGN KEY (`ritual_id`) REFERENCES `rituals` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bundle_lines` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quantity` int NOT NULL,
  `sort_order` int NOT NULL,
  `bundle_id` bigint NOT NULL,
  `equipment_set_piece_id` bigint DEFAULT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKcwlmxbie4gf2bqppv6qeggry5` (`bundle_id`),
  KEY `FKkjdbsessfbm4ubdc8e7mb7gnw` (`equipment_set_piece_id`),
  KEY `FK94nuee30188po3a22lx1jigt1` (`item_id`),
  CONSTRAINT `FK94nuee30188po3a22lx1jigt1` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `FKcwlmxbie4gf2bqppv6qeggry5` FOREIGN KEY (`bundle_id`) REFERENCES `listing_bundles` (`id`),
  CONSTRAINT `FKkjdbsessfbm4ubdc8e7mb7gnw` FOREIGN KEY (`equipment_set_piece_id`) REFERENCES `equipment_set_pieces` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `characteristic_effects` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `element` enum('ADAPTIVE','EARTH','FIRE','NONE','THUNDER','WATER','WIND') NOT NULL,
  `stat_type` enum('ALL_STAT','ATTACK_POWER','ATTACK_SPEED','BASE_DAMAGE_MULTIPLIER','CRITICAL_CHANCE','CRITICAL_DAMAGE','CRITICAL_RATE','DAMAGE_PERCENT','DAMAGE_PERCENT_AIR','DAMAGE_PERCENT_GROUND','DEFENSE','DEXTERITY','ELEMENT_PIERCE','ELEMENT_VALUE','FIELD_MOVE_SPEED','HITTING_RESISTANCE','HITTING_RESISTANCE_PIERCE','HIT_RATE','HP_RECOVERY','INTELLECT','MAGIC_RESISTANCE','MAGIC_RESISTANCE_PIERCE','MAIN_STAT_FLAT','MAX_DAMAGE','MAX_POWER','MIN_DAMAGE','MIN_POWER','MOVE_SPEED','MP_RECOVERY','RESIST_PIERCE','SIGHT','SKILL_DAMAGE_PERCENT','SKILL_RANGE','STRENGTH','STUN_DURATION','VITALITY') NOT NULL,
  `target` enum('ALLY','ALLY_HEAVENLY_KING','ALLY_SAME_ELEMENT','ENEMY','SELF') NOT NULL,
  `value` float NOT NULL,
  `value_type` enum('FLAT','PERCENT_ADD') NOT NULL,
  `characteristic_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqjb8op21kj9sv8ni0u02esnoi` (`characteristic_id`),
  CONSTRAINT `FKqjb8op21kj9sv8ni0u02esnoi` FOREIGN KEY (`characteristic_id`) REFERENCES `legend_general_characteristic` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=361 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `archived_at` datetime(6) DEFAULT NULL,
  `content` varchar(1000) NOT NULL,
  `flag_reason` varchar(500) DEFAULT NULL,
  `flagged` bit(1) NOT NULL,
  `hidden` bit(1) NOT NULL,
  `message_type` enum('SYSTEM','TEXT') NOT NULL,
  `sent_at` datetime(6) NOT NULL,
  `chat_room_id` bigint NOT NULL,
  `sender_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_chat_messages_room_sent` (`chat_room_id`,`sent_at`),
  KEY `idx_chat_messages_archived` (`archived_at`),
  KEY `FKgiqeap8ays4lf684x7m0r2729` (`sender_id`),
  CONSTRAINT `FKbcsxusjp1v4rd8879fhvq8ssb` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_rooms` (`id`),
  CONSTRAINT `FKgiqeap8ays4lf684x7m0r2729` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `counterparty_confirmed_at` datetime(6) DEFAULT NULL,
  `counterparty_last_read_at` datetime(6) DEFAULT NULL,
  `final_price` bigint DEFAULT NULL,
  `initiation_type` enum('APPLY','NEGOTIATE') NOT NULL,
  `listing_id` bigint NOT NULL,
  `listing_type` enum('BUY','SELL') NOT NULL,
  `poster_confirmed_at` datetime(6) DEFAULT NULL,
  `poster_last_read_at` datetime(6) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `counterparty_id` bigint NOT NULL,
  `poster_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_chat_rooms_listing_counterparty` (`listing_type`,`listing_id`,`counterparty_id`),
  KEY `FKgmo1w2vqkvdnswuruugocqr9o` (`counterparty_id`),
  KEY `FKlsj8fwt5ta65u6l0o68s3ltej` (`poster_id`),
  CONSTRAINT `FKgmo1w2vqkvdnswuruugocqr9o` FOREIGN KEY (`counterparty_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKlsj8fwt5ta65u6l0o68s3ltej` FOREIGN KEY (`poster_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `deck_buff` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `element` enum('ADAPTIVE','EARTH','FIRE','NONE','THUNDER','WATER','WIND') NOT NULL,
  `stat_type` enum('ALL_STAT','ATTACK_POWER','ATTACK_SPEED','BASE_DAMAGE_MULTIPLIER','CRITICAL_CHANCE','CRITICAL_DAMAGE','CRITICAL_RATE','DAMAGE_PERCENT','DAMAGE_PERCENT_AIR','DAMAGE_PERCENT_GROUND','DEFENSE','DEXTERITY','ELEMENT_PIERCE','ELEMENT_VALUE','FIELD_MOVE_SPEED','HITTING_RESISTANCE','HITTING_RESISTANCE_PIERCE','HIT_RATE','HP_RECOVERY','INTELLECT','MAGIC_RESISTANCE','MAGIC_RESISTANCE_PIERCE','MAIN_STAT_FLAT','MAX_DAMAGE','MAX_POWER','MIN_DAMAGE','MIN_POWER','MOVE_SPEED','MP_RECOVERY','RESIST_PIERCE','SIGHT','SKILL_DAMAGE_PERCENT','SKILL_RANGE','STRENGTH','STUN_DURATION','VITALITY') NOT NULL,
  `target` enum('ALLY','ALLY_HEAVENLY_KING','ALLY_SAME_ELEMENT','ENEMY','SELF') NOT NULL,
  `value` float NOT NULL,
  `value_type` enum('FLAT','PERCENT_ADD') NOT NULL,
  `source_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgvrcrd0ry1b0oybvmsu09dnh8` (`source_id`),
  CONSTRAINT `FKgvrcrd0ry1b0oybvmsu09dnh8` FOREIGN KEY (`source_id`) REFERENCES `deck_buff_source` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `deck_buff_source` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `source_id` bigint NOT NULL,
  `source_type` enum('CHEUNGJIN','GAHO','GONMYEONG','JINBEOP','LEGEND_GENERAL') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `deck_snapshots` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content_hash` varchar(64) NOT NULL,
  `content_json` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_deck_snapshots_content_hash` (`content_hash`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dps_value_evaluations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `adjust_dps_after` bigint NOT NULL,
  `adjust_dps_before` bigint NOT NULL,
  `adjust_dps_delta` bigint NOT NULL,
  `adjust_dps_increase_rate` double NOT NULL,
  `affected_member_id` bigint DEFAULT NULL,
  `candidate_ref` bigint NOT NULL,
  `candidate_type` enum('ITEM_SET','ITEM_SINGLE','MERCENARY') NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deck_id` bigint NOT NULL,
  `efficiency_per_eok_adjust` double DEFAULT NULL,
  `efficiency_per_eok_final` double DEFAULT NULL,
  `efficiency_per_eok_raw` double DEFAULT NULL,
  `evaluation_hash` varchar(64) NOT NULL,
  `final_dps_after` bigint NOT NULL,
  `final_dps_before` bigint NOT NULL,
  `final_dps_delta` bigint NOT NULL,
  `final_dps_increase_rate` double NOT NULL,
  `mercenary_mode` enum('APPEND','REPLACE') DEFAULT NULL,
  `price` bigint DEFAULT NULL,
  `price_json` text,
  `price_source` enum('MISSING','MIXED','TRADE_STAT','USER_INPUT') NOT NULL,
  `raw_dps_after` bigint NOT NULL,
  `raw_dps_before` bigint NOT NULL,
  `raw_dps_delta` bigint NOT NULL,
  `raw_dps_increase_rate` double NOT NULL,
  `monster_id` bigint NOT NULL,
  `scenario_deck_snapshot_id` bigint DEFAULT NULL,
  `server_id` int DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `request_json` text,
  `baseline_deck_snapshot_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_dps_eval_user_hash` (`user_id`,`evaluation_hash`),
  KEY `FKlapg6jysqgck91f5q9t5gu2u9` (`monster_id`),
  KEY `FKibhieeprf2xx67u1pen0x1qgy` (`scenario_deck_snapshot_id`),
  KEY `FKbf2fhhoj2hcg9646tukh1ml2q` (`server_id`),
  KEY `FK7gom0fdlkwhtbiegv4sernjwy` (`baseline_deck_snapshot_id`),
  CONSTRAINT `FK7gom0fdlkwhtbiegv4sernjwy` FOREIGN KEY (`baseline_deck_snapshot_id`) REFERENCES `deck_snapshots` (`id`),
  CONSTRAINT `FKbf2fhhoj2hcg9646tukh1ml2q` FOREIGN KEY (`server_id`) REFERENCES `servers` (`server_id`),
  CONSTRAINT `FKgulkfrrqmqtrfysw72kyqhltf` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKibhieeprf2xx67u1pen0x1qgy` FOREIGN KEY (`scenario_deck_snapshot_id`) REFERENCES `deck_snapshots` (`id`),
  CONSTRAINT `FKlapg6jysqgck91f5q9t5gu2u9` FOREIGN KEY (`monster_id`) REFERENCES `monsters` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `equipment_items` (
  `item_id` bigint NOT NULL,
  `enhancement` int DEFAULT NULL,
  `equip_slot` enum('APP_ARMOR','APP_BRACELET','APP_EARRING','APP_GREAVES','APP_HELMET','APP_NECKLACE','APP_SPIRIT','APP_WAR_GOD','APP_WEAPON','ARMOR','BELT','CHARM','GLOVES','HELMET','RING_1','RING_2','SHOES','WEAPON') DEFAULT NULL,
  `equipment_kind` enum('APPEARANCE','NORMAL') NOT NULL,
  `has_slot_option` bit(1) NOT NULL,
  `ritual_applicable` bit(1) NOT NULL,
  `is_sain_sword` bit(1) NOT NULL,
  `slot` enum('ACCESSORY','ARMOR','BELT','BRACELET','DIVINE','EARRING','GLOVES','HELMET','LEGGING','NECKLACE','ORB','RING','SHOES','TALISMAN','TITLE','WEAPON','WING') NOT NULL,
  `set_id` bigint DEFAULT NULL,
  `mercenary_id` bigint DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  KEY `FKdqaf1wmelp0540gpgi0v7tpil` (`set_id`),
  KEY `FKffk4urehj7wei1kuhthcqj301` (`mercenary_id`),
  CONSTRAINT `FKdqaf1wmelp0540gpgi0v7tpil` FOREIGN KEY (`set_id`) REFERENCES `equipment_sets` (`id`),
  CONSTRAINT `FKffk4urehj7wei1kuhthcqj301` FOREIGN KEY (`mercenary_id`) REFERENCES `mercenaries` (`id`),
  CONSTRAINT `FKgv4l4r0jnuoiq4fjvlryihp66` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `equipment_set_effects` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `element` enum('ADAPTIVE','EARTH','FIRE','NONE','THUNDER','WATER','WIND') NOT NULL,
  `required_pieces` int NOT NULL,
  `scope` enum('ALLY','ALLY_HEAVENLY_KING','ALLY_SAME_ELEMENT','ENEMY','SELF') NOT NULL,
  `stat_type` enum('ALL_STAT','ATTACK_POWER','ATTACK_SPEED','BASE_DAMAGE_MULTIPLIER','CRITICAL_CHANCE','CRITICAL_DAMAGE','CRITICAL_RATE','DAMAGE_PERCENT','DAMAGE_PERCENT_AIR','DAMAGE_PERCENT_GROUND','DEFENSE','DEXTERITY','ELEMENT_PIERCE','ELEMENT_VALUE','FIELD_MOVE_SPEED','HITTING_RESISTANCE','HITTING_RESISTANCE_PIERCE','HIT_RATE','HP_RECOVERY','INTELLECT','MAGIC_RESISTANCE','MAGIC_RESISTANCE_PIERCE','MAIN_STAT_FLAT','MAX_DAMAGE','MAX_POWER','MIN_DAMAGE','MIN_POWER','MOVE_SPEED','MP_RECOVERY','RESIST_PIERCE','SIGHT','SKILL_DAMAGE_PERCENT','SKILL_RANGE','STRENGTH','STUN_DURATION','VITALITY') NOT NULL,
  `stat_unit` enum('FLAT','LEVEL','PERCENT') NOT NULL,
  `stat_value` int NOT NULL,
  `equipment_set_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_set_effects_set_pieces_stat_element_scope` (`equipment_set_id`,`required_pieces`,`stat_type`,`element`,`scope`),
  CONSTRAINT `FKf6wo6e43t1aktfbxmwjmwtmle` FOREIGN KEY (`equipment_set_id`) REFERENCES `equipment_sets` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=144 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `equipment_set_pieces` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `piece_count` int NOT NULL DEFAULT '1',
  `slot` enum('ACCESSORY','ARMOR','BELT','BRACELET','DIVINE','EARRING','GLOVES','HELMET','LEGGING','NECKLACE','ORB','RING','SHOES','TALISMAN','TITLE','WEAPON','WING') NOT NULL,
  `equipment_item_id` bigint NOT NULL,
  `set_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_set_pieces_set_id_slot` (`set_id`,`slot`),
  KEY `FKanlg37ta6h3y281mb9jxp634h` (`equipment_item_id`),
  CONSTRAINT `FKanlg37ta6h3y281mb9jxp634h` FOREIGN KEY (`equipment_item_id`) REFERENCES `equipment_items` (`item_id`),
  CONSTRAINT `FKkvpgcqxq7nmqur08djtyw2xs1` FOREIGN KEY (`set_id`) REFERENCES `equipment_sets` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=189 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `equipment_set_skill_effects` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enhancement` int DEFAULT NULL,
  `required_pieces` int NOT NULL,
  `set_id` bigint NOT NULL,
  `set_granted_skill_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_set_skill_effect` (`set_id`,`required_pieces`,`enhancement`),
  KEY `FK5tkuvbai56gvxnqwan5dkogxw` (`set_granted_skill_id`),
  CONSTRAINT `FK5tkuvbai56gvxnqwan5dkogxw` FOREIGN KEY (`set_granted_skill_id`) REFERENCES `set_granted_skills` (`id`),
  CONSTRAINT `FKnlg65b49ai763g1mk442oidp1` FOREIGN KEY (`set_id`) REFERENCES `equipment_sets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `equipment_sets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enhancement` int DEFAULT NULL,
  `is_tradeable` tinyint(1) NOT NULL DEFAULT '1',
  `name` varchar(100) NOT NULL,
  `total_pieces` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gaho_level_stat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `level` int NOT NULL,
  `stat_type` enum('ALL_STAT','ATTACK_POWER','ATTACK_SPEED','BASE_DAMAGE_MULTIPLIER','CRITICAL_CHANCE','CRITICAL_DAMAGE','CRITICAL_RATE','DAMAGE_PERCENT','DAMAGE_PERCENT_AIR','DAMAGE_PERCENT_GROUND','DEFENSE','DEXTERITY','ELEMENT_PIERCE','ELEMENT_VALUE','FIELD_MOVE_SPEED','HITTING_RESISTANCE','HITTING_RESISTANCE_PIERCE','HIT_RATE','HP_RECOVERY','INTELLECT','MAGIC_RESISTANCE','MAGIC_RESISTANCE_PIERCE','MAIN_STAT_FLAT','MAX_DAMAGE','MAX_POWER','MIN_DAMAGE','MIN_POWER','MOVE_SPEED','MP_RECOVERY','RESIST_PIERCE','SIGHT','SKILL_DAMAGE_PERCENT','SKILL_RANGE','STRENGTH','STUN_DURATION','VITALITY') NOT NULL,
  `value` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKvrgl5lr8r1whvb9ey9fjknhr` (`level`,`stat_type`)
) ENGINE=InnoDB AUTO_INCREMENT=66 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gems` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `gem_grade` enum('BASIC','ENHANCED','REFINED','SHINING') NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `ritual_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_gems_name_grade_ritual` (`name`,`gem_grade`,`ritual_id`),
  KEY `FKr4tkbpmxlrhqm8d1qwbga5ybp` (`ritual_id`),
  CONSTRAINT `FKr4tkbpmxlrhqm8d1qwbga5ybp` FOREIGN KEY (`ritual_id`) REFERENCES `rituals` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gonmyeong_level_stat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `level` int NOT NULL,
  `stat_type` enum('ALL_STAT','ATTACK_POWER','ATTACK_SPEED','BASE_DAMAGE_MULTIPLIER','CRITICAL_CHANCE','CRITICAL_DAMAGE','CRITICAL_RATE','DAMAGE_PERCENT','DAMAGE_PERCENT_AIR','DAMAGE_PERCENT_GROUND','DEFENSE','DEXTERITY','ELEMENT_PIERCE','ELEMENT_VALUE','FIELD_MOVE_SPEED','HITTING_RESISTANCE','HITTING_RESISTANCE_PIERCE','HIT_RATE','HP_RECOVERY','INTELLECT','MAGIC_RESISTANCE','MAGIC_RESISTANCE_PIERCE','MAIN_STAT_FLAT','MAX_DAMAGE','MAX_POWER','MIN_DAMAGE','MIN_POWER','MOVE_SPEED','MP_RECOVERY','RESIST_PIERCE','SIGHT','SKILL_DAMAGE_PERCENT','SKILL_RANGE','STRENGTH','STUN_DURATION','VITALITY') NOT NULL,
  `value` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1ckt7cs2m5y1urhq61agql5gy` (`level`,`stat_type`)
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `item_mercenary_restrictions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` enum('DIVINE_BEAST','EVIL_BEAST','EVIL_BEAST_AWAKENING','EVOLVE_MONSTER','FIRST_GRADE_GENERAL','FOUR_HEAVENLY_KINGS','FOUR_HEAVENLY_KINGS_AWAKENING','GENERAL_AWAKENING','HIRED_MONSTER','LEGENDARY_GENERAL','MERCENARY','MODIFIED_GENERAL','MYEONG_KING','MYEONG_KING_AWAKENING','PROTAGONIST','SECOND_GRADE_GENERAL','SPIRIT_MONSTER') DEFAULT NULL,
  `item_id` bigint NOT NULL,
  `mercenary_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKiov2dyp0rwr63ix1sa2uhig9r` (`item_id`),
  KEY `FK3prt0mv3bud2pa6yypvb7h37o` (`mercenary_id`),
  CONSTRAINT `FK3prt0mv3bud2pa6yypvb7h37o` FOREIGN KEY (`mercenary_id`) REFERENCES `mercenaries` (`id`),
  CONSTRAINT `FKiov2dyp0rwr63ix1sa2uhig9r` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=357 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `item_skill_effects` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stat_key` enum('ALL_STAT','ATTACK_POWER','ATTACK_SPEED','BASE_DAMAGE_MULTIPLIER','CRITICAL_CHANCE','CRITICAL_DAMAGE','CRITICAL_RATE','DAMAGE_PERCENT','DAMAGE_PERCENT_AIR','DAMAGE_PERCENT_GROUND','DEFENSE','DEXTERITY','ELEMENT_PIERCE','ELEMENT_VALUE','FIELD_MOVE_SPEED','HITTING_RESISTANCE','HITTING_RESISTANCE_PIERCE','HIT_RATE','HP_RECOVERY','INTELLECT','MAGIC_RESISTANCE','MAGIC_RESISTANCE_PIERCE','MAIN_STAT_FLAT','MAX_DAMAGE','MAX_POWER','MIN_DAMAGE','MIN_POWER','MOVE_SPEED','MP_RECOVERY','RESIST_PIERCE','SIGHT','SKILL_DAMAGE_PERCENT','SKILL_RANGE','STRENGTH','STUN_DURATION','VITALITY') NOT NULL,
  `stat_value` int NOT NULL,
  `skill_id` bigint NOT NULL,
  `value_type` enum('FLAT','PERCENT') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_item_skill_effect_skill_stat` (`skill_id`,`stat_key`),
  CONSTRAINT `FKqvocnbwwvj2pr27u66ytqx32v` FOREIGN KEY (`skill_id`) REFERENCES `item_skills` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `item_skill_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `item_id` bigint NOT NULL,
  `skill_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_item_skill_mapping` (`item_id`,`skill_id`),
  KEY `FKb8gfhavyant6stou2x3pl8ubk` (`skill_id`),
  CONSTRAINT `FKb8gfhavyant6stou2x3pl8ubk` FOREIGN KEY (`skill_id`) REFERENCES `item_skills` (`id`),
  CONSTRAINT `FKq40l6cli229ird1mvg0w59m41` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=783 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `item_skills` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `note` text,
  `replaces_base_skill` bit(1) NOT NULL,
  `skill_behavior_type` enum('ACTIVE','PASSIVE','TRIGGER') DEFAULT NULL,
  `skill_key` varchar(100) DEFAULT NULL,
  `skill_name` varchar(100) NOT NULL,
  `trigger_base_skill_key` varchar(100) DEFAULT NULL,
  `trigger_every_n` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_item_skills_skill_name` (`skill_name`)
) ENGINE=InnoDB AUTO_INCREMENT=312 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `item_stats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `element` enum('ADAPTIVE','EARTH','FIRE','NONE','THUNDER','WATER','WIND') NOT NULL,
  `scope` enum('ALLY','ALLY_HEAVENLY_KING','ALLY_SAME_ELEMENT','ENEMY','SELF') NOT NULL,
  `stat_type` enum('ALL_STAT','ATTACK_POWER','ATTACK_SPEED','BASE_DAMAGE_MULTIPLIER','CRITICAL_CHANCE','CRITICAL_DAMAGE','CRITICAL_RATE','DAMAGE_PERCENT','DAMAGE_PERCENT_AIR','DAMAGE_PERCENT_GROUND','DEFENSE','DEXTERITY','ELEMENT_PIERCE','ELEMENT_VALUE','FIELD_MOVE_SPEED','HITTING_RESISTANCE','HITTING_RESISTANCE_PIERCE','HIT_RATE','HP_RECOVERY','INTELLECT','MAGIC_RESISTANCE','MAGIC_RESISTANCE_PIERCE','MAIN_STAT_FLAT','MAX_DAMAGE','MAX_POWER','MIN_DAMAGE','MIN_POWER','MOVE_SPEED','MP_RECOVERY','RESIST_PIERCE','SIGHT','SKILL_DAMAGE_PERCENT','SKILL_RANGE','STRENGTH','STUN_DURATION','VITALITY') NOT NULL,
  `stat_unit` enum('FLAT','LEVEL','PERCENT') NOT NULL,
  `value` int NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_item_stats_item_stat_element_scope` (`item_id`,`stat_type`,`element`,`scope`),
  CONSTRAINT `FKtakqrskwl91ua4d6a6t9mcpsy` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5993 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `item_key` varchar(100) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `trade_category` varchar(50) DEFAULT NULL,
  `type` enum('EQUIPMENT','MATERIAL') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK2c5cyh94y5p7u1rasnt1jj7rm` (`item_key`)
) ENGINE=InnoDB AUTO_INCREMENT=1826 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `keyword_blacklists` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(200) DEFAULT NULL,
  `pattern` varchar(200) NOT NULL,
  `is_regex` bit(1) NOT NULL,
  `created_by` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_keyword_blacklists_active` (`is_active`),
  KEY `FK13emmsagls7ebxxmeeqmx718h` (`created_by`),
  CONSTRAINT `FK13emmsagls7ebxxmeeqmx718h` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `legend_general` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` enum('TYPE_A','TYPE_B') NOT NULL,
  `mercenary_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgo40dij1k651ox2xfvwvda0aa` (`mercenary_id`),
  CONSTRAINT `FKje5necvcl65iome026iyjbg0s` FOREIGN KEY (`mercenary_id`) REFERENCES `mercenaries` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `legend_general_characteristic` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `characteristic_index` int NOT NULL,
  `level` int NOT NULL,
  `name` varchar(50) DEFAULT NULL,
  `legend_general_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKsohdc8pcgcgl0tu54k9kl3b2f` (`legend_general_id`,`characteristic_index`,`level`),
  CONSTRAINT `FKpk4j41hr8x7h52n6k9andyk2b` FOREIGN KEY (`legend_general_id`) REFERENCES `legend_general` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=291 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `legend_general_passive` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `element` enum('ADAPTIVE','EARTH','FIRE','NONE','THUNDER','WATER','WIND') NOT NULL,
  `increment_per_levels` int DEFAULT NULL,
  `increment_value` float DEFAULT NULL,
  `max_value` float DEFAULT NULL,
  `start_level` int DEFAULT NULL,
  `start_value` float DEFAULT NULL,
  `stat_type` enum('ALL_STAT','ATTACK_POWER','ATTACK_SPEED','BASE_DAMAGE_MULTIPLIER','CRITICAL_CHANCE','CRITICAL_DAMAGE','CRITICAL_RATE','DAMAGE_PERCENT','DAMAGE_PERCENT_AIR','DAMAGE_PERCENT_GROUND','DEFENSE','DEXTERITY','ELEMENT_PIERCE','ELEMENT_VALUE','FIELD_MOVE_SPEED','HITTING_RESISTANCE','HITTING_RESISTANCE_PIERCE','HIT_RATE','HP_RECOVERY','INTELLECT','MAGIC_RESISTANCE','MAGIC_RESISTANCE_PIERCE','MAIN_STAT_FLAT','MAX_DAMAGE','MAX_POWER','MIN_DAMAGE','MIN_POWER','MOVE_SPEED','MP_RECOVERY','RESIST_PIERCE','SIGHT','SKILL_DAMAGE_PERCENT','SKILL_RANGE','STRENGTH','STUN_DURATION','VITALITY') NOT NULL,
  `target` enum('ALLY','ALLY_HEAVENLY_KING','ALLY_SAME_ELEMENT','ENEMY','SELF') NOT NULL,
  `value_type` enum('FLAT','PERCENT_ADD') NOT NULL,
  `legend_general_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKkilddaf3d3tif85s68uh6ko1l` (`legend_general_id`),
  CONSTRAINT `FKkilddaf3d3tif85s68uh6ko1l` FOREIGN KEY (`legend_general_id`) REFERENCES `legend_general` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `listing_bundles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bundle_type` enum('EQUIPMENT_SET','EQUIPMENT_SINGLE','MATERIAL_BUNDLE') NOT NULL,
  `title_override` varchar(200) DEFAULT NULL,
  `equipment_set_id` bigint DEFAULT NULL,
  `listing_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7cxgmpjxd5sxnlyy59fngx23c` (`equipment_set_id`),
  KEY `FKi0ex1smaw70wx9ymhnvkkm1po` (`listing_id`),
  CONSTRAINT `FK7cxgmpjxd5sxnlyy59fngx23c` FOREIGN KEY (`equipment_set_id`) REFERENCES `equipment_sets` (`id`),
  CONSTRAINT `FKi0ex1smaw70wx9ymhnvkkm1po` FOREIGN KEY (`listing_id`) REFERENCES `trade_listings` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `material_items` (
  `item_id` bigint NOT NULL,
  `stack_unit_name` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  CONSTRAINT `FK4w53bh79d6uc71dt1mn06k66y` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `material_price_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `avg_price` bigint NOT NULL,
  `crawled_at` datetime(6) NOT NULL,
  `min_price` bigint NOT NULL,
  `sample_count` int NOT NULL,
  `trade_date` date NOT NULL,
  `item_id` bigint NOT NULL,
  `server_id` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_material_price_history_item_server_date` (`item_id`,`server_id`,`trade_date`),
  KEY `FKssml65wjhwc294upyc5d91krk` (`server_id`),
  CONSTRAINT `FKf29utvme8ncq9a4mai0493xuj` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `FKssml65wjhwc294upyc5d91krk` FOREIGN KEY (`server_id`) REFERENCES `servers` (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mercenaries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `category` enum('DIVINE_BEAST','EVIL_BEAST','EVIL_BEAST_AWAKENING','EVOLVE_MONSTER','FIRST_GRADE_GENERAL','FOUR_HEAVENLY_KINGS','FOUR_HEAVENLY_KINGS_AWAKENING','GENERAL_AWAKENING','HIRED_MONSTER','LEGENDARY_GENERAL','MERCENARY','MODIFIED_GENERAL','MYEONG_KING','MYEONG_KING_AWAKENING','PROTAGONIST','SECOND_GRADE_GENERAL','SPIRIT_MONSTER') DEFAULT NULL,
  `is_coming_soon` bit(1) NOT NULL,
  `crawled_at` datetime(6) DEFAULT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `mercenary_key` varchar(100) DEFAULT NULL,
  `mercenary_type` enum('AWAKENED_HEAVENLY_KING','AWAKENED_MYUNGWANG','HEAVENLY_KING','LEGEND_GENERAL','MAIN_CHARACTER','MYUNGWANG','NORMAL_MERCENARY') DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `nation` enum('CHINA','INDIA','JAPAN','JOSEON','MONGOL','NONE','TAIWAN') DEFAULT NULL,
  `nature` enum('EARTH','FIRE','NONE','THUNDER','WATER','WIND') DEFAULT NULL,
  `nature_value` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKhevf4vhk0th8r1k2fpv4xdo1n` (`name`),
  UNIQUE KEY `UKqhrogmgu4x62yhm0iubkefohn` (`mercenary_key`)
) ENGINE=InnoDB AUTO_INCREMENT=372 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mercenary_characteristic_levels` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` varchar(20) NOT NULL,
  `amount_value` float DEFAULT NULL,
  `label` varchar(100) NOT NULL,
  `level` int NOT NULL,
  `stat_type` enum('ALL_STAT','ATTACK_POWER','ATTACK_SPEED','BASE_DAMAGE_MULTIPLIER','CRITICAL_CHANCE','CRITICAL_DAMAGE','CRITICAL_RATE','DAMAGE_PERCENT','DAMAGE_PERCENT_AIR','DAMAGE_PERCENT_GROUND','DEFENSE','DEXTERITY','ELEMENT_PIERCE','ELEMENT_VALUE','FIELD_MOVE_SPEED','HITTING_RESISTANCE','HITTING_RESISTANCE_PIERCE','HIT_RATE','HP_RECOVERY','INTELLECT','MAGIC_RESISTANCE','MAGIC_RESISTANCE_PIERCE','MAIN_STAT_FLAT','MAX_DAMAGE','MAX_POWER','MIN_DAMAGE','MIN_POWER','MOVE_SPEED','MP_RECOVERY','RESIST_PIERCE','SIGHT','SKILL_DAMAGE_PERCENT','SKILL_RANGE','STRENGTH','STUN_DURATION','VITALITY') DEFAULT NULL,
  `characteristic_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_mercenary_characteristic_levels` (`characteristic_id`,`label`,`level`),
  CONSTRAINT `FKnu3k5vi8xobrnhcn1cnacy37k` FOREIGN KEY (`characteristic_id`) REFERENCES `mercenary_characteristics` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=835 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mercenary_characteristics` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `apply_type` enum('ALLY_AUTO','NORMAL','SELF_AUTO') NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `characteristic_key` varchar(100) NOT NULL,
  `name` varchar(50) NOT NULL,
  `point` int DEFAULT NULL,
  `required_characteristic_key` varchar(100) DEFAULT NULL,
  `mercenary_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_mercenary_characteristics_key` (`characteristic_key`),
  KEY `FK51bfohp4gn37yo0ad6sfabgo3` (`mercenary_id`),
  CONSTRAINT `FK51bfohp4gn37yo0ad6sfabgo3` FOREIGN KEY (`mercenary_id`) REFERENCES `mercenaries` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=160 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mercenary_materials` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `material_item_key` varchar(200) DEFAULT NULL,
  `quantity` int NOT NULL,
  `required_credit` int DEFAULT NULL,
  `required_level` int DEFAULT NULL,
  `material_mercenary_id` bigint DEFAULT NULL,
  `result_mercenary_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKrtq2r2ukehbjrp4emwmtew4gb` (`material_mercenary_id`),
  KEY `FKaiuif1vhmo8c89to0hd8b091j` (`result_mercenary_id`),
  CONSTRAINT `FKaiuif1vhmo8c89to0hd8b091j` FOREIGN KEY (`result_mercenary_id`) REFERENCES `mercenaries` (`id`),
  CONSTRAINT `FKrtq2r2ukehbjrp4emwmtew4gb` FOREIGN KEY (`material_mercenary_id`) REFERENCES `mercenaries` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mercenary_skill_effects` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stat_key` enum('ALL_STAT','ATTACK_POWER','ATTACK_SPEED','BASE_DAMAGE_MULTIPLIER','CRITICAL_CHANCE','CRITICAL_DAMAGE','CRITICAL_RATE','DAMAGE_PERCENT','DAMAGE_PERCENT_AIR','DAMAGE_PERCENT_GROUND','DEFENSE','DEXTERITY','ELEMENT_PIERCE','ELEMENT_VALUE','FIELD_MOVE_SPEED','HITTING_RESISTANCE','HITTING_RESISTANCE_PIERCE','HIT_RATE','HP_RECOVERY','INTELLECT','MAGIC_RESISTANCE','MAGIC_RESISTANCE_PIERCE','MAIN_STAT_FLAT','MAX_DAMAGE','MAX_POWER','MIN_DAMAGE','MIN_POWER','MOVE_SPEED','MP_RECOVERY','RESIST_PIERCE','SIGHT','SKILL_DAMAGE_PERCENT','SKILL_RANGE','STRENGTH','STUN_DURATION','VITALITY') NOT NULL,
  `stat_value` int NOT NULL,
  `skill_id` bigint NOT NULL,
  `value_type` enum('FLAT','PERCENT') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_skill_effect_skill_stat` (`skill_id`,`stat_key`),
  CONSTRAINT `FKnp3eyn8ooxlglx584sarg8jmh` FOREIGN KEY (`skill_id`) REFERENCES `mercenary_skills` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mercenary_skills` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `skill_key` varchar(100) DEFAULT NULL,
  `skill_name` varchar(100) NOT NULL,
  `mercenary_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_mercenary_skills_merc_skill` (`mercenary_id`,`skill_name`),
  CONSTRAINT `FKhevpat9ctro5g5fq5xli2gniy` FOREIGN KEY (`mercenary_id`) REFERENCES `mercenaries` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=325 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mercenary_stats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stat_key` enum('ALL_STAT','ATTACK_POWER','ATTACK_SPEED','BASE_DAMAGE_MULTIPLIER','CRITICAL_CHANCE','CRITICAL_DAMAGE','CRITICAL_RATE','DAMAGE_PERCENT','DAMAGE_PERCENT_AIR','DAMAGE_PERCENT_GROUND','DEFENSE','DEXTERITY','ELEMENT_PIERCE','ELEMENT_VALUE','FIELD_MOVE_SPEED','HITTING_RESISTANCE','HITTING_RESISTANCE_PIERCE','HIT_RATE','HP_RECOVERY','INTELLECT','MAGIC_RESISTANCE','MAGIC_RESISTANCE_PIERCE','MAIN_STAT_FLAT','MAX_DAMAGE','MAX_POWER','MIN_DAMAGE','MIN_POWER','MOVE_SPEED','MP_RECOVERY','RESIST_PIERCE','SIGHT','SKILL_DAMAGE_PERCENT','SKILL_RANGE','STRENGTH','STUN_DURATION','VITALITY') NOT NULL,
  `stat_value` int NOT NULL,
  `mercenary_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_mercenary_stats_mercenary_stat_key` (`mercenary_id`,`stat_key`),
  CONSTRAINT `FKom93ac3ikopgm0bqas84uabhm` FOREIGN KEY (`mercenary_id`) REFERENCES `mercenaries` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1970 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `monsters` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `element` enum('ADAPTIVE','EARTH','FIRE','NONE','THUNDER','WATER','WIND') DEFAULT NULL,
  `element_value` int DEFAULT NULL,
  `hitting_resistance` int DEFAULT NULL,
  `hp` bigint DEFAULT NULL,
  `magic_resistance` int DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `hidden` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_monsters_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1427 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `chat_room_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `message` varchar(500) NOT NULL,
  `is_read` bit(1) NOT NULL,
  `type` enum('ABUSE_SUSPECTED','CASH_TRADE_DETECTED','CHAT_MESSAGE','CHAT_OPENED','POSTER_CONFIRMED','REPORT_PROCESSED','REPORT_RECEIVED','REVIEW_PUBLISHED','REVIEW_REQUESTED','TRADE_COMPLETED','USER_BLOCKED','USER_WARNED') NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notifications_user_read` (`user_id`,`is_read`),
  CONSTRAINT `FK9y21adhxn0ayjhfocscqox7bh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `player_character_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `gender` enum('FEMALE','MALE') NOT NULL,
  `job_type` enum('FIRST','NORMAL','SECOND') NOT NULL,
  `nation` enum('CHINA','INDIA','JAPAN','JOSEON','MONGOL','NONE','TAIWAN') NOT NULL,
  `mercenary_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtbensvvuc5l0aoqmtoxm6qvt7` (`mercenary_id`),
  CONSTRAINT `FKq43hwel6y920eb3ko1s86vtt` FOREIGN KEY (`mercenary_id`) REFERENCES `mercenaries` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `token` varchar(512) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK7tdcd6ab5wsgoudnvj7xf1b7l` (`user_id`),
  KEY `idx_refresh_tokens_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `admin_note` varchar(500) DEFAULT NULL,
  `chat_room_id` bigint DEFAULT NULL,
  `description` varchar(1000) NOT NULL,
  `evidence_url` varchar(500) DEFAULT NULL,
  `processed_at` datetime(6) DEFAULT NULL,
  `reason_category` enum('ABUSE','CASH_TRADE','FAKE_LISTING','FRAUD','OTHER') NOT NULL,
  `reporter_type` enum('SYSTEM','USER') NOT NULL,
  `status` enum('DISMISSED','PENDING','PROCESSED','REVIEWING') NOT NULL,
  `target_id` bigint NOT NULL,
  `target_type` enum('CHAT_MESSAGE','TRADE_LISTING','USER','WANTED_LISTING') NOT NULL,
  `processed_by` bigint DEFAULT NULL,
  `reporter_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKp8kw406ca0syx5wkphb6oelt5` (`processed_by`),
  KEY `FKd3qiw2om5d2oh5xb7fbdcq225` (`reporter_id`),
  CONSTRAINT `FKd3qiw2om5d2oh5xb7fbdcq225` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKp8kw406ca0syx5wkphb6oelt5` FOREIGN KEY (`processed_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ritual_applicabilities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `equipment_item_id` bigint NOT NULL,
  `ritual_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_ritual_applicability_ritual_equipment` (`ritual_id`,`equipment_item_id`),
  KEY `FKt2o4hm67dlhmus4vd1pte7f3x` (`equipment_item_id`),
  CONSTRAINT `FKjfvy4clmb15ug8tuywpg26ggi` FOREIGN KEY (`ritual_id`) REFERENCES `rituals` (`id`),
  CONSTRAINT `FKt2o4hm67dlhmus4vd1pte7f3x` FOREIGN KEY (`equipment_item_id`) REFERENCES `equipment_items` (`item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=164 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ritual_set_effects` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `element` enum('ADAPTIVE','EARTH','FIRE','NONE','THUNDER','WATER','WIND') NOT NULL,
  `outcome` enum('GREAT_SUCCESS','SUCCESS') NOT NULL,
  `required_ritual_pieces` int NOT NULL,
  `stat_type` enum('ALL_STAT','ATTACK_POWER','ATTACK_SPEED','BASE_DAMAGE_MULTIPLIER','CRITICAL_CHANCE','CRITICAL_DAMAGE','CRITICAL_RATE','DAMAGE_PERCENT','DAMAGE_PERCENT_AIR','DAMAGE_PERCENT_GROUND','DEFENSE','DEXTERITY','ELEMENT_PIERCE','ELEMENT_VALUE','FIELD_MOVE_SPEED','HITTING_RESISTANCE','HITTING_RESISTANCE_PIERCE','HIT_RATE','HP_RECOVERY','INTELLECT','MAGIC_RESISTANCE','MAGIC_RESISTANCE_PIERCE','MAIN_STAT_FLAT','MAX_DAMAGE','MAX_POWER','MIN_DAMAGE','MIN_POWER','MOVE_SPEED','MP_RECOVERY','RESIST_PIERCE','SIGHT','SKILL_DAMAGE_PERCENT','SKILL_RANGE','STRENGTH','STUN_DURATION','VITALITY') NOT NULL,
  `stat_unit` enum('FLAT','LEVEL','PERCENT') NOT NULL,
  `stat_value` int NOT NULL,
  `equipment_set_id` bigint NOT NULL,
  `ritual_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_ritual_set_effects_ritual_outcome_set_pieces_stat` (`ritual_id`,`outcome`,`equipment_set_id`,`required_ritual_pieces`,`stat_type`),
  KEY `FK8jq1tg1ysvwpk3mkg8lvy7xwr` (`equipment_set_id`),
  CONSTRAINT `FK8jq1tg1ysvwpk3mkg8lvy7xwr` FOREIGN KEY (`equipment_set_id`) REFERENCES `equipment_sets` (`id`),
  CONSTRAINT `FKf166j2tx9kk98qe22a6cx3dtx` FOREIGN KEY (`ritual_id`) REFERENCES `rituals` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=173 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ritual_stats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `element` enum('ADAPTIVE','EARTH','FIRE','NONE','THUNDER','WATER','WIND') NOT NULL,
  `outcome` enum('GREAT_SUCCESS','SUCCESS') NOT NULL,
  `stat_type` enum('ALL_STAT','ATTACK_POWER','ATTACK_SPEED','BASE_DAMAGE_MULTIPLIER','CRITICAL_CHANCE','CRITICAL_DAMAGE','CRITICAL_RATE','DAMAGE_PERCENT','DAMAGE_PERCENT_AIR','DAMAGE_PERCENT_GROUND','DEFENSE','DEXTERITY','ELEMENT_PIERCE','ELEMENT_VALUE','FIELD_MOVE_SPEED','HITTING_RESISTANCE','HITTING_RESISTANCE_PIERCE','HIT_RATE','HP_RECOVERY','INTELLECT','MAGIC_RESISTANCE','MAGIC_RESISTANCE_PIERCE','MAIN_STAT_FLAT','MAX_DAMAGE','MAX_POWER','MIN_DAMAGE','MIN_POWER','MOVE_SPEED','MP_RECOVERY','RESIST_PIERCE','SIGHT','SKILL_DAMAGE_PERCENT','SKILL_RANGE','STRENGTH','STUN_DURATION','VITALITY') NOT NULL,
  `stat_unit` enum('FLAT','LEVEL','PERCENT') NOT NULL,
  `stat_value` int NOT NULL,
  `ritual_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_ritual_stats_ritual_outcome_stat_element` (`ritual_id`,`outcome`,`stat_type`,`element`),
  CONSTRAINT `FK7xrarfuuqfhyfnqrmtgvic00x` FOREIGN KEY (`ritual_id`) REFERENCES `rituals` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rituals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `display_name` varchar(50) NOT NULL,
  `great_success_mark` varchar(20) DEFAULT NULL,
  `ritual_type` enum('ARMOR','WEAPON') NOT NULL,
  `success_mark` varchar(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `servers` (
  `server_id` int NOT NULL,
  `is_active` bit(1) NOT NULL,
  `name` varchar(20) NOT NULL,
  PRIMARY KEY (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `set_granted_skills` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `note` text,
  `skill_behavior_type` enum('ACTIVE','PASSIVE','TRIGGER') DEFAULT NULL,
  `skill_key` varchar(100) NOT NULL,
  `skill_name` varchar(100) NOT NULL,
  `stat_source` enum('AFFINITY','SELF') DEFAULT NULL,
  `trigger_base_skill_key` varchar(100) DEFAULT NULL,
  `trigger_every_n` int DEFAULT NULL,
  `trigger_source` enum('MERCENARY','SELF') DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `skill_coefficients` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `casts_per_second` float DEFAULT NULL,
  `coef_atk` float NOT NULL,
  `coef_dex` float NOT NULL,
  `coef_int` float NOT NULL,
  `coef_lvl` float NOT NULL,
  `coef_str` float NOT NULL,
  `coef_vit` float NOT NULL,
  `confidence` varchar(20) DEFAULT NULL,
  `damage_range_factor` float NOT NULL,
  `hit_count` int NOT NULL,
  `measurement_note` text,
  `row_id` varchar(100) DEFAULT NULL,
  `skill_type` enum('INSTANT','PERSISTENT','TRIGGER') DEFAULT NULL,
  `tick_interval_ms` int DEFAULT NULL,
  `item_skill_id` bigint DEFAULT NULL,
  `mercenary_skill_id` bigint DEFAULT NULL,
  `set_granted_skill_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_skill_coefficients_row_id` (`row_id`),
  KEY `FK8el7j0m8nlmx8nkuoriyjy9sl` (`item_skill_id`),
  KEY `FKcnygpsiq2mr3v24ga35psqcav` (`mercenary_skill_id`),
  KEY `FKmsqvlnq289borvphll5moj23l` (`set_granted_skill_id`),
  CONSTRAINT `FK8el7j0m8nlmx8nkuoriyjy9sl` FOREIGN KEY (`item_skill_id`) REFERENCES `item_skills` (`id`),
  CONSTRAINT `FKcnygpsiq2mr3v24ga35psqcav` FOREIGN KEY (`mercenary_skill_id`) REFERENCES `mercenary_skills` (`id`),
  CONSTRAINT `FKmsqvlnq289borvphll5moj23l` FOREIGN KEY (`set_granted_skill_id`) REFERENCES `set_granted_skills` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spirit_buffs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `element` enum('ADAPTIVE','EARTH','FIRE','NONE','THUNDER','WATER','WIND') NOT NULL,
  `stat_type` enum('ALL_STAT','ATTACK_POWER','ATTACK_SPEED','BASE_DAMAGE_MULTIPLIER','CRITICAL_CHANCE','CRITICAL_DAMAGE','CRITICAL_RATE','DAMAGE_PERCENT','DAMAGE_PERCENT_AIR','DAMAGE_PERCENT_GROUND','DEFENSE','DEXTERITY','ELEMENT_PIERCE','ELEMENT_VALUE','FIELD_MOVE_SPEED','HITTING_RESISTANCE','HITTING_RESISTANCE_PIERCE','HIT_RATE','HP_RECOVERY','INTELLECT','MAGIC_RESISTANCE','MAGIC_RESISTANCE_PIERCE','MAIN_STAT_FLAT','MAX_DAMAGE','MAX_POWER','MIN_DAMAGE','MIN_POWER','MOVE_SPEED','MP_RECOVERY','RESIST_PIERCE','SIGHT','SKILL_DAMAGE_PERCENT','SKILL_RANGE','STRENGTH','STUN_DURATION','VITALITY') NOT NULL,
  `stat_unit` enum('FLAT','LEVEL','PERCENT') NOT NULL,
  `target` enum('ALLY','ALLY_HEAVENLY_KING','ALLY_SAME_ELEMENT','ENEMY','SELF') NOT NULL,
  `value` float NOT NULL,
  `spirit_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnre5wml7xrwa7w1gowona2f9r` (`spirit_id`),
  CONSTRAINT `FKnre5wml7xrwa7w1gowona2f9r` FOREIGN KEY (`spirit_id`) REFERENCES `spirits` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=67 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spirits` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `acquire_condition` text,
  `grade` enum('HIGHEST','LEGEND','LOWER','MIDDLE','UPPER') NOT NULL,
  `name` varchar(100) NOT NULL,
  `nature` enum('EARTH','FIRE','NONE','THUNDER','WATER','WIND') NOT NULL,
  `special_effect_note` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_spirits_nature_grade` (`nature`,`grade`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trade_applications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `message` varchar(500) DEFAULT NULL,
  `responded_at` datetime(6) DEFAULT NULL,
  `status` enum('ACCEPTED','CANCELLED','PENDING','REJECTED') NOT NULL,
  `buyer_id` bigint NOT NULL,
  `listing_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKrh2xc091kk3i31isw4sd1noyy` (`buyer_id`),
  KEY `FKndq28x4bd3mt2k97jxse700eu` (`listing_id`),
  CONSTRAINT `FKndq28x4bd3mt2k97jxse700eu` FOREIGN KEY (`listing_id`) REFERENCES `trade_listings` (`id`),
  CONSTRAINT `FKrh2xc091kk3i31isw4sd1noyy` FOREIGN KEY (`buyer_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trade_confirmed` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cancelled` bit(1) NOT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `confirmed_at` datetime(6) NOT NULL,
  `confirmed_price` bigint NOT NULL,
  `listing_type` enum('BUY','SELL') NOT NULL,
  `server_snapshot` varchar(30) NOT NULL,
  `stat_key_snapshot` varchar(255) NOT NULL,
  `buyer_id` bigint DEFAULT NULL,
  `chat_room_id` bigint DEFAULT NULL,
  `seller_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhjhkkq16pl82k5jy5gnlqjlwt` (`buyer_id`),
  KEY `FKqqmtd4o5hebq2fagaw7dh4tkd` (`chat_room_id`),
  KEY `FKixy6o4glhwq097jxghvje5kff` (`seller_id`),
  CONSTRAINT `FKhjhkkq16pl82k5jy5gnlqjlwt` FOREIGN KEY (`buyer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKixy6o4glhwq097jxghvje5kff` FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKqqmtd4o5hebq2fagaw7dh4tkd` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_rooms` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trade_listings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `hidden` bit(1) NOT NULL,
  `note` varchar(500) DEFAULT NULL,
  `price` bigint NOT NULL,
  `server` varchar(30) NOT NULL,
  `status` enum('ACTIVE','CANCELLED','IN_TRADE','SOLD') NOT NULL,
  `seller_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK3tk2w355tgwrgqom5ul729qcl` (`seller_id`),
  CONSTRAINT `FK3tk2w355tgwrgqom5ul729qcl` FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trade_reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `is_published` bit(1) NOT NULL,
  `rating` enum('BAD','GOOD','NEUTRAL') DEFAULT NULL,
  `reveal_at` datetime(6) NOT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `reviewer_id` bigint NOT NULL,
  `target_id` bigint NOT NULL,
  `trade_confirmed_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_trade_reviews_confirmed_reviewer` (`trade_confirmed_id`,`reviewer_id`),
  KEY `idx_trade_reviews_reveal_at` (`reveal_at`,`is_published`),
  KEY `FK49n69h4aeubh6c2q40ktic6pr` (`reviewer_id`),
  KEY `FKn3n9pyxk8dvqhgjxcfol1fped` (`target_id`),
  CONSTRAINT `FK49n69h4aeubh6c2q40ktic6pr` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKn3n9pyxk8dvqhgjxcfol1fped` FOREIGN KEY (`target_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKqg9gn1n6c3pcsx60yvyhmpid` FOREIGN KEY (`trade_confirmed_id`) REFERENCES `trade_confirmed` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trade_stat_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `price_max` bigint NOT NULL,
  `price_min` bigint NOT NULL,
  `price_sum` bigint NOT NULL,
  `quantity_sum` bigint NOT NULL,
  `stat_date` date NOT NULL,
  `stat_key` varchar(255) NOT NULL,
  `trade_count` int NOT NULL,
  `server_id` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_trade_stat_daily_date_key` (`stat_date`,`stat_key`),
  UNIQUE KEY `uq_trade_stat_daily_date_key_server` (`stat_date`,`stat_key`,`server_id`),
  KEY `FK50fe5bbtlhfr10w9iweydooyy` (`server_id`),
  CONSTRAINT `FK50fe5bbtlhfr10w9iweydooyy` FOREIGN KEY (`server_id`) REFERENCES `servers` (`server_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trade_stat_monthly` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `avg_price` bigint NOT NULL,
  `price_max` bigint DEFAULT NULL,
  `price_min` bigint DEFAULT NULL,
  `quantity_sum` bigint NOT NULL,
  `stat_key` varchar(255) NOT NULL,
  `stat_month` varchar(7) NOT NULL,
  `trade_count` int NOT NULL,
  `server_id` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_trade_stat_monthly_month_key` (`stat_month`,`stat_key`),
  UNIQUE KEY `uq_trade_stat_monthly_month_key_server` (`stat_month`,`stat_key`,`server_id`),
  KEY `FKhj463cx05f9epxdkywwkatnl9` (`server_id`),
  CONSTRAINT `FKhj463cx05f9epxdkywwkatnl9` FOREIGN KEY (`server_id`) REFERENCES `servers` (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_clear_times` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `clear_time_seconds` int NOT NULL,
  `deck_id` bigint DEFAULT NULL,
  `recorded_at` datetime(6) NOT NULL,
  `monster_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `is_public` bit(1) NOT NULL,
  `exp_granted` bit(1) NOT NULL,
  `final_dps` bigint NOT NULL,
  `status` enum('ACTIVE','HIDDEN') NOT NULL,
  `deck_snapshot_id` bigint NOT NULL,
  `adjust_dps` bigint DEFAULT NULL,
  `raw_dps` bigint DEFAULT NULL,
  `total_element_pierce` int DEFAULT NULL,
  `total_resist_pierce` int DEFAULT NULL,
  `effective_monster_element` int DEFAULT NULL,
  `resist_after_debuff` int DEFAULT NULL,
  `resist_pass_rate` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKds9db05mm27gp7k4h2pfpcert` (`monster_id`),
  KEY `FKovyjbov1eb13w40s711lpwr4a` (`user_id`),
  KEY `FKkmbk1ps64hk5w9675coprg21y` (`deck_snapshot_id`),
  CONSTRAINT `FKds9db05mm27gp7k4h2pfpcert` FOREIGN KEY (`monster_id`) REFERENCES `monsters` (`id`),
  CONSTRAINT `FKkmbk1ps64hk5w9675coprg21y` FOREIGN KEY (`deck_snapshot_id`) REFERENCES `deck_snapshots` (`id`),
  CONSTRAINT `FKovyjbov1eb13w40s711lpwr4a` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_deck_member_characteristics` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `selected_level` int NOT NULL,
  `characteristic_id` bigint NOT NULL,
  `deck_member_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_deck_member_characteristics` (`deck_member_id`,`characteristic_id`),
  KEY `FK10mxateuk1brjrtmah40ww616` (`characteristic_id`),
  CONSTRAINT `FK10mxateuk1brjrtmah40ww616` FOREIGN KEY (`characteristic_id`) REFERENCES `mercenary_characteristics` (`id`),
  CONSTRAINT `FK6p409yc6d2hffw6j7bb7pbpe9` FOREIGN KEY (`deck_member_id`) REFERENCES `user_deck_members` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=58 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_deck_member_equips` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enhance_level` int DEFAULT NULL,
  `has_affinity` bit(1) NOT NULL,
  `set_piece_count` int DEFAULT NULL,
  `deck_member_id` bigint NOT NULL,
  `equipment_item_id` bigint DEFAULT NULL,
  `equipment_set_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKli1kc7sxjn06qfxvn95r6ewtf` (`deck_member_id`),
  KEY `FK5wlqj83e503v1c0ex618ma0xp` (`equipment_item_id`),
  KEY `FK5pyu7ji7cfauf25rrvhc95v44` (`equipment_set_id`),
  CONSTRAINT `FK5pyu7ji7cfauf25rrvhc95v44` FOREIGN KEY (`equipment_set_id`) REFERENCES `equipment_sets` (`id`),
  CONSTRAINT `FK5wlqj83e503v1c0ex618ma0xp` FOREIGN KEY (`equipment_item_id`) REFERENCES `equipment_items` (`item_id`),
  CONSTRAINT `FKl1aml5vqvkf5cnmg4snatudby` FOREIGN KEY (`deck_member_id`) REFERENCES `user_deck_members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_deck_member_slot_rituals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `outcome` enum('GREAT_SUCCESS','SUCCESS') NOT NULL,
  `deck_member_slot_id` bigint NOT NULL,
  `ritual_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3qdajwk8vphhoon1l6ux0d78v` (`deck_member_slot_id`),
  KEY `FKj3wn8kuof86ft2iihu1855idw` (`ritual_id`),
  CONSTRAINT `FKj3wn8kuof86ft2iihu1855idw` FOREIGN KEY (`ritual_id`) REFERENCES `rituals` (`id`),
  CONSTRAINT `FKjsurpri38jewq0kifpwn1504b` FOREIGN KEY (`deck_member_slot_id`) REFERENCES `user_deck_member_slots` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_deck_member_slots` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `slot` enum('APP_ARMOR','APP_BRACELET','APP_EARRING','APP_GREAVES','APP_HELMET','APP_NECKLACE','APP_SPIRIT','APP_WAR_GOD','APP_WEAPON','ARMOR','BELT','CHARM','GLOVES','HELMET','RING_1','RING_2','SHOES','WEAPON') NOT NULL,
  `deck_member_id` bigint NOT NULL,
  `equipment_item_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_deck_member_slot` (`deck_member_id`,`slot`),
  KEY `FKmuoyglykr2lpl7gtt3sf2fbw2` (`equipment_item_id`),
  CONSTRAINT `FKirovgwqc43lvkwpf5vos83hx4` FOREIGN KEY (`deck_member_id`) REFERENCES `user_deck_members` (`id`),
  CONSTRAINT `FKmuoyglykr2lpl7gtt3sf2fbw2` FOREIGN KEY (`equipment_item_id`) REFERENCES `equipment_items` (`item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_deck_members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bonus_amount` int NOT NULL,
  `bonus_target` enum('MAIN_STAT','VITALITY') NOT NULL,
  `level` int NOT NULL,
  `deck_id` bigint NOT NULL,
  `mercenary_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_deck_members_deck_mercenary` (`deck_id`,`mercenary_id`),
  KEY `FK6gqde32ohudyogx8fuykoscr7` (`mercenary_id`),
  CONSTRAINT `FK6gqde32ohudyogx8fuykoscr7` FOREIGN KEY (`mercenary_id`) REFERENCES `mercenaries` (`id`),
  CONSTRAINT `FKlylic1xbne5lha45klnc9o0nv` FOREIGN KEY (`deck_id`) REFERENCES `user_decks` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_decks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `attr_x_value` int DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `gaho_level` int DEFAULT NULL,
  `gonmyeong_level` int DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  `total_res_down` int DEFAULT NULL,
  `cheungjin_source_id` bigint DEFAULT NULL,
  `jinbeop_source_id` bigint DEFAULT NULL,
  `spirit_1_id` bigint DEFAULT NULL,
  `spirit_2_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK3hkyosq5gfjkx2fg5bdeicvgo` (`cheungjin_source_id`),
  KEY `FK8y2fk66xn4baus2wuqy2mcaot` (`jinbeop_source_id`),
  KEY `FKi8392mmfsg69m8ejjjdokri7x` (`spirit_1_id`),
  KEY `FK5kiw8gwnjstg9svvhyupjvnqs` (`spirit_2_id`),
  KEY `FK8ltdfcae88jxudtpojb005g9h` (`user_id`),
  CONSTRAINT `FK3hkyosq5gfjkx2fg5bdeicvgo` FOREIGN KEY (`cheungjin_source_id`) REFERENCES `deck_buff_source` (`id`),
  CONSTRAINT `FK5kiw8gwnjstg9svvhyupjvnqs` FOREIGN KEY (`spirit_2_id`) REFERENCES `spirits` (`id`),
  CONSTRAINT `FK8ltdfcae88jxudtpojb005g9h` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK8y2fk66xn4baus2wuqy2mcaot` FOREIGN KEY (`jinbeop_source_id`) REFERENCES `deck_buff_source` (`id`),
  CONSTRAINT `FKi8392mmfsg69m8ejjjdokri7x` FOREIGN KEY (`spirit_1_id`) REFERENCES `spirits` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_watch_targets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `composition` enum('BANSSANG','BYEON','FULL','FULL_BANSSANG','GAMTU') DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `ritual_count` int DEFAULT NULL,
  `ritual_mark` varchar(20) DEFAULT NULL,
  `sort_order` int NOT NULL,
  `target_type` enum('ITEM','SET') NOT NULL,
  `watch_key` varchar(255) NOT NULL,
  `set_id` bigint DEFAULT NULL,
  `item_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_uwt_user_watch_key` (`user_id`,`watch_key`),
  KEY `FKchicw736sn6vkkxxwnw2dfr69` (`set_id`),
  KEY `FKfrwh1ntsrhs7217efqq2gkfmo` (`item_id`),
  CONSTRAINT `FKchicw736sn6vkkxxwnw2dfr69` FOREIGN KEY (`set_id`) REFERENCES `equipment_sets` (`id`),
  CONSTRAINT `FKfrwh1ntsrhs7217efqq2gkfmo` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `FKgeh1gpyhvm8ppsapxn6n56kww` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `block_reason` varchar(500) DEFAULT NULL,
  `blocked_until` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `email` varchar(100) NOT NULL,
  `game_access_time` varchar(100) DEFAULT NULL,
  `game_nickname` varchar(50) DEFAULT NULL,
  `grade` enum('BOSANG','DAESANG','GAEKSANG','GEOSANG','HAENGSANG') NOT NULL,
  `grade_step` int DEFAULT NULL,
  `manner_score` int NOT NULL,
  `nickname` varchar(50) NOT NULL,
  `oauth_id` varchar(255) NOT NULL,
  `oauth_provider` varchar(20) NOT NULL,
  `profile_image_url` varchar(500) DEFAULT NULL,
  `role` enum('ADMIN','USER') NOT NULL,
  `status` enum('ACTIVE','BLOCKED') NOT NULL,
  `total_exp` bigint NOT NULL,
  `trade_count` int NOT NULL,
  `server_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_users_oauth_provider_id` (`oauth_provider`,`oauth_id`),
  KEY `FKmrcdiruupua2dloas8pcxj64o` (`server_id`),
  CONSTRAINT `FKmrcdiruupua2dloas8pcxj64o` FOREIGN KEY (`server_id`) REFERENCES `servers` (`server_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `value_metric_monthly` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `avg_price` bigint NOT NULL,
  `element` enum('ADAPTIVE','EARTH','FIRE','NONE','THUNDER','WATER','WIND') NOT NULL,
  `month` varchar(7) NOT NULL,
  `stat_type` enum('ALL_STAT','ATTACK_POWER','ATTACK_SPEED','BASE_DAMAGE_MULTIPLIER','CRITICAL_CHANCE','CRITICAL_DAMAGE','CRITICAL_RATE','DAMAGE_PERCENT','DAMAGE_PERCENT_AIR','DAMAGE_PERCENT_GROUND','DEFENSE','DEXTERITY','ELEMENT_PIERCE','ELEMENT_VALUE','FIELD_MOVE_SPEED','HITTING_RESISTANCE','HITTING_RESISTANCE_PIERCE','HIT_RATE','HP_RECOVERY','INTELLECT','MAGIC_RESISTANCE','MAGIC_RESISTANCE_PIERCE','MAIN_STAT_FLAT','MAX_DAMAGE','MAX_POWER','MIN_DAMAGE','MIN_POWER','MOVE_SPEED','MP_RECOVERY','RESIST_PIERCE','SIGHT','SKILL_DAMAGE_PERCENT','SKILL_RANGE','STRENGTH','STUN_DURATION','VITALITY') NOT NULL,
  `stat_value` int NOT NULL,
  `trade_count` int NOT NULL,
  `value_for_money` double NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_value_metric_monthly_month_item_stat_element` (`month`,`item_id`,`stat_type`,`element`),
  KEY `FKh0f8u5l1gxln3igd0c2bpxspq` (`item_id`),
  CONSTRAINT `FKh0f8u5l1gxln3igd0c2bpxspq` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wanted_equipment_conditions` (
  `wanted_item_id` bigint NOT NULL,
  `has_ritual` bit(1) NOT NULL,
  `min_enhance_level` int DEFAULT NULL,
  PRIMARY KEY (`wanted_item_id`),
  CONSTRAINT `FKlt18n1mjbujmutql16lwfuamx` FOREIGN KEY (`wanted_item_id`) REFERENCES `wanted_items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wanted_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quantity` int NOT NULL,
  `sort_order` int NOT NULL,
  `item_id` bigint NOT NULL,
  `wanted_listing_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKt7gxwoixh55t76gqs9mrhjxjh` (`item_id`),
  KEY `FKdlugm9kr1yi9svjttqm2nhpgp` (`wanted_listing_id`),
  CONSTRAINT `FKdlugm9kr1yi9svjttqm2nhpgp` FOREIGN KEY (`wanted_listing_id`) REFERENCES `wanted_listings` (`id`),
  CONSTRAINT `FKt7gxwoixh55t76gqs9mrhjxjh` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wanted_listings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `hidden` bit(1) NOT NULL,
  `note` varchar(500) DEFAULT NULL,
  `offered_price` bigint NOT NULL,
  `server` varchar(30) NOT NULL,
  `status` enum('CANCELLED','IN_TRADE','OPEN','PURCHASED') NOT NULL,
  `buyer_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjh5csgr72q4rgag9q6igiuofk` (`buyer_id`),
  CONSTRAINT `FKjh5csgr72q4rgag9q6igiuofk` FOREIGN KEY (`buyer_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wanted_ritual_conditions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `preferred_outcome` enum('ANY','GREAT_SUCCESS','SUCCESS') NOT NULL,
  `ritual_id` bigint NOT NULL,
  `wanted_item_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_wanted_ritual_conditions_item_ritual` (`wanted_item_id`,`ritual_id`),
  KEY `FK6yqhus8xs3hgltfmv38jjtba3` (`ritual_id`),
  CONSTRAINT `FK6yqhus8xs3hgltfmv38jjtba3` FOREIGN KEY (`ritual_id`) REFERENCES `rituals` (`id`),
  CONSTRAINT `FKtp4puu78uh5w8p49h6nr968dy` FOREIGN KEY (`wanted_item_id`) REFERENCES `wanted_items` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

