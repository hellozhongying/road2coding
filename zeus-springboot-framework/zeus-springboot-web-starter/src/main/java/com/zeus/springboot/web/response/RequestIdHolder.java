package com.zeus.springboot.web.response;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * 请求追踪 ID 工具。
 *
 * <p>优先复用客户端传入的 {@code X-Request-Id}，缺失时自动生成 UUID，并缓存在当前请求属性中。</p>
 */
public final class RequestIdHolder {

    /**
     * 客户端与服务端约定的请求追踪头。
     */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final String REQUEST_ID_ATTRIBUTE = RequestIdHolder.class.getName() + ".REQUEST_ID";

    private RequestIdHolder() {
    }

    /**
     * 获取当前请求的 requestId；非 Web 请求上下文中返回一个新的 UUID。
     */
    public static String currentRequestId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return getOrCreateRequestId(servletRequestAttributes.getRequest());
        }
        return UUID.randomUUID().toString();
    }

    /**
     * 从请求属性或请求头中获取 requestId，均不存在时创建并写回请求属性。
     */
    public static String getOrCreateRequestId(HttpServletRequest request) {
        Object existing = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (existing instanceof String requestId && StringUtils.hasText(requestId)) {
            return requestId;
        }

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (!StringUtils.hasText(requestId)) {
            // 请求头缺失时由服务端生成，保证后续响应体、响应头和日志中都有追踪标识。
            requestId = UUID.randomUUID().toString();
        }
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        return requestId;
    }
}
