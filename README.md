# EasyMock

轻量级 Spring Boot Mock 测试工具。通过 AOP 拦截 Controller 接口调用，在不修改业务代码的前提下返回预设的 Mock 数据，适用于开发联调、前后端对接、集成测试等场景。

## 特性

- **零侵入**：以 Spring Boot Starter 形式引入，配置开启即可使用，无需修改业务代码
- **多注解支持**：自动识别 `@PostMapping`、`@GetMapping`、`@RequestMapping`、JAX-RS `@Path` 以及自定义 `@MockApi` 注解
- **两级匹配规则**：
  - **URL 级别**：匹配请求路径即返回 Mock 数据
  - **参数级别**：基于 JsonPath 表达式匹配请求体参数，支持多条件组，每组独立返回不同 Mock 响应
- **可视化管理**：内置 Web 管理控制台，支持规则的增删改查、启用/禁用、导入/导出
- **独立存储**：使用独立的 H2 嵌入式数据库，不干扰宿主应用的数据源
- **环境安全**：仅在指定 Profile 下生效（默认 `local`、`dev`），生产环境不会激活

## 项目结构

```
EasyMock/
├── pom.xml                              # 父 POM（Maven 多模块）
├── easymock-spring-boot-starter/        # 核心 Starter 模块
│   ├── src/main/java/com/aiwishcoding/easymock/
│   │   ├── annotation/                  # @MockApi 自定义注解
│   │   ├── aop/                         # AOP 切面（Mock 拦截核心逻辑）
│   │   ├── autoconfigure/               # Spring Boot 自动配置
│   │   ├── controller/                  # Web 管理页面 & REST API
│   │   ├── model/                       # 数据模型（MockRule, ConditionGroup 等）
│   │   ├── repository/                  # Spring Data JPA 仓储
│   │   ├── scanner/                     # 接口端点自动扫描
│   │   ├── service/                     # 业务逻辑层
│   │   └── util/                        # JsonPath 条件评估器
│   └── src/main/resources/
│       ├── META-INF/spring/               # Spring Boot 4 自动配置注册
│       │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│       ├── templates/easymock/          # Thymeleaf 页面模板
│       └── static/easymock/             # CSS/JS 静态资源
└── easymock-test/                       # 示例/测试应用
    └── src/main/java/com/aiwishcoding/easymock/test/ # 演示 Controller 和 Service
```

## 快速开始

### 1. 编译安装

```bash
cd EasyMock
mvn clean install -DskipTests
```

> **从旧版本升级**：Spring Boot 4.2 使用 H2 2.5，可能无法直接打开旧版 H2 创建的数据库文件。升级前请备份 `easymock.db-path` 对应的数据库；如需保留已有规则，应使用 H2 的迁移工具先导出并导入，或临时指定新的 `easymock.db-path` 启动。

### 2. 在项目中引入依赖

在目标 Spring Boot 项目的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.aiwishcoding</groupId>
    <artifactId>easymock-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 3. 配置启用

在 `application.yml` 中添加：

```yaml
spring:
  profiles:
    active: local  # 未激活 Profile 或 Profile 不在允许列表时，EasyMock 完全不加载

easymock:
  enabled: true
  admin-token: ${EASYMOCK_ADMIN_TOKEN}  # 至少 16 个字符，建议通过环境变量注入
```

### 4. 访问管理控制台

启动应用后，浏览器访问：

```
http://localhost:8080/easymock
```

配置 `admin-token` 后，管理页面和 REST API 都需要认证。浏览器会弹出 Basic 认证对话框，用户名为 `easymock`，密码为配置的令牌；API 客户端也可使用 `Authorization: Bearer <token>` 或 `X-EasyMock-Token: <token>`。为避免浏览器跨站写入请求，Filter 会拒绝携带其他站点 `Origin` 的写操作。

未配置 `admin-token` 时保持兼容行为，不额外认证，此时应由宿主应用的安全框架保护管理路径。

## 运行示例应用

项目自带 `easymock-test` 示例模块：

```bash
cd EasyMock
mvn clean install -DskipTests
cd easymock-test
mvn spring-boot:run
```

访问 http://localhost:8080/easymock 即可体验完整功能。

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `easymock.enabled` | `false` | 是否启用 EasyMock |
| `easymock.allowed-profiles` | `local,dev` | 允许生效的 Spring Profile 列表 |
| `easymock.db-path` | `./data/easymock` | H2 数据库文件路径 |
| `easymock.page-path` | `/easymock` | Web 管理页面的 URL 路径 |
| `easymock.api-path` | `/easymock/api` | REST API 的 URL 路径 |
| `easymock.admin-token` | 未设置 | 管理端访问令牌；设置后启用 Basic/Bearer/请求头认证，至少 16 个字符 |

`page-path` 与 `api-path` 必须以 `/` 开头，不能以 `/` 结尾、不能包含 `*`，且页面路径不能位于 API 路径内部。默认的 `/easymock/api` 位于 `/easymock` 内部是允许的。

配置示例：

```yaml
easymock:
  enabled: true
  allowed-profiles: local,dev,test
  db-path: /opt/data/easymock
  page-path: /mock-admin
  api-path: /mock-admin/api
  admin-token: ${EASYMOCK_ADMIN_TOKEN}
```

