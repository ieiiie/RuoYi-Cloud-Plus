# 设备归属与业务访问边界

DBO 是设备分配的唯一写入方。`ym-iot` 保留归属读取、租户访问校验和业务占用检查，不写入设备当前归属、归属历史或清理任务表。

## 代码位置

- `ownership/controller/DeviceOwnershipController`：当前归属查询接口。
- `ownership/service/IDeviceAccessService` 与 `service/impl/DeviceAccessServiceImpl`：租户、归属版本及操作权限检查。
- `ownership/support/OwnershipActor`：取得经过验证的当前租户身份。
- `ownership/service/support/DeviceOwnershipRepository`：归属读取、锁和业务占用检查的业务门面。
- `ownership/mapper/DeviceOwnershipMapper` 中的 SQL 注解：使用 MyBatis-Plus 访问物理业务库。
- `ownership/service/IOwnershipBlockerService`：以持久化业务事实检查转移阻断条件。
- `ownership/domain/vo`、`domain/dto`：保存查询视图及归属快照；分配、回收请求对象随管理入口归 DBO 维护。
- `ownership/dubbo/IotBusinessGuardRpcServiceImpl`：向平台提供归档和归属变更检查。

## 接口与事务

保留 GET `/iot/device/ownership/{deviceId}/current`，同时校验设备功能权限和实际租户归属。管理端分配、回收与清理入口已迁至 DBO。

读取及发起业务操作时使用 `DeviceOwnershipMapper` 与框架声明式事务，通过注解中的 MySQL `SELECT FOR SHARE` 与 DBO 的 `SELECT FOR UPDATE` 串行化归属变更。业务事务回调可以写入本业务记录，但不能写归属表。

默认库由框架数据源配置指向 `ym-iot`。Mapper、SqlSessionFactory 和事务管理器均沿用框架自动配置，不再额外创建数据源、固定物理库或指定业务事务管理器名称。模块 `application.yml` 保留结果映射和 `STATEMENT` 一级缓存选项，确保锁与归属版本查询实际访问数据库。

Mapper 忽略隐式租户和部门拦截条件，按服务层已校验的租户、设备列表及版本查询。空设备范围不会放大为全部设备；历史查询依据记录保存的租户，通知查询依据完整历史归属区间。地块绑定由农业服务的 `RemoteFieldDeviceBindingService.countActiveBindings(deviceSn)` 查询；不再跨库 JOIN，也不再使用农业库名配置。RPC 失败、编号缺失或无效数量均阻止归属变更，不能按无占用放行。业务 SQL 参数由 MyBatis 绑定。

`IotBusinessGuardRpcService` 的 group 为 `jetlinks-iot-business`，version 为 `2.0.0`。归档和归属变更都检查持久化业务依赖、排队或运行中的施肥任务。归档额外拒绝归属冻结；归属变更检查需允许自身冻结期间的校验。设备或表缺失、数据不完整、异常均拒绝操作。

运行身份应对归属表保持只读权限。数据库授权及运行发布单独验证，参考 DBO 归属说明及仓库 `script/sql/dbo-iot-ownership-*.sql`。旧初始化脚本仅用于隔离验证，不应重新导入运行环境。

业务事务使用 `@Transactional(propagation=REQUIRES_NEW, isolation=READ_COMMITTED, timeout=30, rollbackFor=Exception.class)`；事务方法通过 Spring 代理调用。事件事务和施肥单步事务拆成独立服务，避免类内调用使注解失效。多数据源组件负责选库，不能替代提交、回滚和锁释放。

农业占用接口使用 group=`iot-ownership-guard`、version=`1.0.0`，仅暴露有效绑定数量，服务启用 Dubbo token。它检查全部租户的遗留绑定以防绕过回收限制，不开放 HTTP 入口或地块明细。
