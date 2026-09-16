-- 将旧单体库 ym-sf 的大棚二维布局迁移至农业服务库。
-- 须在地块迁移后执行；顺序为布局根记录、布局列、棚位。

INSERT INTO `ym-agriculture`.`sf_greenhouse_layout` (
  layout_id, tenant_id, version, create_dept, create_by, create_time,
  update_by, update_time, remark
)
SELECT
  layout_id, tenant_id, version, create_dept, create_by, create_time,
  update_by, update_time, remark
FROM `ym-sf`.`sf_greenhouse_layout`
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  version = VALUES(version),
  create_dept = VALUES(create_dept),
  create_by = VALUES(create_by),
  create_time = VALUES(create_time),
  update_by = VALUES(update_by),
  update_time = VALUES(update_time),
  remark = VALUES(remark);

INSERT INTO `ym-agriculture`.`sf_greenhouse_layout_column` (
  column_id, tenant_id, layout_id, column_name, column_order,
  create_dept, create_by, create_time, update_by, update_time, remark
)
SELECT
  column_id, tenant_id, layout_id, column_name, column_order,
  create_dept, create_by, create_time, update_by, update_time, remark
FROM `ym-sf`.`sf_greenhouse_layout_column`
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  layout_id = VALUES(layout_id),
  column_name = VALUES(column_name),
  column_order = VALUES(column_order),
  create_dept = VALUES(create_dept),
  create_by = VALUES(create_by),
  create_time = VALUES(create_time),
  update_by = VALUES(update_by),
  update_time = VALUES(update_time),
  remark = VALUES(remark);

INSERT INTO `ym-agriculture`.`sf_greenhouse_layout_item` (
  item_id, tenant_id, layout_id, column_id, field_id, row_order,
  create_dept, create_by, create_time, update_by, update_time, remark
)
SELECT
  item_id, tenant_id, layout_id, column_id, field_id, row_order,
  create_dept, create_by, create_time, update_by, update_time, remark
FROM `ym-sf`.`sf_greenhouse_layout_item`
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  layout_id = VALUES(layout_id),
  column_id = VALUES(column_id),
  field_id = VALUES(field_id),
  row_order = VALUES(row_order),
  create_dept = VALUES(create_dept),
  create_by = VALUES(create_by),
  create_time = VALUES(create_time),
  update_by = VALUES(update_by),
  update_time = VALUES(update_time),
  remark = VALUES(remark);
