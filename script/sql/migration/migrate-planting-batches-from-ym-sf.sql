-- 将旧单体库 ym-sf 的种植批次及状态流水迁移至农业服务库。
-- 执行前请备份源库与目标库；脚本可重复执行，相同主键会更新为源库当前值。

INSERT INTO `ym-agriculture`.`sf_planting_batch` (
  batch_id, tenant_id, field_id, variety_id, batch_code, cropping_index,
  sowing_date, expected_harvest_date, actual_harvest_date, batch_status,
  status_time, del_flag, create_dept, create_by, create_time, update_by,
  update_time, remark
)
SELECT
  batch_id, tenant_id, field_id, variety_id, batch_code, cropping_index,
  sowing_date, expected_harvest_date, actual_harvest_date, batch_status,
  status_time, del_flag, create_dept, create_by, create_time, update_by,
  update_time, remark
FROM `ym-sf`.`sf_planting_batch`
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  field_id = VALUES(field_id),
  variety_id = VALUES(variety_id),
  batch_code = VALUES(batch_code),
  cropping_index = VALUES(cropping_index),
  sowing_date = VALUES(sowing_date),
  expected_harvest_date = VALUES(expected_harvest_date),
  actual_harvest_date = VALUES(actual_harvest_date),
  batch_status = VALUES(batch_status),
  status_time = VALUES(status_time),
  del_flag = VALUES(del_flag),
  create_dept = VALUES(create_dept),
  create_by = VALUES(create_by),
  create_time = VALUES(create_time),
  update_by = VALUES(update_by),
  update_time = VALUES(update_time),
  remark = VALUES(remark);

INSERT INTO `ym-agriculture`.`sf_planting_batch_log` (
  log_id, tenant_id, batch_id, from_status, to_status, operate_by,
  operate_time, remark
)
SELECT
  log_id, tenant_id, batch_id, from_status, to_status, operate_by,
  operate_time, remark
FROM `ym-sf`.`sf_planting_batch_log`
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  batch_id = VALUES(batch_id),
  from_status = VALUES(from_status),
  to_status = VALUES(to_status),
  operate_by = VALUES(operate_by),
  operate_time = VALUES(operate_time),
  remark = VALUES(remark);
