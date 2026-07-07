package com.zeus.springboot.web.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeus.springboot.web.annotation.ApiLog;
import com.zeus.springboot.web.annotation.LogMask;
import com.zeus.springboot.web.autoconfigure.ZeusWebProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 API 日志切面的请求/响应记录、客户端 IP 解析和敏感字段脱敏。
 */
@ExtendWith(OutputCaptureExtension.class)
class ApiLogAspectTest {

    private final ApiLogAspect aspect = new ApiLogAspect(new ObjectMapper());

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void resetRequestAttributes() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logsApiRequestAndResponse(CapturedOutput output) throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "192.168.1.10, 10.0.0.1");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = TestController.class.getMethod("query", String.class);

        // 通过 CapturedOutput 直接检查日志内容，确保切面输出包含排查问题所需的关键字段。
        Object result = aspect.logApi(joinPoint(method, new Object[]{"zeus"}, Map.of("name", "zeus")),
                method.getAnnotation(ApiLog.class));

        assertThat(result).isEqualTo(Map.of("name", "zeus"));
        assertThat(output).contains("name=Query API");
        assertThat(output).contains("clientIp=192.168.1.10");
        assertThat(output).contains("parameters=[\"zeus\"]");
        assertThat(output).contains("result={\"name\":\"zeus\"}");
        assertThat(output).contains("costTime=");
    }

    @Test
    void logsJsonNodeRequestBody(CapturedOutput output) throws Throwable {
        JsonNode node = objectMapper.readTree("""
                {
                  "username": "zeus",
                  "role": "admin"
                }
                """);
        Method method = TestController.class.getMethod("saveJsonNode", JsonNode.class);

        aspect.logApi(joinPoint(method, new Object[]{node}, Map.of("ok", true)), method.getAnnotation(ApiLog.class));

        assertThat(output).contains("name=Save JsonNode");
        assertThat(output).contains("parameters=[{\"username\":\"zeus\",\"role\":\"admin\"}]");
    }

    @Test
    void masksAnnotatedFieldsAndTruncatesLongLogValues(CapturedOutput output) throws Throwable {
        ZeusWebProperties properties = new ZeusWebProperties();
        properties.getApiLog().setMaxLength(1000);
        ApiLogAspect maskedAspect = new ApiLogAspect(new ObjectMapper(), properties);

        Method method = TestController.class.getMethod("save", SaveUserRequest.class);
        SaveUserRequest request = new SaveUserRequest("zeus", "secret-token", "x".repeat(1200));

        // token 被 @LogMask 标记，应在入参与返回值日志中同时脱敏。
        maskedAspect.logApi(joinPoint(method, new Object[]{request}, request), method.getAnnotation(ApiLog.class));

        assertThat(output).contains("\"token\":\"***\"");
        assertThat(output).doesNotContain("secret-token");
        assertThat(output).doesNotContain("x".repeat(1001));
    }

    private ProceedingJoinPoint joinPoint(Method method, Object[] args, Object result) {
        return new TestProceedingJoinPoint(method, args, result);
    }

    static class TestController {

        @ApiLog("Query API")
        public Map<String, String> query(String name) {
            return Map.of("name", name);
        }

        @ApiLog("Save User")
        public SaveUserRequest save(SaveUserRequest request) {
            return request;
        }

        @ApiLog("Save JsonNode")
        public Map<String, Boolean> saveJsonNode(JsonNode node) {
            return Map.of("ok", node.has("username"));
        }
    }

    static class SaveUserRequest {

        private final String name;

        @LogMask
        private final String token;

        private final String description;

        SaveUserRequest(String name, String token, String description) {
            this.name = name;
            this.token = token;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getToken() {
            return token;
        }

        public String getDescription() {
            return description;
        }
    }

    private record TestProceedingJoinPoint(Method method, Object[] args, Object result) implements ProceedingJoinPoint {

        @Override
        public Object proceed() {
            return result;
        }

        @Override
        public Object proceed(Object[] args) {
            return result;
        }

        @Override
        public void set$AroundClosure(AroundClosure aroundClosure) {
        }

        @Override
        public Object getThis() {
            return null;
        }

        @Override
        public Object getTarget() {
            return null;
        }

        @Override
        public Object[] getArgs() {
            return args;
        }

        @Override
        public Signature getSignature() {
            return new TestMethodSignature(method);
        }

        @Override
        public SourceLocation getSourceLocation() {
            return null;
        }

        @Override
        public String getKind() {
            return METHOD_EXECUTION;
        }

        @Override
        public StaticPart getStaticPart() {
            return null;
        }

        @Override
        public String toShortString() {
            return method.getName();
        }

        @Override
        public String toLongString() {
            return method.toString();
        }

        @Override
        public String toString() {
            return method.toString();
        }
    }

    private record TestMethodSignature(Method method) implements MethodSignature {

        @Override
        public Class<?> getReturnType() {
            return method.getReturnType();
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return method.getParameterTypes();
        }

        @Override
        public String[] getParameterNames() {
            return new String[method.getParameterCount()];
        }

        @Override
        public Class<?>[] getExceptionTypes() {
            return method.getExceptionTypes();
        }

        @Override
        public String getName() {
            return method.getName();
        }

        @Override
        public int getModifiers() {
            return method.getModifiers();
        }

        @Override
        public Class<?> getDeclaringType() {
            return method.getDeclaringClass();
        }

        @Override
        public String getDeclaringTypeName() {
            return method.getDeclaringClass().getName();
        }

        @Override
        public String toShortString() {
            return method.getName();
        }

        @Override
        public String toLongString() {
            return method.toString();
        }

        @Override
        public String toString() {
            return method.toString();
        }
    }
}
