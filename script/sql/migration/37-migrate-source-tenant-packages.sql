-- Migrate legacy tenant packages whose menu permissions are stored in menu_ids.
-- The target keeps its normalized package/app and package/menu relation tables.
-- Prerequisite: 35-migrate-business-menu-tree.sql has migrated the complete business tree.
SET NAMES utf8mb4;

SET @saas_app_id = (
    SELECT app_id FROM `${TARGET_PLATFORM_DB}`.`sys_app`
    WHERE app_key = 'saas-core' AND del_flag = '0' LIMIT 1
);
SET @agriculture_app_id = (
    SELECT app_id FROM `${TARGET_PLATFORM_DB}`.`sys_app`
    WHERE app_key = 'agriculture' AND del_flag = '0' LIMIT 1
);
SET @iot_app_id = (
    SELECT app_id FROM `${TARGET_PLATFORM_DB}`.`sys_app`
    WHERE app_key = 'iot' AND del_flag = '0' LIMIT 1
);

CREATE TEMPORARY TABLE tmp_source_package_guard (
    problem_count INT NOT NULL CHECK (problem_count = 0)
);
INSERT INTO tmp_source_package_guard(problem_count)
SELECT IF(@saas_app_id IS NULL OR @agriculture_app_id IS NULL OR @iot_app_id IS NULL, 1, 0);

CREATE TEMPORARY TABLE tmp_source_package_menu AS
SELECT p.package_id, p.package_name, p.menu_check_strictly, p.status, p.del_flag,
       p.create_dept, p.create_by, p.create_time, p.update_by, p.update_time,
       p.remark, j.menu_id AS source_menu_id
FROM `${SOURCE_PLATFORM_DB}`.`sys_tenant_package` p
JOIN JSON_TABLE(
    CONCAT('["', REPLACE(p.menu_ids, ',', '","'), '"]'),
    '$[*]' COLUMNS(menu_id BIGINT PATH '$')
) j;

CREATE TEMPORARY TABLE tmp_source_business_menu AS
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
          'home-overview', 'organization', 'smartfarming', 'farm', 'monitorcenter',
          'inventory', 'quality', 'badge', 'uav', 'remotecontrol', 'alert', 'iot'
      )
    UNION ALL
    SELECT c.menu_id, c.parent_id, p.target_app_id
    FROM `${SOURCE_PLATFORM_DB}`.`sys_menu` c
    JOIN source_tree p ON p.menu_id = c.parent_id
)
SELECT * FROM source_tree;

-- MySQL cannot reopen one temporary table in multiple UNION branches.
CREATE TEMPORARY TABLE tmp_source_business_menu_for_filter AS
SELECT * FROM tmp_source_business_menu;

-- Directly preserve business menu ids and map legacy platform ids to the
-- target Snowflake-id equivalents that still exist in saas-core.
CREATE TEMPORARY TABLE tmp_target_package_menu AS
SELECT DISTINCT spm.package_id, target_menu.source_menu_id,
       target_menu.menu_id, target_menu.app_id
