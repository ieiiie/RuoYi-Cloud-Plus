# YM 农业平台测试环境验证记录（2026-09-02）

## 已验证

- `ym-agriculture`：连接独立 MySQL 库，Jetty 9221 启动，Nacos 注册成功，Snail Job 2.0.2 客户端启动，gRPC 17891 监听。卫星影像上游 OSS 在无凭据环境下通过领域开关默认关闭。
- `ym-iot`：连接独立 MySQL 库，Jetty 9222 启动，Nacos 注册成功，Snail Job 2.0.2 客户端启动，gRPC 17892 监听；旧 Redis 离线规则调度默认关闭。
- `ym-agriculture-miniapp`：无业务数据源启动，Jetty 9223 启动，Nacos 注册成功，`/mobile/smart-farming/home` 有 HTTP 200 响应。
- `ym-farm-task-miniapp`：无业务数据源启动，Jetty 9224 启动，Nacos 注册成功，`/miniapp/smart-farming/stask/leader/workbench` 有 HTTP 200 响应。
- 两轮迁移演练：`ym-migration-r1-20260902T131243Z`、`ym-migration-r2-20260902T131425Z`均完成；每轮比较 120 张领域表，源/目标行数差异为 0，演练库已自动销毁。
- 源库质量问题在目标等量保留：1 条负库存、486 条遥测设备孤儿引用，未因迁移新增。
- 两个微应用从 `/micro-apps/agriculture/`、`/micro-apps/iot/` 生产构建产物启动，浏览器正常渲染独立登录页，应用标题分别为“YM 农业运营”和“YM 物联网运营”。
- 测试 `ry-cloud` 已执行应用/菜单脚本：新增 `agriculture`、`iot` 2 个 `sys_app` 和 25 条业务菜单；重复执行后数量不增长，角色套餐外权限为 0。受影响四表的执行前备份位于 `ym-mysql:/tmp/ry-cloud-before-agriculture-iot-apps-20260902.sql`。
- Maven 使用 `-Dmaven.test.skip=false` 完成 33/33 Reactor 模块，实际执行的 6 个迁移相关单测全部通过；边界脚本确认无 `TableDataInfo`、废弃 `materialinventory`、`@Scheduled`、Redis 延迟队列、跨域 Entity/Mapper/Service 依赖或跨库 SQL。

## 未计为通过

- Nacos 3 中四个服务 DataId 与 Gateway 路由尚未发布；当前没有获批的管理访问令牌。已提供 `script/config/nacos/publish-ym-services.sh`，强制使用令牌调用 Nacos 3 Admin API，发布前备份并在发布后回读比对。
- Docker Desktop 可用内存约 7.4 GiB，无法让现有平台容器与四个新服务同时常驻；本记录是逐个容器验证，不是 Gateway 并发全链路证明。
- 微应用本地预览服务未配置 Gateway 反向代理，登录页的租户/认证初始化请求返回 404；因此只证明构建产物和独立登录页可渲染，不证明实际登录或无界嵌入通过。
- 现有套餐没有与新菜单完全相同的历史权限码，交集脚本因此没有自动开通任何农业/IoT 套餐关系；需要明确目标套餐后才能开通，不得用全量授权替代。
- 没有 MQTT 测试 Broker，IoT 启动冒烟使用 `MQTT_CLIENT_ENABLED=false`；模拟设备、告警闭环和控制回执未运行验收。
- 没有安全可用的微信、天气、卫星、无人机、AI、HFZK/WVP 与 OSS 凭据；这些外部联调未执行。
- Snail Job Server 和两个执行器启动已验证，但真实失败重试和双节点故障转移尚未执行。
