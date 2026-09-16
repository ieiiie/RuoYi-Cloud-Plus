-- SaaS 租户企业资料：Logo 与省/市/区县行政区划（MySQL）

ALTER TABLE `ry-cloud`.`sys_tenant`
    ADD COLUMN `logo_url` varchar(500) DEFAULT NULL COMMENT '企业Logo地址' AFTER `company_name`,
    ADD COLUMN `province_code` varchar(12) DEFAULT NULL COMMENT '省级行政区划编码' AFTER `address`,
    ADD COLUMN `city_code` varchar(12) DEFAULT NULL COMMENT '市级行政区划编码' AFTER `province_code`,
    ADD COLUMN `district_code` varchar(12) DEFAULT NULL COMMENT '区县级行政区划编码' AFTER `city_code`,
    ADD COLUMN `region_name` varchar(255) DEFAULT NULL COMMENT '行政区划名称' AFTER `district_code`,
    ADD KEY `idx_sys_tenant_district_code` (`district_code`);
