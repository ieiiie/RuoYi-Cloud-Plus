# 组合应用接口权限与 SOP 加载修复

## 问题证据

- 用户提供的 `loadPage index.vue:201 / mounted:359` 对应农业 SOP，不是入库单包装页面。
- 在租户 `916085` 的组合应用 `test` 中复现：`/smart-farming/stask/sops/page` 被农业服务拒绝，权限标识为 `smartfarming:staskSop:list`。
- 只读核对 `ry-cloud`：SOP 六项菜单权限均已启用且同时存在于该租户套餐、管理员角色授权；没有通过新增授权解决问题。
- `SysAppRuntimeServiceImpl` 能计算组合应用选中的源菜单，但 `SysMenuServiceImpl.selectCurrentTenantPackageMenuIds` 仅以套餐直接开通的 appId 过滤菜单。组合应用 appId 与农业源菜单 appId 不同，导致接口权限集合丢失。
- 单独刷新会话后仍能复现，证明不能仅归因于旧 token；套餐范围过滤和会话同步需要同时修复。

## 实现

- `TenantPackageMenuScope` 保留直接应用授权，补充组合应用明确选中且在套餐内、源应用有效的菜单；随后仍由现有 Mapper 与当前用户角色授权取交集。
- 不从源应用自动添加其他菜单，不修改角色、套餐、应用或用户关系表。停用菜单、组合应用、源应用均不进入新增范围。
- `/system/user/getInfo` 读取当前租户服务端权限并更新 token-session 中的菜单、角色、角色 DTO 和数据权限快照；撤销权限采用替换而非旧值合并。该同步发生于用户信息刷新，不宣称实现了所有在线 token 的即时权限推送。
- SOP 加载失败捕获 Promise、展示可重试错误、清空旧列表，不再将失败显示成“暂无 SOP”。重试仍调用后端鉴权接口。
- SOP Drawer 使用 `size` 替代已弃用的 `width`，保持原有宽度表达式。

## 自动验证

- Maven 显式使用 `-Dmaven.test.skip=false -DskipTests=false`，避免仓库默认跳过测试。
- system 及依赖模块构建成功；`TenantPackageMenuScopeTest` 4 项、`SysLoginPermissionServiceTest` 3 项、`RemoteRoleMappingTest` 3 项通过，共 10 项，无跳过。
- 农业 SOP 专项 Vitest 3 项通过；`vue-tsc --noEmit --skipLibCheck` 与 Vite production 构建通过。未重新构建未修改的大屏子包，也不是全仓测试验收。
- 先在仍拒绝权限的环境实测前端：可见错误反馈，控制台不再出现未处理 Promise 或 Drawer width 弃用警告。
- 最终版本 11:20:32 实测 SOP 已通过权限：农业日志执行 `SfStaskSopMapper.selectList_mpCount`，SQL 明确限定 `tenant_id='916085'`。该租户当前为 0 条 SOP，浏览器正常显示空列表，无权限错误提示。
- 浏览器打开“新增 SOP”抽屉，农事分类、农事项、作物范围、语言和 TinyMCE 正文表单正常挂载；error 级日志为空。仅打开并关闭，未保存业务数据。
- 同一租户补查组合应用入库单页面：查询正常完成，当前 0 条记录，error 级控制台日志为空。未创建测试单据。

## 部署

- 仅重建 `ym-system`；保留既有 Docker 配置及 768MiB 内存上限（805306368 字节）。农业、IoT、Auth、DBO 未重启。
- 更新前镜像保留为 `ym/ym-system:before-sop-session-refresh-20260904`。
- 最终镜像：`sha256:06de68ef81d462ce03e1aba7414082e6679e557974876c2af720299c8abf5967`。
- 不涉及生产切换、历史数据修改或设备控制。
