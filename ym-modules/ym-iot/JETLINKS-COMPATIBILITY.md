> 历史迁移记录：当前模块已于 2026-09-14 完成旧实现清理和分层整理；以下旧数量、版本、包摘要及运行链路不作为当前验收依据。最新结构、权限及验证见 [README.md](README.md)。

# ym-iot JetLinks 兼容迁移交付

## 交付边界与验证

仅修改 ym-iot，保留原 5 组 RemoteIot 接口的 25 个方法及 HTTP 入口。ownership 由 Kuhn 独立维护。共享 API、JetLinks、协议、前端与飞行业务由各自负责人维护；本交付不运行数据库迁移、不启动或替换容器、不向实体设备发命令。

本地兼容测试168 项全部通过（测试 XML 见交付目录）：25 项原 Dubbo 实现方法调用、92 项旧 HTTP 路由/签名契约，以及 51 项权限、真实 H2 事务与持久事件、记录、任务恢复、字段映射和时限测试。HTTP 契约测试不等于真实登录/网关/注册中心联调。测试覆盖原始路由快照，完整快照位于 src/test/resources/jetlinks/legacy-http-routes.txt。IoT 与农业模块联编通过。最新共享 API SeriesDto 嵌套值的反序列化也有本地回归。

主线已报告隔离真实 RPC 产品、设备、tag、property、events 验证 PASS，本模块没有重复操作现运行实例。原 78 属性中 **74 个有效属性关联 8 产品，4 个属于已不存在产品 2036365149023100930，备份隔离；另 1 个孤儿标签隔离**，不虚构归属。新原生 device 日志和标签数字 ID、原生 property ID 修复由主线提供者维护。expands.ymTechnicalExtension=true 的协议技术属性只留在 native 采集/存储/UI，不出现在旧业务属性列表；普通新建属性仍持久生成数字 ymPropertyId。兼容层不以 identifier 代替 Long ID。

主线已报告独立 MySQL 三份 SQL 连续执行两轮 PASS：ownership 29/1、4 bindings、12 OPEN、42 施肥记录、363 阀门日志保持；证据由主线保存于 work/migration/mysql-schema-test-002.log。本模块未连接原库或隔离库执行变更。当前 fatjar SHA256 为 24ba701ec2d86e76a36565b9f3ede65d9709a739be2b963d2cf0446e41d479ed，包内 API SHA256 为 ee5639d924dd1ce54a96dee92d6313728efc0aaac095a866cbeaaabe4f74e0e0。

## 活动模式行为

唯一迁移开关为 ym.iot.jetlinks.enabled，源码默认 false，不存在逐设备切换。启用时所有目录、设备、属性、标签、遥测、控制、HFZK、WVP 与告警入口使用 JetLinks RPC。活动模式启动检查拒绝旧 MQTT、Mica、施肥、阀门、HFZK、WVP、EMQX presence 任一旧整合开关仍为 true。示例见 jetlinks-cutover.yml.example，正式切换由部署人一次执行。

依赖为 com.ym.jetlinks:jetlinks-iot-rpc-api:1.0.0。7 个 @DubboReference 使用 group=jetlinks-iot、version=1.0.0、retries=0；默认读 5 秒，管理写 10 秒，命令提交/云台控制 5 秒，视频 start 60 秒，outer await 65 秒。逐方法注解配置回归覆盖共享接口全部方法，虚拟首播 35 秒测试证明不会被旧 25 秒等待边界截断。等待边界不改变 RPC 方法自身 deadline。

RecordDto.data 保持旧 BO/VO camelCase。Long ID 保持数字 String castString 语义，不对 MD5 建映射，不截断 ID。产品、设备、属性、规则 BO/VO 新增可选 version，提供者使用原生 modifyTime 校验 CAS。新档案没有协议/厂家配置时保持待配置状态，不能推断技术接入。新原生日志采用数字 ID；旧原生 MD5 日志隔离不查询；原有施肥记录、阀门会话、控制日志和任务的业务历史保留。

QueryDto.ids 是当前记录类型的 ID 范围，null=全部、[]=无；规则/告警/日志的设备授权放 filters.authorizedDeviceIds，绝不把设备权限塞进规则 ID。列表在过滤和分页前确定当前真实归属，命令、批量操作、GB 编号查询也检查归属。遥测 latest/log/alarm 带每设备 visibleFrom，跨转移不泄露前租户数据。业务 HTTP 历史记录按其持久 tenant 查询，不能改成当前设备租户。

GET /iot/product/category/list 返回 R<List<{id,code,name,parentId,sortIndex}>>，唯一来源 IotCatalogRpcService.categories()，无字典依赖。标准目录过滤使用旧 camelCase，例如 name/code 包含、category 精确及 status。provider 当前 onlineStatus 使用 ONLINE/OFFLINE，设备 status 0 正常/1 停用。OFFLINE 告警依据权威 OFFLINE/offlineSince 持续 offlineMinutes，MQTT 在线且无测点不推断离线；HFZK 数据时效由插件负责，兼容层不运行旧 lastReportTime 离线推断。

