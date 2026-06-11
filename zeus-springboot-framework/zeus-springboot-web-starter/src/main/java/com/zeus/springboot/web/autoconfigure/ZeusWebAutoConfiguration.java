package com.zeus.springboot.web.autoconfigure;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
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
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

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

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    @Bean
    @ConditionalOnClass(Jackson2ObjectMapperBuilder.class)
    @ConditionalOnMissingBean(name = "zeusJacksonDateTimeCustomizer")
    public Jackson2ObjectMapperBuilderCustomizer zeusJacksonDateTimeCustomizer() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        TimeZone shanghaiTimeZone = TimeZone.getTimeZone(SHANGHAI_ZONE);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
        dateFormat.setTimeZone(shanghaiTimeZone);

        return builder -> builder
                .timeZone(shanghaiTimeZone)
                .dateFormat(dateFormat)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter))
                .serializerByType(Instant.class, new ShanghaiInstantSerializer(dateTimeFormatter))
                .serializerByType(OffsetDateTime.class, new ShanghaiOffsetDateTimeSerializer(dateTimeFormatter))
                .serializerByType(ZonedDateTime.class, new ShanghaiZonedDateTimeSerializer(dateTimeFormatter));
    }

    private static class ShanghaiInstantSerializer extends JsonSerializer<Instant> {

        private final DateTimeFormatter formatter;

        private ShanghaiInstantSerializer(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(formatter.format(value.atZone(SHANGHAI_ZONE)));
        }
    }

    private static class ShanghaiOffsetDateTimeSerializer extends JsonSerializer<OffsetDateTime> {

        private final DateTimeFormatter formatter;

        private ShanghaiOffsetDateTimeSerializer(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public void serialize(OffsetDateTime value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(formatter.format(value.atZoneSameInstant(SHANGHAI_ZONE)));
        }
    }

    private static class ShanghaiZonedDateTimeSerializer extends JsonSerializer<ZonedDateTime> {

        private final DateTimeFormatter formatter;

        private ShanghaiZonedDateTimeSerializer(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public void serialize(ZonedDateTime value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(formatter.format(value.withZoneSameInstant(SHANGHAI_ZONE)));
        }
    }

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
