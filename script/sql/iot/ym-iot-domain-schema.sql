
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
CREATE TABLE `iot_alert_record` (
  `alert_id` bigint NOT NULL COMMENT '报警记录主键ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `rule_id` bigint DEFAULT NULL COMMENT '触发的规则ID（关联 iot_alert_rule，手工录入可为空）',
  `alert_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '报警来源类型：THRESHOLD=阈值越限，OFFLINE=离线超时',
  `alert_level` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'WARN' COMMENT '严重级别：INFO=提示，WARN=警告，EMERGENCY=紧急',
  `alert_scope` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'USER' COMMENT '可见范围：USER=租户/用户侧业务告警，SYSTEM=平台运维级告警',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '告警标题',
  `content` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警正文描述',
  `device_id` bigint DEFAULT NULL COMMENT '关联设备ID',
  `property_identifier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '触发告警的属性标识（物模型 identifier）',
  `trigger_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '触发时刻的测点值或快照（文本）',
  `trigger_detail` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '扩展详情（JSON，如多指标、上下文）',
  `recovery_time` datetime DEFAULT NULL COMMENT '恢复时间（条件恢复正常时回写）',
  `handle_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING=待处理，IGNORED=已忽略，RESOLVED=已处理结案',
  `handle_by` bigint DEFAULT NULL COMMENT '处理人用户ID',
  `handle_time` datetime DEFAULT NULL COMMENT '处理操作时间',
  `handle_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '处理说明备注',
  `sms_sent` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否已发短信：0=未发送，1=已发送',
  `sms_time` datetime DEFAULT NULL COMMENT '短信发送时间',
  `notify_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '通知状态：0=未通知（含短信/Webhook 等），1=已通知',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '告警产生时间（入库时间）',
  PRIMARY KEY (`alert_id`) USING BTREE,
  KEY `idx_iot_alert_tenant_status` (`tenant_id`,`handle_status`) USING BTREE,
  KEY `idx_iot_alert_type` (`alert_type`) USING BTREE,
  KEY `idx_iot_alert_create` (`create_time`) USING BTREE,
  KEY `idx_iot_alert_device` (`device_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='物联网报警记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_alert_rule` (
  `rule_id` bigint NOT NULL COMMENT '规则主键ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `product_id` bigint DEFAULT NULL COMMENT '作用范围：某产品下全部设备（与 device_id 二选一或组合，应用层约束）',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则名称（展示用）',
  `rule_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则类型：THRESHOLD=阈值越限，OFFLINE=离线超时，AI_RESULT=AI/模型结果触发',
  `device_id` bigint DEFAULT NULL COMMENT '绑定单台设备ID（关联 iot_device，与产品级范围二选一或组合）',
  `biz_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'IOT' COMMENT '所属业务线/子系统编码（英文，如 IOT=通用，WMS=仓储；农业场景可用标签或业务库关联，不在此表绑地块）',
  `metric_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '阈值类规则监控的指标编码（与测点 metric_code 一致）',
  `property_identifier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物模型属性标识（与 iot_product_property.identifier 一致）',
  `operator` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '比较运算符：GT=大于，LT=小于，GE=大于等于，LE=小于等于，BETWEEN=区间，EQ=等于，NE=不等于',
  `threshold_min` decimal(20,6) DEFAULT NULL COMMENT '阈值下限（数值型）',
  `threshold_max` decimal(20,6) DEFAULT NULL COMMENT '阈值上限（数值型）',
  `threshold_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '非数值阈值（字符串或枚举取值）',
  `offline_minutes` int DEFAULT NULL COMMENT '离线判定：超过若干分钟无上报则告警',
  `duration_seconds` int DEFAULT NULL COMMENT '持续超限秒数（防抖：连续满足条件若干秒才触发）',
  `check_interval_sec` int DEFAULT NULL COMMENT '规则调度检查周期（秒）',
  `circuit_breaker_day` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '日报警熔断：0=关闭，1=开启（当天存在同规则同设备的待处理报警时不再生成新报警，已处理的可再次触发）',
  `webhook_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '触发后回调 URL（HTTP/HTTPS，Webhook）',
  `enabled` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '是否启用：0=禁用，1=启用',
  `notify_sms` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '是否短信通知：0=否，1=是',
  `phone_numbers` json DEFAULT NULL COMMENT '短信通知手机号（多个用英文逗号分隔）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志：0=存在，1=逻辑删除',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建人用户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人用户ID',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`rule_id`) USING BTREE,
  KEY `idx_iot_rule_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_iot_rule_tenant_biz` (`tenant_id`,`biz_code`) USING BTREE,
  KEY `idx_iot_rule_product` (`product_id`) USING BTREE,
  KEY `idx_iot_rule_device` (`device_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='物联网报警规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_data_point` (
  `point_id` bigint NOT NULL COMMENT '测点记录主键ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `device_id` bigint NOT NULL COMMENT '设备ID（关联 iot_device）',
  `metric_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '指标编码（英文蛇形命名，如 soil_moisture，与物模型 identifier 对齐）',
  `property_identifier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物模型属性标识（常与 metric_code 相同，便于与 iot_product_property 对照）',
  `metric_value` decimal(20,6) DEFAULT NULL COMMENT '数值型测点值（主查询列，非数值见 value_text）',
  `value_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '非数值快照（字符串、枚举名等）',
  `metric_unit` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '单位（与物模型 unit 一致时可冗余存储）',
  `collect_time` datetime(3) NOT NULL COMMENT '采集时间（设备侧时间戳，毫秒精度）',
  `received_time` datetime(3) DEFAULT NULL COMMENT '入库时间（平台收到并写入时间）',
  `raw_payload` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '原始上报报文（JSON 等，便于排障与审计）',
  PRIMARY KEY (`point_id`) USING BTREE,
  UNIQUE KEY `uk_device_metric_time` (`tenant_id`,`device_id`,`metric_code`,`collect_time`) USING BTREE,
  UNIQUE KEY `uk_iot_point_dev_metric_time` (`device_id`,`metric_code`,`collect_time`) USING BTREE,
  KEY `idx_iot_point_device_time` (`device_id`,`collect_time`) USING BTREE,
  KEY `idx_iot_point_tenant_time` (`tenant_id`,`collect_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='物联网测点时序表（高频数据建议配合 InfluxDB）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_device` (
  `device_id` bigint NOT NULL COMMENT '设备主键ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `product_id` bigint DEFAULT NULL COMMENT '物模型产品ID（关联 iot_product，未接入物模型可为空）',
  `device_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备编号（租户内唯一，业务侧资产编码或 SN）',
  `device_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备名称（展示用）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序值（升序）',
  `device_secret` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备密钥/令牌（MQTT 密码等，须应用层加密存储与脱敏展示）',
  `imei` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'IMEI（国际移动设备识别码，蜂窝模组唯一标识）',
  `mac` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'MAC 地址（网卡物理地址）',
  `device_category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备大类（英文枚举）：SOIL=土壤仪，MET=气象站，CAMERA=摄像头，UAV=无人机/机场，SAT_GATEWAY=卫星网关，OTHER=其他',
  `lng` decimal(10,7) DEFAULT NULL COMMENT '地图经度（十进制度）',
  `lat` decimal(10,7) DEFAULT NULL COMMENT '地图纬度（十进制度）',
  `online_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'OFFLINE' COMMENT '在线状态：ONLINE=在线，OFFLINE=离线，FAULT=故障/异常',
  `last_report_time` datetime DEFAULT NULL COMMENT '设备最后一次数据上报时间',
  `last_remote_fetch_time` datetime DEFAULT NULL COMMENT '平台最后一次成功从远程拉取到遥测列表的时间（如 HFZK）',
  `last_offline_time` datetime DEFAULT NULL COMMENT '最近一次判定离线时间',
  `ip_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近一次上报来源 IP 地址',
  `protocol` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实际接入协议或厂商协议标识（英文缩写或代号）',
  `firmware_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前固件版本号',
  `config_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '扩展配置（JSON 文本，存放通道、系数等非标准字段）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '档案状态：0=正常启用，1=停用（不再采集但可查询历史）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志：0=存在，1=逻辑删除',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建人用户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人用户ID',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`device_id`) USING BTREE,
  UNIQUE KEY `uk_iot_device_tenant_code` (`tenant_id`,`device_code`) USING BTREE,
  KEY `idx_iot_device_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_iot_device_product` (`product_id`) USING BTREE,
  KEY `idx_iot_device_category` (`device_category`) USING BTREE,
  KEY `idx_iot_device_online` (`online_status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='物联网设备档案表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_device_log` (
  `log_id` bigint NOT NULL COMMENT '日志主键ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `device_id` bigint NOT NULL COMMENT '设备ID（关联 iot_device）',
  `log_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '日志类型：ONLINE=上线，OFFLINE=下线，ERROR=错误/异常，PROPERTY_SET=属性下发，SERVICE_INVOKE=服务调用',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '日志正文或结构化内容（可为 JSON）',
  `log_time` datetime(3) NOT NULL COMMENT '事件发生时间（业务时间）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '写入数据库时间',
  PRIMARY KEY (`log_id`) USING BTREE,
  KEY `idx_iot_devlog_device_time` (`device_id`,`log_time`) USING BTREE,
  KEY `idx_iot_devlog_tenant_time` (`tenant_id`,`log_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='物联网设备日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_device_tag` (
  `tag_id` bigint NOT NULL COMMENT '标签主键ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `device_id` bigint NOT NULL COMMENT '设备ID（关联 iot_device）',
  `tag_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标签键（维度名，如 scene、location，建议英文小写）',
  `tag_value` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标签值（如 farm、greenhouse_1）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`tag_id`) USING BTREE,
  UNIQUE KEY `uk_iot_tag_dev_key` (`tenant_id`,`device_id`,`tag_key`) USING BTREE,
  KEY `idx_iot_tag_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='物联网设备标签表（软分组）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_fertilizer_control_log` (
  `log_id` bigint NOT NULL COMMENT '主键（雪花ID）',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '租户ID',
  `device_id` bigint NOT NULL COMMENT '设备主键（iot_device.id）',
  `device_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '设备编号（冗余，方便查询）',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人用户ID',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '操作人姓名（冗余）',
  `operator_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '操作人IP',
  `command` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '命令: START(一键启动)/STOP(有序停止)/EMERGENCY_STOP(紧急停止)/RESET(复位)/PRIME_WATER(加引水)/CLEAN_TANK(清罐)/START_WITHOUT_RESET(无复位启动)/SET_PARAM(参数下发)/GET_REALTIME(拉取实时数据)',
  `task_id` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '异步任务ID（UUID前8位）',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '最终状态: SUCCEEDED/FAILED/TIMEOUT',
  `error_message` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '错误信息（失败时填写）',
  `request_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '请求体JSON（参数下发时含完整参数）',
  `emergency_reason` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '急停原因（仅急停命令填写）',
  `downlink_frame` text COMMENT '下行帧HEX，多条按换行追加（用于排障）',
  `downlink_topic` text COMMENT '下行MQTT topic，多条按换行追加',
  `begin_at` datetime NOT NULL COMMENT '操作开始时间',
  `end_at` datetime DEFAULT NULL COMMENT '操作结束时间',
  `duration_ms` int DEFAULT NULL COMMENT '操作耗时（毫秒）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`log_id`) USING BTREE,
  KEY `idx_device_id` (`device_id`) USING BTREE,
  KEY `idx_device_code` (`device_code`) USING BTREE,
  KEY `idx_command` (`command`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_begin_at` (`begin_at`) USING BTREE,
  KEY `idx_operator_id` (`operator_id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='施肥机控制操作日志（审计表）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_fertilizer_record` (
  `record_id` bigint NOT NULL COMMENT '施肥流水主键ID（雪花ID）',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号（来自设备所属地块）',
  `device_id` bigint NOT NULL COMMENT '设备ID（iot_device.device_id）',
  `device_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备编号（冗余，便于排障）',
  `record_time` datetime NOT NULL COMMENT '本次施肥时间（uint32 时间戳转换）',
  `fertilization_type` tinyint NOT NULL DEFAULT '0' COMMENT '施肥类型（0x13 帧内 uint16 取低 8 位）',
  `fertilization_seconds` int NOT NULL DEFAULT '0' COMMENT '本次施肥时长（秒）',
  `fertilization_quantity` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT '本次施肥量（kg，由 IEEE754 float 转换，保留 3 位）',
  `raw_payload_hex` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '本条 12 字节原始 HEX，排障用',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，2=删除',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门（保持框架自动填充字段一致）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者用户ID（设备上行写入时为空）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  `update_by` bigint DEFAULT NULL COMMENT '最近更新者用户ID',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  PRIMARY KEY (`record_id`) USING BTREE,
  UNIQUE KEY `uk_iot_fert_record_tenant_device_time_type` (`tenant_id`,`device_id`,`record_time`,`fertilization_type`) USING BTREE COMMENT '同租户+设备+施肥时间+类型 唯一，断网续传重复帧时去重',
  KEY `idx_iot_fert_record_device_time` (`device_id`,`record_time`) USING BTREE,
  KEY `idx_iot_fert_record_tenant_create` (`tenant_id`,`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='施肥机-单次施肥流水（来自 0x13 主动上传帧）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_hfzk_user_device` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `external_device_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '外部设备主键（接口 data[].id）',
  `device_sn` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备序列号',
  `device_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '1 气象 / 2 土壤墒情 / 3 虫情',
  `device_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备名称',
  `longitude` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '经度',
  `latitude` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '纬度',
  `external_user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '外部用户 ID（userId）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `extra_info` json DEFAULT NULL COMMENT '设备扩展信息 JSON（WVP 摄像头存通道快照数组：[{id,name,status,deviceOnline,channel}]）',
  `last_sync_time` datetime NOT NULL COMMENT '最近一次拉取写入时间',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_iot_hfzk_ud_tenant_ext_dev` (`tenant_id`,`external_device_id`) USING BTREE,
  KEY `idx_iot_hfzk_ud_sn` (`tenant_id`,`device_sn`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='中科合肥用户设备列表同步';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_motorvalve_control_log` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `device_id` bigint DEFAULT NULL COMMENT '设备ID',
  `device_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备编号',
  `product_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '产品类型',
  `lora_addr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'LoRa阀门地址（多个用逗号分隔，4G设备为空）',
  `valve_no` int DEFAULT NULL COMMENT '阀门编号，从1开始',
  `target_position` int DEFAULT NULL COMMENT '目标位置（度）',
  `target_percent` int DEFAULT NULL COMMENT '目标百分比（0-100）；仅百分比控制时有值，单通存 percent，三通/五通存选中出口 percent',
  `target_channel` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '目标通道（A/B/C/D，三通/五通百分比控制时有值；单通阀为空）',
  `target_channels` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '三通/五通通道百分比 JSON，如 {"A":100,"B":0}；仅百分比控制三通/五通时有值',
  `command_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '命令类型：CONTROL/READ_DATA/READ_STATUS/NTP_SYNC',
  `command_text` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '下发指令原文',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SENT' COMMENT '状态：SENT/ACK/SUCCESS/FAILED/REJECTED',
  `reply_text` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '应答原文',
  `error_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误码',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_imcl_device` (`tenant_id`,`device_id`) USING BTREE,
  KEY `idx_imcl_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='电动阀控制操作日志';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_motorvalve_valve_session` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `device_id` bigint NOT NULL COMMENT '设备ID',
  `device_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备编号（冗余）',
  `valve_no` int NOT NULL COMMENT '阀门编号，从1开始',
  `valve_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'single_port' COMMENT '阀门类型编码，见 ValveType',
  `open_position` int DEFAULT NULL COMMENT '开阀目标位置（度）',
  `close_position` int DEFAULT NULL COMMENT '关阀目标位置（度）',
  `open_time` datetime NOT NULL COMMENT '开阀确认时间（应答成功时刻）',
  `close_time` datetime DEFAULT NULL COMMENT '关阀确认时间；NULL 表示仍开启中',
  `duration_seconds` int DEFAULT NULL COMMENT '持续秒数，关阀后回填',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED/ABORTED',
  `open_control_log_id` bigint DEFAULT NULL COMMENT '关联开阀 control_log.id',
  `close_control_log_id` bigint DEFAULT NULL COMMENT '关联关阀 control_log.id',
  `open_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开阀操作人',
  `close_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关阀操作人',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注（如重复开阀自动补关）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_imvs_device_valve` (`tenant_id`,`device_id`,`valve_no`),
  KEY `idx_imvs_open_time` (`open_time`),
  KEY `idx_imvs_status` (`status`),
  KEY `idx_imvs_close_time` (`close_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='电动阀开阀会话（开启持续时间）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_product` (
  `product_id` bigint NOT NULL COMMENT '产品主键ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号（多租户隔离）',
  `product_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '产品密钥/类型标识（租户内唯一，设备端烧录或注册用）',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '产品名称（展示用）',
  `node_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DIRECT' COMMENT '节点类型：DIRECT=直连设备，GATEWAY=网关，SUB=网关下子设备',
  `net_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '网络类型：WIFI=无线局域网，CELLULAR=蜂窝(2G/3G/4G/5G)，ETHERNET=有线以太网，LORA=LoRa，NBIOT=窄带物联网',
  `protocol` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '接入协议：MQTT、COAP、HTTP、MODBUS 等（存英文协议名）',
  `data_format` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '上报数据格式：JSON=文本JSON，BINARY=二进制，PROTOBUF=Protobuf 等',
  `device_category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备大类（英文枚举，与业务字典对齐）：SOIL=土壤，MET=气象，CAMERA=摄像头，UAV=无人机相关，OTHER=其他',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '产品状态：0=开发中（物模型可改），1=已发布（建议冻结模型）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志：0=正常存在，1=逻辑删除',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建人用户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人用户ID',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `map_icon_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地图设备点位图标 URL',
  `map_selected_icon_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '选中态图标 URL（可选）',
  PRIMARY KEY (`product_id`) USING BTREE,
  UNIQUE KEY `uk_iot_product_tenant_key` (`tenant_id`,`product_key`) USING BTREE,
  KEY `idx_iot_product_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='物联网物模型产品表（设备类型定义）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_product_property` (
  `property_id` bigint NOT NULL COMMENT '属性定义主键ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `product_id` bigint NOT NULL COMMENT '所属产品ID（关联 iot_product.product_id，逻辑关联无跨库外键）',
  `identifier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '属性标识（英文键，如 temperature、humidity，用于上报与物模型匹配）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '属性显示名称（中文或任意展示文案）',
  `data_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据类型：INT=整数，FLOAT=浮点，STRING=字符串，ENUM=枚举，BOOL=布尔，ARRAY=数组，OBJECT=对象',
  `unit` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物理单位（如℃、%、kWh，展示与换算用）',
  `rw_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'R' COMMENT '读写权限：R=只读（仅设备上报），W=可写（平台可下发修改）',
  `step` decimal(20,6) DEFAULT NULL COMMENT '步长或精度（数值型属性的最小刻度说明）',
  `sort_order` int DEFAULT '0' COMMENT '排序号（控制台列表顺序，数值越小越靠前）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志：0=存在，1=逻辑删除',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建人用户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人用户ID',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`property_id`) USING BTREE,
  UNIQUE KEY `uk_iot_prop_pid_id` (`tenant_id`,`product_id`,`identifier`) USING BTREE,
  KEY `idx_iot_prop_product` (`product_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='物联网物模型属性表（测点定义）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_isup_alarm_event` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `device_id` bigint NOT NULL COMMENT '关联设备ID（FK -> iot_device.device_id）',
  `alarm_type` int DEFAULT NULL COMMENT '报警类型编码（如 EHOME_ALARM=1, EHOME_ISAPI_ALARM=13 等）',
  `alarm_type_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '报警类型描述',
  `alarm_time` datetime DEFAULT NULL COMMENT '报警触发时间',
  `channel_no` int DEFAULT NULL COMMENT '通道号',
  `pic_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '报警图片URL（SS接收后上传MinIO）',
  `event_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '原始报警数据（XML/JSON）',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_device_id` (`device_id`) USING BTREE,
  KEY `idx_alarm_time` (`alarm_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='ISUP报警事件记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_isup_capture_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `device_id` bigint NOT NULL COMMENT '关联设备ID',
  `channel_no` int DEFAULT NULL COMMENT '通道号',
  `capture_time` datetime DEFAULT NULL COMMENT '抓图时间',
  `pic_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '抓图上传MinIO后的URL',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-进行中，1-成功，2-失败',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_device_id` (`device_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='ISUP远程抓图记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_isup_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `config_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置键',
  `config_value` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '配置值',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建人用户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人用户ID',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_config_key` (`config_key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='ISUP配置键值';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_isup_stream_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `device_id` bigint NOT NULL COMMENT '关联设备ID',
  `channel_no` int DEFAULT NULL COMMENT '通道号',
  `session_id` int DEFAULT NULL COMMENT 'SDK返回的sessionId',
  `stream_type` tinyint DEFAULT NULL COMMENT '流类型：0-预览，1-回放',
  `codec_type` tinyint DEFAULT NULL COMMENT '码流类型：0-主码流，1-子码流',
  `start_time` datetime DEFAULT NULL COMMENT '操作开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '操作结束时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-进行中，1-已停止，2-异常中断',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_device_id` (`device_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='ISUP预览回放取流记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
