-- 将旧单体库 ym-sf 的溯源批次、溯源码和扫码日志迁移至农业服务库。
-- 执行前请备份源库与目标库；应先执行 ym-agriculture.sql 创建目标表。
-- 脚本可重复执行，相同主键会更新为源库当前值。

INSERT INTO `ym-agriculture`.`sf_trace_batch` (
  trace_batch_id, tenant_id, planting_batch_id, field_id, variety_id,
  trace_batch_no, product_name, quality_grade, origin_text, producer_name,
  certification_json, label_scope, planned_quantity, generated_quantity,
  status, del_flag, create_by, create_time, update_by, update_time, remark
)
SELECT
  trace_batch_id, tenant_id, planting_batch_id, field_id, variety_id,
  trace_batch_no, product_name, quality_grade, origin_text, producer_name,
  certification_json, label_scope, planned_quantity, generated_quantity,
  status, del_flag, create_by, create_time, update_by, update_time, remark
FROM `ym-sf`.`sf_trace_batch`
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  planting_batch_id = VALUES(planting_batch_id),
  field_id = VALUES(field_id),
  variety_id = VALUES(variety_id),
  trace_batch_no = VALUES(trace_batch_no),
  product_name = VALUES(product_name),
  quality_grade = VALUES(quality_grade),
  origin_text = VALUES(origin_text),
  producer_name = VALUES(producer_name),
  certification_json = VALUES(certification_json),
  label_scope = VALUES(label_scope),
  planned_quantity = VALUES(planned_quantity),
  generated_quantity = VALUES(generated_quantity),
  status = VALUES(status),
  del_flag = VALUES(del_flag),
  create_by = VALUES(create_by),
  create_time = VALUES(create_time),
  update_by = VALUES(update_by),
  update_time = VALUES(update_time),
  remark = VALUES(remark);

INSERT INTO `ym-agriculture`.`sf_trace_code` (
  trace_code_id, tenant_id, trace_batch_id, trace_code, seq_no, status,
  first_scan_time, scan_count, last_scan_time, last_print_time,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  trace_code_id, tenant_id, trace_batch_id, trace_code, seq_no, status,
  first_scan_time, scan_count, last_scan_time, last_print_time,
  create_by, create_time, update_by, update_time, remark
FROM `ym-sf`.`sf_trace_code`
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  trace_batch_id = VALUES(trace_batch_id),
  trace_code = VALUES(trace_code),
  seq_no = VALUES(seq_no),
  status = VALUES(status),
  first_scan_time = VALUES(first_scan_time),
  scan_count = VALUES(scan_count),
  last_scan_time = VALUES(last_scan_time),
  last_print_time = VALUES(last_print_time),
  create_by = VALUES(create_by),
  create_time = VALUES(create_time),
  update_by = VALUES(update_by),
  update_time = VALUES(update_time),
  remark = VALUES(remark);

INSERT INTO `ym-agriculture`.`sf_trace_scan_log` (
  scan_log_id, tenant_id, trace_code_id, trace_code, scan_time,
  scan_result, is_repeat, ip, user_agent, referer
)
SELECT
  scan_log_id, tenant_id, trace_code_id, trace_code, scan_time,
  scan_result, is_repeat, ip, user_agent, referer
FROM `ym-sf`.`sf_trace_scan_log`
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  trace_code_id = VALUES(trace_code_id),
  trace_code = VALUES(trace_code),
  scan_time = VALUES(scan_time),
  scan_result = VALUES(scan_result),
  is_repeat = VALUES(is_repeat),
  ip = VALUES(ip),
  user_agent = VALUES(user_agent),
  referer = VALUES(referer);
