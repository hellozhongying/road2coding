package com.zeus.springboot.web.autoconfigure;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * 为 Web 应用注入一组温和的默认配置。
 *
 * <p>默认属性以最低优先级加入，业务应用在 application.yml 中显式配置时会自然覆盖这些值。</p>
 */
public class ZeusWebDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "zeusWebDefaults";

    private static final Map<String, Object> DEFAULT_PROPERTIES = Map.of(
            "server.port", "8080",
            "server.tomcat.threads.max", "50",
            "server.tomcat.threads.min-spare", "5",
            "server.shutdown", "graceful"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        MutablePropertySources propertySources = environment.getPropertySources();
        if (!propertySources.contains(PROPERTY_SOURCE_NAME)) {
            // 放在最后，保证用户配置、环境变量、命令行参数等更高优先级配置都能覆盖默认值。
            propertySources.addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULT_PROPERTIES));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
