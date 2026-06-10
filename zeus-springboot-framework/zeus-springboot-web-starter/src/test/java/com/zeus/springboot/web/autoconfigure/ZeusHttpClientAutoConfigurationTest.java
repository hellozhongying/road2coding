package com.zeus.springboot.web.autoconfigure;

import java.time.Duration;

import com.zeus.springboot.web.http.ZeusHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Zeus HTTP Client 自动配置的 Bean 装配、开关和属性绑定行为。
 */
class ZeusHttpClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RestClientAutoConfiguration.class,
                    ZeusHttpClientAutoConfiguration.class));

    @Test
    void createsHttpClientBeansByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CloseableHttpClient.class);
            assertThat(context).hasSingleBean(ClientHttpRequestFactory.class);
            assertThat(context).hasSingleBean(ZeusHttpClient.class);
            assertThat(context).hasBean("zeusRestClient");
        });
    }

    @Test
    void backsOffWhenHttpClientDisabled() {
        contextRunner.withPropertyValues("zeus.http-client.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CloseableHttpClient.class);
                    assertThat(context).doesNotHaveBean(ClientHttpRequestFactory.class);
                    assertThat(context).doesNotHaveBean(ZeusHttpClient.class);
                    assertThat(context).doesNotHaveBean("zeusRestClient");
                });
    }

    @Test
    void bindsHttpClientProperties() {
        contextRunner.withPropertyValues(
                        "zeus.http-client.max-total=300",
                        "zeus.http-client.max-per-route=80",
                        "zeus.http-client.connect-timeout=5s",
                        "zeus.http-client.connection-request-timeout=2s",
                        "zeus.http-client.read-timeout=20s")
                .run(context -> {
                    ZeusHttpClientProperties properties = context.getBean(ZeusHttpClientProperties.class);

                    assertThat(properties.getMaxTotal()).isEqualTo(300);
                    assertThat(properties.getMaxPerRoute()).isEqualTo(80);
                    assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
                    assertThat(properties.getConnectionRequestTimeout()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(20));
                });
    }

    @Test
    void backsOffWhenApplicationProvidesZeusHttpClient() {
        contextRunner.withBean(ZeusHttpClient.class, () -> new ZeusHttpClient(RestClient.create()))
                .run(context -> assertThat(context).hasSingleBean(ZeusHttpClient.class));
    }
}
