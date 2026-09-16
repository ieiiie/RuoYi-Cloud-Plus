# 关闭 Same-Token 内网认证

## 修改

将 Nacos 公共配置 `application-common.yml` 中的现有开关改为：

```yaml
sa-token:
  check-same-token: false
```

DBO 的 `ym-dbo.yml` 原本已经是 `false`。同步撤回此前为 DBO 增加的专用 Same-Token 读取和 Dubbo 发送代码，使 HTTP 与 Dubbo 均遵循框架现有配置，不再读取或传递内部 Same-Token。Dubbo 服务注册、元数据 DB0 隔离修复和 DBO 业务 Redis DB2 隔离保持不变。

## 运行配置

2026-09-08 已通过 Nacos 配置 API只修改公共配置的 `check-same-token`，从 `true` 发布为 `false`。System、Auth、Gateway、DBO 均收到并成功加载配置变更事件。

使用 DBO 容器内的短生命周期验证程序直连 System Dubbo 服务，明确禁用 Sa-Token 自带过滤器及此前的 DBO 专用过滤器，然后调用 `SaasSystemControlService.invalidateTenantSessions`。参数使用不存在的随机租户标识，不修改租户或真实用户会话。验证结果 `noSameTokenAccepted=true`，证明 System 热更新后已允许无 Same-Token 的调用。

## 验证边界

此变更关闭内网服务调用的 Same-Token 身份校验，内网 Dubbo 调用不再依赖公共认证凭证；调用方仍需通过网络、注册中心及各接口自身的业务权限边界。HTTP 登录令牌、用户会话、数据库和 Redis 业务缓存配置未改变。

过程证据保存在 `/tmp/same-token-disable-20260908/`。

## 最终结果

- 宿主机默认 JDK 不支持 Java 21，首次构建在 `ym-common-core` 编译前失败；改用 Maven 3.9.11 + Eclipse Temurin 21 临时容器完成干净构建。
- JDK 21 构建成功：DBO 35 项测试通过；公共 Dubbo 4 项执行并通过，另 2 项需要显式临时 Redis 端口的既有测试按条件跳过。
- 构建及运行 JAR 均不包含 `DboRpcSameTokenStore`、`DboRpcSameTokenFilter` 或 DBO 自定义 Dubbo Filter SPI。
- DBO 清理版镜像已部署；回退镜像为 `ym/ym-dbo:rollback-before-disable-same-token-20260908`。
- 最终部署后再次验证 `noSameTokenAccepted=true`。
- 原 Nacos 配置临时备份含其他敏感配置，验证结束后已删除；Nacos 自身保留配置历史。
