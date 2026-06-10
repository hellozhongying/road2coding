package com.zeus.springboot.web.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeus.springboot.web.annotation.ApiLog;
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

@Aspect
public class ApiLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ApiLogAspect.class);
    private static final String UNKNOWN_IP = "unknown";

    private final ObjectMapper objectMapper;

    public ApiLogAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(apiLog)")
    public Object logApi(ProceedingJoinPoint joinPoint, ApiLog apiLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String apiName = getApiName(joinPoint, apiLog);
        String clientIp = getClientIp();
        String parameters = toJson(filterArguments(joinPoint.getArgs()));

        log.info("Api request started. name={}, clientIp={}, parameters={}", apiName, clientIp, parameters);
        try {
            Object result = joinPoint.proceed();
            long costTime = System.currentTimeMillis() - startTime;
            log.info("Api request completed. name={}, clientIp={}, result={}, costTime={}ms",
                    apiName, clientIp, toJson(result), costTime);
            return result;
        } catch (Throwable ex) {
            long costTime = System.currentTimeMillis() - startTime;
            log.info("Api request failed. name={}, clientIp={}, costTime={}ms", apiName, clientIp, costTime, ex);
            throw ex;
        }
    }

    private String getApiName(ProceedingJoinPoint joinPoint, ApiLog apiLog) {
        if (StringUtils.hasText(apiLog.value())) {
            return apiLog.value();
        }
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
        return !(arg instanceof ServletRequest
                || arg instanceof ServletResponse
                || arg instanceof MultipartFile);
    }

    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }
}
