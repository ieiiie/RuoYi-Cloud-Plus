-- -----------------------------------------------------------------------------
-- YM-Cloud-Plus 6.0.0 多租户升级脚本（PostgreSQL）
-- 一次性升级脚本：执行前请备份数据库，并保持 tenant.enable=false。
-- -----------------------------------------------------------------------------

ALTER TABLE sys_social ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000';
CREATE INDEX idx_sys_social_tenant_id ON sys_social (tenant_id);

ALTER TABLE sys_dept ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000';
CREATE INDEX idx_sys_dept_tenant_id ON sys_dept (tenant_id);

ALTER TABLE sys_user ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000';

-- 全量初始化脚本已包含 global_user_id；旧库在执行 6.0.1 前没有此列。
-- 因此仅当该列已存在时创建成员关系唯一索引，旧库仍由 6.0.1 脚本创建。
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'sys_user'
          AND column_name = 'global_user_id'
    ) THEN
        EXECUTE 'CREATE UNIQUE INDEX uk_sys_user_tenant_global_user ON sys_user (tenant_id, global_user_id)';
    END IF;
END $$;

ALTER TABLE sys_post ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000';
CREATE INDEX idx_sys_post_tenant_id ON sys_post (tenant_id);

ALTER TABLE sys_role ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000';
CREATE INDEX idx_sys_role_tenant_role_key ON sys_role (tenant_id, role_key);

ALTER TABLE sys_oper_log ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000';
CREATE INDEX idx_sys_oper_log_tenant_time ON sys_oper_log (tenant_id, oper_time);

ALTER TABLE sys_dict_type ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000';
DROP INDEX sys_dict_type_index1;
CREATE UNIQUE INDEX uk_sys_dict_type_tenant_type ON sys_dict_type (tenant_id, dict_type);

ALTER TABLE sys_dict_data ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000';
CREATE INDEX idx_sys_dict_data_tenant_type ON sys_dict_data (tenant_id, dict_type);

ALTER TABLE sys_config ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000';
CREATE INDEX idx_sys_config_tenant_key ON sys_config (tenant_id, config_key);

ALTER TABLE sys_login_info ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000';
CREATE INDEX idx_sys_login_info_tenant_time ON sys_login_info (tenant_id, login_time);

ALTER TABLE sys_notice ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000';
CREATE INDEX idx_sys_notice_tenant_id ON sys_notice (tenant_id);

ALTER TABLE sys_message ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000';
CREATE INDEX idx_sys_message_tenant_time ON sys_message (tenant_id, create_time);

ALTER TABLE sys_oss ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000';
CREATE INDEX idx_sys_oss_tenant_id ON sys_oss (tenant_id);

CREATE TABLE sys_tenant (
    id                int8         NOT NULL,
    tenant_id         varchar(20)  NOT NULL,
    contact_user_name varchar(50)  NOT NULL,
    contact_phone     varchar(20)  NOT NULL,
    company_name      varchar(100) NOT NULL,
    license_number    varchar(100),
    address           varchar(255),
    domain            varchar(255),
    intro             varchar(1000),
    package_id        int8,
    expire_time       timestamp,
    account_count     int8         NOT NULL DEFAULT -1,
    status            char         NOT NULL DEFAULT '0',
    del_flag          char         NOT NULL DEFAULT '0',
    create_dept       int8,
    create_by         int8,
    create_time       timestamp,
    update_by         int8,
    update_time       timestamp,
    remark            varchar(500),
    CONSTRAINT sys_tenant_pk PRIMARY KEY (id),
    CONSTRAINT uk_sys_tenant_tenant_id UNIQUE (tenant_id)
);
CREATE INDEX idx_sys_tenant_status ON sys_tenant (status);

CREATE TABLE sys_tenant_package (
    package_id          int8          NOT NULL,
    package_name        varchar(100)  NOT NULL,
    menu_ids            varchar(4000) NOT NULL DEFAULT '',
    menu_check_strictly boolean       NOT NULL DEFAULT true,
    status              char          NOT NULL DEFAULT '0',
    del_flag            char          NOT NULL DEFAULT '0',
    create_dept         int8,
    create_by           int8,
    create_time         timestamp,
    update_by           int8,
    update_time         timestamp,
    remark              varchar(500),
    CONSTRAINT sys_tenant_package_pk PRIMARY KEY (package_id)
);
CREATE INDEX idx_sys_tenant_package_status ON sys_tenant_package (status);

INSERT INTO sys_tenant_package
    (package_id, package_name, menu_ids, menu_check_strictly, status, del_flag,
     create_dept, create_by, create_time, remark)
SELECT 1762000000000000001,
       '默认套餐',
       COALESCE((SELECT string_agg(menu_id::text, ',' ORDER BY menu_id) FROM sys_menu), ''),
       true, '0', '0', 1761000000000000103, 1761100000000000001, now(), '升级自动创建的默认套餐'
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package WHERE package_id = 1762000000000000001);

INSERT INTO sys_tenant
    (id, tenant_id, contact_user_name, contact_phone, company_name, package_id,
     account_count, status, del_flag, create_dept, create_by, create_time, remark)
SELECT 1762000000000000002,
       '000000', '平台管理员', '15888888888', '默认管理租户', 1762000000000000001,
       -1, '0', '0', 1761000000000000103, 1761100000000000001, now(),
       '升级前已有系统数据所属的默认租户'
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant WHERE tenant_id = '000000');

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time,
     update_by, update_time, remark)
VALUES
    (1761400000000001700, '租户管理', 1761400000000000001, 12, 'tenant', 'system/tenant/index', '', 'N', 'Y', 'C', '0', '0', 'system:tenant:list', 'company', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '平台租户管理菜单'),
    (1761400000000001701, '租户查询', 1761400000000001700, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:query', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, ''),
    (1761400000000001702, '租户新增', 1761400000000001700, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:add', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, ''),
    (1761400000000001703, '租户修改', 1761400000000001700, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:edit', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, ''),
    (1761400000000001704, '租户删除', 1761400000000001700, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:remove', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, ''),
    (1761400000000001705, '租户导出', 1761400000000001700, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:export', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, ''),
    (1761400000000001710, '租户套餐', 1761400000000000001, 13, 'tenant-package', 'system/tenant/package/index', '', 'N', 'Y', 'C', '0', '0', 'system:tenantPackage:list', 'price-tag', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '平台租户套餐菜单'),
    (1761400000000001711, '套餐查询', 1761400000000001710, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:query', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, ''),
    (1761400000000001712, '套餐新增', 1761400000000001710, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:add', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, ''),
    (1761400000000001713, '套餐修改', 1761400000000001710, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:edit', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, ''),
    (1761400000000001714, '套餐删除', 1761400000000001710, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:remove', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, ''),
    (1761400000000001715, '套餐导出', 1761400000000001710, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:export', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
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
    (1761300000000000001, 1761400000000001715)
ON CONFLICT (role_id, menu_id) DO NOTHING;
