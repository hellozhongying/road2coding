package com.zeus.springboot.web.repeat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeus.springboot.web.annotation.NoRepeatSubmit;
import com.zeus.springboot.web.autoconfigure.ZeusWebProperties;
import com.zeus.springboot.web.exception.ParamException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证防重复提交切面的 Redis 原子写入、重复提交拦截和 key 生成规则。
 */
class NoRepeatSubmitAspectTest {

    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    private final ZeusWebProperties properties = new ZeusWebProperties();

    private final NoRepeatSubmitAspect aspect = new NoRepeatSubmitAspect(
            stringRedisTemplate, new ObjectMapper(), properties);

    @AfterEach
    void resetRequestAttributes() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void allowsFirstSubmitAndUsesAnnotationTtl() throws Throwable {
        Method method = TestController.class.getMethod("create", Map.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, new Object[]{Map.of("name", "zeus")});
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), eq(Duration.ofSeconds(10)))).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");
        bindRequest("POST", "/orders", "user-1", "192.168.1.10");

        Object result = aspect.preventRepeatSubmit(joinPoint, method.getAnnotation(NoRepeatSubmit.class));

        assertThat(result).isEqualTo("ok");
        verify(joinPoint).proceed();
    }

    @Test
    void rejectsRepeatSubmitWithAnnotationMessage() throws Throwable {
        Method method = TestController.class.getMethod("create", Map.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, new Object[]{Map.of("name", "zeus")});
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        bindRequest("POST", "/orders", "user-1", "192.168.1.10");

        assertThatThrownBy(() -> aspect.preventRepeatSubmit(joinPoint, method.getAnnotation(NoRepeatSubmit.class)))
                .isInstanceOf(ParamException.class)
                .hasMessage("订单正在提交，请勿重复点击");
    }

    @Test
    void usesGlobalConfigWhenAnnotationKeepsDefaults() throws Throwable {
        properties.getNoRepeatSubmit().setInterval(8);
        properties.getNoRepeatSubmit().setMessage("请稍后再试");
        Method method = TestController.class.getMethod("update", Map.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, new Object[]{Map.of("name", "zeus")});
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), eq(Duration.ofSeconds(8)))).thenReturn(false);
        bindRequest("PUT", "/users/1", null, "192.168.1.10");

        assertThatThrownBy(() -> aspect.preventRepeatSubmit(joinPoint, method.getAnnotation(NoRepeatSubmit.class)))
                .isInstanceOf(ParamException.class)
                .hasMessage("请稍后再试");
    }

    @Test
    void includesUserPathMethodSignatureAndParameterHashInKey() throws Exception {
        Method method = TestController.class.getMethod("create", Map.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, new Object[]{Map.of("name", "zeus")});
        bindRequest("POST", "/orders", "user-1", "192.168.1.10");

        String key = aspect.buildSubmitKey(joinPoint, method.getAnnotation(NoRepeatSubmit.class));

        assertThat(key).startsWith("zeus:web:no-repeat-submit:user:user-1:");
        assertThat(key).contains(":POST::orders:");
    }

    @Test
    void fallsBackToClientIpWhenUserHeaderMissing() throws Exception {
        Method method = TestController.class.getMethod("update", Map.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, new Object[]{Map.of("name", "zeus")});
        bindRequest("PUT", "/users/1", null, "192.168.1.10");

        String key = aspect.buildSubmitKey(joinPoint, method.getAnnotation(NoRepeatSubmit.class));

        assertThat(key).contains(":ip:192.168.1.10:");
    }

    @Test
    void excludesParametersWhenAnnotationOverridesGlobalConfig() throws Exception {
        properties.getNoRepeatSubmit().setIncludeParams(true);
        Method method = TestController.class.getMethod("delete", HttpServletRequest.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, new Object[]{mock(HttpServletRequest.class)});
        bindRequest("DELETE", "/orders/1", "user-1", "192.168.1.10");

        String key = aspect.buildSubmitKey(joinPoint, method.getAnnotation(NoRepeatSubmit.class));

        assertThat(key).doesNotEndWith(":4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e762302547130b089440000");
    }

    private ProceedingJoinPoint mockJoinPoint(Method method, Object[] args) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.toLongString()).thenReturn(method.toGenericString());
        return joinPoint;
    }

    private void bindRequest(String method, String requestUri, String userId, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, requestUri);
        if (userId != null) {
            request.addHeader("X-User-Id", userId);
        }
        request.setRemoteAddr(remoteAddr);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    static class TestController {

        @NoRepeatSubmit(interval = 10, message = "订单正在提交，请勿重复点击")
        public String create(Map<String, Object> request) {
            return "ok";
        }

        @NoRepeatSubmit
        public String update(Map<String, Object> request) {
            return "ok";
        }

        @NoRepeatSubmit(paramStrategy = NoRepeatSubmit.ParamStrategy.EXCLUDE)
        public String delete(HttpServletRequest request) {
            return "ok";
        }
    }
}
