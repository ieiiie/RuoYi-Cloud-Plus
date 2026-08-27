-- -----------------------------------------------------------------------------
-- RuoYi-Cloud-Plus 6.0.2 全局字典升级脚本（MySQL）
--
-- 前置条件：已执行 update_6.0.0-tenant.sql，sys_dict_type 和 sys_dict_data
-- 均包含 tenant_id。执行期间请停止服务或保持 tenant.enable=false。
-- 本脚本不自动合并多个租户的字典；检测到非默认租户数据时会直接中止。
-- -----------------------------------------------------------------------------

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS check_update_6_0_2_global_dict;

DELIMITER //
CREATE PROCEDURE check_update_6_0_2_global_dict()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM sys_dict_type
        WHERE tenant_id IS NULL OR tenant_id <> '000000'
    ) OR EXISTS (
        SELECT 1
        FROM sys_dict_data
        WHERE tenant_id IS NULL OR tenant_id <> '000000'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '全局字典升级中止：仅支持默认租户 000000 的字典数据';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM sys_dict_type
        GROUP BY dict_type
        HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '全局字典升级中止：存在重复的字典类型';
    END IF;
END //
DELIMITER ;

CALL check_update_6_0_2_global_dict();
DROP PROCEDURE check_update_6_0_2_global_dict;

ALTER TABLE sys_dict_type
    DROP INDEX uk_sys_dict_type_tenant_type,
    DROP COLUMN tenant_id,
    ADD UNIQUE KEY dict_type (dict_type);

ALTER TABLE sys_dict_data
    DROP INDEX idx_sys_dict_data_tenant_type,
    DROP COLUMN tenant_id;

-- 初始脚本通常已保留 dict_type 索引；若历史库缺失，则补齐该全局查询索引。
SET @has_dict_data_type_index = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_dict_data'
      AND index_name = 'idx_sys_dict_data_type'
);
SET @ensure_dict_data_type_index_sql = IF(
    @has_dict_data_type_index = 0,
    'ALTER TABLE sys_dict_data ADD KEY idx_sys_dict_data_type (dict_type)',
    'SELECT 1'
);
PREPARE ensure_dict_data_type_index_stmt FROM @ensure_dict_data_type_index_sql;
EXECUTE ensure_dict_data_type_index_stmt;
DEALLOCATE PREPARE ensure_dict_data_type_index_stmt;

UPDATE sys_menu
SET menu_name = '全局字典',
    remark = '全局字典菜单'
WHERE menu_id = 1761400000000000105;

-- 部署新服务后调用 DELETE /dict/type/refreshCache 清理 global:sys_dict 与
-- global:sys_dict_type；旧租户前缀字典缓存已不再读取，可在 Redis 运维窗口清理。
