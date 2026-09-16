-- -----------------------------------------------------------------------------
-- YM-Cloud-Plus 6.0.1 全局账号与浏览器级租户切换升级脚本（PostgreSQL）
--
-- 前置条件：已执行 update_6.0.0-tenant.sql，sys_user 已包含 tenant_id。
-- 本版本不迁移历史 sys_user 数据；新建账号和新加入租户的成员会由应用写入
-- global_user_id。当前按无历史数据库交付：不会把旧 sys_user 的账号资料迁移到
-- sys_global_user，且会移除旧表中的 user_name、phone_number、password。
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS sys_global_user (
    global_user_id int8         NOT NULL,
    user_name      varchar(30)  NOT NULL,
    nick_name      varchar(30)  NOT NULL,
    user_type      varchar(10)  NOT NULL DEFAULT 'sys_user',
    email          varchar(50),
    phone_number   varchar(11),
    gender         char         DEFAULT '0',
    avatar         int8,
    password       varchar(100) NOT NULL,
    status         char         NOT NULL DEFAULT '0',
    del_flag       char         NOT NULL DEFAULT '0',
    create_dept    int8,
    create_by      int8,
    create_time    timestamp,
    update_by      int8,
    update_time    timestamp,
    remark         varchar(500),
    CONSTRAINT sys_global_user_pk PRIMARY KEY (global_user_id),
    CONSTRAINT uk_sys_global_user_name UNIQUE (user_name),
    CONSTRAINT uk_sys_global_user_phone UNIQUE (phone_number),
    CONSTRAINT uk_sys_global_user_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS sys_global_social (
    id                 int8         NOT NULL,
    global_user_id     int8         NOT NULL,
    auth_id            varchar(255) NOT NULL,
    source             varchar(255) NOT NULL,
    open_id            varchar(255),
    user_name          varchar(30),
    nick_name          varchar(30),
    email              varchar(255),
    avatar             varchar(500),
    access_token       varchar(2000),
    expire_in          int4,
    refresh_token      varchar(2000),
    access_code        varchar(255),
    union_id           varchar(255),
    scope              varchar(255),
    token_type         varchar(255),
    id_token           varchar(2000),
    mac_algorithm      varchar(255),
    mac_key            varchar(255),
    code               varchar(255),
    oauth_token        varchar(255),
    oauth_token_secret varchar(255),
    create_dept        int8,
    create_by          int8,
    create_time        timestamp,
    update_by          int8,
    update_time        timestamp,
    del_flag           char         NOT NULL DEFAULT '0',
    CONSTRAINT sys_global_social_pk PRIMARY KEY (id),
    CONSTRAINT uk_sys_global_social_auth UNIQUE (source, auth_id),
    CONSTRAINT uk_sys_global_social_open UNIQUE (open_id)
);
CREATE INDEX IF NOT EXISTS idx_sys_global_social_user ON sys_global_social (global_user_id);

ALTER TABLE sys_user ADD COLUMN global_user_id int8;
CREATE INDEX idx_sys_user_global_user_id ON sys_user (global_user_id);
CREATE UNIQUE INDEX uk_sys_user_tenant_global_user ON sys_user (tenant_id, global_user_id);

-- 用户名、手机号和密码只允许保存在 sys_global_user。DROP COLUMN 会移除依赖索引。
ALTER TABLE sys_user
    DROP COLUMN user_name,
    DROP COLUMN phone_number,
    DROP COLUMN password;
