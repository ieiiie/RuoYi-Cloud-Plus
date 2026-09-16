# 电动阀分页、抽屉与详情 404 修复

## 已完成

- ym-iot-app：分页去除重复摘要；抽屉改用 size=420 与 wrapper 最大宽度；日志失败不再中断初始化，并丢弃关闭后的旧响应。
- ym-iot：三个只读详情路由移至无 MQTT 条件的 Controller/Service；原命令 Service 委托同一查询实现。原有设备、租户隔离和控制权限保留。
- 新增 GET /motorvalve/capabilities，前端默认禁用指令、检查能力后再决定是否开放。未开启 MQTT。

## 验证

- MotorValveDetailQueryTest：7 tests，0 failures/errors/skips。
- 前端 detail.test.ts + business-table/index.test.ts：10 tests passed。
- IoT 前端 typecheck、定向 ESLint 和 Vite production build 通过；后端 package 通过。
- 仅更新测试环境 ym-iot，容器 healthy；现有网络、端口和数据卷保持不变。
- 浏览器 /test/iot/remotecontrol/motorvalve：真实历史日志两条可见；无新的详情请求地址不存在错误；抽屉内外面板均 420px、贴右显示；关闭返回列表；分页摘要不再竖排。
- 未执行读取、控制、批量控制、NTP 等任何硬件指令；未修改业务数据、套餐或授权。

## 回滚与边界

旧镜像：ym/ym-iot:before-motorvalve-fix-20260904-1538；修复镜像：ym/ym-iot:motorvalve-fix-20260904-1538。

未验证真实硬件控制、MQTT 开启后的联调、其他租户账号与移动端视口。

独立观察：启动日志中 Snail Job 注册服务返回 JDBC 连接失败；页面还存在 Modal bodyStyle / InputNumber addonAfter 的既有弃用警告。本次没有改动这些独立问题，也不将容器 healthy 等同于它们已修复。
