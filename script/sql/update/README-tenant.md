# 6.0.0 多租户升级

本次改造采用“共享库、共享表、`tenant_id` 行级隔离”模式，与 RuoYi-Cloud-Plus 2.X 的核心模型一致。平台全局表（菜单、租户、套餐、客户端、OSS 配置及用户/角色关联表）不参与行级拦截；用户、角色、部门、字典、参数、日志、消息和 OSS 数据按租户隔离。

## 升级顺序

1. 停止服务，并备份业务数据库与 Redis。
2. 保持 Nacos `application-common.yml` 的 `tenant.enable: false`。
3. 已有数据库按数据库类型执行对应脚本：

   - MySQL：[update_6.0.0-tenant.sql](update_6.0.0-tenant.sql)
   - PostgreSQL：[postgres/update_6.0.0-tenant.sql](postgres/update_6.0.0-tenant.sql)
   - Oracle：[oracle/update_6.0.0-tenant.sql](oracle/update_6.0.0-tenant.sql)

   然后执行全局账号升级脚本：

   - MySQL：[update_6.0.1-global-user.sql](update_6.0.1-global-user.sql)
   - PostgreSQL：[postgres/update_6.0.1-global-user.sql](postgres/update_6.0.1-global-user.sql)
   - Oracle：[oracle/update_6.0.1-global-user.sql](oracle/update_6.0.1-global-user.sql)

   6.0.0 会将现有数据写入默认租户 `000000`。6.0.1 不迁移既有用户为
   全局账号；本项目当前按“无历史数据库”交付，会移除 `sys_user.user_name`、
   `sys_user.phone_number`、`sys_user.password`。新建账号和新加入的租户成员会由
   应用自动写入 `sys_global_user` 与 `sys_user.global_user_id`，认证资料只保存在
   `sys_global_user`。

   新建 MySQL 数据库直接导入 `ry-cloud.sql`：该完整初始化脚本已经合并 6.0.0
   多租户、6.0.1 全局账号、6.0.2 全局字典、6.0.3 菜单清理和 6.0.4 Nacos 控制台
   菜单清理，不要再执行升级脚本。

   已执行 6.0.0 的 MySQL 数据库还需执行全局字典升级脚本：

   - MySQL：[update_6.0.2-global-dict.sql](update_6.0.2-global-dict.sql)

   6.0.2 会将 `sys_dict_type`、`sys_dict_data` 恢复为平台全局表，移除两张表的
   `tenant_id`。脚本仅接受两张表的数据都属于默认租户 `000000`；检测到其他租户
   数据会中止执行，不会自动合并字典数据。执行后可通过“全局字典”的刷新缓存接口
   清理新的全局缓存，旧租户前缀字典缓存不再被应用读取，可在 Redis 运维窗口清理。

   已有 MySQL 数据库还需执行菜单清理脚本：

   - MySQL：[update_6.0.3-remove-system-tool-plus-menu.sql](update_6.0.3-remove-system-tool-plus-menu.sql)

   6.0.3 会删除“系统工具”和“PLUS官网”菜单及其子菜单，并同步清理角色授权与
   租户套餐中的菜单引用。

   已有 MySQL 数据库还需执行 Nacos 控制台菜单清理脚本：

   - MySQL：[update_6.0.4-remove-nacos-console-menu.sql](update_6.0.4-remove-nacos-console-menu.sql)

   6.0.4 仅删除“系统监控”下的“Nacos控制台”导航入口，并同步清理角色授权与
   租户套餐中的菜单引用；不会停止或删除 Nacos 服务。

   PostgreSQL、Oracle 新建数据库仍可先导入对应全量脚本
   （`postgres_ry_cloud.sql`、`oracle_ry_cloud.sql`），再只执行 6.0.0 租户脚本；
   此路径不需要重复执行 6.0.1。
4. 在 Nacos 将 `tenant.enable` 改为 `true`，刷新配置并重启 gateway、auth、system、resource 等全部服务。
5. 登录、图形验证码、短信验证码（`/resource/sms/code`）和邮箱验证码
   （`/resource/email/code`）不再携带 `tenantId`，系统会按成员创建时间自动进入
   第一个有效租户；注册仍必须携带目标 `tenantId`。登录后可通过
   `GET /auth/tenant/list` 查询可进入租户，并通过 `PUT /auth/tenant/{tenantId}`
   在当前浏览器 token 会话内切换且不会换发 token。客户端收到切换成功响应后应
   重新加载当前用户信息、菜单和路由。

本次仅交付后端接口契约。社交登录和小程序登录已按全局绑定表定位账号，外部平台
参数与真实回调需在部署环境配置；当前不把该部分视为完成外部平台联调。

租户创建入口为 `POST /tenant`，套餐管理入口为 `/tenant/package`。两类接口只允许默认租户中拥有 `superadmin` 角色的用户访问；新租户会自动生成根部门、租户管理员账号、`tenant_admin` 角色，并仅复制默认租户的参数配置。全局字典由拥有 `system:dict:*` 权限的角色共同维护，修改会影响所有租户。

> 该目录中的 MySQL 脚本是一次性版本迁移脚本。若数据库已经手工添加过部分字段，请不要直接重复执行；应先按实际表结构比对后补齐缺失 DDL。