## 匹配规则

### URL 级别匹配

最简单的模式——只要请求路径命中规则，直接返回预设的 Mock 响应。规则 URL 支持 Ant 风格路径变量，例如 `/api/items/{id}` 可匹配 `/api/items/42`。匹配时会先剥离应用的 Servlet Context Path，因此部署在 `/app` 时，外部请求 `/app/api/items/42` 仍可命中该规则。

### 参数级别匹配

在一条规则中可配置**多个条件组**，每组包含若干 JsonPath 条件和独立的 Mock 返回值。匹配时按顺序遍历条件组，首个满足的组决定返回值。

支持的条件操作符：

| 操作符 | 说明 |
|--------|------|
| `EQUALS` | 等于 |
| `NOT_EQUALS` | 不等于 |
| `CONTAINS` | 包含（字符串） |
| `GT` / `GTE` | 大于 / 大于等于 |
| `LT` / `LTE` | 小于 / 小于等于 |
| `REGEX` | 正则匹配 |
| `EXISTS` | 字段存在 |
| `NOT_EXISTS` | 字段不存在 |

JsonPath 示例：`$.orderId`、`$.data.user.level`、`$.items[0].price`

## 支持的注解

EasyMock 自动拦截以下注解标记的方法：

| 注解 | 来源 | 说明 |
|------|------|------|
| `@PostMapping` | Spring MVC | POST 请求处理方法 |
| `@GetMapping` | Spring MVC | GET 请求处理方法 |
| `@RequestMapping` | Spring MVC | 通用请求映射 |
| `@Path` | JAX-RS (`jakarta.ws.rs`) | JAX-RS 路径注解（可选依赖，自动检测） |
| `@MockApi` | EasyMock | 自定义注解，用于标记非 HTTP 方法为可 Mock |

### @MockApi 注解

用于将非 Controller 方法（如 Service 层调用外部接口的方法）纳入 Mock 管理：

```java
@Service
public class ExternalApiService {

    @MockApi("/api/external/notify")
    public Map<String, Object> sendNotification(Map<String, Object> request) {
        // 真实实现：调用外部 HTTP API
        // 配置 Mock 规则后，该方法可被拦截返回 Mock 数据
    }
}
```

## 管理控制台功能

- **规则列表**：查看所有 Mock 规则，按接口分组筛选
- **新建/编辑规则**：可视化配置规则名称、匹配路径、匹配类型、条件组、Mock 返回值、HTTP 状态码和启用状态
- **启用/禁用**：快速切换规则的激活状态
- **优先级**：同一接口多条规则时，按优先级排序匹配
- **导入/导出**：支持 JSON 格式的规则批量导入导出，便于团队共享

## REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/easymock/api/rules` | 获取所有规则 |
| POST | `/easymock/api/rules` | 创建规则 |
| PUT | `/easymock/api/rules/{id}` | 更新规则 |
| DELETE | `/easymock/api/rules/{id}` | 删除规则 |
| PUT | `/easymock/api/rules/{id}/toggle` | 切换启用状态 |
| GET | `/easymock/api/endpoints` | 扫描当前应用所有可 Mock 接口 |
| GET | `/easymock/api/export` | 导出规则（可选 `?url=` 按接口过滤） |
| POST | `/easymock/api/import` | 导入规则（multipart file） |

创建、更新和导入会校验 JSON 响应、JsonPath、操作符、正则表达式、HTTP 状态码及条件组结构；非法请求返回 `400`，不存在的规则返回 `404`。错误响应统一为 `{"success":false,"message":"..."}`。

## 测试

```bash
# Java 单元测试与示例应用集成测试
mvn test

# 管理端 JavaScript 测试（需要 Node.js 18+）
node --test easymock-spring-boot-starter/src/test/js/easymock.test.js
```

## 技术栈

- Java 17+
- Spring Boot 4.2.x（Spring Framework 7）
- Jakarta EE API
- Jackson 3
- Spring AOP
- Spring Data JPA + H2 Database
- Thymeleaf 3 + Bootstrap 5
- Jayway JsonPath
- CodeMirror（JSON 编辑器）

## 设计要点

1. **独立数据源**：EasyMock 使用独立的 DataSource、EntityManagerFactory 和 TransactionManager，命名隔离（`easymockDataSource`、`easymockEntityManagerFactory`、`easymockTransactionManager`），不影响宿主应用的数据库配置。

2. **Profile 安全保护**：自动配置与 AOP 切面均要求当前应用明确激活了允许的 Profile；未激活 Profile 或未命中允许列表时不会加载管理端，也不会执行 Mock。

3. **自动配置**：通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册，只需引入 JAR + 配置 `easymock.enabled=true`，无需 `@Import` 或包扫描配置。

4. **JAX-RS 可选支持**：`jakarta.ws.rs-api` 声明为 `<optional>true</optional>`，仅当宿主应用有 JAX-RS 依赖时才激活 `@Path` 拦截（通过 `@ConditionalOnClass` 实现）。

## 版本

当前版本：**1.0.0-SNAPSHOT**

## License

内部工具，仅供项目组使用。
