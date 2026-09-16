# YM 农业平台测试环境启动验证（2026-09-03）

## 验证范围

- Docker Desktop 内存上限：`12535590912` 字节（约 11.67 GiB）。
- 基础设施：MySQL、Redis、Nacos 3.2.3、Snail Job Server 2.0.2。
- 平台服务：`ym-gateway`、`ym-auth`、`ym-system`、`ym-dbo`。
- 迁移服务：`ym-agriculture`、`ym-iot`、`ym-agriculture-miniapp`、`ym-farm-task-miniapp`。
- 配置来源：`/media/admin/extra/ym-project/ym-cloud` 的 `test` 分支配置，并在目标 Nacos YAML 中写入直接值。

本报告不记录密码、AppSecret、API Secret、Token 等敏感明文。

## 本轮修复

1. `ym-agriculture` 增加阿里云 OSS SDK，修复 OSS 自动配置缺类导致的启动失败。
2. `ym-iot` 的 WVP 适配使用独立 Jackson 2 `ObjectMapper`，避免 Spring Boot 4/Jackson 3 宿主注入冲突。
3. 电动阀 MQTT 条件改为同时判断业务 MQTT 和电动阀开关，关闭状态不再错误创建组件。
4. 关闭框架 Mica MQTT 客户端，仅保留迁移后的业务 Paho MQTT，消除对本机 `127.0.0.1:1883` 的无效重连。
5. Docker Compose 将 8 个 Java 服务日志改为命名卷，并设置 JVM 堆、元空间、直接内存上限和 `restart: unless-stopped`。
6. Nacos 发布脚本纳入 `datasource.yml` 和 `ym-auth.yml`。

## 构建结果

执行：

```text
bash mvnw -pl ym-modules/ym-agriculture,ym-modules/ym-iot -am package -Dmaven.test.skip=false -DskipTests=false
```

- Reactor：`29/29` 成功。
- `ym-agriculture`：1 个测试通过。
- `ym-iot`：1 个测试通过。
- 本轮没有使用 `-DskipTests` 跳过测试。

## 配置与注册结果

- YAML 语法解析通过；发布脚本 Bash 语法检查通过。
- 已发布并从 Nacos 回读：`datasource.yml`、`ym-auth.yml`、`ym-agriculture.yml`、`ym-iot.yml`、两个 BFF 配置和 `ym-gateway.yml`。
- 8 个 Java 服务均记录 `NacosServiceRegistry ... register finished`，注册地址为 Docker Desktop VM 内的 `192.168.65.9`。
- 8 个 Java 服务均记录应用 `Started`，当前容器均为 `running=true`、`oom=false`、`exit=0`。

## HTTP 与服务间链路

- Gateway 容器内访问 `/auth/code` 返回 HTTP 200 和验证码数据。
- `/mobile/smart-farming/**`、`/miniapp/smart-farming/stask/**`、`/smart-farming/**`、`/iot/**` 均命中统一鉴权链，未登录返回业务码 401；未出现 404 或 503。
- 最新日志未发现 Dubbo `No provider`、数据库连接失败或服务启动异常。
- 当前 Compose 使用 `network_mode: host`。Docker Desktop 会忽略同服务的 `ports` 映射，因此宿主机 `127.0.0.1:8080` 暂不可访问；本轮 HTTP 冒烟从 Gateway 容器内部完成。该项仍需在 Docker Desktop 改为桥接网络或启用其 Host Networking 后进行浏览器验证。

## 数据库与调度

- `ym-agriculture`：105 张表；包含 V1.5 库存、工单、天气预警等关键表。
- `ym-iot`：18 张表；遥测统一落到 MySQL `iot_data_point`，并包含告警、施肥机、电动阀、ISUP/HFZK 等表。
- Snail Job 执行器组 `ym-agriculture`、`ym-iot` 均启用。
- `sj_job_executor` 中农业 9 个、IoT 7 个执行器；Server 节点表有 3 条有效记录。
- Snail Job 启动最初出现一次无分桶节点 SQL 错误；服务节点建立后错误不再重复，客户端均显示启动成功。重试和节点故障转移尚未执行专项破坏性演练。

## IoT 与外围上游

通过：

- 施肥机 CRC 启动自检 8 个 fixture 全部通过。
- 业务 Paho MQTT 已连接原项目 Broker，并成功订阅 `+/pub0`（QoS 1）。
- 电动阀开关关闭时仅建立 Broker 连接，不注册处理器或订阅主题，符合当前原配置。

未通过：

- EMQX 管理 API 使用原配置返回 401 `BAD_API_KEY_OR_SECRET`。
- WVP 登录使用原配置返回 HTTP 502。
- HFZK 登录请求被远端提前断开。
- UAV 自动登录使用原配置返回 HTTP 502。

上述失败均来自外部测试上游或原凭据，领域服务本身保持运行；不能据此声明真实设备、视频、EMQX 在线对齐和无人机联调通过。

## 资源稳定性

- 12 个核心容器当前全部运行；迁移服务重启次数为 0、OOM 标记为 false。
- Docker Desktop 当前服务状态为 `active/running`。
- 全量构建与首次并发启动叠加时 Docker Desktop 曾被宿主 OOM killer 终止；构建完成后按服务顺序启动并加入 JVM 上限，目前运行稳定。
- 当前容器内存合计接近 Desktop 上限，后续浏览器、前端构建或并发压测前应继续保留余量，避免与 Maven/IDE 大内存任务同时执行。

## 本轮结论

测试环境的数据库、配置中心、调度中心、平台服务和四个迁移服务已经启动，Nacos 注册、基础 Gateway 鉴权链、Dubbo Provider 可用性、MySQL 建表、Snail Job 客户端注册及业务 MQTT 连接完成冒烟验证。

浏览器从宿主机访问、带真实登录态的业务接口、EMQX/WVP/HFZK/UAV 外部联调、Snail Job 重试/故障转移、两轮真实历史数据演练仍未通过，不能计入完整迁移验收。
