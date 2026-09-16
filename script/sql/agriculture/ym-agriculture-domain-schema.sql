
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_ai_customer_binding` (
  `binding_id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `customer_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中台客户号，人工在中台建客户后回填',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING=待配置客户号 ACTIVE=已关联',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`binding_id`) USING BTREE,
  UNIQUE KEY `uk_sf_ai_bind_tenant` (`tenant_id`) USING BTREE,
  UNIQUE KEY `uk_sf_ai_customer_no` (`customer_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='算法中台客户与租户绑定';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_ai_inference_log` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `task_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务号',
  `task_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务名称',
  `model_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模型号',
  `model_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模型名称',
  `customer_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中台客户号',
  `customer_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户名称',
  `algorithm_type_id` bigint DEFAULT NULL COMMENT '算法类型 id',
  `algorithm_type_value` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '算法类型名称',
  `cls_score` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中台推送的分类得分原始值（如 {''Car'': 0.78}）',
  `cls_score_label` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '解析后的分类标签',
  `cls_score_value` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '解析后的分类数值字符串（如 0.78）',
  `img_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图片 URL',
  `uav_media_id` bigint DEFAULT NULL COMMENT '关联 sf_uav_media_file.uav_media_id',
  `source_file_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原始图片 file_id',
  `source_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原始图片文件名',
  `source_object_key` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原始图片对象键或URL',
  `shoot_lat` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原始图片拍摄纬度',
  `shoot_lng` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原始图片拍摄经度',
  `shoot_time` datetime DEFAULT NULL COMMENT '原始图片拍摄时间',
  `alarm_time` bigint DEFAULT NULL COMMENT '告警时间（中台时间戳，毫秒）',
  `video_play_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原始视频地址',
  `stream_server_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流服务地址',
  `computing_video_play_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计算流地址',
  `push_video_play_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '推流侧地址',
  `raw_json` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '回调原文 JSON',
  `create_time` datetime DEFAULT NULL COMMENT '入库时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_ai_inf_dedup` (`tenant_id`,`task_no`,`alarm_time`,`img_url`(200)) USING BTREE,
  KEY `idx_ai_inf_tenant_time` (`tenant_id`,`alarm_time`) USING BTREE,
  KEY `idx_ai_inf_task` (`task_no`) USING BTREE,
  KEY `idx_ai_inf_media` (`uav_media_id`) USING BTREE,
  KEY `idx_ai_inf_task_media` (`tenant_id`,`task_no`,`uav_media_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='算法中台推理结果回调流水';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_asset_device` (
  `asset_device_id` bigint NOT NULL COMMENT '资产设备主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `asset_type_id` bigint NOT NULL COMMENT '资产种类主键',
  `device_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备编号',
  `device_name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备名称',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'IDLE/IN_USE/MAINTENANCE/SCRAPPED',
  `current_holder_employee_id` bigint DEFAULT NULL COMMENT '当前领用员工ID',
  `current_holder_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前领用人姓名快照',
  `acquired_date` date DEFAULT NULL COMMENT '购置日期',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`asset_device_id`),
  UNIQUE KEY `uk_sf_asset_device_no` (`tenant_id`,`device_no`),
  KEY `idx_sf_asset_device_page` (`tenant_id`,`del_flag`,`status`,`asset_type_id`,`device_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产设备';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_asset_type` (
  `asset_type_id` bigint NOT NULL COMMENT '资产种类主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `type_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '种类编码',
  `type_name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '种类名称',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`asset_type_id`),
  UNIQUE KEY `uk_sf_asset_type_code` (`tenant_id`,`type_code`),
  KEY `idx_sf_asset_type_page` (`tenant_id`,`del_flag`,`enabled`,`type_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产种类';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_asset_usage_log` (
  `usage_log_id` bigint NOT NULL COMMENT '资产使用流水主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `asset_device_id` bigint NOT NULL COMMENT '资产设备主键',
  `action` varchar(24) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'CREATE/CHECK_OUT/RETURN/MAINTENANCE/SCRAP',
  `from_status` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原状态',
  `to_status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '新状态',
  `holder_employee_id` bigint DEFAULT NULL COMMENT '领用员工ID',
  `holder_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '领用人姓名快照',
  `operator_employee_id` bigint DEFAULT NULL COMMENT '操作员工ID',
  `operator_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人姓名快照',
  `occurred_at` datetime NOT NULL COMMENT '发生时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '说明',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`usage_log_id`),
  KEY `idx_sf_asset_usage_timeline` (`tenant_id`,`asset_device_id`,`occurred_at`,`usage_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产领用归还与状态时间线';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_crop_species` (
  `species_id` bigint NOT NULL COMMENT '物种ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `species_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '物种编号（租户内唯一）',
  `species_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '物种名称',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `map_icon_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地图图标 URL（OSS/HTTPS）',
  `map_icon_emoji` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地图 Emoji（可选）',
  `growth_stage_config_json` json DEFAULT NULL COMMENT '生长阶段配置JSON',
  `remote_sensing_code` int DEFAULT NULL COMMENT '遥感编号，卫星遥感时作为 code_croptype',
  PRIMARY KEY (`species_id`) USING BTREE,
  UNIQUE KEY `uk_sf_crop_species_tenant_code` (`tenant_id`,`species_code`) USING BTREE,
  KEY `idx_sf_crop_species_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='农作物物种表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_crop_species_template` (
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `template_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板编码',
  `species_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物种编码',
  `species_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物种名称',
  `remote_sensing_code` int DEFAULT NULL COMMENT '遥感作物编码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `map_icon_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地图图标URL',
  `map_icon_emoji` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地图图标Emoji',
  `growth_stage_config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '生长阶段配置JSON',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_species_template_code` (`template_code`),
  UNIQUE KEY `uk_species_code` (`species_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='耘上田注册作物物种模板表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_crop_variety` (
  `variety_id` bigint NOT NULL COMMENT '品种ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `species_id` bigint NOT NULL COMMENT '物种ID（sf_crop_species）',
  `variety_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '品种编号（租户内唯一）',
  `variety_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '品种名称',
  `growth_cycle_days` int DEFAULT NULL COMMENT '生长周期（天）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `map_icon_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '品种级图标覆盖 URL',
  `map_icon_emoji` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '品种级 Emoji 覆盖',
  PRIMARY KEY (`variety_id`) USING BTREE,
  UNIQUE KEY `uk_sf_crop_variety_tenant_code` (`tenant_id`,`variety_code`) USING BTREE,
  KEY `idx_sf_crop_variety_species` (`species_id`) USING BTREE,
  KEY `idx_sf_crop_variety_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='农作物品种表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_crop_variety_template` (
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `template_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板编码',
  `species_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '所属物种编码',
  `variety_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '品种编码',
  `variety_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '品种名称',
  `growth_cycle_days` int DEFAULT NULL COMMENT '生长周期天数',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `map_icon_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地图图标URL',
  `map_icon_emoji` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地图图标Emoji',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_variety_template_code` (`template_code`),
  UNIQUE KEY `uk_variety_code` (`variety_code`),
  KEY `idx_variety_species_code` (`species_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='耘上田注册作物品种模板表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_farm_work_assignment` (
  `assignment_id` bigint NOT NULL COMMENT '农事分配主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `greenhouse_id` bigint NOT NULL COMMENT '大棚ID，对应 sf_field.field_id 且 field_type=GREENHOUSE',
  `work_item_id` bigint NOT NULL COMMENT '农事项目ID，对应 sf_farm_work_dict.dict_id 且 node_type=ITEM',
  `work_item_name_snapshot` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '农事项目名称快照',
  `work_item_code_snapshot` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '农事项目编码快照',
  `category_id_snapshot` bigint DEFAULT NULL COMMENT '农事分类ID快照',
  `category_name_snapshot` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '农事分类名称快照',
  `leader_id` bigint NOT NULL COMMENT '组长人员ID，对应 sys_employee.employee_id',
  `assigned_at` datetime NOT NULL COMMENT '分配时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`assignment_id`) USING BTREE,
  UNIQUE KEY `uk_sf_farm_assign_greenhouse_item` (`tenant_id`,`greenhouse_id`,`work_item_id`) USING BTREE,
  KEY `idx_sf_farm_assign_greenhouse_leader` (`tenant_id`,`greenhouse_id`,`leader_id`) USING BTREE,
  KEY `idx_sf_farm_assign_leader` (`tenant_id`,`leader_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask农事分配表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_farm_work_dict` (
  `dict_id` bigint NOT NULL COMMENT '农事字典主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父节点ID：0=分类，非0=项目所属分类ID',
  `node_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '节点类型：CATEGORY=分类，ITEM=项目',
  `dict_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `dict_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '编码，租户内分类与项目共用命名空间',
  `min_workers` int DEFAULT NULL COMMENT '最少工人数，仅项目有效，最小0',
  `max_workers` int DEFAULT NULL COMMENT '最多工人数，仅项目有效，最大30',
  `requires_material` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要领料，仅 ITEM 有效',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态：0=启用，1=停用，仅项目有效',
  `custom_form_template_json` json DEFAULT NULL COMMENT '自定义表单模板JSON，仅ITEM有效',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '同级排序，越小越靠前',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志：0=存在，1=删除',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`) USING BTREE,
  KEY `idx_sf_farm_work_code` (`tenant_id`,`dict_code`,`del_flag`) USING BTREE,
  KEY `idx_sf_farm_work_parent_sort` (`tenant_id`,`parent_id`,`sort_order`,`dict_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask农事分类与项目字典';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_farm_work_dict_template` (
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `template_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板编码',
  `parent_template_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '父模板编码',
  `node_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点类型（CATEGORY分类 ITEM项目）',
  `dict_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '农事编码',
  `dict_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '农事名称',
  `min_workers` int DEFAULT NULL COMMENT '最少工人数',
  `max_workers` int DEFAULT NULL COMMENT '最多工人数',
  `requires_material` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要领料，仅 ITEM 有效',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `custom_form_template_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '自定义表单模板JSON',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_farm_work_template_code` (`template_code`),
  UNIQUE KEY `uk_farm_work_dict_code` (`dict_code`),
  KEY `idx_farm_work_parent_template_code` (`parent_template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='耘上田注册农事字典模板表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_farming_record` (
  `record_id` bigint NOT NULL COMMENT '农事记录主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT=草稿，SUBMITTED=已提交',
  `happened_at` datetime NOT NULL COMMENT '农事发生时间',
  `work_period_json` json DEFAULT NULL COMMENT '农事作业时段JSON，数组格式：[开始时间,结束时间]，精确到分钟',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '今日小结，最大500字',
  `weather_json` json DEFAULT NULL COMMENT '天气快照JSON',
  `growth_stage_json` json DEFAULT NULL COMMENT '生长阶段快照JSON',
  `resource_json` json DEFAULT NULL COMMENT '资源投入JSON：人工、农机、物料',
  `feedback_json` json DEFAULT NULL COMMENT '现场反馈JSON：苗情长势等',
  `sensor_snapshot_json` json DEFAULT NULL COMMENT '地块关联传感器数据快照JSON',
  `environment_summary_json` json DEFAULT NULL COMMENT '环境摘要JSON',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志：0=存在，1=删除',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`record_id`) USING BTREE,
  KEY `idx_sf_farming_record_time` (`tenant_id`,`happened_at`,`record_id`) USING BTREE,
  KEY `idx_sf_farming_record_status_submit` (`tenant_id`,`status`,`submit_time`,`record_id`) USING BTREE,
  KEY `idx_sf_farming_record_creator_status` (`tenant_id`,`create_by`,`status`,`record_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='移动端农事记录主表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_farming_record_field` (
  `id` bigint NOT NULL COMMENT '记录地块明细主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `record_id` bigint NOT NULL COMMENT '农事记录ID',
  `field_id` bigint NOT NULL COMMENT '地块ID',
  `field_code_snapshot` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地块编号快照',
  `field_name_snapshot` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '地块名称快照',
  `sowing_date_snapshot` date DEFAULT NULL COMMENT '种植日期快照',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '记录内排序',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sf_farming_record_field_record` (`tenant_id`,`record_id`,`sort_order`,`id`) USING BTREE,
  KEY `idx_sf_farming_record_field_field` (`tenant_id`,`field_id`,`record_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='移动端农事记录地块明细';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_farming_record_media` (
  `media_id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `record_id` bigint NOT NULL COMMENT '农事记录 ID',
  `kind` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'IMAGE/VIDEO/AUDIO',
  `url` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '访问 URL 或可解析 OSS key',
  `seq` int NOT NULL DEFAULT '0' COMMENT '同记录内排序',
  `lat` decimal(12,8) DEFAULT NULL COMMENT '拍摄纬度（可选）',
  `lng` decimal(12,8) DEFAULT NULL COMMENT '拍摄经度（可选）',
  `caption` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `captured_at` datetime DEFAULT NULL COMMENT '拍摄时间（可选）',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`media_id`) USING BTREE,
  KEY `idx_sf_far_med_record` (`record_id`) USING BTREE,
  KEY `idx_sf_far_med_tenant_record` (`tenant_id`,`record_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='农事记录媒体';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_farming_record_type` (
  `type_id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `type_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户内唯一编码（API/前端稳定引用）',
  `type_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '展示名称',
  `list_icon_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列表/Tab 可选图标 URL',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序（升序）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '0正常 1停用',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`type_id`) USING BTREE,
  UNIQUE KEY `uk_sf_farming_type_tenant_code` (`tenant_id`,`type_code`) USING BTREE,
  KEY `idx_sf_farming_type_tenant_status` (`tenant_id`,`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='农事类型主数据（移动端与管理端共用）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_farming_record_work_item` (
  `item_id` bigint NOT NULL COMMENT '记录农事项目明细主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `record_id` bigint NOT NULL COMMENT '农事记录ID',
  `work_item_id` bigint NOT NULL COMMENT '农事项目ID，对应 sf_farm_work_dict.dict_id',
  `work_item_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '农事项目编码快照',
  `work_item_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '农事项目名称快照',
  `category_id` bigint NOT NULL COMMENT '农事分类ID',
  `category_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '农事分类编码快照',
  `category_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '农事分类名称快照',
  `custom_form_template_json` json DEFAULT NULL COMMENT '农事项目自定义表单模板快照JSON',
  `custom_form_data_json` json DEFAULT NULL COMMENT '农事项目自定义表单提交值JSON',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '记录内排序',
  PRIMARY KEY (`item_id`) USING BTREE,
  KEY `idx_sf_farming_record_work_item_record` (`tenant_id`,`record_id`,`sort_order`,`item_id`) USING BTREE,
  KEY `idx_sf_farming_record_work_item_work` (`tenant_id`,`work_item_id`,`record_id`) USING BTREE,
  KEY `idx_sf_farming_record_work_item_category` (`tenant_id`,`category_id`,`record_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='移动端农事记录项目明细';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_field` (
  `field_id` bigint NOT NULL COMMENT '地块ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号（农场主=租户主体，地块归属本租户）',
  `owner_user_id` bigint DEFAULT NULL COMMENT '业务负责人用户ID（sys_user.user_id，PRD归属主体）；与tenant_id一致性应用层校验',
  `field_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地块编码（可选，租户内唯一）',
  `field_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '地块名称',
  `greenhouse_short_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '大棚简称，大屏优先显示',
  `greenhouse_color` char(7) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL COMMENT '大棚大屏强调色，#RRGGBB',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序值，升序',
  `field_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'FIELD' COMMENT '地块类型：FIELD大田 GREENHOUSE大棚',
  `boundary_geojson` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '地块范围（GeoJSON Polygon）',
  `center_lng` decimal(10,7) DEFAULT NULL COMMENT '中心经度',
  `center_lat` decimal(10,7) DEFAULT NULL COMMENT '中心纬度',
  `area_mu` decimal(12,4) DEFAULT NULL COMMENT '面积（亩）',
  `address_text` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地址描述',
  `admin_division_text` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行政区划路径文本（展示与模糊搜索）',
  `admin_division_adcode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '区划国标代码（可选，级联控件/精确筛选）',
  `map_zoom_level` int DEFAULT NULL COMMENT '地图缩放层级（保存时 zoom，便于回显）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '档案启用（0正常 1停用），对应PRD正常/停用开关',
  `field_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'IDLE' COMMENT '地块业务状态：IDLE空闲 IN_USE使用中 MAINTENANCE维护中',
  `map_provider` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地图服务商：amap/baidu 等',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `uav_device_sn` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '飞控工作空间下无人机设备 SN（与 uavBinding 对应空间一致；应用层校验设备在该空间设备列表中）',
  PRIMARY KEY (`field_id`) USING BTREE,
  UNIQUE KEY `uk_sf_field_tenant_code` (`tenant_id`,`field_code`) USING BTREE,
  KEY `idx_sf_field_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_sf_field_status` (`field_status`) USING BTREE,
  KEY `idx_sf_field_owner` (`owner_user_id`) USING BTREE,
  KEY `idx_sf_field_archive` (`tenant_id`,`status`) USING BTREE,
  KEY `idx_sf_field_admin_adcode` (`admin_division_adcode`) USING BTREE,
  KEY `idx_sf_field_type` (`field_type`) USING BTREE,
  KEY `idx_sf_field_tenant_name` (`tenant_id`,`field_name`) USING BTREE,
  KEY `idx_sf_field_tenant_sort` (`tenant_id`,`sort_order`,`field_id`) USING BTREE,
  KEY `idx_sf_field_health_list` (`tenant_id`,`status`,`del_flag`,`sort_order`,`field_code`,`field_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='地块表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_field_iot` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `field_id` bigint NOT NULL COMMENT '地块ID（sf_field.field_id）',
  `device_sn` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '物联网设备 SN',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_sf_field_iot` (`tenant_id`,`field_id`,`device_sn`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='地块与物联网设备关联（按SN）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_greenhouse_layout` (
  `layout_id` bigint NOT NULL COMMENT '布局主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '布局乐观锁版本',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`layout_id`) USING BTREE,
  UNIQUE KEY `uk_sf_greenhouse_layout_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='租户大棚二维布局';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_greenhouse_layout_column` (
  `column_id` bigint NOT NULL COMMENT '布局列主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `layout_id` bigint NOT NULL COMMENT '布局主键，逻辑关联 sf_greenhouse_layout.layout_id',
  `column_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '列名称',
  `column_order` int NOT NULL COMMENT '列顺序，从1开始',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`column_id`) USING BTREE,
  UNIQUE KEY `uk_sf_greenhouse_layout_column_order` (`layout_id`,`column_order`) USING BTREE,
  UNIQUE KEY `uk_sf_greenhouse_layout_column_name` (`layout_id`,`column_name`) USING BTREE,
  KEY `idx_sf_greenhouse_layout_column_tenant` (`tenant_id`,`layout_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='大棚布局列';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_greenhouse_layout_item` (
  `item_id` bigint NOT NULL COMMENT '棚位主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `layout_id` bigint NOT NULL COMMENT '布局主键，逻辑关联 sf_greenhouse_layout.layout_id',
  `column_id` bigint NOT NULL COMMENT '布局列主键，逻辑关联 sf_greenhouse_layout_column.column_id',
  `field_id` bigint NOT NULL COMMENT '大棚地块主键，逻辑关联 sf_field.field_id',
  `row_order` int NOT NULL COMMENT '列内行顺序，从1开始',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`item_id`) USING BTREE,
  UNIQUE KEY `uk_sf_greenhouse_layout_item_field` (`layout_id`,`field_id`) USING BTREE,
  UNIQUE KEY `uk_sf_greenhouse_layout_item_position` (`column_id`,`row_order`) USING BTREE,
  KEY `idx_sf_greenhouse_layout_item_tenant` (`tenant_id`,`layout_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='大棚布局棚位';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_i18n_text` (
  `translation_id` bigint NOT NULL COMMENT '翻译主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `locale` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标语言，例如 ug-CN',
  `source_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '中文原文快照',
  `source_hash` char(64) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT '中文原文 SHA-256',
  `translated_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '目标语言译文',
  `translation_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'PENDING/PROCESSING/SUCCESS/FAILED',
  `translation_origin` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'MACHINE/MANUAL',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '已尝试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次自动重试时间',
  `lease_until` datetime DEFAULT NULL COMMENT 'Worker 租约截止时间',
  `processing_token` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Worker 领取令牌',
  `provider` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '翻译服务提供商',
  `last_error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近失败错误码',
  `last_error_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近失败摘要',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志：0存在 1删除',
  `create_dept` bigint DEFAULT NULL,
  PRIMARY KEY (`translation_id`) USING BTREE,
  UNIQUE KEY `uk_sf_i18n_phrase` (`tenant_id`,`locale`,`source_hash`) USING BTREE,
  KEY `idx_sf_i18n_phrase_worker` (`del_flag`,`translation_status`,`next_retry_time`,`lease_until`,`translation_id`) USING BTREE,
  KEY `idx_sf_i18n_phrase_page` (`tenant_id`,`del_flag`,`translation_status`,`update_time`,`translation_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='智慧农业业务文本多语言翻译';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inspection_ai_context_summary` (
  `summary_id` bigint NOT NULL COMMENT '摘要主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编号',
  `conversation_id` bigint NOT NULL COMMENT '会话主键',
  `covered_through_message_seq` int NOT NULL COMMENT '已覆盖最大消息序号',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '结构化中文历史摘要',
  `model_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '实际调用模型',
  `fallback_used` tinyint NOT NULL DEFAULT '0' COMMENT '是否使用备选模型',
  `provider_request_id` varchar(128) DEFAULT NULL,
  `input_tokens` int DEFAULT NULL,
  `output_tokens` int DEFAULT NULL,
  `total_tokens` int DEFAULT NULL,
  `generation_status` varchar(16) NOT NULL COMMENT 'PENDING/COMPLETED/FAILED',
  `error_code` varchar(64) DEFAULT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `started_time` datetime DEFAULT NULL,
  `completed_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`summary_id`),
  UNIQUE KEY `uk_sf_inspection_ai_summary_coverage` (`conversation_id`,`covered_through_message_seq`),
  KEY `idx_sf_inspection_ai_summary_latest` (`tenant_id`,`conversation_id`,`covered_through_message_seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='巡查照片 AI 历史摘要';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inspection_ai_conversation` (
  `conversation_id` bigint NOT NULL COMMENT '会话主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编号',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话标题',
  `first_question_time` datetime DEFAULT NULL COMMENT '首轮提问时间',
  `last_question_time` datetime DEFAULT NULL COMMENT '最后提问时间',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`conversation_id`),
  KEY `idx_sf_inspection_ai_conversation_recent` (`tenant_id`,`last_question_time`,`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='巡查照片 AI 会话';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inspection_ai_message` (
  `message_id` bigint NOT NULL COMMENT '消息主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编号',
  `conversation_id` bigint NOT NULL COMMENT '会话主键',
  `message_seq` int NOT NULL COMMENT '会话内递增序号',
  `role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'USER 或 ASSISTANT',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '问题或回答正文',
  `client_request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户请求幂等 UUID',
  `reply_to_message_id` bigint DEFAULT NULL COMMENT '助手回答对应的用户消息',
  `generation_attempt` int NOT NULL DEFAULT '0' COMMENT '同一问题的生成次数',
  `generation_status` varchar(16) NOT NULL DEFAULT 'COMPLETED' COMMENT 'PENDING/STREAMING/COMPLETED/FAILED/SUPERSEDED',
  `model_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '实际调用模型',
  `provider_request_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模型服务请求标识',
  `input_tokens` int DEFAULT NULL,
  `output_tokens` int DEFAULT NULL,
  `total_tokens` int DEFAULT NULL,
  `error_code` varchar(64) DEFAULT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `started_time` datetime DEFAULT NULL,
  `completed_time` datetime DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`message_id`),
  UNIQUE KEY `uk_sf_inspection_ai_message_seq` (`conversation_id`,`message_seq`),
  UNIQUE KEY `uk_sf_inspection_ai_request` (`tenant_id`,`create_by`,`client_request_id`),
  KEY `idx_sf_inspection_ai_message_conversation` (`tenant_id`,`conversation_id`,`message_id`),
  KEY `idx_sf_inspection_ai_message_reply` (`reply_to_message_id`,`generation_attempt`),
  KEY `idx_sf_inspection_ai_message_generation` (`generation_status`,`started_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='巡查照片 AI 消息';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inspection_ai_message_photo` (
  `message_photo_id` bigint NOT NULL COMMENT '消息附件主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编号',
  `message_id` bigint NOT NULL COMMENT '用户消息主键',
  `photo_id` bigint NOT NULL COMMENT '归档照片主键快照',
  `oss_id` bigint NOT NULL COMMENT 'OSS 对象主键快照',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '原始文件名快照',
  `archive_id` bigint DEFAULT NULL COMMENT '归档主键快照',
  `archive_field_id` bigint DEFAULT NULL COMMENT '归档大棚快照主键',
  `archive_date_snapshot` date DEFAULT NULL COMMENT '归档日期快照',
  `greenhouse_code_snapshot` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '大棚编号快照',
  `greenhouse_name_snapshot` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '大棚名称快照',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '本次发送顺序',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`message_photo_id`),
  UNIQUE KEY `uk_sf_inspection_ai_message_photo` (`message_id`,`photo_id`),
  KEY `idx_sf_inspection_ai_message_photo_message` (`tenant_id`,`message_id`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='巡查照片 AI 消息附件快照';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inspection_photo_archive` (
  `archive_id` bigint NOT NULL COMMENT '归档主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `archive_date` date NOT NULL COMMENT '巡查归档日期',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`archive_id`) USING BTREE,
  UNIQUE KEY `uk_sf_inspection_archive_tenant_date` (`tenant_id`,`archive_date`) USING BTREE,
  KEY `idx_sf_inspection_archive_tenant_date` (`tenant_id`,`archive_date`,`archive_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='巡查照片归档日期';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inspection_photo_archive_field` (
  `archive_field_id` bigint NOT NULL COMMENT '归档大棚快照主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `archive_id` bigint NOT NULL COMMENT '归档主键，逻辑关联 sf_inspection_photo_archive.archive_id',
  `field_id` bigint NOT NULL COMMENT '来源大棚主键，逻辑关联 sf_field.field_id',
  `field_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归档时大棚编号快照',
  `field_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '归档时大棚名称快照',
  `sort_order` int DEFAULT NULL COMMENT '归档时大棚排序值',
  `planting_batch_id` bigint DEFAULT NULL COMMENT '归档时进行中种植批次主键',
  `species_id` bigint DEFAULT NULL COMMENT '归档时作物物种主键',
  `species_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归档时作物物种名称快照',
  `variety_id` bigint DEFAULT NULL COMMENT '归档时作物品种主键',
  `variety_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归档时作物品种名称快照',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`archive_field_id`) USING BTREE,
  UNIQUE KEY `uk_sf_inspection_archive_field` (`archive_id`,`field_id`) USING BTREE,
  KEY `idx_sf_inspection_archive_field_source` (`tenant_id`,`field_id`) USING BTREE,
  KEY `idx_sf_inspection_archive_field_order` (`tenant_id`,`archive_id`,`sort_order`,`field_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='巡查照片归档大棚快照';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inspection_photo_archive_photo` (
  `photo_id` bigint NOT NULL COMMENT '归档照片关联主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `archive_field_id` bigint NOT NULL COMMENT '归档大棚快照主键，逻辑关联 sf_inspection_photo_archive_field.archive_field_id',
  `oss_id` bigint NOT NULL COMMENT '系统 OSS 对象主键，逻辑关联 master 库 sys_oss.oss_id',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '上传原始文件名',
  `client_upload_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '小程序单张上传幂等标识，管理端上传为空',
  `seq` int NOT NULL DEFAULT '0' COMMENT '同一归档大棚内照片顺序',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`photo_id`) USING BTREE,
  UNIQUE KEY `uk_sf_inspection_archive_photo_oss` (`archive_field_id`,`oss_id`) USING BTREE,
  UNIQUE KEY `uk_sf_inspection_archive_photo_client_upload` (`archive_field_id`,`client_upload_id`),
  KEY `idx_sf_inspection_archive_photo_order` (`tenant_id`,`archive_field_id`,`seq`,`photo_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='巡查照片归档照片关联';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_balance` (
  `balance_id` bigint NOT NULL COMMENT '余额主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `material_id` bigint NOT NULL COMMENT '物资主键',
  `quantity` decimal(18,1) NOT NULL DEFAULT '0.0' COMMENT '当前结余',
  `abnormal_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '异常原因',
  `outbound_locked` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否锁定出库',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`balance_id`),
  UNIQUE KEY `uk_sf_inventory_balance_material` (`tenant_id`,`material_id`),
  KEY `idx_sf_inventory_balance_abnormal` (`tenant_id`,`outbound_locked`,`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存余额缓存';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_business_sequence` (
  `sequence_id` bigint NOT NULL COMMENT '序列主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `business_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '单据类型',
  `sequence_date` date NOT NULL COMMENT '序列日期',
  `current_value` bigint NOT NULL DEFAULT '0' COMMENT '当前序号',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`sequence_id`),
  UNIQUE KEY `uk_sf_inventory_sequence_scope` (`tenant_id`,`business_type`,`sequence_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户业务编号序列';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_idempotency` (
  `idempotency_id` bigint NOT NULL COMMENT '幂等记录主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `operator_employee_id` bigint NOT NULL COMMENT '发起员工ID',
  `operation_type` varchar(48) COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作类型',
  `idempotency_key` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '客户端幂等键',
  `request_hash` char(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '规范化请求摘要',
  `operation_status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'PROCESSING/SUCCEEDED',
  `response_json` json DEFAULT NULL COMMENT '首次成功响应',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`idempotency_id`),
  UNIQUE KEY `uk_sf_inventory_idempotency_scope` (`tenant_id`,`operation_type`,`idempotency_key`),
  KEY `idx_sf_inventory_idempotency_status` (`operation_status`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存领域持久化幂等记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_inbound_line` (
  `inbound_line_id` bigint NOT NULL COMMENT '入库单行主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `inbound_order_id` bigint NOT NULL COMMENT '入库单主键',
  `material_id` bigint NOT NULL COMMENT '物资主键',
  `material_code_snapshot` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物资编码快照',
  `material_name_snapshot` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物资名称快照',
  `specification_snapshot` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格快照',
  `unit_snapshot` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '单位快照',
  `quantity` decimal(18,1) NOT NULL COMMENT '入库数量',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`inbound_line_id`),
  UNIQUE KEY `uk_sf_inventory_inbound_material` (`tenant_id`,`inbound_order_id`,`material_id`),
  KEY `idx_sf_inventory_inbound_line_material` (`tenant_id`,`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存入库单明细';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_inbound_order` (
  `inbound_order_id` bigint NOT NULL COMMENT '入库单主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `order_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '入库单号',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'COMPLETED',
  `supplier_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应方名称快照',
  `business_date` date NOT NULL COMMENT '业务日期',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`inbound_order_id`),
  UNIQUE KEY `uk_sf_inventory_inbound_no` (`tenant_id`,`order_no`),
  KEY `idx_sf_inventory_inbound_page` (`tenant_id`,`business_date`,`inbound_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存入库单';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_ledger` (
  `ledger_id` bigint NOT NULL COMMENT '流水主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `material_id` bigint NOT NULL COMMENT '物资主键',
  `ledger_type` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'INBOUND/OUTBOUND/RETURN/ADJUST',
  `business_subtype` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务子类型',
  `quantity_delta` decimal(18,1) NOT NULL COMMENT '库存增量，出库为负',
  `balance_after` decimal(18,1) NOT NULL COMMENT '变更后结余',
  `related_order_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '关联单据类型',
  `related_order_id` bigint NOT NULL COMMENT '关联单据ID',
  `related_order_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '关联单据编号',
  `source_line_id` bigint NOT NULL COMMENT '来源单据行ID',
  `effective` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否有效',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '历史纠错版本',
  `correction_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近纠错原因',
  `operator_employee_id` bigint DEFAULT NULL COMMENT '操作员工ID',
  `operator_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人姓名快照',
  `occurred_at` datetime NOT NULL COMMENT '业务发生时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`ledger_id`),
  UNIQUE KEY `uk_sf_inventory_ledger_source` (`tenant_id`,`related_order_type`,`source_line_id`),
  KEY `idx_sf_inventory_ledger_material_time` (`tenant_id`,`material_id`,`occurred_at`,`ledger_id`),
  KEY `idx_sf_inventory_ledger_order` (`tenant_id`,`related_order_type`,`related_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存增量事实流水';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_legacy_material_map` (
  `map_id` bigint NOT NULL COMMENT '映射主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `legacy_material_id` bigint NOT NULL COMMENT '旧物资ID',
  `inventory_material_id` bigint NOT NULL COMMENT '新物资ID',
  `original_quantity` decimal(18,3) NOT NULL COMMENT '旧余额原值',
  `rounded_quantity` decimal(18,1) NOT NULL COMMENT 'HALF_UP 一位小数余额',
  `rounding_delta` decimal(18,3) NOT NULL COMMENT '舍入差额',
  `migration_batch` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '迁移批次',
  `migrated_at` datetime NOT NULL COMMENT '迁移时间',
  PRIMARY KEY (`map_id`),
  UNIQUE KEY `uk_sf_inventory_legacy_old` (`tenant_id`,`legacy_material_id`),
  UNIQUE KEY `uk_sf_inventory_legacy_new` (`tenant_id`,`inventory_material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='旧库存物资只读映射与舍入审计';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_material` (
  `material_id` bigint NOT NULL COMMENT '库存物资主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `material_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物资编码，租户内唯一',
  `material_name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物资名称',
  `category_id` bigint NOT NULL COMMENT '物料分类主键',
  `category` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资分类',
  `specification` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '计量单位',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`material_id`),
  UNIQUE KEY `uk_sf_inventory_material_code` (`tenant_id`,`material_code`),
  KEY `idx_sf_inventory_material_page` (`tenant_id`,`del_flag`,`enabled`,`material_name`),
  KEY `idx_sf_inventory_material_category` (`tenant_id`,`category_id`,`del_flag`,`enabled`,`material_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存物资档案';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_material_category` (
  `category_id` bigint NOT NULL COMMENT '物料分类主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `category_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称，租户内唯一',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `uk_sf_inventory_material_category_name` (`tenant_id`,`category_name`),
  KEY `idx_sf_inventory_material_category_page` (`tenant_id`,`enabled`,`sort_order`,`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存物料分类';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_operation_log` (
  `operation_log_id` bigint NOT NULL COMMENT '操作审计主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `aggregate_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '聚合类型',
  `aggregate_id` bigint NOT NULL COMMENT '聚合主键',
  `action` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作动作',
  `reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原因',
  `before_snapshot` json DEFAULT NULL COMMENT '操作前完整快照',
  `after_snapshot` json DEFAULT NULL COMMENT '操作后完整快照',
  `operator_employee_id` bigint DEFAULT NULL COMMENT '操作员工ID',
  `operator_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人姓名快照',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`operation_log_id`),
  KEY `idx_sf_inventory_operation_aggregate` (`tenant_id`,`aggregate_type`,`aggregate_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存与资产领域操作审计';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_outbound_line` (
  `outbound_line_id` bigint NOT NULL COMMENT '出库行主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `outbound_order_id` bigint NOT NULL COMMENT '出库单主键',
  `material_receipt_line_id` bigint DEFAULT NULL COMMENT '领料单行ID',
  `material_id` bigint NOT NULL COMMENT '物资主键',
  `material_code_snapshot` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物资编码快照',
  `material_name_snapshot` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物资名称快照',
  `specification_snapshot` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格快照',
  `unit_snapshot` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '单位快照',
  `requested_quantity` decimal(18,1) NOT NULL COMMENT '申请数量',
  `actual_quantity` decimal(18,1) NOT NULL COMMENT '实际数量',
  `deleted_by_keeper` tinyint(1) NOT NULL DEFAULT '0' COMMENT '库管是否删除该行',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '行版本',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`outbound_line_id`),
  UNIQUE KEY `uk_sf_inventory_outbound_material` (`tenant_id`,`outbound_order_id`,`material_id`),
  KEY `idx_sf_inventory_outbound_line_material` (`tenant_id`,`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存出库单明细';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_outbound_order` (
  `outbound_order_id` bigint NOT NULL COMMENT '出库单主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `order_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '出库单号',
  `source` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'MATERIAL_RECEIPT/WEB_DIRECT',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'PENDING/COMPLETED/CANCELLED',
  `material_receipt_id` bigint DEFAULT NULL COMMENT '关联领料单',
  `task_package_id` bigint DEFAULT NULL COMMENT '任务包ID',
  `farm_item_id` bigint DEFAULT NULL COMMENT '农事项ID',
  `leader_employee_id` bigint DEFAULT NULL COMMENT '组长员工ID',
  `receiver_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '领用人姓名快照',
  `task_name_snapshot` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务名称快照',
  `farm_work_name_snapshot` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '农事项名称快照',
  `greenhouse_names_snapshot` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '大棚名称快照',
  `business_date` date NOT NULL COMMENT '业务日期',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认人',
  `confirmed_at` datetime DEFAULT NULL COMMENT '确认时间',
  `cancelled_by` bigint DEFAULT NULL COMMENT '取消人',
  `cancelled_at` datetime DEFAULT NULL COMMENT '取消时间',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`outbound_order_id`),
  UNIQUE KEY `uk_sf_inventory_outbound_no` (`tenant_id`,`order_no`),
  KEY `idx_sf_inventory_outbound_page` (`tenant_id`,`status`,`create_time`,`outbound_order_id`),
  KEY `idx_sf_inventory_outbound_receipt` (`tenant_id`,`material_receipt_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存出库单';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_return_line` (
  `return_line_id` bigint NOT NULL COMMENT '退库行主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `return_order_id` bigint NOT NULL COMMENT '退库单主键',
  `material_receipt_line_id` bigint DEFAULT NULL COMMENT '领料单行ID',
  `material_id` bigint NOT NULL COMMENT '物资主键',
  `material_code_snapshot` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物资编码快照',
  `material_name_snapshot` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物资名称快照',
  `specification_snapshot` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格快照',
  `unit_snapshot` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '单位快照',
  `issued_quantity_snapshot` decimal(18,1) NOT NULL DEFAULT '0.0' COMMENT '原实发快照',
  `returned_before_snapshot` decimal(18,1) NOT NULL DEFAULT '0.0' COMMENT '申请时累计已退',
  `remaining_before_snapshot` decimal(18,1) NOT NULL DEFAULT '0.0' COMMENT '申请时剩余可退',
  `requested_return_quantity` decimal(18,1) NOT NULL COMMENT '申请退库数量',
  `actual_return_quantity` decimal(18,1) DEFAULT NULL COMMENT '实际退库数量',
  `deleted_by_keeper` tinyint(1) NOT NULL DEFAULT '0' COMMENT '库管是否删除该行',
  `invalid_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行级失效原因',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '行版本',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`return_line_id`),
  UNIQUE KEY `uk_sf_inventory_return_material` (`tenant_id`,`return_order_id`,`material_id`),
  KEY `idx_sf_inventory_return_line_material` (`tenant_id`,`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存退库单明细';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_return_order` (
  `return_order_id` bigint NOT NULL COMMENT '退库单主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `order_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '退库单号',
  `source` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'LEADER_APPLY/WEB_DIRECT',
  `status` varchar(24) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'PENDING_RETURN/RETURNED',
  `material_receipt_id` bigint DEFAULT NULL COMMENT '关联领料单',
  `receipt_no_snapshot` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '领料单号快照',
  `task_package_id` bigint DEFAULT NULL COMMENT '任务包ID',
  `farm_item_id` bigint DEFAULT NULL COMMENT '农事项ID',
  `leader_employee_id` bigint DEFAULT NULL COMMENT '组长员工ID',
  `returner_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '退库人姓名快照',
  `task_name_snapshot` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务名称快照',
  `farm_work_name_snapshot` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '农事项名称快照',
  `greenhouse_names_snapshot` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '大棚名称快照',
  `invalid_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '整单失效原因',
  `business_date` date NOT NULL COMMENT '业务日期',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认人',
  `confirmed_at` datetime DEFAULT NULL COMMENT '确认时间',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`return_order_id`),
  UNIQUE KEY `uk_sf_inventory_return_no` (`tenant_id`,`order_no`),
  KEY `idx_sf_inventory_return_page` (`tenant_id`,`status`,`create_time`,`return_order_id`),
  KEY `idx_sf_inventory_return_receipt` (`tenant_id`,`material_receipt_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存退库单';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_stocktake_line` (
  `stocktake_line_id` bigint NOT NULL COMMENT '盘点行主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `stocktake_order_id` bigint NOT NULL COMMENT '盘点单主键',
  `material_id` bigint NOT NULL COMMENT '物资主键',
  `material_code_snapshot` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物资编码快照',
  `material_name_snapshot` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物资名称快照',
  `specification_snapshot` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格快照',
  `unit_snapshot` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '单位快照',
  `system_quantity` decimal(18,1) NOT NULL COMMENT '系统数量',
  `actual_quantity` decimal(18,1) NOT NULL COMMENT '实盘数量',
  `difference_quantity` decimal(18,1) NOT NULL COMMENT '实盘减系统',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`stocktake_line_id`),
  UNIQUE KEY `uk_sf_inventory_stocktake_material` (`tenant_id`,`stocktake_order_id`,`material_id`),
  KEY `idx_sf_inventory_stocktake_line_material` (`tenant_id`,`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存盘点单明细';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_inventory_stocktake_order` (
  `stocktake_order_id` bigint NOT NULL COMMENT '盘点单主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `order_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '盘点单号',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'COMPLETED',
  `business_date` date NOT NULL COMMENT '业务日期',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`stocktake_order_id`),
  UNIQUE KEY `uk_sf_inventory_stocktake_no` (`tenant_id`,`order_no`),
  KEY `idx_sf_inventory_stocktake_page` (`tenant_id`,`business_date`,`stocktake_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存盘点单';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_market_product` (
  `product_id` bigint NOT NULL COMMENT '商品主键',
  `product_name` varchar(128) NOT NULL COMMENT '平台商品名称',
  `normalized_name` varchar(128) NOT NULL COMMENT '用于自动匹配的标准化名称',
  `category` varchar(32) NOT NULL COMMENT 'grain/vegetable/fruit/livestock/aquatic',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示排序，升序',
  `enabled_flag` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`product_id`),
  UNIQUE KEY `uk_sf_market_product_name` (`category`,`normalized_name`),
  KEY `idx_sf_market_product_sort` (`category`,`sort_order`,`product_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农业行情平台商品';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_market_product_mapping` (
  `mapping_id` bigint NOT NULL COMMENT '来源商品映射主键',
  `source_code` varchar(64) NOT NULL COMMENT '来源编码',
  `external_product_key` varchar(191) NOT NULL COMMENT '来源内稳定商品标识',
  `product_id` bigint NOT NULL COMMENT '平台商品ID',
  `last_source_name` varchar(128) NOT NULL COMMENT '最近来源商品名称',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`mapping_id`),
  UNIQUE KEY `uk_sf_market_product_mapping` (`source_code`,`external_product_key`),
  KEY `idx_sf_market_product_mapping_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农业行情来源商品映射';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_market_quote` (
  `quote_id` bigint NOT NULL COMMENT '正式报价主键',
  `latest_ingest_id` bigint NOT NULL COMMENT '最近处理的暂存记录ID',
  `product_id` bigint NOT NULL COMMENT '平台商品ID',
  `source_code` varchar(64) NOT NULL COMMENT '来源编码',
  `external_product_key` varchar(191) NOT NULL COMMENT '来源内商品标识',
  `series_key_sha256` char(64) NOT NULL COMMENT '价格序列SHA-256',
  `specification` varchar(128) DEFAULT NULL COMMENT '规格或等级',
  `quote_type` varchar(32) NOT NULL COMMENT 'MARKET/OFFICIAL_AVERAGE',
  `source_price` decimal(20,6) NOT NULL COMMENT '来源原始价格',
  `source_unit` varchar(32) NOT NULL COMMENT '来源原始单位',
  `price` decimal(20,6) NOT NULL COMMENT '平台标准价格',
  `unit` varchar(32) NOT NULL COMMENT '平台标准单位',
  `origin` varchar(128) DEFAULT NULL COMMENT '产地或地区',
  `market_name` varchar(191) DEFAULT NULL COMMENT '报价市场',
  `quote_date` date NOT NULL COMMENT '行情业务日期',
  `collected_at` datetime(3) DEFAULT NULL COMMENT '采集时间',
  `source_url` varchar(2048) NOT NULL COMMENT '来源地址',
  `previous_price` decimal(20,6) DEFAULT NULL COMMENT '上一有效报价',
  `change_amount` decimal(20,6) DEFAULT NULL COMMENT '涨跌额',
  `change_percent` decimal(12,4) DEFAULT NULL COMMENT '涨跌幅，0.7表示0.7%',
  `trend` varchar(16) NOT NULL DEFAULT 'unknown' COMMENT 'up/down/flat/unknown',
  `warnings_json` json DEFAULT NULL COMMENT '报价告警数组',
  `publish_status` varchar(16) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/OFFLINE',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`quote_id`),
  UNIQUE KEY `uk_sf_market_quote_series_date` (`series_key_sha256`,`quote_date`),
  KEY `idx_sf_market_quote_latest` (`publish_status`,`series_key_sha256`,`quote_date`),
  KEY `idx_sf_market_quote_product` (`product_id`,`quote_date`),
  KEY `idx_sf_market_quote_source` (`source_code`,`quote_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农业行情正式报价';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_market_quote_ingest_staging` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '暂存主键',
  `request_id` varchar(64) NOT NULL COMMENT '爬虫逻辑提交唯一ID',
  `crawl_run_id` varchar(64) NOT NULL COMMENT '采集批次',
  `source_code` varchar(64) NOT NULL COMMENT '来源编码',
  `external_product_key` varchar(191) NOT NULL COMMENT '来源内稳定商品标识',
  `product_name` varchar(128) NOT NULL COMMENT '来源商品名称',
  `specification` varchar(128) DEFAULT NULL COMMENT '来源规格或等级',
  `source_category` varchar(128) DEFAULT NULL COMMENT '来源原始品类',
  `quote_type` varchar(32) NOT NULL COMMENT 'MARKET/OFFICIAL_AVERAGE',
  `price` decimal(20,6) NOT NULL COMMENT '来源报价数值',
  `unit` varchar(32) NOT NULL COMMENT '来源计价单位',
  `origin` varchar(128) DEFAULT NULL COMMENT '来源明确给出的产地或地区',
  `market_name` varchar(191) DEFAULT NULL COMMENT '报价市场名称',
  `quote_date` date NOT NULL COMMENT '行情业务日期',
  `fetched_at_utc` datetime(3) NOT NULL COMMENT 'UTC采集时间',
  `source_url` varchar(2048) NOT NULL COMMENT '来源页面或授权接口地址',
  `raw_price_text` varchar(512) NOT NULL COMMENT '报价对应原始文本',
  `raw_payload_json` json NOT NULL COMMENT '原始结构化载荷',
  `payload_sha256` char(64) NOT NULL COMMENT '载荷SHA-256',
  `ingest_status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/ACCEPTED/UNCHANGED/REJECTED/FAILED/IGNORED',
  `result_action` varchar(32) DEFAULT NULL COMMENT 'CREATED/UPDATED/UNCHANGED/IGNORED',
  `process_attempts` int NOT NULL DEFAULT '0' COMMENT '处理次数',
  `warnings_json` json DEFAULT NULL COMMENT '处理告警数组',
  `correction_json` json DEFAULT NULL COMMENT '运营修正字段，不覆盖原始值',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误说明',
  `product_id` bigint DEFAULT NULL COMMENT '归一后的平台商品ID',
  `quote_id` bigint DEFAULT NULL COMMENT '正式报价ID',
  `handled_by` bigint DEFAULT NULL COMMENT '最近处理人ID',
  `handler_name` varchar(100) DEFAULT NULL COMMENT '最近处理人名称快照',
  `handle_comment` varchar(500) DEFAULT NULL COMMENT '处理说明',
  `handled_at` datetime DEFAULT NULL COMMENT '人工处理时间',
  `warning_acknowledged` tinyint(1) NOT NULL DEFAULT '0' COMMENT '告警是否已确认',
  `received_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '入库时间',
  `processing_started_at` datetime(3) DEFAULT NULL COMMENT '处理开始时间',
  `processed_at` datetime(3) DEFAULT NULL COMMENT '处理完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sf_market_ingest_request` (`source_code`,`request_id`),
  KEY `idx_sf_market_ingest_status` (`ingest_status`,`received_at`),
  KEY `idx_sf_market_ingest_run` (`crawl_run_id`,`source_code`),
  KEY `idx_sf_market_ingest_business` (`source_code`,`external_product_key`,`quote_date`)
) ENGINE=InnoDB AUTO_INCREMENT=219 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农业行情采集暂存表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_market_source` (
  `source_code` varchar(64) NOT NULL COMMENT '分配给爬虫的来源编码',
  `source_name` varchar(100) NOT NULL COMMENT '前台展示来源',
  `allowed_domains_json` json NOT NULL COMMENT '允许来源域名数组',
  `authorization_note` varchar(500) DEFAULT NULL COMMENT '授权、robots或开放数据依据',
  `enabled_flag` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许入库',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`source_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农业行情来源白名单';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_media_asset` (
  `media_id` bigint NOT NULL COMMENT '媒体ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `device_id` bigint DEFAULT NULL COMMENT '来源设备',
  `field_id` bigint DEFAULT NULL COMMENT '关联地块',
  `batch_id` bigint DEFAULT NULL COMMENT '关联种植批次',
  `media_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'VIDEO/IMAGE',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'CAMERA/UAV/OTHER',
  `uav_job_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '飞控任务 job_id',
  `uav_file_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '飞控媒体 file_id',
  `oss_id` bigint DEFAULT NULL COMMENT '关联 sys_oss.oss_id',
  `file_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '访问地址冗余',
  `cover_oss_id` bigint DEFAULT NULL COMMENT '封面图 OSS',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间（视频）',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间（视频）',
  `retain_until` date DEFAULT NULL COMMENT '计划保留到期日',
  `meta_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '分辨率、时长、抽帧间隔等元数据',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`media_id`) USING BTREE,
  UNIQUE KEY `uk_sf_media_uav_file` (`tenant_id`,`uav_job_id`,`uav_file_id`) USING BTREE,
  KEY `idx_sf_media_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_sf_media_device` (`device_id`) USING BTREE,
  KEY `idx_sf_media_field` (`field_id`) USING BTREE,
  KEY `idx_sf_media_batch` (`batch_id`) USING BTREE,
  KEY `idx_sf_media_retain` (`retain_until`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='媒体资产表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_news_article` (
  `article_id` bigint NOT NULL COMMENT '文章主键',
  `source_code` varchar(64) NOT NULL COMMENT '来源编码',
  `external_article_key` varchar(191) NOT NULL COMMENT '来源内稳定文章标识',
  `canonical_url` varchar(2048) NOT NULL COMMENT '规范URL',
  `origin_url` varchar(2048) NOT NULL COMMENT '最近原文URL',
  `latest_source_sha256` char(64) NOT NULL COMMENT '最近一次来源正文摘要，用于避免人工编辑后重复采集',
  `current_revision_id` bigint DEFAULT NULL COMMENT '当前待处理版本',
  `published_revision_id` bigint DEFAULT NULL COMMENT '当前线上版本',
  `article_status` varchar(16) NOT NULL DEFAULT 'REVIEWING' COMMENT 'REVIEWING/REJECTED/PUBLISHED/OFFLINE',
  `recommend_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否推荐',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '推荐排序，升序',
  `read_count` bigint NOT NULL DEFAULT '0' COMMENT '阅读数',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`article_id`),
  UNIQUE KEY `uk_sf_news_article_external` (`source_code`,`external_article_key`),
  UNIQUE KEY `uk_sf_news_article_url` (`source_code`,`canonical_url`(512)),
  KEY `idx_sf_news_article_publish` (`article_status`,`recommend_flag`,`publish_time`),
  KEY `idx_sf_news_article_revision` (`published_revision_id`,`current_revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农业资讯文章身份表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_news_ingest_staging` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `crawl_run_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `external_article_key` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_article_id` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `origin_url` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `canonical_url` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `fetched_at_utc` datetime(3) NOT NULL,
  `payload_schema_version` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `payload_json` json NOT NULL,
  `extracted_html` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `raw_html` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_sha256` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `raw_sha256` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `payload_sha256` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `ingest_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `result_action` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `process_attempts` int unsigned NOT NULL DEFAULT '0',
  `warnings_json` json DEFAULT NULL,
  `error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `error_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `article_id` bigint unsigned DEFAULT NULL,
  `revision_id` bigint unsigned DEFAULT NULL,
  `received_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `processing_started_at` datetime(3) DEFAULT NULL,
  `processed_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_news_ingest_request` (`source_code`,`request_id`),
  KEY `idx_news_ingest_run` (`crawl_run_id`),
  KEY `idx_news_ingest_status` (`ingest_status`,`received_at`),
  KEY `idx_news_ingest_article` (`source_code`,`external_article_key`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='农业资讯采集暂存表，保存等待平台处理的候选文章';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_news_media_transfer` (
  `task_id` bigint NOT NULL COMMENT '任务主键',
  `revision_id` bigint NOT NULL COMMENT '文章版本主键',
  `media_type` varchar(16) NOT NULL COMMENT 'IMAGE/VIDEO/POSTER',
  `source_url` varchar(2048) NOT NULL COMMENT '原始媒体地址',
  `source_url_sha256` char(64) NOT NULL COMMENT '原始地址SHA-256',
  `transfer_status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/SUCCESS/FAILED/CANCELLED',
  `attempt_count` int NOT NULL DEFAULT '0' COMMENT '处理次数',
  `next_retry_at` datetime DEFAULT NULL COMMENT '下次重试时间',
  `oss_id` bigint DEFAULT NULL COMMENT '平台OSS主键',
  `oss_url` varchar(2048) DEFAULT NULL COMMENT '平台OSS地址',
  `content_type` varchar(100) DEFAULT NULL COMMENT '实际媒体MIME',
  `file_size` bigint DEFAULT NULL COMMENT '文件字节数',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误说明',
  `processing_started_at` datetime DEFAULT NULL COMMENT '处理开始时间',
  `completed_at` datetime DEFAULT NULL COMMENT '处理完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_sf_news_media_revision_url` (`revision_id`,`media_type`,`source_url_sha256`),
  KEY `idx_sf_news_media_pending` (`transfer_status`,`next_retry_at`,`create_time`),
  KEY `idx_sf_news_media_revision` (`revision_id`,`transfer_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农业资讯媒体自动转存任务';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_news_review_log` (
  `log_id` bigint NOT NULL COMMENT '日志主键',
  `article_id` bigint NOT NULL COMMENT '文章主键',
  `revision_id` bigint DEFAULT NULL COMMENT '版本主键',
  `action` varchar(16) NOT NULL COMMENT 'EDIT/APPROVE/REJECT/OFFLINE',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(100) NOT NULL COMMENT '操作人名称快照',
  `comment` varchar(500) DEFAULT NULL COMMENT '操作说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_sf_news_review_article` (`article_id`,`create_time`),
  KEY `idx_sf_news_review_revision` (`revision_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农业资讯审核操作日志';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_news_revision` (
  `revision_id` bigint NOT NULL COMMENT '版本主键',
  `article_id` bigint NOT NULL COMMENT '文章主键',
  `revision_no` int NOT NULL COMMENT '文章版本号',
  `source_request_id` varchar(64) DEFAULT NULL COMMENT '来源暂存请求ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `summary` varchar(500) DEFAULT NULL COMMENT '摘要',
  `category` varchar(32) NOT NULL DEFAULT 'general' COMMENT 'policy/knowledge/market/general',
  `source_name` varchar(100) NOT NULL COMMENT '展示来源',
  `author` varchar(100) DEFAULT NULL COMMENT '作者',
  `origin_published_at` datetime DEFAULT NULL COMMENT '原文发布时间',
  `origin_url` varchar(2048) NOT NULL COMMENT '原文URL',
  `cover_url` varchar(2048) DEFAULT NULL COMMENT '封面OSS地址',
  `tags_json` json DEFAULT NULL COMMENT '标签数组',
  `sanitized_html` mediumtext NOT NULL COMMENT '清洗后后台预览HTML',
  `content_blocks_json` json NOT NULL COMMENT '移动端结构化正文',
  `content_sha256` char(64) NOT NULL COMMENT '版本正文摘要',
  `media_status` varchar(16) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/TRANSFERRING/READY/FAILED',
  `media_total` int NOT NULL DEFAULT '0' COMMENT '媒体总数',
  `media_succeeded` int NOT NULL DEFAULT '0' COMMENT '媒体转存成功数',
  `media_failed` int NOT NULL DEFAULT '0' COMMENT '媒体转存失败数',
  `review_status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `reviewer_name` varchar(100) DEFAULT NULL COMMENT '审核人名称快照',
  `review_comment` varchar(500) DEFAULT NULL COMMENT '审核意见',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`revision_id`),
  UNIQUE KEY `uk_sf_news_revision_no` (`article_id`,`revision_no`),
  UNIQUE KEY `uk_sf_news_revision_request` (`source_request_id`),
  KEY `idx_sf_news_revision_review` (`review_status`,`create_time`),
  KEY `idx_sf_news_revision_article` (`article_id`,`revision_id`),
  KEY `idx_sf_news_revision_media` (`media_status`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农业资讯不可覆盖版本表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_news_source` (
  `source_id` bigint NOT NULL COMMENT '来源主键',
  `source_code` varchar(64) NOT NULL COMMENT '分配给爬虫的来源编码',
  `source_name` varchar(100) NOT NULL COMMENT '前台展示来源',
  `allowed_domains_json` json NOT NULL COMMENT '允许采集的域名数组',
  `authorization_note` varchar(500) DEFAULT NULL COMMENT '授权、robots或开放数据依据说明',
  `enabled_flag` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许入库',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`source_id`),
  UNIQUE KEY `uk_sf_news_source_code` (`source_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农业资讯来源白名单';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_planting_batch` (
  `batch_id` bigint NOT NULL COMMENT '批次ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `field_id` bigint NOT NULL COMMENT '地块ID',
  `variety_id` bigint NOT NULL COMMENT '品种ID',
  `batch_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '批次编号（展示用，租户内唯一可选）',
  `cropping_index` int NOT NULL DEFAULT '1' COMMENT '当年第几茬',
  `sowing_date` date DEFAULT NULL COMMENT '定植/播种日期',
  `expected_harvest_date` date DEFAULT NULL COMMENT '预计采收日期',
  `actual_harvest_date` date DEFAULT NULL COMMENT '实际采收日期',
  `batch_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PLANNING' COMMENT 'PLANNING/PLANTING/GROWING/HARVESTING/FINISHED/FAILED',
  `status_time` datetime DEFAULT NULL COMMENT '当前状态变更时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`batch_id`) USING BTREE,
  UNIQUE KEY `uk_sf_batch_tenant_code` (`tenant_id`,`batch_code`) USING BTREE,
  KEY `idx_sf_batch_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_sf_batch_field` (`field_id`) USING BTREE,
  KEY `idx_sf_batch_variety` (`variety_id`) USING BTREE,
  KEY `idx_sf_batch_status` (`batch_status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='种植批次表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_planting_batch_log` (
  `log_id` bigint NOT NULL COMMENT '日志ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `batch_id` bigint NOT NULL COMMENT '批次ID',
  `from_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原状态',
  `to_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '新状态',
  `operate_by` bigint DEFAULT NULL COMMENT '操作人用户ID',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`log_id`) USING BTREE,
  KEY `idx_sf_batch_log_batch` (`batch_id`) USING BTREE,
  KEY `idx_sf_batch_log_time` (`operate_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='种植批次状态变更日志';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_satellite_schedule` (
  `schedule_id` bigint NOT NULL COMMENT '计划主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000',
  `planting_batch_id` bigint NOT NULL COMMENT '关联种植批次',
  `field_id` bigint DEFAULT NULL COMMENT '关联地块',
  `dk_geom` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'GeoJSON 地块边界',
  `code_croptype` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '作物类型码',
  `task_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务类型(growth/soilmoisture等)',
  `pixel_image` int DEFAULT '10',
  `detect_start` date NOT NULL COMMENT '检测开始日期',
  `detect_end` date NOT NULL COMMENT '检测结束日期',
  `cycle_days` int NOT NULL COMMENT '执行周期(天)',
  `next_run_date` date NOT NULL COMMENT '下次执行日期',
  `schedule_status` tinyint DEFAULT '1' COMMENT '1=活跃 0=暂停 2=已完成',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0',
  `field_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：地块名称',
  `species_id` bigint DEFAULT NULL COMMENT '冗余：物种ID',
  `species_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：物种名称',
  `variety_id` bigint DEFAULT NULL COMMENT '冗余：品种ID',
  `variety_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：品种名称',
  `planting_batch_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：种植批次展示名',
  PRIMARY KEY (`schedule_id`) USING BTREE,
  KEY `idx_status_next` (`schedule_status`,`next_run_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='卫星遥感周期计划';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_satellite_task` (
  `task_id` bigint NOT NULL COMMENT '主键；任务表内部 ID（MyBatis-Plus ASSIGN_ID）',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号；多租户数据隔离',
  `field_id` bigint DEFAULT NULL COMMENT '可选；关联业务地块 sf_field.field_id，便于按地块查询',
  `planting_batch_id` bigint DEFAULT NULL COMMENT '创建时地块进行中种植批次ID',
  `planting_batch_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：种植批次展示名（创建时快照，对应 batch_code）',
  `field_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：地块名称（创建时快照）',
  `species_id` bigint DEFAULT NULL COMMENT '冗余：物种ID',
  `species_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：物种名称',
  `variety_id` bigint DEFAULT NULL COMMENT '冗余：品种ID',
  `variety_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：品种名称',
  `dk_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '遥感地块业务唯一标识（对外）；与提交遥感 HTTP、回调 JSON 中 dk_id 一致，库内唯一',
  `dk_geom` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '地块几何；GeoJSON 字符串，提交遥感服务时使用',
  `code_croptype` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '作物类型编码；等于 sf_crop_variety.variety_code（租户内品种编号）',
  `start_date` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '监测/任务起始日期；格式 YYYY-MM-DD，与遥感接口约定一致',
  `end_date` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '监测/任务结束日期；格式 YYYY-MM-DD',
  `task_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '遥感任务类型；如土壤墒情 soilmoisture 等，与外部服务枚举一致',
  `pixel_image` int DEFAULT '10' COMMENT '影像像素/分辨率相关参数；默认 10，随请求提交遥感服务',
  `status` int NOT NULL DEFAULT '0' COMMENT '任务状态：0 待提交，1 已提交待处理，2 处理中，3 成功，4 失败',
  `message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '说明信息；外部返回摘要、失败原因或内部备注',
  `submit_count` int DEFAULT '0' COMMENT '已向外部遥感服务发起提交的次数（含重试）',
  `last_submit_time` datetime DEFAULT NULL COMMENT '最近一次成功或尝试调用外部遥感提交接口的时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门 ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建人用户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人用户 ID',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志：0 正常，1 已删除（逻辑删除）',
  `schedule_id` bigint DEFAULT NULL COMMENT '关联周期计划 schedule_id，单次任务为 null',
  PRIMARY KEY (`task_id`) USING BTREE,
  UNIQUE KEY `uk_sf_satellite_dk` (`dk_id`) USING BTREE,
  KEY `idx_sf_satellite_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_sf_satellite_field` (`field_id`) USING BTREE,
  KEY `idx_sf_satellite_status` (`status`) USING BTREE,
  KEY `idx_sst_schedule_id` (`schedule_id`) USING BTREE,
  KEY `idx_sf_satellite_task_health` (`tenant_id`,`field_id`,`planting_batch_id`,`del_flag`,`dk_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='遥感任务主表：创建、定时提交外部服务、接收回调更新状态';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_satellite_task_result` (
  `result_id` bigint NOT NULL COMMENT '主键；单条回调结果记录 ID（MyBatis-Plus ASSIGN_ID）',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号；与任务表一致，多租户隔离',
  `dk_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '关联 sf_satellite_task.dk_id；标识本次结果所属遥感地块任务',
  `planting_batch_id` bigint DEFAULT NULL COMMENT '回调落库时与主任务快照一致',
  `task_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '遥感任务类型；与请求/回调中 task_type 一致',
  `dk_bounds` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '地块范围；回调 JSON 序列化存储（含 west/south/east/north、center_lon、center_lat 等）',
  `break_value` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '分级断点值；回调中为二维数组，存 JSON 字符串',
  `area` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '各级面积等；回调中为一维数组，存 JSON 字符串',
  `object_key1` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'OSS 对象键 1；成果文件在对象存储中的 key',
  `object_key2` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'OSS 对象键 2；第二路成果或附加文件 key',
  `image_pixel` int DEFAULT NULL COMMENT '影像像素/分辨率参数；与回调 image_pixel 一致',
  `success` tinyint(1) DEFAULT NULL COMMENT '本条回调结果是否成功：1 成功，0 失败（与回调 success 一致）',
  `image_date` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '影像日期或成果日期；回调透传字符串',
  `bucket` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'OSS 存储桶名称；与 object_key 组合定位文件',
  `oss_url` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成果访问 URL；可由配置前缀 + key 拼接或直接存回调/生成地址',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门 ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建人用户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间（通常为回调落库时间）',
  `update_by` bigint DEFAULT NULL COMMENT '更新人用户 ID',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志：0 正常，1 已删除（逻辑删除）',
  PRIMARY KEY (`result_id`) USING BTREE,
  KEY `idx_sf_sat_result_dk` (`dk_id`) USING BTREE,
  KEY `idx_sf_sat_result_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_sf_satellite_result_health` (`tenant_id`,`planting_batch_id`,`success`,`del_flag`,`image_date`,`task_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='遥感任务回调结果表：外部服务异步回调一条写入一条';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_solar_term_definition` (
  `term_code` varchar(32) NOT NULL COMMENT '节气编码，例如 lichun',
  `term_name` varchar(16) NOT NULL COMMENT '节气名称',
  `term_order` int NOT NULL COMMENT '24节气顺序，立春=1',
  `solar_longitude` int DEFAULT NULL COMMENT '太阳黄经（度）',
  `intro` varchar(1000) NOT NULL COMMENT '节气简介',
  `seasonal_description` varchar(1000) NOT NULL COMMENT '时令特点',
  `customs_json` json DEFAULT NULL COMMENT '传统习俗数组 JSON',
  `content_version` varchar(32) NOT NULL DEFAULT 'v1' COMMENT '内容版本',
  `enabled_flag` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`term_code`),
  UNIQUE KEY `uk_sf_solar_term_order` (`term_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='二十四节气定义内容';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_solar_term_occurrence` (
  `occurrence_id` bigint NOT NULL COMMENT '主键',
  `term_year` int NOT NULL COMMENT '公历年份（按交节所在公历年）',
  `term_code` varchar(32) NOT NULL COMMENT '节气编码',
  `term_name` varchar(16) NOT NULL COMMENT '节气名称',
  `occurred_at` datetime NOT NULL COMMENT '北京时间交节时间',
  `gregorian_date` date NOT NULL COMMENT '公历日期',
  `lunar_date_text` varchar(64) DEFAULT NULL COMMENT '农历日期文本',
  `algorithm_version` varchar(32) NOT NULL DEFAULT 'lunar-1.7.7' COMMENT '算法版本',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`occurrence_id`),
  UNIQUE KEY `uk_sf_solar_term_year_code` (`term_year`,`term_code`),
  KEY `idx_sf_solar_term_occurred` (`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='具体年份节气交节时间';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_acceptance` (
  `acceptance_id` bigint NOT NULL COMMENT '验收记录主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `order_id` bigint NOT NULL COMMENT '工单ID',
  `acceptor_employee_id` bigint NOT NULL COMMENT '验收人员工ID',
  `acceptor_role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '验收人角色编码',
  `result` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '验收结果：PASS/REJECT',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '不合格原因',
  `acceptance_photos` json DEFAULT NULL COMMENT '验收照片，JSON数组，元素为OSS文件ID或文件对象',
  `accepted_at` datetime NOT NULL COMMENT '验收时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`acceptance_id`) USING BTREE,
  KEY `idx_sf_stask_acceptance_order` (`tenant_id`,`order_id`,`accepted_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask验收记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_clock_location` (
  `clock_location_id` bigint NOT NULL COMMENT '打卡地点主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `location_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '地点名称',
  `center_lng` decimal(12,8) NOT NULL COMMENT '中心点经度，CGCS2000',
  `center_lat` decimal(12,8) NOT NULL COMMENT '中心点纬度，CGCS2000',
  `coordinate_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CGCS2000' COMMENT '坐标系类型，固定 CGCS2000',
  `radius_meters` int NOT NULL DEFAULT '200' COMMENT '允许打卡半径，单位：米',
  `enabled` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '是否启用围栏：0-关闭 1-开启',
  `map_provider` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'tianditu' COMMENT '地图服务商',
  `map_zoom_level` int DEFAULT NULL COMMENT '天地图缩放级别',
  `address_text` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地址描述',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`clock_location_id`) USING BTREE,
  UNIQUE KEY `uk_sf_stask_clock_location_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask任务打卡地点表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_clock_record` (
  `clock_id` bigint NOT NULL COMMENT '打卡记录主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `order_id` bigint NOT NULL COMMENT '工单ID',
  `leader_id` bigint NOT NULL COMMENT '组长员工ID',
  `clock_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '打卡类型：GPS/PHOTO',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '纬度',
  `proof_photos` json DEFAULT NULL COMMENT '证明照片，JSON数组，元素为OSS文件ID或文件对象',
  `clock_time` datetime NOT NULL COMMENT '打卡时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `distance_meters` int DEFAULT NULL COMMENT '与租户打卡地点中心点距离，单位：米',
  PRIMARY KEY (`clock_id`) USING BTREE,
  KEY `idx_sf_stask_clock_order` (`tenant_id`,`order_id`,`clock_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask到岗打卡记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_completion` (
  `completion_id` bigint NOT NULL COMMENT '完工记录主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `order_id` bigint NOT NULL COMMENT '工单ID',
  `leader_id` bigint NOT NULL COMMENT '组长员工ID',
  `work_photos` json NOT NULL COMMENT '作业照片，JSON数组，元素为OSS文件ID或文件对象',
  `completion_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '完工备注',
  `completed_at` datetime NOT NULL COMMENT '完工提交时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`completion_id`) USING BTREE,
  KEY `idx_sf_stask_completion_order` (`tenant_id`,`order_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask完工记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_dispatch` (
  `dispatch_id` bigint NOT NULL COMMENT '派工明细主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `order_id` bigint NOT NULL COMMENT '工单ID',
  `leader_id` bigint NOT NULL COMMENT '组长员工ID',
  `worker_id` bigint NOT NULL COMMENT '工人员工ID',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '派工状态：PENDING/ACCEPTED/REJECTED/CANCELLED',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '拒绝原因',
  `leader_evaluation` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组长评价',
  `invited_at` datetime NOT NULL COMMENT '邀请时间',
  `responded_at` datetime DEFAULT NULL COMMENT '响应时间',
  `cancelled_at` datetime DEFAULT NULL COMMENT '撤销时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `invite_sms_sent_at` datetime DEFAULT NULL COMMENT '邀请短信发送时间',
  PRIMARY KEY (`dispatch_id`) USING BTREE,
  KEY `idx_sf_stask_dispatch_order` (`tenant_id`,`order_id`,`status`) USING BTREE,
  KEY `idx_sf_stask_dispatch_worker` (`tenant_id`,`worker_id`,`status`) USING BTREE,
  KEY `idx_stask_dispatch_invite_sms` (`tenant_id`,`worker_id`,`status`,`invite_sms_sent_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask派工明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_flow_log` (
  `log_id` bigint NOT NULL COMMENT '流转日志主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `order_id` bigint NOT NULL COMMENT '工单ID',
  `package_id` bigint DEFAULT NULL COMMENT '任务包ID',
  `from_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流转前状态',
  `to_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流转后状态',
  `event` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流转事件',
  `operator_employee_id` bigint DEFAULT NULL COMMENT '操作人员工ID',
  `operator_role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人角色编码',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流转说明',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`log_id`) USING BTREE,
  KEY `idx_sf_stask_flow_order` (`tenant_id`,`order_id`,`create_time`) USING BTREE,
  KEY `idx_sf_stask_flow_package` (`tenant_id`,`package_id`,`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask工单流转日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_inspection` (
  `inspection_id` bigint NOT NULL COMMENT '抽检主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `found_date` date NOT NULL COMMENT '问题发现日期',
  `severity` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '严重程度 NORMAL/SERIOUS',
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'UNPROCESSED' COMMENT '状态 UNPROCESSED/PROCESSED',
  `problem_description` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '问题描述',
  `problem_photo_json` json DEFAULT NULL COMMENT '问题照片 JSON',
  `greenhouse_id` bigint NOT NULL COMMENT '大棚 ID',
  `greenhouse_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '大棚名称快照',
  `planting_batch_id_snapshot` bigint DEFAULT NULL COMMENT '种植批次快照',
  `crop_id_snapshot` bigint DEFAULT NULL COMMENT '物种 ID 快照',
  `crop_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物种名称快照',
  `crop_variety_id_snapshot` bigint DEFAULT NULL COMMENT '品种 ID 快照',
  `crop_variety_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '品种名称快照',
  `order_id` bigint DEFAULT NULL COMMENT '关联拆分工单 ID',
  `work_item_id_snapshot` bigint DEFAULT NULL COMMENT '农事项 ID 快照',
  `work_item_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '农事项名称快照',
  `responsible_technician_employee_id` bigint NOT NULL COMMENT '负责技术员 ID',
  `responsible_technician_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '负责技术员名称快照',
  `responsible_leader_employee_id` bigint DEFAULT NULL COMMENT '负责组长 ID',
  `responsible_leader_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '负责组长名称快照',
  `discoverer_employee_id` bigint NOT NULL COMMENT '发现人 ID',
  `discoverer_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '发现人名称快照',
  `creator_employee_id` bigint NOT NULL COMMENT '创建人 ID',
  `creator_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建人名称快照',
  `first_handle_description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '首次处理说明',
  `first_handle_photo_json` json DEFAULT NULL COMMENT '首次处理照片 JSON',
  `first_handle_employee_id` bigint DEFAULT NULL COMMENT '首次处理人 ID',
  `first_handle_employee_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '首次处理人名称快照',
  `first_handled_at` datetime DEFAULT NULL COMMENT '首次处理时间',
  `current_handle_description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前处理说明',
  `current_handle_photo_json` json DEFAULT NULL COMMENT '当前处理照片 JSON',
  `current_handle_employee_id` bigint DEFAULT NULL COMMENT '当前处理人 ID',
  `current_handle_employee_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前处理人名称快照',
  `current_handled_at` datetime DEFAULT NULL COMMENT '当前处理时间',
  `latest_reject_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最新打回原因',
  `latest_reject_employee_id` bigint DEFAULT NULL COMMENT '最新打回人 ID',
  `latest_reject_employee_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最新打回人名称快照',
  `latest_rejected_at` datetime DEFAULT NULL COMMENT '最新打回时间',
  `handle_count` int NOT NULL DEFAULT '0' COMMENT '成功处理次数',
  `reject_count` int NOT NULL DEFAULT '0' COMMENT '成功打回次数',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人审计字段',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`inspection_id`) USING BTREE,
  KEY `idx_sf_stask_inspection_tenant_deleted_found` (`tenant_id`,`del_flag`,`found_date`,`inspection_id`) USING BTREE,
  KEY `idx_sf_stask_inspection_tenant_status` (`tenant_id`,`del_flag`,`status`,`inspection_id`) USING BTREE,
  KEY `idx_sf_stask_inspection_technician_status` (`tenant_id`,`del_flag`,`responsible_technician_employee_id`,`status`) USING BTREE,
  KEY `idx_sf_stask_inspection_leader_status` (`tenant_id`,`del_flag`,`responsible_leader_employee_id`,`status`) USING BTREE,
  KEY `idx_sf_stask_inspection_greenhouse` (`tenant_id`,`del_flag`,`greenhouse_id`) USING BTREE,
  KEY `idx_sf_stask_inspection_order` (`tenant_id`,`order_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask 农事抽检记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_leader_labor_record` (
  `labor_record_id` bigint NOT NULL COMMENT '日用工记录主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `leader_employee_id` bigint NOT NULL COMMENT '组长员工ID',
  `leader_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组长姓名快照',
  `plan_date` date NOT NULL COMMENT '任务计划日期',
  `labor_count` decimal(10,2) NOT NULL COMMENT '日用工人数，不含组长本人',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `last_source` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SINGLE_ACCEPT/BATCH_ACCEPT/MANUAL_EDIT',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`labor_record_id`) USING BTREE,
  UNIQUE KEY `uk_sf_stask_labor_tenant_leader_plan` (`tenant_id`,`leader_employee_id`,`plan_date`) USING BTREE,
  KEY `idx_sf_stask_labor_tenant_plan` (`tenant_id`,`plan_date`,`leader_employee_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask 组长计划日用工记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_material_allocation` (
  `allocation_id` bigint NOT NULL COMMENT '组长物料拆分主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `task_package_id` bigint NOT NULL COMMENT '任务包ID',
  `farm_item_id` bigint NOT NULL COMMENT '农事项ID',
  `leader_employee_id` bigint NOT NULL COMMENT '组长员工ID',
  `leader_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组长姓名快照',
  `greenhouse_count` int NOT NULL COMMENT '负责大棚数',
  `leader_order` int NOT NULL COMMENT '创建时名单顺序',
  `greenhouse_names_snapshot` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '大棚名称快照',
  `algorithm_version` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'V1',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`allocation_id`),
  UNIQUE KEY `uk_sf_stask_material_allocation` (`tenant_id`,`task_package_id`,`farm_item_id`,`leader_employee_id`),
  KEY `idx_sf_stask_material_allocation_package` (`tenant_id`,`task_package_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务物料组长拆分快照';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_material_allocation_line` (
  `allocation_line_id` bigint NOT NULL COMMENT '拆分物料行主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `allocation_id` bigint NOT NULL COMMENT '拆分聚合主键',
  `task_material_id` bigint NOT NULL COMMENT '任务物料快照主键',
  `material_id` bigint NOT NULL COMMENT '库存物资ID',
  `allocated_quantity` decimal(18,1) NOT NULL COMMENT '拆分数量',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`allocation_line_id`),
  UNIQUE KEY `uk_sf_stask_allocation_line_material` (`tenant_id`,`allocation_id`,`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务物料组长拆分明细';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_material_receipt` (
  `material_receipt_id` bigint NOT NULL COMMENT '领料单主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `receipt_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '领料单号',
  `task_package_id` bigint NOT NULL COMMENT '任务包ID',
  `farm_item_id` bigint NOT NULL COMMENT '农事项ID',
  `leader_employee_id` bigint NOT NULL COMMENT '组长员工ID',
  `leader_name_snapshot` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组长姓名快照',
  `task_name_snapshot` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务名称快照',
  `farm_work_name_snapshot` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '农事项名称快照',
  `greenhouse_names_snapshot` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '大棚名称快照',
  `status` varchar(24) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'PENDING_ACCEPTANCE/UNCLAIMED/PENDING_OUTBOUND/RECEIVED/VOIDED',
  `arrived` tinyint(1) NOT NULL DEFAULT '0' COMMENT '任一关联工单是否已到岗',
  `keeper_cancel_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近一次库管取消待出库理由',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`material_receipt_id`),
  UNIQUE KEY `uk_sf_stask_material_receipt_scope` (`tenant_id`,`task_package_id`,`farm_item_id`,`leader_employee_id`),
  UNIQUE KEY `uk_sf_stask_material_receipt_no` (`tenant_id`,`receipt_no`),
  KEY `idx_sf_stask_material_receipt_leader` (`tenant_id`,`leader_employee_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务领料单';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_material_receipt_line` (
  `material_receipt_line_id` bigint NOT NULL COMMENT '领料单行主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `material_receipt_id` bigint NOT NULL COMMENT '领料单主键',
  `material_id` bigint NOT NULL COMMENT '库存物资ID',
  `material_code_snapshot` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物资编码快照',
  `material_name_snapshot` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物资名称快照',
  `specification_snapshot` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格快照',
  `unit_snapshot` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '单位快照',
  `requested_quantity` decimal(18,1) NOT NULL COMMENT '申请数量',
  `actual_quantity` decimal(18,1) NOT NULL DEFAULT '0.0' COMMENT '累计实发数量',
  `returned_quantity` decimal(18,1) NOT NULL DEFAULT '0.0' COMMENT '累计已退数量',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '行版本',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`material_receipt_line_id`),
  UNIQUE KEY `uk_sf_stask_receipt_line_material` (`tenant_id`,`material_receipt_id`,`material_id`),
  KEY `idx_sf_stask_receipt_line_material` (`tenant_id`,`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务领料单明细';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_operation_idempotency` (
  `operation_id` bigint NOT NULL COMMENT '幂等操作主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `employee_id` bigint NOT NULL COMMENT '发起操作的员工ID',
  `operation_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'BATCH_ACCEPT/BATCH_CLOCK_IN',
  `idempotency_key` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '客户端幂等键',
  `request_hash` char(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '规范化请求SHA-256摘要',
  `operation_status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'PROCESSING/SUCCEEDED',
  `response_json` json DEFAULT NULL COMMENT '首次成功响应JSON',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`operation_id`) USING BTREE,
  UNIQUE KEY `uk_sf_stask_idempotency_scope` (`tenant_id`,`employee_id`,`operation_type`,`idempotency_key`) USING BTREE,
  KEY `idx_sf_stask_idempotency_status_time` (`operation_status`,`update_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask 批量操作持久化幂等记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_sop` (
  `sop_id` bigint NOT NULL COMMENT '农事SOP主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `work_item_id` bigint NOT NULL COMMENT '农事项目ID',
  `crop_scope` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '作物范围：ALL/SPECIFIC',
  `crop_species_id` bigint NOT NULL DEFAULT '0' COMMENT '作物物种ID；0表示全部作物',
  `language` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '内容语言：zh-CN/ug-CN',
  `content_blocks` json NOT NULL COMMENT '有序内容块JSON',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`sop_id`) USING BTREE,
  UNIQUE KEY `uk_sf_stask_sop_match` (`tenant_id`,`work_item_id`,`crop_species_id`,`language`) USING BTREE,
  KEY `idx_sf_stask_sop_page` (`tenant_id`,`work_item_id`,`crop_scope`,`crop_species_id`,`language`,`sop_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='农事标准作业规程';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_task_material` (
  `task_material_id` bigint NOT NULL COMMENT '任务物料快照主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `task_package_id` bigint NOT NULL COMMENT '任务包ID',
  `farm_item_id` bigint NOT NULL COMMENT '农事项ID',
  `material_id` bigint NOT NULL COMMENT '库存物资ID',
  `material_code_snapshot` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物资编码快照',
  `material_name_snapshot` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物资名称快照',
  `specification_snapshot` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格快照',
  `unit_snapshot` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '单位快照',
  `total_quantity` decimal(18,1) NOT NULL COMMENT '任务包农事项总量',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`task_material_id`),
  UNIQUE KEY `uk_sf_stask_task_material` (`tenant_id`,`task_package_id`,`farm_item_id`,`material_id`),
  KEY `idx_sf_stask_task_material_package` (`tenant_id`,`task_package_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务农事项物料总量快照';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_task_package` (
  `package_id` bigint NOT NULL COMMENT '任务包主键',
  `package_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务包编号',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `creator_employee_id` bigint NOT NULL COMMENT '创建人员工ID',
  `creator_role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建人角色编码',
  `handler_technician_employee_id` bigint DEFAULT NULL COMMENT '最终经手技术员员工ID，权限事实源',
  `handler_technician_employee_name_snapshot` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '经手技术员姓名快照，仅用于展示',
  `plan_date` date NOT NULL COMMENT '计划作业日期',
  `overall_tech_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '整体技术说明',
  `status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务包状态',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`package_id`) USING BTREE,
  KEY `idx_stask_task_package_tenant_creator_status` (`tenant_id`,`creator_employee_id`,`status`) USING BTREE,
  KEY `idx_stask_task_package_tenant_role_status` (`tenant_id`,`creator_role_code`,`status`) USING BTREE,
  KEY `idx_stask_task_package_plan_date` (`tenant_id`,`plan_date`) USING BTREE,
  KEY `idx_sf_stask_task_package_handler_status` (`tenant_id`,`handler_technician_employee_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask 任务包主表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_voice_broadcast` (
  `broadcast_id` bigint NOT NULL COMMENT '播报记录主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `order_id` bigint NOT NULL COMMENT '工单ID',
  `source_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '中文任务说明',
  `uyghur_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '维语任务说明',
  `text_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '中文说明SHA-256',
  `language` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ug' COMMENT '播报语言',
  `voice_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '讯飞音色',
  `oss_id` bigint DEFAULT NULL COMMENT '系统OSS文件ID',
  `audio_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '音频播放地址',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '生成状态：QUEUED/PROCESSING/SUCCESS/FAILED',
  `fail_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`broadcast_id`) USING BTREE,
  UNIQUE KEY `uk_stask_voice_order_text` (`tenant_id`,`order_id`,`language`,`text_hash`) USING BTREE,
  KEY `idx_stask_voice_order` (`tenant_id`,`order_id`,`status`) USING BTREE,
  KEY `idx_stask_voice_worker` (`status`,`retry_count`,`update_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='stask任务维语语音播报表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_work_order` (
  `order_id` bigint NOT NULL COMMENT '工单主键',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工单编号',
  `package_id` bigint NOT NULL COMMENT '任务包ID，同一批创建任务共享',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `creator_employee_id` bigint NOT NULL COMMENT '创建人员工ID',
  `creator_role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建人角色编码',
  `handler_technician_employee_id` bigint DEFAULT NULL COMMENT '从任务包继承的最终经手技术员员工ID',
  `handler_technician_employee_name_snapshot` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '从任务包继承的经手技术员姓名快照',
  `plan_date` date NOT NULL COMMENT '计划作业日期',
  `greenhouse_id` bigint DEFAULT NULL COMMENT '大棚ID，拆分后工单必填',
  `greenhouse_code_snapshot` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '大棚编码快照',
  `greenhouse_name_snapshot` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '大棚名称快照',
  `work_item_id` bigint DEFAULT NULL COMMENT '农事项目ID，拆分后工单必填',
  `work_item_name_snapshot` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '农事项目名称快照',
  `work_item_code_snapshot` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '农事项目编码快照',
  `category_id_snapshot` bigint DEFAULT NULL COMMENT '农事分类ID快照',
  `category_name_snapshot` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '农事分类名称快照',
  `manager_requirement` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '申请人作业要求',
  `manager_photos` json DEFAULT NULL COMMENT '申请人照片，JSON数组，元素为OSS文件ID或文件对象',
  `tech_instruction` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '技术员针对农事项说明',
  `tech_photos` json DEFAULT NULL COMMENT '技术员参考照片，JSON数组，元素为OSS文件ID或文件对象',
  `overall_tech_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '整体技术说明',
  `leader_id` bigint DEFAULT NULL COMMENT '组长员工ID',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工单状态',
  `required_worker_count` decimal(10,2) DEFAULT NULL COMMENT '组长接单填写的工人数量（不含组长），结算人工取数依据',
  `accepted_worker_count` int NOT NULL DEFAULT '0' COMMENT '已接受工人数',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`order_id`) USING BTREE,
  UNIQUE KEY `uk_sf_stask_work_order_no` (`tenant_id`,`order_no`) USING BTREE,
  KEY `idx_sf_stask_work_order_package` (`tenant_id`,`package_id`) USING BTREE,
  KEY `idx_sf_stask_work_order_status` (`tenant_id`,`status`,`plan_date`) USING BTREE,
  KEY `idx_sf_stask_work_order_leader` (`tenant_id`,`leader_id`,`status`) USING BTREE,
  KEY `idx_sf_stask_work_order_handler_status` (`tenant_id`,`handler_technician_employee_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask工单主表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_work_order_greenhouse` (
  `greenhouse_item_id` bigint NOT NULL COMMENT '任务包大棚明细ID',
  `package_id` bigint NOT NULL COMMENT '任务包ID',
  `order_id` bigint DEFAULT NULL COMMENT '拆分工单ID，严格分组拆单后多个大棚可归属同一工单',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `item_id` bigint DEFAULT NULL COMMENT '任务包农事项明细ID',
  `work_item_id` bigint DEFAULT NULL COMMENT '农事项目ID',
  `greenhouse_id` bigint NOT NULL COMMENT '大棚ID',
  `greenhouse_code_snapshot` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '大棚编码快照',
  `greenhouse_name_snapshot` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '大棚名称快照',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`greenhouse_item_id`) USING BTREE,
  KEY `idx_sf_stask_work_order_greenhouse_package` (`tenant_id`,`package_id`,`greenhouse_id`) USING BTREE,
  KEY `idx_sf_stask_work_order_greenhouse_item` (`tenant_id`,`package_id`,`item_id`) USING BTREE,
  KEY `idx_sf_stask_work_order_greenhouse_order` (`tenant_id`,`order_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask任务包大棚明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_work_order_item` (
  `item_id` bigint NOT NULL COMMENT '任务包农事项明细ID',
  `package_id` bigint NOT NULL COMMENT '任务包ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `work_item_id` bigint NOT NULL COMMENT '农事项目ID',
  `work_item_name_snapshot` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '农事项目名称快照',
  `work_item_code_snapshot` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '农事项目编码快照',
  `category_id_snapshot` bigint DEFAULT NULL COMMENT '农事分类ID快照',
  `category_name_snapshot` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '农事分类名称快照',
  `manager_requirement` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '申请人作业要求',
  `manager_photos` json DEFAULT NULL COMMENT '申请人照片，JSON数组，元素为OSS文件ID或文件对象',
  `tech_instruction` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '技术员针对农事项说明',
  `tech_photos` json DEFAULT NULL COMMENT '技术员参考照片，JSON数组，元素为OSS文件ID或文件对象',
  `leader_assign_mode` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'AUTO' COMMENT '组长分配模式：AUTO自动 MANUAL手动',
  `manual_leader_id` bigint DEFAULT NULL COMMENT '手动指派组长员工ID',
  `manual_leader_name_snapshot` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手动指派组长姓名快照',
  `leader_id_snapshot` bigint DEFAULT NULL COMMENT '最终有效组长员工ID快照',
  `leader_name_snapshot` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最终有效组长姓名快照',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '任务包内排序',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`item_id`) USING BTREE,
  KEY `idx_sf_stask_work_order_item_package` (`tenant_id`,`package_id`,`sort_order`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask任务包农事项明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_worker_skill` (
  `skill_id` bigint NOT NULL COMMENT '技能记录主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `employee_id` bigint NOT NULL COMMENT '员工ID，对应 sys_employee.employee_id',
  `work_item_id` bigint NOT NULL COMMENT '农事项目ID，对应 sf_farm_work_dict.dict_id',
  `crop_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ALL' COMMENT '作物类型编码，来源sf_crop_species.species_code，ALL表示全部作物',
  `skill_level` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '技能等级：ADVANCED-高级 MEDIUM-中级 JUNIOR-初级',
  `work_count` int NOT NULL DEFAULT '0' COMMENT '累计从事次数，系统统计字段',
  `last_work_date` date DEFAULT NULL COMMENT '最近一次作业日期，系统统计字段',
  `average_score` decimal(5,2) DEFAULT NULL COMMENT '平均得分预留字段，当前不计算',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`skill_id`) USING BTREE,
  UNIQUE KEY `uk_sf_stask_worker_skill_item_crop` (`tenant_id`,`employee_id`,`work_item_id`,`crop_type`) USING BTREE,
  KEY `idx_sf_stask_worker_skill_item` (`tenant_id`,`work_item_id`,`skill_level`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='stask工人农事技能表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_stask_yield_record` (
  `yield_id` bigint NOT NULL COMMENT '产量记录主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `harvest_date` date NOT NULL COMMENT '收获日期',
  `species_id` bigint NOT NULL COMMENT '物种主键',
  `variety_id` bigint NOT NULL COMMENT '品种主键',
  `species_name_snapshot` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '物种名称快照',
  `variety_name_snapshot` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '品种名称快照',
  `yield_kg` decimal(14,2) NOT NULL COMMENT '产量，固定单位公斤',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`yield_id`) USING BTREE,
  KEY `idx_sf_stask_yield_page` (`tenant_id`,`del_flag`,`harvest_date`,`variety_name_snapshot`,`yield_id`) USING BTREE,
  KEY `idx_sf_stask_yield_species` (`tenant_id`,`del_flag`,`species_id`,`harvest_date`) USING BTREE,
  KEY `idx_sf_stask_yield_variety` (`tenant_id`,`del_flag`,`variety_id`,`harvest_date`) USING BTREE,
  CONSTRAINT `chk_sf_stask_yield_positive` CHECK ((`yield_kg` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='农事产量记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_trace_batch` (
  `trace_batch_id` bigint NOT NULL COMMENT '溯源批次ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `planting_batch_id` bigint DEFAULT NULL COMMENT '关联种植批次 sf_planting_batch.batch_id，可为空',
  `field_id` bigint NOT NULL COMMENT '地块ID sf_field.field_id',
  `variety_id` bigint NOT NULL COMMENT '品种ID sf_crop_variety.variety_id',
  `trace_batch_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '溯源批次号，租户内唯一',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品名称，如精品甜瓜',
  `quality_grade` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '品质等级',
  `origin_text` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '产地展示文本',
  `producer_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '生产单位',
  `certification_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '绿色/有机/检测报告等认证资料JSON',
  `label_scope` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SINGLE_FRUIT' COMMENT '标签对象：SINGLE_FRUIT单果 BOX整箱',
  `planned_quantity` int NOT NULL DEFAULT '0' COMMENT '计划生成标签数量',
  `generated_quantity` int NOT NULL DEFAULT '0' COMMENT '已生成标签数量',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT草稿 GENERATED已生成 PUBLISHED已发布 DISABLED已停用',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志：0存在 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`trace_batch_id`) USING BTREE,
  UNIQUE KEY `uk_sf_trace_batch_tenant_no` (`tenant_id`,`trace_batch_no`) USING BTREE,
  KEY `idx_sf_trace_batch_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_sf_trace_batch_planting` (`planting_batch_id`) USING BTREE,
  KEY `idx_sf_trace_batch_field` (`field_id`) USING BTREE,
  KEY `idx_sf_trace_batch_variety` (`variety_id`) USING BTREE,
  KEY `idx_sf_trace_batch_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='瓜果溯源批次表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_trace_code` (
  `trace_code_id` bigint NOT NULL COMMENT '溯源码ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `trace_batch_id` bigint NOT NULL COMMENT '溯源批次ID sf_trace_batch.trace_batch_id',
  `trace_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '全局唯一溯源码',
  `seq_no` int NOT NULL COMMENT '批次内序号',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL正常 VOID作废',
  `first_scan_time` datetime DEFAULT NULL COMMENT '首次扫码时间',
  `scan_count` int NOT NULL DEFAULT '0' COMMENT '扫码次数',
  `last_scan_time` datetime DEFAULT NULL COMMENT '最近扫码时间',
  `last_print_time` datetime DEFAULT NULL COMMENT '最近打印/重打时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`trace_code_id`) USING BTREE,
  UNIQUE KEY `uk_sf_trace_code` (`trace_code`) USING BTREE,
  UNIQUE KEY `uk_sf_trace_code_batch_seq` (`trace_batch_id`,`seq_no`) USING BTREE,
  KEY `idx_sf_trace_code_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_sf_trace_code_batch` (`trace_batch_id`) USING BTREE,
  KEY `idx_sf_trace_code_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='瓜果溯源码表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_trace_scan_log` (
  `scan_log_id` bigint NOT NULL COMMENT '扫码日志ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号，由溯源码反查；未找到码时可为空',
  `trace_code_id` bigint DEFAULT NULL COMMENT '溯源码ID sf_trace_code.trace_code_id；未找到码时为空',
  `trace_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '溯源码快照',
  `scan_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '扫码时间',
  `scan_result` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '扫码结果：OK正常 VOID作废 NOT_FOUND未找到 DISABLED批次停用',
  `is_repeat` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否重复扫码：0否 1是',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '扫码端IP',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '扫码端User-Agent',
  `referer` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源页面',
  PRIMARY KEY (`scan_log_id`) USING BTREE,
  KEY `idx_sf_trace_scan_code` (`trace_code_id`,`scan_time`) USING BTREE,
  KEY `idx_sf_trace_scan_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_sf_trace_scan_time` (`scan_time`) USING BTREE,
  KEY `idx_sf_trace_scan_trace_code` (`trace_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='瓜果溯源扫码日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_uav_ai_task` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户ID',
  `field_id` bigint DEFAULT NULL COMMENT '地块ID',
  `planting_batch_id` bigint DEFAULT NULL COMMENT '提交时地块进行中种植批次ID',
  `planting_batch_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：种植批次展示名（创建时快照，对应 batch_code）',
  `flight_schedule_id` bigint DEFAULT NULL COMMENT 'sf_uav_flight_schedule.schedule_id',
  `field_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：地块名称（创建时快照）',
  `species_id` bigint DEFAULT NULL COMMENT '冗余：物种ID',
  `species_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：物种名称',
  `variety_id` bigint DEFAULT NULL COMMENT '冗余：品种ID',
  `variety_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：品种名称',
  `uav_job_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'UAV任务ID（jobId）',
  `alg_model_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '本次提交算法中台 modelNo',
  `alg_task_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '算法平台任务号（taskNo）',
  `model_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'AI model number used by this task',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态：SUBMITTED/FINISHED/FAILED等',
  `scheduled_stop_at` datetime DEFAULT NULL COMMENT '计划调用中台停止的时刻',
  `analysis_duration_ms` bigint DEFAULT NULL COMMENT '分析时长(毫秒)，由业务起止时间计算',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `infer_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'VIDEO' COMMENT '推理类型：VIDEO/IMAGE',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sf_uav_ai_task_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_sf_uav_ai_task_field` (`field_id`) USING BTREE,
  KEY `idx_sf_uav_ai_task_job` (`uav_job_id`) USING BTREE,
  KEY `idx_sf_uav_ai_task_alg` (`alg_task_no`) USING BTREE,
  KEY `idx_sf_uav_ai_task_scheduled_stop` (`scheduled_stop_at`,`status`) USING BTREE,
  KEY `idx_sf_uav_ai_task_model` (`tenant_id`,`uav_job_id`,`field_id`,`model_no`,`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='UAV AI 分析任务映射表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_uav_flight_plan` (
  `plan_id` bigint NOT NULL COMMENT '计划ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户ID',
  `planting_batch_id` bigint NOT NULL COMMENT '种植批次ID',
  `planting_batch_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '种植批次名称快照',
  `field_id` bigint NOT NULL COMMENT '地块ID',
  `field_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地块名称快照',
  `species_id` bigint DEFAULT NULL COMMENT '作物物种ID快照',
  `species_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作物物种名称快照',
  `variety_id` bigint DEFAULT NULL COMMENT '作物品种ID快照',
  `variety_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作物品种名称快照',
  `plan_name` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计划名称',
  `workspace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '无人机工作空间ID',
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '飞控用户ID',
  `source` int DEFAULT NULL COMMENT '飞控任务来源',
  `file_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '航线文件ID',
  `dock_sn` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '机场设备SN',
  `wayline_type` int DEFAULT NULL COMMENT '航线类型',
  `rth_altitude` int DEFAULT NULL COMMENT '返航高度',
  `out_of_control_action` int DEFAULT NULL COMMENT '失控动作',
  `min_battery_capacity` int DEFAULT NULL COMMENT '最低电池电量',
  `min_storage_capacity` int DEFAULT NULL COMMENT '最低存储容量',
  `exit_wayline_when_rc_lost` int DEFAULT NULL COMMENT '遥控失联时是否退出航线',
  `ai_model_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '首个AI模型编号',
  `ai_model_nos` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'AI模型编号列表，逗号分隔',
  `plan_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '计划模式：IMMEDIATE立即飞、SCHEDULED预定飞一次、REPEAT周期重复飞',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '计划状态：PLANNING计划中、PAUSED停用、COMPLETED已完成、FAILED执行失败',
  `execute_time` datetime DEFAULT NULL COMMENT '预定飞一次的执行时间',
  `expire_policy` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '过期策略：SKIP_EXPIRED过期后不执行',
  `repeat_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '重复模式：DAILY每天、WEEKLY每周、MONTHLY每月',
  `daily_times` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '每天起飞时间点，HH:mm逗号分隔',
  `weekdays` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '每周执行日，1-7逗号分隔，1为周一',
  `month_days` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '每月执行日期，1-31逗号分隔',
  `start_time` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '每周或每月统一起飞时间，HH:mm',
  `effective_start_at` datetime DEFAULT NULL COMMENT '周期任务生效开始时间',
  `effective_end_at` datetime DEFAULT NULL COMMENT '周期任务生效结束时间，空表示无限期',
  `next_occurrence_at` datetime DEFAULT NULL COMMENT '下一次触发时间',
  `last_error` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近一次错误信息',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`plan_id`) USING BTREE,
  KEY `idx_sf_uav_flight_plan_batch` (`tenant_id`,`planting_batch_id`,`status`) USING BTREE,
  KEY `idx_sf_uav_flight_plan_next` (`status`,`next_occurrence_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='无人机飞行计划';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_uav_flight_plan_execution` (
  `execution_id` bigint NOT NULL COMMENT '执行记录ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户ID',
  `plan_id` bigint NOT NULL COMMENT '计划ID',
  `occurrence_at` datetime NOT NULL COMMENT '计划触发时间',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行状态：PENDING待执行、SUCCESS成功、FAILED失败、SKIPPED已跳过',
  `flight_task_id` bigint DEFAULT NULL COMMENT '本地飞行任务ID，对应sf_uav_flight_task.id',
  `uav_job_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '飞控任务ID',
  `error_message` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误信息',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`execution_id`) USING BTREE,
  UNIQUE KEY `uk_sf_uav_plan_execution_once` (`plan_id`,`occurrence_at`,`del_flag`) USING BTREE,
  KEY `idx_sf_uav_plan_execution_plan` (`tenant_id`,`plan_id`,`occurrence_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='无人机飞行计划执行记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_uav_flight_task` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `field_id` bigint NOT NULL COMMENT '地块ID',
  `field_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：地块名称（创建时快照）',
  `species_id` bigint DEFAULT NULL COMMENT '冗余：物种ID',
  `species_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：物种名称',
  `variety_id` bigint DEFAULT NULL COMMENT '冗余：品种ID',
  `variety_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：品种名称',
  `planting_batch_id` bigint DEFAULT NULL COMMENT '创建时地块进行中种植批次ID',
  `planting_batch_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '冗余：种植批次展示名（对应 batch_code）',
  `flight_schedule_id` bigint DEFAULT NULL COMMENT 'sf_uav_flight_schedule.schedule_id',
  `workspace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '飞控工作空间ID',
  `job_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '飞行任务ID（飞控 jobId）',
  `task_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称（本地存储时已在原始名称后拼接 jobId）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志：0=正常，1=删除',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建人用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人用户ID',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `status` int DEFAULT NULL COMMENT '飞行任务状态',
  `progress` int DEFAULT NULL COMMENT '执行进度(百分比等)',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '飞控用户名(英文)',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '飞控返回码/错误码',
  `uploading` int DEFAULT NULL COMMENT '上传状态(按飞控定义)',
  `conditions` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务条件(原始JSON/描述)',
  `source` int DEFAULT NULL COMMENT '任务来源',
  `uav_job_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '飞控任务ID(列表接口返回的jobId)',
  `uav_job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '飞控任务名称',
  `file_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '航线文件ID',
  `file_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '航线文件名称',
  `dock_sn` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '机场设备序列号',
  `dock_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '机场名称',
  `wayline_type` int DEFAULT NULL COMMENT '航线类型(枚举值)',
  `task_type` int DEFAULT NULL COMMENT '任务类型(枚举值)',
  `execute_time` datetime DEFAULT NULL COMMENT '计划执行时间',
  `begin_time` datetime DEFAULT NULL COMMENT '任务开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '任务结束时间',
  `completed_time` datetime DEFAULT NULL COMMENT '任务完成时间',
  `username_cn` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '飞控用户名(中文)',
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '飞控用户ID',
  `rth_altitude` int DEFAULT NULL COMMENT '返航高度(米)',
  `out_of_control_action` int DEFAULT NULL COMMENT '失控动作(枚举值)',
  `exit_wayline_when_rc_lost_enum` int DEFAULT NULL COMMENT '遥控失联退出航线枚举值',
  `media_count` int DEFAULT NULL COMMENT '媒体总数',
  `uploaded_count` int DEFAULT NULL COMMENT '已上传媒体数量',
  `parent_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父任务ID',
  `is_breakpoint` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否断点任务(false/true)',
  `ai_model_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建任务时指定的AI分析模型编号(算法中台modelNo)',
  `ai_model_nos` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'AI model numbers, comma separated',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sf_uav_task_tenant_field` (`tenant_id`,`field_id`) USING BTREE,
  KEY `idx_sf_uav_task_job` (`job_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='无人机飞行任务记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_uav_media_file` (
  `uav_media_id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户',
  `workspace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '飞控 workspace_id',
  `sync_job_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '本次拉取使用的任务 ID（getFileByJobId 路径参数）',
  `media_group_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '飞控 data 分组键（如 任务名-时间），无分组时为空',
  `file_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '飞控 file_id',
  `file_name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `file_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `object_key` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对象存储 key 或直链',
  `is_original` tinyint(1) DEFAULT NULL COMMENT '是否原片',
  `drone` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `payload` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tinny_fingerprint` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `fingerprint` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `file_create_time` datetime DEFAULT NULL COMMENT '飞控 create_time',
  `payload_job_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '媒体 JSON 内 job_id',
  `absolute_altitude` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `relative_altitude` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `lat` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `lng` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `job_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `fly_line_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `fly_line_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `file_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '媒体类型/扩展名提示',
  `field_id` bigint DEFAULT NULL COMMENT '绑定地块（同步时写入）',
  `batch_id` bigint DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`uav_media_id`) USING BTREE,
  UNIQUE KEY `uk_sf_uav_media_tenant_job_file` (`tenant_id`,`sync_job_id`,`file_id`) USING BTREE,
  KEY `idx_sf_uav_media_tenant_sync_job` (`tenant_id`,`sync_job_id`) USING BTREE,
  KEY `idx_sf_uav_media_workspace` (`workspace_id`) USING BTREE,
  KEY `idx_sf_uav_media_field` (`field_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='无人机任务媒体文件（飞控原始结构）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_uav_workspace_binding` (
  `binding_id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `region_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '飞控区域编码（与 addWorkspace 一致）',
  `workspace_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工作空间名称（飞控侧唯一语义，本租户内业务唯一）',
  `workspace_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `workspace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '飞控 workspace_id（UUID）',
  `bind_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '飞控绑定码 bind_code',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`binding_id`) USING BTREE,
  KEY `idx_uav_bind_tenant_name` (`tenant_id`,`workspace_name`) USING BTREE,
  KEY `idx_uav_bind_workspace` (`workspace_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='无人机飞控工作空间绑定';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_weather_alert` (
  `warning_id` varchar(64) NOT NULL COMMENT '来源预警唯一ID（字符串）',
  `province_code` varchar(12) DEFAULT NULL COMMENT '省级行政区划编码',
  `city_code` varchar(12) DEFAULT NULL COMMENT '市级行政区划编码',
  `district_code` varchar(12) DEFAULT NULL COMMENT '区县行政区划编码',
  `title` varchar(256) NOT NULL COMMENT '预警标题',
  `headline` varchar(256) DEFAULT NULL COMMENT '来源摘要标题',
  `alert_type_code` varchar(32) DEFAULT NULL COMMENT '预警类型编码',
  `alert_type_name` varchar(64) DEFAULT NULL COMMENT '预警类型名称',
  `level_code` varchar(16) DEFAULT NULL COMMENT '预警等级编码',
  `level_name` varchar(32) DEFAULT NULL COMMENT '预警等级名称',
  `publisher` varchar(128) DEFAULT NULL COMMENT '发布单位',
  `effective_at` datetime DEFAULT NULL COMMENT '发布时间或生效时间',
  `expires_at` datetime DEFAULT NULL COMMENT '失效时间，来源无明确值时为空',
  `area_text` varchar(256) DEFAULT NULL COMMENT '影响区域文本',
  `description` text COMMENT '预警正文',
  `instruction_json` json DEFAULT NULL COMMENT '防御指南数组 JSON',
  `source_url` varchar(512) DEFAULT NULL COMMENT '来源详情页地址',
  `active_flag` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否仍有效',
  `freshness_status` varchar(16) NOT NULL DEFAULT 'FRESH' COMMENT 'FRESH/STALE/UNAVAILABLE',
  `last_seen_at` datetime NOT NULL COMMENT '最近一次从来源同步到该预警的时间',
  `missing_count` int NOT NULL DEFAULT '0' COMMENT '连续成功同步但未再出现的次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`warning_id`),
  KEY `idx_sf_weather_alert_active_province` (`active_flag`,`province_code`,`effective_at`),
  KEY `idx_sf_weather_alert_active_city` (`active_flag`,`city_code`,`effective_at`),
  KEY `idx_sf_weather_alert_active_district` (`active_flag`,`district_code`,`effective_at`),
  KEY `idx_sf_weather_alert_last_seen` (`last_seen_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='中央气象台公开预警缓存';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_weather_alert_sync_meta` (
  `meta_key` varchar(64) NOT NULL COMMENT '元数据键',
  `meta_value` varchar(256) DEFAULT NULL COMMENT '元数据值',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`meta_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='气象预警同步水位';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_weather_forecast` (
  `forecast_id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `sync_id` bigint NOT NULL COMMENT '一次拉取批次',
  `adcode` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '高德区域编码 city 参数',
  `province` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '省',
  `city_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '城市/区名称',
  `forecast_report_time` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '高德预报发布时间 reporttime',
  `cast_date` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '预报日期 date',
  `week_num` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '星期 week',
  `day_weather` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `night_weather` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `day_temp` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `night_temp` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `day_wind` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `night_wind` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `day_power` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `night_power` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `day_temp_float` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `night_temp_float` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `pull_time` datetime NOT NULL COMMENT '本服务拉取入库时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`forecast_id`) USING BTREE,
  KEY `idx_sf_wf_tenant_adcode_pull` (`tenant_id`,`adcode`,`pull_time`) USING BTREE,
  KEY `idx_sf_wf_sync` (`sync_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='高德天气预报（extensions=all）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sf_weather_live` (
  `live_id` bigint NOT NULL COMMENT '主键',
  `adcode` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '高德区域编码 city 参数',
  `province` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '省',
  `city_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '城市/区名称',
  `weather` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '天气现象',
  `temperature` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实时气温，单位：摄氏度',
  `wind_direction` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '风向',
  `wind_power` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '风力级别',
  `humidity` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '空气湿度',
  `report_time` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '高德数据发布时间 reporttime',
  `pull_time` datetime NOT NULL COMMENT '本服务拉取入库时间',
  PRIMARY KEY (`live_id`) USING BTREE,
  KEY `idx_sf_wl_adcode_pull` (`adcode`,`pull_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='高德实况天气（extensions=base）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

CREATE TABLE `domain_command_idempotency` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `action_name` varchar(128) NOT NULL COMMENT '领域动作',
  `business_id` varchar(128) NOT NULL COMMENT '稳定业务号',
  `request_id` varchar(128) NOT NULL COMMENT '客户端幂等请求号',
  `status` varchar(16) NOT NULL COMMENT 'RUNNING/SUCCEEDED/FAILED',
  `result_value` int DEFAULT NULL COMMENT '成功结果值',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '失败后重试次数',
  `started_at` datetime NOT NULL,
  `finished_at` datetime DEFAULT NULL,
  `failure_reason` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_domain_command_request` (`tenant_id`,`action_name`,`request_id`),
  KEY `idx_domain_command_business` (`tenant_id`,`action_name`,`business_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨服务领域命令幂等记录';
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
