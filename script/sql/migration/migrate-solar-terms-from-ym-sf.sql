-- 将旧单体库 ym-sf 的节气数据迁入 ym-agriculture。
-- 执行前先执行 script/sql/agriculture/ym-agriculture.sql，并确保两个库在同一 MySQL 实例。

SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO `ym-agriculture`.sf_solar_term_definition (
  term_code, term_name, term_order, solar_longitude, intro,
  seasonal_description, customs_json, content_version, enabled_flag,
  create_time, update_time
)
SELECT
  term_code, term_name, term_order, solar_longitude, intro,
  seasonal_description, customs_json, content_version, enabled_flag,
  create_time, update_time
FROM `ym-sf`.sf_solar_term_definition
ON DUPLICATE KEY UPDATE
  term_name = VALUES(term_name),
  term_order = VALUES(term_order),
  solar_longitude = VALUES(solar_longitude),
  intro = VALUES(intro),
  seasonal_description = VALUES(seasonal_description),
  customs_json = VALUES(customs_json),
  content_version = VALUES(content_version),
  enabled_flag = VALUES(enabled_flag),
  update_time = VALUES(update_time);

INSERT INTO `ym-agriculture`.sf_solar_term_occurrence (
  occurrence_id, term_year, term_code, term_name, occurred_at,
  gregorian_date, lunar_date_text, algorithm_version, create_time, update_time
)
SELECT
  occurrence_id, term_year, term_code, term_name, occurred_at,
  gregorian_date, lunar_date_text, algorithm_version, create_time, update_time
FROM `ym-sf`.sf_solar_term_occurrence
ON DUPLICATE KEY UPDATE
  term_name = VALUES(term_name),
  occurred_at = VALUES(occurred_at),
  gregorian_date = VALUES(gregorian_date),
  lunar_date_text = VALUES(lunar_date_text),
  algorithm_version = VALUES(algorithm_version),
  update_time = VALUES(update_time);

COMMIT;

SELECT 'sf_solar_term_definition' AS table_name,
       (SELECT COUNT(*) FROM `ym-sf`.sf_solar_term_definition) AS source_count,
       (SELECT COUNT(*) FROM `ym-agriculture`.sf_solar_term_definition) AS target_count
UNION ALL
SELECT 'sf_solar_term_occurrence',
       (SELECT COUNT(*) FROM `ym-sf`.sf_solar_term_occurrence),
       (SELECT COUNT(*) FROM `ym-agriculture`.sf_solar_term_occurrence);
