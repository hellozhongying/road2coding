package com.zeus.springboot.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 跳过统一响应包装。
 *
 * <p>可标记在 Controller 类或方法上，适合文件下载、三方回调、健康检查等需要保留原始响应体的场景。</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreResponseWrap {
}
