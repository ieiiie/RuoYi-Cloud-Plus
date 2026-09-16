# 按 ym-cloud 恢复 IoT 全局表配置

## 决策与范围

用户明确要求按原 `ym-cloud` 的逻辑修复农业工作台“未知产品”，不采用局部产品查询放行方案。

参考 `/media/admin/extra/ym-project/ym-cloud/ym-app/src/main/resources/application.yml` 和该工程 `config/application.yml`，恢复以下 `tenant.excludes`：

- `iot_device`
- `iot_product`
- `iot_product_property`
- `iot_data_point`

这四张表不再由自动租户插件追加 `tenant_id` 条件。业务代码已有显式查询条件和权限校验不变；本次不能被理解为只开放产品名称的局部修复。

## 实施

- 更新本仓库 `script/config/nacos/application-common.yml` 的四个排除项及说明。
- 使用 Nacos 3 Admin API 更新本地 Nacos 的 `public / DEFAULT_GROUP / application-common.yml`。
- 发布前备份、CAS MD5 并发检查、发布后完整回读一致性校验均通过；解析后比对确认仅新增四个列表项，保留其他配置。
- 备份：`/tmp/ym-iot-tenant-excludes-re5fgd0a/before.json`、`after.json`，目录 0700、文件 0600，包含敏感配置，不应提交。
- 未修改 Java、前端业务代码或数据库记录，未修改其他 DataId，未恢复本地定时任务配置。

## 验证

- 原项目四表排除配置和目标 YAML 校验通过；每项仅出现一次。
- `git diff --check -- script/config/nacos/application-common.yml` 通过。
- IoT 容器 running / healthy，配置热更新生效，本次未重启服务。
- 浏览器刷新 `/agriculture/workspace`，当前租户为新疆源森农场：8 个产品分组全部恢复真实名称，土壤检测仪、气象仪、虫情检测仪、施肥机、海康威视摄像头、大疆机场3、五通阀、分体阀；“未知产品”消失。
- 页面设备数与故障截图保持一致。本次未执行 Maven 构建/单测，因为未修改 Java 代码；未执行设备控制命令。

## 回滚

如需撤回，应读取最新 Nacos 配置，仅移除本次四个排除项后通过 API 发布并回读验证；本地模板同步移除。不要在发生后续配置变更时直接用完整旧备份覆盖。租户数据可见性将恢复本次变更前行为。
