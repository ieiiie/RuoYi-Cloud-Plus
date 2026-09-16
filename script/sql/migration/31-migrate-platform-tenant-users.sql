-- 增量迁移平台租户、用户及其最小可用身份关系。
-- 前置条件：10-mapping-schema.sql 与 20-build-identity-maps.sql 已执行且无冲突。
-- 目标库已有记录优先；源库中不存在租户主数据的用户不会迁移。

START TRANSACTION;

-- 已存在的全局账号如果已经加入目标租户，复用目标成员 ID。
UPDATE `${MAPPING_DB}`.`user_map` m
JOIN `${SOURCE_PLATFORM_DB}`.`sys_user` s ON s.user_id = m.old_user_id
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id = BINARY s.tenant_id
JOIN `${TARGET_PLATFORM_DB}`.`sys_user` t
  ON t.global_user_id = m.target_global_user_id
 AND BINARY t.tenant_id = BINARY tm.target_tenant_id
SET m.target_user_id = t.user_id
WHERE m.conflict_reason IS NULL
  AND tm.conflict_reason IS NULL;

-- 新租户沿用目标系统租户的默认套餐和目标端第一条 OSS 配置。
INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_tenant`
(`id`,`tenant_id`,`contact_user_name`,`contact_phone`,`company_name`,`license_number`,`address`,
 `province_code`,`city_code`,`district_code`,`region_name`,`longitude`,`latitude`,`domain`,`intro`,
 `package_id`,`oss_config_id`,`expire_time`,`account_count`,`status`,`del_flag`,`create_dept`,`create_by`,
 `create_time`,`update_by`,`update_time`,`remark`)
SELECT s.id, tm.target_tenant_id, COALESCE(NULLIF(s.contact_user_name, ''), s.company_name), s.contact_phone,
       s.company_name, s.license_number, s.address, s.province_code, s.city_code, s.district_code,
       s.region_name, s.longitude, s.latitude, s.domain, s.intro,
       COALESCE((SELECT package_id FROM `${TARGET_PLATFORM_DB}`.`sys_tenant`
                 WHERE tenant_id = '000000' LIMIT 1), s.package_id),
       (SELECT oss_config_id FROM `${TARGET_PLATFORM_DB}`.`sys_oss_config`
        ORDER BY oss_config_id LIMIT 1),
       s.expire_time, s.account_count, s.status, s.del_flag, s.create_dept,
       create_user.target_user_id, s.create_time, update_user.target_user_id, s.update_time, s.remark
FROM `${SOURCE_PLATFORM_DB}`.`sys_tenant` s
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id = BINARY s.tenant_id
LEFT JOIN `${MAPPING_DB}`.`user_map` create_user ON create_user.old_user_id = s.create_by
LEFT JOIN `${MAPPING_DB}`.`user_map` update_user ON update_user.old_user_id = s.update_by
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_tenant` t
  ON BINARY t.tenant_id = BINARY tm.target_tenant_id
WHERE t.tenant_id IS NULL
  AND tm.conflict_reason IS NULL;

-- 已存在租户只补空的行政区划与坐标，不覆盖目标端有效值。
UPDATE `${TARGET_PLATFORM_DB}`.`sys_tenant` t
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.target_tenant_id = BINARY t.tenant_id
JOIN `${SOURCE_PLATFORM_DB}`.`sys_tenant` s ON BINARY s.tenant_id = BINARY tm.old_tenant_id
SET t.address = COALESCE(t.address, s.address),
    t.province_code = COALESCE(t.province_code, s.province_code),
    t.city_code = COALESCE(t.city_code, s.city_code),
    t.district_code = COALESCE(t.district_code, s.district_code),
    t.region_name = COALESCE(t.region_name, s.region_name),
    t.longitude = COALESCE(t.longitude, s.longitude),
    t.latitude = COALESCE(t.latitude, s.latitude)
WHERE tm.conflict_reason IS NULL;

