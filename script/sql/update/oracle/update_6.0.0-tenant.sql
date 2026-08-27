-- -----------------------------------------------------------------------------
-- RuoYi-Cloud-Plus 6.0.0 多租户升级脚本（Oracle）
-- 一次性升级脚本：执行前请备份数据库，并保持 tenant.enable=false。
-- -----------------------------------------------------------------------------

ALTER TABLE sys_social ADD (tenant_id varchar2(20) DEFAULT '000000' NOT NULL);
CREATE INDEX idx_sys_social_tenant_id ON sys_social (tenant_id);

ALTER TABLE sys_dept ADD (tenant_id varchar2(20) DEFAULT '000000' NOT NULL);
CREATE INDEX idx_sys_dept_tenant_id ON sys_dept (tenant_id);

ALTER TABLE sys_user ADD (tenant_id varchar2(20) DEFAULT '000000' NOT NULL);

-- 全量初始化脚本已包含 GLOBAL_USER_ID；旧库在执行 6.0.1 前没有此列。
-- 因此仅当该列已存在时创建成员关系唯一索引，旧库仍由 6.0.1 脚本创建。
DECLARE
    v_column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_column_count
    FROM user_tab_columns
    WHERE table_name = 'SYS_USER'
      AND column_name = 'GLOBAL_USER_ID';
    IF v_column_count > 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_sys_user_tenant_global_user ON sys_user (tenant_id, global_user_id)';
    END IF;
END;
/

ALTER TABLE sys_post ADD (tenant_id varchar2(20) DEFAULT '000000' NOT NULL);
CREATE INDEX idx_sys_post_tenant_id ON sys_post (tenant_id);

ALTER TABLE sys_role ADD (tenant_id varchar2(20) DEFAULT '000000' NOT NULL);
CREATE INDEX idx_sys_role_tenant_key ON sys_role (tenant_id, role_key);

ALTER TABLE sys_oper_log ADD (tenant_id varchar2(20) DEFAULT '000000' NOT NULL);
CREATE INDEX idx_sys_oper_log_tenant_time ON sys_oper_log (tenant_id, oper_time);

ALTER TABLE sys_dict_type ADD (tenant_id varchar2(20) DEFAULT '000000' NOT NULL);
DROP INDEX sys_dict_type_index1;
CREATE UNIQUE INDEX uk_sys_dict_type_tenant ON sys_dict_type (tenant_id, dict_type);

ALTER TABLE sys_dict_data ADD (tenant_id varchar2(20) DEFAULT '000000' NOT NULL);
CREATE INDEX idx_sys_dict_data_tenant ON sys_dict_data (tenant_id, dict_type);

ALTER TABLE sys_config ADD (tenant_id varchar2(20) DEFAULT '000000' NOT NULL);
CREATE INDEX idx_sys_config_tenant_key ON sys_config (tenant_id, config_key);

ALTER TABLE sys_login_info ADD (tenant_id varchar2(20) DEFAULT '000000' NOT NULL);
CREATE INDEX idx_sys_login_tenant_time ON sys_login_info (tenant_id, login_time);

ALTER TABLE sys_notice ADD (tenant_id varchar2(20) DEFAULT '000000' NOT NULL);
CREATE INDEX idx_sys_notice_tenant_id ON sys_notice (tenant_id);

ALTER TABLE sys_message ADD (tenant_id varchar2(20) DEFAULT '000000' NOT NULL);
CREATE INDEX idx_sys_message_tenant ON sys_message (tenant_id, create_time);

ALTER TABLE sys_oss ADD (tenant_id varchar2(20) DEFAULT '000000' NOT NULL);
CREATE INDEX idx_sys_oss_tenant_id ON sys_oss (tenant_id);

CREATE TABLE sys_tenant (
    id                number(20)    NOT NULL,
    tenant_id         varchar2(20)  NOT NULL,
    contact_user_name varchar2(50)  NOT NULL,
    contact_phone     varchar2(20)  NOT NULL,
    company_name      varchar2(100) NOT NULL,
    license_number    varchar2(100),
    address           varchar2(255),
    domain            varchar2(255),
    intro             varchar2(1000),
    package_id        number(20),
    expire_time       date,
    account_count     number(20)    DEFAULT -1 NOT NULL,
    status            char(1)       DEFAULT '0' NOT NULL,
    del_flag          char(1)       DEFAULT '0' NOT NULL,
    create_dept       number(20),
    create_by         number(20),
    create_time       date,
    update_by         number(20),
    update_time       date,
    remark            varchar2(500),
    CONSTRAINT pk_sys_tenant PRIMARY KEY (id),
    CONSTRAINT uk_sys_tenant_tid UNIQUE (tenant_id)
);
CREATE INDEX idx_sys_tenant_status ON sys_tenant (status);

CREATE TABLE sys_tenant_package (
    package_id          number(20)    NOT NULL,
    package_name        varchar2(100) NOT NULL,
    menu_ids            varchar2(4000) DEFAULT '',
    menu_check_strictly number(1)     DEFAULT 1 NOT NULL,
    status              char(1)       DEFAULT '0' NOT NULL,
    del_flag            char(1)       DEFAULT '0' NOT NULL,
    create_dept         number(20),
    create_by           number(20),
    create_time         date,
    update_by           number(20),
    update_time         date,
    remark              varchar2(500),
    CONSTRAINT pk_sys_tenant_package PRIMARY KEY (package_id)
);
CREATE INDEX idx_sys_tenant_pkg_status ON sys_tenant_package (status);

