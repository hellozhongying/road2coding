package com.zeus.springboot.web.exception;

import com.zeus.springboot.web.response.RequestIdHolder;
import com.zeus.springboot.web.response.Result;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ParamException.class)
    public ResponseEntity<Result<Void>> handleParamException(ParamException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getErrorCode());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<Result<Void>> handleValidationException(Exception exception) {
        // Bean Validation failures are client-side parameter errors, so they use the same public contract.
        return buildResponse(HttpStatus.BAD_REQUEST, CommonErrorCode.PARAM_ERROR);
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Result<Void>> handleServiceException(ServiceException exception) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception exception) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.SYSTEM_ERROR);
    }

    private ResponseEntity<Result<Void>> buildResponse(HttpStatus status, ErrorCode errorCode) {
        return ResponseEntity.status(status)
                .body(Result.failure(errorCode, RequestIdHolder.currentRequestId()));
    }
}
