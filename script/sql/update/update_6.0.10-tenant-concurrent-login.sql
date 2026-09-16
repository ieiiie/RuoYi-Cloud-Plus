-- SaaS 租户用户并发登录限制。
INSERT INTO sys_config_definition
    (definition_id, app_id, config_name, config_key, value_type, default_value, required_flag,
     min_length, max_length, min_value, max_value, regex_pattern, enum_options, tenant_editable,
     order_num, status, issued_flag, del_flag, create_time, remark)
VALUES
    (1761700000000000004, NULL, '用户并发登录数量', 'sys.account.maxConcurrentLoginCount',
     'INTEGER', '-1', 1, NULL, NULL, -1, 2147483647, '^(-1|[1-9][0-9]*)$', NULL, 1,
     30, '0', 1, '0', SYSDATE(),
     'PC、Android、iOS、微信小程序分别计数；-1表示不限，正整数表示每类客户端允许同时在线的会话数')
ON DUPLICATE KEY UPDATE
    app_id = VALUES(app_id),
    config_name = VALUES(config_name),
    value_type = VALUES(value_type),
    default_value = VALUES(default_value),
    required_flag = VALUES(required_flag),
    min_length = VALUES(min_length),
    max_length = VALUES(max_length),
    min_value = VALUES(min_value),
    max_value = VALUES(max_value),
    regex_pattern = VALUES(regex_pattern),
    enum_options = VALUES(enum_options),
    tenant_editable = VALUES(tenant_editable),
    order_num = VALUES(order_num),
    status = VALUES(status),
    issued_flag = VALUES(issued_flag),
    del_flag = VALUES(del_flag),
    remark = VALUES(remark);

UPDATE sys_config c
JOIN sys_config_definition d ON d.config_key = 'sys.account.maxConcurrentLoginCount'
SET c.definition_id = d.definition_id
WHERE c.config_key = 'sys.account.maxConcurrentLoginCount';

INSERT INTO sys_config
    (config_id, tenant_id, definition_id, config_name, config_key, config_value, config_type,
     create_dept, create_by, create_time, update_by, update_time, remark)
SELECT UUID_SHORT(), t.tenant_id, d.definition_id, d.config_name, d.config_key, d.default_value, 'Y',
       NULL, NULL, SYSDATE(), NULL, NULL, d.remark
FROM sys_tenant t
JOIN sys_config_definition d ON d.config_key = 'sys.account.maxConcurrentLoginCount'
LEFT JOIN sys_config c
       ON c.tenant_id = t.tenant_id
      AND c.definition_id = d.definition_id
WHERE t.del_flag = '0'
  AND c.config_id IS NULL;
