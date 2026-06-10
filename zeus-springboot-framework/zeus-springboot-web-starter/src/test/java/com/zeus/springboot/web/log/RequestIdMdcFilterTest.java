package com.zeus.springboot.web.log;

import com.zeus.springboot.web.response.RequestIdHolder;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证请求 ID 过滤器会正确写入 MDC、响应头，并在请求结束后清理线程上下文。
 */
class RequestIdMdcFilterTest {

    private final RequestIdMdcFilter filter = new RequestIdMdcFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void putsRequestIdIntoMdcAndResponseHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdHolder.REQUEST_ID_HEADER, "trace-001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                requestIdInChain.set(MDC.get(RequestIdMdcFilter.REQUEST_ID_MDC_KEY)));

        assertThat(requestIdInChain).hasValue("trace-001");
        assertThat(response.getHeader(RequestIdHolder.REQUEST_ID_HEADER)).isEqualTo("trace-001");
        assertThat(MDC.get(RequestIdMdcFilter.REQUEST_ID_MDC_KEY)).isNull();
    }

    @Test
    void generatesRequestIdWhenHeaderMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                requestIdInChain.set(MDC.get(RequestIdMdcFilter.REQUEST_ID_MDC_KEY)));

        assertThat(requestIdInChain.get()).isNotBlank();
        assertThat(response.getHeader(RequestIdHolder.REQUEST_ID_HEADER)).isEqualTo(requestIdInChain.get());
        assertThat(MDC.get(RequestIdMdcFilter.REQUEST_ID_MDC_KEY)).isNull();
    }
}
