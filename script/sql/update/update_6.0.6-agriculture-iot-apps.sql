-- 农业、IoT 微应用目录与最小权限边界（MySQL / Dbo）
-- 前置：update_6.0.5-saas-operations.sql 已执行；本脚本不自动扩大现有角色权限。
SET NAMES utf8mb4;

SET @agriculture_app_id = 1762100000000000101;
SET @iot_app_id = 1762100000000000102;
SET @agriculture_root_id = 1761400000000020000;
SET @iot_root_id = 1761400000000030000;

INSERT INTO sys_app
    (app_id,app_key,app_name,app_type,entry,initial_path,alive,sync,icon,order_num,status,del_flag,create_time,remark)
VALUES
    (@agriculture_app_id,'agriculture','农业运营','MICRO','/micro-apps/agriculture/','/',1,1,
     'ant-design:environment-outlined',10,'0','0',NOW(),'农业、农事任务、库存及大屏'),
    (@iot_app_id,'iot','物联网运营','MICRO','/micro-apps/iot/','/',1,1,
     'ant-design:api-outlined',20,'0','0',NOW(),'产品、设备、遥测、告警与控制')
ON DUPLICATE KEY UPDATE app_name=VALUES(app_name),entry=VALUES(entry),initial_path=VALUES(initial_path),
    alive=VALUES(alive),sync=VALUES(sync),status='0',del_flag='0',remark=VALUES(remark);

-- 目录和页面。component 均相对各微应用 src/views；废弃 material-inventory 不录入。
INSERT INTO sys_menu
    (menu_id,app_id,menu_name,parent_id,order_num,path,component,query_param,is_frame,is_cache,
     menu_type,visible,status,perms,icon,active_menu,ext,create_time,remark)
VALUES
    (@agriculture_root_id,@agriculture_app_id,'农业运营',0,1,'agriculture',NULL,'','N','Y','M','0','0','','environment','','',NOW(),'农业微应用根目录'),
    (1761400000000020010,@agriculture_app_id,'地块与大棚',@agriculture_root_id,10,'field','smartfarming/field/index','','N','Y','C','0','0','smartfarming:field:list','environment','','',NOW(),''),
    (1761400000000020020,@agriculture_app_id,'作物与批次',@agriculture_root_id,20,'batch','smartfarming/batch/index','','N','Y','C','0','0','smartfarming:batch:list','seedling','','',NOW(),''),
    (1761400000000020030,@agriculture_app_id,'农事记录',@agriculture_root_id,30,'farming','smartfarming/farming/farm-records/index','','N','Y','C','0','0','smartfarming:farming:list','form','','',NOW(),''),
    (1761400000000020040,@agriculture_app_id,'工单任务',@agriculture_root_id,40,'stask','smartfarming/stask/task-list/index','','N','Y','C','0','0','smartfarming:stask:list','audit','','',NOW(),''),
    (1761400000000020050,@agriculture_app_id,'农事派工',@agriculture_root_id,50,'assignment','smartfarming/stask/farm-assign/index','','N','Y','C','0','0','smartfarming:assignment:list','team','','',NOW(),''),
    (1761400000000020060,@agriculture_app_id,'员工与用工',@agriculture_root_id,60,'employee','smartfarming/stask/badge/index','','N','Y','C','0','0','system:employee:list','peoples','','',NOW(),''),
    (1761400000000020070,@agriculture_app_id,'SOP',@agriculture_root_id,70,'sop','smartfarming/stask/sop/index','','N','Y','C','0','0','smartfarming:sop:list','guide','','',NOW(),''),
    (1761400000000020080,@agriculture_app_id,'库存物料',@agriculture_root_id,80,'inventory','smartfarming/inventory/balance/index','','N','Y','C','0','0','smartfarming:inventory:list','warehouse','','',NOW(),''),
    (1761400000000020090,@agriculture_app_id,'入库与出库',@agriculture_root_id,90,'inventory-orders','smartfarming/inventory/inbound/index','','N','Y','C','0','0','smartfarming:inventory:order:list','swap','','',NOW(),''),
    (1761400000000020100,@agriculture_app_id,'盘点与修正',@agriculture_root_id,100,'stocktake','smartfarming/inventory/stocktake/index','','N','Y','C','0','0','smartfarming:inventory:stocktake:list','check-square','','',NOW(),''),
    (1761400000000020110,@agriculture_app_id,'天气与预警',@agriculture_root_id,110,'weather','smartfarming/weather/index','','N','Y','C','0','0','smartfarming:weather:list','cloud','','',NOW(),''),
    (1761400000000020120,@agriculture_app_id,'卫星遥感',@agriculture_root_id,120,'satellite','smartfarming/satellite/index','','N','Y','C','0','0','smartfarming:satellite:list','global','','',NOW(),''),
    (1761400000000020130,@agriculture_app_id,'无人机巡检',@agriculture_root_id,130,'uav','smartfarming/uav/index','','N','Y','C','0','0','smartfarming:uav:list','rocket','','',NOW(),''),
    (1761400000000020140,@agriculture_app_id,'农业大屏',@agriculture_root_id,140,'bigscreen','bigscreen/index','','N','Y','C','0','0','smartfarming:screen:view','dashboard','','',NOW(),''),
    (@iot_root_id,@iot_app_id,'物联网运营',0,1,'iot',NULL,'','N','Y','M','0','0','','api','','',NOW(),'IoT 微应用根目录'),
    (1761400000000030010,@iot_app_id,'产品与物模型',@iot_root_id,10,'product','iot/product/index','','N','Y','C','0','0','iot:product:list','product','','',NOW(),''),
    (1761400000000030020,@iot_app_id,'设备管理',@iot_root_id,20,'device','iot/device/index','','N','Y','C','0','0','iot:device:list','device','','',NOW(),''),
    (1761400000000030030,@iot_app_id,'设备日志',@iot_root_id,30,'device-log','iot/device/log/index','','N','Y','C','0','0','iot:deviceLog:list','log','','',NOW(),''),
    (1761400000000030040,@iot_app_id,'告警规则',@iot_root_id,40,'alert-rule','iot/alert/rule/index','','N','Y','C','0','0','iot:alertRule:list','bell','','',NOW(),''),
    (1761400000000030050,@iot_app_id,'告警记录',@iot_root_id,50,'alert-record','iot/alert/record/index','','N','Y','C','0','0','iot:alertRecord:list','warning','','',NOW(),''),
    (1761400000000030060,@iot_app_id,'施肥机',@iot_root_id,60,'fertilizer','iot/fertilizer/index','','N','Y','C','0','0','iot:fertilizer:list','control','','',NOW(),''),
    (1761400000000030070,@iot_app_id,'电动阀',@iot_root_id,70,'motorvalve','iot/motorvalve/index','','N','Y','C','0','0','iot:motorvalve:list','switcher','','',NOW(),''),
    (1761400000000030080,@iot_app_id,'HFZK 设备',@iot_root_id,80,'hfzk','iot/hfzk/index','','N','Y','C','0','0','iot:hfzk:list','cloud-sync','','',NOW(),''),
    (1761400000000030090,@iot_app_id,'视频设备',@iot_root_id,90,'wvp','iot/wvp/index','','N','Y','C','0','0','iot:wvp:list','video-camera','','',NOW(),'')
