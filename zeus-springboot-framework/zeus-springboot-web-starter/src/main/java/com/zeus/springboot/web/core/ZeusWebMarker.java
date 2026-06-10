package com.zeus.springboot.web.core;

/**
 * Zeus Web Starter 是否生效的标记 Bean。
 *
 * <p>业务侧或测试中可以通过该 Bean 判断自动配置是否已装配。</p>
 */
public record ZeusWebMarker(boolean enabled) {
}
