package com.zeus.springboot.web.response;

import com.zeus.springboot.web.exception.ErrorCode;
import com.zeus.springboot.web.log.RequestIdMdcFilter;
import org.slf4j.MDC;

import java.time.Instant;

/**
 * 统一接口响应模型。
 *
 * @param code      业务状态码，成功固定为 {@code 200}
 * @param message   响应提示信息
 * @param data      成功时的业务数据
 * @param requestId 请求追踪 ID，便于前后端联动排查
 * @param timestamp 响应生成时间
 */
public record Result<T>(int code, String message, T data, String requestId, Instant timestamp) {

    private static final int SUCCESS_CODE = 200;

    private static final int ERROR_CODE = 500;

    private static final String SUCCESS_MESSAGE = "success";

    /**
     * 构造成功响应。
     */
    public static <T> Result<T> OK() {
        return success(null);
    }

    /**
     * 构造成功响应。
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data, currentRequestId(), Instant.now());
    }

    /**
     * 构造失败响应，默认错误码为 500。
     */
    public static <T> Result<T> error(String message) {
        return failure(message);
    }

    /**
     * 构造失败响应，默认错误码为 500。
     */
    public static <T> Result<T> failure(String message) {
        return failure(ERROR_CODE, message);
    }

    /**
     * 根据错误码构造失败响应。
     */
    public static <T> Result<T> failure(ErrorCode errorCode) {
        return failure(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 根据明确的错误码和提示信息构造失败响应。
     */
    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null, currentRequestId(), Instant.now());
    }

    private static String currentRequestId() {
        return MDC.get(RequestIdMdcFilter.REQUEST_ID_MDC_KEY);
    }
}
