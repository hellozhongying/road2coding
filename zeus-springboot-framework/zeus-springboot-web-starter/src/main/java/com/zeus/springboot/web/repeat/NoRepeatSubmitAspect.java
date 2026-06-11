package com.zeus.springboot.web.repeat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeus.springboot.web.annotation.NoRepeatSubmit;
import com.zeus.springboot.web.autoconfigure.ZeusWebProperties;
import com.zeus.springboot.web.exception.CommonErrorCode;
import com.zeus.springboot.web.exception.ParamException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * {@link NoRepeatSubmit} 注解的防重复提交切面。
 */
@Aspect
public class NoRepeatSubmitAspect {

    private static final String UNKNOWN = "unknown";

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    private final ZeusWebProperties.NoRepeatSubmit properties;

    public NoRepeatSubmitAspect(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper,
                                ZeusWebProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties == null ? new ZeusWebProperties.NoRepeatSubmit() : properties.getNoRepeatSubmit();
    }

    @Around("@annotation(noRepeatSubmit)")
    public Object preventRepeatSubmit(ProceedingJoinPoint joinPoint, NoRepeatSubmit noRepeatSubmit) throws Throwable {
        Duration ttl = resolveTtl(noRepeatSubmit);
        String key = buildSubmitKey(joinPoint, noRepeatSubmit);
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, UUID.randomUUID().toString(), ttl);
        if (Boolean.FALSE.equals(success)) {
            throw new ParamException(CommonErrorCode.PARAM_ERROR, resolveMessage(noRepeatSubmit));
        }
        return joinPoint.proceed();
    }

    String buildSubmitKey(ProceedingJoinPoint joinPoint, NoRepeatSubmit noRepeatSubmit) {
        HttpServletRequest request = getCurrentRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        StringBuilder key = new StringBuilder(resolveKeyPrefix())
                .append(':')
                .append(resolveSubmitter(request))
                .append(':')
                .append(hash(signature.toLongString()));

        if (request != null) {
            key.append(':').append(request.getMethod()).append(':').append(normalizePath(request.getRequestURI()));
        }
        if (shouldIncludeParams(noRepeatSubmit)) {
            key.append(':').append(hash(toJson(filterArguments(joinPoint.getArgs()))));
        }
        return key.toString();
    }

    private Duration resolveTtl(NoRepeatSubmit noRepeatSubmit) {
        long interval = noRepeatSubmit.interval() > 0 ? noRepeatSubmit.interval() : properties.getInterval();
        if (interval <= 0) {
            interval = 5;
        }
        return Duration.ofMillis(noRepeatSubmit.timeUnit().toMillis(interval));
    }

    private String resolveMessage(NoRepeatSubmit noRepeatSubmit) {
        if (StringUtils.hasText(noRepeatSubmit.message())) {
            return noRepeatSubmit.message();
        }
        return StringUtils.hasText(properties.getMessage()) ? properties.getMessage() : "请勿重复提交";
    }

    private String resolveKeyPrefix() {
        return StringUtils.hasText(properties.getKeyPrefix())
                ? properties.getKeyPrefix()
                : "zeus:web:no-repeat-submit";
    }

    @SuppressWarnings("deprecation")
    private boolean shouldIncludeParams(NoRepeatSubmit noRepeatSubmit) {
        if (noRepeatSubmit.paramStrategy() == NoRepeatSubmit.ParamStrategy.INCLUDE) {
            return true;
        }
        if (noRepeatSubmit.paramStrategy() == NoRepeatSubmit.ParamStrategy.EXCLUDE) {
            return false;
        }
        return properties.isIncludeParams() && noRepeatSubmit.includeParams();
    }

    private String resolveSubmitter(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String userHeader = properties.getUserIdentifyHeader();
        if (StringUtils.hasText(userHeader)) {
            String user = request.getHeader(userHeader);
            if (StringUtils.hasText(user)) {
                return "user:" + user;
            }
        }
        if (properties.isIncludeClientIp()) {
            return "ip:" + getClientIp(request);
        }
        return UNKNOWN;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor) && !UNKNOWN.equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !UNKNOWN.equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String normalizePath(String path) {
        return StringUtils.hasText(path) ? path.replace('/', ':') : UNKNOWN;
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
            if (shouldUseArgument(arg)) {
                filteredArgs.add(arg);
            }
        }
        return filteredArgs;
    }

    private boolean shouldUseArgument(Object arg) {
        return !(arg instanceof ServletRequest
                || arg instanceof ServletResponse
                || arg instanceof MultipartFile);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
