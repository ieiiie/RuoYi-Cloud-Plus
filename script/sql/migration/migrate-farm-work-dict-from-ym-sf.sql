-- 将旧单体库 ym-sf 的农事分类与项目迁移至农业服务库。
-- 脚本可重复执行；相同主键按源库当前值更新。

INSERT INTO `ym-agriculture`.`sf_farm_work_dict` (
  dict_id, tenant_id, parent_id, node_type, dict_name, dict_code,
  min_workers, max_workers, requires_material, status,
  custom_form_template_json, sort_order, del_flag,
  create_dept, create_by, create_time, update_by, update_time, remark
)
SELECT
  dict_id, tenant_id, parent_id, node_type, dict_name, dict_code,
  min_workers, max_workers, requires_material, status,
  custom_form_template_json, sort_order, del_flag,
  create_dept, create_by, create_time, update_by, update_time, remark
FROM `ym-sf`.`sf_farm_work_dict`
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  parent_id = VALUES(parent_id),
  node_type = VALUES(node_type),
  dict_name = VALUES(dict_name),
  dict_code = VALUES(dict_code),
  min_workers = VALUES(min_workers),
  max_workers = VALUES(max_workers),
  requires_material = VALUES(requires_material),
  status = VALUES(status),
  custom_form_template_json = VALUES(custom_form_template_json),
  sort_order = VALUES(sort_order),
  del_flag = VALUES(del_flag),
  create_dept = VALUES(create_dept),
  create_by = VALUES(create_by),
  create_time = VALUES(create_time),
  update_by = VALUES(update_by),
  update_time = VALUES(update_time),
  remark = VALUES(remark);
