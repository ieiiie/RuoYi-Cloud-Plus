-- -----------------------------------------------------------------------------
-- RuoYi-Cloud-Plus 6.0.0 多租户升级脚本（MySQL）
--
-- 使用方式：
-- 1. 停止相关服务，保持 script/config/nacos/application-common.yml 中 tenant.enable=false。
-- 2. 先备份 ry-cloud 数据库，再执行本文件一次。
-- 3. 将 Nacos 的 tenant.enable 改为 true，重启全部服务并使用 tenantId=000000 登录。
--
-- 说明：已有数据会统一归入默认管理租户 000000；sys_menu、客户端、OSS 配置和
-- 关联表仍为平台全局数据，不添加 tenant_id。
-- -----------------------------------------------------------------------------

-- 业务表增加租户编号。默认值确保已有数据可无停机逻辑迁移到默认租户。
ALTER TABLE sys_social
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER id,
    ADD KEY idx_sys_social_tenant_id (tenant_id);

ALTER TABLE sys_dept
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER dept_id,
    ADD KEY idx_sys_dept_tenant_id (tenant_id);

ALTER TABLE sys_user
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER user_id,
    ADD KEY idx_sys_user_tenant_user_name (tenant_id, user_name),
    ADD KEY idx_sys_user_tenant_phone (tenant_id, phone_number);

ALTER TABLE sys_post
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER post_id,
    ADD KEY idx_sys_post_tenant_id (tenant_id);

ALTER TABLE sys_role
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER role_id,
    ADD KEY idx_sys_role_tenant_role_key (tenant_id, role_key);

ALTER TABLE sys_oper_log
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER oper_id,
    ADD KEY idx_sys_oper_log_tenant_time (tenant_id, oper_time);

-- 原 dict_type 是全局唯一；租户模式下应改为“同一租户内唯一”。
ALTER TABLE sys_dict_type
    DROP INDEX dict_type,
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER dict_id,
    ADD UNIQUE KEY uk_sys_dict_type_tenant_type (tenant_id, dict_type);

ALTER TABLE sys_dict_data
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER dict_code,
    ADD KEY idx_sys_dict_data_tenant_type (tenant_id, dict_type);

ALTER TABLE sys_config
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER config_id,
    ADD KEY idx_sys_config_tenant_key (tenant_id, config_key);

ALTER TABLE sys_login_info
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER info_id,
    ADD KEY idx_sys_login_info_tenant_time (tenant_id, login_time);

ALTER TABLE sys_notice
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER notice_id,
    ADD KEY idx_sys_notice_tenant_id (tenant_id);

ALTER TABLE sys_message
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER message_id,
    ADD KEY idx_sys_message_tenant_time (tenant_id, create_time);

ALTER TABLE sys_oss
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER oss_id,
    ADD KEY idx_sys_oss_tenant_id (tenant_id);

