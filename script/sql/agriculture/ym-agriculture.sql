-- YM 农业业务服务 MySQL 初始化脚本。
-- 业务表按迁移切片逐步纳入；本版本已包含节气与农历日期能力。

CREATE DATABASE IF NOT EXISTS `ym-agriculture`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `ym-agriculture`;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sf_solar_term_definition (
  term_code VARCHAR(32) NOT NULL COMMENT '节气编码，例如 lichun',
  term_name VARCHAR(16) NOT NULL COMMENT '节气名称',
  term_order INT NOT NULL COMMENT '24 节气顺序，立春=1',
  solar_longitude INT DEFAULT NULL COMMENT '太阳黄经（度）',
  intro VARCHAR(1000) NOT NULL COMMENT '节气简介',
  seasonal_description VARCHAR(1000) NOT NULL COMMENT '时令特点',
  customs_json JSON DEFAULT NULL COMMENT '传统习俗数组 JSON',
  content_version VARCHAR(32) NOT NULL DEFAULT 'v1' COMMENT '内容版本',
  enabled_flag TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (term_code),
  UNIQUE KEY uk_sf_solar_term_order (term_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二十四节气定义内容';

CREATE TABLE IF NOT EXISTS sf_solar_term_occurrence (
  occurrence_id BIGINT NOT NULL COMMENT '主键',
  term_year INT NOT NULL COMMENT '公历年份（按交节所在公历年）',
  term_code VARCHAR(32) NOT NULL COMMENT '节气编码',
  term_name VARCHAR(16) NOT NULL COMMENT '节气名称',
  occurred_at DATETIME NOT NULL COMMENT '北京时间交节时间',
  gregorian_date DATE NOT NULL COMMENT '公历日期',
  lunar_date_text VARCHAR(64) DEFAULT NULL COMMENT '农历日期文本',
  algorithm_version VARCHAR(32) NOT NULL DEFAULT 'lunar-1.7.7' COMMENT '算法版本',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (occurrence_id),
  UNIQUE KEY uk_sf_solar_term_year_code (term_year, term_code),
  KEY idx_sf_solar_term_occurred (occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='具体年份节气交节时间';

CREATE TABLE IF NOT EXISTS sf_field (
  field_id BIGINT NOT NULL COMMENT '地块主键',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  owner_user_id BIGINT DEFAULT NULL COMMENT '业务负责人用户ID',
  field_code VARCHAR(64) DEFAULT NULL COMMENT '地块编码，租户内唯一',
  field_name VARCHAR(200) NOT NULL COMMENT '地块名称',
  greenhouse_short_name VARCHAR(32) DEFAULT NULL COMMENT '大棚简称',
  greenhouse_color CHAR(7) CHARACTER SET ascii DEFAULT NULL COMMENT '大棚强调色#RRGGBB',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
  field_type VARCHAR(32) NOT NULL DEFAULT 'FIELD' COMMENT '地块类型：FIELD/GREENHOUSE',
  boundary_geojson LONGTEXT DEFAULT NULL COMMENT '地块边界GeoJSON',
  center_lng DECIMAL(10,7) DEFAULT NULL COMMENT '中心点经度',
  center_lat DECIMAL(10,7) DEFAULT NULL COMMENT '中心点纬度',
  area_mu DECIMAL(12,4) DEFAULT NULL COMMENT '面积（亩）',
  address_text VARCHAR(500) DEFAULT NULL COMMENT '地址描述',
  admin_division_text VARCHAR(500) DEFAULT NULL COMMENT '行政区划路径文本',
  admin_division_adcode VARCHAR(32) DEFAULT NULL COMMENT '行政区划代码',
  map_zoom_level INT DEFAULT NULL COMMENT '地图缩放级别',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '档案状态：0正常 1停用',
  field_status VARCHAR(32) NOT NULL DEFAULT 'IDLE' COMMENT '业务状态：IDLE/IN_USE/MAINTENANCE',
  map_provider VARCHAR(32) DEFAULT NULL COMMENT '地图服务商',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  create_dept BIGINT DEFAULT NULL COMMENT '创建部门',
  create_by BIGINT DEFAULT NULL COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (field_id),
  UNIQUE KEY uk_sf_field_tenant_code (tenant_id, field_code),
  KEY idx_sf_field_tenant (tenant_id),
  KEY idx_sf_field_status (tenant_id, status, del_flag),
  KEY idx_sf_field_business_status (tenant_id, field_status, del_flag),
  KEY idx_sf_field_owner (tenant_id, owner_user_id, del_flag),
  KEY idx_sf_field_admin_adcode (tenant_id, admin_division_adcode, del_flag),
  KEY idx_sf_field_type (tenant_id, field_type, del_flag),
  KEY idx_sf_field_name (tenant_id, field_name, del_flag),
  KEY idx_sf_field_sort (tenant_id, sort_order, field_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地块档案';

CREATE TABLE IF NOT EXISTS sf_field_iot (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  field_id BIGINT NOT NULL COMMENT '地块ID',
  device_sn VARCHAR(128) NOT NULL COMMENT '物联网设备编码',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  create_dept BIGINT DEFAULT NULL COMMENT '创建部门',
  create_by BIGINT DEFAULT NULL COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sf_field_iot_device (tenant_id, device_sn),
  KEY idx_sf_field_iot_device (tenant_id, device_sn, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地块与物联网设备关联';

CREATE TABLE IF NOT EXISTS sf_greenhouse_layout (
  layout_id BIGINT NOT NULL COMMENT '布局主键',
  tenant_id VARCHAR(20) NOT NULL COMMENT '租户编号',
  version BIGINT NOT NULL DEFAULT 0 COMMENT '布局乐观锁版本',
  create_dept BIGINT DEFAULT NULL COMMENT '创建部门',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (layout_id),
  UNIQUE KEY uk_sf_greenhouse_layout_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户大棚二维布局';

CREATE TABLE IF NOT EXISTS sf_greenhouse_layout_column (
  column_id BIGINT NOT NULL COMMENT '布局列主键',
  tenant_id VARCHAR(20) NOT NULL COMMENT '租户编号',
  layout_id BIGINT NOT NULL COMMENT '布局主键',
  column_name VARCHAR(32) NOT NULL COMMENT '列名称',
  column_order INT NOT NULL COMMENT '列顺序，从1开始',
  create_dept BIGINT DEFAULT NULL COMMENT '创建部门',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (column_id),
  UNIQUE KEY uk_sf_greenhouse_layout_column_order (layout_id, column_order),
  UNIQUE KEY uk_sf_greenhouse_layout_column_name (layout_id, column_name),
  KEY idx_sf_greenhouse_layout_column_tenant (tenant_id, layout_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大棚布局列';

CREATE TABLE IF NOT EXISTS sf_greenhouse_layout_item (
  item_id BIGINT NOT NULL COMMENT '棚位主键',
  tenant_id VARCHAR(20) NOT NULL COMMENT '租户编号',
  layout_id BIGINT NOT NULL COMMENT '布局主键',
  column_id BIGINT NOT NULL COMMENT '布局列主键',
  field_id BIGINT NOT NULL COMMENT '大棚地块主键',
  row_order INT NOT NULL COMMENT '列内行顺序，从1开始',
  create_dept BIGINT DEFAULT NULL COMMENT '创建部门',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (item_id),
  UNIQUE KEY uk_sf_greenhouse_layout_item_field (layout_id, field_id),
  UNIQUE KEY uk_sf_greenhouse_layout_item_position (column_id, row_order),
  KEY idx_sf_greenhouse_layout_item_tenant (tenant_id, layout_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大棚布局棚位';

CREATE TABLE IF NOT EXISTS sf_crop_species (
  species_id BIGINT NOT NULL COMMENT '品类ID',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  species_code VARCHAR(64) NOT NULL COMMENT '品类编码，租户内唯一',
  species_name VARCHAR(100) NOT NULL COMMENT '品类名称',
  remote_sensing_code INT DEFAULT NULL COMMENT '遥感作物编码',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  map_icon_url VARCHAR(512) DEFAULT NULL COMMENT '地图图标URL',
  map_icon_emoji VARCHAR(16) DEFAULT NULL COMMENT '地图Emoji',
  growth_stage_config_json JSON DEFAULT NULL COMMENT '生长阶段配置',
  create_dept BIGINT DEFAULT NULL COMMENT '创建部门',
  create_by BIGINT DEFAULT NULL COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (species_id),
  UNIQUE KEY uk_sf_crop_species_tenant_code (tenant_id, species_code),
  KEY idx_sf_crop_species_tenant_status (tenant_id, status, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作物品类';

CREATE TABLE IF NOT EXISTS sf_crop_variety (
  variety_id BIGINT NOT NULL COMMENT '品种ID',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  species_id BIGINT NOT NULL COMMENT '品类ID',
  variety_code VARCHAR(64) NOT NULL COMMENT '品种编码，租户内唯一',
  variety_name VARCHAR(100) NOT NULL COMMENT '品种名称',
  growth_cycle_days INT DEFAULT NULL COMMENT '生长周期（天）',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  map_icon_url VARCHAR(512) DEFAULT NULL COMMENT '地图图标URL',
  map_icon_emoji VARCHAR(16) DEFAULT NULL COMMENT '地图Emoji',
  create_dept BIGINT DEFAULT NULL COMMENT '创建部门',
  create_by BIGINT DEFAULT NULL COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (variety_id),
  UNIQUE KEY uk_sf_crop_variety_tenant_code (tenant_id, variety_code),
  KEY idx_sf_crop_variety_species (tenant_id, species_id, del_flag),
  KEY idx_sf_crop_variety_tenant_status (tenant_id, status, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作物品种';

CREATE TABLE IF NOT EXISTS sf_planting_batch (
  batch_id BIGINT NOT NULL COMMENT '种植批次ID',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  field_id BIGINT NOT NULL COMMENT '地块ID',
  variety_id BIGINT NOT NULL COMMENT '品种ID',
  batch_code VARCHAR(64) NOT NULL COMMENT '批次编号',
  cropping_index INT NOT NULL DEFAULT 1 COMMENT '第几茬',
  sowing_date DATETIME NOT NULL COMMENT '播种日期',
  expected_harvest_date DATETIME DEFAULT NULL COMMENT '预计采收日期',
  actual_harvest_date DATETIME DEFAULT NULL COMMENT '实际采收日期',
  batch_status VARCHAR(32) NOT NULL COMMENT '批次状态',
  status_time DATETIME DEFAULT NULL COMMENT '状态变更时间',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  create_dept BIGINT DEFAULT NULL COMMENT '创建部门',
  create_by BIGINT DEFAULT NULL COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (batch_id),
  UNIQUE KEY uk_sf_planting_batch_tenant_code (tenant_id, batch_code),
  KEY idx_sf_planting_batch_field (tenant_id, field_id, del_flag),
  KEY idx_sf_planting_batch_variety (tenant_id, variety_id, del_flag),
  KEY idx_sf_planting_batch_status (tenant_id, batch_status, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='种植批次';

CREATE TABLE IF NOT EXISTS sf_planting_batch_log (
  log_id BIGINT NOT NULL COMMENT '流水ID',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  batch_id BIGINT NOT NULL COMMENT '种植批次ID',
  from_status VARCHAR(32) DEFAULT NULL COMMENT '原状态',
  to_status VARCHAR(32) NOT NULL COMMENT '目标状态',
  operate_by BIGINT DEFAULT NULL COMMENT '操作人',
  operate_time DATETIME NOT NULL COMMENT '操作时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (log_id),
  KEY idx_sf_planting_batch_log_batch (tenant_id, batch_id),
  KEY idx_sf_planting_batch_log_time (tenant_id, operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='种植批次状态流水';

CREATE TABLE IF NOT EXISTS sf_farm_work_dict (
  dict_id BIGINT NOT NULL COMMENT '农事字典主键',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父节点ID：0为分类，非0为项目所属分类',
  node_type VARCHAR(16) NOT NULL COMMENT '节点类型：CATEGORY分类 ITEM项目',
  dict_name VARCHAR(50) NOT NULL COMMENT '农事名称',
  dict_code VARCHAR(20) NOT NULL COMMENT '农事编码，租户内唯一',
  min_workers INT DEFAULT NULL COMMENT '最少工人数',
  max_workers INT DEFAULT NULL COMMENT '最多工人数',
  requires_material TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要领料',
  status CHAR(1) DEFAULT NULL COMMENT '状态：0启用 1停用',
  custom_form_template_json JSON DEFAULT NULL COMMENT '自定义表单模板',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '同级排序',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  create_dept BIGINT DEFAULT NULL COMMENT '创建部门',
  create_by BIGINT DEFAULT NULL COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (dict_id),
  KEY idx_sf_farm_work_code (tenant_id, dict_code, del_flag),
  KEY idx_sf_farm_work_parent_sort (tenant_id, parent_id, sort_order, dict_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农事分类与项目字典';

CREATE TABLE IF NOT EXISTS sf_stask_yield_record (
  yield_id BIGINT NOT NULL COMMENT '产量记录主键',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  harvest_date DATE NOT NULL COMMENT '收获日期',
  species_id BIGINT NOT NULL COMMENT '物种主键',
  variety_id BIGINT NOT NULL COMMENT '品种主键',
  species_name_snapshot VARCHAR(200) NOT NULL COMMENT '物种名称快照',
  variety_name_snapshot VARCHAR(200) NOT NULL COMMENT '品种名称快照',
  yield_kg DECIMAL(14,2) NOT NULL COMMENT '产量，固定单位公斤',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  create_dept BIGINT DEFAULT NULL COMMENT '创建部门',
  create_by BIGINT DEFAULT NULL COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (yield_id),
  KEY idx_sf_stask_yield_page (tenant_id, del_flag, harvest_date, variety_name_snapshot, yield_id),
  KEY idx_sf_stask_yield_species (tenant_id, del_flag, species_id, harvest_date),
  KEY idx_sf_stask_yield_variety (tenant_id, del_flag, variety_id, harvest_date),
  CONSTRAINT chk_sf_stask_yield_positive CHECK (yield_kg > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农事产量记录';

CREATE TABLE IF NOT EXISTS sf_stask_clock_location (
  clock_location_id BIGINT NOT NULL COMMENT '打卡地点主键',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  location_name VARCHAR(200) NOT NULL COMMENT '地点名称',
  center_lng DECIMAL(10,7) NOT NULL COMMENT '中心点经度，CGCS2000',
  center_lat DECIMAL(10,7) NOT NULL COMMENT '中心点纬度，CGCS2000',
  coordinate_type VARCHAR(32) NOT NULL DEFAULT 'CGCS2000' COMMENT '坐标系类型',
  radius_meters INT NOT NULL COMMENT '允许打卡半径，米',
  enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT '围栏状态：0关闭 1开启',
  map_provider VARCHAR(32) NOT NULL DEFAULT 'tianditu' COMMENT '地图服务商',
  map_zoom_level INT DEFAULT NULL COMMENT '地图缩放级别',
  address_text VARCHAR(500) DEFAULT NULL COMMENT '地址描述',
  create_dept BIGINT DEFAULT NULL COMMENT '创建部门',
  create_by BIGINT DEFAULT NULL COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (clock_location_id),
  UNIQUE KEY uk_sf_stask_clock_location_tenant (tenant_id),
  CONSTRAINT chk_sf_stask_clock_location_radius CHECK (radius_meters > 0),
  CONSTRAINT chk_sf_stask_clock_location_enabled CHECK (enabled IN ('0', '1'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农事任务打卡地点';

CREATE TABLE IF NOT EXISTS sf_trace_batch (
  trace_batch_id BIGINT NOT NULL COMMENT '溯源批次ID',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  planting_batch_id BIGINT DEFAULT NULL COMMENT '关联种植批次ID',
  field_id BIGINT NOT NULL COMMENT '地块ID',
  variety_id BIGINT NOT NULL COMMENT '品种ID',
  trace_batch_no VARCHAR(64) NOT NULL COMMENT '溯源批次号，租户内唯一',
  product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
  quality_grade VARCHAR(50) DEFAULT NULL COMMENT '品质等级',
  origin_text VARCHAR(255) NOT NULL COMMENT '产地展示文本',
  producer_name VARCHAR(200) NOT NULL COMMENT '生产单位',
  certification_json LONGTEXT DEFAULT NULL COMMENT '认证资料JSON',
  label_scope VARCHAR(32) NOT NULL DEFAULT 'SINGLE_FRUIT' COMMENT '标签对象：SINGLE_FRUIT单果 BOX整箱',
  planned_quantity INT NOT NULL DEFAULT 0 COMMENT '计划生成标签数量',
  generated_quantity INT NOT NULL DEFAULT 0 COMMENT '已生成标签数量',
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT草稿 GENERATED已生成 PUBLISHED已发布 DISABLED已停用',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  create_by BIGINT DEFAULT NULL COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (trace_batch_id),
  UNIQUE KEY uk_sf_trace_batch_tenant_no (tenant_id, trace_batch_no),
  KEY idx_sf_trace_batch_tenant (tenant_id),
  KEY idx_sf_trace_batch_planting (planting_batch_id),
  KEY idx_sf_trace_batch_field (field_id),
  KEY idx_sf_trace_batch_variety (variety_id),
  KEY idx_sf_trace_batch_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='瓜果溯源批次';

CREATE TABLE IF NOT EXISTS sf_trace_code (
  trace_code_id BIGINT NOT NULL COMMENT '溯源码ID',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  trace_batch_id BIGINT NOT NULL COMMENT '溯源批次ID',
  trace_code VARCHAR(64) NOT NULL COMMENT '全局唯一溯源码',
  seq_no INT NOT NULL COMMENT '批次内序号',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL正常 VOID作废',
  first_scan_time DATETIME DEFAULT NULL COMMENT '首次扫码时间',
  scan_count INT NOT NULL DEFAULT 0 COMMENT '扫码次数',
  last_scan_time DATETIME DEFAULT NULL COMMENT '最近扫码时间',
  last_print_time DATETIME DEFAULT NULL COMMENT '最近打印时间',
  create_by BIGINT DEFAULT NULL COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (trace_code_id),
  UNIQUE KEY uk_sf_trace_code (trace_code),
  UNIQUE KEY uk_sf_trace_code_batch_seq (trace_batch_id, seq_no),
  KEY idx_sf_trace_code_tenant (tenant_id),
  KEY idx_sf_trace_code_batch (trace_batch_id),
  KEY idx_sf_trace_code_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='瓜果溯源码';

CREATE TABLE IF NOT EXISTS sf_trace_scan_log (
  scan_log_id BIGINT NOT NULL COMMENT '扫码日志ID',
  tenant_id VARCHAR(20) DEFAULT NULL COMMENT '租户编号，未找到码时可为空',
  trace_code_id BIGINT DEFAULT NULL COMMENT '溯源码ID，未找到码时可为空',
  trace_code VARCHAR(64) NOT NULL COMMENT '溯源码快照',
  scan_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '扫码时间',
  scan_result VARCHAR(32) NOT NULL COMMENT '扫码结果：OK正常 VOID作废 NOT_FOUND未找到 DISABLED批次停用',
  is_repeat TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否重复扫码',
  ip VARCHAR(64) DEFAULT NULL COMMENT '扫码端IP',
  user_agent VARCHAR(500) DEFAULT NULL COMMENT '扫码端User-Agent',
  referer VARCHAR(500) DEFAULT NULL COMMENT '来源页面',
  PRIMARY KEY (scan_log_id),
  KEY idx_sf_trace_scan_code (trace_code_id, scan_time),
  KEY idx_sf_trace_scan_tenant (tenant_id),
  KEY idx_sf_trace_scan_time (scan_time),
  KEY idx_sf_trace_scan_trace_code (trace_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='瓜果溯源扫码日志';

CREATE TABLE IF NOT EXISTS sf_market_source (
  source_code VARCHAR(64) NOT NULL COMMENT '分配给爬虫的来源编码',
  source_name VARCHAR(100) NOT NULL COMMENT '前台展示来源',
  allowed_domains_json JSON NOT NULL COMMENT '允许来源域名数组',
  authorization_note VARCHAR(500) DEFAULT NULL COMMENT '授权、robots或开放数据依据',
  enabled_flag TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许入库',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (source_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农业行情来源白名单';

CREATE TABLE IF NOT EXISTS sf_market_quote_ingest_staging (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '暂存主键',
  request_id VARCHAR(64) NOT NULL COMMENT '爬虫逻辑提交唯一ID',
  crawl_run_id VARCHAR(64) NOT NULL COMMENT '采集批次',
  source_code VARCHAR(64) NOT NULL COMMENT '来源编码',
  external_product_key VARCHAR(191) NOT NULL COMMENT '来源内稳定商品标识',
  product_name VARCHAR(128) NOT NULL COMMENT '来源商品名称',
  specification VARCHAR(128) DEFAULT NULL COMMENT '来源规格或等级',
  source_category VARCHAR(128) DEFAULT NULL COMMENT '来源原始品类',
  quote_type VARCHAR(32) NOT NULL COMMENT 'MARKET/OFFICIAL_AVERAGE',
  price DECIMAL(20,6) NOT NULL COMMENT '来源报价数值',
  unit VARCHAR(32) NOT NULL COMMENT '来源计价单位',
  origin VARCHAR(128) DEFAULT NULL COMMENT '来源明确给出的产地或地区',
  market_name VARCHAR(191) DEFAULT NULL COMMENT '报价市场名称',
  quote_date DATE NOT NULL COMMENT '行情业务日期',
  fetched_at_utc DATETIME(3) NOT NULL COMMENT 'UTC采集时间',
  source_url VARCHAR(2048) NOT NULL COMMENT '来源页面或授权接口地址',
  raw_price_text VARCHAR(512) NOT NULL COMMENT '报价对应原始文本',
  raw_payload_json JSON NOT NULL COMMENT '原始结构化载荷',
  payload_sha256 CHAR(64) NOT NULL COMMENT '载荷SHA-256',
  ingest_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/ACCEPTED/UNCHANGED/REJECTED/FAILED/IGNORED',
  result_action VARCHAR(32) DEFAULT NULL COMMENT 'CREATED/UPDATED/UNCHANGED/IGNORED',
  process_attempts INT NOT NULL DEFAULT 0 COMMENT '处理次数',
  warnings_json JSON DEFAULT NULL COMMENT '处理告警数组',
  correction_json JSON DEFAULT NULL COMMENT '运营修正字段，不覆盖原始值',
  error_code VARCHAR(64) DEFAULT NULL COMMENT '错误码',
  error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误说明',
  product_id BIGINT DEFAULT NULL COMMENT '归一后的平台商品ID',
  quote_id BIGINT DEFAULT NULL COMMENT '正式报价ID',
  handled_by BIGINT DEFAULT NULL COMMENT '最近处理人ID',
  handler_name VARCHAR(100) DEFAULT NULL COMMENT '最近处理人名称快照',
  handle_comment VARCHAR(500) DEFAULT NULL COMMENT '处理说明',
  handled_at DATETIME DEFAULT NULL COMMENT '人工处理时间',
  warning_acknowledged TINYINT(1) NOT NULL DEFAULT 0 COMMENT '告警是否已确认',
  received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '入库时间',
  processing_started_at DATETIME(3) DEFAULT NULL COMMENT '处理开始时间',
  processed_at DATETIME(3) DEFAULT NULL COMMENT '处理完成时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sf_market_ingest_request (source_code, request_id),
  KEY idx_sf_market_ingest_status (ingest_status, received_at),
  KEY idx_sf_market_ingest_run (crawl_run_id, source_code),
  KEY idx_sf_market_ingest_business (source_code, external_product_key, quote_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农业行情采集暂存表';

CREATE TABLE IF NOT EXISTS sf_market_product (
  product_id BIGINT NOT NULL COMMENT '商品主键',
  product_name VARCHAR(128) NOT NULL COMMENT '平台商品名称',
  normalized_name VARCHAR(128) NOT NULL COMMENT '用于自动匹配的标准化名称',
  category VARCHAR(32) NOT NULL COMMENT 'grain/vegetable/fruit/livestock/aquatic',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '展示排序，升序',
  enabled_flag TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (product_id),
  UNIQUE KEY uk_sf_market_product_name (category, normalized_name),
  KEY idx_sf_market_product_sort (category, sort_order, product_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农业行情平台商品';

CREATE TABLE IF NOT EXISTS sf_market_product_mapping (
  mapping_id BIGINT NOT NULL COMMENT '来源商品映射主键',
  source_code VARCHAR(64) NOT NULL COMMENT '来源编码',
  external_product_key VARCHAR(191) NOT NULL COMMENT '来源内稳定商品标识',
  product_id BIGINT NOT NULL COMMENT '平台商品ID',
  last_source_name VARCHAR(128) NOT NULL COMMENT '最近来源商品名称',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (mapping_id),
  UNIQUE KEY uk_sf_market_product_mapping (source_code, external_product_key),
  KEY idx_sf_market_product_mapping_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农业行情来源商品映射';

CREATE TABLE IF NOT EXISTS sf_market_quote (
  quote_id BIGINT NOT NULL COMMENT '正式报价主键',
  latest_ingest_id BIGINT NOT NULL COMMENT '最近处理的暂存记录ID',
  product_id BIGINT NOT NULL COMMENT '平台商品ID',
  source_code VARCHAR(64) NOT NULL COMMENT '来源编码',
  external_product_key VARCHAR(191) NOT NULL COMMENT '来源内商品标识',
  series_key_sha256 CHAR(64) NOT NULL COMMENT '价格序列SHA-256',
  specification VARCHAR(128) DEFAULT NULL COMMENT '规格或等级',
  quote_type VARCHAR(32) NOT NULL COMMENT 'MARKET/OFFICIAL_AVERAGE',
  source_price DECIMAL(20,6) NOT NULL COMMENT '来源原始价格',
  source_unit VARCHAR(32) NOT NULL COMMENT '来源原始单位',
  price DECIMAL(20,6) NOT NULL COMMENT '平台标准价格',
  unit VARCHAR(32) NOT NULL COMMENT '平台标准单位',
  origin VARCHAR(128) DEFAULT NULL COMMENT '产地或地区',
  market_name VARCHAR(191) DEFAULT NULL COMMENT '报价市场',
  quote_date DATE NOT NULL COMMENT '行情业务日期',
  collected_at DATETIME(3) DEFAULT NULL COMMENT '采集时间',
  source_url VARCHAR(2048) NOT NULL COMMENT '来源地址',
  previous_price DECIMAL(20,6) DEFAULT NULL COMMENT '上一有效报价',
  change_amount DECIMAL(20,6) DEFAULT NULL COMMENT '涨跌额',
  change_percent DECIMAL(12,4) DEFAULT NULL COMMENT '涨跌幅，0.7表示0.7%',
  trend VARCHAR(16) NOT NULL DEFAULT 'unknown' COMMENT 'up/down/flat/unknown',
  warnings_json JSON DEFAULT NULL COMMENT '报价告警数组',
  publish_status VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/OFFLINE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (quote_id),
  UNIQUE KEY uk_sf_market_quote_series_date (series_key_sha256, quote_date),
  KEY idx_sf_market_quote_latest (publish_status, series_key_sha256, quote_date),
  KEY idx_sf_market_quote_product (product_id, quote_date),
  KEY idx_sf_market_quote_source (source_code, quote_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农业行情正式报价';

INSERT INTO sf_market_source (source_code, source_name, allowed_domains_json, authorization_note, enabled_flag)
SELECT 'moa_pfsc', '全国农产品批发市场价格信息系统', JSON_ARRAY('pfsc.agri.cn'), '正式采集前需确认开放数据或接口授权范围', 1
WHERE NOT EXISTS (SELECT 1 FROM sf_market_source WHERE source_code = 'moa_pfsc');

INSERT INTO sf_market_source (source_code, source_name, allowed_domains_json, authorization_note, enabled_flag)
SELECT 'moa_ncpsc', '重点农产品市场信息平台', JSON_ARRAY('ncpscxx.moa.gov.cn'), '正式采集前需确认开放数据或接口授权范围', 1
WHERE NOT EXISTS (SELECT 1 FROM sf_market_source WHERE source_code = 'moa_ncpsc');

INSERT INTO sf_market_source (source_code, source_name, allowed_domains_json, authorization_note, enabled_flag)
SELECT 'mofcom_cif', '商务预报', JSON_ARRAY('cif.mofcom.gov.cn'), '正式采集前需确认开放数据或接口授权范围', 1
WHERE NOT EXISTS (SELECT 1 FROM sf_market_source WHERE source_code = 'mofcom_cif');

CREATE OR REPLACE SQL SECURITY DEFINER VIEW v_sf_market_quote_ingest_receipt AS
SELECT request_id, crawl_run_id, source_code, payload_sha256, ingest_status, result_action,
       product_id, quote_id, warnings_json, error_code, error_message, received_at, processed_at
FROM sf_market_quote_ingest_staging;

CREATE TABLE IF NOT EXISTS domain_command_idempotency (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id VARCHAR(20) NOT NULL COMMENT '租户编号',
  action_name VARCHAR(128) NOT NULL COMMENT '领域动作',
  business_id VARCHAR(128) NOT NULL COMMENT '稳定业务号',
  request_id VARCHAR(128) NOT NULL COMMENT '客户端幂等请求号',
  status VARCHAR(16) NOT NULL COMMENT 'RUNNING/SUCCEEDED/FAILED',
  result_value INT DEFAULT NULL COMMENT '成功结果值',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '失败后重试次数',
  started_at DATETIME NOT NULL,
  finished_at DATETIME DEFAULT NULL,
  failure_reason VARCHAR(1000) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_domain_command_request (tenant_id, action_name, request_id),
  KEY idx_domain_command_business (tenant_id, action_name, business_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨服务领域命令幂等记录';
