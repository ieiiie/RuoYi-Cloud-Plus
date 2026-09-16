# 租户更换套餐后应用不可见修复

## 根因与代码

DBO `SaasTenantServiceImpl.updateByBo` 原先仅更新租户套餐 ID 并使会话失效，没有同步角色菜单。
现在更换套餐时，在现有本地事务内调用 `SaasTenantProvisionService.synchronizeTenantRoleMenus`：

- 只查询当前租户角色，管理员菜单替换为套餐菜单集合。
- 普通角色仅删除套餐外授权，不套用模板增加授权；不修改角色启停状态。
- 套餐不变时不重设权限；更新失败或同步失败不发送会话失效通知。
- 沿用现有事务提交后的会话失效通知，不全量清空 Redis。

## 测试环境数据修复

租户 `916085`（SaaS联动测试租户_20260828），绑定套餐实际名称 `testaaaa`，ID `2095694430483292161`。
该套餐配置组合应用 `test`。

管理员 `2093240822863159299` 原有 18 项授权，与套餐交集为 0；修复后为套餐全部 132 项，套餐外授权为 0。
普通角色 `2093240823425196034` 原来和修复后均为 0 项，未新增授权。
未修改用户角色关系、其他租户、套餐和应用定义。

执行脚本：`script/sql/migration/20260904-repair-tenant-916085-package-menus.sql`。
旧 18 项授权保存在 `ry-cloud.bak_role_menu_916085_20260904`；脚本带前置校验和回退说明，不应重复执行。

## 验证与部署

- 显式使用 `-DskipTests=false -Dmaven.test.skip=false` 执行两个新增测试类，8 项成功、0 失败、0 跳过。
- `mvn -pl ym-modules/ym-dbo -am package -DskipTests=false -Dmaven.test.skip=false -Dtest='SaasTenantRoleMenusTest,SaasTenantPackageSwitchTest' -Dsurefire.failIfNoSpecifiedTests=false`：28 个 Reactor 模块构建成功。
- 仅部署并重启 `ym-dbo`，日志确认 `Started YmDboApplication`、运营平台服务启动成功；内存限制保持 805306368 字节（768 MiB）。
- 旧镜像保留为 `ym/ym-dbo:before-package-sync-20260904`。
- 浏览器重新载入 5666 工作台，当前租户仍为 `SaaS联动测试租户_20260828`，已出现 `test / 打开应用` 按钮，原空状态消失。

边界：本次是针对套餐切换的单元回归、真实授权数据校验、DBO 启动和工作台展示验证；未宣称全部业务功能或完整 Reactor 自动化测试通过。
