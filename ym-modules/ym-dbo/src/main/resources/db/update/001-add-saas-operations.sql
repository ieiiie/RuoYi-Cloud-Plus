-- Dbo 内部运营平台：SaaS 运营菜单（MySQL）
-- 业务数据仍位于 ry-cloud；本库只保存 Dbo 自身账号、权限与菜单。

SET NAMES utf8mb4;

-- 与 Dbo 自身的系统管理完全分离。
INSERT IGNORE INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time,
     update_by, update_time, remark)
VALUES
    (1762400000000000000, 'SaaS运营', 0, 2, 'saas', NULL, '', 'N', 'Y', 'M', '0', '0', '', 'ant-design:cloud-server-outlined', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '操作 ry-cloud 权威库'),
    (1762400000000000100, '应用管理', 1762400000000000000, 1, 'app', 'saas/app/index', '', 'N', 'Y', 'C', '0', '0', 'saas:app:list', 'ant-design:appstore-outlined', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '管理SaaS应用'),
    (1762400000000000200, '租户管理', 1762400000000000000, 2, 'tenant', 'saas/tenant/index', '', 'N', 'Y', 'C', '0', '0', 'saas:tenant:list', 'ant-design:team-outlined', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '管理SaaS租户'),
    (1762400000000000300, '租户套餐', 1762400000000000000, 3, 'tenant-package', 'saas/tenant-package/index', '', 'N', 'Y', 'C', '0', '0', 'saas:tenant-package:list', 'ant-design:gift-outlined', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '管理应用与菜单套餐'),
    (1762400000000000400, '租户菜单', 1762400000000000000, 4, 'menu', 'saas/menu/index', '', 'N', 'Y', 'C', '0', '0', 'saas:menu:list', 'ant-design:menu-outlined', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '按应用维护SaaS菜单'),
    (1762400000000000500, '全局字典', 1762400000000000000, 5, 'dict', 'saas/dict/index', '', 'N', 'Y', 'C', '0', '0', 'saas:dict:list', 'ant-design:book-outlined', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '维护ry-cloud全局字典'),
    (1762400000000000600, '参数定义', 1762400000000000000, 6, 'config-definition', 'saas/config-definition/index', '', 'N', 'Y', 'C', '0', '0', 'saas:config-definition:list', 'ant-design:control-outlined', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '定义SaaS租户参数'),
    (1762400000000000700, 'OSS配置', 1762400000000000000, 7, 'oss-config', 'saas/oss-config/index', '', 'N', 'Y', 'C', '0', '0', 'saas:oss-config:list', 'ant-design:cloud-upload-outlined', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '维护SaaS对象存储配置'),
    (1762400000000000800, '角色权限模板', 1762400000000000000, 8, 'role-template', 'saas/role-template/index', '', 'N', 'Y', 'C', '0', '0', 'saas:role-template:list', 'ant-design:safety-certificate-outlined', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '维护SaaS内置角色模板'),
    (1762400000000000900, '租户字典类型', 1762400000000000000, 9, 'tenant-dict', 'saas/tenant-dict/index', '', 'N', 'Y', 'C', '0', '0', 'saas:tenant-dict:list', 'ant-design:unordered-list-outlined', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, 'Dbo定义租户可维护的字典类型');

-- 每个资源统一提供 query/add/edit/remove 权限；模板额外提供 sync。
INSERT IGNORE INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time,
     update_by, update_time, remark)
SELECT 1762410000000000000 + (r.seq * 100) + a.seq,
       CONCAT(r.title, a.title), r.menu_id, a.seq, '', '', '', 'N', 'Y', 'F', '0', '0',
       CONCAT('saas:', r.resource, ':', a.action), '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''
FROM (
    SELECT 1 seq, 1762400000000000100 menu_id, '应用' title, 'app' resource UNION ALL
    SELECT 2, 1762400000000000200, '租户', 'tenant' UNION ALL
    SELECT 3, 1762400000000000300, '套餐', 'tenant-package' UNION ALL
    SELECT 4, 1762400000000000400, '菜单', 'menu' UNION ALL
    SELECT 5, 1762400000000000500, '字典', 'dict' UNION ALL
    SELECT 6, 1762400000000000600, '参数定义', 'config-definition' UNION ALL
    SELECT 7, 1762400000000000700, 'OSS配置', 'oss-config' UNION ALL
    SELECT 8, 1762400000000000800, '角色模板', 'role-template' UNION ALL
    SELECT 9, 1762400000000000900, '租户字典类型', 'tenant-dict'
) r
CROSS JOIN (
    SELECT 1 seq, '查询' title, 'query' action UNION ALL
    SELECT 2, '新增', 'add' UNION ALL
    SELECT 3, '修改', 'edit' UNION ALL
    SELECT 4, '删除', 'remove'
) a;

INSERT IGNORE INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time,
     update_by, update_time, remark)
VALUES
    (1762410000000000890, '模板同步', 1762400000000000800, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'saas:role-template:sync', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');

-- Dbo 默认超级管理员获得全部运营权限。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1761300000000000001, menu_id FROM sys_menu WHERE menu_id BETWEEN 1762400000000000000 AND 1762499999999999999;

-- 合并应用管理与租户菜单（新库初始化与增量迁移保持一致）。
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
