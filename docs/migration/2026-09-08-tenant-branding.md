# SaaS 主布局使用租户名称与 Logo

## 范围与数据来源

- SaaS 主布局左上角读取当前租户名称及 `ry-cloud.sys_tenant.logo_url`。
- Logo 仍由 DBO 租户管理维护；本次未修改数据库数据、Nacos 配置或 DBO 品牌。
- 组合应用显式传入的标题保留，不被租户名称覆盖。
- 未配置 Logo 的租户使用默认图标；切换时不会保留上一个租户的 Logo。

## 实现

- `SysTenant`、`SysTenantVo`、`RemoteTenantUserVo`、`TenantLoginVo` 增加 `logoUrl`。
- `LoginUser` 增加可空 `tenantLogoUrl`，已有会话无该字段时允许默认展示。
- 普通成员和平台管理员的租户列表、登录及切换租户响应均透传 Logo。
- bell-plus 的 `src/layouts/tenant-branding.ts` 根据响应式 `currentTenant` 计算品牌，不修改全局偏好或新增前端管理权限请求。
- `src/layouts/basic/layout.vue` 在主 Logo 和侧边扩展标题位置应用品牌数据。
- 当前 antdv-next Avatar 不支持原 `image-style` 属性，`src/core/ui/adapter/avatar.vue` 改为局部样式应用 `fit`，企业 Logo 使用 `contain` 完整显示。

## 验证结果

- 后端定向 Maven 构建成功，`TenantBrandingTest` 3 项通过。
- 前端品牌解析 Vitest 4 项通过。
- bell-plus `vue-tsc --noEmit`、修改文件 ESLint、生产构建通过。
- 浏览器实测默认管理租户名称替换 Plus Admin。
- 切换到已有成员的新疆农牧投联鸿(测试)租户后，名称同步变化，Logo 图片实际加载成功，计算样式为 `object-fit: contain`。
- 刷新后仍保持该租户名称与 Logo；最后切回默认管理租户，恢复默认图标，无旧 Logo 残留。
- 未执行重新输入账号密码的登录流程；登录响应字段映射由定向单元测试覆盖。

## 运行环境

- SaaS 开发服务已恢复在 5666 端口。
- 仅重建并更新 `ym-system`、`ym-auth`，两个容器健康检查均为 healthy。
- 已保留回滚镜像标签 `ym/ym-system:before-tenant-branding-20260908` 与 `ym/ym-auth:before-tenant-branding-20260908`。
- 未提交或推送代码；保留工作区原有其他改动。
