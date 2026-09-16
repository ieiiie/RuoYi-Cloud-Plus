# ym-iot：迁移后的业务服务

JetLinks 承接设备技术接入、协议处理、采集、视频与原生告警。ym-iot 保留农业业务接口、租户归属校验、Dubbo 适配、业务历史以及可恢复的命令任务和事件投影。

## 目录与包结构

源码根包为 `com.ym.iot`，先按业务划分，再按职责分层：

```text
com/ym/iot/
├── YmIotApplication.java
├── device/          设备目录、标签、遥测查询与 HTTP 入站适配
├── product/         产品与物模型属性的业务查询
├── alarm/           原生告警业务接口、通知 RPC 接收及范围校验
├── ownership/       设备归属查询、访问校验和业务占用检查
├── motorvalve/      阀门业务查询、控制入口、会话与历史记录
├── fertilizer/      施肥记录、状态展示和纯数据转换
├── hfzk/            保留现有业务 HTTP 契约的设备查询接口
├── wvp/             保留现有业务 HTTP 契约的视频查询与控制接口
├── jetlinks/        JetLinks RPC 客户端、业务适配和事件任务
└── dubbo/           面向农业等服务的 RemoteIot* 实现
```

各业务目录按实际需要包含以下包，不创建空占位目录：

| 包 | 职责 |
| --- | --- |
| `controller` | HTTP 接口、参数校验、功能权限及响应 |
| `domain` | 业务实体，替代原 `model/entity` |
| `domain/bo` | 请求与查询参数，替代原 `model/bo` |
| `domain/vo` | 页面返回与导出对象，替代原 `model/vo` |
| `domain/dto` | 内部传递对象和外部数据结构适配 |
| `service` | 业务接口，统一 `I*Service` 命名 |
| `service/impl` | 业务实现；控制器与远程入口依赖接口 |
| `mapper` | MyBatis-Plus Mapper 接口，复杂查询和锁语句使用 @Select/@Insert/@Update 注解 |
| `support`、`enums` | 无状态转换、访问上下文与枚举 |
| `config`、`dubbo` | 配置与远程服务实现 |

`device/telemetry` 只保留入站格式适配器及注册表，其服务接口归入 `device/service`，入站上下文和处理结果归入 `device/domain/dto`。施肥机的状态枚举、展示快照、解码和转换工具分别归入 `enums`、`domain/vo`、`support`；已无独立控制链的 `fertilizer/control` 目录移除。

`hfzk`、`wvp` 仍有在用的业务入口与兼容响应对象，继续保留；它们通过 JetLinks 访问设备，不代表恢复厂家直连。跨服务公开契约仍在 `ym-api-iot` 与 `jetlinks-iot-rpc-api`，不随本模块包整理改变。

设备归属与事务边界见 [设备归属说明](docs/device-ownership.md)。

### JetLinks 适配层

`src/main/java/com/ym/iot/jetlinks` 已按以下职责整理：

| 目录 | 职责 |
| --- | --- |
| `controller` | HTTP 参数、功能权限和标准响应；继承 `BaseController`、使用 `@Validated`，通过构造器注入业务接口 |
| `service` | `IJetLinks*` 业务契约，供控制器和本模块 Dubbo 适配调用 |
| `service/impl` | RPC 结果适配、业务编排、任务推进和事件投影 |
| `client` | `JetLinksRpcClient`；集中保留 Dubbo 服务版本、超时、重试和调用上下文 |
| `mapper` | 事件、任务、目录与业务投影的 注解 Mapper 接口；不再使用 Mapper XML |
| `service/support` | 命令任务事务门面与业务历史 ORM 查询 |
| `config` | 迁移模式检查 |
| `dubbo` | JetLinks 持久业务事件推送入口 |
| `support` | 数据映射、租户访问校验、图表转换和阀型元数据等复用逻辑 |

