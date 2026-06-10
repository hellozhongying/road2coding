package com.zeus.springboot.web.log;

import com.zeus.springboot.web.response.RequestIdHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求 ID 日志追踪过滤器。
 *
 * <p>每个请求进入应用后，将 {@code X-Request-Id} 请求头或自动生成的请求 ID 写入 SLF4J MDC，
 * 使 Controller、Service、DAO 等同一次请求内的日志都可以通过 {@code requestId} 串联追踪。</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdMdcFilter extends OncePerRequestFilter {

    /**
     * MDC 中存放请求 ID 的 key，logback 模板通过 {@code %X{requestId}} 输出该值。
     */
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = RequestIdHolder.getOrCreateRequestId(request);
        String previousRequestId = MDC.get(REQUEST_ID_MDC_KEY);

        // 请求开始时写入 MDC，并把 requestId 回写到响应头，方便客户端与服务端日志对齐。
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(RequestIdHolder.REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Servlet 容器线程会被复用，请求结束后必须恢复或清理 MDC，避免串到下一次请求。
            if (previousRequestId == null) {
                MDC.remove(REQUEST_ID_MDC_KEY);
            } else {
                MDC.put(REQUEST_ID_MDC_KEY, previousRequestId);
            }
        }
    }
}
