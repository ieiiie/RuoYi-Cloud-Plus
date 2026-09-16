SELECT table_schema, COUNT(*) table_count, COALESCE(SUM(table_rows), 0) estimated_rows
FROM information_schema.tables
WHERE table_schema IN ('${TARGET_AGRICULTURE_DB}', '${TARGET_IOT_DB}')
GROUP BY table_schema ORDER BY table_schema;

SELECT 'inventory_negative_balance' check_name, COUNT(*) problem_count
FROM `${TARGET_AGRICULTURE_DB}`.`sf_inventory_balance` WHERE quantity < 0
UNION ALL
SELECT 'work_order_missing_package', COUNT(*)
FROM `${TARGET_AGRICULTURE_DB}`.`sf_stask_work_order` o
LEFT JOIN `${TARGET_AGRICULTURE_DB}`.`sf_stask_task_package` p ON p.package_id = o.package_id
WHERE o.package_id IS NOT NULL AND p.package_id IS NULL
UNION ALL
SELECT 'dispatch_missing_order', COUNT(*)
FROM `${TARGET_AGRICULTURE_DB}`.`sf_stask_dispatch` d
LEFT JOIN `${TARGET_AGRICULTURE_DB}`.`sf_stask_work_order` o ON o.order_id = d.order_id
WHERE o.order_id IS NULL
UNION ALL
SELECT 'field_iot_missing_device', COUNT(*)
FROM `${TARGET_AGRICULTURE_DB}`.`sf_field_iot` f
LEFT JOIN `${TARGET_IOT_DB}`.`iot_device` d ON d.device_code = f.device_sn
WHERE d.device_id IS NULL
UNION ALL
SELECT 'telemetry_missing_device', COUNT(*)
FROM `${TARGET_IOT_DB}`.`iot_data_point` p
LEFT JOIN `${TARGET_IOT_DB}`.`iot_device` d ON d.device_id = p.device_id
WHERE d.device_id IS NULL
UNION ALL
SELECT 'alert_missing_rule', COUNT(*)
FROM `${TARGET_IOT_DB}`.`iot_alert_record` r
LEFT JOIN `${TARGET_IOT_DB}`.`iot_alert_rule` a ON a.rule_id = r.rule_id
WHERE r.rule_id IS NOT NULL AND a.rule_id IS NULL;

SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema IN ('${TARGET_AGRICULTURE_DB}', '${TARGET_IOT_DB}')
ORDER BY table_schema, table_name;
