-- 应用短地址登录配置；不修改菜单、角色、套餐授权和 DBO 登录体系。
SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_app' AND column_name = 'login_theme');
SET @ddl = IF(@column_exists = 0,
  'ALTER TABLE sys_app ADD COLUMN login_theme varchar(32) DEFAULT NULL COMMENT ''独立入口登录页面 agriculture/iot'' AFTER app_type',
  'SELECT 1');
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;
UPDATE sys_app SET login_theme = app_key
  WHERE app_type = 'MICRO' AND app_key IN ('agriculture', 'iot') AND login_theme IS NULL;
UPDATE sys_app SET login_theme = 'agriculture'
  WHERE app_type = 'COMPOSITE' AND app_key = 'test' AND login_theme IS NULL;
