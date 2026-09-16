-- 单次测试环境修复。仅租户 916085；普通角色不新增权限。
-- 前置：testaaaa 套餐未变化、132 个菜单、管理员旧授权 18 项、普通角色零授权。
-- 再次执行会因备份表已存在而停止，请先使用末尾校验查询确认状态。
USE `ry-cloud`;
CREATE TABLE `bak_role_menu_916085_20260904` LIKE `sys_role_menu`;
DELIMITER $$
CREATE PROCEDURE repair_tenant_916085_package_menus()
BEGIN
    DECLARE current_package BIGINT;
    DECLARE n BIGINT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    START TRANSACTION;
    SELECT package_id INTO current_package FROM sys_tenant
      WHERE tenant_id = '916085' AND del_flag = '0' FOR UPDATE;
    IF current_package IS NULL OR current_package <> 2095694430483292161 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tenant package changed; abort';
    END IF;
    SELECT COUNT(*) INTO n FROM sys_tenant_package
      WHERE package_id = current_package AND status = '0' AND del_flag = '0';
    IF n <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Package unavailable; abort'; END IF;
    SELECT COUNT(*) INTO n FROM sys_tenant_package_menu WHERE package_id = current_package;
    IF n <> 132 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Package menu count changed; abort'; END IF;
    SELECT COUNT(*) INTO n FROM sys_role WHERE tenant_id = '916085' AND del_flag = '0';
    IF n <> 2 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tenant role count changed; abort'; END IF;
    SELECT COUNT(*) INTO n FROM sys_role WHERE tenant_id = '916085'
      AND role_id = 2093240822863159299 AND role_key = 'tenant_admin' AND status = '0' AND del_flag = '0';
    IF n <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Administrator changed; abort'; END IF;
    SELECT COUNT(*) INTO n FROM sys_role_menu WHERE role_id = 2093240822863159299;
    IF n <> 18 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Original grants changed; abort'; END IF;
    SELECT COUNT(*) INTO n FROM sys_role_menu rm JOIN sys_role r ON r.role_id = rm.role_id
      WHERE r.tenant_id = '916085' AND r.role_id <> 2093240822863159299 AND r.del_flag = '0';
    IF n <> 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ordinary role grants changed; abort'; END IF;
    INSERT INTO bak_role_menu_916085_20260904 SELECT rm.* FROM sys_role_menu rm
      JOIN sys_role r ON r.role_id = rm.role_id WHERE r.tenant_id = '916085' AND r.del_flag = '0';
    DELETE FROM sys_role_menu WHERE role_id = 2093240822863159299;
    INSERT INTO sys_role_menu(role_id, menu_id)
      SELECT 2093240822863159299, menu_id FROM sys_tenant_package_menu WHERE package_id = current_package;
    SELECT COUNT(*) INTO n FROM sys_role_menu WHERE role_id = 2093240822863159299;
    IF n <> 132 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Result count mismatch; rollback'; END IF;
    COMMIT;
END$$
DELIMITER ;
CALL repair_tenant_916085_package_menus();
DROP PROCEDURE repair_tenant_916085_package_menus;

-- 校验：管理员 132，普通角色 0，outside_package 全部为 0，备份 18。
SELECT r.role_id, r.role_key, COUNT(rm.menu_id) AS menu_count,
       SUM(rm.menu_id IS NOT NULL AND pm.menu_id IS NULL) AS outside_package
FROM sys_role r LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
LEFT JOIN sys_tenant_package_menu pm ON pm.menu_id = rm.menu_id AND pm.package_id = 2095694430483292161
WHERE r.tenant_id = '916085' AND r.del_flag = '0' GROUP BY r.role_id, r.role_key;
SELECT COUNT(*) AS backup_rows FROM bak_role_menu_916085_20260904;
-- 若需要回退，在确认套餐和角色没有后续变更后，仅恢复该管理员：
-- START TRANSACTION;
-- DELETE FROM sys_role_menu WHERE role_id = 2093240822863159299;
-- INSERT INTO sys_role_menu SELECT * FROM bak_role_menu_916085_20260904 WHERE role_id = 2093240822863159299;
-- COMMIT;