ON DUPLICATE KEY UPDATE app_id=VALUES(app_id),menu_name=VALUES(menu_name),parent_id=VALUES(parent_id),
    order_num=VALUES(order_num),path=VALUES(path),component=VALUES(component),perms=VALUES(perms),status='0';

-- 套餐先开应用，再开菜单。这里只对已明确含有对应历史业务权限的套餐求交集，不做全量授权。
INSERT IGNORE INTO sys_tenant_package_app(package_id,app_id)
SELECT DISTINCT pm.package_id,@agriculture_app_id
FROM sys_tenant_package_menu pm JOIN sys_menu old_menu ON old_menu.menu_id=pm.menu_id
WHERE old_menu.perms LIKE 'smartfarming:%' OR old_menu.perms LIKE 'system:employee:%';

INSERT IGNORE INTO sys_tenant_package_app(package_id,app_id)
SELECT DISTINCT pm.package_id,@iot_app_id
FROM sys_tenant_package_menu pm JOIN sys_menu old_menu ON old_menu.menu_id=pm.menu_id
WHERE old_menu.perms LIKE 'iot:%';

INSERT IGNORE INTO sys_tenant_package_menu(package_id,menu_id)
SELECT DISTINCT pa.package_id,m.menu_id FROM sys_tenant_package_app pa
JOIN sys_menu m ON m.app_id=pa.app_id
WHERE pa.app_id IN (@agriculture_app_id,@iot_app_id)
  AND (m.perms='' OR EXISTS (
      SELECT 1 FROM sys_tenant_package_menu old_pm
      JOIN sys_menu old_m ON old_m.menu_id=old_pm.menu_id
      WHERE old_pm.package_id=pa.package_id AND old_m.perms=m.perms));

-- 校验：应返回 0。新角色权限必须同时位于套餐菜单内，现有角色不在本脚本中自动扩权。
SELECT COUNT(*) AS role_permission_outside_package
FROM sys_role_menu rm
JOIN sys_role r ON r.role_id=rm.role_id
JOIN sys_tenant t ON t.tenant_id=r.tenant_id
LEFT JOIN sys_tenant_package_menu pm ON pm.package_id=t.package_id AND pm.menu_id=rm.menu_id
WHERE pm.menu_id IS NULL;

SELECT app_key,entry,initial_path,status FROM sys_app WHERE app_key IN ('agriculture','iot');
SELECT app_id,COUNT(*) AS menu_count FROM sys_menu
WHERE app_id IN (@agriculture_app_id,@iot_app_id) GROUP BY app_id;
