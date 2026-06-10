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

@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.springframework.boot.autoconfigure.data.redis.LettuceConnectionConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.JedisConnectionConfiguration"
})
@AutoConfigureBefore(RedisAutoConfiguration.class)
@ConditionalOnClass({RedisOperations.class, RedisProperties.class})
public class ZeusRedisAutoConfiguration {

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
        redisObjectMapper.activateDefaultTypingAsProperty(
                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.Id.CLASS.getDefaultPropertyName());
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }
}