FROM tmp_source_package_menu spm
JOIN (
    SELECT source_menu.menu_id AS source_menu_id, target_menu.menu_id, target_menu.app_id
    FROM tmp_source_business_menu source_menu
    JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` target_menu
      ON target_menu.menu_id = source_menu.menu_id
     AND target_menu.app_id = source_menu.target_app_id

    UNION ALL

    SELECT source_menu.menu_id AS source_menu_id, target_menu.menu_id, target_menu.app_id
    FROM `${SOURCE_PLATFORM_DB}`.`sys_menu` source_menu
    JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` target_menu
      ON target_menu.menu_id = 1761400000000000000 + source_menu.menu_id
     AND target_menu.app_id = @saas_app_id
    LEFT JOIN tmp_source_business_menu_for_filter business_menu
      ON business_menu.menu_id = source_menu.menu_id
    WHERE business_menu.menu_id IS NULL

    UNION ALL

    SELECT source_menu.menu_id AS source_menu_id, target_menu.menu_id, target_menu.app_id
    FROM `${SOURCE_PLATFORM_DB}`.`sys_menu` source_menu
    JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` target_menu
      ON target_menu.app_id = @saas_app_id
     AND target_menu.perms = CASE source_menu.perms
         WHEN 'system:tenantDict:list' THEN 'system:tenantDict:list'
         WHEN 'system:tenantDict:query' THEN 'system:tenantDict:query'
         WHEN 'system:tenantDictType:add' THEN 'system:tenantDict:add'
         WHEN 'system:tenantDictData:add' THEN 'system:tenantDict:add'
         WHEN 'system:tenantDictType:edit' THEN 'system:tenantDict:edit'
         WHEN 'system:tenantDictData:edit' THEN 'system:tenantDict:edit'
         WHEN 'system:tenantDictType:remove' THEN 'system:tenantDict:remove'
         WHEN 'system:tenantDictData:remove' THEN 'system:tenantDict:remove'
         ELSE '__unsupported__'
     END
    WHERE source_menu.menu_id = 2608187000000000001
       OR source_menu.parent_id = 2608187000000000001
) target_menu ON target_menu.source_menu_id = spm.source_menu_id;

START TRANSACTION;

INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_tenant_package`
    (`package_id`, `package_name`, `menu_check_strictly`, `status`, `del_flag`,
     `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT p.package_id, p.package_name, p.menu_check_strictly, p.status, p.del_flag,
       NULL, NULL, p.create_time, NULL, p.update_time, p.remark
FROM `${SOURCE_PLATFORM_DB}`.`sys_tenant_package` p
ON DUPLICATE KEY UPDATE
    `package_name` = VALUES(`package_name`),
    `menu_check_strictly` = VALUES(`menu_check_strictly`),
    `status` = VALUES(`status`),
    `del_flag` = VALUES(`del_flag`),
    `update_time` = VALUES(`update_time`),
    `remark` = VALUES(`remark`);

DELETE target_menu
FROM `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` target_menu
JOIN `${SOURCE_PLATFORM_DB}`.`sys_tenant_package` source_package
  ON source_package.package_id = target_menu.package_id;

INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` (`package_id`, `menu_id`)
SELECT DISTINCT package_id, menu_id FROM tmp_target_package_menu;

DELETE target_app
FROM `${TARGET_PLATFORM_DB}`.`sys_tenant_package_app` target_app
JOIN `${SOURCE_PLATFORM_DB}`.`sys_tenant_package` source_package
  ON source_package.package_id = target_app.package_id;

INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_tenant_package_app` (`package_id`, `app_id`)
SELECT DISTINCT package_id, app_id FROM tmp_target_package_menu;

COMMIT;

SELECT 'source_package_menu_mapping' AS check_name,
       source_package.package_id, source_package.package_name,
       COUNT(DISTINCT source_menu.source_menu_id) AS source_value,
       COUNT(DISTINCT target_menu.menu_id) AS target_value,
       COUNT(DISTINCT source_menu.source_menu_id) -
           COUNT(DISTINCT target_menu.source_menu_id) AS unsupported_source_value
FROM `${SOURCE_PLATFORM_DB}`.`sys_tenant_package` source_package
JOIN tmp_source_package_menu source_menu
  ON source_menu.package_id = source_package.package_id
LEFT JOIN tmp_target_package_menu target_menu
  ON target_menu.package_id = source_menu.package_id
 AND target_menu.source_menu_id = source_menu.source_menu_id
GROUP BY source_package.package_id, source_package.package_name
ORDER BY source_package.package_id;

SELECT 'unsupported_source_package_menu' AS check_name,
       source_menu.package_id, source_menu.source_menu_id,
       menu.menu_name, menu.path, menu.perms
FROM tmp_source_package_menu source_menu
JOIN `${SOURCE_PLATFORM_DB}`.`sys_menu` menu
  ON menu.menu_id = source_menu.source_menu_id
LEFT JOIN tmp_target_package_menu target_menu
  ON target_menu.package_id = source_menu.package_id
 AND target_menu.source_menu_id = source_menu.source_menu_id
WHERE target_menu.menu_id IS NULL
ORDER BY source_menu.package_id, source_menu.source_menu_id;

SELECT 'package_menu_orphan' AS check_name, COUNT(*) AS problem_count
FROM `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` package_menu
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_tenant_package` package_row
  ON package_row.package_id = package_menu.package_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` menu
  ON menu.menu_id = package_menu.menu_id
WHERE package_row.package_id IS NULL OR menu.menu_id IS NULL;

SELECT package_row.package_id, package_row.package_name,
       COUNT(DISTINCT package_menu.menu_id) AS menu_count,
       GROUP_CONCAT(DISTINCT app.app_key ORDER BY app.order_num, app.app_key) AS app_keys
FROM `${TARGET_PLATFORM_DB}`.`sys_tenant_package` package_row
JOIN `${SOURCE_PLATFORM_DB}`.`sys_tenant_package` source_package
  ON source_package.package_id = package_row.package_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_tenant_package_menu` package_menu
  ON package_menu.package_id = package_row.package_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_tenant_package_app` package_app
  ON package_app.package_id = package_row.package_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_app` app ON app.app_id = package_app.app_id
GROUP BY package_row.package_id, package_row.package_name
ORDER BY package_row.package_id;

DROP TEMPORARY TABLE tmp_target_package_menu;
DROP TEMPORARY TABLE tmp_source_business_menu_for_filter;
DROP TEMPORARY TABLE tmp_source_business_menu;
DROP TEMPORARY TABLE tmp_source_package_menu;
DROP TEMPORARY TABLE tmp_source_package_guard;
