-- 对存量租户补齐启用的全局参数。仅插入缺失记录，不覆盖租户自定义值。
-- 执行前备份 sys_config；在 SaaS 数据库运行，不在运营库 ry_dbo 运行。
START TRANSACTION;
SET @config_base = GREATEST((SELECT COALESCE(MAX(config_id), 0) FROM sys_config), 2099000000000000000);
INSERT INTO sys_config (config_id, definition_id, tenant_id, config_name, config_key,
 config_value, config_type, create_time, remark)
SELECT @config_base + ROW_NUMBER() OVER (ORDER BY t.tenant_id, d.definition_id),
 d.definition_id, t.tenant_id, d.config_name, d.config_key, d.default_value, 'Y', NOW(), d.remark
FROM sys_tenant t CROSS JOIN sys_config_definition d
WHERE t.del_flag = '0' AND d.del_flag = '0' AND d.status = '0' AND d.app_id IS NULL
 AND NOT EXISTS (SELECT 1 FROM sys_config c WHERE c.tenant_id = t.tenant_id
   AND (c.definition_id = d.definition_id OR c.config_key = d.config_key));
COMMIT;