归属初始化 effective_from=1000-01-01 原值保持；仅 RPC 查询边界统一 Math.max(0, effectiveFrom.toEpochMilli())，包括 latest、series、records，30 台基线回归验证不传负数。getLatestMapFresh 仅传 bypassCache=true，不发读设备命令。records 使用提供者 from/to/limit 实现精确、最近和相邻查询；series 分桶由提供者处理。原 metricCode 与物模型 identifier 显式双向映射，保留数值、文本、布尔、对象和数组。新 HTTP 遥测在当前归属验证后调用 ingest，拒绝归属期之前数据。

## 控制、业务事实与归属

阀门按协议 schema 调用 controlValve、percentControl、readData 等，阀型来自明确的 core 配置。施肥 control 使用 RESET/START/STOP/EMERGENCY_STOP/PRIME_WATER/CLEAN_TANK/START_WITHOUT_RESET，参数下发使用已定义 writeRegister。不猜函数、不写旧 MQTT。ACCEPTED/SENT 不代表成功；COMMAND_CHANGED 的 SUCCEEDED 配合 targetReached/readComplete/writeVerified 才确认。命令提交前持久 requestId、operatorId、tenant、assignmentVersion；超时标 UNKNOWN，禁止自动重发。多步骤任务逐个等待已确认结果，优先处理急停。

活动模式自动启动独立 jetLinksBusinessScheduler，每次完成后间隔 1 秒领取最多 500 个事件，租约 120 秒；无须 SnailJob 注册，禁用 SnailJob 仍运行。原 jetLinksBusinessEventJob 入口保留供运维触发，同进程 AtomicBoolean 禁止重入，多实例由提供者租约和物理库收据协调。业务 consumer-id 必须稳定。事件去重收据与业务更新同一物理事务，提交后 ack；失败不 ack，重复投递不重复业务记录。隔离联调使用 JETLINKS_MIGRATION_TEST 注册组、独立注册中心命名空间/事件消费者、关闭 Spring discovery 和 SnailJob，避免正式路由污染；SMS 关闭以免测试通知。

fertilizer.record 使用稳定 sourceRecordId 映射一次生成的业务数字 ID；durationMinutes 原样写入旧 fertilizationSeconds 字段（旧字段实际为分钟，不能乘 60），类型取低 8 位、数量和 rawHex 按协议事实。阀门根据位置状态变化和确认事件维护会话，不能按设备+角度永久去重。原生 operatorId/caller 完整保存在持久事件及命令投影审计中；原 Long 操作人字段仅写已识别的 ym-iot 数字用户，原生无 RuoYi 映射时为 NULL，不 hash、截断或冒充用户。业务命令使用持久 assignmentVersion 定位归属；原生 -1 或非命令遥测按源时间查归属历史，不能套最近命令版本。RECOVER 同租户旧任务可继续关联，跨租户 transfer 的旧任务须由归属流程先解决。

产品告警规则持久 authorizedDeviceIds + assignmentVersions 快照，整个规则覆盖范围必须是当前授权集合的子集。转移时由 ownership 调用 JetLinksRuleOwnershipCleanup，先持久清理规则范围，再解冻新 fence；失败保持 SYNC_PENDING。空范围禁用并保留规则，不把跨租户整条规则返回。

UAV upsertUavDevices 的返回数量是处理技术档案数量，不是已分配或可见设备数量。未知 SN 创建 UAV_DOCK/OTHER 技术档案和 tenant=NULL 的业务镜像，忽略 candidate.tenantId；已知 SN 全批次先授权，不能跨租户覆盖。仅配置 statusSource=UAV 的已知设备调用 reportState(...,source=UAV)；若 device/product 的来源是 EMQX 或其他值则跳过状态提交，仍更新档案并计数，不能覆盖权威来源。未知新建技术机场默认 statusSource=UAV。机场源 DTO 只有批次 syncTime（实际调用者填接收同步时刻），没有每条独立状态时间；使用 syncTime，否则 receivedAt，并在配置标记 statusTimestampSource。DJI OSD/state/status 的遥测由 Sartre 独立只读 native MQTT 协议承接，机场配置 statusSource=EMQX；原飞行业务仓库和 MQTT 客户端保留，不由本模块移动或停止。完整机场链路仍需主线协议产物联调。

## 事件镜像、通知与恢复

DEVICE_STATUS_CHANGED 使用独立状态版本和源时间更新 online_status，并持久保留 offlineSince/source 原事实；后来的普通目录事件不覆盖状态。PRODUCT_CHANGED 与 CATEGORY_CHANGED 更新本地产品/分类镜像和关联设备分类；归档写墓碑，乱序旧创建不能复活。分类 API 仍只读 core，镜像不成为第二分类来源。

