-- Rebuild only mapping rows for the current source snapshot.
DELETE FROM `${MAPPING_DB}`.`tenant_map`;
DELETE FROM `${MAPPING_DB}`.`user_map`;
DELETE FROM `${MAPPING_DB}`.`role_map`;
DELETE FROM `${MAPPING_DB}`.`menu_map`;
DELETE FROM `${MAPPING_DB}`.`oss_map`;

INSERT INTO `${MAPPING_DB}`.`tenant_map`
(`old_tenant_id`, `target_tenant_id`, `match_rule`, `conflict_reason`)
SELECT s.tenant_id,
       COALESCE(by_id.tenant_id, by_license.tenant_id, s.tenant_id),
       CASE
         WHEN by_id.tenant_id IS NOT NULL THEN 'TENANT_ID'
         WHEN by_license.tenant_id IS NOT NULL THEN 'LICENSE_NUMBER'
         ELSE 'PRESERVE_ID'
       END,
       CASE WHEN by_id.tenant_id IS NOT NULL AND by_license.tenant_id IS NOT NULL
                  AND by_id.tenant_id <> by_license.tenant_id
            THEN CONCAT('tenant id/license match different targets: ', by_id.tenant_id, '/', by_license.tenant_id)
            ELSE NULL END
FROM `${SOURCE_PLATFORM_DB}`.`sys_tenant` s
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_tenant` by_id ON by_id.tenant_id = s.tenant_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_tenant` by_license
  ON NULLIF(TRIM(s.license_number), '') IS NOT NULL
 AND by_license.license_number = s.license_number;

INSERT INTO `${MAPPING_DB}`.`user_map`
(`old_user_id`, `old_global_user_id`, `target_global_user_id`, `target_user_id`, `match_rule`, `conflict_reason`)
WITH identity_hits AS (
  SELECT s.user_id, g.global_user_id, 'USERNAME' match_rule
  FROM `${SOURCE_PLATFORM_DB}`.`sys_user` s
  JOIN `${TARGET_PLATFORM_DB}`.`sys_global_user` g ON g.user_name = s.user_name
  UNION ALL
  SELECT s.user_id, g.global_user_id, 'PHONE'
  FROM `${SOURCE_PLATFORM_DB}`.`sys_user` s
  JOIN `${TARGET_PLATFORM_DB}`.`sys_global_user` g
    ON NULLIF(TRIM(s.phonenumber), '') IS NOT NULL AND g.phone_number = s.phonenumber
  UNION ALL
  SELECT s.user_id, g.global_user_id, 'EMAIL'
  FROM `${SOURCE_PLATFORM_DB}`.`sys_user` s
  JOIN `${TARGET_PLATFORM_DB}`.`sys_global_user` g
    ON NULLIF(TRIM(s.email), '') IS NOT NULL AND LOWER(g.email) = LOWER(s.email)
), hit_summary AS (
  SELECT user_id, COUNT(DISTINCT global_user_id) hit_count,
         MIN(global_user_id) global_user_id,
         GROUP_CONCAT(DISTINCT match_rule ORDER BY match_rule) match_rules
  FROM identity_hits GROUP BY user_id
)
SELECT s.user_id, s.global_user_id,
       CASE WHEN COALESCE(h.hit_count, 0) = 1 THEN h.global_user_id
            WHEN COALESCE(h.hit_count, 0) = 0 THEN COALESCE(s.global_user_id, s.user_id)
            ELSE NULL END,
       s.user_id,
       CASE WHEN COALESCE(h.hit_count, 0) = 1 THEN CONCAT('IDENTITY:', h.match_rules)
            WHEN COALESCE(h.hit_count, 0) = 0 THEN 'CREATE_GLOBAL_PRESERVE_ID'
            ELSE 'CONFLICT' END,
       CASE WHEN h.hit_count > 1 THEN CONCAT('username/phone/email matched ', h.hit_count, ' global accounts')
            ELSE NULL END
FROM `${SOURCE_PLATFORM_DB}`.`sys_user` s
LEFT JOIN hit_summary h ON h.user_id = s.user_id;

INSERT INTO `${MAPPING_DB}`.`role_map`
SELECT s.role_id, COALESCE(t.role_id, s.role_id),
       IF(t.role_id IS NULL, 'PRESERVE_ID', 'TENANT_AND_ROLE_KEY'), NULL
FROM `${SOURCE_PLATFORM_DB}`.`sys_role` s
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_role` t
  ON t.tenant_id = s.tenant_id AND t.role_key = s.role_key;

INSERT INTO `${MAPPING_DB}`.`menu_map`
SELECT s.menu_id, COALESCE(t.menu_id, s.menu_id),
       IF(t.menu_id IS NULL, 'PRESERVE_ID', 'PERMISSION'), NULL
FROM `${SOURCE_PLATFORM_DB}`.`sys_menu` s
LEFT JOIN (
  SELECT perms, MIN(menu_id) menu_id
  FROM `${TARGET_PLATFORM_DB}`.`sys_menu`
  WHERE NULLIF(TRIM(perms), '') IS NOT NULL
  GROUP BY perms
) t ON NULLIF(TRIM(s.perms), '') IS NOT NULL AND t.perms = s.perms;

INSERT INTO `${MAPPING_DB}`.`oss_map`
SELECT s.oss_id, COALESCE(t.oss_id, s.oss_id), SHA2(s.url, 256),
       IF(t.oss_id IS NULL, 'PRESERVE_ID', 'URL'), 'NOT_CHECKED', NULL
FROM `${SOURCE_PLATFORM_DB}`.`sys_oss` s
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_oss` t ON t.url = s.url;

-- A non-empty result is a hard stop. The runner refuses to continue.
SELECT 'TENANT' AS conflict_type, old_tenant_id AS source_id, conflict_reason
FROM `${MAPPING_DB}`.`tenant_map` WHERE conflict_reason IS NOT NULL
UNION ALL
SELECT 'USER', CAST(old_user_id AS CHAR), conflict_reason
FROM `${MAPPING_DB}`.`user_map` WHERE conflict_reason IS NOT NULL
UNION ALL
SELECT 'ROLE', CAST(old_role_id AS CHAR), conflict_reason
FROM `${MAPPING_DB}`.`role_map` WHERE conflict_reason IS NOT NULL
UNION ALL
SELECT 'MENU', CAST(old_menu_id AS CHAR), conflict_reason
FROM `${MAPPING_DB}`.`menu_map` WHERE conflict_reason IS NOT NULL
UNION ALL
SELECT 'OSS', CAST(old_oss_id AS CHAR), conflict_reason
FROM `${MAPPING_DB}`.`oss_map` WHERE conflict_reason IS NOT NULL;
