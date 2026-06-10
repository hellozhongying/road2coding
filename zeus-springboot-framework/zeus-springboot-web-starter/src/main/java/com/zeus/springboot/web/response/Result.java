package com.zeus.springboot.web.response;

import com.zeus.springboot.web.exception.ErrorCode;

import java.time.Instant;

/**
 * 统一接口响应模型。
 *
 * @param code      业务状态码，成功固定为 {@code 0}
 * @param message   响应提示信息
 * @param data      成功时的业务数据
 * @param requestId 请求追踪 ID，便于前后端联动排查
 * @param timestamp 响应生成时间
 */
public record Result<T>(String code, String message, T data, String requestId, Instant timestamp) {

    private static final String SUCCESS_CODE = "0";

    private static final String SUCCESS_MESSAGE = "success";

    /**
     * 构造成功响应。
     */
    public static <T> Result<T> success(T data, String requestId) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data, requestId, Instant.now());
    }

    /**
     * 根据错误码构造失败响应。
     */
    public static Result<Void> failure(ErrorCode errorCode, String requestId) {
        return failure(errorCode.getCode(), errorCode.getMessage(), requestId);
    }

    /**
     * 根据明确的错误码和提示信息构造失败响应。
     */
    public static Result<Void> failure(String code, String message, String requestId) {
        return new Result<>(code, message, null, requestId, Instant.now());
    }
}
