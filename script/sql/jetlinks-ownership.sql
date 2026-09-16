-- MySQL 8, AUTHORIZED SOURCE MIGRATION ONLY. This file has not been run against a runtime database.
-- Run from ym-iot with ordinary writers stopped, using a client that stops on SQL errors (never --force).
-- Set @ownership_saas_schema, @ownership_parent_menu_id and @ownership_app_id from verified sys_menu evidence before sourcing.
-- The existing ym-iot / ym-agriculture schemas are on the same MySQL server. No business rows are modified.
-- This exact bootstrap expects 30 current iot_device rows: 29 tenant 658226, 1 tenant 000000,
-- and the four existing sf_field_iot rows. An unexpected source or any preexisting ownership conflicts aborts.
-- No tenant fallback, INSERT IGNORE, REPLACE or duplicate-key overwrite is used.
-- '1000-01-01' is an explicit unbounded legacy baseline, NOT a claim about the actual assignment date.
-- This baseline keeps all pre-migration MySQL DATETIME business history visible to its stored old tenant.
-- Once applied, the marker makes retries a no-op, even after legitimate subsequent ownership transfers.
-- Review validation SELECTs at the end. DDL can persist after a later precheck failure; DML rolls back.
USE `ym-iot`;

CREATE TABLE IF NOT EXISTS iot_device_ownership (
    device_id BIGINT NOT NULL COMMENT 'Long identifier; JetLinks ID is this same decimal string',
    tenant_id VARCHAR(20) COLLATE utf8mb4_bin NULL COMMENT 'NULL is unassigned; 000000 is a real tenant',
    assignment_version BIGINT NOT NULL COMMENT 'Published version V, freeze uses V+1, next assignment or RECOVER publishes V+2',
    effective_from DATETIME(3) NOT NULL,
    fence_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (device_id),
    KEY idx_ownership_tenant (tenant_id, device_id),
    CONSTRAINT chk_ownership_version CHECK (assignment_version >= 0),
    CONSTRAINT chk_ownership_tenant CHECK (tenant_id IS NULL OR CHAR_LENGTH(TRIM(tenant_id)) > 0),
    CONSTRAINT chk_ownership_fence CHECK (fence_status IN ('ACTIVE','FROZEN','SYNC_PENDING'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS iot_device_ownership_history (
    device_id BIGINT NOT NULL,
    assignment_version BIGINT NOT NULL,
    tenant_id VARCHAR(20) COLLATE utf8mb4_bin NULL,
    previous_tenant_id VARCHAR(20) COLLATE utf8mb4_bin NULL,
    effective_from DATETIME(3) NOT NULL,
    effective_to DATETIME(3) NULL,
    action VARCHAR(20) NOT NULL,
    operator_id BIGINT NULL COMMENT 'NULL only for bootstrap; never fabricate a human actor',
    operator_tenant_id VARCHAR(20) COLLATE utf8mb4_bin NULL,
    reason VARCHAR(500) NOT NULL,
    PRIMARY KEY (device_id, assignment_version),
    KEY idx_ownership_history_tenant_time (tenant_id, device_id, effective_from, effective_to),
    CONSTRAINT chk_ownership_interval CHECK (effective_to IS NULL OR effective_to >= effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS iot_device_ownership_migration (
    migration_key VARCHAR(100) NOT NULL PRIMARY KEY,
    applied_at DATETIME(3) NOT NULL,
    device_count INT NOT NULL,
    binding_count INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Durable rule-scope cleanup; never delete the saved notification-rule mirror.
CREATE TABLE IF NOT EXISTS iot_device_ownership_rule_cleanup (
    device_id BIGINT NOT NULL,
    assignment_version BIGINT NOT NULL,
    previous_tenant_id VARCHAR(20) COLLATE utf8mb4_bin NOT NULL,
    previous_assignment_version BIGINT NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    operator_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL,
    completed_at DATETIME(3) NULL,
    PRIMARY KEY (device_id, assignment_version),
    UNIQUE KEY uk_ownership_cleanup_request (request_id),
    CONSTRAINT chk_ownership_cleanup_status CHECK (status IN ('PENDING','DONE')),
    CONSTRAINT chk_ownership_cleanup_version CHECK (assignment_version = previous_assignment_version + 2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS migrate_jetlinks_ownership;
DELIMITER $$
CREATE PROCEDURE migrate_jetlinks_ownership()
main: BEGIN
    DECLARE claimed_lock INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        IF claimed_lock = 1 THEN DO RELEASE_LOCK('ym-iot.jetlinks-ownership-bootstrap'); END IF;
        RESIGNAL;
    END;
    SELECT GET_LOCK('ym-iot.jetlinks-ownership-bootstrap', 10) INTO claimed_lock;
    IF claimed_lock IS NULL OR claimed_lock <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Ownership migration lock unavailable';
    END IF;
    START TRANSACTION;
    IF EXISTS (SELECT 1 FROM iot_device_ownership_migration WHERE migration_key='jetlinks-ownership-v1') THEN
        IF (SELECT COUNT(*) FROM iot_device_ownership_history WHERE action='INITIALIZE') <> 30
           OR (SELECT COUNT(*) FROM iot_device_ownership_history WHERE action='INITIALIZE' AND tenant_id='658226') <> 29
           OR (SELECT COUNT(*) FROM iot_device_ownership_history WHERE action='INITIALIZE' AND tenant_id='000000') <> 1
           OR EXISTS (SELECT 1 FROM iot_device_ownership_history h LEFT JOIN iot_device_ownership o ON o.device_id=h.device_id
                      WHERE h.action='INITIALIZE' AND (o.device_id IS NULL OR o.assignment_version<h.assignment_version)) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Applied ownership bootstrap audit is inconsistent; manual investigation required';
        END IF;
        COMMIT;
        DO RELEASE_LOCK('ym-iot.jetlinks-ownership-bootstrap');
        LEAVE main;
    END IF;
    IF (SELECT COUNT(*) FROM iot_device WHERE del_flag='0') <> 30
       OR (SELECT COUNT(*) FROM iot_device WHERE del_flag='0' AND BINARY tenant_id=BINARY '658226') <> 29
       OR (SELECT COUNT(*) FROM iot_device WHERE del_flag='0' AND BINARY tenant_id=BINARY '000000') <> 1
       OR EXISTS (SELECT 1 FROM iot_device WHERE del_flag='0' AND
                  (device_id<=0 OR tenant_id IS NULL OR CHAR_LENGTH(tenant_id)<>6 OR TRIM(tenant_id)<>tenant_id)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected exactly 30 current devices (29 tenant658226,1 tenant000000); no tenant fallback allowed';
    END IF;
    IF (SELECT COUNT(*) FROM `ym-agriculture`.sf_field_iot) <> 4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected four original field bindings; no bindings will be rewritten';
    END IF;
    -- Every existing active binding must identify exactly one current device of that SAME tenant.
    IF EXISTS (
        SELECT f.id FROM `ym-agriculture`.sf_field_iot f
        LEFT JOIN iot_device d ON d.device_code=f.device_sn AND d.del_flag='0'
        WHERE f.del_flag='0'
        GROUP BY f.id,f.tenant_id
        HAVING COUNT(d.device_id)<>1 OR SUM(BINARY d.tenant_id=BINARY f.tenant_id)<>1
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing field binding has ambiguous/missing device or conflicting tenant; migration stopped';
    END IF;
    -- A prior independent migration is a conflict, even if its current tenant happens to match.
    -- Only our complete marker authorizes an idempotent retry. Never repair partial state by guessing.
    IF EXISTS (SELECT 1 FROM iot_device_ownership)
       OR EXISTS (SELECT 1 FROM iot_device_ownership_history) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Ownership tables already contain rows without our complete marker; refusing to overwrite conflicts';
    END IF;
    INSERT INTO iot_device_ownership (device_id,tenant_id,assignment_version,effective_from,fence_status)
    SELECT device_id,tenant_id,1,'1000-01-01 00:00:00.000','ACTIVE' FROM iot_device WHERE del_flag='0';
    INSERT INTO iot_device_ownership_history
        (device_id,assignment_version,tenant_id,previous_tenant_id,effective_from,effective_to,action,operator_id,operator_tenant_id,reason)
    SELECT device_id,1,tenant_id,NULL,effective_from,NULL,'INITIALIZE',NULL,NULL,
        'Exact existing iot_device ownership; unbounded legacy history baseline'
    FROM iot_device_ownership;
    IF (SELECT COUNT(*) FROM iot_device_ownership) <> 30
       OR (SELECT COUNT(*) FROM iot_device_ownership_history) <> 30
       OR EXISTS (SELECT 1 FROM iot_device d LEFT JOIN iot_device_ownership o ON o.device_id=d.device_id
                  WHERE d.del_flag='0' AND (o.device_id IS NULL OR BINARY o.tenant_id<>BINARY d.tenant_id)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Ownership copy validation failed; rolling back';
    END IF;
    INSERT INTO iot_device_ownership_migration VALUES ('jetlinks-ownership-v1',CURRENT_TIMESTAMP(3),30,4);
    COMMIT;
    DO RELEASE_LOCK('ym-iot.jetlinks-ownership-bootstrap');
END$$
DELIMITER ;
CALL migrate_jetlinks_ownership();
DROP PROCEDURE migrate_jetlinks_ownership;

-- Read-only evidence. Current counts may legitimately differ after later business assignments.
SELECT tenant_id,COUNT(*) AS initialized_devices FROM iot_device_ownership_history WHERE action='INITIALIZE' GROUP BY tenant_id;
SELECT * FROM iot_device_ownership_migration WHERE migration_key='jetlinks-ownership-v1';
SELECT COUNT(*) AS existing_field_bindings FROM `ym-agriculture`.sf_field_iot;

-- Explicit SaaS schema input is required for permission registration; NEVER assume a default schema.
-- Required explicit inputs (verified for this deployment; no input is defaulted by this script):
-- SET @ownership_saas_schema='ry-cloud';
-- SET @ownership_parent_menu_id=2036363230619754498;
-- SET @ownership_app_id=1762100000000000102;
-- The selected parent must belong to this app, be a C page and have path=device.
-- This second transaction is independently idempotent. It adds one button only and never rewrites
-- any existing menu, role or tenant-package grants. Only platform superadmins may use the API.
DROP PROCEDURE IF EXISTS register_jetlinks_ownership_permission;
DELIMITER $$
CREATE PROCEDURE register_jetlinks_ownership_permission()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
    IF @ownership_saas_schema IS NULL OR @ownership_saas_schema NOT REGEXP '^[A-Za-z0-9_-]+$' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Set ownership_saas_schema explicitly to the verified SaaS database, then rerun; ownership copy is idempotent';
    END IF;
    IF @ownership_parent_menu_id IS NULL OR @ownership_app_id IS NULL
       OR CAST(@ownership_parent_menu_id AS CHAR) NOT REGEXP '^[1-9][0-9]{0,18}$'
       OR CAST(@ownership_app_id AS CHAR) NOT REGEXP '^[1-9][0-9]{0,18}$'
       OR CAST(@ownership_parent_menu_id AS DECIMAL(20,0))>9223372036854775807
       OR CAST(@ownership_app_id AS DECIMAL(20,0))>9223372036854775807 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Set positive Long ownership_parent_menu_id and ownership_app_id explicitly from verified sys_menu evidence';
    END IF;
    START TRANSACTION;
    SET @ownership_permission_sql=CONCAT('SELECT COUNT(*) INTO @ownership_parent_count FROM `',
        @ownership_saas_schema,'`.sys_menu WHERE menu_id=@ownership_parent_menu_id AND app_id=@ownership_app_id AND menu_type=''C'' AND path=''device''');
    PREPARE ownership_permission_stmt FROM @ownership_permission_sql;
    EXECUTE ownership_permission_stmt;
    DEALLOCATE PREPARE ownership_permission_stmt;
    IF @ownership_parent_count<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Explicit parent/app mismatch: expected one C menu with path=device; no parent will be guessed';
    END IF;
    SET @ownership_permission_sql=CONCAT('SELECT COUNT(*),SUM(parent_id=@ownership_parent_menu_id AND app_id=@ownership_app_id AND menu_type=''F'') INTO @ownership_perm_count,@ownership_perm_matches FROM `',
        @ownership_saas_schema,'`.sys_menu WHERE perms=''iot:device:assign''');
    PREPARE ownership_permission_stmt FROM @ownership_permission_sql;
    EXECUTE ownership_permission_stmt;
    DEALLOCATE PREPARE ownership_permission_stmt;
    IF @ownership_perm_count>1 OR (@ownership_perm_count=1 AND COALESCE(@ownership_perm_matches,0)<>1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing iot:device:assign permission conflicts with requested menu; not overwritten';
    END IF;
    IF @ownership_perm_count=0 THEN
        SET @ownership_permission_sql=CONCAT('INSERT INTO `',@ownership_saas_schema,
            '`.sys_menu(menu_id,app_id,menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_time,remark) ',
            'VALUES (1761400000000030021,@ownership_app_id,''设备归属管理'',@ownership_parent_menu_id,90,''#'',''N'',''Y'',''F'',''0'',''0'',''iot:device:assign'',''#'',CURRENT_TIMESTAMP,''平台管理员设备分配、释放、转移及历史'')');
        PREPARE ownership_permission_stmt FROM @ownership_permission_sql;
        EXECUTE ownership_permission_stmt;
        DEALLOCATE PREPARE ownership_permission_stmt;
    END IF;
    COMMIT;
END$$
DELIMITER ;
CALL register_jetlinks_ownership_permission();
DROP PROCEDURE register_jetlinks_ownership_permission;
-- Do not add ownership tables to tenant.excludes: JDBC access SQL is scoped by authenticated tenant.
