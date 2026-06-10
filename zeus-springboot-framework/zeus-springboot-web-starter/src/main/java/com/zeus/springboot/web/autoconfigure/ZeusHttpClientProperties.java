package com.zeus.springboot.web.autoconfigure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zeus.http-client")
public class ZeusHttpClientProperties {

    /**
     * Zeus HTTP Client 自动配置开关。
     */
    private boolean enabled = true;

    /**
     * HTTP 连接池最大总连接数。
     */
    private int maxTotal = 200;

    /**
     * 每个路由允许的最大连接数。
     */
    private int maxPerRoute = 50;

    /**
     * 建立连接的超时时间。
     */
    private Duration connectTimeout = Duration.ofSeconds(3);

    /**
     * 从连接池获取连接的超时时间。
     */
    private Duration connectionRequestTimeout = Duration.ofSeconds(3);

    /**
     * 读取响应数据的超时时间。
     */
    private Duration readTimeout = Duration.ofSeconds(10);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public int getMaxPerRoute() {
        return maxPerRoute;
    }

    public void setMaxPerRoute(int maxPerRoute) {
        this.maxPerRoute = maxPerRoute;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(Duration connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}
