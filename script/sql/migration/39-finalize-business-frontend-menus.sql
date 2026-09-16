-- Finalize the migrated agriculture/IoT menu tree for the two real micro apps.
-- Authoritative source: ym-test sys_menu exported on 2026-09-03.
-- Exact expected result after deprecated material-inventory removal:
-- M=12, C=50, F=70, total=132.
SET NAMES utf8mb4;

SET @agriculture_app_id = (
    SELECT app_id FROM `${TARGET_PLATFORM_DB}`.`sys_app`
    WHERE app_key = 'agriculture' AND del_flag = '0' LIMIT 1
);
SET @iot_app_id = (
    SELECT app_id FROM `${TARGET_PLATFORM_DB}`.`sys_app`
    WHERE app_key = 'iot' AND del_flag = '0' LIMIT 1
);

CREATE TEMPORARY TABLE tmp_frontend_menu_guard (
    problem_count INT NOT NULL CHECK (problem_count = 0)
);
INSERT INTO tmp_frontend_menu_guard(problem_count)
SELECT IF(@agriculture_app_id IS NULL OR @iot_app_id IS NULL, 1, 0);

CREATE TEMPORARY TABLE tmp_deprecated_inventory_menu (
    menu_id BIGINT PRIMARY KEY
);
INSERT INTO tmp_deprecated_inventory_menu(menu_id) VALUES
    (207220000000000001),
    (207220000000000002),
    (207220000000000003),
    (207220000000000004),
    (207220000000000005),
    (207220000000000006);

SELECT 'deprecated_menu_before_delete' AS check_name, COUNT(*) AS affected_count
FROM `${TARGET_PLATFORM_DB}`.`sys_menu` menu
JOIN tmp_deprecated_inventory_menu deprecated ON deprecated.menu_id = menu.menu_id;
SELECT 'deprecated_role_grant_before_delete' AS check_name, COUNT(*) AS affected_count
FROM `${TARGET_PLATFORM_DB}`.`sys_role_menu` relation_row
JOIN tmp_deprecated_inventory_menu deprecated ON deprecated.menu_id = relation_row.menu_id;
SELECT 'deprecated_template_grant_before_delete' AS check_name, COUNT(*) AS affected_count
FROM `${TARGET_PLATFORM_DB}`.`sys_role_template_menu` relation_row
JOIN tmp_deprecated_inventory_menu deprecated ON deprecated.menu_id = relation_row.menu_id;
SELECT 'deprecated_package_grant_before_delete' AS check_name, COUNT(*) AS affected_count
FROM `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` relation_row
JOIN tmp_deprecated_inventory_menu deprecated ON deprecated.menu_id = relation_row.menu_id;

START TRANSACTION;

DELETE relation_row
FROM `${TARGET_PLATFORM_DB}`.`sys_role_menu` relation_row
JOIN tmp_deprecated_inventory_menu deprecated ON deprecated.menu_id = relation_row.menu_id;

DELETE relation_row
FROM `${TARGET_PLATFORM_DB}`.`sys_role_template_menu` relation_row
JOIN tmp_deprecated_inventory_menu deprecated ON deprecated.menu_id = relation_row.menu_id;

DELETE relation_row
FROM `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` relation_row
JOIN tmp_deprecated_inventory_menu deprecated ON deprecated.menu_id = relation_row.menu_id;

DELETE menu
FROM `${TARGET_PLATFORM_DB}`.`sys_menu` menu
JOIN tmp_deprecated_inventory_menu deprecated ON deprecated.menu_id = menu.menu_id;

UPDATE `${TARGET_PLATFORM_DB}`.`sys_menu`
SET path = 'bigscreen',
    component = 'agriculture/bigscreen',
    is_frame = 'N',
    query_param = '',
    active_menu = '',
    update_time = NOW(),
    remark = '智慧大棚大屏；农业微应用内部 /bigscreen 全屏路由'
WHERE menu_id = 2079841603903594497
  AND app_id = @agriculture_app_id;

UPDATE `${TARGET_PLATFORM_DB}`.`sys_menu`
SET component = 'smartfarming/farming/farm-records/index',
    update_time = NOW()
WHERE menu_id = 2063892453664067586
  AND app_id = @agriculture_app_id;

COMMIT;

SELECT 'deprecated_menu_after_delete' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_menu` menu
JOIN tmp_deprecated_inventory_menu deprecated ON deprecated.menu_id = menu.menu_id;
SELECT 'deprecated_role_grant_after_delete' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_role_menu` relation_row
JOIN tmp_deprecated_inventory_menu deprecated ON deprecated.menu_id = relation_row.menu_id;
SELECT 'deprecated_template_grant_after_delete' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_role_template_menu` relation_row
JOIN tmp_deprecated_inventory_menu deprecated ON deprecated.menu_id = relation_row.menu_id;
SELECT 'deprecated_package_grant_after_delete' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` relation_row
JOIN tmp_deprecated_inventory_menu deprecated ON deprecated.menu_id = relation_row.menu_id;

SELECT 'business_menu_type_count' AS check_name, menu.menu_type, COUNT(*) AS actual_count,
       CASE menu.menu_type WHEN 'M' THEN 12 WHEN 'C' THEN 50 WHEN 'F' THEN 70 END AS expected_count
FROM `${TARGET_PLATFORM_DB}`.`sys_menu` menu
WHERE menu.app_id IN (@agriculture_app_id, @iot_app_id)
GROUP BY menu.menu_type
ORDER BY menu.menu_type;

SELECT 'business_menu_total' AS check_name, COUNT(*) AS actual_count, 132 AS expected_count
FROM `${TARGET_PLATFORM_DB}`.`sys_menu`
WHERE app_id IN (@agriculture_app_id, @iot_app_id);

SELECT 'business_menu_orphan' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_menu` child
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` parent ON parent.menu_id = child.parent_id
WHERE child.app_id IN (@agriculture_app_id, @iot_app_id)
  AND child.parent_id <> 0
  AND parent.menu_id IS NULL;

SELECT 'business_page_without_component' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_menu`
WHERE app_id IN (@agriculture_app_id, @iot_app_id)
  AND menu_type = 'C'
  AND COALESCE(TRIM(component), '') = '';

SELECT 'bigscreen_internal_route' AS check_name, COUNT(*) AS actual_count, 1 AS expected_count
FROM `${TARGET_PLATFORM_DB}`.`sys_menu`
WHERE menu_id = 2079841603903594497
  AND app_id = @agriculture_app_id
  AND path = 'bigscreen'
  AND component = 'agriculture/bigscreen'
  AND is_frame = 'N';

SELECT 'role_permission_outside_package' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_role_menu` role_menu
JOIN `${TARGET_PLATFORM_DB}`.`sys_role` role_row ON role_row.role_id = role_menu.role_id
JOIN `${TARGET_PLATFORM_DB}`.`sys_tenant` tenant ON tenant.tenant_id = role_row.tenant_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` package_menu
  ON package_menu.package_id = tenant.package_id
 AND package_menu.menu_id = role_menu.menu_id
WHERE package_menu.menu_id IS NULL;

DROP TEMPORARY TABLE tmp_deprecated_inventory_menu;
DROP TEMPORARY TABLE tmp_frontend_menu_guard;
