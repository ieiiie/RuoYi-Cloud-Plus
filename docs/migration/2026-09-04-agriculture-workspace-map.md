# 农业工作台天地图空白修复

## 原因

- 故障页面：`/test/agriculture/workspace`，农业直达入口 `/agriculture/workspace` 同样受影响。
- 浏览器实测地图容器约 551 × 907，容器存在但没有地图子元素，不是容器尺寸不足。
- 原加载器使用 `document.head.append(script)`；当前无界 2.1.0 在 `effect.js` 中拦截的是 `appendChild` / `insertBefore`，该插入方式绕过沙箱脚本处理。
- 原初始化在脚本 load 后发现子应用 `T.Map` 不存在便静默返回。仅增加等待也无法修复；切换到 `appendChild` 后实际渲染恢复。

## 修改

- 农业微应用 `src/utils/tianditu.ts`：用无界支持的 `appendChild` 插入官方 SDK，等待 `T.Map` 可用，兼容已有脚本，提供缺少 Key、脚本加载失败、超时错误，并清理计时器。
- `src/views/dashboard/workspace/index.vue`：复用加载器、显示失败原因，地图初始化与工作台数据获取并行；异步返回后不再为已卸载页面创建地图或启动轮询。
- 保留现有天地图二维卫星影像、注记和地块/传感器覆盖物逻辑，没有引入 Maptalks，也没有更改 Key、数据库、权限或后端接口。

## 验证

- 组合应用工作台已通过浏览器截图确认卫星底图及道路注记显示。
- 农业直达入口刷新后，地图 DOM 内 25 张图片全部加载成功。
- `vitest run src/utils/tianditu.test.ts`：6 项通过。
- `vue-tsc --noEmit --skipLibCheck`：通过。
- 定向 ESLint：通过。
- `vite build --mode production`：通过；未修改独立大屏源码，未重建独立大屏包。
- 原始 `ym-vue` 与 `ym-cloud` 仓库均保持干净。本次仅修改农业微应用 3 个源码/测试文件及本记录，未重启后端、未提交或推送。
- 当前截图的地图显示北京默认视野；这证明底图恢复，不等同于所有租户地块边界业务验收。天气显示“暂无天气数据”不属于本次地图故障修改范围。
