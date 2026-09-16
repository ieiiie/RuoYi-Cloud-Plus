-- SaaS 租户联系电话可空，并修复企业资料字段的乱码注释（MySQL）

SET NAMES utf8mb4;

-- 修复早期错误字符集会话写入的操作日志部门名称，仅匹配已确认的乱码字节。
UPDATE `ry_dbo`.`sys_oper_log` oper_log
JOIN `ry_dbo`.`sys_dept` dept ON dept.dept_id = oper_log.dept_id
SET oper_log.dept_name = dept.dept_name
WHERE HEX(oper_log.dept_name) = 'C3A7C2A0E2809DC3A5C28FE28098C3A9C692C2A8C3A9E28094C2A8';

ALTER TABLE `ry-cloud`.`sys_tenant`
    MODIFY COLUMN `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
    MODIFY COLUMN `logo_url` varchar(500) DEFAULT NULL COMMENT '企业Logo地址',
    MODIFY COLUMN `province_code` varchar(12) DEFAULT NULL COMMENT '省级行政区划编码',
    MODIFY COLUMN `city_code` varchar(12) DEFAULT NULL COMMENT '市级行政区划编码',
    MODIFY COLUMN `district_code` varchar(12) DEFAULT NULL COMMENT '区县级行政区划编码',
    MODIFY COLUMN `region_name` varchar(255) DEFAULT NULL COMMENT '行政区划名称';

-- 管理员手机号写入全局用户表；数据库唯一索引用于兜底并发创建。
-- 重复执行迁移时，已存在的唯一索引不会被再次创建。
SET @global_user_phone_unique_index_exists = (
    SELECT COUNT(*)
    FROM `information_schema`.`statistics`
    WHERE `table_schema` = 'ry-cloud'
      AND `table_name` = 'sys_global_user'
      AND `index_name` = 'uk_sys_global_user_phone'
      AND `non_unique` = 0
);
SET @global_user_phone_unique_index_sql = IF(
    @global_user_phone_unique_index_exists = 0,
    'ALTER TABLE `ry-cloud`.`sys_global_user` ADD UNIQUE KEY `uk_sys_global_user_phone` (`phone_number`)',
    'SELECT 1'
);
PREPARE global_user_phone_unique_index_statement FROM @global_user_phone_unique_index_sql;
EXECUTE global_user_phone_unique_index_statement;
DEALLOCATE PREPARE global_user_phone_unique_index_statement;
