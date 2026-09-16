# Nacos 旧定时任务配置清理

## 环境与边界

- Nacos：本机 `127.0.0.1:8848`，namespace `public`，group `DEFAULT_GROUP`。
- 盘点 10 个 DataId，仅修改 `ym-agriculture.yml`、`ym-iot.yml`。
- 使用 Nacos 3 Admin API 备份、发布、回读，未直接修改 Nacos 数据库。
- 基于线上原文仅删除已无引用的字段及由此产生的空父节点，未用本地模板覆盖整份线上配置。
- 本地两份配置模板原先已不包含这些字段；本次补充 README 配置边界，避免重新添加。

## 删除字段（11 项）

农业（4 项）：

```text
smart-farming.news.media-transfer.interval-seconds
ym.alg-back.token-refresh.interval-ms
ym.smart-farming.translation.worker.interval-seconds
ym.stask.voice.worker.interval-seconds
```

IoT（7 项）：

```text
ym.iot.alert.offline.bootstrap-delay-ms
ym.iot.alert.offline.legacy-redis-enabled
ym.iot.alert.offline.poll-batch
ym.iot.alert.offline.poll-interval-ms
ym.iot.alert.offline.reconcile-interval-ms
ym.iot.emqx-presence.poll-interval-seconds
ym.iot.wvp.token-refresh.interval-ms
```

对应当前执行器：`sfNewsMediaTransferJob`、`algBackTokenRefreshJob`、
`sfI18nTextProcessJob`、`sfStaskVoiceBroadcastJob`、`iotAlertOfflineEvaluationJob`、
`emqxPresenceSyncJob`、`wvpTokenRefreshJob`。周期由 Snail-Job 定义，本次未更改其任务定义或主动触发任务。

## 保留

- `snail-job` 客户端连接、组、命名空间、RPC 配置。
- `worker.enabled`（仍被语音/翻译执行器读取，翻译即时处理器也依赖此开关）。
- 批量大小、租约、处理超时、重试次数与重试间隔。
- 第三方接口、账号、数据源、设备心跳、命令节流和缓存批量刷新等其他配置。

## 验证与恢复

- 农业、IoT 运行 JAR 的 SHA-256 与当前本机构建产物分别一致；源码与产物中未发现被删调度参数的读取引用。
- YAML 解析比较：删除前后差异仅为上述字段及空父节点；所有保留字段值不变。
- 两份配置发布后逐字回读一致，再次运行返回 `Already clean`。
- 清理后农业、IoT、Snail-Job Server 容器均 `running / healthy`，重启计数均为 0；未执行服务重启。
- 未主动运行定时任务或验证外部业务执行、重试、故障转移，不将配置清理视为业务任务验收。
- 原始快照：`/tmp/ym-nacos-schedule-audit-yht3b51_/`，目录权限 `0700`，配置文件 `0600`，包含敏感配置，不提交到 Git。
- 恢复时取 `ym-agriculture.yml.json`、`ym-iot.yml.json` 中的 `content`，通过同一 namespace/group/DataId 的 Nacos Admin API 发布并回读。恢复前先备份届时线上内容，避免覆盖后续他人改动；也可在 Nacos 历史版本中回滚本次发布。
