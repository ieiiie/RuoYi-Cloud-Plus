# 农业与农事任务领域重组

后续部署已完成：农业、认证和两个小程序聚合服务均已切换到本次产物；启动、注册和网关验证结果见 [部署记录](2026-09-04-agriculture-domain-deployment.md)。本文下方“尚未完成的环境验收”保留代码重构阶段的证据边界，最新部署状态以该记录为准。

## 结果与边界

保留 `ym-agriculture` 单服务、Maven 坐标、数据库结构和网关路径。业务根包仍为 `com.ym.agriculture`，启动类原位保留；其余实现归入 `farming`、`farmtask`、`shared`。功能包内部的 controller/service/dao/model 分层、类名及业务流程保持原样。

允许的依赖方向为 `farmtask → farming → shared`，任务也可直接使用共享能力。农业不得引用任务实现或任务契约；共享能力不得引用农业/任务实现或农业远程契约。农业与任务之间仍为同进程调用，不引入新的分布式事务。

## 功能迁移映射

以下路径均相对于 `ym-modules/ym-agriculture/src/main/java/com/ym/agriculture`。

| 原功能包 | 新位置 |
| --- | --- |
| field、crop、batch、farmwork、farmrecord、trace | `farming/<原功能包>` |
| weather、weatheralert、solarterms、market、news、dashboard、bigscreen | `farming/<原功能包>` |
| satellite、uav、algback、integration、tenantinit | `farming/<原功能包>` |
| employee、worker、assignment、workorder、leaderlabor、clocklocation、voice | `farmtask/<原功能包>` |
| inventory、inspection、inspectionphotoarchive、inspectionaichat、sop、yieldrecord、screen | `farmtask/<原功能包>` |
| common、config、support、media、job | `shared/<原功能包>` |
| 农业的三个 Dubbo Provider | `farming/dubbo` |
| 任务、库存、员工准入的三个 Dubbo Provider | `farmtask/dubbo` |
| dubbo/support/RemoteCommandIdempotencyExecutor | `shared/dubbo/support` |

`i18n` 按职责分开：

- `shared.i18n`：翻译存储、供应商客户端、公共模型、消息解析、本地化注解和处理器、资源事件及同步监听器、翻译 Worker 和 Job。
- `farming.i18n.support`：`FarmWorkI18nSourceFactory`、`SmartFarmingI18nSourceFactory`。天气已有的来源工厂和登记器随天气功能归农业。
- `farmtask.i18n`：员工翻译读取与监听、任务资源登记、任务文本组合、回填与预翻译编排、翻译管理 Controller/BO/VO、小程序语言过滤与响应处理、短信渲染。

农业初始化与天气登记使用农业来源工厂和共享翻译服务，不引用任务侧回填编排。农活字典的 `FarmWorkAssignmentSnapshotSynchronizer` 仍由农业定义、任务实现，保留原事务中的任务快照同步。

## 远程契约与 HTTP 兼容性

API 根目录仍位于 `ym-api/ym-api-agriculture/src/main/java/com/ym/agriculture/api`。

| 新契约包 | 接口 |
| --- | --- |
| `api.farming` | RemoteAgricultureService、RemoteAgricultureMobileService、RemoteAgricultureTenantService |
| `api.farmtask` | RemoteFarmTaskService、RemoteInventoryService、RemoteEmployeeAdmissionService |

BO/VO 随领域进入 `domain.bo` / `domain.vo`；任务、库存、员工准入和打卡点模型归任务，其余归农业。未保留旧包名的兼容接口。

`getClockLocation()` 从农业契约迁至 `RemoteFarmTaskService`，Provider 方法体完整迁移。任务小程序改调新契约，原 `GET /miniapp/smart-farming/stask/clock-location` 路径、`R` 包装及返回字段不变。未配置打卡点时仍返回 CGCS2000、tianditu、configured=false、fenceCheckRequired=false。

农业小程序、任务小程序及 `ym-auth` 的实际调用点已同步。自动配置导入文件同步到新包；根组件扫描和原 Mapper 通配扫描仍覆盖三个领域。既有 Advice 的匹配范围未扩大，任务执行器名称、资源路径、权限标识、配置键及缓存键名未改变。

## 验证证据

2026-09-04 执行结果：

