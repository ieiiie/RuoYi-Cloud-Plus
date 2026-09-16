-- Expand package, role and role-template grants after the complete business menu tree exists.
-- Run this script only after the target authorization scope has been explicitly approved.
SET NAMES utf8mb4;

SET @agriculture_app_id = (
    SELECT app_id FROM `${TARGET_PLATFORM_DB}`.`sys_app`
    WHERE app_key = 'agriculture' AND del_flag = '0' LIMIT 1
);
SET @iot_app_id = (
    SELECT app_id FROM `${TARGET_PLATFORM_DB}`.`sys_app`
    WHERE app_key = 'iot' AND del_flag = '0' LIMIT 1
);

CREATE TEMPORARY TABLE tmp_business_menu_grant_guard (
    problem_count INT NOT NULL CHECK (problem_count = 0)
);
INSERT INTO tmp_business_menu_grant_guard(problem_count)
SELECT IF(@agriculture_app_id IS NULL OR @iot_app_id IS NULL, 1, 0);

CREATE TEMPORARY TABLE tmp_source_business_menu_grant AS
WITH RECURSIVE source_tree AS (
    SELECT s.menu_id, s.parent_id,
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
    SELECT c.menu_id, c.parent_id, p.target_app_id
    FROM `${SOURCE_PLATFORM_DB}`.`sys_menu` c
    JOIN source_tree p ON p.menu_id = c.parent_id
)
SELECT * FROM source_tree;

-- Refuse authorization expansion if the complete menu rows have not been migrated first.
INSERT INTO tmp_business_menu_grant_guard(problem_count)
SELECT COUNT(*)
FROM tmp_source_business_menu_grant s
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` t
  ON t.menu_id = s.menu_id AND t.app_id = s.target_app_id
WHERE t.menu_id IS NULL;

CREATE TEMPORARY TABLE tmp_placeholder_menu_grant AS
SELECT menu_id, app_id
FROM `${TARGET_PLATFORM_DB}`.`sys_menu`
WHERE menu_id IN (
    1761400000000020000,
    1761400000000020010, 1761400000000020020, 1761400000000020030,
    1761400000000020040, 1761400000000020050, 1761400000000020060,
    1761400000000020070, 1761400000000020080, 1761400000000020090,
    1761400000000020100, 1761400000000020110, 1761400000000020120,
    1761400000000020130, 1761400000000020140,
    1761400000000030000,
    1761400000000030010, 1761400000000030020, 1761400000000030030,
    1761400000000030040, 1761400000000030050, 1761400000000030060,
    1761400000000030070, 1761400000000030080, 1761400000000030090
);

CREATE TEMPORARY TABLE tmp_placeholder_app_count_grant AS
SELECT app_id, COUNT(*) AS menu_count
FROM tmp_placeholder_menu_grant
GROUP BY app_id;

-- Only actors that already had every placeholder of an app receive its complete source tree.
-- Partial grants are deliberately left unchanged.
CREATE TEMPORARY TABLE tmp_full_package_app_grant AS
SELECT pm.package_id, p.app_id
FROM `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` pm
JOIN tmp_placeholder_menu_grant p ON p.menu_id = pm.menu_id
JOIN tmp_placeholder_app_count_grant pc ON pc.app_id = p.app_id
GROUP BY pm.package_id, p.app_id, pc.menu_count
HAVING COUNT(DISTINCT pm.menu_id) = pc.menu_count;

CREATE TEMPORARY TABLE tmp_full_role_app_grant AS
SELECT rm.role_id, p.app_id
FROM `${TARGET_PLATFORM_DB}`.`sys_role_menu` rm
JOIN tmp_placeholder_menu_grant p ON p.menu_id = rm.menu_id
JOIN tmp_placeholder_app_count_grant pc ON pc.app_id = p.app_id
GROUP BY rm.role_id, p.app_id, pc.menu_count
HAVING COUNT(DISTINCT rm.menu_id) = pc.menu_count;

CREATE TEMPORARY TABLE tmp_full_template_app_grant AS
SELECT tm.template_id, p.app_id
FROM `${TARGET_PLATFORM_DB}`.`sys_role_template_menu` tm
JOIN tmp_placeholder_menu_grant p ON p.menu_id = tm.menu_id
JOIN tmp_placeholder_app_count_grant pc ON pc.app_id = p.app_id
GROUP BY tm.template_id, p.app_id, pc.menu_count
HAVING COUNT(DISTINCT tm.menu_id) = pc.menu_count;

INSERT IGNORE INTO `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` (`package_id`, `menu_id`)
SELECT f.package_id, s.menu_id
FROM tmp_full_package_app_grant f
JOIN tmp_source_business_menu_grant s ON s.target_app_id = f.app_id;

INSERT IGNORE INTO `${TARGET_PLATFORM_DB}`.`sys_role_menu` (`role_id`, `menu_id`)
SELECT f.role_id, s.menu_id
FROM tmp_full_role_app_grant f
JOIN tmp_source_business_menu_grant s ON s.target_app_id = f.app_id;

INSERT IGNORE INTO `${TARGET_PLATFORM_DB}`.`sys_role_template_menu` (`template_id`, `menu_id`)
SELECT f.template_id, s.menu_id
FROM tmp_full_template_app_grant f
JOIN tmp_source_business_menu_grant s ON s.target_app_id = f.app_id;

-- Hide compatibility placeholders only after their complete replacement tree is authorized.
UPDATE `${TARGET_PLATFORM_DB}`.`sys_menu` m
JOIN tmp_placeholder_menu_grant p ON p.menu_id = m.menu_id
SET m.visible = '1',
    m.remark = CASE
        WHEN m.remark LIKE '%完整源菜单树迁移后的兼容占位%' THEN m.remark
        ELSE CONCAT_WS('；', NULLIF(m.remark, ''), '完整源菜单树迁移后的兼容占位')
    END;

SELECT 'expanded_package_app' AS check_name, COUNT(*) AS target_value
FROM tmp_full_package_app_grant;
SELECT 'expanded_role_app' AS check_name, COUNT(*) AS target_value
FROM tmp_full_role_app_grant;
SELECT 'expanded_role_template_app' AS check_name, COUNT(*) AS target_value
FROM tmp_full_template_app_grant;

SELECT 'visible_placeholder_menu' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_menu` m
JOIN tmp_placeholder_menu_grant p ON p.menu_id = m.menu_id
WHERE m.visible = '0';

SELECT 'role_permission_outside_package' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_role_menu` rm
JOIN `${TARGET_PLATFORM_DB}`.`sys_role` r ON r.role_id = rm.role_id
JOIN `${TARGET_PLATFORM_DB}`.`sys_tenant` t ON t.tenant_id = r.tenant_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` pm
  ON pm.package_id = t.package_id AND pm.menu_id = rm.menu_id
WHERE pm.menu_id IS NULL;

DROP TEMPORARY TABLE tmp_full_template_app_grant;
DROP TEMPORARY TABLE tmp_full_role_app_grant;
DROP TEMPORARY TABLE tmp_full_package_app_grant;
DROP TEMPORARY TABLE tmp_placeholder_app_count_grant;
DROP TEMPORARY TABLE tmp_placeholder_menu_grant;
DROP TEMPORARY TABLE tmp_source_business_menu_grant;
DROP TEMPORARY TABLE tmp_business_menu_grant_guard;