控制器与远程适配依赖 `IJetLinks*` 接口；迁移入口不再通过可选注入回退到旧厂家客户端。明确的权限、业务完整性等错误使用框架 `ServiceException`。

业务数据库访问统一使用框架 MyBatis-Plus。历史查询由施肥记录、阀门日志和会话各自的实体 Mapper 接收强类型 BO，通过 `QueryWrapper` 绑定条件；归属锁、任务状态更新、事件幂等收据和批次投影使用带命名参数的注解 Mapper，业务类不再拼接 SQL 或注入 `JdbcTemplate`。

IoT 使用框架配置的默认数据库 `ym-iot`，Mapper 与 `@Transactional` 共用框架自动配置的数据源和事务管理器，不另行指定库名或创建业务专用事务管理器。保留 `REQUIRES_NEW`、`READ_COMMITTED` 和 30 秒事务超时；事件收据与业务更新一起提交后才向 JetLinks 确认。结果映射及 `STATEMENT` 一级缓存选项放在模块 `application.yml`，动态数据源的正常路由能力保持不变。

这些 Mapper 的归属范围由服务层鉴权后显式传入，忽略隐式租户、部门条件，避免将全局设备目录或历史租户记录错误过滤。历史查询必须提供实际租户；转移设备后，旧业务记录仍属于发生时的租户。`ProjectionRow` 只允许六张已声明实体表及其持久化字段，动态 SQL 的值全部绑定；地块占用通过农业服务的 `RemoteFieldDeviceBindingService` 查询，IoT 不依赖农业库名。事务由框架自动配置管理，业务模块不再声明事务管理器。

## 清理范围

2026-09-14 清理了 176 个已退出运行链路的源码、资源和配套旧测试文件（包重组的移动文件另外记录）。当次主代码由 339 个 Java 文件整理为 185 个。

随后完成目录整理：移动 79 个主类，删除 3 个已迁至 DBO 且无引用的归属管理对象，补齐 4 个业务服务接口，当时主代码为 186 个 Java 文件；同步移动 3 个告警测试和设备归属说明文档，清除源码与测试树中的 145 个空目录，包括旧 `alert`、`mqtt`、`log` 及 `com/ym/mqtt` 目录。包移动不删除仍被调用的业务类。

- 删除旧 MQTT 公共实现、施肥机和电动阀编解码、直接发指令与本地任务执行器。
- 删除 HFZK 厂家 HTTP 登录、令牌维护和轮询采集，以及 WVP 直连认证、拉流和目录同步实现。
- 删除旧 EMQX 在线状态轮询、旧遥测直写存储和已无调用的缓存、设备名称与租户解析链。
- 删除对应自动装配入口和直接依赖；仍由共享组件传递引入的基础依赖保留。
- 原施肥罐参数提取为 `FertilizerTankParamBo`；状态转换提取为无 I/O 的 `FertilizerStateProjector`。

以下内容仍被当前业务使用，继续保留：HTTP 遥测入站适配及 RPC 转发；原接口所需的 BO/VO；施肥记录、阀门会话和控制日志；设备归属、授权版本及转移校验；持久化命令任务、事件收据、失败重放与业务镜像。

本次不删除业务数据库表或历史数据。移除旧缓存不会改变业务历史的租户归属；历史查询仍按记录发生时保存的租户查询。

## 权限规范

权限严格采用 `[A-Za-z0-9]+:[A-Za-z0-9]+:[A-Za-z0-9]+`：三段分别表示模块、资源和操作，段内只有字母或数字。

| 原权限形式 | 新权限形式 |
| --- | --- |
| `iot:alarm:config:edit` | `iot:alarmConfig:edit` |
| `iot:alarm:record:handle` | `iot:alarmRecord:handle` |
| 历史别名 `iot:alert:rule:*` | `iot:alarmConfig:*`（这里的 `*` 仅表示对应操作，不写入数据库） |
| 历史别名 `iot:device:log:*` | `iot:deviceLog:*`（同上） |

