# 6.0.0 多租户升级

本次改造采用“共享库、共享表、`tenant_id` 行级隔离”模式，与 RuoYi-Cloud-Plus 2.X 的核心模型一致。平台全局表（菜单、租户、套餐、客户端、OSS 配置及用户/角色关联表）不参与行级拦截；用户、角色、部门、字典、参数、日志、消息和 OSS 数据按租户隔离。

## 升级顺序

1. 停止服务，并备份业务数据库与 Redis。
2. 保持 Nacos `application-common.yml` 的 `tenant.enable: false`。
3. 按数据库类型执行对应脚本：

   - MySQL：[update_6.0.0-tenant.sql](update_6.0.0-tenant.sql)
   - PostgreSQL：[postgres/update_6.0.0-tenant.sql](postgres/update_6.0.0-tenant.sql)
   - Oracle：[oracle/update_6.0.0-tenant.sql](oracle/update_6.0.0-tenant.sql)

   现有数据会写入默认租户 `000000`。
4. 在 Nacos 将 `tenant.enable` 改为 `true`，刷新配置并重启 gateway、auth、system、resource 等全部服务。
5. 登录、验证码和注册请求需携带 `tenantId`；原系统管理员使用 `tenantId: "000000"`。

租户创建入口为 `POST /tenant`，套餐管理入口为 `/tenant/package`。两类接口只允许默认租户中拥有 `superadmin` 角色的用户访问；新租户会自动生成根部门、租户管理员账号、`tenant_admin` 角色，并复制默认租户的字典和参数配置。

> 该目录中的 MySQL 脚本是一次性版本迁移脚本。若数据库已经手工添加过部分字段，请不要直接重复执行；应先按实际表结构比对后补齐缺失 DDL。
