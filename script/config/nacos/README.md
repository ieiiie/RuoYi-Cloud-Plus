# 将此文件夹下所有配置文件内容复制到 `nacos` 对应的配置中

`ym-dbo.yml` 对应新模块 `ym-modules/ym-dbo`。Dbo 使用独立数据库和登录态，
通过网关 `/dbo/**` 访问，不启用多租户。

资源业务已合并到 `ym-system`。部署时发布更新后的 `ym-system.yml` 和
`ym-gateway.yml`，并从 Nacos 删除旧的 `ym-resource.yml` 配置。

YM 农业四服务及 Gateway 路由使用 Nacos 3 Admin API 发布，禁止直接修改
`config_info`：

```bash
NACOS_ACCESS_TOKEN='<approved token>' \
  bash script/config/nacos/publish-ym-services.sh
```

脚本会先将现有响应备份到 `/tmp/ym-nacos-backup-*`，按“领域服务、BFF、Gateway”
顺序发布，并逐项回读比对。没有 `NACOS_ACCESS_TOKEN` 时脚本会失败关闭。

## Snail-Job 迁移后的配置边界

农业和 IoT 的周期扫描、同步与刷新频率在 Snail-Job 控制台维护，不再向
Nacos 添加旧 `interval-seconds`、`interval-ms`、`poll-interval-*` 等本地调度参数。
具体删除清单与当前环境核验见
[`2026-09-04 清理记录`](../../../docs/migration/2026-09-04-nacos-schedule-cleanup.md)。

不要删除 `snail-job` 客户端连接配置、任务业务启停开关、批量大小、租约、
超时或重试参数。尤其 `ym.stask.voice.worker.enabled` 和
`ym.smart-farming.translation.worker.enabled` 仍由当前执行器读取；后者还控制
事务提交后的即时翻译处理。设备心跳、命令间隔、缓存刷新也不属于本次清理范围。
