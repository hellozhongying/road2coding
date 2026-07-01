package com.zeus.springboot.web.exception;

import com.zeus.springboot.web.response.RequestIdHolder;
import com.zeus.springboot.web.response.Result;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务主动抛出的参数异常，统一返回 200 响应，由 Result.code 承载业务错误码。
     */
    @ExceptionHandler(ParamException.class)
    public ResponseEntity<Result<Void>> handleParamException(ParamException exception) {
        String requestId = RequestIdHolder.currentRequestId();
        log.warn("Parameter exception handled, requestId={}, code={}, message={}",
                requestId, exception.getErrorCode().getCode(), exception.getErrorCode().getMessage());
        return buildResponse(exception.getErrorCode(), requestId);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<Result<Void>> handleValidationException(Exception exception) {
        // Bean Validation 校验失败属于客户端参数错误，统一沿用 PARAM_ERROR 对外响应。
        String requestId = RequestIdHolder.currentRequestId();
        log.warn("Validation exception handled, requestId={}, message={}",
                requestId, exception.getMessage());
        return buildResponse(CommonErrorCode.PARAM_ERROR, requestId);
    }

    /**
     * 业务服务异常，按错误码内容返回统一失败结果。
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Result<Void>> handleServiceException(ServiceException exception) {
        String requestId = RequestIdHolder.currentRequestId();
        log.error("Service exception handled, requestId={}, code={}, message={}",
                requestId, exception.getErrorCode().getCode(),
                exception.getErrorCode().getMessage(), exception);
        return buildResponse(exception.getErrorCode(), requestId);
    }

    /**
     * 兜底处理未被显式捕获的异常，避免框架默认错误结构泄露到接口响应中。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception exception) {
        String requestId = RequestIdHolder.currentRequestId();
        log.error("Unhandled exception handled by global exception handler, requestId={}",
                requestId, exception);
        return buildResponse(CommonErrorCode.SYSTEM_ERROR, requestId);
    }

    private ResponseEntity<Result<Void>> buildResponse(ErrorCode errorCode, String requestId) {
        return ResponseEntity.ok(Result.failure(errorCode, requestId));
    }
}
