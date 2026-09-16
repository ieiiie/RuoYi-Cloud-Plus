-- Incremental MySQL 8 migration; NOT executed by this implementation.
-- Apply only to existing ym-iot after backup, before deploying DBO ownership.
-- Does not rerun jetlinks-ownership.sql, change ownership, close history, or reset a fence.
USE `ym-iot`;
DROP PROCEDURE IF EXISTS add_dbo_ownership_audit;
DELIMITER $$
CREATE PROCEDURE add_dbo_ownership_audit()
BEGIN
    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN
        ('iot_device_ownership','iot_device_ownership_history','iot_device_ownership_rule_cleanup')) <> 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing ownership schema required; never bootstrap automatically';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='iot_device_ownership_history' AND column_name='operator_source') THEN
        ALTER TABLE iot_device_ownership_history ADD COLUMN operator_source VARCHAR(20) NOT NULL DEFAULT 'BUSINESS'
            COMMENT 'DBO and business operator IDs are separate namespaces';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='iot_device_ownership_rule_cleanup' AND column_name='operator_source') THEN
        ALTER TABLE iot_device_ownership_rule_cleanup ADD COLUMN operator_source VARCHAR(20) NOT NULL DEFAULT 'BUSINESS';
    END IF;
END$$
DELIMITER ;
CALL add_dbo_ownership_audit();
DROP PROCEDURE add_dbo_ownership_audit;
-- Existing pending cleanup retains BUSINESS identity; newly created DBO rows explicitly set DBO.
SELECT fence_status,COUNT(*) FROM iot_device_ownership GROUP BY fence_status;
SELECT operator_source,COUNT(*) FROM iot_device_ownership_history GROUP BY operator_source;
