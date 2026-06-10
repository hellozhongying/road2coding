package com.zeus.springboot.web.exception;

/**
 * 业务服务异常。
 *
 * <p>用于表达服务端业务处理失败，统一异常处理器会保留其中的错误码和错误信息。</p>
 */
public class ServiceException extends RuntimeException {

    private final ErrorCode errorCode;

    public ServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ServiceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
