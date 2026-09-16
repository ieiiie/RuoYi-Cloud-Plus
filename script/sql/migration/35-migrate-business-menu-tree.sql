-- Preserve the V1.5 agriculture and IoT menu trees from the source platform.
-- Platform/auth/tenant/monitor/tool menus stay owned by the target saas-core app.
-- Prerequisite: update_6.0.6-agriculture-iot-apps.sql has created both sys_app rows.
SET NAMES utf8mb4;

SET @agriculture_app_id = (
    SELECT app_id FROM `${TARGET_PLATFORM_DB}`.`sys_app`
    WHERE app_key = 'agriculture' AND del_flag = '0' LIMIT 1
);
SET @iot_app_id = (
    SELECT app_id FROM `${TARGET_PLATFORM_DB}`.`sys_app`
    WHERE app_key = 'iot' AND del_flag = '0' LIMIT 1
);

-- A failed guard INSERT stops the script before persistent rows are changed.
CREATE TEMPORARY TABLE tmp_business_menu_guard (
    problem_count INT NOT NULL CHECK (problem_count = 0)
);
INSERT INTO tmp_business_menu_guard(problem_count)
SELECT IF(@agriculture_app_id IS NULL OR @iot_app_id IS NULL, 1, 0);

CREATE TEMPORARY TABLE tmp_source_business_menu AS
WITH RECURSIVE source_tree AS (
    SELECT s.menu_id, s.menu_name, s.parent_id, s.order_num, s.path, s.component,
           s.query_param, s.is_frame, s.is_cache, s.menu_type, s.visible, s.status,
           s.perms, s.icon, s.create_time, s.update_time, s.remark,
           CASE
               WHEN s.path IN ('remotecontrol', 'alert', 'iot') THEN @iot_app_id
               ELSE @agriculture_app_id
           END AS target_app_id
    FROM `${SOURCE_PLATFORM_DB}`.`sys_menu` s
    WHERE COALESCE(s.parent_id, 0) = 0
      AND s.path IN (
          'workspace', 'smartfarming/big-screen', 'http://localhost:5174/full-dashboard',
          'home-overview', 'organization', 'smartfarming', 'farm', 'monitorcenter', 'inventory',
          'quality', 'badge', 'uav', 'remotecontrol', 'alert', 'iot'
      )
    UNION ALL
    SELECT c.menu_id, c.menu_name, c.parent_id, c.order_num, c.path, c.component,
           c.query_param, c.is_frame, c.is_cache, c.menu_type, c.visible, c.status,
           c.perms, c.icon, c.create_time, c.update_time, c.remark, p.target_app_id
    FROM `${SOURCE_PLATFORM_DB}`.`sys_menu` c
    JOIN source_tree p ON p.menu_id = c.parent_id
)
SELECT *
FROM source_tree
WHERE menu_id NOT IN (
    207220000000000001,
    207220000000000002,
    207220000000000003,
    207220000000000004,
    207220000000000005,
    207220000000000006
);

-- The authoritative ym-test snapshot must contain exactly the effective
-- business tree after deprecated material-inventory is excluded.
INSERT INTO tmp_business_menu_guard(problem_count)
SELECT IF(
    COUNT(*) = 132
    AND SUM(menu_type = 'M') = 12
    AND SUM(menu_type = 'C') = 50
    AND SUM(menu_type = 'F') = 70,
    0,
    1
)
FROM tmp_source_business_menu;

