package com.zeus.springboot.web.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeus.springboot.web.repeat.NoRepeatSubmitAspect;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证 @NoRepeatSubmit 自动配置仅在 Redis、ObjectMapper 和开关条件满足时生效。
 */
class ZeusNoRepeatSubmitAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ZeusNoRepeatSubmitAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));

    @Test
    void createsNoRepeatSubmitAspectWhenRedisAndObjectMapperExist() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(NoRepeatSubmitAspect.class));
    }

    @Test
    void backsOffNoRepeatSubmitAspectWhenDisabled() {
        contextRunner.withPropertyValues("zeus.web.no-repeat-submit.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(NoRepeatSubmitAspect.class));
    }

    @Test
    void backsOffNoRepeatSubmitAspectWhenStarterDisabled() {
        contextRunner.withPropertyValues("zeus.web.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(NoRepeatSubmitAspect.class));
    }

    @Test
    void backsOffNoRepeatSubmitAspectWithoutObjectMapper() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ZeusNoRepeatSubmitAutoConfiguration.class))
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .run(context -> assertThat(context).doesNotHaveBean(NoRepeatSubmitAspect.class));
    }

    @Test
    void doesNotRequireRedisOnClasspath() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ZeusNoRepeatSubmitAutoConfiguration.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withClassLoader(new FilteredClassLoader(RedisOperations.class))
                .run(context -> assertThat(context).doesNotHaveBean(NoRepeatSubmitAspect.class));
    }
}
