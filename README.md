# NovaMart

NovaMart 是一个基于 Java 与 Spring Boot 的商城后端项目，提供用户、商品、购物车和普通订单相关的 REST API。当前仓库包含后端源码和 MySQL 初始化脚本；没有前端工程。

## 功能与技术栈

源码中的控制器和服务覆盖以下业务：

- 用户注册、登录、刷新 Token、登出及当前用户信息。
- 在售商品分页查询、按关键字或分类筛选、分类列表和商品详情。
- 登录用户的购物车查询、添加、修改数量、删除、清空及合并。
- 从购物车提交普通订单、分页查看订单、查看详情，以及模拟支付、取消和确认收货。

项目配置中声明的主要技术包括 Java 17、Spring Boot 4.0.8、Spring MVC、Spring Security、MyBatis-Plus、PageHelper、MySQL、JWT、BCrypt、Redis，以及 RabbitMQ、Redisson 和 WebSocket 相关依赖。依赖或配置声明本身不代表相应业务流程都已实现；例如，当前可见源码没有秒杀业务控制器或服务。

## 环境准备

- JDK 17。
- MySQL，并允许应用连接到 `novamart` 数据库。
- Maven Wrapper 已包含在仓库中，无需单独安装 Maven。
- Redis、RabbitMQ 的连接参数也在 `src/main/resources/application.yml` 中配置；如需使用对应集成，请准备服务并配置可访问的地址和凭据。

建议在启动前通过环境变量覆盖配置中的开发默认值，尤其是数据库、Redis、RabbitMQ 凭据和 JWT 密钥。配置项如下：

| 环境变量 | 用途 |
| --- | --- |
| `SERVER_PORT` | HTTP 端口，默认 `8080` |
| `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` | MySQL 连接地址、用户名和密码 |
| `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`REDIS_DATABASE` | Redis 连接参数；当前配置也用 `REDIS_HOST` 作为 RabbitMQ 主机地址 |
| `RABBITMQ_PORT`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD`、`RABBITMQ_VHOST` | RabbitMQ 连接参数 |
| `JWT_SECRET` | Base64 编码的 JWT 密钥，解码后至少 256 bit |
| `CORS_ALLOWED_ORIGINS` | 允许跨域的来源列表，多个来源用逗号分隔 |

## 数据库初始化

脚本会创建 `novamart` 数据库及所需表，并插入示例商品和一条示例秒杀活动。使用 MySQL 客户端执行：

```bash
mysql --default-character-set=utf8mb4 -uroot -p < src/main/resources/db/schema.sql
```

在 Windows PowerShell 中可通过 `cmd` 执行重定向：

```powershell
cmd /c "mysql --default-character-set=utf8mb4 -uroot -p < src\main\resources\db\schema.sql"
```

如使用非默认数据库账号或连接信息，请相应调整 `DB_URL`、`DB_USERNAME` 和 `DB_PASSWORD`。

## 构建与启动

在仓库根目录运行。Windows PowerShell：

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/novamart?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<你的 MySQL 密码>"
.\mvnw.cmd spring-boot:run
```

macOS/Linux：

```bash
export DB_URL='jdbc:mysql://localhost:3306/novamart?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8'
export DB_USERNAME='root'
export DB_PASSWORD='<你的 MySQL 密码>'
./mvnw spring-boot:run
```

默认服务端口为 `8080`。运行测试可使用 `.\mvnw.cmd test`（Windows）或 `./mvnw test`（macOS/Linux）。

## 主要 API

所有接口均以 `/api` 为前缀；分页接口使用 `pageNum` 和 `pageSize` 参数。

| 路径 | 说明 |
| --- | --- |
| `/api/user/register`、`/api/user/login`、`/api/user/refresh` | 注册、登录和刷新 Token |
| `/api/user/logout`、`/api/user/me` | 登出和查询当前用户 |
| `/api/product/page`、`/api/product/categories`、`/api/product/{id}` | 商品分页、分类和详情 |
| `/api/cart/list`、`/api/cart/add`、`/api/cart/update`、`/api/cart/{productId}` | 购物车查询、添加、修改和删除 |
| `/api/cart`、`/api/cart/merge` | 清空购物车、合并购物车 |
| `/api/order/submit`、`/api/order/page`、`/api/order/{id}` | 提交订单、订单分页和详情 |
| `/api/order/{id}/pay`、`/api/order/{id}/cancel`、`/api/order/{id}/confirm` | 模拟支付、取消订单和确认收货 |

控制器注释将商品接口标为公开，将购物车和订单接口标为需登录；用户注册、登录和刷新接口为公开接口。所有接口的响应使用统一的 `ApiResult` 包装。

## 目录结构

```text
.
├── .mvn/wrapper/                 Maven Wrapper 配置
├── illustration/                 各版本的设计与实现说明
├── mvnw、mvnw.cmd                Maven Wrapper 启动脚本
├── pom.xml                       Maven 依赖和 Java 版本配置
└── src
    ├── main
    │   ├── java/com/example/novamart
    │   │   ├── common/           统一响应和异常处理
    │   │   ├── config/           Web、密码和 MyBatis 配置
    │   │   ├── controller/       REST API 控制器
    │   │   ├── dto/              请求 DTO
    │   │   ├── entity/           数据实体
    │   │   ├── mapper/           MyBatis-Plus Mapper
    │   │   ├── service/          业务服务
    │   │   ├── util/             工具类
    │   │   └── vo/               响应视图对象
    │   └── resources
    │       ├── application.yml   应用及中间件配置
    │       └── db/schema.sql     数据库初始化脚本
    └── test/java/                测试源码
```

