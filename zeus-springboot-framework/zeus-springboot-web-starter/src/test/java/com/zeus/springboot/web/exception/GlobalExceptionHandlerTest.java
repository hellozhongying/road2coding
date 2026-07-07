package com.zeus.springboot.web.exception;

import com.zeus.springboot.web.response.Result;
import com.zeus.springboot.web.log.RequestIdMdcFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证全局异常处理器既返回统一响应，也输出定位问题所需的异常日志。
 */
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void logsUnhandledExceptionWithStackTrace(CapturedOutput output) {
        IllegalStateException exception = new IllegalStateException("database unavailable");

        ResponseEntity<Result<Void>> response = handler.handleException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().code()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("系统异常");
        assertThat(output)
                .contains("Unhandled exception handled by global exception handler")
                .contains("java.lang.IllegalStateException: database unavailable")
                .contains("GlobalExceptionHandlerTest.logsUnhandledExceptionWithStackTrace");
    }

    @Test
    void resultErrorUsesDefaultCodeAndMdcRequestId() {
        MDC.put(RequestIdMdcFilter.REQUEST_ID_MDC_KEY, "request-from-mdc");
        try {
            Result<Void> result = Result.error("失败了");

            assertThat(result.code()).isEqualTo(500);
            assertThat(result.message()).isEqualTo("失败了");
            assertThat(result.requestId()).isEqualTo("request-from-mdc");
        } finally {
            MDC.remove(RequestIdMdcFilter.REQUEST_ID_MDC_KEY);
        }
    }

    @Test
    void resultOkUsesDefaultCodeAndMdcRequestId() {
        MDC.put(RequestIdMdcFilter.REQUEST_ID_MDC_KEY, "ok-request");
        try {
            Result<Void> result = Result.OK();

            assertThat(result.code()).isEqualTo(200);
            assertThat(result.message()).isEqualTo("success");
            assertThat(result.data()).isNull();
            assertThat(result.requestId()).isEqualTo("ok-request");
        } finally {
            MDC.remove(RequestIdMdcFilter.REQUEST_ID_MDC_KEY);
        }
    }
}
