# YM 农业平台迁移验收矩阵

> 基线：`ym-cloud@test`、`ym-vue@main`、`sf-screen@test` 当前工作区。状态只描述仓库内可验证交付，不把生产设备、真实微信凭据或未完成的测试环境联调记为通过。

| 源功能 | 目标模块 | 主要目标表 | 兼容接口/契约 | 前端入口 | 自动化验证 | 当前状态 |
|---|---|---|---|---|---|---|
| 地块、大棚、布局、设备绑定 | `ym-agriculture` | `sf_field`、`sf_greenhouse_layout`、`sf_field_iot` | `/smart-farming/field/**`、`RemoteAgricultureService` | 农业应用 `/field` | Maven 编译、迁移行数/引用校验 | 已迁移 |
| 作物、品种、种植批次 | `ym-agriculture` | `sf_crop`、`sf_planting_batch` | `/smart-farming/crop/**`、`/planting-batch/**` | 农业应用 `/crop`、`/planting-batch` | Maven 编译、迁移行数校验 | 已迁移 |
| 农事字典、记录及移动端记录 | `ym-agriculture`、农业 BFF | `sf_farm_work_*` | `/smart-farming/farm-work/**`、`/mobile/smart-farming/farming-records/**`、`RemoteAgricultureMobileService` | 农业应用 `/farm-work` | Provider/BFF 编译、幂等入口静态检查 | 已迁移；移动端天气详情仍为兼容聚合 |
| 工单、任务包、派工、接单、执行、打卡、验收 | `ym-agriculture`、农事任务 BFF | `sf_stask_*`、`sf_work_order*` | `/smart-farming/stask/**`、`/miniapp/smart-farming/stask/**`、`RemoteFarmTaskService` | 农业应用 `/work-order` | 状态转换单测、Provider/BFF 编译、BFF 容器启动 | 已迁移；历史工作台路径返回 HTTP 200，带身份的全状态链仍未运行验收 |
| 巡检、SOP、验收表单 | `ym-agriculture` | `sf_inspection*`、`sf_sop*` | `/smart-farming/inspection/**`、`/sop/**` | 农业应用 `/inspection`、`/sop` | Maven 编译、迁移表校验 | 已迁移 |
| 员工、邀请码、班组、技能、工人、组长用工、工时、劳务 | `ym-agriculture`、`ym-auth` | `sys_employee`、`sf_invite_code`、`sf_worker*`、`sf_leader_labor*` | `/system/employee/**`、`/system/invite-code/**`、`RemoteEmployeeAdmissionService` | 农业应用 `/employee`、`/labor` | Auth/Provider 编译、身份映射冲突校验 | 业务域已迁移；平台身份写入和真实微信登录待凭据验证 |
| 库存 V1.5、仓库、余额、台账、入出库、领退料、盘点、修正、资产 | `ym-agriculture`、农事任务 BFF | `inventory_*` | `/smart-farming/inventory/**`、`/miniapp/smart-farming/stask/inventory/**`、`RemoteInventoryService` | 农业应用 `/inventory`、`/asset` | 数量汇总校验、幂等入口、Provider/BFF 编译 | 已迁移；废弃 `materialinventory` 明确排除 |
| 产量、溯源 | `ym-agriculture` | `sf_yield_record`、`sf_trace*` | `/smart-farming/yield/**`、`/trace/**` | 农业应用 `/yield`、`/trace` | 迁移行数校验 | 已迁移 |
| 天气、预警、新闻、行情、节气、媒体 | `ym-agriculture`、农业 BFF | `sf_weather*`、`sf_news*`、`sf_market*`、`sf_solar_term*` | `/smart-farming/weather/**`、`/mobile/smart-farming/weather/**` | 农业应用 `/weather`、`/market`、`/news` | Maven 编译、迁移行数校验 | 已迁移；真实第三方凭据未验证 |
| 卫星、无人机、AI 巡检、图片归档、算法回调、语音 | `ym-agriculture` | `sf_satellite*`、`sf_uav*`、`sf_ai*`、`sf_voice*` | `/smart-farming/satellite/**`、`/uav/**`、`/ai/**`、`/voice/**` | 农业应用外围能力路由 | Maven 编译、迁移表校验 | 已迁移；外部平台联调待安全凭据 |
| 农业首页、统计、大屏 | `ym-agriculture`、农业 BFF | 领域表聚合 | `RemoteAgricultureMobileService`、大屏查询接口 | 农业应用 `/bigscreen` | 大屏生产构建、静态资源复制校验 | 已迁移；保留 `sf-screen` 未提交版本副本 |
| 产品、物模型、设备、标签、日志、遥测 | `ym-iot` | `iot_product*`、`iot_device*`、`iot_data_point` | `/iot/**`、`RemoteIotDeviceService`、`RemoteIotTelemetryService` | IoT 应用 `/product`、`/device`、`/telemetry` | Maven 单测、迁移行数/孤儿引用校验、容器启动 | 已迁移；服务已连接 MySQL 并注册 Nacos |
| 告警规则、记录、离线检测、EMQX/MQTT | `ym-iot` | `iot_alert_*`、`iot_device_log` | `/iot/alert/**`、`RemoteIotAlertService` | IoT 应用 `/alert` | 告警单测、MQTT 处理编译、调度启动日志 | 已迁移；Snail Job 成为离线检测主入口，旧 Redis 调度已默认关闭；无测试 Broker，未做 MQTT 模拟设备验收 |
| 施肥机、电动阀、控制会话与回执 | `ym-iot` | `iot_fertilizer_*`、`iot_motorvalve_*` | `/fertilizer/**`、`/motorvalve/**`、`RemoteIotControlService` | IoT 应用控制路由 | Maven 编译、控制幂等契约检查 | 已迁移；真实设备不在本次通过边界 |
| HFZK、WVP/ISUP | `ym-iot` | `iot_hfzk_*`、`iot_isup_*` | `/iot/hfzk/**`、WVP/ISUP 适配接口 | IoT 应用 `/hfzk`、`/wvp` | Maven 编译、DDL 建表、两轮迁移行数校验 | 代码和 `iot_isup_*` 四表 DDL 已迁移；真实视频设备未验证 |
| 农业/IoT 定时与补偿任务 | 对应领域服务 + Snail Job | `snail_job.sj_*` | Snail Job executor | 无 | Server 2.0.2/Java 21 启动冒烟、两领域客户端/gRPC 端口启动 | Server 与农业/IoT 执行器已启动；实际失败重试和双节点故障转移未验证 |
| 微信与员工登录兼容 | `ym-auth`、`ym-system`、农业契约 | 平台社交关系表 + 农业员工表 | `/auth/mp-weixin/**`、`/auth/employee-wx/**` | 两个微应用独立登录 | Secret 缺失失败关闭、Auth 编译 | 适配已落地；真实凭据和平台身份迁移未执行 |
| 农业运营微应用 | `ym-agriculture-app` | 无 | 领域 HTTP API | `/micro-apps/agriculture/` | `pnpm typecheck`、生产构建 | 已创建并构建；当前为领域通用页面，完整源 CRUD 视觉/交互逐页验收未完成 |
| IoT 运营微应用 | `ym-iot-app` | 无 | IoT HTTP API | `/micro-apps/iot/` | `pnpm typecheck`、生产构建 | 已创建并构建；完整控制页真实回执验收未完成 |
| 菜单、应用、套餐、角色映射 | Dbo SQL | `sys_app`、菜单/套餐/角色关系 | 平台管理接口 | 主应用菜单 | SQL 重复执行、权限交集校验 | 测试 `ry-cloud` 已新增 2 应用和 25 菜单，重复执行数量不增长，角色越界为 0；现有套餐无同权限匹配，因不自动扩权而保持 0 关联 |
| 历史数据迁移 | `ym_migration` + 两个领域库 | 映射表及全部领域表 | 编号脚本 `00`–`50` | 无 | 两轮全新库演练、120 表源/目标行数对比、数量/状态/孤儿校验 | 两轮均为零行数差异并完成演练库销毁；源数据仍有 1 条负库存、486 条历史遥测孤儿，目标未新增问题 |

## 明确未计为通过的环境项

- Nacos 3 的四个服务 DataId 和 Gateway 路由尚未发布；管理 API 需要获批的管理员访问令牌。
- Docker Desktop 当前可用内存约 7.4 GiB，平台基础容器已占用约 5.9 GiB；四个新服务已逐个完成启动和 Nacos 注册，但无法在现有容量下与全部平台服务同时常驻，故 Gateway + 四服务同时全链路未通过。
- 农业服务的卫星影像上游 OSS 适配已改为默认关闭的显式开关；开启时必须提供真实存储平台配置，本次未提供凭据，OSS 对象可用性未计为通过。
- `ry-cloud` 平台身份、菜单和社交关系未自动写入；演练默认只读平台库，避免扩大权限或污染共享测试数据。
- 未提供真实微信、天气、卫星、无人机、AI、HFZK/WVP 及设备控制凭据，相关代码存在不等于外部联调通过。
