package com.zeus.springboot.web.response;

import com.zeus.springboot.web.exception.ErrorCode;

import java.time.Instant;

public record Result<T>(String code, String message, T data, String requestId, Instant timestamp) {

    private static final String SUCCESS_CODE = "0";

    private static final String SUCCESS_MESSAGE = "success";

    public static <T> Result<T> success(T data, String requestId) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data, requestId, Instant.now());
    }

    public static Result<Void> failure(ErrorCode errorCode, String requestId) {
        return failure(errorCode.getCode(), errorCode.getMessage(), requestId);
    }

    public static Result<Void> failure(String code, String message, String requestId) {
        return new Result<>(code, message, null, requestId, Instant.now());
    }
}
