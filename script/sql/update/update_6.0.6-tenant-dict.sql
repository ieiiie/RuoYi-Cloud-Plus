-- 租户字典：Dbo 定义类型，SaaS 租户独立维护字典值（MySQL）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_tenant_dict_type (
  dict_id bigint NOT NULL COMMENT '字典主键',
  dict_name varchar(100) NOT NULL COMMENT '字典名称',
  dict_type varchar(100) NOT NULL COMMENT '字典类型',
  create_dept bigint DEFAULT NULL COMMENT '创建部门',
  create_by bigint DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by bigint DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (dict_id),
  UNIQUE KEY uk_sys_tenant_dict_type_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Dbo定义的租户字典类型';

CREATE TABLE IF NOT EXISTS sys_tenant_dict_data (
  dict_code bigint NOT NULL COMMENT '字典编码主键',
  tenant_id varchar(20) NOT NULL COMMENT '租户编号',
  dict_sort int NOT NULL DEFAULT 0 COMMENT '字典排序',
  dict_label varchar(100) NOT NULL COMMENT '字典标签',
  dict_value varchar(100) NOT NULL COMMENT '系统生成的不可变业务编码',
  dict_type varchar(100) NOT NULL COMMENT '字典类型',
  css_class varchar(100) DEFAULT NULL COMMENT '样式属性',
  list_class varchar(100) DEFAULT NULL COMMENT '列表样式',
  is_default char(1) NOT NULL DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  create_dept bigint DEFAULT NULL COMMENT '创建部门',
  create_by bigint DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by bigint DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (dict_code),
  UNIQUE KEY uk_sys_tenant_dict_value (tenant_id, dict_type, dict_value),
  UNIQUE KEY uk_sys_tenant_dict_label (tenant_id, dict_type, dict_label),
  KEY idx_sys_tenant_dict_list (tenant_id, dict_type, dict_sort, dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户独立字典值';

INSERT IGNORE INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
   menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time,
   update_by, update_time, remark)
VALUES
  (1761400000000011800, '租户字典', 1761400000000000001, 12, 'tenant-dict', 'system/tenant-dict/index', '', 'N', 'Y', 'C', '0', '0', 'system:tenantDict:list', 'ant-design:unordered-list-outlined', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '维护当前租户字典值'),
  (1761400000000011801, '租户字典查询', 1761400000000011800, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantDict:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
  (1761400000000011802, '租户字典新增', 1761400000000011800, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantDict:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
  (1761400000000011803, '租户字典修改', 1761400000000011800, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantDict:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
  (1761400000000011804, '租户字典删除', 1761400000000011800, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantDict:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');

-- 租户套餐默认获得租户字典；已有系统管理权限的角色同步获得菜单和按钮。
INSERT IGNORE INTO sys_tenant_package_menu(package_id, menu_id)
SELECT p.package_id, m.menu_id
FROM sys_tenant_package p
CROSS JOIN sys_menu m
WHERE m.menu_id BETWEEN 1761400000000011800 AND 1761400000000011804;

INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT rm.role_id, m.menu_id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE rm.menu_id = 1761400000000000001
  AND m.menu_id BETWEEN 1761400000000011800 AND 1761400000000011804;
