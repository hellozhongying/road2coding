package com.zeus.springboot.web.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证 RedisTemplate 自动配置在默认、用户覆盖和缺失依赖场景下的行为。
 */
class ZeusRedisAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ZeusRedisAutoConfiguration.class))
            .withBean(ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules())
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class));

    @Test
    void createsJsonRedisTemplateWhenRedisExists() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("redisTemplate");

            RedisTemplate<?, ?> redisTemplate = context.getBean("redisTemplate", RedisTemplate.class);
            assertThat(redisTemplate.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
            assertThat(redisTemplate.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
            assertThat(redisTemplate.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
            assertThat(redisTemplate.getHashValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
        });
    }

    @Test
    void jsonRedisTemplateBacksOffSpringBootDefaultRedisTemplate() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ZeusRedisAutoConfiguration.class, RedisAutoConfiguration.class))
                .withBean(ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules())
                .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .run(context -> {
                    RedisTemplate<?, ?> redisTemplate = context.getBean("redisTemplate", RedisTemplate.class);

                    assertThat(redisTemplate.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
                    assertThat(redisTemplate.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
                });
    }

    @Test
    void serializesAndDeserializesUserObjectsAsJson() {
        contextRunner.run(context -> {
            RedisTemplate<String, SampleUser> redisTemplate = context.getBean(RedisTemplate.class);
            @SuppressWarnings("unchecked")
            RedisSerializer<Object> valueSerializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();

            // 序列化器需要保留类型信息，才能从 Object value 中还原为实际业务对象。
            SampleUser user = (SampleUser) valueSerializer.deserialize(valueSerializer.serialize(new SampleUser("zeus", 18)));

            assertThat(user.name()).isEqualTo("zeus");
            assertThat(user.age()).isEqualTo(18);
        });
    }

    @Test
    void backsOffWhenUserProvidesRedisTemplate() {
        contextRunner.withUserConfiguration(CustomRedisTemplateConfiguration.class)
                .run(context -> assertThat(context).hasSingleBean(RedisTemplate.class));
    }

    @Test
    void doesNotCreateRedisTemplateWithoutConnectionFactory() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ZeusRedisAutoConfiguration.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .run(context -> assertThat(context).doesNotHaveBean(RedisTemplate.class));
    }

    @Test
    void doesNotRequireRedisOnClasspath() {
        contextRunner.withClassLoader(new FilteredClassLoader(RedisOperations.class))
                .run(context -> assertThat(context).doesNotHaveBean("redisTemplate"));
    }

    record SampleUser(String name, int age) {
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomRedisTemplateConfiguration {

        @Bean(name = "redisTemplate")
        RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(redisConnectionFactory);
            return template;
        }
    }
}
