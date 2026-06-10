package com.zeus.springboot.web.exception;

public enum CommonErrorCode implements ErrorCode {

    PARAM_ERROR("400", "参数错误"),
    SERVICE_ERROR("500", "服务异常"),
    SYSTEM_ERROR("500", "系统异常");

    private final String code;

    private final String message;

    CommonErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
