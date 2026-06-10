package com.zeus.springboot.web.response;

/**
 * 简化版错误响应模型。
 *
 * <p>保留给需要只返回错误码和错误信息的场景；默认统一响应使用 {@link Result}。</p>
 */
public record ErrorResponse(String code, String message) {
}
