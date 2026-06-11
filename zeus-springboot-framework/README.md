# Zeus Spring Boot Framework

Zeus Spring Boot Framework 是一组面向 Spring Boot 应用的 starter 集合，用于沉淀通用 Web 能力、统一接口规范和基础工程约定。

当前项目包含以下模块：

| 模块 | 说明 |
| --- | --- |
| `zeus-springboot-web-starter` | 面向 Servlet Web 应用的基础 starter，提供统一响应、全局异常处理、请求 ID、接口日志、Redis 序列化和 HTTP Client 能力。 |

## 环境要求

- JDK 21+
- Spring Boot 3.5.x
- Maven 3.9+

## 快速接入

先在本地安装或发布本项目：

```bash
mvn clean install
```

在业务 Spring Boot 应用中引入 starter：

```xml
<dependency>
    <groupId>io.github.hellozhongying</groupId>
    <artifactId>zeus-springboot-web-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

引入依赖后，starter 会通过 Spring Boot 自动配置机制生效，无需额外添加 `@Enable...` 注解。

## 功能概览

`zeus-springboot-web-starter` 当前支持以下能力：

| 功能 | 说明 |
| --- | --- |
| 自动配置 | 基于 Spring Boot `AutoConfiguration.imports` 自动装配 Web 相关组件。 |
| 统一响应包装 | Controller 返回值会被自动包装为 `Result<T>` 结构。 |
| 跳过响应包装 | 通过 `@IgnoreResponseWrap` 在类或方法级别跳过统一响应包装。 |
| 全局异常处理 | 内置 `GlobalExceptionHandler`，统一处理 `ParamException`、`ServiceException` 和其他未捕获异常。 |
| 错误码契约 | 通过 `ErrorCode` 接口约定业务错误码和错误信息。 |
| 参数校验异常处理 | 统一处理 `@Valid`、绑定失败和方法参数约束异常，返回标准 `Result` 错误响应。 |
| 请求 ID | 从 `X-Request-Id` 请求头读取请求 ID；不存在时自动生成 UUID。 |
| 日志追踪 | 自动将 requestId 写入 MDC，日志格式默认输出 requestId，便于按请求追踪日志链路。 |
| 接口日志 | 通过 `@ApiLog` 记录接口名称、客户端 IP、请求参数、返回结果、耗时和异常。 |
| 日志脱敏与限长 | 通过 `@LogMask` 标记敏感字段，接口日志默认替换为 `***`，单段日志值默认最多输出 1000 字符。 |
| 防重复提交 | 通过 `@NoRepeatSubmit` 基于 Redis 原子写入短期提交凭证，拦截时间窗口内的重复提交。 |
| Redis 对象 JSON 序列化 | 业务项目引入 Redis 组件时，默认 `RedisTemplate` 支持对象 JSON 存取。 |
| HTTP Client | 默认提供基于 Spring `RestClient` 和 Apache HttpClient 5 的连接池 HTTP 客户端，并内置简单 GET/POST 工具类。 |
| 常用工具依赖 | 内置 `commons-lang3`、`commons-collections4`、`commons-io` 和 `guava`，覆盖字符串、集合、IO 与 Guava 增强工具。 |

## 自动配置说明

Web 自动配置仅在 Servlet Web 应用中生效，并要求 classpath 中存在 `DispatcherServlet`；Redis 自动配置仅在业务项目引入 Redis 组件时生效。

自动配置类为：

```text
com.zeus.springboot.web.autoconfigure.ZeusWebAutoConfiguration
com.zeus.springboot.web.autoconfigure.ZeusRedisAutoConfiguration
com.zeus.springboot.web.autoconfigure.ZeusHttpClientAutoConfiguration
com.zeus.springboot.web.autoconfigure.ZeusNoRepeatSubmitAutoConfiguration
```

自动装配的 Bean 包括：

| Bean | 条件 | 说明 |
| --- | --- | --- |
| `ZeusWebMarker` | 缺失同类型 Bean 时创建 | 标记 starter 是否启用。 |
| `GlobalExceptionHandler` | 缺失同类型 Bean 时创建 | 提供全局异常处理。 |
| `RequestIdMdcFilter` | 缺失同类型 Bean 时创建 | 将请求 ID 写入 MDC，并回写 `X-Request-Id` 响应头。 |
| `ResponseWrapAdvice` | 存在 `ObjectMapper`，且 `zeus.web.enabled=true` 时创建 | 提供统一响应包装。 |
| `ApiLogAspect` | 存在 `ObjectMapper`，且 `zeus.web.enabled=true` 时创建 | 提供 `@ApiLog` 切面日志。 |
| `NoRepeatSubmitAspect` | 存在 `ObjectMapper` 和 `StringRedisTemplate`，且 `zeus.web.enabled=true`、`zeus.web.no-repeat-submit.enabled=true` 时创建 | 提供 `@NoRepeatSubmit` 防重复提交。 |
| `CloseableHttpClient` | 缺失名为 `zeusCloseableHttpClient` 的 Bean 时创建 | 提供 Apache HttpClient 5 连接池客户端。 |
| `ClientHttpRequestFactory` | 缺失名为 `zeusClientHttpRequestFactory` 的 Bean 时创建 | 将 Apache HttpClient 5 接入 Spring HTTP 调用。 |
| `RestClient` | 缺失名为 `zeusRestClient` 的 Bean 时创建 | 提供 starter 默认同步 HTTP 客户端。 |
| `ZeusHttpClient` | 缺失同类型 Bean 时创建 | 提供简单 GET 和 JSON POST 调用工具。 |

所有自动配置 Bean 都使用 `@ConditionalOnMissingBean`，业务系统可以通过声明同类型 Bean 覆盖默认实现。

### Redis 自动配置

Redis 能力是可选能力。业务项目没有引入 Redis 组件时，starter 不会要求业务项目强制引入 Redis 依赖，也不会注册 Redis 相关 Bean。

当业务项目同时引入 `spring-boot-starter-data-redis` 且容器中存在 `RedisConnectionFactory` 和 `ObjectMapper` 时，starter 会在缺失名为 `redisTemplate` 的 Bean 时提供默认 `RedisTemplate`：

| 配置项 | 默认行为 |
| --- | --- |
| key serializer | `StringRedisSerializer` |
| hash key serializer | `StringRedisSerializer` |
| value serializer | `GenericJackson2JsonRedisSerializer` |
| hash value serializer | `GenericJackson2JsonRedisSerializer` |

因此业务代码可以直接注入 `RedisTemplate<String, T>` 存取任意用户定义对象。对象会以 JSON 字符串形式写入 Redis，读取时由 Jackson 自动反序列化为对应 Java 对象。

如果业务系统需要完全自定义 Redis 序列化方式，可以声明名为 `redisTemplate` 的 Bean 覆盖 starter 默认实现。

### HTTP Client 自动配置

HTTP Client 能力默认开启。starter 会创建一个基于 Apache HttpClient 5 连接池的 Spring `RestClient`，并额外提供 `ZeusHttpClient` 用于简单 GET 和 JSON POST 请求。

默认连接池和超时配置：

| 配置项 | 默认值 |
| --- | --- |
| max total connections | `200` |
| max connections per route | `50` |
| connect timeout | `3s` |
| connection request timeout | `3s` |
| read timeout | `10s` |

业务代码可以直接注入 `ZeusHttpClient`：

```java
@Service
public class RemoteUserService {

