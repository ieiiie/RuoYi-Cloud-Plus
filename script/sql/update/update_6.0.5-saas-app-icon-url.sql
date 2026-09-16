-- 在 SaaS 权威库执行，支持应用图标保存 OSS 图片地址；兼容已有 Iconify 名称。
ALTER TABLE sys_app MODIFY COLUMN icon varchar(1024) DEFAULT NULL COMMENT '应用图标（OSS图片URL或历史图标名称）';
