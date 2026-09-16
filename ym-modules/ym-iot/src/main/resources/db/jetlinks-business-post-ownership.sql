-- ORDER: backup legacy 30 device tenants -> ownership.sql bootstrap+marker COMMIT
-- -> this separate DDL -> start the active consumer. Never run inside ownership transaction.
-- The failing CHECK deliberately stops an uninitialized migration; do not use mysql --force.
CREATE TEMPORARY TABLE ym_iot_migration_precondition (
 ready INT NOT NULL CHECK (ready = 1)
);
INSERT INTO ym_iot_migration_precondition(ready)
 SELECT CASE WHEN COUNT(*)=1 THEN 1 ELSE 0 END
 FROM iot_device_ownership_migration WHERE migration_key='jetlinks-ownership-v1' AND device_count=30 AND binding_count=4;
DROP TEMPORARY TABLE ym_iot_migration_precondition;
-- Preserve the original tenant values; NULL is only for new unassigned technical mirrors.
ALTER TABLE iot_device MODIFY COLUMN tenant_id VARCHAR(20) NULL DEFAULT NULL,
 MODIFY COLUMN device_category VARCHAR(64) NULL;
ALTER TABLE iot_product MODIFY COLUMN tenant_id VARCHAR(20) NULL DEFAULT NULL,
 MODIFY COLUMN device_category VARCHAR(64) NULL;
