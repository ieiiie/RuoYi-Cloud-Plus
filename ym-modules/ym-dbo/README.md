# ym-dbo

`ym-dbo` 是无多租户的内部运营平台服务，使用独立的 Dbo 用户、角色、菜单和登录态。

## 数据边界

- `master` 数据源指向 `ry_dbo`，保存 Dbo 自身账号与系统管理数据。
- `saas` 数据源指向 `ry-cloud`，仅供 `/saas/**` 运营接口维护 SaaS 平台数据。
- Dbo 不加入 SaaS 全局账号体系，不经过租户拦截器。
- Redis 使用独立数据库及 `dbo:` Key 前缀，避免与 SaaS 会话互相覆盖。

## 初始化

MySQL 新库先执行 `src/main/resources/db/ry_dbo.sql`，再按文件名前缀顺序执行
`src/main/resources/db/update` 下的升级脚本。最终的 `005-remove-non-core-modules.sql`
会清理工作流、定时任务、代码生成、AI 和演示表及菜单。

## 访问方式

服务注册名为 `ym-dbo`，默认端口 `9203`。通过网关使用 `/dbo/**`，例如：

- `POST /dbo/auth/login`
- `GET /dbo/auth/code`
- `GET /dbo/system/user/getInfo`
- `GET /dbo/system/menu/getRouters`
- `/dbo/system/**`
- `/dbo/saas/**`

Nacos 配置为 `public / DEFAULT_GROUP / ym-dbo.yml`。
