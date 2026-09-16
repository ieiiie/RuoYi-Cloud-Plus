# 本地 Docker 稳定性调整

## 故障证据

- `systemctl --user show docker-desktop` 返回 `Result=oom-kill`，2026-09-04 13:58:36 用户服务日志也记录同一结果。
- 原 Docker Desktop 设置为 16384 MiB；宿主总内存约 30 GiB，Docker 停止后仍占用约 14 GiB，宿主 swap 使用约 12 GiB。
- 恢复后农业瞬时占用约 755/768 MiB；Snail Job 约 1.25 GiB、Nacos 约 1.05 GiB，后二者原来均没有容器内存上限。
- 这是明确的 Docker 用户服务 OOM 证据；没有权限读取完整内核日志，不推断具体 OOM 选择算法或所有历史退出的原因。

## 已落地配置

- Docker Desktop `MemoryMiB` 从 16384 改为 10240，swap 保持 2048 MiB。原设置备份：`/home/admin/.docker/desktop/settings-store.json.before-ym-stability-20260904`。
- 用户 systemd drop-in `/home/admin/.config/systemd/user/docker-desktop.service.d/ym-stability.conf`：失败退出 30 秒后重启，600 秒内限 3 次启动。正常手动停止不会触发恢复；未更改开机自启偏好。
- YM Java 进程使用 G1、明确堆/Metaspace/直接内存边界、`ActiveProcessorCount=2`、OOM 时退出；`init + exec` 转发终止信号，60 秒容器退出宽限。
- Compose 管理的 YM 服务、Snail Job、MySQL、Redis stdout/stderr 日志使用 20 MiB × 3 轮转。应用自身命名卷内的日志沿用原 logback 规则，Nacos 独立容器日志未改写，不宣称所有文件日志均有此上限。
- HTTP 健康探针有 120 秒启动宽限。YM 根路径 200 仅说明 HTTP 存活，不等于全部 Dubbo/业务依赖健康；不开放或绕过受保护的 Actuator。

| 服务 | 容器内存上限 | 内存加 swap 上限 | Java 最大堆 |
| --- | --- | --- | --- |
| 农业 | 1024 MiB | 1280 MiB | 384 MiB |
| Gateway/Auth/System/DBO/IoT，各自 | 768 MiB | 1024 MiB | 256 MiB |
| Snail Job | 1024 MiB | 1280 MiB | 384 MiB |
| Nacos | 1536 MiB | 1792 MiB | 保留原 512 MiB |
| MySQL | 1280 MiB | 1536 MiB | 不适用 |
| Redis | 512 MiB | 768 MiB | 不适用 |

当前 10 个容器的内存硬上限合计 9 GiB，Docker VM 为 10 GiB。只适用于当前本地开发负载；不要同时启动 ELK/SkyWalking 等整套扩展。Redis 未开启淘汰策略，避免通过删除缓存/业务数据来换取内存。

## 启动与回滚

Docker Desktop 运行后，在仓库根目录执行：

```bash
bash script/docker/start-ym-local.sh
```

脚本使用实际基础设施 Compose `/media/admin/extra/docker-services/ym-infra/compose.yaml`，只启动 MySQL/Redis，等待现有 Nacos 3 就绪，再依次启动 Snail Job、System、Auth、DBO、农业、IoT、Gateway。使用 `--no-deps`，不启动主 Compose 中的参考 MySQL/Nacos 或其他扩展。不会启动原本停止的两个小程序服务和 InfluxDB。

Nacos 非 Compose 创建，其 `docker update` 内存限制会保留至容器被删除；脚本每次重新应用限制。若要重建 Nacos，应另行保留原命名数据卷、网络和配置。

主 Compose 调整前备份位于 `/tmp/ym-compose-before-stability-20260904.yml`（权限 600，含既有配置，不应上传）。如需回滚，先对照当前差异仅撤销本次资源配置，不覆盖之后的新修改；恢复旧 16 GiB VM 设置会重新引入宿主内存压力。严禁通过 `down -v` 回滚。

## 验证边界

- 两份 Compose 均通过 `config --quiet`，启动脚本通过 `bash -n`。
- 重建后 MySQL/Redis 均 healthy，数据卷仍为 `ym-infra_ym_mysql_data` 和 `ym-infra_ym_redis_data`，未删除业务数据。
- 14:14:40 核验：6 个 YM 服务、Snail Job、MySQL、Redis 共 9 个重建容器均 healthy、RestartCount=0、OOMKilled=false；Nacos running，v3 readiness HTTP 200（该独立容器历史 RestartCount=1，未抹除）。Docker Desktop active、Result=success、NRestarts=0。
- 当前容器合计约 4.9 GiB；农业约 653 MiB/1 GiB、Snail Job 约 349 MiB/1 GiB；宿主 MemAvailable 约 6.4 GiB。宿主既有 swap 约 12 GiB 未清理，不运行 swapoff 制造内存峰值。
- Gateway `/auth/code` HTTP 200 且业务 code=200；`/dbo/auth/code` HTTP 200；`/system/v3/api-docs` HTTP 200 且返回 OpenAPI 3.1.0。农业/IoT 在容器内访问各自 `/v3/api-docs` 返回 200。
- `/agriculture/v3/api-docs` 不是既有 Gateway 路由（试探返回 500），不将其作为验收接口，也未为此改变业务路由。Docker Desktop 环境下宿主直接访问 9221/9222 不通，本次保留原 host 网络拓扑，以容器内地址验证，未添加新端口暴露。
- 启动脚本完整执行退出码 0；6 个 Java 应用均有 Started 日志。仅完成短时启动/存活与基础路由检查，没有执行长期压力测试、真实设备控制或后台任务副作用验证。

参考：[Docker Desktop 资源设置](https://docs.docker.com/desktop/settings-and-maintenance/settings/)、[Compose 服务配置](https://docs.docker.com/reference/compose-file/services/)。容器 unhealthy 本身不会被 restart 策略自动重启；退出时才按策略恢复，不添加无限健康失败重启循环。
