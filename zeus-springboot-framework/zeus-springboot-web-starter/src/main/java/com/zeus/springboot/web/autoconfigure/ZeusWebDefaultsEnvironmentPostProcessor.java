package com.zeus.springboot.web.autoconfigure;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

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
            propertySources.addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULT_PROPERTIES));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
