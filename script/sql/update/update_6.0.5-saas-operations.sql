-- SaaS 运营能力迁移至 Dbo（MySQL）
-- 执行前请备份 ry-cloud。本脚本只修改 SaaS 权威库，不向 ry_dbo 复制业务数据。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 全局应用目录。
CREATE TABLE IF NOT EXISTS sys_app (
    app_id          bigint(20)    NOT NULL COMMENT '应用ID',
    app_key         varchar(64)   NOT NULL COMMENT '应用标识',
    app_name        varchar(100)  NOT NULL COMMENT '应用名称',
    app_type        varchar(16)   NOT NULL DEFAULT 'MICRO' COMMENT '应用类型（CORE/MICRO/COMPOSITE）',
    entry           varchar(255)  DEFAULT NULL COMMENT '微应用同域入口',
    initial_path    varchar(255)  NOT NULL DEFAULT '/' COMMENT '初始路由',
    alive           tinyint(1)    NOT NULL DEFAULT 1 COMMENT '是否保活',
    sync            tinyint(1)    NOT NULL DEFAULT 1 COMMENT '是否同步路由',
    icon            varchar(1024)  DEFAULT NULL COMMENT '应用图标',
    order_num       int(4)        NOT NULL DEFAULT 0 COMMENT '显示顺序',
    status          char(1)       NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    del_flag        char(1)       NOT NULL DEFAULT '0' COMMENT '删除标志',
    create_dept     bigint(20)    DEFAULT NULL COMMENT '创建部门',
    create_by       bigint(20)    DEFAULT NULL COMMENT '创建者',
    create_time     datetime      DEFAULT NULL COMMENT '创建时间',
    update_by       bigint(20)    DEFAULT NULL COMMENT '更新者',
    update_time     datetime      DEFAULT NULL COMMENT '更新时间',
    remark          varchar(500)  DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (app_id),
    UNIQUE KEY uk_sys_app_key (app_key),
    KEY idx_sys_app_status_sort (status, order_num)
) ENGINE=InnoDB COMMENT='SaaS全局应用表';

INSERT INTO sys_app
    (app_id, app_key, app_name, app_type, entry, initial_path, alive, sync, icon,
     order_num, status, del_flag, create_time, remark)
VALUES
    (1762100000000000001, 'saas-core', 'SaaS核心', 'CORE', NULL, '/', 1, 1,
     'ant-design:appstore-outlined', 0, '0', '0', SYSDATE(), '主应用核心菜单，不显示为工作台卡片')
ON DUPLICATE KEY UPDATE app_name = VALUES(app_name), app_type = 'CORE', entry = NULL;

-- 现有 ext.microApp 数据在增加 app_id 前迁入应用表。兼容 ext 为空或无微应用配置的数据库。
INSERT IGNORE INTO sys_app
    (app_id, app_key, app_name, app_type, entry, initial_path, alive, sync, icon,
     order_num, status, del_flag, create_time, remark)
SELECT menu_id,
       JSON_UNQUOTE(JSON_EXTRACT(ext, '$.microApp.name')),
       menu_name,
       'MICRO',
       JSON_UNQUOTE(JSON_EXTRACT(ext, '$.microApp.entry')),
       COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(ext, '$.microApp.initialPath')), 'null'), '/'),
       COALESCE(JSON_EXTRACT(ext, '$.microApp.alive') + 0, 1),
       COALESCE(JSON_EXTRACT(ext, '$.microApp.sync') + 0, 1),
       icon,
       order_num,
       status,
       '0',
       COALESCE(create_time, SYSDATE()),
       '由 sys_menu.ext.microApp 自动迁移'
FROM sys_menu
WHERE parent_id = 0
  AND JSON_VALID(ext)
  AND JSON_EXTRACT(ext, '$.microApp.name') IS NOT NULL;

ALTER TABLE sys_menu
    ADD COLUMN app_id bigint(20) NOT NULL DEFAULT 1762100000000000001 COMMENT '所属应用ID' AFTER menu_id,
    ADD KEY idx_sys_menu_app_parent (app_id, parent_id);

UPDATE sys_menu m
JOIN sys_app a ON a.app_id = m.menu_id AND a.app_type = 'MICRO'
SET m.app_id = a.app_id;

-- 将已迁移的微应用配置从 ext 中移除；ext 继续作为通用扩展字段。
UPDATE sys_menu
SET ext = JSON_REMOVE(ext, '$.microApp')
WHERE JSON_VALID(ext) AND JSON_EXTRACT(ext, '$.microApp') IS NOT NULL;

-- 2. 套餐应用、菜单规范化授权。
CREATE TABLE IF NOT EXISTS sys_tenant_package_app (
    package_id bigint(20) NOT NULL COMMENT '套餐ID',
    app_id     bigint(20) NOT NULL COMMENT '应用ID',
    PRIMARY KEY (package_id, app_id),
    KEY idx_package_app_app_id (app_id)
) ENGINE=InnoDB COMMENT='租户套餐应用关联表';

