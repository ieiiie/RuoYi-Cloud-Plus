-- ym-iot 权限收敛为三个字母/数字段。只修改 perms，不新增授权、不修改菜单 ID。
-- 发布时与前后端版本一起执行；事务失败可以直接回滚，保留执行前 sys_menu 备份。
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE sys_menu SET perms = 'iot:alarmConfig:list' WHERE BINARY perms = 'iot:alarm:config:list';
UPDATE sys_menu SET perms = 'iot:alarmConfig:query' WHERE BINARY perms = 'iot:alarm:config:query';
UPDATE sys_menu SET perms = 'iot:alarmConfig:add' WHERE BINARY perms = 'iot:alarm:config:add';
UPDATE sys_menu SET perms = 'iot:alarmConfig:edit' WHERE BINARY perms = 'iot:alarm:config:edit';
UPDATE sys_menu SET perms = 'iot:alarmConfig:remove' WHERE BINARY perms = 'iot:alarm:config:remove';
UPDATE sys_menu SET perms = 'iot:alarmConfig:list' WHERE BINARY perms = 'iot:alert:rule:list';
UPDATE sys_menu SET perms = 'iot:alarmConfig:query' WHERE BINARY perms = 'iot:alert:rule:query';
UPDATE sys_menu SET perms = 'iot:alarmConfig:add' WHERE BINARY perms = 'iot:alert:rule:add';
UPDATE sys_menu SET perms = 'iot:alarmConfig:edit' WHERE BINARY perms = 'iot:alert:rule:edit';
UPDATE sys_menu SET perms = 'iot:alarmConfig:remove' WHERE BINARY perms = 'iot:alert:rule:remove';
UPDATE sys_menu SET perms = 'iot:alarmRecord:list' WHERE BINARY perms = 'iot:alarm:record:list';
UPDATE sys_menu SET perms = 'iot:alarmRecord:query' WHERE BINARY perms = 'iot:alarm:record:query';
UPDATE sys_menu SET perms = 'iot:alarmRecord:handle' WHERE BINARY perms = 'iot:alarm:record:handle';
UPDATE sys_menu SET perms = 'iot:alarmRecord:list' WHERE BINARY perms = 'iot:alert:record:list';
UPDATE sys_menu SET perms = 'iot:alarmRecord:query' WHERE BINARY perms = 'iot:alert:record:query';
UPDATE sys_menu SET perms = 'iot:alarmRecord:handle' WHERE BINARY perms = 'iot:alert:record:handle';
UPDATE sys_menu SET perms = 'iot:deviceLog:list' WHERE BINARY perms = 'iot:device:log:list';
UPDATE sys_menu SET perms = 'iot:deviceLog:query' WHERE BINARY perms = 'iot:device:log:query';
UPDATE sys_menu SET perms = 'iot:deviceLog:export' WHERE BINARY perms = 'iot:device:log:export';
COMMIT;