MERGE INTO sys_tenant_package t
USING (
    SELECT 1762000000000000001 AS package_id,
           '默认套餐' AS package_name,
           NVL((SELECT LISTAGG(TO_CHAR(menu_id), ',') WITHIN GROUP (ORDER BY menu_id) FROM sys_menu), '') AS menu_ids,
           1 AS menu_check_strictly,
           '0' AS status,
           '0' AS del_flag,
           1761000000000000103 AS create_dept,
           1761100000000000001 AS create_by,
           SYSDATE AS create_time,
           '升级自动创建的默认套餐' AS remark
    FROM dual
) s ON (t.package_id = s.package_id)
WHEN NOT MATCHED THEN INSERT
    (package_id, package_name, menu_ids, menu_check_strictly, status, del_flag,
     create_dept, create_by, create_time, remark)
VALUES
    (s.package_id, s.package_name, s.menu_ids, s.menu_check_strictly, s.status, s.del_flag,
     s.create_dept, s.create_by, s.create_time, s.remark);

MERGE INTO sys_tenant t
USING (
    SELECT 1762000000000000002 AS id,
           '000000' AS tenant_id,
           '平台管理员' AS contact_user_name,
           '15888888888' AS contact_phone,
           '默认管理租户' AS company_name,
           1762000000000000001 AS package_id,
           -1 AS account_count,
           '0' AS status,
           '0' AS del_flag,
           1761000000000000103 AS create_dept,
           1761100000000000001 AS create_by,
           SYSDATE AS create_time,
           '升级前已有系统数据所属的默认租户' AS remark
    FROM dual
) s ON (t.tenant_id = s.tenant_id)
WHEN NOT MATCHED THEN INSERT
    (id, tenant_id, contact_user_name, contact_phone, company_name, package_id,
     account_count, status, del_flag, create_dept, create_by, create_time, remark)
VALUES
    (s.id, s.tenant_id, s.contact_user_name, s.contact_phone, s.company_name, s.package_id,
     s.account_count, s.status, s.del_flag, s.create_dept, s.create_by, s.create_time, s.remark);

INSERT ALL
    INTO sys_menu VALUES (1761400000000001700, '租户管理', 1761400000000000001, 12, 'tenant', 'system/tenant/index', '', 'N', 'Y', 'C', '0', '0', 'system:tenant:list', 'company', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '平台租户管理菜单')
    INTO sys_menu VALUES (1761400000000001701, '租户查询', 1761400000000001700, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '')
    INTO sys_menu VALUES (1761400000000001702, '租户新增', 1761400000000001700, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '')
    INTO sys_menu VALUES (1761400000000001703, '租户修改', 1761400000000001700, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '')
    INTO sys_menu VALUES (1761400000000001704, '租户删除', 1761400000000001700, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '')
    INTO sys_menu VALUES (1761400000000001705, '租户导出', 1761400000000001700, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '')
    INTO sys_menu VALUES (1761400000000001710, '租户套餐', 1761400000000000001, 13, 'tenant-package', 'system/tenant/package/index', '', 'N', 'Y', 'C', '0', '0', 'system:tenantPackage:list', 'price-tag', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '平台租户套餐菜单')
    INTO sys_menu VALUES (1761400000000001711, '套餐查询', 1761400000000001710, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '')
    INTO sys_menu VALUES (1761400000000001712, '套餐新增', 1761400000000001710, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '')
    INTO sys_menu VALUES (1761400000000001713, '套餐修改', 1761400000000001710, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '')
    INTO sys_menu VALUES (1761400000000001714, '套餐删除', 1761400000000001710, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '')
    INTO sys_menu VALUES (1761400000000001715, '套餐导出', 1761400000000001710, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '')
SELECT 1 FROM dual;

INSERT ALL
    INTO sys_role_menu VALUES (1761300000000000001, 1761400000000001700)
    INTO sys_role_menu VALUES (1761300000000000001, 1761400000000001701)
    INTO sys_role_menu VALUES (1761300000000000001, 1761400000000001702)
    INTO sys_role_menu VALUES (1761300000000000001, 1761400000000001703)
    INTO sys_role_menu VALUES (1761300000000000001, 1761400000000001704)
    INTO sys_role_menu VALUES (1761300000000000001, 1761400000000001705)
    INTO sys_role_menu VALUES (1761300000000000001, 1761400000000001710)
    INTO sys_role_menu VALUES (1761300000000000001, 1761400000000001711)
    INTO sys_role_menu VALUES (1761300000000000001, 1761400000000001712)
    INTO sys_role_menu VALUES (1761300000000000001, 1761400000000001713)
    INTO sys_role_menu VALUES (1761300000000000001, 1761400000000001714)
    INTO sys_role_menu VALUES (1761300000000000001, 1761400000000001715)
SELECT 1 FROM dual;
