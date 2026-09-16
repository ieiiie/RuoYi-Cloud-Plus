
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
CREATE TABLE `sys_employee` (
  `employee_id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `user_id` bigint DEFAULT NULL COMMENT '关联 sys_user.user_id（可空，有账号时填写）',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '登录账号（已关联 sys_user 时冗余 sys_user.user_name，未关联为 NULL）',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号',
  `id_card` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '身份证号',
  `wx_phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '微信关联手机号',
  `wx_openid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '微信openid',
  `invite_code_id` bigint DEFAULT NULL COMMENT '使用的绑定邀请码ID（方案一使用）',
  `gender` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '2' COMMENT '性别：0男 1女 2未知',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `photo` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '照片 OSS URL',
  `person_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '人员类型：0内部 1外部',
  `app_role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '应用角色编码（由各应用配置枚举）',
  `miniapp_register_approval_role_id` bigint DEFAULT NULL COMMENT ' 小程序注册审批系统角色ID',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '状态：1正常 0停用 2待审核',
  `review_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核状态：0待审核 1通过 2拒绝；后台录入无需审核时为空',
  `review_by` bigint DEFAULT NULL COMMENT '审核人ID（关联sys_user.user_id）',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `review_remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核备注（通过/不通过原因）',
  `submit_device` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提交设备信息（小程序端获取）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`employee_id`) USING BTREE,
  UNIQUE KEY `uk_sys_emp_tenant_phone` (`tenant_id`,`phone`) USING BTREE,
  UNIQUE KEY `uk_sys_emp_wx_openid` (`wx_openid`) USING BTREE,
  UNIQUE KEY `uk_sys_emp_user_id` (`user_id`) USING BTREE,
  KEY `idx_sys_emp_invite_code_id` (`invite_code_id`) USING BTREE,
  KEY `idx_sys_emp_tenant_type` (`tenant_id`,`person_type`) USING BTREE,
  KEY `idx_sys_emp_tenant_role` (`tenant_id`,`app_role_code`) USING BTREE,
  KEY `idx_sys_employee_miniapp_register_approval_role` (`tenant_id`,`miniapp_register_approval_role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='平台员工主数据';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_employee_miniapp_role` (
  `employee_id` bigint NOT NULL COMMENT '人员ID',
  `role_id` bigint NOT NULL COMMENT '系统角色ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户编号',
  PRIMARY KEY (`employee_id`,`role_id`),
  KEY `idx_sys_employee_miniapp_role_tenant_role` (`tenant_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人员小程序系统角色关联';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_invite_code` (
  `code_id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `invite_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '邀请码（方案一4位数字，方案二6位数字）',
  `code_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '邀请码类型：1角色邀请码（方案二） 2人员绑定码（方案一）',
  `employee_id` bigint DEFAULT NULL COMMENT '绑定目标员工ID（方案一专用，方案二为NULL）',
  `app_role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联应用角色编码（方案二专用，方案一为NULL）',
  `person_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '人员类型：0内部 1外部',
  `max_uses` int NOT NULL DEFAULT '1' COMMENT '最大使用次数',
  `used_count` int NOT NULL DEFAULT '0' COMMENT '已使用次数',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间（NULL表示永久有效）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '状态：1有效 0停用',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`code_id`) USING BTREE,
  UNIQUE KEY `uk_sys_invite_code` (`invite_code`) USING BTREE,
  KEY `idx_sys_invite_employee` (`employee_id`) USING BTREE,
  KEY `idx_sys_invite_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_sys_invite_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='平台邀请码表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
