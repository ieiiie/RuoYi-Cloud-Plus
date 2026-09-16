-- -----------------------------------------------------------------------------
-- YM-Cloud-Plus 6.0.1 全局账号与浏览器级租户切换升级脚本（MySQL）
--
-- 前置条件：已执行 update_6.0.0-tenant.sql，sys_user 已包含 tenant_id。
-- 本版本不迁移历史 sys_user 数据；新建账号和新加入租户的成员会由应用写入
-- global_user_id。当前按无历史数据库交付：不会把旧 sys_user 的账号资料迁移到
-- sys_global_user，且会移除旧表中的 user_name、phone_number、password。
-- 执行期间保持 tenant.enable=false，完成后再按部署计划启用。
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS sys_global_user (
    global_user_id bigint(20)      NOT NULL COMMENT '全局账号ID',
    user_name      varchar(30)     NOT NULL COMMENT '用户账号',
    nick_name      varchar(30)     NOT NULL COMMENT '用户昵称',
    user_type      varchar(10)     NOT NULL DEFAULT 'sys_user' COMMENT '用户类型',
    email          varchar(50)     DEFAULT NULL COMMENT '用户邮箱',
    phone_number   varchar(11)     DEFAULT NULL COMMENT '手机号码',
    gender         char(1)         DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
    avatar         bigint(20)      DEFAULT NULL COMMENT '头像 OSS ID',
    password       varchar(100)    NOT NULL COMMENT '密码',
    status         char(1)         NOT NULL DEFAULT '0' COMMENT '全局账号状态（0正常 1停用）',
    del_flag       char(1)         NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    create_dept    bigint(20)      DEFAULT NULL COMMENT '创建部门',
    create_by      bigint(20)      DEFAULT NULL COMMENT '创建者',
    create_time    datetime        DEFAULT NULL COMMENT '创建时间',
    update_by      bigint(20)      DEFAULT NULL COMMENT '更新者',
    update_time    datetime        DEFAULT NULL COMMENT '更新时间',
    remark         varchar(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (global_user_id),
    UNIQUE KEY uk_sys_global_user_name (user_name),
    UNIQUE KEY uk_sys_global_user_phone (phone_number),
    UNIQUE KEY uk_sys_global_user_email (email)
) ENGINE=InnoDB COMMENT='全局账号表';

CREATE TABLE IF NOT EXISTS sys_global_social (
    id                 bigint(20)      NOT NULL COMMENT '主键',
    global_user_id     bigint(20)      NOT NULL COMMENT '全局账号ID',
    auth_id            varchar(255)    NOT NULL COMMENT '平台+平台唯一id',
    source             varchar(255)    NOT NULL COMMENT '用户来源',
    open_id            varchar(255)    DEFAULT NULL COMMENT '平台编号唯一id',
    user_name          varchar(30)     DEFAULT NULL COMMENT '登录账号',
    nick_name          varchar(30)     DEFAULT NULL COMMENT '用户昵称',
    email              varchar(255)    DEFAULT NULL COMMENT '用户邮箱',
    avatar             varchar(500)    DEFAULT NULL COMMENT '头像地址',
    access_token       varchar(2000)   DEFAULT NULL COMMENT '用户的授权令牌',
    expire_in          int             DEFAULT NULL COMMENT '用户的授权令牌有效期',
    refresh_token      varchar(2000)   DEFAULT NULL COMMENT '刷新令牌',
    access_code        varchar(255)    DEFAULT NULL COMMENT '平台授权信息',
    union_id           varchar(255)    DEFAULT NULL COMMENT '用户 unionid',
    scope              varchar(255)    DEFAULT NULL COMMENT '授权范围',
    token_type         varchar(255)    DEFAULT NULL COMMENT '令牌类型',
    id_token           varchar(2000)   DEFAULT NULL COMMENT 'id token',
    mac_algorithm      varchar(255)    DEFAULT NULL COMMENT '小米平台算法',
    mac_key            varchar(255)    DEFAULT NULL COMMENT '小米平台密钥',
    code               varchar(255)    DEFAULT NULL COMMENT '授权 code',
    oauth_token        varchar(255)    DEFAULT NULL COMMENT 'OAuth token',
    oauth_token_secret varchar(255)    DEFAULT NULL COMMENT 'OAuth token secret',
    create_dept        bigint(20)      DEFAULT NULL COMMENT '创建部门',
    create_by          bigint(20)      DEFAULT NULL COMMENT '创建者',
    create_time        datetime        DEFAULT NULL COMMENT '创建时间',
    update_by          bigint(20)      DEFAULT NULL COMMENT '更新者',
    update_time        datetime        DEFAULT NULL COMMENT '更新时间',
    del_flag           char(1)         NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_global_social_auth (source, auth_id),
    UNIQUE KEY uk_sys_global_social_open (open_id),
    KEY idx_sys_global_social_user (global_user_id)
) ENGINE=InnoDB COMMENT='全局第三方账号绑定表';

ALTER TABLE sys_user
    ADD COLUMN global_user_id bigint(20) DEFAULT NULL COMMENT '全局账号ID' AFTER tenant_id,
    ADD KEY idx_sys_user_global_user_id (global_user_id),
    ADD UNIQUE KEY uk_sys_user_tenant_global_user (tenant_id, global_user_id);

-- 用户名、手机号和密码只允许保存在 sys_global_user。删除列时会同时清理依赖索引。
ALTER TABLE sys_user
    DROP COLUMN user_name,
    DROP COLUMN phone_number,
    DROP COLUMN password;
