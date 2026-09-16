# 启用物联网 MQTT

- 用户明确授权将 `ym.mqtt.enabled` 设置为 `true`。
- 目标仅为当前 Nacos `public / DEFAULT_GROUP / ym-iot.yml`；从实时配置读取，单字段替换，YAML 结构比较确认无其他变化。
- 发布前备份、二次读取并发检查、`casMd5` 发布和发布后精确回读均通过。
- 敏感配置备份位于 `/tmp/ym-iot-mqtt-enable-20260908-64zoib5l/`，目录及文件由 umask 077 保护，不提交。
- 仅重启 `ym-iot`，新进程健康检查 healthy，日志确认 MQTT 连接及施肥机主题订阅成功。
- 容器内只读 `GET /fertilizer/runtime-config` 返回 HTTP 200、业务 code 200，测试模式保持 false。
- 未主动调用设备状态查询或下发控制命令。现有 MQTT 接入正常启动后的订阅及消息处理未禁用。
- WVP 外部服务仍返回 502，与本配置变更无关。

## 报警规则检查周期说明（源码核对，未修改规则或任务）

- 离线规则的 `checkIntervalSec` 被 `IotAlertOfflineEvaluationThrottle` 用作 Redis NX+TTL 的最小评估间隔。
- 修改规则事务提交后，`AlertRuleChangeApplier` 清除规则节流键并广播其他节点。
- 下一次 Snail Job 扫描可使用新配置；并不修改 Snail Job 任务的触发频率，也不为每条规则创建独立任务。
- 实际检查时间受任务扫描频率及运行延迟影响；7200 秒表示规则最小检查间隔两小时，不保证精确到秒触发。
