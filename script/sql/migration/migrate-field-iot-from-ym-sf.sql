-- 将旧单体库 ym-sf 的地块设备绑定迁移至农业服务库。
-- 执行前请备份源库与目标库；脚本可重复执行。

DROP PROCEDURE IF EXISTS check_sf_field_iot_unique;
DELIMITER //
CREATE PROCEDURE check_sf_field_iot_unique()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `ym-sf`.`sf_field_iot`
    WHERE del_flag = '0'
    GROUP BY tenant_id, device_sn
    HAVING COUNT(DISTINCT field_id) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '存在同一租户设备绑定多个地块的历史数据，请清理后重试';
  END IF;
END//
DELIMITER ;
CALL check_sf_field_iot_unique();
DROP PROCEDURE check_sf_field_iot_unique;

INSERT INTO `ym-agriculture`.`sf_field_iot` (
  id, tenant_id, field_id, device_sn, del_flag,
  create_dept, create_by, create_time, update_by, update_time, remark
)
SELECT
  id, tenant_id, field_id, device_sn, del_flag,
  create_dept, create_by, create_time, update_by, update_time, remark
FROM `ym-sf`.`sf_field_iot`
WHERE del_flag = '0'
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  field_id = VALUES(field_id),
  device_sn = VALUES(device_sn),
  del_flag = VALUES(del_flag),
  create_dept = VALUES(create_dept),
  create_by = VALUES(create_by),
  create_time = VALUES(create_time),
  update_by = VALUES(update_by),
  update_time = VALUES(update_time),
  remark = VALUES(remark);