CREATE TABLE IF NOT EXISTS sys_tenant_package_menu (
    package_id bigint(20) NOT NULL COMMENT '套餐ID',
    menu_id    bigint(20) NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (package_id, menu_id),
    KEY idx_package_menu_menu_id (menu_id)
) ENGINE=InnoDB COMMENT='租户套餐菜单关联表';

INSERT IGNORE INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, m.menu_id
FROM sys_tenant_package p
JOIN sys_menu m ON FIND_IN_SET(
    CAST(m.menu_id AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci,
    p.menu_ids COLLATE utf8mb4_unicode_ci
) > 0;

INSERT IGNORE INTO sys_tenant_package_app (package_id, app_id)
SELECT DISTINCT pm.package_id, m.app_id
FROM sys_tenant_package_menu pm
JOIN sys_menu m ON m.menu_id = pm.menu_id;

ALTER TABLE sys_tenant_package DROP COLUMN menu_ids;

-- 3. 角色权限模板。
CREATE TABLE IF NOT EXISTS sys_role_template (
    template_id        bigint(20)    NOT NULL COMMENT '模板ID',
    template_name      varchar(100)  NOT NULL COMMENT '模板名称',
    template_key       varchar(100)  NOT NULL COMMENT '模板键',
    app_id             bigint(20)    DEFAULT NULL COMMENT '所属应用；空表示全局模板',
    role_key           varchar(100)  NOT NULL COMMENT '下发角色标识',
    role_sort          int(4)        NOT NULL DEFAULT 1 COMMENT '角色排序',
    data_scope         char(1)       NOT NULL DEFAULT '1' COMMENT '数据范围（不支持2自定义部门）',
    status             char(1)       NOT NULL DEFAULT '0' COMMENT '状态',
    template_version   int(11)       NOT NULL DEFAULT 1 COMMENT '模板版本',
    del_flag           char(1)       NOT NULL DEFAULT '0' COMMENT '删除标志',
    create_dept        bigint(20)    DEFAULT NULL COMMENT '创建部门',
    create_by          bigint(20)    DEFAULT NULL COMMENT '创建者',
    create_time        datetime      DEFAULT NULL COMMENT '创建时间',
    update_by          bigint(20)    DEFAULT NULL COMMENT '更新者',
    update_time        datetime      DEFAULT NULL COMMENT '更新时间',
    remark             varchar(500)  DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (template_id),
    UNIQUE KEY uk_sys_role_template_key (template_key),
    KEY idx_role_template_app (app_id, status)
) ENGINE=InnoDB COMMENT='SaaS角色权限模板';

CREATE TABLE IF NOT EXISTS sys_role_template_menu (
    template_id bigint(20) NOT NULL COMMENT '模板ID',
    menu_id     bigint(20) NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (template_id, menu_id),
    KEY idx_role_template_menu_menu (menu_id)
) ENGINE=InnoDB COMMENT='角色模板菜单关联表';

ALTER TABLE sys_role
    ADD COLUMN template_id bigint(20) DEFAULT NULL COMMENT '角色模板ID' AFTER role_id,
    ADD COLUMN template_version int(11) DEFAULT NULL COMMENT '已同步模板版本' AFTER template_id,
    ADD COLUMN is_builtin tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否内置角色' AFTER template_version,
    ADD UNIQUE KEY uk_sys_role_tenant_template (tenant_id, template_id);

ALTER TABLE sys_role
    DROP INDEX idx_sys_role_tenant_role_key,
    ADD UNIQUE KEY uk_sys_role_tenant_role_key (tenant_id, role_key);

INSERT INTO sys_role_template
    (template_id, template_name, template_key, app_id, role_key, role_sort, data_scope,
     status, template_version, del_flag, create_time, remark)
VALUES
    (1762200000000000001, '租户管理员', 'tenant-admin', NULL, 'tenant_admin', 1, '1',
     '0', 1, '0', SYSDATE(), '全局内置租户管理员模板')
ON DUPLICATE KEY UPDATE template_name = VALUES(template_name);

-- 现有租户管理员纳入内置模板；默认租户 superadmin 仍保持技术角色。
UPDATE sys_role
SET template_id = 1762200000000000001,
    template_version = 1,
    is_builtin = 1
WHERE tenant_id <> '000000'
  AND role_key = 'tenant_admin'
  AND del_flag = '0';

-- 4. 参数定义与租户参数值。
CREATE TABLE IF NOT EXISTS sys_config_definition (
    definition_id      bigint(20)    NOT NULL COMMENT '定义ID',
    app_id             bigint(20)    DEFAULT NULL COMMENT '所属应用；空表示全局参数',
    config_name        varchar(100)  NOT NULL COMMENT '参数名称',
    config_key         varchar(100)  NOT NULL COMMENT '参数键名',
    value_type         varchar(16)   NOT NULL DEFAULT 'STRING' COMMENT 'STRING/INTEGER/DECIMAL/BOOLEAN/ENUM/JSON/PASSWORD',
    default_value      varchar(2000) DEFAULT NULL COMMENT '默认值',
    required_flag      tinyint(1)    NOT NULL DEFAULT 0 COMMENT '是否必填',
    min_length         int(11)       DEFAULT NULL COMMENT '最小长度',
    max_length         int(11)       DEFAULT NULL COMMENT '最大长度',
    min_value          decimal(30,10) DEFAULT NULL COMMENT '最小值',
    max_value          decimal(30,10) DEFAULT NULL COMMENT '最大值',
    regex_pattern      varchar(500)  DEFAULT NULL COMMENT '正则表达式',
    enum_options       json          DEFAULT NULL COMMENT '枚举选项JSON数组',
    tenant_editable    tinyint(1)    NOT NULL DEFAULT 1 COMMENT '租户是否可编辑',
    order_num          int(4)        NOT NULL DEFAULT 0 COMMENT '排序',
    status             char(1)       NOT NULL DEFAULT '0' COMMENT '状态',
    issued_flag        tinyint(1)    NOT NULL DEFAULT 0 COMMENT '是否已向租户发放',
    del_flag           char(1)       NOT NULL DEFAULT '0' COMMENT '删除标志',
    create_dept        bigint(20)    DEFAULT NULL COMMENT '创建部门',
    create_by          bigint(20)    DEFAULT NULL COMMENT '创建者',
    create_time        datetime      DEFAULT NULL COMMENT '创建时间',
    update_by          bigint(20)    DEFAULT NULL COMMENT '更新者',
    update_time        datetime      DEFAULT NULL COMMENT '更新时间',
    remark             varchar(500)  DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (definition_id),
    UNIQUE KEY uk_config_definition_key (config_key),
    KEY idx_config_definition_app (app_id, status)
) ENGINE=InnoDB COMMENT='SaaS租户参数定义';

ALTER TABLE sys_config
    ADD COLUMN definition_id bigint(20) DEFAULT NULL COMMENT '参数定义ID' AFTER config_id,
    ADD UNIQUE KEY uk_sys_config_tenant_definition (tenant_id, definition_id);

INSERT INTO sys_config_definition
    (definition_id, config_name, config_key, value_type, default_value, required_flag,
     tenant_editable, order_num, status, issued_flag, del_flag, create_time, remark)
SELECT MIN(config_id), MAX(config_name), config_key,
       CASE WHEN LOWER(MAX(config_value)) IN ('true', 'false') THEN 'BOOLEAN' ELSE 'STRING' END,
       MAX(CASE WHEN tenant_id = '000000' THEN config_value END),
       0, 1, 0, '0', 1, '0', SYSDATE(), MAX(remark)
FROM sys_config
GROUP BY config_key
ON DUPLICATE KEY UPDATE config_name = VALUES(config_name);

UPDATE sys_config c
JOIN sys_config_definition d ON d.config_key = c.config_key
SET c.definition_id = d.definition_id
WHERE c.definition_id IS NULL;

-- 5. SaaS 移除运营菜单，只保留租户参数值编辑和文件管理。
DELETE rm FROM sys_role_menu rm
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE m.menu_id IN (1761400000000000102,1761400000000000105,1761400000000000133,
                    1761400000000001700,1761400000000001710)
   OR m.parent_id IN (1761400000000000102,1761400000000000105,1761400000000000133,
                      1761400000000001700,1761400000000001710)
   OR m.perms LIKE 'system:ossConfig:%';

DELETE FROM sys_menu
WHERE menu_id IN (1761400000000000102,1761400000000000105,1761400000000000133,
                  1761400000000001700,1761400000000001710)
   OR parent_id IN (1761400000000000102,1761400000000000105,1761400000000000133,
                    1761400000000001700,1761400000000001710)
   OR perms LIKE 'system:ossConfig:%';

UPDATE sys_menu
SET menu_name = '租户参数', component = 'system/config/index', remark = '租户仅可修改参数值'
WHERE menu_id = 1761400000000000106;

DELETE rm FROM sys_role_menu rm
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE m.parent_id = 1761400000000000106
  AND m.perms IN ('system:config:add','system:config:remove','system:config:export');
DELETE FROM sys_menu
WHERE parent_id = 1761400000000000106
  AND perms IN ('system:config:add','system:config:remove','system:config:export');
UPDATE sys_menu SET menu_name = '参数值修改', perms = 'system:config:valueEdit'
WHERE parent_id = 1761400000000000106 AND perms = 'system:config:edit';

-- 模块裁剪或历史菜单删除可能留下套餐关系；Dbo 服务已在删菜单时级联清理，
-- 此幂等语句用于修复升级前的存量孤儿数据。
DELETE package_menu
FROM sys_tenant_package_menu package_menu
LEFT JOIN sys_menu menu ON menu.menu_id = package_menu.menu_id
WHERE menu.menu_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;
