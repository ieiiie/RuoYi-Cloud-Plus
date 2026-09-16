-- 事件回执查询索引；保留 event_id 主键、原始载荷和事务去重语义。
-- 在 ym-iot 库执行；只检查/创建本次索引，可重复运行。
-- LOCK=NONE 不支持时明确失败，不降级为锁表；短暂元数据锁最多等待 10 秒。
SET SESSION lock_wait_timeout=10;

SET @event_index_ddl = IF(NOT EXISTS (
 SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
 AND table_name='iot_jetlinks_business_event' AND index_name='idx_business_event_created'
), 'ALTER TABLE iot_jetlinks_business_event ADD INDEX idx_business_event_created(created_at,event_id), ALGORITHM=INPLACE, LOCK=NONE', 'SELECT 1');
PREPARE event_index_stmt FROM @event_index_ddl;
EXECUTE event_index_stmt;
DEALLOCATE PREPARE event_index_stmt;

SET @event_index_ddl = IF(NOT EXISTS (
 SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
 AND table_name='iot_jetlinks_business_event' AND index_name='idx_business_event_device_created'
), 'ALTER TABLE iot_jetlinks_business_event ADD INDEX idx_business_event_device_created(device_id,created_at,event_id), ALGORITHM=INPLACE, LOCK=NONE', 'SELECT 1');
PREPARE event_index_stmt FROM @event_index_ddl;
EXECUTE event_index_stmt;
DEALLOCATE PREPARE event_index_stmt;

SET @event_index_ddl = IF(NOT EXISTS (
 SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
 AND table_name='iot_jetlinks_business_event' AND index_name='idx_business_event_type_created'
), 'ALTER TABLE iot_jetlinks_business_event ADD INDEX idx_business_event_type_created(event_type,created_at,event_id), ALGORITHM=INPLACE, LOCK=NONE', 'SELECT 1');
PREPARE event_index_stmt FROM @event_index_ddl;
EXECUTE event_index_stmt;
DEALLOCATE PREPARE event_index_stmt;
