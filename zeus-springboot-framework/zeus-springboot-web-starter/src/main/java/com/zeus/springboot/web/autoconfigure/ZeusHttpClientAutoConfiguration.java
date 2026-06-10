package com.zeus.springboot.web.autoconfigure;

import com.zeus.springboot.web.http.ZeusHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@AutoConfiguration(after = RestClientAutoConfiguration.class)
@ConditionalOnClass({RestClient.class, CloseableHttpClient.class})
@ConditionalOnProperty(prefix = "zeus.http-client", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZeusHttpClientProperties.class)
public class ZeusHttpClientAutoConfiguration {

    /**
     * 创建带连接池的 Apache HttpClient 5 客户端，作为 Zeus 默认 HTTP 传输实现。
     */
    @Bean
    @ConditionalOnMissingBean(name = "zeusCloseableHttpClient")
    public CloseableHttpClient zeusCloseableHttpClient(ZeusHttpClientProperties properties) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(properties.getMaxTotal());
        connectionManager.setDefaultMaxPerRoute(properties.getMaxPerRoute());

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .evictExpiredConnections()
                .build();
    }

    /**
     * 将 Apache HttpClient 5 适配为 Spring HTTP 请求工厂，并应用超时配置。
     */
    @Bean
    @ConditionalOnMissingBean(name = "zeusClientHttpRequestFactory")
    public ClientHttpRequestFactory zeusClientHttpRequestFactory(
            @Qualifier("zeusCloseableHttpClient") CloseableHttpClient httpClient,
            ZeusHttpClientProperties properties) {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setConnectionRequestTimeout(properties.getConnectionRequestTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return requestFactory;
    }

    /**
     * 基于 Spring Boot 提供的 RestClient.Builder 创建 Zeus 默认 RestClient。
     */
    @Bean
    @ConditionalOnMissingBean(name = "zeusRestClient")
    public RestClient zeusRestClient(
            RestClient.Builder builder,
            @Qualifier("zeusClientHttpRequestFactory") ClientHttpRequestFactory requestFactory) {
        return builder.requestFactory(requestFactory).build();
    }

    /**
     * 提供面向业务代码的简易 HTTP 调用工具。
     */
    @Bean
    @ConditionalOnMissingBean
    public ZeusHttpClient zeusHttpClient(@Qualifier("zeusRestClient") RestClient restClient) {
        return new ZeusHttpClient(restClient);
    }
}
