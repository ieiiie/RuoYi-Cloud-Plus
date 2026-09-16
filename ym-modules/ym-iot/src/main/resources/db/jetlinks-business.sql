-- Apply only during the operator-controlled deployment. Never run on application startup.
CREATE TABLE IF NOT EXISTS iot_jetlinks_command_task (
 request_id VARCHAR(64) PRIMARY KEY, command_id VARCHAR(128) NULL,
 device_id BIGINT NOT NULL, tenant_id VARCHAR(32) NOT NULL, assignment_version BIGINT NOT NULL,
 function_id VARCHAR(128) NOT NULL, state VARCHAR(32) NOT NULL, inputs_json LONGTEXT,priority INT NOT NULL DEFAULT 0,
 result_json LONGTEXT, error_message TEXT, created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
 UNIQUE KEY uk_command_id(command_id), KEY idx_device_state(device_id,state), KEY idx_tenant(tenant_id,device_id,assignment_version)
);
CREATE TABLE IF NOT EXISTS iot_jetlinks_business_event (
 event_id VARCHAR(128) PRIMARY KEY,event_type VARCHAR(64) NOT NULL,device_id VARCHAR(64),
 source_time BIGINT NOT NULL,payload_json LONGTEXT NOT NULL,created_at DATETIME(3) NOT NULL,
 KEY idx_business_event_created(created_at,event_id),
 KEY idx_business_event_device_created(device_id,created_at,event_id),
 KEY idx_business_event_type_created(event_type,created_at,event_id)
);

-- One durable source identity maps to one business ID. Also orders catalog/command/valve projections.
CREATE TABLE IF NOT EXISTS iot_jetlinks_projection (
 projection_key VARCHAR(192) PRIMARY KEY,business_id BIGINT NOT NULL,
 source_time BIGINT NOT NULL,version BIGINT NOT NULL,payload_json LONGTEXT NOT NULL
);
-- Run jetlinks-business-post-ownership.sql AFTER ownership bootstrap commits its marker.
-- SourceRecordId replaces old lossy time/type uniqueness. MySQL 8 has no DROP INDEX IF EXISTS.
-- Both halves are independently repeatable, including recovery after a prior partial run.
SET @ym_iot_index_ddl = IF(EXISTS (
 SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
 AND table_name='iot_fertilizer_record' AND index_name='uk_iot_fert_record_tenant_device_time_type'
), 'ALTER TABLE iot_fertilizer_record DROP INDEX uk_iot_fert_record_tenant_device_time_type', 'SELECT 1');
PREPARE ym_iot_index_stmt FROM @ym_iot_index_ddl;
EXECUTE ym_iot_index_stmt;
DEALLOCATE PREPARE ym_iot_index_stmt;
SET @ym_iot_index_ddl = IF(NOT EXISTS (
 SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
 AND table_name='iot_fertilizer_record' AND index_name='idx_iot_fert_record_tenant_device_time_type'
), 'ALTER TABLE iot_fertilizer_record ADD INDEX idx_iot_fert_record_tenant_device_time_type(tenant_id,device_id,record_time,fertilization_type)', 'SELECT 1');
PREPARE ym_iot_index_stmt FROM @ym_iot_index_ddl;
EXECUTE ym_iot_index_stmt;
DEALLOCATE PREPARE ym_iot_index_stmt;

CREATE TABLE IF NOT EXISTS iot_jetlinks_category (
 category_id VARCHAR(64) PRIMARY KEY,code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,name VARCHAR(128) NOT NULL,
 parent_id VARCHAR(64),sort_index BIGINT NOT NULL DEFAULT 0,archived TINYINT NOT NULL DEFAULT 0,
 source_time BIGINT NOT NULL,version BIGINT NOT NULL,UNIQUE KEY uk_category_code(code)
);
-- Native category codes are case-sensitive: built-in 'other' and migrated 'OTHER' both exist.
-- Repeat-safe for already-created mirrors; preserve both IDs and the unique code constraint.
ALTER TABLE iot_jetlinks_category MODIFY COLUMN code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
CREATE TABLE IF NOT EXISTS iot_jetlinks_event_failure (
 event_id VARCHAR(128) PRIMARY KEY,consumer_id VARCHAR(128) NOT NULL,event_type VARCHAR(64) NOT NULL,
 payload_json LONGTEXT NOT NULL,state VARCHAR(32) NOT NULL,attempts INT NOT NULL DEFAULT 0,
 last_error TEXT,replay_by VARCHAR(64),updated_at DATETIME(3) NOT NULL,KEY idx_failure_state(state,updated_at)
);
CREATE TABLE IF NOT EXISTS iot_jetlinks_notification (
 alert_id BIGINT NOT NULL,phone VARCHAR(32) NOT NULL,tenant_id VARCHAR(32) NOT NULL,
 payload_json LONGTEXT NOT NULL,state VARCHAR(32) NOT NULL,attempts INT NOT NULL DEFAULT 0,
 last_error TEXT,replay_by VARCHAR(64),next_retry_at DATETIME(3) NOT NULL,updated_at DATETIME(3) NOT NULL,
 PRIMARY KEY(alert_id,phone),KEY idx_notification_due(state,next_retry_at)
);
