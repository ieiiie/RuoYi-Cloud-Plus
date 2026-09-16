-- 仅在 Dbo 自身数据库执行；不操作 ry-cloud 的应用、业务菜单或组合关系。
-- 可重复执行。保留原菜单 ID 和写权限授权，将旧页面转为应用列表读取权限节点。
START TRANSACTION;

UPDATE sys_menu
SET parent_id = 1762400000000000400,
    menu_name = '应用列表查询', menu_type = 'F', path = '', component = '',
    visible = '0', status = '0', update_time = NOW(),
    remark = '应用维护已合并至租户菜单；保留 saas:app:list 权限及原角色关联'
WHERE menu_id = 1762400000000000100 AND perms = 'saas:app:list'
  AND (parent_id <> 1762400000000000400 OR menu_type <> 'F' OR component <> '' OR path <> '');

UPDATE sys_menu
SET parent_id = 1762400000000000400, update_time = NOW()
WHERE parent_id = 1762400000000000100 AND menu_type = 'F' AND perms LIKE 'saas:app:%';

-- 只有原来已拥有应用或租户菜单权限的角色获得合并页面所需的读取依赖。
-- 不添加任何 add/edit/remove 权限。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT granted.role_id, target.menu_id
FROM sys_role_menu granted
JOIN sys_menu source ON source.menu_id = granted.menu_id
JOIN sys_menu target ON target.menu_id = 1762400000000000000
    OR target.perms IN ('saas:app:list', 'saas:app:query', 'saas:menu:list', 'saas:menu:query')
WHERE source.perms IN (
    'saas:app:list', 'saas:app:query', 'saas:app:add', 'saas:app:edit', 'saas:app:remove',
    'saas:menu:list', 'saas:menu:query', 'saas:menu:add', 'saas:menu:edit', 'saas:menu:remove'
);

COMMIT;
