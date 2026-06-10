package com.zeus.springboot.web.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeus.springboot.web.core.ZeusWebMarker;
import com.zeus.springboot.web.exception.CommonErrorCode;
import com.zeus.springboot.web.exception.GlobalExceptionHandler;
import com.zeus.springboot.web.exception.ParamException;
import com.zeus.springboot.web.log.ApiLogAspect;
import com.zeus.springboot.web.log.RequestIdMdcFilter;
import com.zeus.springboot.web.response.ResponseWrapAdvice;
import com.zeus.springboot.web.response.Result;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ZeusWebAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ZeusWebAutoConfiguration.class));

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
