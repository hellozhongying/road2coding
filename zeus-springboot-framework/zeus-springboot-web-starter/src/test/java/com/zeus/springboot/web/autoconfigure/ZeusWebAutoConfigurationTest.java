package com.zeus.springboot.web.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeus.springboot.web.core.ZeusWebMarker;
import com.zeus.springboot.web.exception.CommonErrorCode;
import com.zeus.springboot.web.exception.GlobalExceptionHandler;
import com.zeus.springboot.web.exception.ParamException;
import com.zeus.springboot.web.exception.ServiceException;
import com.zeus.springboot.web.log.ApiLogAspect;
import com.zeus.springboot.web.log.RequestIdMdcFilter;
import com.zeus.springboot.web.response.ResponseWrapAdvice;
import com.zeus.springboot.web.response.Result;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Zeus Web 核心自动配置的默认装配、开关退让和默认环境属性。
 */
class ZeusWebAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ZeusWebAutoConfiguration.class));

    private final WebApplicationContextRunner jacksonContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, ZeusWebAutoConfiguration.class));

    @Test
    void createsMarkerBeanByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ZeusWebMarker.class);
            assertThat(context.getBean(ZeusWebMarker.class).enabled()).isTrue();
        });
    }

    @Test
    void bindsEnabledProperty() {
        contextRunner.withPropertyValues("zeus.web.enabled=false")
                .run(context -> assertThat(context.getBean(ZeusWebMarker.class).enabled()).isFalse());
    }

    @Test
    void createsGlobalExceptionHandler() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(GlobalExceptionHandler.class));
    }

    @Test
    void createsRequestIdMdcFilter() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(RequestIdMdcFilter.class));
    }

    @Test
    void createsApiLogAspectWhenObjectMapperExists() {
        contextRunner.withBean(ObjectMapper.class)
                .run(context -> assertThat(context).hasSingleBean(ApiLogAspect.class));
    }

    @Test
    void createsResponseWrapAdviceWhenObjectMapperExists() {
        contextRunner.withBean(ObjectMapper.class)
                .run(context -> assertThat(context).hasSingleBean(ResponseWrapAdvice.class));
    }

    @Test
    void backsOffApiLogAspectWhenStarterDisabled() {
        contextRunner.withBean(ObjectMapper.class)
                .withPropertyValues("zeus.web.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ApiLogAspect.class));
    }

    @Test
    void backsOffResponseWrapAdviceWhenStarterDisabled() {
        contextRunner.withBean(ObjectMapper.class)
                .withPropertyValues("zeus.web.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ResponseWrapAdvice.class));
    }

    @Test
    void configuresGlobalJsonDateTimeFormatWithShanghaiTimeZone() {
        jacksonContextRunner.run(context -> {
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

            assertThat(objectMapper.writeValueAsString(new Date(0L))).isEqualTo("\"1970-01-01 08:00:00\"");
            assertThat(objectMapper.writeValueAsString(Instant.EPOCH)).isEqualTo("\"1970-01-01 08:00:00\"");
            assertThat(objectMapper.writeValueAsString(LocalDateTime.of(2026, 6, 11, 9, 8, 7)))
                    .isEqualTo("\"2026-06-11 09:08:07\"");
        });
    }

    @Test
    void handlesParamExceptionWithErrorCode() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Result<Void>> response = handler.handleParamException(
                new ParamException(CommonErrorCode.PARAM_ERROR));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("400");
        assertThat(response.getBody().message()).isEqualTo("参数错误");
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().requestId()).isNotBlank();
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void handlesParamExceptionWithCustomMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Result<Void>> response = handler.handleParamException(
                new ParamException(CommonErrorCode.PARAM_ERROR, "请勿重复提交"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("400");
        assertThat(response.getBody().message()).isEqualTo("请勿重复提交");
    }

    @Test
    void handlesParamExceptionWithDefaultErrorCodeAndCustomMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Result<Void>> response = handler.handleParamException(
                new ParamException("用户名不能为空"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("400");
        assertThat(response.getBody().message()).isEqualTo("用户名不能为空");
    }

    @Test
    void handlesServiceExceptionWithDefaultErrorCodeAndCustomMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Result<Void>> response = handler.handleServiceException(
                new ServiceException("订单支付失败"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("500");
        assertThat(response.getBody().message()).isEqualTo("订单支付失败");
    }

    @Test
    void providesWebServerDefaults() {
        MockEnvironment environment = new MockEnvironment();

        new ZeusWebDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("server.port")).isEqualTo("8080");
        assertThat(environment.getProperty("server.tomcat.threads.max")).isEqualTo("50");
        assertThat(environment.getProperty("server.tomcat.threads.min-spare")).isEqualTo("5");
        assertThat(environment.getProperty("server.shutdown")).isEqualTo("graceful");
    }

    @Test
    void backsOffWebServerDefaultsWhenApplicationProvidesValues() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.port", "9090")
                .withProperty("server.tomcat.threads.max", "100")
                .withProperty("server.tomcat.threads.min-spare", "10")
                .withProperty("server.shutdown", "immediate");

        new ZeusWebDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("server.port")).isEqualTo("9090");
        assertThat(environment.getProperty("server.tomcat.threads.max")).isEqualTo("100");
        assertThat(environment.getProperty("server.tomcat.threads.min-spare")).isEqualTo("10");
        assertThat(environment.getProperty("server.shutdown")).isEqualTo("immediate");
    }
}
