# 应用短入口与统一登录（测试环境）

## 入口约定

- SaaS：`http://127.0.0.1:5666/`
- 农业：`http://127.0.0.1:5666/agriculture`
- 物联网：`http://127.0.0.1:5666/iot`
- 组合应用：`http://127.0.0.1:5666/test`
- 农业深链接：`/agriculture/organization/employee`
- 组合深链接：`/test/agriculture/organization/employee`
- 旧 `/apps/*`、`/composite-apps/*` 已移除，不提供别名或重定向。
- `/micro-apps/agriculture/`、`/micro-apps/iot/` 仍是内部资源及开发独立服务 base，不是本次统一会话入口。

## 实现

1. 主应用静态注册短入口，匿名时读取 `/system/app/public/{appKey}`，用无界 AUTH 模式挂载对应子应用的真实登录页面。地址保持原始深链接，不跳 SaaS 登录页。
2. 公开接口仅返回 `appKey/appName/appType/loginTheme`，只允许已启用的 MICRO/COMPOSITE 应用；系统保留字、未知应用、未配置可信登录页的应用均拒绝。没有公开菜单、租户、权限、令牌或任意外部资源 URL。
3. 子应用登录请求使用宿主 ClientID；登录结果携带服务器返回的 ClientID 回传宿主，由宿主重新请求用户和租户信息验证后持久化既有会话，完整重载原应用地址。没有引入新的 SSO 服务，没有修改 Sa-Token ClientID 校验。
4. 同源 SaaS 窗口通过不含令牌的 storage 通知重载既有持久化会话。登录后业务菜单仍来自 `/system/app/authorized`；授权接口失败不再回退到旧菜单推导授权。
5. 组合菜单、面包屑和标签页 URL 改为根短路径。微应用路由就绪后才向宿主同步路径，避免刷新深链接被启动时临时 `/` 覆盖。组合应用继续按授权菜单校验路径，并保留查询参数和 hash。
6. 登录、组合壳和独立壳使用不同偏好命名空间；SELF 模式恢复侧栏、顶栏、面包屑和标签页，避免组合应用的 full-content 设置污染独立应用。
   子应用侧栏改用菜单 click 事件，确保在详情页点击仍处于选中状态的列表菜单也会导航；微应用启动 URL 固定，后续路由变化通过导航总线同步，不反复激活无界实例。
7. DBO 仅增加应用管理的“登录页面”配置项，登录/会话体系不参与共享。

## 数据与部署

- 执行 `script/sql/20260904-app-login-theme.sql`，新增 `sys_app.login_theme`；农业/iot 对应自己的页面，既有 `test` 组合应用使用农业登录页。
- 未修改角色、套餐、菜单授权关系。其他未配置登录页的应用需要在 DBO 应用管理中选择登录页面；不会自动赋予权限。
- Nacos `ym-gateway.yml` 仅增添 `/system/app/public/*` 匿名白名单，并读取回验；没有开放 `/system/app/**`。
- 原线上测试配置已备份在 `/tmp/ym-gateway-before-app-entry-20260904.yml`（0600，可能包含敏感配置，不入库）。
- 测试环境 System、DBO 已重新构建并重启，健康检查通过；Gateway 使用动态白名单，无需全量重启。
- 生产沿用 Nginx 根路由 `try_files ... /index.html` 和微应用资源 fallback；部署时配置正确 API 上游、域名及 HTTPS。本次未发布生产。

## 验证记录

- System/DBO Maven package 通过；新增后端策略测试 3 项通过。
- SaaS 主应用相关回归 46 项通过；农业 16 项、IoT 16 项通过（授权范围、嵌入路由、ClientID、外壳偏好），加后端共 81 项。
- 四个前端 typecheck、生产构建通过；定向 lint 无错误。两个子应用菜单组件各保留一条原有 `vue/no-required-prop-with-default` 警告。
- 匿名读取 agriculture/iot/test 登录配置成功；匿名 `/system/app/authorized` 返回 401；CORE、保留字及不存在应用均拒绝。
- 浏览器已观察农业、IoT 自有匿名登录页；会话建立后农业进入 `/agriculture/dashboard`，已打开 SaaS 与 IoT 窗口分别进入 `/dashboard` 与 `/iot/dashboard`。
- 农业独立壳与组合壳的侧栏、顶栏、标签页已恢复；人员列表加载成功，短深链接直接访问和刷新保持在人员管理。
- 旧 `/apps/agriculture` 浏览器显示“页面不存在”，没有重定向。
- 人员编辑页在短地址下正常显示，`name/mode` 查询参数保留；只打开并取消编辑，没有保存。通过人员管理菜单返回列表。
- URL 边界测试覆盖双斜杠、编码斜杠、反斜杠和上级目录，禁止无界加载应用资源目录之外的地址；正常查询参数中的编码斜杠仍保留。

## 验证边界

- 本次没有提交业务表单、删除业务数据或操作真实设备。
- 没有退出用户当前登录来重复测试全窗口登出，也未用普通角色执行完整租户切换/权限矩阵回归；相关授权仍由现有服务端控制。
- 不承诺直接访问不同端口 5670/5671 的独立开发站点可跨源共享登录；统一入口必须使用同一域名、协议和端口。
- OAuth/短信等第三方流程及生产域名部署未实测。