后端权限注解、农业 IoT 子应用告警按钮和权限初始化 SQL 已同步。迁移文件为根目录 `script/sql/jetlinks-permissions-three-segment.sql`，仅更新精确匹配的 `sys_menu.perms`，保留菜单 ID、名称、路由和角色菜单关联；重复执行不会重复创建菜单。

发布时应一起更新前端、后端和菜单权限，随后重新登录或按现有平台方式刷新登录权限缓存。执行数据库变更前保存实际环境的菜单权限原值；回退须按菜单 ID 恢复原值，不能简单反向替换多个历史别名。

源码 Nacos 样例 `script/config/nacos/ym-iot.yml` 已清除旧厂家接入配置，启用 JetLinks 模式，并保留公共数据源、注册中心、任务配置和历史阀位哨兵值。此次未修改运行中的 Nacos，也未重启服务。

## 验证

使用 Java 21，从仓库根目录运行：

```bash
mvn -pl ym-modules/ym-iot -am clean package \
  -Dmaven.test.skip=false -DskipTests=false
```

模块 Surefire 明确使用 `jetlinks,ownership` 标签，防止被根工程的运行环境标签过滤为零项测试；`failIfNoTests` 防止空测试误报成功。

上一轮功能清理的验证结果（目录整理后的验证见下节）：

- ym-iot：216 项测试，215 通过、1 跳过、0 失败、0 错误；跳过项需要额外提供的历史遥测回放样本。
- 真实 `SaInterceptor`：80 个 HTTP 接口，840 项权限断言通过；测试不调用控制器业务方法。
- IoT 前端：39 项告警 API 和处理表单测试通过，`vue-tsc --noEmit --skipLibCheck` 通过。
- `target/ym-iot.jar` 干净构建通过，并核对被删除旧类、原平铺包类和旧直接接入依赖未残留。

测试使用隔离数据及模拟 RPC；此次未替换运行服务，没有执行设备指令、发送通知或修改生产规则。实际 HTTP 登录、注册中心和真实 JetLinks 联调属于发布后的验证，不以本轮本地测试替代。

## 本次交付资料

删除清单、包移动映射、权限映射、完整源码备份、权限原值快照及按 ID 回退 SQL 统一保存于本次任务的 `work/iot-framework-cleanup-20260914` 目录。完整备份包含本轮开始前的工作区内容；恢复时应按清单逐文件处理，避免覆盖后续独立修改。

`JETLINKS-COMPATIBILITY.md` 保留为此前迁移的历史记录，其中旧测试数量、旧包摘要、旧接口实现和配置版本不代表本轮交付结果；当前结构及验证以本文和本次交付报告为准。

## 目录整理交付

目录整理的逐文件移动清单、清除的空目录清单和操作前源码备份保存于任务目录 `work/iot-package-layout-20260914`。本轮仅修改源码与模块文档，保留业务路由、JSON 字段、Dubbo 接口及版本、权限标识、数据库表和事务边界；运行服务和数据库未变更。

构建须使用 `clean` 清理旧包下的 `.class`，不能将新旧包的构建目录直接合并。此前按旧源码准备的发布包需要重新构建，不应直接作为本轮版本发布。

本次目录整理验证结果：

- Java 21 干净 `clean package` 成功；247 项模块测试，246 通过、1 因缺少外部历史遥测样本跳过。
- 三组原生告警测试纳入模块回归；新增检查验证 5 个业务接口可由 Spring 唯一装配。
- 真实 `SaInterceptor` 验证 80 个 HTTP 路由、840 项权限断言通过。
- 64 个保留并移动的实体、BO/VO/DTO 比对通过，类体仅有包引用变化。
- 包名与路径全部一致，源码树无空目录；JAR 包含全部新包类，不含迁移前包名及已删除的 3 个类。

本轮没有发布镜像、重启服务或修改数据库。

## ORM 改造交付

