-- 将旧单体库 ym-sf 的地块档案迁移至农业服务库。
-- 执行前请备份源库与目标库；脚本可重复执行，目标主键相同的数据会更新为源库当前值。

INSERT INTO `ym-agriculture`.`sf_field` (
  field_id, tenant_id, owner_user_id, field_code, field_name,
  greenhouse_short_name, greenhouse_color, sort_order, field_type,
  boundary_geojson, center_lng, center_lat, area_mu, address_text,
  admin_division_text, admin_division_adcode, map_zoom_level, status,
  field_status, map_provider, del_flag, create_dept, create_by, create_time,
  update_by, update_time, remark
)
SELECT
  field_id, tenant_id, owner_user_id, field_code, field_name,
  greenhouse_short_name, greenhouse_color, sort_order, field_type,
  boundary_geojson, center_lng, center_lat, area_mu, address_text,
  admin_division_text, admin_division_adcode, map_zoom_level, status,
  field_status, map_provider, del_flag, create_dept, create_by, create_time,
  update_by, update_time, remark
FROM `ym-sf`.`sf_field`
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  owner_user_id = VALUES(owner_user_id),
  field_code = VALUES(field_code),
  field_name = VALUES(field_name),
  greenhouse_short_name = VALUES(greenhouse_short_name),
  greenhouse_color = VALUES(greenhouse_color),
  sort_order = VALUES(sort_order),
  field_type = VALUES(field_type),
  boundary_geojson = VALUES(boundary_geojson),
  center_lng = VALUES(center_lng),
  center_lat = VALUES(center_lat),
  area_mu = VALUES(area_mu),
  address_text = VALUES(address_text),
  admin_division_text = VALUES(admin_division_text),
  admin_division_adcode = VALUES(admin_division_adcode),
  map_zoom_level = VALUES(map_zoom_level),
  status = VALUES(status),
  field_status = VALUES(field_status),
  map_provider = VALUES(map_provider),
  del_flag = VALUES(del_flag),
  create_dept = VALUES(create_dept),
  create_by = VALUES(create_by),
  create_time = VALUES(create_time),
  update_by = VALUES(update_by),
  update_time = VALUES(update_time),
  remark = VALUES(remark);
