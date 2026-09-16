-- 租户字典默认值模板：仅初始化新租户，不补发存量租户（MySQL）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_tenant_dict_default_data (
  dict_code bigint NOT NULL COMMENT '默认值主键',
  dict_sort int NOT NULL DEFAULT 0 COMMENT '字典排序',
  dict_label varchar(100) NOT NULL COMMENT '字典标签',
  dict_value varchar(100) NOT NULL COMMENT '稳定业务编码，创建后不可修改',
  dict_type varchar(100) NOT NULL COMMENT '字典类型，创建后不可修改',
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
  UNIQUE KEY uk_sys_tenant_dict_default_value (dict_type, dict_value),
  UNIQUE KEY uk_sys_tenant_dict_default_label (dict_type, dict_label),
  KEY idx_sys_tenant_dict_default_list (dict_type, dict_sort, dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='新租户字典默认值模板';
