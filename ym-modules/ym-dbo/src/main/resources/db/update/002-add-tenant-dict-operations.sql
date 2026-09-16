-- Dbo 内部运营平台：租户字典类型菜单（MySQL）
SET NAMES utf8mb4;

INSERT IGNORE INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time,
     update_by, update_time, remark)
VALUES
    (1762400000000000900, '租户字典类型', 1762400000000000000, 9, 'tenant-dict', 'saas/tenant-dict/index', '', 'N', 'Y', 'C', '0', '0', 'saas:tenant-dict:list', 'ant-design:unordered-list-outlined', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, 'Dbo定义租户可维护的字典类型'),
    (1762410000000000901, '租户字典类型查询', 1762400000000000900, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'saas:tenant-dict:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1762410000000000902, '租户字典类型新增', 1762400000000000900, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'saas:tenant-dict:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1762410000000000903, '租户字典类型修改', 1762400000000000900, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'saas:tenant-dict:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1762410000000000904, '租户字典类型删除', 1762400000000000900, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'saas:tenant-dict:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');

INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1761300000000000001, menu_id
FROM sys_menu
WHERE menu_id IN (1762400000000000900,1762410000000000901,1762410000000000902,
                  1762410000000000903,1762410000000000904);
