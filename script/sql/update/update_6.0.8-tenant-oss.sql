-- SaaS 租户绑定 OSS 配置。存量租户沿用升级前唯一的默认配置。

DROP PROCEDURE IF EXISTS upgrade_6_0_8_tenant_oss;
DELIMITER $$
CREATE PROCEDURE upgrade_6_0_8_tenant_oss()
BEGIN
    DECLARE v_status_column_count INT DEFAULT 0;
    DECLARE v_binding_column_count INT DEFAULT 0;
    DECLARE v_binding_index_count INT DEFAULT 0;
    DECLARE v_default_count INT DEFAULT 0;
    DECLARE v_default_config_id BIGINT DEFAULT NULL;

    SELECT COUNT(*) INTO v_status_column_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_oss_config'
      AND column_name = 'status';

    SELECT COUNT(*) INTO v_binding_column_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_tenant'
      AND column_name = 'oss_config_id';

    IF v_status_column_count = 0 THEN
        IF v_binding_column_count = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sys_oss_config.status 已不存在，但 sys_tenant.oss_config_id 尚未创建';
        END IF;
    ELSE
        SELECT COUNT(*), MAX(oss_config_id)
        INTO v_default_count, v_default_config_id
        FROM sys_oss_config
        WHERE status = 'Y';

        IF v_default_count <> 1 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '升级要求 sys_oss_config 中恰好存在一个默认配置';
        END IF;

        IF v_binding_column_count = 0 THEN
            ALTER TABLE sys_tenant
                ADD COLUMN oss_config_id BIGINT(20) DEFAULT NULL COMMENT 'OSS配置ID' AFTER package_id;
        END IF;

        UPDATE sys_tenant
        SET oss_config_id = v_default_config_id
        WHERE oss_config_id IS NULL;

        IF EXISTS (SELECT 1 FROM sys_tenant WHERE oss_config_id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '存在未绑定 OSS 配置的租户，升级已中止';
        END IF;

        ALTER TABLE sys_tenant
            MODIFY COLUMN oss_config_id BIGINT(20) NOT NULL COMMENT 'OSS配置ID';

        SELECT COUNT(*) INTO v_binding_index_count
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_tenant'
          AND index_name = 'idx_sys_tenant_oss_config_id';

        IF v_binding_index_count = 0 THEN
            ALTER TABLE sys_tenant
                ADD KEY idx_sys_tenant_oss_config_id (oss_config_id);
        END IF;

        ALTER TABLE sys_oss_config DROP COLUMN status;
    END IF;
END$$
DELIMITER ;

CALL upgrade_6_0_8_tenant_oss();
DROP PROCEDURE upgrade_6_0_8_tenant_oss;
