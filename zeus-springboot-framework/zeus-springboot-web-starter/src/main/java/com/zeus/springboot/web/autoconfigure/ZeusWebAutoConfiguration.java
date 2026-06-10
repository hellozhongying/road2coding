package com.zeus.springboot.web.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeus.springboot.web.core.ZeusWebMarker;
import com.zeus.springboot.web.exception.GlobalExceptionHandler;
import com.zeus.springboot.web.log.ApiLogAspect;
import com.zeus.springboot.web.log.RequestIdMdcFilter;
import com.zeus.springboot.web.response.ResponseWrapAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Zeus Web Starter 核心自动配置。
 *
 * <p>仅在 Servlet Web 应用中生效，负责装配统一异常处理、响应包装、请求追踪和 API 日志能力。</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@EnableConfigurationProperties(ZeusWebProperties.class)
public class ZeusWebAutoConfiguration {

    /**
     * 注册标记 Bean，便于外部判断 Zeus Web Starter 是否已加载。
     */
    @Bean
    @ConditionalOnMissingBean
    public ZeusWebMarker zeusWebMarker(ZeusWebProperties properties) {
        return new ZeusWebMarker(properties.isEnabled());
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestIdMdcFilter requestIdMdcFilter() {
        // 提前把 requestId 放入 MDC，保证业务日志和 @ApiLog 日志使用同一个追踪标识。
        return new RequestIdMdcFilter();
    }

    @Bean
    @ConditionalOnBean(ObjectMapper.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "zeus.web", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ResponseWrapAdvice responseWrapAdvice(ObjectMapper objectMapper, ZeusWebProperties properties) {
        return new ResponseWrapAdvice(objectMapper, properties);
    }

    /**
     * 装配 @ApiLog 切面，依赖 ObjectMapper 完成参数和结果序列化。
     */
    @Bean
    @ConditionalOnBean(ObjectMapper.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "zeus.web", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ApiLogAspect apiLogAspect(ObjectMapper objectMapper, ZeusWebProperties properties) {
        return new ApiLogAspect(objectMapper, properties);
    }
}
