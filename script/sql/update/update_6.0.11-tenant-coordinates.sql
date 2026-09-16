-- 对齐 ym-test.sys_tenant 的租户中心点坐标字段。
-- 两个字段均可空，不修改或回填存量租户数据；脚本可重复执行。

DROP PROCEDURE IF EXISTS upgrade_6_0_11_tenant_coordinates;
DELIMITER $$
CREATE PROCEDURE upgrade_6_0_11_tenant_coordinates()
BEGIN
    DECLARE v_longitude_column_count INT DEFAULT 0;
    DECLARE v_latitude_column_count INT DEFAULT 0;

    SELECT COUNT(*) INTO v_longitude_column_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_tenant'
      AND column_name = 'longitude';

    IF v_longitude_column_count = 0 THEN
        ALTER TABLE sys_tenant
            ADD COLUMN longitude DECIMAL(10, 7) DEFAULT NULL
                COMMENT '租户所在地中心点经度，来源 sys_region.center_lng'
                AFTER region_name;
    END IF;

    SELECT COUNT(*) INTO v_latitude_column_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_tenant'
      AND column_name = 'latitude';

    IF v_latitude_column_count = 0 THEN
        ALTER TABLE sys_tenant
            ADD COLUMN latitude DECIMAL(10, 7) DEFAULT NULL
                COMMENT '租户所在地中心点纬度，来源 sys_region.center_lat'
                AFTER longitude;
    END IF;
END$$
DELIMITER ;

CALL upgrade_6_0_11_tenant_coordinates();
DROP PROCEDURE upgrade_6_0_11_tenant_coordinates;
