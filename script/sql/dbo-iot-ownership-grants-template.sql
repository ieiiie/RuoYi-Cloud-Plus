-- REVIEW TEMPLATE ONLY: outputs grant SQL; does not create accounts or grant privileges itself.
-- First provision two new dedicated MySQL accounts through secret management. These names are placeholders.
-- Never reuse root, a role, or an account with global/schema-level writes: MySQL grants are additive.
SET @dbo_iot_grantee = '\'ym_dbo_iot\'@\'%\'';
SET @business_iot_grantee = '\'ym_iot_runtime\'@\'%\'';
-- Review existing privileges and SHOW GRANTS for both exact runtime identities before executing generated SQL.
SELECT * FROM information_schema.user_privileges WHERE grantee IN (@dbo_iot_grantee,@business_iot_grantee);
SELECT * FROM information_schema.schema_privileges WHERE grantee IN (@dbo_iot_grantee,@business_iot_grantee);
-- DBO profile read + SELECT FOR SHARE lock; FOR UPDATE applies only to ownership rows.
-- Never grant technical profile DML.
SELECT CONCAT('GRANT SELECT ON `ym-iot`.`iot_device` TO ',@dbo_iot_grantee,';') AS grant_sql;
SELECT CONCAT('GRANT SELECT, INSERT, UPDATE ON `ym-iot`.`',table_name,'` TO ',@dbo_iot_grantee,';') AS grant_sql
FROM information_schema.tables WHERE table_schema='ym-iot'
AND table_name IN ('iot_device_ownership','iot_device_ownership_history','iot_device_ownership_rule_cleanup');
-- Align with work/iot-responsibility-20260910/prepare-runtime.py narrow table grants.
-- Ownership is read-only. Excluded/retired tables have NO grants, including legacy alarm/log data.
SELECT CONCAT('GRANT SELECT ON `ym-iot`.`',table_name,'` TO ',@business_iot_grantee,';') AS grant_sql
FROM information_schema.tables WHERE table_schema='ym-iot' AND table_type='BASE TABLE'
AND table_name IN ('iot_device_ownership','iot_device_ownership_history','iot_device_ownership_rule_cleanup');
-- Remaining active business tables may be read, but this does not permit arbitrary DML.
SELECT CONCAT('GRANT SELECT ON `ym-iot`.`',table_name,'` TO ',@business_iot_grantee,';') AS grant_sql
FROM information_schema.tables WHERE table_schema='ym-iot' AND table_type='BASE TABLE'
AND table_name NOT IN (
    'iot_device_ownership','iot_device_ownership_history','iot_device_ownership_rule_cleanup',
    'iot_alert_rule','iot_alert_record','iot_device_log','iot_jetlinks_notification',
    'iot_device_ownership_migration','iot_data_point','iot_hfzk_user_device',
    'iot_isup_alarm_event','iot_isup_capture_record','iot_isup_config','iot_isup_stream_record')
AND LEFT(table_name,10) <> 'ym_before_' AND LEFT(table_name,8) <> 'retired_';
-- Only the event-maintained mirrors and retained business records below may be inserted/updated.
-- Never grant DELETE; catalog/ownership/history cannot be written by business forms.
SELECT CONCAT('GRANT INSERT, UPDATE ON `ym-iot`.`',table_name,'` TO ',@business_iot_grantee,';') AS grant_sql
FROM information_schema.tables WHERE table_schema='ym-iot' AND table_type='BASE TABLE'
AND table_name IN (
    'domain_job_execution_log','iot_device','iot_product','iot_product_property','iot_device_tag',
    'iot_jetlinks_category','iot_jetlinks_business_event','iot_jetlinks_projection',
    'iot_jetlinks_event_failure','iot_jetlinks_command_task','iot_fertilizer_control_log',
    'iot_fertilizer_record','iot_motorvalve_control_log','iot_motorvalve_valve_session');
SELECT CONCAT('GRANT SELECT ON `ym-agriculture`.`sf_field_iot` TO ',@business_iot_grantee,';') AS grant_sql;
-- Deploy ym-iot with the new business account and DBO iot datasource with the new writer account.
-- The DBO saas datasource retains SELECT on sys_tenant. Existing shared accounts are not modified here.
-- Acceptance MUST verify UPDATE/INSERT/DELETE on all three ownership tables fail for business credentials,
-- DBO credentials cannot UPDATE iot_device, and business cannot read old/retired alarm tables.
-- New grants do not revoke pre-existing privileges; only apply to fresh dedicated accounts or
-- separately review/remove obsolete grants. Do not run destructive acceptance on real ownership rows.
