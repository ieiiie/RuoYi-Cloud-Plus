-- 角色模板删除策略与租户角色快照。
ALTER TABLE sys_role_template
    ADD COLUMN tenant_deletable tinyint(1) NOT NULL DEFAULT 0 COMMENT '租户是否可删除下发角色' AFTER template_version;

ALTER TABLE sys_role
    ADD COLUMN tenant_deletable tinyint(1) NOT NULL DEFAULT 0 COMMENT '模板角色是否允许租户删除' AFTER is_builtin;

UPDATE sys_role_template SET tenant_deletable = 0;
UPDATE sys_role SET tenant_deletable = 0;
