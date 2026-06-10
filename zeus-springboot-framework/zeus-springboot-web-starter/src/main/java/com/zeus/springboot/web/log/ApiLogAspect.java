package com.zeus.springboot.web.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeus.springboot.web.annotation.ApiLog;
import com.zeus.springboot.web.autoconfigure.ZeusWebProperties;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ApiLog} 注解的日志切面。
 *
 * <p>围绕 Controller 方法记录请求开始、正常完成和异常失败三类日志，并在输出前进行敏感字段脱敏。</p>
 */
@Aspect
public class ApiLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ApiLogAspect.class);
    private static final String UNKNOWN_IP = "unknown";
    private static final int DEFAULT_MAX_LOG_LENGTH = 1000;
    private static final String DEFAULT_MASK_TEXT = "***";

    private final ObjectMapper objectMapper;

    private final LogValueSanitizer logValueSanitizer;

    private final int maxLogLength;

    public ApiLogAspect(ObjectMapper objectMapper) {
        this(objectMapper, new ZeusWebProperties());
    }

    public ApiLogAspect(ObjectMapper objectMapper, ZeusWebProperties properties) {
        this.objectMapper = objectMapper;
        ZeusWebProperties.ApiLog apiLog = properties == null ? new ZeusWebProperties.ApiLog() : properties.getApiLog();
        String maskText = StringUtils.hasText(apiLog.getMaskText()) ? apiLog.getMaskText() : DEFAULT_MASK_TEXT;
        this.logValueSanitizer = new LogValueSanitizer(objectMapper, maskText);
        this.maxLogLength = apiLog.getMaxLength() > 0 ? apiLog.getMaxLength() : DEFAULT_MAX_LOG_LENGTH;
    }

    @Around("@annotation(apiLog)")
    public Object logApi(ProceedingJoinPoint joinPoint, ApiLog apiLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String apiName = getApiName(joinPoint, apiLog);
        String clientIp = getClientIp();
        String parameters = toJson(filterArguments(joinPoint.getArgs()));

        // 请求开始和结束分开记录，便于定位慢请求和失败请求发生在链路中的哪个阶段。
        log.info(truncate("Api request started. name=%s, clientIp=%s, parameters=%s"
                .formatted(apiName, clientIp, parameters)));
        try {
            Object result = joinPoint.proceed();
            long costTime = System.currentTimeMillis() - startTime;
            log.info(truncate("Api request completed. name=%s, clientIp=%s, result=%s, costTime=%dms"
                    .formatted(apiName, clientIp, toJson(result), costTime)));
            return result;
        } catch (Throwable ex) {
            long costTime = System.currentTimeMillis() - startTime;
            log.info(truncate("Api request failed. name=%s, clientIp=%s, costTime=%dms"
                    .formatted(apiName, clientIp, costTime)), ex);
            throw ex;
        }
    }

    private String getApiName(ProceedingJoinPoint joinPoint, ApiLog apiLog) {
        if (StringUtils.hasText(apiLog.value())) {
            return apiLog.value();
        }
        // 未显式指定名称时，使用稳定的方法签名作为日志名称。
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    }

    private String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return UNKNOWN_IP;
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor) && !UNKNOWN_IP.equalsIgnoreCase(xForwardedFor)) {
            // 经过多级代理时，X-Forwarded-For 第一个 IP 通常是原始客户端 IP。
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !UNKNOWN_IP.equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private HttpServletRequest getCurrentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private List<Object> filterArguments(Object[] args) {
        List<Object> filteredArgs = new ArrayList<>();
        if (args == null) {
            return filteredArgs;
        }
        for (Object arg : args) {
            if (shouldLogArgument(arg)) {
                filteredArgs.add(arg);
            }
        }
        return filteredArgs;
    }

    private boolean shouldLogArgument(Object arg) {
        // Servlet 原生对象和上传文件不适合直接序列化进日志，避免冗余输出或触发不可预期的读取。
        return !(arg instanceof ServletRequest
                || arg instanceof ServletResponse
                || arg instanceof MultipartFile);
    }

    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return truncate(objectMapper.writeValueAsString(logValueSanitizer.sanitize(value)));
        } catch (JsonProcessingException ex) {
            // 序列化失败时退化为 toString，保证日志切面不影响业务请求执行。
            return truncate(String.valueOf(value));
        }
    }

    private String truncate(String value) {
        if (value.length() <= maxLogLength) {
            return value;
        }
        return value.substring(0, maxLogLength);
    }
}
