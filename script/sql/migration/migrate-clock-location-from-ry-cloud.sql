-- 将旧系统参数中的租户打卡地点配置迁移至农业服务库。
-- 执行前请备份源库与目标库；脚本可重复执行，相同租户会更新为当前配置值。

INSERT INTO `ym-agriculture`.`sf_stask_clock_location` (
  clock_location_id, tenant_id, location_name, center_lng, center_lat,
  coordinate_type, radius_meters, enabled, map_provider, map_zoom_level,
  address_text, create_dept, create_by, create_time, update_by, update_time, remark
)
SELECT
  config_id,
  tenant_id,
  JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.locationName')),
  CAST(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.centerLng')) AS DECIMAL(10,7)),
  CAST(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.centerLat')) AS DECIMAL(10,7)),
  'CGCS2000',
  CAST(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.radiusMeters')) AS UNSIGNED),
  COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.enabled')), '1'),
  'tianditu',
  CAST(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.mapZoomLevel')) AS UNSIGNED),
  JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.addressText')),
  create_dept, create_by, create_time, update_by, update_time,
  JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.remark'))
FROM `ry-cloud`.`sys_config`
WHERE config_key = 'stask.leader.clock-location'
  AND JSON_VALID(config_value)
  AND JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.locationName')) IS NOT NULL
  AND JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.centerLng')) IS NOT NULL
  AND JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.centerLat')) IS NOT NULL
  AND CAST(JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.radiusMeters')) AS UNSIGNED) > 0
ON DUPLICATE KEY UPDATE
  location_name = VALUES(location_name),
  center_lng = VALUES(center_lng),
  center_lat = VALUES(center_lat),
  coordinate_type = VALUES(coordinate_type),
  radius_meters = VALUES(radius_meters),
  enabled = VALUES(enabled),
  map_provider = VALUES(map_provider),
  map_zoom_level = VALUES(map_zoom_level),
  address_text = VALUES(address_text),
  update_by = VALUES(update_by),
  update_time = VALUES(update_time),
  remark = VALUES(remark);
