package com.zeus.springboot.web.exception;

/**
 * 参数错误异常。
 *
 * <p>Controller 或业务校验发现请求参数不合法时抛出，统一异常处理器会通过 Result.code 表达参数错误。</p>
 */
public class ParamException extends RuntimeException {

    private final ErrorCode errorCode;

    public ParamException(String message) {
        this(CommonErrorCode.PARAM_ERROR, message);
    }

    public ParamException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ParamException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ParamException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = new MessageOverrideErrorCode(errorCode, message);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    private record MessageOverrideErrorCode(ErrorCode delegate, String message) implements ErrorCode {

        @Override
        public int getCode() {
            return delegate.getCode();
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
