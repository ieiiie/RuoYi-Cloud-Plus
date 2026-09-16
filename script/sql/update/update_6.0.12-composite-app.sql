-- 跨微前端组合应用（MySQL）。
-- 可重复执行；只增加组合关系，不改变现有应用、菜单、套餐和角色授权。

SET NAMES utf8mb4;

ALTER TABLE sys_app
    MODIFY COLUMN app_type varchar(16) NOT NULL DEFAULT 'MICRO'
        COMMENT '应用类型（CORE/MICRO/COMPOSITE）';

CREATE TABLE IF NOT EXISTS sys_composite_app_menu (
    app_id  bigint(20) NOT NULL COMMENT '组合应用ID',
    menu_id bigint(20) NOT NULL COMMENT '源微应用菜单ID',
    PRIMARY KEY (app_id, menu_id),
    KEY idx_composite_app_menu_menu_id (menu_id)
) ENGINE=InnoDB COMMENT='组合应用菜单关联表';

-- 清理应用或菜单已不存在的历史孤儿关系；正常重复执行不会影响有效数据。
DELETE relation
FROM sys_composite_app_menu relation
LEFT JOIN sys_app app ON app.app_id = relation.app_id AND app.del_flag = '0'
LEFT JOIN sys_menu menu ON menu.menu_id = relation.menu_id
WHERE app.app_id IS NULL OR menu.menu_id IS NULL;