- 重构前干净构建通过，农业原有六个测试类共 12 项通过。
- 重构后四个相关服务及依赖干净打包通过。Java 测试共 24 项，失败/错误/跳过均为 0：农业 19 项、农业小程序 2 项、任务小程序 1 项、认证服务 2 项。
- 领域依赖规则五项 Python 测试通过，完整迁移边界脚本通过。
- 新增行为验证覆盖：打卡点未配置默认值与全部字段传递、原 HTTP 入口调用任务契约、农业翻译来源在无任务 Bean 的 Spring 上下文中到达共享监听器，以及登记异常继续同步向调用线程传播。
- 装配验证覆盖：根组件扫描发现六个 Provider 及契约绑定，原 Mapper 扫描模式发现农业/任务/共享 Mapper，三条迁移后的自动配置资源解析及客户端 Bean 装配。
- 保存并比较 1,189 个迁移前源文件/资源；其中 1,183 个除包名、import、排版空白外一致。其余六处为两个打卡点 Provider、两个对应契约、打卡点 BFF，以及一份仅修改包引用的 package-info 文档。68 个 Controller 中只有打卡点 BFF 改动内部调用；全部 502 个模型文件除包名/import 外一致。
- 缓存迁移脚本在无网络、无业务数据挂载的临时 Redis 7.4 容器中通过检查、迁移、内容保留、TTL 保留、幂等、无关键拒绝、非法 JSON 保护和缺失键验证；临时容器已删除。

可重复执行的检查：

```bash
python3 script/verify-agriculture-domains.py
python3 -m unittest discover -s script/tests -p 'test_agriculture_domains.py'
bash script/verify-migration-boundaries.sh
bash mvnw -pl ym-modules/ym-agriculture,ym-modules/ym-agriculture-miniapp,ym-modules/ym-farm-task-miniapp,ym-auth -am clean package -Dmaven.test.skip=false -DskipTests=false -Dgroups=dev
```

Maven 默认会跳过测试，必须显式使用上述参数；MapStruct Plus 编译处理器还需要用户 `.msp` 缓存写权限。沙箱缓存写入失败与业务测试失败应分别判断。

本次临时证据目录为 `/tmp/ym-domain-reorg-20260904/`，包含迁移前源文件快照 `before.json`、逐文件/逐类映射 `mapping.json`、源文件比对 `source-audit.json`、基线及最终构建日志和测试统计。该目录为本机临时证据，不是运行时依赖。

## 协调发布与尚未完成的环境验收

本次没有替换运行中的服务、变更 Nacos/网关、执行真实业务写入，也没有提交或推送。以上装配测试不等于新版本已经在注册中心注册成功，或完整业务链路已经通过环境验收。

发布时需要协调升级农业服务、农业小程序、农事任务小程序和认证服务。新旧 Dubbo 接口包名不同，不支持混合版本运行；如果存在仓库外消费者，同样需要同步升级。

实际发现的缓存兼容点：Redis 通用 Codec 会保存非 final 对象的全类名，邀请码二维码缓存中的 `MiniProgramCodeVo` 从 `com.ym.agriculture.employee.model.vo` 移至 `com.ym.agriculture.farmtask.employee.model.vo`。仅协调升级服务不足以读取未过期的旧缓存。

为此提供 `script/migration/20260904-agriculture-cache-types.lua`：只接收一个已盘点的二维码缓存键，默认只检查；`apply` 原子替换旧类名，保留其他 JSON 内容、键名和 TTL；不扫描、不删除缓存。它不处理 access_token、登录会话或其他缓存。

在目标业务 Redis 数据库中盘点物理键 `*global:wechat:miniprogram:invite_code_qr:*`，使用已有连接及认证方式逐键执行。以下 `<已盘点的完整缓存键>` 必须替换为真实键；需要保留部署环境可能附加的 Redis 前缀。

```bash
redis-cli --eval script/migration/20260904-agriculture-cache-types.lua '<已盘点的完整缓存键>' , check
# 协调发布窗口内，旧实例停止写入后执行：
redis-cli --eval script/migration/20260904-agriculture-cache-types.lua '<已盘点的完整缓存键>' , apply
```

脚本没有对当前业务 Redis 执行。回滚时应同步回滚全部消费者，并处理该类可再生二维码缓存，避免旧代码读取新类名。

环境验收待协调发布后完成：确认六个新 Dubbo 契约注册与调用、同一租户下农业查询和打卡点查询、任务执行与验收、领退料、巡检、员工准入以及中维文响应。涉及写入的链路应使用明确的测试租户和可清理数据，不把当前旧版本健康状态当作新版本验收结果。