    private final ZeusHttpClient zeusHttpClient;

    public RemoteUserService(ZeusHttpClient zeusHttpClient) {
        this.zeusHttpClient = zeusHttpClient;
    }

    public UserDetail getUser(Long id) {
        return zeusHttpClient.get("https://example.com/users/" + id, UserDetail.class);
    }

    public CreateUserResponse createUser(CreateUserRequest request) {
        return zeusHttpClient.postJson(
                "https://example.com/users",
                request,
                CreateUserResponse.class);
    }
}
```

如果需要使用更完整的 Spring `RestClient` API，可以注入名为 `zeusRestClient` 的 Bean：

```java
@Resource(name = "zeusRestClient")
private RestClient zeusRestClient;
```

业务系统需要完全自定义默认 HTTP 客户端时，可以声明以下 Bean 覆盖 starter 默认实现：

| Bean 名称或类型 | 覆盖内容 |
| --- | --- |
| `zeusCloseableHttpClient` | 自定义 Apache HttpClient 5 客户端。 |
| `zeusClientHttpRequestFactory` | 自定义 Spring `ClientHttpRequestFactory`。 |
| `zeusRestClient` | 自定义 starter 默认 `RestClient`。 |
| `ZeusHttpClient` 类型 Bean | 自定义工具类实现。 |

## 配置项说明

配置前缀：`zeus.web`

| 配置项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `zeus.web.enabled` | `boolean` | `true` | Zeus Web starter 功能总开关。为 `false` 时，不会注册统一响应包装 `ResponseWrapAdvice` 和接口日志切面 `ApiLogAspect`；全局异常处理器仍会注册。 |
| `zeus.web.response-wrap.exclude-paths` | `List<String>` | 空列表 | 跳过统一响应包装的 Ant 风格路径，例如 `/actuator/**`。 |
| `zeus.web.api-log.max-length` | `int` | `1000` | `@ApiLog` 单条日志消息的最大长度，同时限制请求参数或响应结果的序列化长度。配置小于等于 0 时回退为 1000。 |
| `zeus.web.api-log.mask-text` | `String` | `***` | `@LogMask` 标记字段在接口日志中的替换文本。空值会回退为 `***`。 |
| `zeus.web.no-repeat-submit.enabled` | `boolean` | `true` | 是否启用 `@NoRepeatSubmit` 防重复提交切面。 |
| `zeus.web.no-repeat-submit.key-prefix` | `String` | `zeus:web:no-repeat-submit` | Redis 防重 key 前缀。 |
| `zeus.web.no-repeat-submit.interval` | `long` | `5` | 注解未指定 `interval` 时使用的默认防重窗口，单位秒。 |
| `zeus.web.no-repeat-submit.message` | `String` | `请勿重复提交` | 注解未指定 `message` 时使用的默认提示信息。 |
| `zeus.web.no-repeat-submit.include-params` | `boolean` | `true` | 是否默认把请求参数摘要纳入防重 key。 |
| `zeus.web.no-repeat-submit.user-identify-header` | `String` | `X-User-Id` | 请求头中的用户标识名称；存在该请求头时优先按用户维度生成防重 key。 |
| `zeus.web.no-repeat-submit.include-client-ip` | `boolean` | `true` | 未取到用户标识时，是否把客户端 IP 纳入防重 key。 |

示例：

```yaml
zeus:
  web:
    enabled: true
    response-wrap:
      exclude-paths:
        - /actuator/**
        - /v3/api-docs/**
        - /swagger-ui/**
    api-log:
      max-length: 1000
      mask-text: "***"
```

关闭统一响应包装和接口日志切面：

```yaml
zeus:
  web:
    enabled: false
```

HTTP Client 配置前缀：`zeus.http-client`

| 配置项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `zeus.http-client.enabled` | `boolean` | `true` | HTTP Client 自动配置开关。 |
| `zeus.http-client.max-total` | `int` | `200` | 连接池最大总连接数。 |
| `zeus.http-client.max-per-route` | `int` | `50` | 单个路由最大连接数。 |
| `zeus.http-client.connect-timeout` | `Duration` | `3s` | 建立连接超时时间。 |
| `zeus.http-client.connection-request-timeout` | `Duration` | `3s` | 从连接池获取连接的超时时间。 |
| `zeus.http-client.read-timeout` | `Duration` | `10s` | 读取响应超时时间。 |

示例：

```yaml
zeus:
  http-client:
    enabled: true
    max-total: 300
    max-per-route: 80
    connect-timeout: 5s
    connection-request-timeout: 2s
    read-timeout: 20s
```

## 统一响应

默认情况下，Controller 返回值会自动包装成 `Result<T>`：

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "id": 1,
    "name": "Zeus"
  },
  "requestId": "9b97fb72-78a2-48b3-a9b2-7e52d37cda2e",
  "timestamp": "2026-06-10T08:00:00Z"
}
```

响应字段说明：

| 字段 | 说明 |
| --- | --- |
| `code` | 响应码。成功固定为 `0`。 |
| `message` | 响应消息。成功固定为 `success`。 |
| `data` | 业务返回数据。 |
| `requestId` | 请求 ID，来自 `X-Request-Id` 请求头或自动生成的 UUID。 |
| `timestamp` | 响应创建时间。 |

示例 Controller：

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public UserDetail getUser(@PathVariable Long id) {
        return new UserDetail(id, "Zeus");
    }
}
```

如果 Controller 已经返回 `Result<?>`，starter 不会重复包装：

```java
@GetMapping("/raw-result")
public Result<String> rawResult() {
    return Result.success("ok", RequestIdHolder.currentRequestId());
}
```

对于 `String` 返回值，starter 会自动序列化为 JSON 字符串，并设置响应 `Content-Type` 为 `application/json`。

## 跳过响应包装

如果某些接口需要返回原始内容，可以使用 `@IgnoreResponseWrap`。

方法级别跳过：

```java
@IgnoreResponseWrap
@GetMapping("/plain")
public String plainText() {
    return "ok";
}
```

类级别跳过：

```java
@IgnoreResponseWrap
@RestController
@RequestMapping("/files")
public class FileController {

    @GetMapping("/download")
    public byte[] download() {
        return new byte[0];
    }
}
```

也可以通过配置批量跳过统一响应包装，适合健康检查、API 文档、文件下载等框架接口：

```yaml
zeus:
  web:
    response-wrap:
      exclude-paths:
        - /actuator/**
        - /v3/api-docs/**
        - /swagger-ui/**
```

## 全局异常处理

starter 内置以下异常处理规则：

| 异常类型 | HTTP 状态码 | 响应错误码来源 |
| --- | --- | --- |
| `ParamException` | `400 Bad Request` | 异常中携带的 `ErrorCode` |
| `MethodArgumentNotValidException` | `400 Bad Request` | `CommonErrorCode.PARAM_ERROR` |
| `BindException` | `400 Bad Request` | `CommonErrorCode.PARAM_ERROR` |
| `ConstraintViolationException` | `400 Bad Request` | `CommonErrorCode.PARAM_ERROR` |
| `ServiceException` | `500 Internal Server Error` | 异常中携带的 `ErrorCode` |
| 其他 `Exception` | `500 Internal Server Error` | `CommonErrorCode.SYSTEM_ERROR` |

异常响应结构与普通响应一致，`data` 为 `null`：

```json
{
  "code": "400",
  "message": "参数错误",
  "data": null,
  "requestId": "9b97fb72-78a2-48b3-a9b2-7e52d37cda2e",
  "timestamp": "2026-06-10T08:00:00Z"
}
```

内置公共错误码：

| 枚举 | code | message |
| --- | --- | --- |
| `CommonErrorCode.PARAM_ERROR` | `400` | `参数错误` |
| `CommonErrorCode.SERVICE_ERROR` | `500` | `服务异常` |
| `CommonErrorCode.SYSTEM_ERROR` | `500` | `系统异常` |

## 自定义错误码

业务错误码可以实现 `ErrorCode` 接口：

```java
public enum BizErrorCode implements ErrorCode {
    USER_NOT_FOUND("A0001", "用户不存在"),
    ORDER_STATUS_INVALID("B0001", "订单状态不正确");

    private final String code;
    private final String message;

    BizErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
```

在业务代码中抛出异常：

```java
if (user == null) {
    throw new ParamException(BizErrorCode.USER_NOT_FOUND);
}
```

```java
try {
    orderService.pay(orderId);
} catch (Exception ex) {
    throw new ServiceException(BizErrorCode.ORDER_STATUS_INVALID, ex);
}
```

## 请求 ID

starter 使用 `RequestIdHolder` 获取当前请求 ID：

1. 优先读取请求头 `X-Request-Id`。
2. 如果请求头不存在或为空，自动生成 UUID。
3. 同一个请求生命周期内会复用同一个 requestId。

客户端传入请求 ID：

```bash
curl -H "X-Request-Id: trace-001" http://localhost:8080/users/1
```

业务代码中获取当前请求 ID：

```java
String requestId = RequestIdHolder.currentRequestId();
```

请求进入 Servlet 过滤器链时，starter 会自动把 requestId 写入 SLF4J MDC：

| MDC Key | 来源 |
| --- | --- |
| `requestId` | `X-Request-Id` 请求头或自动生成的 UUID |

因此一次请求中的 Controller、Service、DAO 等日志都会带上同一个 requestId。响应也会回写 `X-Request-Id`，方便客户端和服务端日志互相对齐。

## Logback 默认配置

starter 内置 `logback-spring.xml`，业务应用引入 starter 后，如果没有自定义 `logback.xml` 或 `logback-spring.xml`，会自动使用默认配置。

默认日志格式：

```text
yyyy-MM-dd HH:mm:ss.SSS LEVEL [thread] [requestId] logger - message
```

默认输出：

| Appender | 说明 |
| --- | --- |
| `CONSOLE` | 控制台日志。 |
| `FILE` | 文件日志，默认写入 `logs/${spring.application.name}.log`。 |

文件日志按天滚动，每天一个归档文件，默认保留 180 天：

```text
logs/2026-06-10/${spring.application.name}.log.2026-06-10.log
```

可通过以下配置调整日志目录、文件名和保留时间：

```yaml
spring:
  application:
    name: demo-service

logging:
  file:
    path: logs
    name: demo-service.log

zeus:
  logging:
    file:
      max-history: 180
      total-size-cap: 20GB
```

如果业务应用需要完全自定义日志，只需在业务应用中提供自己的 `logback-spring.xml` 或 `logback.xml`。

## 接口日志

在 Controller 方法上添加 `@ApiLog` 后，会记录接口调用日志。

```java
@ApiLog("查询用户详情")
@GetMapping("/users/{id}")
public UserDetail getUser(@PathVariable Long id) {
    return userService.getUser(id);
}
```

如果不指定注解值，默认使用 `类名#方法名` 作为接口名称：

```java
@ApiLog
@PostMapping("/users")
public Long createUser(@RequestBody CreateUserRequest request) {
    return userService.createUser(request);
}
```

日志内容包括：

| 阶段 | 内容 |
| --- | --- |
| 请求开始 | 接口名称、客户端 IP、请求参数 |
| 请求完成 | 接口名称、客户端 IP、返回结果、耗时 |
| 请求失败 | 接口名称、客户端 IP、耗时、异常堆栈 |

客户端 IP 获取顺序：

1. `X-Forwarded-For` 的第一个 IP
2. `X-Real-IP`
3. `request.getRemoteAddr()`

日志参数会过滤以下类型，避免直接序列化底层 Web 对象或文件内容：

- `ServletRequest`
- `ServletResponse`
- `MultipartFile`

### 日志脱敏

在请求参数或响应结果对象的字段、JavaBean getter 上添加 `@LogMask`，接口日志会将该属性替换为 `***`。

```java
public class CreateUserRequest {

    private String username;

    @LogMask
    private String password;

    @LogMask
    private String token;

    // getters/setters
}
```

示例日志片段：

```text
parameters=[{"username":"zeus","password":"***","token":"***"}]
```

替换文本和日志长度可以通过配置调整：

```yaml
zeus:
  web:
    api-log:
      mask-text: "***"
      max-length: 1000
```

`max-length` 会限制 `@ApiLog` 的单条日志消息长度，也会限制序列化后的请求参数集合或响应结果长度，默认最多输出 1000 字符，避免大对象、长文本或异常响应把接口日志撑得过长。

## 覆盖默认实现

如果业务系统需要定制默认行为，可以声明同类型 Bean 覆盖 starter 自动配置。

例如自定义全局异常处理器：

```java
@RestControllerAdvice
public class CustomGlobalExceptionHandler extends GlobalExceptionHandler {
}
```

例如自定义响应包装逻辑：

```java
@Bean
public ResponseWrapAdvice responseWrapAdvice(ObjectMapper objectMapper) {
    return new CustomResponseWrapAdvice(objectMapper);
}
```

## 构建与测试

构建整个项目：

```bash
mvn clean package
```

运行测试：

```bash
mvn test
```

## 注意事项

- 该 starter 仅支持 Spring MVC Servlet Web 应用，不适用于 WebFlux 应用。
- `zeus.web.enabled=false` 只关闭统一响应包装和 `@ApiLog` 切面，不关闭全局异常处理。
- `@ApiLog` 会记录请求参数和返回结果，敏感字段请使用 `@LogMask` 标记；不适合记录的接口可以不添加 `@ApiLog`。
- 健康检查、Swagger/OpenAPI 文档、文件下载等原始响应接口，建议通过 `zeus.web.response-wrap.exclude-paths` 或 `@IgnoreResponseWrap` 跳过统一包装。
- 如果业务应用没有可用的 `ObjectMapper` Bean，统一响应包装和接口日志切面不会自动注册。
