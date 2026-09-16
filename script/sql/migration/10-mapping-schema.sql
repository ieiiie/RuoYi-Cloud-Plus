CREATE DATABASE IF NOT EXISTS `${MAPPING_DB}` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `${MAPPING_DB}`.`tenant_map` (
  `old_tenant_id` varchar(20) NOT NULL,
  `target_tenant_id` varchar(20) DEFAULT NULL,
  `match_rule` varchar(32) NOT NULL,
  `conflict_reason` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`old_tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `${MAPPING_DB}`.`user_map` (
  `old_user_id` bigint NOT NULL,
  `old_global_user_id` bigint DEFAULT NULL,
  `target_global_user_id` bigint DEFAULT NULL,
  `target_user_id` bigint DEFAULT NULL,
  `match_rule` varchar(64) NOT NULL,
  `conflict_reason` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`old_user_id`),
  KEY `idx_user_map_global` (`target_global_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `${MAPPING_DB}`.`role_map` (
  `old_role_id` bigint NOT NULL,
  `target_role_id` bigint DEFAULT NULL,
  `match_rule` varchar(32) NOT NULL,
  `conflict_reason` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`old_role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `${MAPPING_DB}`.`menu_map` (
  `old_menu_id` bigint NOT NULL,
  `target_menu_id` bigint DEFAULT NULL,
  `match_rule` varchar(32) NOT NULL,
  `conflict_reason` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`old_menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `${MAPPING_DB}`.`oss_map` (
  `old_oss_id` bigint NOT NULL,
  `target_oss_id` bigint DEFAULT NULL,
  `object_url_hash` char(64) NOT NULL,
  `match_rule` varchar(32) NOT NULL,
  `object_status` varchar(32) NOT NULL DEFAULT 'NOT_CHECKED',
  `conflict_reason` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`old_oss_id`),
  KEY `idx_oss_url_hash` (`object_url_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `${MAPPING_DB}`.`migration_run` (
  `run_id` varchar(64) NOT NULL,
  `round_no` int NOT NULL,
  `started_at` datetime NOT NULL,
  `finished_at` datetime DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `source_snapshot_at` datetime NOT NULL,
  `failure_reason` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
