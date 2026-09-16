# 农业领域重组部署记录

部署时间：2026-09-04T16:44:27+08:00

## 已部署服务

| 服务 | 容器 ID | 镜像 ID | 状态 | 重启次数 |
| --- | --- | --- | --- | --- |
| ym-agriculture | `18357c011259` | `976a6493bc39` | healthy | 0 |
| ym-auth | `f5c24b9ab85a` | `b18ae7c1200b` | healthy | 0 |
| ym-agriculture-miniapp | `b0da6c56acb9` | `707a1924ebb0` | healthy | 0 |
| ym-farm-task-miniapp | `193de4461249` | `0ba905603fb4` | healthy | 0 |

四个服务均从本次已经通过干净构建与测试的 Jar 构建本地镜像，容器内 `app.jar` 的 SHA-256 与工作区产物逐一一致。四个 Jar 携带的 `ym-api-agriculture` 校验和相同，均为新的 farming/farmtask 契约。

两个小程序聚合服务部署前处于停止状态，本次已重建并启动。农业和认证旧实例在协调窗口停止后替换；Gateway、System、DBO、IoT、Snail Job、MySQL、Redis 和 Nacos 未重建，未更改网关路由或 Nacos 配置。

## 验证结果

- 四个容器 healthy，RestartCount=0，OOMKilled=false，均有应用 Started 日志。
- 四个服务各有一个 DEFAULT_GROUP 的 Nacos 健康实例，农业另有 DUBBO_GROUP 的健康实例。
- Dubbo 元数据已包含 `api.farming` 的农业、农业小程序、农业租户三个契约，以及 `api.farmtask` 的任务、库存、员工准入三个契约。旧接口的历史 mapping 字段未清除，不作为旧接口仍被新实例导出的证据。
- 新实例日志未发现 ERROR、No provider available、No available provider 或 Failed to subscribe。
- Gateway `GET /auth/code`：HTTP 200，业务 code=200。
- Gateway `GET /mobile/smart-farming/fields/list` 和 `GET /miniapp/smart-farming/stask/clock-location`：未登录时 HTTP 200、业务 code=401，提示缺少有效 token。这验证鉴权响应，不代表已完成登录后的业务调用。
- 农业业务 Redis DB0 的二维码缓存键盘点为 0；停旧实例后复查仍为 0，没有执行缓存值替换或删除。

健康证据包括 Docker HTTP 存活探针、应用 Started、Nacos 健康注册和网关认证入口。直接访问 `/v3/api-docs` 返回业务 401，未取得可比较的运行时 OpenAPI 基线；Nacos 未配置监控 Basic 凭据，因此没有绕过受保护的 Actuator，也不宣称其返回 UP。未执行任务、领退料或巡检等真实业务写入。

## 回滚依据

旧镜像已在替换前保留：

- `ym-agriculture`：`ym/ym-agriculture:before-domain-20260904T083718Z`（原状态：running）。
- `ym-agriculture-miniapp`：`ym/ym-agriculture-miniapp:before-domain-20260904T083718Z`（原状态：exited）。
- `ym-farm-task-miniapp`：`ym/ym-farm-task-miniapp:before-domain-20260904T083718Z`（原状态：exited）。
- `ym-auth`：`ym/ym-auth:before-domain-20260904T083718Z`（原状态：running）。

如需回滚，应同步回退四个服务的镜像，避免旧消费者与新 Provider 混跑。用对应回滚标签重新标记为 `ym/<服务名>:6.0.0` 后，按本次相同 Compose 文件逐服务执行 `up -d --no-deps --force-recreate --wait`；是否恢复两个小程序原先停止状态，应按回滚目标处理。不要执行 `down -v`。

本机部署证据位于 `/tmp/ym-domain-release-20260904/`，包含旧镜像身份、镜像构建日志、部署日志、运行包校验和、Nacos 注册、网关探测和最终状态。临时读取的配置和会话凭据文件已清理，未纳入仓库。

本次没有提交或推送代码。
