# 农业微应用登录页迁移

## 范围与结果

来源：`ym-vue/apps/web-antd` 当前工作区的登录页（源仓库未修改）。
目标：`ym-agriculture-app`。本次仅迁移登录展示，不调整认证接口、权限、租户或主应用回调协议；不影响 DBO、IoT 登录页及农业业务页面品牌配置。

- 复制完整 `app-logo-lianhong.png` 与农业插画 `login-slogan.png`，两张图片的 SHA-256 均与源文件一致。
- 使用 Vite `BASE_URL` 解析图片资源，支持同域短入口和独立微应用入口，避免向 SaaS 根目录请求图片。
- 迁移宣传语、等比例插画、完整 Logo（不额外重复品牌文字）与页脚版权。
- 保留左/中/右登录布局及明暗主题、语言工具栏，按源页面隐藏第三方登录入口。
- 保留目标项目的 Antdv Next 表单、验证码、账号记忆、校验、认证 Store 和失败刷新；不引入旧 Vben 表单内核或旧租户认证链路。

## 验证

- Vitest：4 个测试文件、11 项通过，覆盖登录展示、账号记忆、验证码参数、失败刷新、必填校验、AUTH/独立模式 ClientID 以及嵌入式布局/路由。
- `pnpm typecheck` 通过。
- 本次修改的 Vue/TypeScript 文件 ESLint 检查通过。
- `pnpm exec vite build --mode production` 通过，生成农业微应用 `dist` / `dist.zip`；未重建无关大屏源码。
- 浏览器实际检查 `/agriculture` 与 `:5670/micro-apps/agriculture/auth/login`：Logo、插画、宣传语、表单和版权均正常显示。
- 真实验证码未填写、真实登录未提交；实际账号登录后回跳与跨应用登录成功不作为本次人工验收结论，原有实现未改变。

## 主要文件（相对 ym-agriculture-app）

- `src/layouts/auth.vue`
- `src/layouts/authentication/authentication.vue`
- `src/layouts/authentication/form.vue`
- `src/views/_core/authentication/login.vue`
- `src/views/_core/authentication/login.test.ts`
- `src/locales/langs/{zh-CN,en-US}/authentication.json`
- `public/{app-logo-lianhong,login-slogan}.png`
