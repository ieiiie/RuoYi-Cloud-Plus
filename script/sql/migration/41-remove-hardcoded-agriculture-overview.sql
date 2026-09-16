-- 仅删除已确认的农业硬编码首页；保留农业应用、独立大屏和其他地图页面。
-- 执行前按 menu_id=2059893469555458049 备份下列五张表的数据。
USE `ry-cloud`;
DELIMITER $$
DROP PROCEDURE IF EXISTS remove_hardcoded_agriculture_overview$$
CREATE PROCEDURE remove_hardcoded_agriculture_overview()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    START TRANSACTION;
    IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2059893469555458049
        AND (app_id <> 1762100000000000101 OR component <> 'dashboard/home-overview/index')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu identity changed; abort removal';
    END IF;
    IF EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2059893469555458049) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected child menus; abort removal';
    END IF;
    DELETE FROM sys_composite_app_menu WHERE menu_id = 2059893469555458049;
    DELETE FROM sys_tenant_package_menu WHERE menu_id = 2059893469555458049;
    DELETE FROM sys_role_template_menu WHERE menu_id = 2059893469555458049;
    DELETE FROM sys_role_menu WHERE menu_id = 2059893469555458049;
    DELETE FROM sys_menu WHERE menu_id = 2059893469555458049;
    COMMIT;
END$$
CALL remove_hardcoded_agriculture_overview()$$
DROP PROCEDURE remove_hardcoded_agriculture_overview$$
DELIMITER ;

-- 校验：以下计数均应为 0，可重复执行本脚本。
SELECT 'menu' AS kind, COUNT(*) AS remaining FROM sys_menu WHERE menu_id = 2059893469555458049
UNION ALL SELECT 'role', COUNT(*) FROM sys_role_menu WHERE menu_id = 2059893469555458049
UNION ALL SELECT 'role_template', COUNT(*) FROM sys_role_template_menu WHERE menu_id = 2059893469555458049
UNION ALL SELECT 'package', COUNT(*) FROM sys_tenant_package_menu WHERE menu_id = 2059893469555458049
UNION ALL SELECT 'composite', COUNT(*) FROM sys_composite_app_menu WHERE menu_id = 2059893469555458049;
