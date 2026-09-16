-- MySQL 8. Source migration only; not applied to production by this task.
-- Explicit verified deployment inputs, same client session (no implicit schema/app fallback):
-- SET @iot_permissions_saas_schema='ry-cloud';
-- SET @iot_permissions_app_id=1762100000000000102;
-- SET @iot_permissions_agriculture_app_id=1762100000000000101;
-- Run after ownership.sql. Stop on errors, never mysql --force.
-- Fill NULL/blank permission on ten existing active C pages so existing page roles retain READ access.
-- Reuse existing exact button permissions. Register missing actions without granting any role/package.
-- Existing nonempty permission, menu identity or duplicate conflicts abort the entire DML transaction.
-- No user/business/device data or role/tenant-package grants are changed. Sessions need refreshed permissions.
-- Select the verified SaaS database as the mysql client default before sourcing this file.
-- This file is UTF-8. Set the client charset before creating SQL literals and procedures.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_jetlinks_http_permissions;
DELIMITER $$
CREATE PROCEDURE migrate_jetlinks_http_permissions()
BEGIN
    DECLARE locked INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        IF locked=1 THEN DO RELEASE_LOCK('jetlinks-http-permissions'); END IF;
        RESIGNAL;
    END;
    IF @iot_permissions_saas_schema IS NULL OR @iot_permissions_saas_schema NOT REGEXP '^[A-Za-z0-9_-]+$'
       OR @iot_permissions_app_id IS NULL OR @iot_permissions_agriculture_app_id IS NULL
       OR CAST(@iot_permissions_app_id AS CHAR) NOT REGEXP '^[1-9][0-9]{0,18}$'
       OR CAST(@iot_permissions_agriculture_app_id AS CHAR) NOT REGEXP '^[1-9][0-9]{0,18}$'
       OR CAST(@iot_permissions_app_id AS DECIMAL(20,0))>9223372036854775807
       OR CAST(@iot_permissions_agriculture_app_id AS DECIMAL(20,0))>9223372036854775807 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Set explicit verified SaaS schema and both positive Long app IDs';
    END IF;
    SELECT GET_LOCK('jetlinks-http-permissions',10) INTO locked;
    IF locked IS NULL OR locked<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='HTTP permission migration lock unavailable'; END IF;
    DROP TEMPORARY TABLE IF EXISTS expected_iot_http_pages;
    DROP TEMPORARY TABLE IF EXISTS expected_iot_http_buttons;
    CREATE TEMPORARY TABLE expected_iot_http_pages (
        menu_id BIGINT PRIMARY KEY,app_id BIGINT NOT NULL,parent_id BIGINT NOT NULL,
        path VARCHAR(200) NOT NULL,perms VARCHAR(100) NOT NULL UNIQUE
    );
    CREATE TEMPORARY TABLE expected_iot_http_buttons (
        menu_id BIGINT PRIMARY KEY,app_id BIGINT NOT NULL,parent_id BIGINT NOT NULL,
        menu_name VARCHAR(64) NOT NULL,perms VARCHAR(100) NOT NULL UNIQUE,order_num INT NOT NULL
    );
    INSERT INTO expected_iot_http_pages VALUES
        (2036362780382191617,@iot_permissions_app_id,2036362602250100737,'product','iot:product:list'),
        (2036363230619754498,@iot_permissions_app_id,2036362602250100737,'device','iot:device:list'),
        (2036364100237385730,@iot_permissions_app_id,2036362602250100737,'device/log','iot:deviceLog:list'),
        (2036363886457905154,@iot_permissions_app_id,2036363426850267138,'rule','iot:alarmConfig:list'),
        (2036363610095214593,@iot_permissions_app_id,2036363426850267138,'record','iot:alarmRecord:list'),
        (2042078063734329346,@iot_permissions_app_id,2070032256496316418,'iot','iot:monitor:list'),
        (2042078150459953154,@iot_permissions_app_id,2070032256496316418,'camera','iot:video:list'),
        (2059842701977833473,@iot_permissions_app_id,2070032256496316418,'iot/fertilizer','iot:fertilizer:list'),
        (2061333728416821250,@iot_permissions_app_id,2070032256496316418,'motorvalve','iot:motorvalve:list'),
        (2035915490939580417,@iot_permissions_agriculture_app_id,2034884382739337217,'field','sf:field:list');
    INSERT INTO expected_iot_http_buttons VALUES
        (2080909000000100000,@iot_permissions_app_id,2036363610095214593,'报警记录处理','iot:alarmRecord:handle',100),
        (2080909000000100001,@iot_permissions_app_id,2036363610095214593,'报警记录详情读取','iot:alarmRecord:query',101),
        (2080909000000100002,@iot_permissions_app_id,2036363886457905154,'报警规则新增','iot:alarmConfig:add',102),
        (2080909000000100003,@iot_permissions_app_id,2036363886457905154,'报警规则修改','iot:alarmConfig:edit',103),
        (2080909000000100004,@iot_permissions_app_id,2036363886457905154,'报警规则详情读取','iot:alarmConfig:query',104),
        (2080909000000100005,@iot_permissions_app_id,2036363886457905154,'报警规则归档/删除','iot:alarmConfig:remove',105),
        (2080909000000100006,@iot_permissions_app_id,2036363230619754498,'设备新增','iot:device:add',106),
        (2080909000000100007,@iot_permissions_app_id,2036363230619754498,'设备归属管理','iot:device:assign',107),
        (2080909000000100008,@iot_permissions_app_id,2036363230619754498,'设备修改','iot:device:edit',108),
        (2080909000000100009,@iot_permissions_app_id,2036363230619754498,'设备导出','iot:device:export',109),
        (2080909000000100010,@iot_permissions_app_id,2036364100237385730,'设备日志导出','iot:deviceLog:export',110),
        (2080909000000100011,@iot_permissions_app_id,2036364100237385730,'设备日志详情读取','iot:deviceLog:query',111),
        (2080909000000100012,@iot_permissions_app_id,2036363230619754498,'设备详情读取','iot:device:query',112),
        (2080909000000100013,@iot_permissions_app_id,2036363230619754498,'设备归档/删除','iot:device:remove',113),
        (2080909000000100014,@iot_permissions_app_id,2036363230619754498,'设备重置密钥','iot:device:resetSecret',114),
        (2080909000000100015,@iot_permissions_app_id,2036363230619754498,'事件恢复详情读取','iot:event:query',115),
        (2080909000000100016,@iot_permissions_app_id,2036363230619754498,'事件恢复重放/重试','iot:event:replay',116),
        (2080909000000100017,@iot_permissions_app_id,2059842701977833473,'施肥机清罐','iot:fertilizer:cleanTank',117),
        (2080909000000100018,@iot_permissions_app_id,2059842701977833473,'施肥机急停','iot:fertilizer:emergencyStop',118),
        (2080909000000100019,@iot_permissions_app_id,2059842701977833473,'施肥机参数下发','iot:fertilizer:paramApply',119),
        (2080909000000100020,@iot_permissions_app_id,2059842701977833473,'施肥机加引水','iot:fertilizer:primeWater',120),
        (2080909000000100021,@iot_permissions_app_id,2059842701977833473,'施肥机主动读取','iot:fertilizer:read',121),
        (2080909000000100022,@iot_permissions_app_id,2059842701977833473,'施肥机复位','iot:fertilizer:reset',122),
        (2080909000000100023,@iot_permissions_app_id,2059842701977833473,'施肥机启动','iot:fertilizer:start',123),
        (2080909000000100024,@iot_permissions_app_id,2059842701977833473,'施肥机无复位启动','iot:fertilizer:startWithoutReset',124),
        (2080909000000100025,@iot_permissions_app_id,2059842701977833473,'施肥机停止','iot:fertilizer:stop',125),
        (2080909000000100026,@iot_permissions_app_id,2061333728416821250,'阀门批量控制','iot:motorvalve:batchControl',126),
        (2080909000000100027,@iot_permissions_app_id,2061333728416821250,'阀门控制','iot:motorvalve:control',127),
        (2080909000000100028,@iot_permissions_app_id,2061333728416821250,'阀门时间同步','iot:motorvalve:ntpSync',128),
        (2080909000000100029,@iot_permissions_app_id,2061333728416821250,'阀门开度控制','iot:motorvalve:percentControl',129),
        (2080909000000100030,@iot_permissions_app_id,2061333728416821250,'阀门主动读取','iot:motorvalve:read',130),
        (2080909000000100031,@iot_permissions_app_id,2036362780382191617,'产品新增','iot:product:add',131),
        (2080909000000100032,@iot_permissions_app_id,2036362780382191617,'产品修改','iot:product:edit',132),
        (2080909000000100033,@iot_permissions_app_id,2036362780382191617,'产品导出','iot:product:export',133),
        (2080909000000100034,@iot_permissions_app_id,2036362780382191617,'产品详情读取','iot:product:query',134),
        (2080909000000100035,@iot_permissions_app_id,2036362780382191617,'产品归档/删除','iot:product:remove',135),
        (2080909000000100036,@iot_permissions_app_id,2036362780382191617,'属性新增','iot:property:add',136),
        (2080909000000100037,@iot_permissions_app_id,2036362780382191617,'属性修改','iot:property:edit',137),
        (2080909000000100038,@iot_permissions_app_id,2036362780382191617,'属性列表读取','iot:property:list',138),
        (2080909000000100039,@iot_permissions_app_id,2036362780382191617,'属性详情读取','iot:property:query',139),
        (2080909000000100040,@iot_permissions_app_id,2036362780382191617,'属性归档/删除','iot:property:remove',140),
        (2080909000000100041,@iot_permissions_app_id,2036363230619754498,'遥测接收上报','iot:telemetry:ingress',141),
        (2080909000000100042,@iot_permissions_app_id,2042078150459953154,'视频控制','iot:video:control',142),
        (2080909000000100043,@iot_permissions_app_id,2042078150459953154,'视频播放','iot:video:play',143);
    START TRANSACTION;
    SET @iot_http_sql=CONCAT('SELECT COUNT(*) INTO @iot_http_conflicts FROM expected_iot_http_pages e LEFT JOIN `',@iot_permissions_saas_schema,'`.sys_menu m ON m.menu_id=e.menu_id WHERE m.menu_id IS NULL OR NOT(m.app_id<=>e.app_id) OR NOT(m.parent_id<=>e.parent_id) OR NOT(BINARY m.path<=>BINARY e.path) OR NOT(m.menu_type<=>''C'') OR COALESCE(m.status,'''')<>''0'' OR (NULLIF(TRIM(m.perms),'''') IS NOT NULL AND BINARY m.perms<>BINARY e.perms)');
    PREPARE iot_http_stmt FROM @iot_http_sql; EXECUTE iot_http_stmt; DEALLOCATE PREPARE iot_http_stmt;
    IF @iot_http_conflicts<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Verified page identity or existing permission conflicts; no menus changed'; END IF;
    SET @iot_http_sql=CONCAT('SELECT COUNT(*) INTO @iot_http_conflicts FROM expected_iot_http_pages e JOIN `',@iot_permissions_saas_schema,'`.sys_menu m ON m.perms=e.perms WHERE m.menu_id<>e.menu_id');
    PREPARE iot_http_stmt FROM @iot_http_sql; EXECUTE iot_http_stmt; DEALLOCATE PREPARE iot_http_stmt;
    IF @iot_http_conflicts<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Page read permission already registered elsewhere; no permission overwritten'; END IF;
    SET @iot_http_sql=CONCAT('SELECT COUNT(*) INTO @iot_http_conflicts FROM (SELECT e.perms FROM expected_iot_http_buttons e JOIN `',@iot_permissions_saas_schema,'`.sys_menu m ON m.perms=e.perms GROUP BY e.perms HAVING COUNT(*)<>1 OR COALESCE(SUM(m.app_id=e.app_id AND m.parent_id=e.parent_id AND m.menu_type=''F'' AND BINARY m.perms=BINARY e.perms),0)<>1) conflicts');
    PREPARE iot_http_stmt FROM @iot_http_sql; EXECUTE iot_http_stmt; DEALLOCATE PREPARE iot_http_stmt;
    IF @iot_http_conflicts<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing action permission conflicts with verified parent/app; no overwrite'; END IF;
    SET @iot_http_sql=CONCAT('SELECT COUNT(*) INTO @iot_http_conflicts FROM expected_iot_http_buttons e JOIN `',@iot_permissions_saas_schema,'`.sys_menu m ON m.menu_id=e.menu_id WHERE NOT EXISTS (SELECT 1 FROM `',@iot_permissions_saas_schema,'`.sys_menu assigned WHERE assigned.perms=e.perms)');
    PREPARE iot_http_stmt FROM @iot_http_sql; EXECUTE iot_http_stmt; DEALLOCATE PREPARE iot_http_stmt;
    IF @iot_http_conflicts<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reserved new button ID is already used; no row overwritten'; END IF;
    SET @iot_http_sql=CONCAT('UPDATE `',@iot_permissions_saas_schema,'`.sys_menu m JOIN expected_iot_http_pages e ON e.menu_id=m.menu_id SET m.perms=e.perms WHERE NULLIF(TRIM(m.perms),'''') IS NULL');
    PREPARE iot_http_stmt FROM @iot_http_sql; EXECUTE iot_http_stmt; DEALLOCATE PREPARE iot_http_stmt;
    SET @iot_http_sql=CONCAT('INSERT INTO `',@iot_permissions_saas_schema,'`.sys_menu(menu_id,app_id,menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_time,remark) SELECT e.menu_id,e.app_id,e.menu_name,e.parent_id,e.order_num,''#'',''N'',''Y'',''F'',''0'',''0'',e.perms,''#'',CURRENT_TIMESTAMP,''HTTP功能权限；新增按钮须由管理员明确授权'' FROM expected_iot_http_buttons e WHERE NOT EXISTS (SELECT 1 FROM `',@iot_permissions_saas_schema,'`.sys_menu m WHERE m.perms=e.perms)');
    PREPARE iot_http_stmt FROM @iot_http_sql; EXECUTE iot_http_stmt; DEALLOCATE PREPARE iot_http_stmt;
    COMMIT;
    DO RELEASE_LOCK('jetlinks-http-permissions');
    DROP TEMPORARY TABLE expected_iot_http_pages;
    DROP TEMPORARY TABLE expected_iot_http_buttons;
END$$
DELIMITER ;
CALL migrate_jetlinks_http_permissions();
DROP PROCEDURE migrate_jetlinks_http_permissions;
