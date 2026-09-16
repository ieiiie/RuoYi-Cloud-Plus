# 应用管理与租户菜单合并

## 实现

- Dbo 仅保留“租户菜单”导航，左侧维护应用基本信息，右侧按应用类型维护菜单或组合菜单选择。旧 `/saas/app` 跳转 `/saas/menu`。
- 非组合应用仍按 `appId` 查询菜单。组合应用按微应用分组，选择目录、页面和按钮权限；不复制或修改源菜单。
- 组合菜单提供显式保存、撤销，以及切换应用、编辑应用、离开页面前的保存/放弃/取消提示。浏览器关闭或刷新使用原生未保存提示。
- 新组合应用默认停用，允许空菜单；启用或保存非空菜单必须含页面。基本信息更新省略 `menuIds` 时保留关系，显式列表仍兼容原客户端。
- 新增 `PUT /saas/app/{appId}/menus`，请求 `{ "menuIds": [...] }`，使用 `saas:app:edit` 权限、事务、日志、防重复提交及原授权失效通知。菜单与状态更新锁定同一应用，避免并发启用和清空穿透校验。
- 微应用资源入口及用户访问短路径未改变。

## 导航迁移

执行文件：`ym-modules/ym-dbo/src/main/resources/db/update/008-merge-app-menu-management.sql`。

仅在 `ry_dbo` 执行。原应用页面 ID 转为 `saas:app:list` 功能权限节点，原应用按钮归入租户菜单。已有应用/菜单权限的角色补齐合并页面读取依赖，不新增写权限。初始化脚本已同步。

本地执行前备份 `sys_menu`、`sys_role_menu` 到 `/tmp/dbo-before-menu-merge.sql`。两次执行后导航和授权结果一致，写权限集合未变，`ry-cloud` 应用、业务菜单归属及组合关系未变。验证记录：`/tmp/dbo-migration-verification.json`。

## 验证

- 前端 `vue-tsc --noEmit --skipLibCheck` 通过；修改文件 Prettier 检查通过。
- JDK 21 Maven 构建成功，显式启用测试：`CompositeAppMenuSelectionTest` 8 项、`SaasAppMenuUpdateTest` 6 项、`SaasTenantRoleMenusTest` 4 项、`SaasTenantPackageSwitchTest` 4 项，共 22 项，无失败、错误或跳过。
- 测试使用 `-DreuseForks=false` 隔离各测试类既有的静态转换器模拟缓存。
- 浏览器验证组合菜单回显、未保存切换提示及放弃修改；临时应用新建后自动选中且停用，勾选农业工作台与物联网视频监控，保存后数据库仅含两个页面和补齐的实时监控目录。
- 编辑该应用名称并启用后，三条组合关系保持不变。删除临时应用后选择相邻应用；临时应用无有效记录，关联记录为零。
- `/saas/app` 最终跳转 `/saas/menu`；数据库旧页面入口为零。
- 核对 `SysAppRuntimeServiceImpl`：组合选择先与套餐菜单取交集，普通用户再经 `selectMenuTreeByUserId`、`selectMenuPermsByUserId` 限定角色权限。此链路未修改；租户角色/套餐测试通过。本次未使用真实普通租户账号执行完整登录与业务页面验收。

## 本地部署

- 仅替换 `ym-dbo`，镜像 `ym/ym-dbo:6.0.0`；原镜像保留为 `ym/ym-dbo:before-app-menu-merge-20260904`。
- Dbo 日志确认启动成功，重启计数 0，内存上限保持 768 MiB。
- 通过现有网关 `http://localhost:18081/dbo/auth/code` 验证业务响应 `code=200`。Docker Desktop 环境的宿主 9203 不是本次访问验证入口。
- 未提交或推送 Git，保留其他既有工作区改动。
