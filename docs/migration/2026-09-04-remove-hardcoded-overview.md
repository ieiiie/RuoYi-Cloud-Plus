# 删除农业硬编码首页

## 已执行

- 删除农业微应用 `src/views/dashboard/home-overview` 下 19 个页面、图表、地图、样式及 mock 数据文件。
- 将只被该页引用的 `public/vendor/maptalks` 静态副本移出发布目录（约 12 MiB）。
- 保留独立大屏使用的 Maptalks 依赖及组件；保留正常地块地图、卫星/无人机页面。源 ym-vue 未修改。
- 执行 `script/sql/migration/41-remove-hardcoded-agriculture-overview.sql`：删除 ry-cloud 中菜单 2059893469555458049（首页）及 1 条角色关系、3 条套餐关系、1 条组合应用关系；没有子菜单或角色模板关系。农业应用本身保留。
- 脚本带菜单身份、子菜单前置检查及事务；再次执行后五张表的目标记录计数仍均为 0。

## 验证

- 页面覆盖测试：4 项通过，有效农业组件从 41 项调整为 40 项，并断言不再发布 home-overview Vue 组件。
- `pnpm typecheck`：通过。
- `pnpm exec vite build --mode production`：通过；这是农业主应用构建，本次未重新构建未修改的独立大屏包。
- 浏览器：组合应用菜单中已无“首页”，工作台、数据大屏、智慧大棚大屏和其他业务目录保留。
- 原组合链接 `/composite-apps/test/agriculture/home-overview` 显示已失效菜单的 403 提示，不能继续呈现硬编码数据。
- 点击返回入口后的自动化快照遇到 Wujie getBoundingClientRect 错误，未据此宣称返回链路验证通过。

## 回退材料

本机临时目录 `/tmp/remove-agriculture-overview-MlIL7D`：

- `frontend.tar.gz`：删除前的 19 个源码文件、静态地图副本及原测试文件。
- `menu-restore.sql`：五张表中该菜单关联记录的完整 INSERT 备份。
- `maptalks/`：移出的静态资源原目录。

这些临时备份可能被系统清理，需长期保留时应另行归档。数据库回退应先核对目标 ID 是否被重新使用，不能盲目覆盖新增数据。
