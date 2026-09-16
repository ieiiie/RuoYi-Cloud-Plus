# 2026-09-04 业务表格统一与入库详情卡死修复

## 本轮范围

- 农业、IoT 两个微应用；以人员管理的 VxePager 为分页基准。
- 只修改目标前端及本报告，不修改源 ym-vue、数据库菜单/套餐/授权、后端服务配置。
- 不是全量迁移验收；未执行库存创建、纠错保存、作废或删除。

## 修复与接入

1. 入库详情卡死根因：antdv-next 的 TableColumn 默认插槽用于子列解析，不是旧版行渲染插槽。旧代码解构 record 时连续抛异常。将历史纠错、新建单据、单据详情中的列改为 TableColumn 声明 + Table bodyCell 渲染；保留原值、申请/实际/系统数量、库存影响和行操作。
2. 两个应用新增 components/business-table：
   - BusinessTable 保留 Ant Table 的业务列、插槽、行选择、排序与过滤；隐藏内置分页，使用实际 VxePager。
   - BusinessPagination 兼容既有 v-model、change、showSizeChange，先更新页码/条数，再触发查询。
   - 与人员管理共享分页布局、默认 10 条和 10/20/30 选项。已有非标准当前条数保留为可见选项，以兼容旧深链接/页面初值。
   - 页大小变化遵循人员管理现有行为：保留有效页码，超过新页数时回退至最后一页；空数据页码至少 1。
   - 服务端分页不再次截取数据；本地分页继续由 Ant Table 处理。分页变化不额外制造重复回调。
   - 统一表头、行高、边框、对齐、悬浮样式与底部分页分布，排除隐藏测量行，防止表头下方空白行。
   - 初始化 Vxe 中文/英文和主题，修复直接打开库存页时出现 vxe.pager.* 文案。
3. 共接入 41 个业务页面/子组件（农业 34、IoT 7），既有 VxeGrid 复用同一分页常量。非分页详情表仍不分页；页面自身权限和操作未改变。

## 自动化验证

- 农业：vue-tsc 通过；本轮修改文件 ESLint 无错误，公共组件最终 lint 无警告；37 个测试文件、315 项测试通过；生产构建通过。
- IoT：vue-tsc 通过；本轮修改文件 ESLint 无错误，公共组件最终 lint 无警告；29 个测试文件、219 项测试通过；生产构建通过。
- 新增：两端各 7 个真实表格渲染/分页兼容测试；农业额外 3 个实际抽屉模板编译与列覆盖回归测试。
- Vitest 配置内联转换 antdv-next/@v-c 的 ESM 依赖，使真实 UI 组件测试能够运行。
- 原有 use-sortable mock 提升警告仍存在；构建的大包体积警告未作为本轮修复范围。
- 最终执行日志：/tmp/ym-agriculture-app-table-{types,component-lint,full-tests,build}.log 及 IoT 同名日志。

## 浏览器证据与未完成边界

- 在 5666 SaaS 无界嵌入模式复现原入库详情错误，修改后已成功展示物资、规格、单位、数量、纠错数量和关闭按钮。
- 新增入库抽屉成功打开，物资选择、数量输入与明细行可见；未提交单据。
- 入库列表已截图确认共享分页（总数、条数选择、首页/跳页/上一页/页码/下一页/末页）和统一行样式。
- 复测期间出现临时主应用 403，从工作台授权应用入口重新进入后恢复；未改变授权。
- 随后后端发生不可用：Gateway Nacos 连接错误，后端容器运行时间重新计时，最终 docker ps 无运行容器，docker ps -a 显示相关服务/基础设施退出（137、143、0 等）。本轮未操作容器启动/停止，不能仅据退出码断定原因。
- 后端中断后详情请求返回内部服务器错误；这与已复现并修复的前端 TableColumn 渲染错误不同。
- 因此尚未完成：后端恢复后的全部页面真实翻页、接口联调、所有详情分支逐页操作验收；IoT 未完成本轮逐页浏览器验收。不能将单测/构建通过视为这些检查通过。

## 原值显示补充验证

- 用户反馈历史纠错原值空白后，重新打开最新页面验证单据 RK2026082000002：原值 1.0、修改值 1.0、预计库存差额 0.0 均可见。此次未提交纠错，也未修改库存数据。
- 现有业务模板已包含 originalQuantity 渲染分支，本次新增 original-quantity.test.ts，编译实际纠错 Table 模板并挂载真实表格，覆盖数值 1、0、字符串 1.0、12.3；修改编辑值后原值不变。
- 新增 4 项测试、类型检查和测试文件 ESLint 通过。仅新增回归测试，没有再次改动业务逻辑；本补充不扩大前述逐页验收范围。

## 本轮接入文件

- /media/admin/extra/ym-project/ym-agriculture-app/src/views/agriculture/satellites/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/agriculture/uav/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/batch/batch-detail-panel.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/farming/farm-records/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/field/ai-inference/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/field/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/inspection-photo-archive/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/inventory/assets/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/inventory/balance/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/inventory/materials/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/inventory/shared/document-create-drawer.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/inventory/shared/historical-correction-drawer.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/inventory/shared/ledger-drawer.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/inventory/shared/order-detail-drawer.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/inventory/shared/order-list-page.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/market-quote/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/news-review/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/satellite/components/satellite-sensing-result-panel.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/satellite/components/satellite-task-detail-drawer.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/satellite/health/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/species/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/species/variety-by-species/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/stask/farm-assign/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/stask/farm-work/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/stask/sop/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/stask/task-list/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/stask/task-list/package-detail-drawer.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/stask/task-list/task-detail-drawer.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/uav/bindings/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/uav/devices/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/uav/flight-plans/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/uav/jobs/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/smartfarming/uav-task/index.vue
- /media/admin/extra/ym-project/ym-agriculture-app/src/views/system/employee/profile/worker-skills-panel.vue
- /media/admin/extra/ym-project/ym-iot-app/src/views/agriculture/camera/index.vue
- /media/admin/extra/ym-project/ym-iot-app/src/views/agriculture/iot/index.vue
- /media/admin/extra/ym-project/ym-iot-app/src/views/iot/device/device-detail-logs.vue
- /media/admin/extra/ym-project/ym-iot-app/src/views/iot/device/device-detail-tags.vue
- /media/admin/extra/ym-project/ym-iot-app/src/views/iot/device/device-pest-data-modal.vue
- /media/admin/extra/ym-project/ym-iot-app/src/views/iot/motorvalve/index.vue
- /media/admin/extra/ym-project/ym-iot-app/src/views/iot/product/product-detail-drawer.vue
