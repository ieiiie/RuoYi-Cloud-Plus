-- 将旧单体库 ym-sf 的产量记录迁移至农业服务库。
-- 执行前请备份源库与目标库；脚本可重复执行，相同主键会更新为源库当前值。

INSERT INTO `ym-agriculture`.`sf_stask_yield_record` (
  yield_id, tenant_id, harvest_date, species_id, variety_id,
  species_name_snapshot, variety_name_snapshot, yield_kg, del_flag,
  create_dept, create_by, create_time, update_by, update_time
)
SELECT
  yield_id, tenant_id, harvest_date, species_id, variety_id,
  species_name_snapshot, variety_name_snapshot, yield_kg, del_flag,
  create_dept, create_by, create_time, update_by, update_time
FROM `ym-sf`.`sf_stask_yield_record`
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  harvest_date = VALUES(harvest_date),
  species_id = VALUES(species_id),
  variety_id = VALUES(variety_id),
  species_name_snapshot = VALUES(species_name_snapshot),
  variety_name_snapshot = VALUES(variety_name_snapshot),
  yield_kg = VALUES(yield_kg),
  del_flag = VALUES(del_flag),
  create_dept = VALUES(create_dept),
  create_by = VALUES(create_by),
  create_time = VALUES(create_time),
  update_by = VALUES(update_by),
  update_time = VALUES(update_time);