-- 平台租户主数据。
CREATE TABLE IF NOT EXISTS sys_tenant (
    id                bigint(20)      NOT NULL COMMENT '主键',
    tenant_id         varchar(20)     NOT NULL COMMENT '租户编号',
    contact_user_name varchar(50)     NOT NULL COMMENT '联系人',
    contact_phone     varchar(20)     NOT NULL COMMENT '联系电话',
    company_name      varchar(100)    NOT NULL COMMENT '企业名称',
    license_number    varchar(100)    DEFAULT NULL COMMENT '统一社会信用代码',
    address           varchar(255)    DEFAULT NULL COMMENT '地址',
    domain            varchar(255)    DEFAULT NULL COMMENT '域名',
    intro             varchar(1000)   DEFAULT NULL COMMENT '企业简介',
    package_id        bigint(20)      DEFAULT NULL COMMENT '租户套餐编号',
    expire_time       datetime        DEFAULT NULL COMMENT '过期时间',
    account_count     bigint(20)      NOT NULL DEFAULT -1 COMMENT '用户数量上限，-1表示不限制',
    status            char(1)         NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    del_flag          char(1)         NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    create_dept       bigint(20)      DEFAULT NULL COMMENT '创建部门',
    create_by         bigint(20)      DEFAULT NULL COMMENT '创建者',
    create_time       datetime        DEFAULT NULL COMMENT '创建时间',
    update_by         bigint(20)      DEFAULT NULL COMMENT '更新者',
    update_time       datetime        DEFAULT NULL COMMENT '更新时间',
    remark            varchar(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_tenant_tenant_id (tenant_id),
    KEY idx_sys_tenant_status (status)
) ENGINE=InnoDB COMMENT='租户表';

-- 平台套餐主数据。menu_ids 保存全局 sys_menu 主键列表。
CREATE TABLE IF NOT EXISTS sys_tenant_package (
    package_id           bigint(20)    NOT NULL COMMENT '套餐主键',
    package_name         varchar(100)  NOT NULL COMMENT '套餐名称',
    menu_ids             varchar(4000) DEFAULT '' COMMENT '关联菜单ID，逗号分隔',
    menu_check_strictly  tinyint(1)    NOT NULL DEFAULT 1 COMMENT '菜单树是否父子联动',
    status               char(1)       NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    del_flag             char(1)       NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    create_dept          bigint(20)    DEFAULT NULL COMMENT '创建部门',
    create_by            bigint(20)    DEFAULT NULL COMMENT '创建者',
    create_time          datetime      DEFAULT NULL COMMENT '创建时间',
    update_by            bigint(20)    DEFAULT NULL COMMENT '更新者',
    update_time          datetime      DEFAULT NULL COMMENT '更新时间',
    remark               varchar(500)  DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (package_id),
    KEY idx_sys_tenant_package_status (status)
) ENGINE=InnoDB COMMENT='租户套餐表';

-- 初始套餐在此时只包含升级前已有菜单；下面新增的“租户管理”菜单仅平台超级管理员可用。
SET SESSION group_concat_max_len = 8192;
INSERT INTO sys_tenant_package
    (package_id, package_name, menu_ids, menu_check_strictly, status, del_flag,
     create_dept, create_by, create_time, remark)
SELECT 1762000000000000001,
       '默认套餐',
       COALESCE(GROUP_CONCAT(CAST(menu_id AS CHAR) ORDER BY menu_id SEPARATOR ','), ''),
       1, '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), '升级自动创建的默认套餐'
FROM sys_menu
HAVING NOT EXISTS (SELECT 1 FROM sys_tenant_package WHERE package_id = 1762000000000000001);

INSERT INTO sys_tenant
    (id, tenant_id, contact_user_name, contact_phone, company_name, package_id,
     account_count, status, del_flag, create_dept, create_by, create_time, remark)
SELECT 1762000000000000002,
       '000000',
       '平台管理员',
       '15888888888',
       '默认管理租户',
       1762000000000000001,
       -1, '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(),
       '升级前已有系统数据所属的默认租户'
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant WHERE tenant_id = '000000');

-- 平台租户管理的菜单与按钮权限。前端若接入管理页，组件路径与现有系统菜单保持一致。
INSERT IGNORE INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time,
     update_by, update_time, remark)
VALUES
    (1761400000000001700, '租户管理', 1761400000000000001, 12, 'tenant', 'system/tenant/index', '', 'N', 'Y', 'C', '0', '0', 'system:tenant:list', 'company', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '平台租户管理菜单'),
    (1761400000000001701, '租户查询', 1761400000000001700, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001702, '租户新增', 1761400000000001700, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001703, '租户修改', 1761400000000001700, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001704, '租户删除', 1761400000000001700, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001705, '租户导出', 1761400000000001700, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001710, '租户套餐', 1761400000000000001, 13, 'tenant-package', 'system/tenant/package/index', '', 'N', 'Y', 'C', '0', '0', 'system:tenantPackage:list', 'price-tag', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '平台租户套餐菜单'),
    (1761400000000001711, '套餐查询', 1761400000000001710, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001712, '套餐新增', 1761400000000001710, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001713, '套餐修改', 1761400000000001710, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001714, '套餐删除', 1761400000000001710, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001715, '套餐导出', 1761400000000001710, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');

-- 默认超级管理员角色显式取得以上权限；租户管理员角色仍由套餐初始化，不会获得平台接口权限。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
    (1761300000000000001, 1761400000000001700),
    (1761300000000000001, 1761400000000001701),
    (1761300000000000001, 1761400000000001702),
    (1761300000000000001, 1761400000000001703),
    (1761300000000000001, 1761400000000001704),
    (1761300000000000001, 1761400000000001705),
    (1761300000000000001, 1761400000000001710),
    (1761300000000000001, 1761400000000001711),
    (1761300000000000001, 1761400000000001712),
    (1761300000000000001, 1761400000000001713),
    (1761300000000000001, 1761400000000001714),
    (1761300000000000001, 1761400000000001715);
