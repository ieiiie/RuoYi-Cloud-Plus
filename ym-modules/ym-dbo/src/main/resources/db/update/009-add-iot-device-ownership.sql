-- Run in DBO's own database only. Does not modify business tenant menus or ownership rows.
-- IDs checked for conflicts before any DML; reruns preserve explicit role assignments.
DROP PROCEDURE IF EXISTS add_dbo_iot_ownership_menu;
DELIMITER $$
CREATE PROCEDURE add_dbo_iot_ownership_menu()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
    IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=1762400000000000000 AND path='saas') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected DBO SaaS parent menu missing';
    END IF;
    IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id BETWEEN 1762400000000001000 AND 1762400000000001004
        AND perms NOT LIKE 'saas:iot-device-ownership:%') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DBO ownership menu ID conflict';
    END IF;
    IF EXISTS (SELECT 1 FROM sys_menu WHERE perms LIKE 'saas:iot-device-ownership:%'
        AND menu_id NOT BETWEEN 1762400000000001000 AND 1762400000000001004) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DBO ownership permission already uses other IDs';
    END IF;
    START TRANSACTION;
    INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_time,remark)
    SELECT 1762400000000001000,'设备归属',1762400000000000000,10,'iot-device-ownership','saas/iot-device-ownership/index','N','Y','C','0','0','saas:iot-device-ownership:query','ant-design:deployment-unit-outlined',NOW(),'DBO直写IoT归属；技术档案由JetLinks维护'
    WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=1762400000000001000);
    INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_time)
    SELECT 1762400000000001000+a.seq,a.title,1762400000000001000,a.seq,'','','N','Y','F','0','0',CONCAT('saas:iot-device-ownership:',a.action),'#',NOW()
    FROM (SELECT 1 seq,'分配设备' title,'assign' action UNION ALL SELECT 2,'收回设备','release'
        UNION ALL SELECT 3,'转移设备','transfer' UNION ALL SELECT 4,'重试归属同步','retry') a
    WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=1762400000000001000+a.seq);
    -- Default platform administrator only; other operations roles must be explicitly granted in DBO.
    INSERT INTO sys_role_menu (role_id,menu_id)
    SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m
        ON m.menu_id BETWEEN 1762400000000001000 AND 1762400000000001004
    WHERE r.role_id=1761300000000000001 AND r.del_flag='0'
        AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id=r.role_id AND x.menu_id=m.menu_id);
    COMMIT;
END$$
DELIMITER ;
CALL add_dbo_iot_ownership_menu();
DROP PROCEDURE add_dbo_iot_ownership_menu;
