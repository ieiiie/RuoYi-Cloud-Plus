-- Tenant ids: every tenant-aware domain table is rewritten through the explicit map.
-- The runner expands ${AGRICULTURE_TENANT_UPDATE_SQL} and ${IOT_TENANT_UPDATE_SQL}
-- from information_schema; no default tenant is substituted.
${AGRICULTURE_TENANT_UPDATE_SQL}
${IOT_TENANT_UPDATE_SQL}

-- Employee business identity belongs to agriculture. User/audit columns are
-- expanded by the runner only when the target table actually has that column.
${AGRICULTURE_USER_UPDATE_SQL}
${IOT_USER_UPDATE_SQL}
${AGRICULTURE_OSS_UPDATE_SQL}

-- Every source tenant/user/OSS reference must have a mapping.
SELECT 'tenant_map_missing' check_name, COUNT(*) problem_count
FROM `${MAPPING_DB}`.`tenant_map` WHERE target_tenant_id IS NULL
UNION ALL
SELECT 'user_map_missing', COUNT(*) FROM `${MAPPING_DB}`.`user_map` WHERE target_user_id IS NULL
UNION ALL
SELECT 'oss_map_missing', COUNT(*) FROM `${MAPPING_DB}`.`oss_map` WHERE target_oss_id IS NULL;
