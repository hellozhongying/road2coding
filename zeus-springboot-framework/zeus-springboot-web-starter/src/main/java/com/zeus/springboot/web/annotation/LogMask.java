package com.zeus.springboot.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记日志中需要脱敏的字段或 JavaBean getter。
 *
 * <p>{@link ApiLog} 序列化请求参数和响应结果时，会把被标记成员替换为配置的掩码文本。</p>
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogMask {
}