ALARM_CHANGED/CREATED 按固定 createTime 与发生时 assignmentVersion 校验历史归属，保留旧警报 DTO 字段。ruleSnapshot、deviceName、deviceCode 和收件人必须来自触发时不可变事实，缺快照且未明确 notifySms=0 则记录失败并拒绝 ack。每个 alertId+phone 只有一条通知意图，调用现有 IotAlertNotifyService.sendSnapshot，无当前租户回查。发送成功持久 SENT；明确失败 FAILED 每 30 秒重试、最多 10 次；网络结果丢失或 SENDING 超过 120 秒记 UNKNOWN，不自动重发。短信平台没有通用幂等键，进程在外部发送后崩溃不能证明未送达，须人工核对；不能声称外部短信精确一次。sms.enabled=false 时意图标 DISABLED，不在以后开关启用时补发旧通知。

事件失败表保存不可变原事件、attempts、last_error 和重放操作者。自动重试沿 provider 租约重投递，提交成功/ack失败只等待安全重投递；人工重放重新应用原事件，标 REPLAYED_ACK_PENDING，后续 provider 重投递完成 ack。以下入口同时要求平台超级管理员及事件权限，不向普通租户暴露跨租户失败内容：

- GET /iot/jetlinks/events/failures?limit=100（iot:event:query）。
- POST /iot/jetlinks/events/replay?eventId=...（iot:event:replay）。
- GET /iot/jetlinks/events/notifications/failures（iot:event:query）。
- POST /iot/jetlinks/events/notifications/retry?alertId=...&phone=...（iot:event:replay）；UNKNOWN 需显式 confirmUnknown=true，先核对短信平台。

## 构建与隔离部署准备

使用 Java 21（JetLinks 提供者单独使用 Java 17），从 RuoYi 根目录构建：

```sh
mvn -pl ym-modules/ym-iot -am -Pprod -Dprofiles.active=prod -Dmaven.test.skip=true package
```

产物 target/ym-iot.jar 为 Spring Boot 可执行 fatjar；保持源码默认关闭迁移，仅在隔离实例外部配置整体启用。不要直接复用正式实例的 Nacos namespace、数据库、Redis、Dubbo 注册组和任务配置。

固定镜像构建仅使用已批准、已拉取的 Java 21 基础镜像 digest。提供 JAVA_RUNTIME_IMAGE=registry/image@sha256:<64位摘要> 后运行本模块 build-jetlinks-image.sh。脚本拒绝浮动 tag、记录 jar SHA256、用 Dockerfile.jetlinks 验证 COPY 字节；默认输出标签 ym-iot:jetlinks-<jar摘要前12位>。构建参数 --pull=false --network=none，不启动、停止或替换任何容器。镜像构建上下文只含 fatjar 和 Dockerfile。当前尚未执行镜像构建。

数据库变更材料为 src/main/resources/db/jetlinks-business.sql 和 jetlinks-business-post-ownership.sql，归属表/迁移材料由 ownership 提供。须由部署人备份并审查后应用到隔离库；本模块不会自动执行。旧施肥唯一索引替换为普通索引前，分别查询 information_schema.statistics 判断存在性，两个步骤独立可重跑；已有实验 command_task 表需核对 priority 列，CREATE TABLE IF NOT EXISTS 不升级现存旧表。顺序必须为：备份原 30 台 tenant（29/1）及 4 bindings → 兼容表 DDL → ownership bootstrap COMMIT migration marker → 主线验证 30 台归属与 core fence 版本一致 → post-ownership DDL → 启动消费者。post SQL 检查 jetlinks-ownership-v1 marker、device_count=30、binding_count=4；marker 仅证明本地初始化，不证明远端 fence。后置 DDL 将 device/product tenant_id 改为 nullable 且保留旧行值，分类列扩至 VARCHAR(64)。MySQL DDL 有隐式提交，不得放在 ownership bootstrap 事务内部，不得用 mysql --force 忽略检查。

本地测试命令（根 POM 有默认跳过与标签过滤，须显式覆盖）：

```sh
mvn -pl ym-modules/ym-iot,ym-modules/ym-agriculture -am test \
  -Dmaven.test.skip=false -DskipTests=false -Dprofiles.active=jetlinks \
  '-Dtest=JetLinks*Test' -Dsurefire.failIfNoSpecifiedTests=false
```

若环境禁用 JVM 动态 attach，使用 Mockito 官方 jar 的 -javaagent 选项启动测试 JVM。联调验收还需真实 ym-iot 登录租户、权限/分页/转移边界、HTTP 序列化、Dubbo token/注册中心、事件任务调度、首播与提供者失败语义。真实控制验收由主线单独授权，本交付不执行。
