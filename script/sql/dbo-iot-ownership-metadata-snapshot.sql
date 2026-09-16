-- MySQL 8 incremental migration; REVIEW/APPLY SEPARATELY, never run automatically at application startup.
-- Prerequisites: back up ym-iot.iot_device_ownership_history (including any existing snapshot values),
-- confirm the exact database, and apply jetlinks-ownership.sql + dbo-iot-ownership-audit.sql previously.
-- Do NOT rerun the ownership initializer as part of this change. Existing tables must be InnoDB.
-- Apply before starting the snapshot-enabled DBO writer or historical reader. Serialize migration runs;
-- ALTER TABLE commits implicitly and may wait on metadata locks: use the agreed maintenance window.
-- Migration principal: ALTER on this table; CREATE ROUTINE, ALTER ROUTINE, EXECUTE on ym-iot;
-- SELECT on this table for verification and visibility of its information_schema definition.
-- No account creation, password, GRANT or runtime DDL permission is embedded here.
-- Runtime grants: the existing DBO SELECT/INSERT/UPDATE table grants cover the new column;
-- ym-iot historical readers need SELECT only. If grants are column-specific, explicitly review/add
-- DBO UPDATE(metadata_snapshot) and reader SELECT(metadata_snapshot) with the DBA before rollout.
-- Keep existing technical-profile grants read-only for DBO. See dbo-iot-ownership-grants-template.sql.
USE `ym-iot`;
DROP PROCEDURE IF EXISTS add_dbo_ownership_metadata_snapshot;
DELIMITER $$
CREATE PROCEDURE add_dbo_ownership_metadata_snapshot()
BEGIN
    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()
        AND table_name IN ('iot_device_ownership','iot_device_ownership_history')
        AND engine='InnoDB') <> 2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing InnoDB ownership schema required; do not bootstrap';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='iot_device_ownership_history' AND column_name='operator_source') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Apply dbo-iot-ownership-audit.sql before snapshot migration';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='iot_device_ownership_history' AND column_name='metadata_snapshot') THEN
        ALTER TABLE iot_device_ownership_history ADD COLUMN metadata_snapshot LONGTEXT NULL
            COMMENT 'Display-only device/property whitelist captured when tenant interval closes';
    ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='iot_device_ownership_history' AND column_name='metadata_snapshot'
        AND data_type='longtext' AND is_nullable='YES' AND column_default IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing metadata_snapshot must be nullable LONGTEXT without default';
    END IF;
END$$
DELIMITER ;
CALL add_dbo_ownership_metadata_snapshot();
DROP PROCEDURE add_dbo_ownership_metadata_snapshot;

-- Read-only acceptance: verify nullable LONGTEXT and run this migration twice; the second run is a no-op.
SELECT column_name,data_type,is_nullable FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name='iot_device_ownership_history' AND column_name='metadata_snapshot';
-- Counts only: do not print snapshot contents to deployment logs.
SELECT COUNT(*) AS history_rows,COUNT(metadata_snapshot) AS snapshot_rows FROM iot_device_ownership_history;

-- Data-preserving rollback: roll back the application release and leave this nullable column AND all
-- captured JSON in place. Earlier writers name INSERT/UPDATE columns explicitly and tolerate it.
-- Do not DROP COLUMN, clear values, reopen intervals, alter owners/fences, or delete history.
-- Already closed rows remain NULL: never backfill with current device/product state or a new owner's data.
-- A future historical backfill requires independently archived evidence and a separately reviewed plan.
