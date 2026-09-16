# 按 ym-cloud 开发配置同步农业与 IoT

## 来源与范围

- 用户指定来源：`/media/admin/extra/ym-project/ym-cloud/ym-app/src/main/resources/application-dev.yml`。
- 源仓库分支 `test`，只读，未使用 `config/application-test.yml`、生产配置或外置 `config/application.yml` 覆盖该文件。
- 目标：本机 Nacos `127.0.0.1:8848`，namespace `public`，group `DEFAULT_GROUP`，DataId `ym-agriculture.yml` 与 `ym-iot.yml`。
- YAML 多文档按顺序合并；`${变量:默认值}` 使用该文件明确给出的默认值，并递归解析文件内属性引用，不假定原单体运行环境另有环境变量。按目标字段类型解析布尔、整数、重试数组等。
- 仅同步两份目标配置中已存在且开发配置有对应值的业务字段。目标独有配置保留，不将单体全部配置直接覆盖到微服务。
- 保留 Spring 数据源、Dubbo、Snail-Job、框架 MQTT 禁用适配；未恢复旧轮询/Cron 参数。源文件不存在的巡检 AI、小程序、新闻媒体等参数保持原值。

## 实际变更

农业 3 项：

```text
amap-weather.key
satellite.delete-base-url
satellite.call-url
```

IoT 11 项：

```text
ym.mqtt.enabled
ym.iot.data-point.influx.url
ym.iot.data-point.influx.token
ym.iot.data-point.influx.bucket
ym.iot.data-point.influx.bucket-5m
ym.iot.data-point.influx.bucket-1h
ym.iot.data-point.influx.measurement
ym.iot.data-point.influx.latest-cache-seconds
ym.iot.fertilizer.enabled
ym.iot.fertilizer.test-mode.enabled
ym.iot.fertilizer.crc.startup-self-check
```

- MQTT 与施肥机 enabled 均按 dev 设为 `false`，施肥机 test-mode.enabled 设为 `true`，CRC startup-self-check 设为 `false`。
- InfluxDB 地址与存储命名按 dev 同步；未改 `ym.iot.data-point.storage`，没有启动 InfluxDB 服务。
- 农业另有 84 项、IoT 另有 97 项源值已经一致（包括解析后的语音/翻译配置与 GIS OSS 凭据），无需重复改写。

## 验证与生效边界

- 发布前备份、精确 YAML 字段差异检查、并发变更前置检查、带 casMd5 发布、发布后原文回读全部完成。
- 10 个已备份 DataId 最终逐一核对：仅上述 14 项变化，其余配置保持一致。
- 本次不重启服务、不下发设备命令、不主动执行 Snail-Job，不验证第三方地址连通性或外部 Key 的有效性。
- `@ConditionalOnProperty` 与启动时建立的连接不保证随配置热刷新重新创建/销毁；不能将 Nacos 写入成功等同于现存 MQTT 连接已经关闭。完整按新开关启动需另行重启目标服务。

## 备份、恢复与后续发布

- 原始快照及发布结果：`/tmp/ym-nacos-schedule-audit-v9metyiw/`。目录 `0700`，敏感配置文件 `0600`。
- `ym-agriculture.yml.json` / `ym-iot.yml.json` 是本次发布前状态；`*.dev-planned.yml` / `*.dev-after.json` 为计划和回读结果；`dev-sync-summary.json` 不包含配置值。
- 恢复时先备份届时 Nacos 内容，再通过 Admin API 恢复原快照的 `content`，并回读核对，避免覆盖后续他人改动。
- 本次只发布 Nacos，未将凭据变更再复制进仓库配置模板。以后运行整份模板发布脚本前，应先与 Nacos 当前版本比较，防止恢复本次变更前的值。