-- Source ids are preserved. Refuse to overwrite an unrelated target-app menu.
INSERT INTO tmp_business_menu_guard(problem_count)
SELECT COUNT(*)
FROM tmp_source_business_menu s
JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` t ON t.menu_id = s.menu_id
WHERE t.app_id <> s.target_app_id;

INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_menu`
    (`menu_id`, `app_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
     `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`,
     `icon`, `active_menu`, `ext`, `create_dept`, `create_by`, `create_time`,
     `update_by`, `update_time`, `remark`)
SELECT s.menu_id, s.target_app_id, s.menu_name, s.parent_id, s.order_num,
       CASE WHEN s.menu_id = 2079841603903594497 THEN 'bigscreen' ELSE s.path END,
       CASE
           WHEN s.menu_id = 2079841603903594497 THEN 'agriculture/bigscreen'
           WHEN s.menu_id = 2063892453664067586 THEN 'smartfarming/farming/farm-records/index'
           ELSE s.component
       END,
       s.query_param,
       CASE
           WHEN s.menu_id = 2079841603903594497 THEN 'N'
           WHEN s.is_frame = 0 THEN 'Y'
           ELSE 'N'
       END,
       CASE WHEN s.is_cache = 0 THEN 'Y' ELSE 'N' END,
       s.menu_type, s.visible, s.status, s.perms, s.icon, '', '', NULL, NULL,
       s.create_time, NULL, s.update_time, s.remark
FROM tmp_source_business_menu s
ON DUPLICATE KEY UPDATE
    `app_id` = VALUES(`app_id`),
    `menu_name` = VALUES(`menu_name`),
    `parent_id` = VALUES(`parent_id`),
    `order_num` = VALUES(`order_num`),
    `path` = VALUES(`path`),
    `component` = VALUES(`component`),
    `query_param` = VALUES(`query_param`),
    `is_frame` = VALUES(`is_frame`),
    `is_cache` = VALUES(`is_cache`),
    `menu_type` = VALUES(`menu_type`),
    `visible` = VALUES(`visible`),
    `status` = VALUES(`status`),
    `perms` = VALUES(`perms`),
    `icon` = VALUES(`icon`),
    `remark` = VALUES(`remark`);

-- Validation for the 2026-09-03 authoritative ym-test snapshot: 132 rows,
-- split 109 agriculture and 23 IoT (including the organization directory).
SELECT 'business_menu_count' AS check_name,
       COUNT(*) AS source_value,
       SUM(t.menu_id IS NOT NULL) AS target_value
FROM tmp_source_business_menu s
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` t ON t.menu_id = s.menu_id;

-- M=directory, C=page menu, F=button permission. The target count must match each source type.
SELECT 'business_menu_type_count' AS check_name,
       s.menu_type,
       COUNT(*) AS source_value,
       SUM(t.menu_id IS NOT NULL AND t.menu_type = s.menu_type) AS target_value,
       SUM(s.menu_type = 'F' AND COALESCE(s.perms, '') <> '') AS source_button_permission_value,
       SUM(t.menu_type = 'F' AND COALESCE(t.perms, '') <> '') AS target_button_permission_value
FROM tmp_source_business_menu s
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` t ON t.menu_id = s.menu_id
GROUP BY s.menu_type
ORDER BY s.menu_type;

SELECT 'business_menu_field_mismatch' AS check_name, COUNT(*) AS problem_count
FROM tmp_source_business_menu s
JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` t ON t.menu_id = s.menu_id
WHERE t.app_id <> s.target_app_id
   OR NOT (t.menu_name <=> s.menu_name)
   OR NOT (t.parent_id <=> s.parent_id)
   OR NOT (t.order_num <=> s.order_num)
   OR NOT (t.path <=> CASE WHEN s.menu_id = 2079841603903594497 THEN 'bigscreen' ELSE s.path END)
   OR NOT (t.component <=> CASE
       WHEN s.menu_id = 2079841603903594497 THEN 'agriculture/bigscreen'
       WHEN s.menu_id = 2063892453664067586 THEN 'smartfarming/farming/farm-records/index'
       ELSE s.component
   END)
   OR NOT (t.query_param <=> s.query_param)
   OR NOT (t.menu_type <=> s.menu_type)
   OR NOT (t.visible <=> s.visible)
   OR NOT (t.status <=> s.status)
   OR NOT (t.perms <=> s.perms)
   OR NOT (t.icon <=> s.icon)
   OR t.is_frame <> CASE
       WHEN s.menu_id = 2079841603903594497 THEN 'N'
       WHEN s.is_frame = 0 THEN 'Y'
       ELSE 'N'
   END
   OR t.is_cache <> CASE WHEN s.is_cache = 0 THEN 'Y' ELSE 'N' END;

SELECT 'business_menu_orphan' AS check_name, COUNT(*) AS problem_count
FROM tmp_source_business_menu s
JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` child ON child.menu_id = s.menu_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` parent ON parent.menu_id = child.parent_id
WHERE COALESCE(child.parent_id, 0) <> 0 AND parent.menu_id IS NULL;

SELECT a.app_key, COUNT(*) AS migrated_menu_count,
       SUM(s.parent_id = 0) AS migrated_root_count
FROM tmp_source_business_menu s
JOIN `${TARGET_PLATFORM_DB}`.`sys_app` a ON a.app_id = s.target_app_id
GROUP BY a.app_key ORDER BY a.app_key;

SELECT 'role_permission_outside_package' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_role_menu` rm
JOIN `${TARGET_PLATFORM_DB}`.`sys_role` r ON r.role_id = rm.role_id
JOIN `${TARGET_PLATFORM_DB}`.`sys_tenant` t ON t.tenant_id = r.tenant_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` pm
  ON pm.package_id = t.package_id AND pm.menu_id = rm.menu_id
WHERE pm.menu_id IS NULL;

DROP TEMPORARY TABLE tmp_source_business_menu;
DROP TEMPORARY TABLE tmp_business_menu_guard;
