-- -----------------------------------------------------------------------------
-- RuoYi-Cloud-Plus 6.0.4 Nacos 控制台菜单清理脚本（MySQL）
--
-- 删除“系统监控”下的“Nacos控制台”菜单及其子菜单，并清理角色授权、租户套餐菜单引用。
-- 请在备份数据库后执行；完整初始化数据库无需执行本脚本。
-- 本脚本不会停止、删除或修改 Nacos 服务。
-- -----------------------------------------------------------------------------

SET NAMES utf8mb4;
SET SESSION group_concat_max_len = 8192;

DROP PROCEDURE IF EXISTS remove_update_6_0_4_nacos_console_menu;

DELIMITER //
CREATE PROCEDURE remove_update_6_0_4_nacos_console_menu()
BEGIN
    DECLARE removed_menu_ids TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '';

    DROP TEMPORARY TABLE IF EXISTS tmp_update_6_0_4_removed_menu_ids;
    CREATE TEMPORARY TABLE tmp_update_6_0_4_removed_menu_ids (
        menu_id bigint(20) NOT NULL,
        PRIMARY KEY (menu_id)
    );

    INSERT IGNORE INTO tmp_update_6_0_4_removed_menu_ids (menu_id)
    WITH RECURSIVE menu_tree AS (
        SELECT menu_id
        FROM sys_menu
        WHERE menu_id = 1761400000000000112
        UNION ALL
        SELECT child.menu_id
        FROM sys_menu child
        INNER JOIN menu_tree parent
            ON child.parent_id = parent.menu_id
    )
    SELECT menu_id
    FROM menu_tree;

    SELECT COALESCE(GROUP_CONCAT(CAST(menu_id AS CHAR) ORDER BY menu_id SEPARATOR ','), '')
    INTO removed_menu_ids
    FROM tmp_update_6_0_4_removed_menu_ids;

    DELETE role_menu
    FROM sys_role_menu role_menu
    INNER JOIN tmp_update_6_0_4_removed_menu_ids removed_menu
        ON role_menu.menu_id = removed_menu.menu_id;

    UPDATE sys_tenant_package tenant_package
    SET menu_ids = (
        SELECT COALESCE(
            GROUP_CONCAT(CAST(menu.menu_id AS CHAR)
                ORDER BY FIND_IN_SET(
                    CAST(menu.menu_id AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_general_ci,
                    tenant_package.menu_ids COLLATE utf8mb4_general_ci
                )
                SEPARATOR ','),
            ''
        )
        FROM sys_menu menu
        WHERE FIND_IN_SET(
            CAST(menu.menu_id AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_general_ci,
            tenant_package.menu_ids COLLATE utf8mb4_general_ci
        ) > 0
          AND FIND_IN_SET(
              CAST(menu.menu_id AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_general_ci,
              removed_menu_ids
          ) = 0
    )
    WHERE EXISTS (
        SELECT 1
        FROM sys_menu package_menu
        WHERE FIND_IN_SET(
            CAST(package_menu.menu_id AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_general_ci,
            tenant_package.menu_ids COLLATE utf8mb4_general_ci
        ) > 0
          AND FIND_IN_SET(
              CAST(package_menu.menu_id AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_general_ci,
              removed_menu_ids
          ) > 0
    );

    DELETE menu
    FROM sys_menu menu
    INNER JOIN tmp_update_6_0_4_removed_menu_ids removed_menu
        ON menu.menu_id = removed_menu.menu_id;

    DROP TEMPORARY TABLE IF EXISTS tmp_update_6_0_4_removed_menu_ids;
END //
DELIMITER ;

START TRANSACTION;
CALL remove_update_6_0_4_nacos_console_menu();
COMMIT;
DROP PROCEDURE remove_update_6_0_4_nacos_console_menu;
