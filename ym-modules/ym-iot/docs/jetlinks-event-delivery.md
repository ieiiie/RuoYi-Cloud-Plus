# JetLinks 业务事件与持久任务

## 正常流程

1. JetLinks 事件写入 `ym_rpc_event`，实际事务提交后唤醒推送器。
2. 推送器领取当前消费者的一条租约，通过 `IotChangeInboxRpcService.receive` 推送。
3. IoT 在独立声明式事务中写入唯一收据及业务投影。重复事件不会重复记历史。
4. 施肥命令回执在投影提交后推进下一步；持久任务已经等待回执、结束或处于 UNKNOWN 时，不重复提交。
5. IoT 返回成功后 JetLinks 才确认事件。失败返回 false，底层数据库异常不跨 Dubbo 传输；失败明细仍留在业务事件失败表。

施肥任务入队同样通过提交后回调执行第一步。事务回滚时不下发指令；任务数据保存在数据库，进程启动时分批恢复。启动恢复对在途命令核对一次状态，正常运行不轮询命令。

## 失败与恢复

- 未确认投递保留在数据库，按租约到期重试；失联或数据库异常时使用有上限的延时重试。
- 有新的事件时可提前唤醒，不因上一条失败事件的租约等待而阻塞新事件。
- 无积压、无故障时不创建定时检查；不存在每秒拉取或固定 60 秒检查。
- 单次投递只领取一条，避免批量串行调用超过后续事件的租约。
- 投影已提交但任务推进/确认失败时，重复回执继续唤醒原持久任务，不重复投影。
- 任务提交结果未知则保存 UNKNOWN，不自动生成新请求重发。每一步保留固定的 `requestId:stepIndex`。
- 服务启动进行一次推送握手和存量任务恢复；失败时只重试未完成的握手/恢复。进程停止时尚未执行的任务保留在数据库，下一次启动恢复。
- 不清空原事件、收据、失败记录或任务表，不重建业务 ID。

## 契约与配置

- 平台唤醒接口：`IotChangeDispatchRpcService`，group=`jetlinks-iot`，version=`1.0.0`。
- 业务收件箱：`IotChangeInboxRpcService`，group=`jetlinks-iot-business`，version=`1.0.0`。
- 两个新服务启用内部 Dubbo token，不新增浏览器接口。
- IoT 保留原 `ym.iot.jetlinks.consumer-id`，默认 `ym-iot-business-v1`。
- JetLinks 的 `jetlinks.iot-rpc.business-consumer-id` 必须与之相同，默认值一致。不要换消费者 ID，否则会从该消费者未确认的历史重新投递。
- 原 `IotChangeRpcService.pending/acknowledge` 保留，供迁移兼容及手动恢复使用，IoT 正常运行不定时调用。
- 农业的只读 `RemoteFieldDeviceBindingService` 是归属检查的新依赖。IoT 不直接访问农业库；不可用时拒绝回收/转移，不默认为零绑定。

## 发布顺序

1. 构建并提供更新后的 `jetlinks-iot-rpc-api`、`ym-api-agriculture`。
2. 发布农业服务，提供只读地块绑定检查。
3. 发布 JetLinks Provider，提供推送及启动唤醒服务。旧版 IoT 尚未提供收件箱时事件保持可重试，已有租约接口仍可用。
4. 发布 IoT，提供收件箱并发起启动握手。确认两端消费者 ID 一致。
5. 停用运维侧已登记的旧 `jetLinksBusinessEventJob`；本模块已移除 SnailJob 客户端直接依赖及执行器注册。

不需要新增数据库表，也不需要修改既有归属记录或业务历史。本次源码测试不等同于运行环境已发布；发布后再验证真实注册中心及服务发现。

## 回归入口

- IoT Maven 默认回归覆盖注解 Mapper、框架默认数据源和多数据源路由、事务回滚、回执幂等、任务推进和旧 HTTP/Dubbo 契约。
- `OwnershipMysqlIntegrationTest` 仅当提供 `iot.test.mysql.env` 时连接固定隔离端口 13306/ownership_test，验证 MySQL FOR SHARE 及并发归属变更。
- Provider `BusinessEventPushQualityTest` 使用虚拟时钟验证空闲不轮询、提交确认、失败重试与新事件提前唤醒。
- `BusinessEventPushPostgresIT` 与 `RpcJournalPostgresIT` 只连接固定隔离端口 15432/rpc_test，验证真实提交、回滚、租约和启动积压。
