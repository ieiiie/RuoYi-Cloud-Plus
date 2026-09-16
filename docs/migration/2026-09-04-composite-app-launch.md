# 组合应用入口无法打开修复

## 原因与修复

SaaS 前端：`/media/admin/extra/框架/bell-plus`。

1. 窗口管理器先 `window.open('', name)` 再写入地址，内置浏览器未展示新窗口，但页面已标记“已打开”。现在直接传入同域目标 URL；弹窗返回 null 时退回当前页导航。新窗口状态由应用挂载/心跳确认，不再在调用 open 后提前标记。
2. 组合应用直接访问时自动转到 `/composite-apps/test/agriculture/workspace` 后显示 403。宿主 `context.menus` 与授权菜单使用同一引用，子应用 `backMenuToVbenMenu` 原地改写了菜单路径。现改为深拷贝菜单后传给子应用，保护宿主授权树，保留严格深链授权匹配。
3. 组合菜单忽略后端为顶级页面生成的无 meta、单子节点、`/` Layout 包装，避免把纯数字 route name 显示成业务目录。真实目录和页面仍保留。

修改：`src/micro-app/context.ts`、`window-manager.ts`、`composite.ts` 及对应测试。
未修改套餐、角色或后端授权数据；未修改农业和 IoT 源页面。

## 验证

- `pnpm exec vitest run src/micro-app`：4 文件、16 测试全部通过。
- `pnpm run typecheck`：通过。
- `pnpm run build`：成功生成生产包。
- 当前 5666 开发服务已热更新，无需重启后端。
- 使用租户 `SaaS联动测试租户_20260828`，实际点击工作台 `test / 打开应用` 创建了浏览器新标签页。
- 新标签页加载农业工作台；人员管理页面加载搜索、表格和分页；切换 IoT 产品页面加载搜索、产品列表和分页，未再出现 403。
- 该租户上述列表为空；农业工作台另提示未配置天地图 Key。本次未补第三方凭据，也未将空列表验证视为业务写操作通过。
