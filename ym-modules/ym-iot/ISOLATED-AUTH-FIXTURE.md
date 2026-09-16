# 隔离联调认证方案（无生产入口）

正常 fatjar 包含真实 LoginHelper、LoginUser、StpLogicJwtForSimple、PlusSaTokenDao/TenantSaTokenDao、SaPermissionImpl 和 Redis 依赖。生产路由没有增加登录后门，也未关闭 SaCheckPermission。主线 helper 必须仅在独立测试 JVM 或 test classpath 运行，不能加入 src/main 或生产 Spring 扫描。

1. helper 与隔离 ym-iot 使用同一套实际生效的 sa-token 配置（token-name、Bearer prefix、JWT secret、timeout）以及相同 tenant.enable。不要手工拼 Redis key，TenantSaTokenDao 会使用全局认证命名空间。
2. 连接主线新建的**独立 Redis 实例**，显式配置 host/port/database/password 与 Redisson 连接，不借用正式 Redis 的默认 DB。不同 DB 不能代替确认独立端点。关闭 discovery/Snail，Dubbo registry group 使用 JETLINKS_MIGRATION_TEST，SMS 关闭。
3. helper 初始化真实 Redis/Redisson、SaTokenConfiguration 和（启用多租户时）TenantSaTokenDao，建立测试 servlet 请求上下文后调用真实 LoginHelper.login。LoginUser 必填 userId、userType=sys_user、tenantId、username；从隔离库原租户成员取数，globalUserId 如实填，不给普通租户伪造平台管理员 ID。menuPermission 与 rolePermission 是实际 SaPermissionImpl 读取的会话权限集合。
4. 调用形状：LoginHelper.login(loginUser, new SaLoginParameter().setTimeout(3600)); 然后从 StpUtil.getTokenValue() 取 token。该路径自己写 token session 内 loginUser 及真实 extra userId/globalUserId/tenantId，无需登录 auth 服务或写业务库。
5. HTTP 请求使用实际 token-name header，值为 Bearer + token。SaTokenServlet 上下文若由 helper 建立，可用 test 依赖 MockHttpServletRequest/MockHttpServletResponse + RequestContextHolder；helper 写入后销毁本地上下文，不注销仍待 HTTP 验证的 token。不要把 token 输出到共享构建日志或文档。
6. 至少创建：租户 A 有读权限、租户 A 无目标权限、租户 B 有读权限、独立平台管理员（仅事件恢复测试需要）四类会话。验证无 token 拒绝、无权限拒绝、A/B 列表与分页边界、对方 deviceId/GB 编号/批量命令拒绝、1000 年初始化 latest、转移后的 visibleFrom。命令只验授权拒绝或由主线使用隔离 fake provider，不向实体设备下发。

源码依据：ym-common/ym-common-satoken/.../LoginHelper.java 的 login 将 LoginUser 写入 tokenSession；SaPermissionImpl 优先读取 LoginUser.menuPermission/rolePermission；ym-common/ym-common-tenant/.../TenantSaTokenDao 负责全局认证 key 前缀；LoginUser.getLoginId() 组合 userType:userId。独立 helper 已在交付目录实现并用 ym-iot-migration-redis 实际签发四类真实身份会话；另一个新 JVM 完成反序列化与权限解析。helper 不在 src/main，不进入 fatjar。外部 YAML 未写 dynamic-active-timeout 时必须保留 common-satoken.yml 默认 true，否则登录没有活跃时间状态而被真实服务报告 TOKEN_FREEZE。

实现与可复用 runner：`/home/admin/Documents/Codex/2026-09-09/ym-iot-compatibility/auth-fixture/README.md`。真实原 SaaS 普通身份：658226 user2053641427716415491/global2053641427716415490；000000 user/global1761100000000000003；平台管理员 user/global1761100000000000001。A/B 最小读取权限、A空权限、真实管理员event:query四会话均仅写独立Redis。当前 tokens-002.json 为0600，严禁输出其内容。真实隔离HTTP 29/1列表、分页、详情、跨租户与GB编号及混合latest批次拒绝共14项已通过；旧包3项注解检查失败记录保留，待主线换新fatjar后使用同一token复测。

最终隔离运行验证：主线换为 ym-iot:jetlinks-1.0.0-20260909 后，复用 tokens-002 全部21项HTTP通过；匿名401、空权限403和29/1归属范围均验证。未发送命令/短信/视频操作，也未改变归属基线。
