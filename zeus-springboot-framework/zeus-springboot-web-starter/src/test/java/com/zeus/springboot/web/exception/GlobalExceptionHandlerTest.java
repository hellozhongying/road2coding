package com.zeus.springboot.web.exception;

import com.zeus.springboot.web.response.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("500");
        assertThat(response.getBody().message()).isEqualTo("系统异常");
        assertThat(output)
                .contains("Unhandled exception handled by global exception handler")
                .contains("java.lang.IllegalStateException: database unavailable")
                .contains("GlobalExceptionHandlerTest.logsUnhandledExceptionWithStackTrace");
    }
}
