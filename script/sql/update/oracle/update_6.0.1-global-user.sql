-- -----------------------------------------------------------------------------
-- RuoYi-Cloud-Plus 6.0.1 全局账号与浏览器级租户切换升级脚本（Oracle）
--
-- 前置条件：已执行 update_6.0.0-tenant.sql，sys_user 已包含 tenant_id。
-- 本版本不迁移历史 sys_user 数据；新建账号和新加入租户的成员会由应用写入
-- global_user_id。当前按无历史数据库交付：不会把旧 sys_user 的账号资料迁移到
-- sys_global_user，且会移除旧表中的 user_name、phone_number、password。
-- -----------------------------------------------------------------------------

CREATE TABLE sys_global_user (
    global_user_id number(20)    NOT NULL,
    user_name      varchar2(30)  NOT NULL,
    nick_name      varchar2(30)  NOT NULL,
    user_type      varchar2(10)  DEFAULT 'sys_user' NOT NULL,
    email          varchar2(50),
    phone_number   varchar2(11),
    gender         char(1)       DEFAULT '0',
    avatar         number(20),
    password       varchar2(100) NOT NULL,
    status         char(1)       DEFAULT '0' NOT NULL,
    del_flag       char(1)       DEFAULT '0' NOT NULL,
    create_dept    number(20),
    create_by      number(20),
    create_time    date,
    update_by      number(20),
    update_time    date,
    remark         varchar2(500),
    CONSTRAINT pk_sys_global_user PRIMARY KEY (global_user_id),
    CONSTRAINT uk_sys_global_user_name UNIQUE (user_name),
    CONSTRAINT uk_sys_global_user_phone UNIQUE (phone_number),
    CONSTRAINT uk_sys_global_user_email UNIQUE (email)
);

CREATE TABLE sys_global_social (
    id                 number(20)      NOT NULL,
    global_user_id     number(20)      NOT NULL,
    auth_id            varchar2(255)   NOT NULL,
    source             varchar2(255)   NOT NULL,
    open_id            varchar2(255),
    user_name          varchar2(30),
    nick_name          varchar2(30),
    email              varchar2(255),
    avatar             varchar2(500),
    access_token       varchar2(2000),
    expire_in          number(20),
    refresh_token      varchar2(2000),
    access_code        varchar2(255),
    union_id           varchar2(255),
    scope              varchar2(255),
    token_type         varchar2(255),
    id_token           varchar2(2000),
    mac_algorithm      varchar2(255),
    mac_key            varchar2(255),
    code               varchar2(255),
    oauth_token        varchar2(255),
    oauth_token_secret varchar2(255),
    create_dept        number(20),
    create_by          number(20),
    create_time        date,
    update_by          number(20),
    update_time        date,
    del_flag           char(1)         DEFAULT '0' NOT NULL,
    CONSTRAINT pk_sys_global_social PRIMARY KEY (id),
    CONSTRAINT uk_sys_global_social_auth UNIQUE (source, auth_id),
    CONSTRAINT uk_sys_global_social_open UNIQUE (open_id)
);
CREATE INDEX idx_sys_global_social_user ON sys_global_social (global_user_id);

ALTER TABLE sys_user ADD (global_user_id number(20));
CREATE INDEX idx_sys_user_global_user_id ON sys_user (global_user_id);
CREATE UNIQUE INDEX uk_sys_user_tenant_global_user ON sys_user (tenant_id, global_user_id);

-- 用户名、手机号和密码只允许保存在 sys_global_user。DROP COLUMN 会移除依赖索引。
ALTER TABLE sys_user DROP COLUMN user_name;
ALTER TABLE sys_user DROP COLUMN phone_number;
ALTER TABLE sys_user DROP COLUMN password;
