package com.zeus.springboot.web.response;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

public final class RequestIdHolder {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final String REQUEST_ID_ATTRIBUTE = RequestIdHolder.class.getName() + ".REQUEST_ID";

    private RequestIdHolder() {
    }

    public static String currentRequestId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return getOrCreateRequestId(servletRequestAttributes.getRequest());
        }
        return UUID.randomUUID().toString();
    }

    public static String getOrCreateRequestId(HttpServletRequest request) {
        Object existing = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (existing instanceof String requestId && StringUtils.hasText(requestId)) {
            return requestId;
        }

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
        }
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        return requestId;
    }
}