-- 已有租户可能仍指向旧环境的 OSS 配置 ID；仅在当前引用无效时使用目标端默认配置。
UPDATE `${TARGET_PLATFORM_DB}`.`sys_tenant` t
JOIN `${MAPPING_DB}`.`tenant_map` tm
  ON BINARY tm.target_tenant_id = BINARY t.tenant_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_oss_config` current_oss
  ON current_oss.oss_config_id = t.oss_config_id
JOIN (
  SELECT MIN(oss_config_id) AS oss_config_id
  FROM `${TARGET_PLATFORM_DB}`.`sys_oss_config`
) fallback_oss ON fallback_oss.oss_config_id IS NOT NULL
SET t.oss_config_id = fallback_oss.oss_config_id
WHERE tm.conflict_reason IS NULL
  AND current_oss.oss_config_id IS NULL;

-- 全局账号只为拥有有效租户映射的源成员创建。
INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_global_user`
(`global_user_id`,`user_name`,`nick_name`,`user_type`,`email`,`phone_number`,`gender`,`avatar`,`password`,
 `status`,`del_flag`,`create_dept`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT DISTINCT um.target_global_user_id, s.user_name, s.nick_name, COALESCE(s.user_type, 'sys_user'),
       NULLIF(TRIM(s.email), ''), NULLIF(TRIM(s.phonenumber), ''), s.sex, s.avatar, s.password,
       s.status, s.del_flag, s.create_dept, create_user.target_user_id, s.create_time,
       update_user.target_user_id, s.update_time, s.remark
FROM `${SOURCE_PLATFORM_DB}`.`sys_user` s
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id = BINARY s.tenant_id
JOIN `${MAPPING_DB}`.`user_map` um ON um.old_user_id = s.user_id
LEFT JOIN `${MAPPING_DB}`.`user_map` create_user ON create_user.old_user_id = s.create_by
LEFT JOIN `${MAPPING_DB}`.`user_map` update_user ON update_user.old_user_id = s.update_by
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_global_user` g
  ON g.global_user_id = um.target_global_user_id
WHERE g.global_user_id IS NULL
  AND tm.conflict_reason IS NULL
  AND um.conflict_reason IS NULL;

-- 部门主键保持不变；负责人和审计用户按用户映射转换。
INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_dept`
(`dept_id`,`tenant_id`,`parent_id`,`ancestors`,`dept_name`,`dept_category`,`order_num`,`leader`,`phone`,
 `email`,`status`,`del_flag`,`create_dept`,`create_by`,`create_time`,`update_by`,`update_time`)
SELECT d.dept_id, tm.target_tenant_id, d.parent_id, d.ancestors, d.dept_name, d.dept_category,
       d.order_num, leader_user.target_user_id, d.phone, d.email, d.status, d.del_flag, d.create_dept,
       create_user.target_user_id, d.create_time, update_user.target_user_id, d.update_time
FROM `${SOURCE_PLATFORM_DB}`.`sys_dept` d
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id = BINARY d.tenant_id
LEFT JOIN `${MAPPING_DB}`.`user_map` leader_user ON leader_user.old_user_id = d.leader
LEFT JOIN `${MAPPING_DB}`.`user_map` create_user ON create_user.old_user_id = d.create_by
LEFT JOIN `${MAPPING_DB}`.`user_map` update_user ON update_user.old_user_id = d.update_by
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_dept` target_dept ON target_dept.dept_id = d.dept_id
WHERE target_dept.dept_id IS NULL
  AND tm.conflict_reason IS NULL;

-- 角色按租户和 role_key 复用，否则保留源角色 ID。
INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_role`
(`role_id`,`template_id`,`template_version`,`is_builtin`,`tenant_deletable`,`tenant_id`,`role_name`,
 `role_key`,`role_sort`,`data_scope`,`menu_check_strictly`,`dept_check_strictly`,`status`,`del_flag`,
 `create_dept`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT rm.target_role_id, NULL, NULL, 0, IF(tm.target_tenant_id = '000000', 0, 1),
       tm.target_tenant_id, r.role_name, r.role_key, r.role_sort, r.data_scope,
       r.menu_check_strictly, r.dept_check_strictly, r.status, r.del_flag, r.create_dept,
       create_user.target_user_id, r.create_time, update_user.target_user_id, r.update_time, r.remark
FROM `${SOURCE_PLATFORM_DB}`.`sys_role` r
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id = BINARY r.tenant_id
JOIN `${MAPPING_DB}`.`role_map` rm ON rm.old_role_id = r.role_id
LEFT JOIN `${MAPPING_DB}`.`user_map` create_user ON create_user.old_user_id = r.create_by
LEFT JOIN `${MAPPING_DB}`.`user_map` update_user ON update_user.old_user_id = r.update_by
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_role` target_role ON target_role.role_id = rm.target_role_id
WHERE target_role.role_id IS NULL
  AND tm.conflict_reason IS NULL
  AND rm.conflict_reason IS NULL;

-- 本地租户成员；源部门不存在时 dept_id 置空，避免产生悬空引用。
INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_user`
(`user_id`,`tenant_id`,`global_user_id`,`dept_id`,`nick_name`,`user_type`,`email`,`gender`,`avatar`,`status`,
 `del_flag`,`login_ip`,`login_date`,`create_dept`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT um.target_user_id, tm.target_tenant_id, um.target_global_user_id,
       CASE WHEN source_dept.dept_id IS NULL THEN NULL ELSE s.dept_id END,
       s.nick_name, s.user_type, NULLIF(TRIM(s.email), ''), s.sex, s.avatar, s.status, s.del_flag,
       s.login_ip, s.login_date, s.create_dept, create_user.target_user_id, s.create_time,
       update_user.target_user_id, s.update_time, s.remark
FROM `${SOURCE_PLATFORM_DB}`.`sys_user` s
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id = BINARY s.tenant_id
JOIN `${MAPPING_DB}`.`user_map` um ON um.old_user_id = s.user_id
LEFT JOIN `${SOURCE_PLATFORM_DB}`.`sys_dept` source_dept ON source_dept.dept_id = s.dept_id
LEFT JOIN `${MAPPING_DB}`.`user_map` create_user ON create_user.old_user_id = s.create_by
LEFT JOIN `${MAPPING_DB}`.`user_map` update_user ON update_user.old_user_id = s.update_by
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_user` target_user ON target_user.user_id = um.target_user_id
WHERE target_user.user_id IS NULL
  AND tm.conflict_reason IS NULL
  AND um.conflict_reason IS NULL;

INSERT IGNORE INTO `${TARGET_PLATFORM_DB}`.`sys_user_role` (`user_id`, `role_id`)
SELECT um.target_user_id, rm.target_role_id
FROM `${SOURCE_PLATFORM_DB}`.`sys_user_role` ur
JOIN `${SOURCE_PLATFORM_DB}`.`sys_user` s ON s.user_id = ur.user_id
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id = BINARY s.tenant_id
JOIN `${MAPPING_DB}`.`user_map` um ON um.old_user_id = ur.user_id
JOIN `${MAPPING_DB}`.`role_map` rm ON rm.old_role_id = ur.role_id
JOIN `${TARGET_PLATFORM_DB}`.`sys_user` target_user ON target_user.user_id = um.target_user_id
JOIN `${TARGET_PLATFORM_DB}`.`sys_role` target_role ON target_role.role_id = rm.target_role_id
WHERE tm.conflict_reason IS NULL
  AND um.conflict_reason IS NULL
  AND rm.conflict_reason IS NULL;

INSERT IGNORE INTO `${TARGET_PLATFORM_DB}`.`sys_role_dept` (`role_id`, `dept_id`)
SELECT rm.target_role_id, rd.dept_id
FROM `${SOURCE_PLATFORM_DB}`.`sys_role_dept` rd
JOIN `${SOURCE_PLATFORM_DB}`.`sys_role` r ON r.role_id = rd.role_id
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id = BINARY r.tenant_id
JOIN `${MAPPING_DB}`.`role_map` rm ON rm.old_role_id = rd.role_id
JOIN `${TARGET_PLATFORM_DB}`.`sys_dept` target_dept ON target_dept.dept_id = rd.dept_id
JOIN `${TARGET_PLATFORM_DB}`.`sys_role` target_role ON target_role.role_id = rm.target_role_id
WHERE tm.conflict_reason IS NULL
  AND rm.conflict_reason IS NULL;

-- 只关联目标端已存在的菜单；不存在的旧菜单不创建悬空角色授权。
INSERT IGNORE INTO `${TARGET_PLATFORM_DB}`.`sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.target_role_id, mm.target_menu_id
FROM `${SOURCE_PLATFORM_DB}`.`sys_role_menu` source_role_menu
JOIN `${SOURCE_PLATFORM_DB}`.`sys_role` source_role ON source_role.role_id = source_role_menu.role_id
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id = BINARY source_role.tenant_id
JOIN `${MAPPING_DB}`.`role_map` rm ON rm.old_role_id = source_role_menu.role_id
JOIN `${MAPPING_DB}`.`menu_map` mm ON mm.old_menu_id = source_role_menu.menu_id
JOIN `${TARGET_PLATFORM_DB}`.`sys_role` target_role ON target_role.role_id = rm.target_role_id
JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` target_menu ON target_menu.menu_id = mm.target_menu_id
WHERE tm.conflict_reason IS NULL
  AND rm.conflict_reason IS NULL
  AND mm.conflict_reason IS NULL;

-- 旧社交账号转换到全局账号关系，只迁移拥有有效租户主数据的用户。
INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_global_social`
(`id`,`global_user_id`,`auth_id`,`source`,`open_id`,`user_name`,`nick_name`,`email`,`avatar`,`access_token`,
 `expire_in`,`refresh_token`,`access_code`,`union_id`,`scope`,`token_type`,`id_token`,`mac_algorithm`,`mac_key`,
 `code`,`oauth_token`,`oauth_token_secret`,`create_dept`,`create_by`,`create_time`,`update_by`,`update_time`,`del_flag`)
SELECT social.id, um.target_global_user_id, social.auth_id, social.source, social.open_id,
       social.user_name, social.nick_name, social.email, social.avatar, social.access_token,
       social.expire_in, social.refresh_token, social.access_code, social.union_id, social.scope,
       social.token_type, social.id_token, social.mac_algorithm, social.mac_key, social.code,
       social.oauth_token, social.oauth_token_secret, social.create_dept, create_user.target_user_id,
       social.create_time, update_user.target_user_id, social.update_time, social.del_flag
FROM `${SOURCE_PLATFORM_DB}`.`sys_social` social
JOIN `${SOURCE_PLATFORM_DB}`.`sys_user` source_user ON source_user.user_id = social.user_id
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id = BINARY source_user.tenant_id
JOIN `${MAPPING_DB}`.`user_map` um ON um.old_user_id = social.user_id
LEFT JOIN `${MAPPING_DB}`.`user_map` create_user ON create_user.old_user_id = social.create_by
LEFT JOIN `${MAPPING_DB}`.`user_map` update_user ON update_user.old_user_id = social.update_by
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_global_social` target_social
  ON BINARY target_social.open_id = BINARY social.open_id
WHERE target_social.id IS NULL
  AND tm.conflict_reason IS NULL
  AND um.conflict_reason IS NULL;

INSERT INTO `${TARGET_PLATFORM_DB}`.`sys_global_social`
(`id`,`global_user_id`,`auth_id`,`source`,`open_id`,`user_name`,`nick_name`,`access_token`,`del_flag`,
 `create_time`,`update_time`)
SELECT MIN(bind.id), MIN(um.target_global_user_id), CONCAT(bind.appid, ':', bind.openid),
       CONCAT('wechat_miniprogram:', bind.appid), bind.openid,
       MIN(global_user.user_name), MIN(global_user.nick_name), '', '0',
       MIN(bind.create_time), MAX(bind.update_time)
FROM `${SOURCE_PLATFORM_DB}`.`sys_user_wechat_bind` bind
JOIN `${SOURCE_PLATFORM_DB}`.`sys_user` source_user ON source_user.user_id = bind.user_id
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id = BINARY source_user.tenant_id
JOIN `${MAPPING_DB}`.`user_map` um ON um.old_user_id = bind.user_id
JOIN `${TARGET_PLATFORM_DB}`.`sys_global_user` global_user
  ON global_user.global_user_id = um.target_global_user_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_global_social` target_social
  ON BINARY target_social.open_id = BINARY bind.openid
WHERE target_social.id IS NULL
  AND tm.conflict_reason IS NULL
  AND um.conflict_reason IS NULL
GROUP BY bind.appid, bind.openid;

COMMIT;

SELECT 'source_tenants' check_name, COUNT(*) row_count
FROM `${SOURCE_PLATFORM_DB}`.`sys_tenant`
UNION ALL
SELECT 'mapped_tenants', COUNT(*) FROM `${MAPPING_DB}`.`tenant_map` WHERE conflict_reason IS NULL
UNION ALL
SELECT 'source_users', COUNT(*) FROM `${SOURCE_PLATFORM_DB}`.`sys_user`
UNION ALL
SELECT 'migratable_users', COUNT(*)
FROM `${SOURCE_PLATFORM_DB}`.`sys_user` source_user
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id = BINARY source_user.tenant_id
WHERE tm.conflict_reason IS NULL
UNION ALL
SELECT 'orphan_tenant_users', COUNT(*)
FROM `${SOURCE_PLATFORM_DB}`.`sys_user` source_user
LEFT JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id = BINARY source_user.tenant_id
WHERE tm.old_tenant_id IS NULL
UNION ALL
SELECT 'missing_target_menu_grants', COUNT(*)
FROM `${SOURCE_PLATFORM_DB}`.`sys_role_menu` source_role_menu
JOIN `${SOURCE_PLATFORM_DB}`.`sys_role` source_role ON source_role.role_id = source_role_menu.role_id
JOIN `${MAPPING_DB}`.`tenant_map` tm ON BINARY tm.old_tenant_id = BINARY source_role.tenant_id
JOIN `${MAPPING_DB}`.`menu_map` mm ON mm.old_menu_id = source_role_menu.menu_id
LEFT JOIN `${TARGET_PLATFORM_DB}`.`sys_menu` target_menu ON target_menu.menu_id = mm.target_menu_id
WHERE tm.conflict_reason IS NULL
  AND target_menu.menu_id IS NULL;
