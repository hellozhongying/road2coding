package com.zeus.springboot.web.exception;

/**
 * 参数错误异常。
 *
 * <p>Controller 或业务校验发现请求参数不合法时抛出，统一异常处理器会映射为 HTTP 400。</p>
 */
public class ParamException extends RuntimeException {

    private final ErrorCode errorCode;

    public ParamException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ParamException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
