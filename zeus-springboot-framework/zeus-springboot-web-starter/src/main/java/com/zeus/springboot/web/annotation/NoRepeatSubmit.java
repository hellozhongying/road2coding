package com.zeus.springboot.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 标记需要防重复提交的 Controller 方法。
 *
 * <p>被标记的方法会通过 Redis 原子写入短期提交凭证，避免用户在时间窗口内重复点击或重试提交。</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoRepeatSubmit {

    /**
     * 防重复提交窗口时间；小于等于 0 时使用全局配置。
     */
    long interval() default -1;

    /**
     * 时间单位。
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 重复提交时的提示信息；为空时使用全局配置。
     */
    String message() default "";

    /**
     * 是否把请求参数纳入防重 key。
     *
     * @deprecated 使用 {@link #paramStrategy()}，该属性会在后续版本移除。
     */
    @Deprecated(since = "0.0.1", forRemoval = false)
    boolean includeParams() default true;

    /**
     * 请求参数参与防重 key 的策略。
     */
    ParamStrategy paramStrategy() default ParamStrategy.DEFAULT;

    enum ParamStrategy {

        /**
         * 使用全局配置。
         */
        DEFAULT,

        /**
         * 强制把请求参数纳入防重 key。
         */
        INCLUDE,

        /**
         * 强制不把请求参数纳入防重 key。
         */
        EXCLUDE
    }
}
