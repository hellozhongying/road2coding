# Zeus Spring Boot Framework

Zeus Spring Boot Framework 是一组面向 Spring Boot 应用的 starter 集合，用于沉淀通用 Web 能力、统一接口规范和基础工程约定。

当前项目包含以下模块：

| 模块 | 说明 |
| --- | --- |
| `zeus-springboot-web-starter` | 面向 Servlet Web 应用的基础 starter，提供统一响应、全局异常处理、请求 ID 和接口日志能力。 |

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
    <groupId>com.zeus</groupId>
    <artifactId>zeus-springboot-web-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
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
| 请求 ID | 从 `X-Request-Id` 请求头读取请求 ID；不存在时自动生成 UUID。 |
| 接口日志 | 通过 `@ApiLog` 记录接口名称、客户端 IP、请求参数、返回结果、耗时和异常。 |

## 自动配置说明

starter 仅在 Servlet Web 应用中生效，并要求 classpath 中存在 `DispatcherServlet`。

自动配置类为：

```text
com.zeus.springboot.web.autoconfigure.ZeusWebAutoConfiguration
```

自动装配的 Bean 包括：

| Bean | 条件 | 说明 |
| --- | --- | --- |
| `ZeusWebMarker` | 缺失同类型 Bean 时创建 | 标记 starter 是否启用。 |
| `GlobalExceptionHandler` | 缺失同类型 Bean 时创建 | 提供全局异常处理。 |
| `ResponseWrapAdvice` | 存在 `ObjectMapper`，且 `zeus.web.enabled=true` 时创建 | 提供统一响应包装。 |
| `ApiLogAspect` | 存在 `ObjectMapper`，且 `zeus.web.enabled=true` 时创建 | 提供 `@ApiLog` 切面日志。 |

所有自动配置 Bean 都使用 `@ConditionalOnMissingBean`，业务系统可以通过声明同类型 Bean 覆盖默认实现。

## 配置项说明

配置前缀：`zeus.web`

| 配置项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `zeus.web.enabled` | `boolean` | `true` | Zeus Web starter 功能总开关。为 `false` 时，不会注册统一响应包装 `ResponseWrapAdvice` 和接口日志切面 `ApiLogAspect`；全局异常处理器仍会注册。 |

示例：

```yaml
zeus:
  web:
    enabled: true
```

关闭统一响应包装和接口日志切面：

```yaml
zeus:
  web:
    enabled: false
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

## 全局异常处理

starter 内置以下异常处理规则：

| 异常类型 | HTTP 状态码 | 响应错误码来源 |
| --- | --- | --- |
| `ParamException` | `400 Bad Request` | 异常中携带的 `ErrorCode` |
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
- `@ApiLog` 会记录请求参数和返回结果，请避免在敏感接口中直接记录密码、密钥、令牌等敏感信息。
- 如果业务应用没有可用的 `ObjectMapper` Bean，统一响应包装和接口日志切面不会自动注册。
