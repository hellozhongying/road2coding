package com.zeus.springboot.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要记录 API 访问日志的 Controller 方法。
 *
 * <p>被标记的方法会通过 {@code ApiLogAspect} 输出请求参数、响应结果、客户端 IP 和耗时。</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiLog {

    /**
     * 日志中展示的 API 名称；为空时默认使用 {@code 类名#方法名}。
     */
    String value() default "";
}
