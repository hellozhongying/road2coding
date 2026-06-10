package com.zeus.springboot.web.autoconfigure;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate 自动配置。
 *
 * <p>在业务未自定义 {@code redisTemplate} 时，提供 key 为字符串、value 为 JSON 的通用 RedisTemplate。</p>
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.springframework.boot.autoconfigure.data.redis.LettuceConnectionConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.JedisConnectionConfiguration"
})
@AutoConfigureBefore(RedisAutoConfiguration.class)
@ConditionalOnClass({RedisOperations.class, RedisProperties.class})
public class ZeusRedisAutoConfiguration {

    /**
     * 创建默认 RedisTemplate，覆盖 Spring Boot 默认的 JDK 序列化体验。
     */
    @Bean(name = "redisTemplate")
    @ConditionalOnBean(ObjectMapper.class)
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(name = "redisTemplate")
    @SuppressWarnings({"rawtypes", "unchecked"})
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        RedisSerializer<String> stringSerializer = new StringRedisSerializer();
        RedisSerializer<Object> jsonSerializer = jsonRedisSerializer(objectMapper);

        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.setDefaultSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    private RedisSerializer<Object> jsonRedisSerializer(ObjectMapper objectMapper) {
        ObjectMapper redisObjectMapper = objectMapper.copy();
        GenericJackson2JsonRedisSerializer.registerNullValueSerializer(redisObjectMapper, null);
        // 写入类型信息，保证 Object 类型 value 反序列化时能恢复实际 Java 类型。
        redisObjectMapper.activateDefaultTypingAsProperty(
                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.Id.CLASS.getDefaultPropertyName());
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }
}
