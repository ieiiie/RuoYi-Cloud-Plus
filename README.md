# YM-Cloud-Plus

YM-Cloud-Plus 是面向 SaaS 业务的 Java 21 微服务底座，提供认证、网关、系统管理、内部运营平台、全局账号与浏览器级租户切换能力。

## 当前服务

| 服务 | Maven 坐标 | 默认端口 | 说明 |
| --- | --- | ---: | --- |
| 网关 | `com.ym:ym-gateway` | 8080 | 统一路由与边界校验 |
| 认证中心 | `com.ym:ym-auth` | 9210 | 多方式登录、全局账号和租户会话 |
| 系统服务 | `com.ym:ym-system` | 9201 | SaaS 租户、权限、文件与基础管理 |
| 运营平台 | `com.ym:ym-dbo` | 9203 | 独立账号体系的内部运营服务 |

API 契约位于 `ym-api`，公共基础能力位于 `ym-common`，两者不作为独立服务部署。

## 基础环境

- JDK 21
- Spring Boot 4.1
- Spring Cloud 2025.1
- Nacos 3
- Apache Dubbo 3
- Apache Seata 2
- MySQL 8
- Redis 8

Nacos 中的活动配置为 `application-common.yml`、`ym-gateway.yml`、`ym-auth.yml`、`ym-system.yml` 和 `ym-dbo.yml`。

## 构建

```bash
./mvnw clean package -DskipTests
```

各服务 Dockerfile 均使用 `bellsoft/liberica-openjdk-rocky:21.0.12-cds`，构建产物和镜像统一使用 `ym-*` 命名。

## 数据与接口兼容

本次品牌与技术命名调整不改变 `ry-cloud`、`ry_dbo`、`ry-seata` 数据库，不改变 `sys_*` 表名，也不改变现有 HTTP 路径与 JSON 契约。

## 上游与开源声明

本项目基于 [Dromara RuoYi-Cloud-Plus](https://github.com/dromara/RuoYi-Cloud-Plus) 的 MIT 许可代码持续演进。上游版权和许可声明保留在 [LICENSE](LICENSE) 中。
