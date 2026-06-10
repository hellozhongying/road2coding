package com.zeus.springboot.web.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeus.springboot.web.annotation.ApiLog;
import com.zeus.springboot.web.annotation.LogMask;
import com.zeus.springboot.web.autoconfigure.ZeusWebProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class ApiLogAspectTest {

    private final ApiLogAspect aspect = new ApiLogAspect(new ObjectMapper());

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
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"zeus"});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn(Map.of("name", "zeus"));
        when(signature.getMethod()).thenReturn(method);

        Object result = aspect.logApi(joinPoint, method.getAnnotation(ApiLog.class));

        assertThat(result).isEqualTo(Map.of("name", "zeus"));
        assertThat(output).contains("name=Query API");
        assertThat(output).contains("clientIp=192.168.1.10");
        assertThat(output).contains("parameters=[\"zeus\"]");
        assertThat(output).contains("result={\"name\":\"zeus\"}");
        assertThat(output).contains("costTime=");
    }

    @Test
    void masksAnnotatedFieldsAndTruncatesLongLogValues(CapturedOutput output) throws Throwable {
        ZeusWebProperties properties = new ZeusWebProperties();
        properties.getApiLog().setMaxLength(1000);
        ApiLogAspect maskedAspect = new ApiLogAspect(new ObjectMapper(), properties);

        Method method = TestController.class.getMethod("save", SaveUserRequest.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        SaveUserRequest request = new SaveUserRequest("zeus", "secret-token", "x".repeat(1200));
        when(joinPoint.getArgs()).thenReturn(new Object[]{request});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn(request);
        when(signature.getMethod()).thenReturn(method);

        maskedAspect.logApi(joinPoint, method.getAnnotation(ApiLog.class));

        assertThat(output).contains("\"token\":\"***\"");
        assertThat(output).doesNotContain("secret-token");
        assertThat(output).doesNotContain("x".repeat(1001));
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
}
