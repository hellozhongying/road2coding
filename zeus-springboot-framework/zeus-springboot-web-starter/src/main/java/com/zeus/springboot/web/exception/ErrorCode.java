package com.zeus.springboot.web.exception;

/**
 * Zeus Web 异常体系使用的错误码契约。
 */
public interface ErrorCode {

    /**
     * 返回对外暴露的业务错误码。
     */
    String getCode();

    /**
     * 返回对外暴露的错误提示信息。
     */
    String getMessage();
}
