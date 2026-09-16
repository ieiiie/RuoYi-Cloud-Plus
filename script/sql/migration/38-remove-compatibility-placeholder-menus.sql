-- Remove the temporary agriculture/IoT menu roots after the source menu trees exist.
-- Keep sys_app and package/app relations because the real source menus still use them.
SET NAMES utf8mb4;

CREATE TEMPORARY TABLE tmp_compatibility_menu_delete AS
WITH RECURSIVE menu_tree AS (
    SELECT menu_id
    FROM `${TARGET_PLATFORM_DB}`.`sys_menu`
    WHERE menu_id IN (1761400000000020000, 1761400000000030000)

    UNION ALL

    SELECT child.menu_id
    FROM `${TARGET_PLATFORM_DB}`.`sys_menu` child
    JOIN menu_tree parent ON parent.menu_id = child.parent_id
)
SELECT DISTINCT menu_id FROM menu_tree;

SELECT 'compatibility_menu_before_delete' AS check_name, COUNT(*) AS affected_count
FROM tmp_compatibility_menu_delete;
SELECT 'package_menu_before_delete' AS check_name, COUNT(*) AS affected_count
FROM `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` relation_row
JOIN tmp_compatibility_menu_delete menu ON menu.menu_id = relation_row.menu_id;
SELECT 'role_menu_before_delete' AS check_name, COUNT(*) AS affected_count
FROM `${TARGET_PLATFORM_DB}`.`sys_role_menu` relation_row
JOIN tmp_compatibility_menu_delete menu ON menu.menu_id = relation_row.menu_id;
SELECT 'role_template_menu_before_delete' AS check_name, COUNT(*) AS affected_count
FROM `${TARGET_PLATFORM_DB}`.`sys_role_template_menu` relation_row
JOIN tmp_compatibility_menu_delete menu ON menu.menu_id = relation_row.menu_id;

START TRANSACTION;

DELETE relation_row
FROM `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` relation_row
JOIN tmp_compatibility_menu_delete menu ON menu.menu_id = relation_row.menu_id;

DELETE relation_row
FROM `${TARGET_PLATFORM_DB}`.`sys_role_menu` relation_row
JOIN tmp_compatibility_menu_delete menu ON menu.menu_id = relation_row.menu_id;

DELETE relation_row
FROM `${TARGET_PLATFORM_DB}`.`sys_role_template_menu` relation_row
JOIN tmp_compatibility_menu_delete menu ON menu.menu_id = relation_row.menu_id;

DELETE target_menu
FROM `${TARGET_PLATFORM_DB}`.`sys_menu` target_menu
JOIN tmp_compatibility_menu_delete menu ON menu.menu_id = target_menu.menu_id;

COMMIT;

SELECT 'compatibility_menu_after_delete' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_menu`
WHERE menu_id IN (SELECT menu_id FROM tmp_compatibility_menu_delete);

SELECT 'package_menu_after_delete' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` relation_row
JOIN tmp_compatibility_menu_delete menu ON menu.menu_id = relation_row.menu_id;

SELECT 'role_menu_after_delete' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_role_menu` relation_row
JOIN tmp_compatibility_menu_delete menu ON menu.menu_id = relation_row.menu_id;

SELECT 'role_template_menu_after_delete' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_role_template_menu` relation_row
JOIN tmp_compatibility_menu_delete menu ON menu.menu_id = relation_row.menu_id;

SELECT 'menu_orphan' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_menu` child
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` parent ON parent.menu_id = child.parent_id
WHERE child.parent_id <> 0 AND parent.menu_id IS NULL;

SELECT 'package_app_gap' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` package_menu
JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` menu ON menu.menu_id = package_menu.menu_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_tenant_package_app` package_app
  ON package_app.package_id = package_menu.package_id
 AND package_app.app_id = menu.app_id
WHERE package_app.app_id IS NULL;

DROP TEMPORARY TABLE tmp_compatibility_menu_delete;
