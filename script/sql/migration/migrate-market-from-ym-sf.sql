-- 将旧单体库 ym-sf 的农业行情数据迁移至农业服务库。
-- 执行前请备份源库与目标库，并先执行 ym-agriculture.sql。
-- 脚本可重复执行，相同主键或业务唯一键会更新为源库当前值。

INSERT INTO `ym-agriculture`.`sf_market_source` (
  source_code, source_name, allowed_domains_json, authorization_note,
  enabled_flag, create_time, update_time
)
SELECT
  source_code, source_name, allowed_domains_json, authorization_note,
  enabled_flag, create_time, update_time
FROM `ym-sf`.`sf_market_source`
ON DUPLICATE KEY UPDATE
  source_name = VALUES(source_name),
  allowed_domains_json = VALUES(allowed_domains_json),
  authorization_note = VALUES(authorization_note),
  enabled_flag = VALUES(enabled_flag),
  create_time = VALUES(create_time),
  update_time = VALUES(update_time);

INSERT INTO `ym-agriculture`.`sf_market_product` (
  product_id, product_name, normalized_name, category, sort_order,
  enabled_flag, create_time, update_time
)
SELECT
  product_id, product_name, normalized_name, category, sort_order,
  enabled_flag, create_time, update_time
FROM `ym-sf`.`sf_market_product`
ON DUPLICATE KEY UPDATE
  product_name = VALUES(product_name),
  normalized_name = VALUES(normalized_name),
  category = VALUES(category),
  sort_order = VALUES(sort_order),
  enabled_flag = VALUES(enabled_flag),
  create_time = VALUES(create_time),
  update_time = VALUES(update_time);

INSERT INTO `ym-agriculture`.`sf_market_product_mapping` (
  mapping_id, source_code, external_product_key, product_id,
  last_source_name, create_time, update_time
)
SELECT
  mapping_id, source_code, external_product_key, product_id,
  last_source_name, create_time, update_time
FROM `ym-sf`.`sf_market_product_mapping`
ON DUPLICATE KEY UPDATE
  source_code = VALUES(source_code),
  external_product_key = VALUES(external_product_key),
  product_id = VALUES(product_id),
  last_source_name = VALUES(last_source_name),
  create_time = VALUES(create_time),
  update_time = VALUES(update_time);

INSERT INTO `ym-agriculture`.`sf_market_quote_ingest_staging` (
  id, request_id, crawl_run_id, source_code, external_product_key,
  product_name, specification, source_category, quote_type, price,
  unit, origin, market_name, quote_date, fetched_at_utc, source_url,
  raw_price_text, raw_payload_json, payload_sha256, ingest_status,
  result_action, process_attempts, warnings_json, correction_json,
  error_code, error_message, product_id, quote_id, handled_by,
  handler_name, handle_comment, handled_at, warning_acknowledged,
  received_at, processing_started_at, processed_at
)
SELECT
  id, request_id, crawl_run_id, source_code, external_product_key,
  product_name, specification, source_category, quote_type, price,
  unit, origin, market_name, quote_date, fetched_at_utc, source_url,
  raw_price_text, raw_payload_json, payload_sha256, ingest_status,
  result_action, process_attempts, warnings_json, correction_json,
  error_code, error_message, product_id, quote_id, handled_by,
  handler_name, handle_comment, handled_at, warning_acknowledged,
  received_at, processing_started_at, processed_at
FROM `ym-sf`.`sf_market_quote_ingest_staging`
ON DUPLICATE KEY UPDATE
  request_id = VALUES(request_id),
  crawl_run_id = VALUES(crawl_run_id),
  source_code = VALUES(source_code),
  external_product_key = VALUES(external_product_key),
  product_name = VALUES(product_name),
  specification = VALUES(specification),
  source_category = VALUES(source_category),
  quote_type = VALUES(quote_type),
  price = VALUES(price),
  unit = VALUES(unit),
  origin = VALUES(origin),
  market_name = VALUES(market_name),
  quote_date = VALUES(quote_date),
  fetched_at_utc = VALUES(fetched_at_utc),
  source_url = VALUES(source_url),
  raw_price_text = VALUES(raw_price_text),
  raw_payload_json = VALUES(raw_payload_json),
  payload_sha256 = VALUES(payload_sha256),
  ingest_status = VALUES(ingest_status),
  result_action = VALUES(result_action),
  process_attempts = VALUES(process_attempts),
  warnings_json = VALUES(warnings_json),
  correction_json = VALUES(correction_json),
  error_code = VALUES(error_code),
  error_message = VALUES(error_message),
  product_id = VALUES(product_id),
  quote_id = VALUES(quote_id),
  handled_by = VALUES(handled_by),
  handler_name = VALUES(handler_name),
  handle_comment = VALUES(handle_comment),
  handled_at = VALUES(handled_at),
  warning_acknowledged = VALUES(warning_acknowledged),
  received_at = VALUES(received_at),
  processing_started_at = VALUES(processing_started_at),
  processed_at = VALUES(processed_at);

INSERT INTO `ym-agriculture`.`sf_market_quote` (
  quote_id, latest_ingest_id, product_id, source_code,
  external_product_key, series_key_sha256, specification, quote_type,
  source_price, source_unit, price, unit, origin, market_name,
  quote_date, collected_at, source_url, previous_price, change_amount,
  change_percent, trend, warnings_json, publish_status, create_time, update_time
)
SELECT
  quote_id, latest_ingest_id, product_id, source_code,
  external_product_key, series_key_sha256, specification, quote_type,
  source_price, source_unit, price, unit, origin, market_name,
  quote_date, collected_at, source_url, previous_price, change_amount,
  change_percent, trend, warnings_json, publish_status, create_time, update_time
FROM `ym-sf`.`sf_market_quote`
ON DUPLICATE KEY UPDATE
  latest_ingest_id = VALUES(latest_ingest_id),
  product_id = VALUES(product_id),
  source_code = VALUES(source_code),
  external_product_key = VALUES(external_product_key),
  series_key_sha256 = VALUES(series_key_sha256),
  specification = VALUES(specification),
  quote_type = VALUES(quote_type),
  source_price = VALUES(source_price),
  source_unit = VALUES(source_unit),
  price = VALUES(price),
  unit = VALUES(unit),
  origin = VALUES(origin),
  market_name = VALUES(market_name),
  quote_date = VALUES(quote_date),
  collected_at = VALUES(collected_at),
  source_url = VALUES(source_url),
  previous_price = VALUES(previous_price),
  change_amount = VALUES(change_amount),
  change_percent = VALUES(change_percent),
  trend = VALUES(trend),
  warnings_json = VALUES(warnings_json),
  publish_status = VALUES(publish_status),
  create_time = VALUES(create_time),
  update_time = VALUES(update_time);

CREATE OR REPLACE SQL SECURITY DEFINER VIEW `ym-agriculture`.`v_sf_market_quote_ingest_receipt` AS
SELECT request_id, crawl_run_id, source_code, payload_sha256, ingest_status, result_action,
       product_id, quote_id, warnings_json, error_code, error_message, received_at, processed_at
FROM `ym-agriculture`.`sf_market_quote_ingest_staging`;
