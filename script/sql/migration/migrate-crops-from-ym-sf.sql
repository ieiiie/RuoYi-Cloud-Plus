-- 将旧单体库 ym-sf 的作物品类和品种迁移至农业服务库。
-- 先迁移品类再迁移品种；脚本可重复执行，同主键记录会更新。

INSERT INTO `ym-agriculture`.`sf_crop_species` (
  species_id, tenant_id, species_code, species_name, remote_sensing_code,
  status, del_flag, map_icon_url, map_icon_emoji, growth_stage_config_json,
  create_dept, create_by, create_time, update_by, update_time, remark
)
SELECT
  species_id, tenant_id, species_code, species_name, remote_sensing_code,
  status, del_flag, map_icon_url, map_icon_emoji, growth_stage_config_json,
  create_dept, create_by, create_time, update_by, update_time, remark
FROM `ym-sf`.`sf_crop_species`
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  species_code = VALUES(species_code),
  species_name = VALUES(species_name),
  remote_sensing_code = VALUES(remote_sensing_code),
  status = VALUES(status),
  del_flag = VALUES(del_flag),
  map_icon_url = VALUES(map_icon_url),
  map_icon_emoji = VALUES(map_icon_emoji),
  growth_stage_config_json = VALUES(growth_stage_config_json),
  create_dept = VALUES(create_dept),
  create_by = VALUES(create_by),
  create_time = VALUES(create_time),
  update_by = VALUES(update_by),
  update_time = VALUES(update_time),
  remark = VALUES(remark);

INSERT INTO `ym-agriculture`.`sf_crop_variety` (
  variety_id, tenant_id, species_id, variety_code, variety_name,
  growth_cycle_days, status, del_flag, map_icon_url, map_icon_emoji,
  create_dept, create_by, create_time, update_by, update_time, remark
)
SELECT
  variety_id, tenant_id, species_id, variety_code, variety_name,
  growth_cycle_days, status, del_flag, map_icon_url, map_icon_emoji,
  create_dept, create_by, create_time, update_by, update_time, remark
FROM `ym-sf`.`sf_crop_variety`
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  species_id = VALUES(species_id),
  variety_code = VALUES(variety_code),
  variety_name = VALUES(variety_name),
  growth_cycle_days = VALUES(growth_cycle_days),
  status = VALUES(status),
  del_flag = VALUES(del_flag),
  map_icon_url = VALUES(map_icon_url),
  map_icon_emoji = VALUES(map_icon_emoji),
  create_dept = VALUES(create_dept),
  create_by = VALUES(create_by),
  create_time = VALUES(create_time),
  update_by = VALUES(update_by),
  update_time = VALUES(update_time),
  remark = VALUES(remark);
