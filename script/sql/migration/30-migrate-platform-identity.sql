-- Must run only after 20-build-identity-maps.sql returned zero conflicts.
-- Existing platform rows win; missing tenant/global/local identities preserve source ids when safe.

INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_tenant`
(`id`,`tenant_id`,`contact_user_name`,`contact_phone`,`company_name`,`license_number`,`address`,
 `province_code`,`city_code`,`district_code`,`region_name`,`domain`,`intro`,`package_id`,`oss_config_id`,
 `expire_time`,`account_count`,`status`,`del_flag`,`create_dept`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT s.id, m.target_tenant_id, COALESCE(NULLIF(s.contact_user_name,''), s.company_name), s.contact_phone,
       s.company_name, s.license_number, s.address, s.province_code, s.city_code, s.district_code,
       s.region_name, s.domain, s.intro,
       COALESCE((SELECT package_id FROM `${TARGET_PLATFORM_DB}`.`sys_tenant` WHERE tenant_id='000000' LIMIT 1), s.package_id),
       (SELECT oss_config_id FROM `${TARGET_PLATFORM_DB}`.`sys_oss_config` ORDER BY oss_config_id LIMIT 1),
       s.expire_time, s.account_count, s.status, s.del_flag, s.create_dept, s.create_by, s.create_time,
       s.update_by, s.update_time, s.remark
FROM `${SOURCE_PLATFORM_DB}`.`sys_tenant` s
JOIN `${MAPPING_DB}`.`tenant_map` m ON BINARY m.old_tenant_id=BINARY s.tenant_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_tenant` t ON BINARY t.tenant_id=BINARY m.target_tenant_id
WHERE t.tenant_id IS NULL AND m.conflict_reason IS NULL;

INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_global_user`
(`global_user_id`,`user_name`,`nick_name`,`user_type`,`email`,`phone_number`,`gender`,`avatar`,`password`,
 `status`,`del_flag`,`create_dept`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT DISTINCT m.target_global_user_id, s.user_name, s.nick_name, COALESCE(s.user_type,'sys_user'),
       NULLIF(TRIM(s.email),''), NULLIF(TRIM(s.phonenumber),''), s.sex, s.avatar, s.password,
       s.status, s.del_flag, s.create_dept, s.create_by, s.create_time, s.update_by, s.update_time, s.remark
FROM `${SOURCE_PLATFORM_DB}`.`sys_user` s
JOIN `${MAPPING_DB}`.`user_map` m ON m.old_user_id=s.user_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_global_user` g ON g.global_user_id=m.target_global_user_id
WHERE g.global_user_id IS NULL AND m.conflict_reason IS NULL;

-- If the matched global account is already a member of the mapped tenant,
-- reuse that local member id instead of preserving the old id.
UPDATE `${MAPPING_DB}`.`user_map` m
JOIN `${SOURCE_PLATFORM_DB}`.`sys_user` s ON s.user_id=m.old_user_id
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id=BINARY s.tenant_id
JOIN `${TARGET_PLATFORM_DB}`.`sys_user` t
  ON t.global_user_id=m.target_global_user_id
 AND BINARY t.tenant_id=BINARY tm.target_tenant_id
SET m.target_user_id=t.user_id
WHERE m.conflict_reason IS NULL AND tm.conflict_reason IS NULL;

INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_user`
(`user_id`,`tenant_id`,`global_user_id`,`dept_id`,`nick_name`,`user_type`,`email`,`gender`,`avatar`,`status`,
 `del_flag`,`login_ip`,`login_date`,`create_dept`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT m.target_user_id, tm.target_tenant_id, m.target_global_user_id, s.dept_id, s.nick_name, s.user_type,
       NULLIF(TRIM(s.email),''), s.sex, s.avatar, s.status, s.del_flag, s.login_ip, s.login_date,
       s.create_dept, s.create_by, s.create_time, s.update_by, s.update_time, s.remark
FROM `${SOURCE_PLATFORM_DB}`.`sys_user` s
JOIN `${MAPPING_DB}`.`user_map` m ON m.old_user_id=s.user_id
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id=BINARY s.tenant_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_user` t ON t.user_id=m.target_user_id
WHERE t.user_id IS NULL AND m.conflict_reason IS NULL AND tm.conflict_reason IS NULL;

INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_oss`
(`oss_id`,`tenant_id`,`file_name`,`original_name`,`file_suffix`,`url`,`ext1`,`create_dept`,`create_time`,
 `create_by`,`update_time`,`update_by`,`service`)
SELECT om.target_oss_id, tm.target_tenant_id, s.file_name, s.original_name, s.file_suffix, s.url, s.ext1,
       s.create_dept, s.create_time, umc.target_user_id, s.update_time, umu.target_user_id, s.service
FROM `${SOURCE_PLATFORM_DB}`.`sys_oss` s
JOIN `${MAPPING_DB}`.`oss_map` om ON om.old_oss_id=s.oss_id
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id=BINARY s.tenant_id
LEFT JOIN `${MAPPING_DB}`.`user_map` umc ON umc.old_user_id=s.create_by
LEFT JOIN `${MAPPING_DB}`.`user_map` umu ON umu.old_user_id=s.update_by
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_oss` t ON t.oss_id=om.target_oss_id
WHERE t.oss_id IS NULL AND om.conflict_reason IS NULL AND tm.conflict_reason IS NULL;

-- Convert legacy social rows to the global account relation.
INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_global_social`
(`id`,`global_user_id`,`auth_id`,`source`,`open_id`,`user_name`,`nick_name`,`email`,`avatar`,`access_token`,
 `expire_in`,`refresh_token`,`access_code`,`union_id`,`scope`,`token_type`,`id_token`,`mac_algorithm`,`mac_key`,
 `code`,`oauth_token`,`oauth_token_secret`,`create_dept`,`create_by`,`create_time`,`update_by`,`update_time`,`del_flag`)
SELECT s.id, m.target_global_user_id, s.auth_id, s.source, s.open_id, s.user_name, s.nick_name, s.email, s.avatar,
       s.access_token, s.expire_in, s.refresh_token, s.access_code, s.union_id, s.scope, s.token_type, s.id_token,
       s.mac_algorithm, s.mac_key, s.code, s.oauth_token, s.oauth_token_secret, s.create_dept, s.create_by,
       s.create_time, s.update_by, s.update_time, s.del_flag
FROM `${SOURCE_PLATFORM_DB}`.`sys_social` s
JOIN `${MAPPING_DB}`.`user_map` m ON m.old_user_id=s.user_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_global_social` t ON BINARY t.open_id=BINARY s.open_id
WHERE t.id IS NULL AND m.conflict_reason IS NULL;

-- Legacy explicit WeChat binding has no access token. One openid maps to one global account;
-- a conflict query below blocks ambiguous cross-account bindings.
INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_global_social`
(`id`,`global_user_id`,`auth_id`,`source`,`open_id`,`user_name`,`nick_name`,`access_token`,`del_flag`,
 `create_time`,`update_time`)
SELECT MIN(w.id), MIN(m.target_global_user_id), CONCAT(w.appid, ':', w.openid),
       CONCAT('wechat_miniprogram:', w.appid), w.openid, MIN(g.user_name), MIN(g.nick_name), '', '0',
       MIN(w.create_time), MAX(w.update_time)
FROM `${SOURCE_PLATFORM_DB}`.`sys_user_wechat_bind` w
JOIN `${MAPPING_DB}`.`user_map` m ON m.old_user_id=w.user_id
JOIN `${TARGET_PLATFORM_DB}`.`sys_global_user` g ON g.global_user_id=m.target_global_user_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_global_social` t ON BINARY t.open_id=BINARY w.openid
WHERE t.id IS NULL AND m.conflict_reason IS NULL
GROUP BY w.appid,w.openid;

SELECT 'wechat_openid_multi_account' check_name, COUNT(*) problem_count
FROM (
  SELECT w.appid,w.openid
  FROM `${SOURCE_PLATFORM_DB}`.`sys_user_wechat_bind` w
  JOIN `${MAPPING_DB}`.`user_map` m ON m.old_user_id=w.user_id
  GROUP BY w.appid,w.openid HAVING COUNT(DISTINCT m.target_global_user_id) > 1
) conflicts;