2026-09-14 将 11 个类中直接使用 JDBC 或配置 JDBC 的部分迁至 MyBatis-Plus；新增 8 个 Mapper 接口、5 份 XML，以及必要的表实体和内部 DTO。该阶段曾使用 `JetLinksHistoryQuery` 汇总历史查询；后续优化已移除此类，历史查询现直接进入各业务 Mapper。`DeviceOwnershipRepository`、`JetLinksBusinessStore` 保留在 `service/support`。

测试中保留 JDBC 建表和结果核对，以独立验证真实 MyBatis Mapper 的读写结果；业务源码不保留 `JdbcTemplate`、手动连接、Statement、ResultSet 或直接 SQL 执行。专项回归覆盖框架默认数据源及事务、租户插件兼容、事务回滚、历史范围、逻辑删除、命令状态时间顺序以及动态标识符校验。

操作前备份、逐文件清单与验证报告位于任务目录 `work/iot-orm-20260914`。本轮不变更业务表结构、HTTP/Dubbo 契约或权限标识；没有替换运行服务、更新数据库或发送设备指令。

本轮 ORM 验证结果：Java 21 干净构建通过；模块 261 项测试，260 通过、1 因缺少遥测样本跳过，0 失败、0 错误。其中包括新增 8 项 ORM 专项测试，并将 6 项已有告警设备归属测试纳入默认回归。58 条固定 SQL 与原 JDBC 语句逐条核对一致；8 个 Mapper、5 份 XML 和 3 个服务辅助类均已进入 JAR，旧持久化类没有残留。共享锁语句保留原 MySQL `FOR SHARE`，本轮使用隔离 H2 和 Spring 自动配置验证事务及查询，未连接实际 MySQL 做跨服务锁竞争验证。

## 事件驱动与注解 Mapper（2026-09-14）

本轮在上述 ORM 版本基础上继续整理：删除未使用的 `DomainJobExecutorSupport`、每秒轮询的 `JetLinksBusinessJob` 及调度配置、SnailJob 直接依赖；原有五份 Mapper XML 的 SQL 已迁入接口注解。业务服务移除 `TransactionTemplate`，采用 `@Transactional`，沿用框架默认事务管理器并保留业务事务边界。

JetLinks 在事件提交后通过 Dubbo 主动推送；业务收据和投影提交后推进必要的命令步骤，再确认事件。施肥任务在提交后执行第一步，后续由命令回执推进。未确认事件使用持久租约重投；启动时恢复存量任务，空闲时没有定时查询。详细部署顺序、契约和异常行为见 [事件驱动说明](docs/jetlinks-event-delivery.md)。

`List.getFirst()` 在 JDK 21 中有效；本模块统一使用明确的非空/数量检查与 `get(0)`，项目仍使用 JDK 21。

本轮备份和验证资料位于任务目录 `work/iot-events-annotations-20260914`。之前几节是各次改造的历史记录；当前实现不再依赖 Mapper XML 或农业库名配置。


### 2026-09-14 历史查询优化

- 移除按表名、列名分支的通用历史查询类，由施肥记录、阀门控制日志、阀门会话 Mapper 分别处理强类型查询条件。
- 施肥记录和控制日志使用数据库 COUNT 和分页 SQL，VO 转换沿用框架 MapStruct。默认每页 20 条、上限 500 条，未传分页参数也不加载全部历史；越界页返回空列表并保留总数。
- 按业务时间倒序，再按记录 ID 倒序，保证同一时间的数据分页顺序稳定。施肥设备编号仍精确匹配，阀门设备编号仍模糊匹配。
- 最近已结束会话在数据库筛选、排序和限量；时长统计使用 Mapper 注解 SQL 汇总，不再加载全部会话计算。
- 会话列表接口继续返回调用方筛选范围内的列表，并为 OPEN 会话计算实时持续时间。租户、逻辑删除、历史归属和接口返回结构保持不变。
- 不删除历史表或记录，不修改 JetLinks 数据，不发送设备指令。
