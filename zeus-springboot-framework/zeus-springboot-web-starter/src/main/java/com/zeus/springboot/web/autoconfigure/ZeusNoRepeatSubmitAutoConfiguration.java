package com.zeus.springboot.web.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeus.springboot.web.repeat.NoRepeatSubmitAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * {@code @NoRepeatSubmit} 防重复提交自动配置。
 *
 * <p>Redis 是可选能力，因此该配置仅在业务应用引入 Redis 并提供 {@link StringRedisTemplate} 时生效。</p>
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(StringRedisTemplate.class)
@EnableConfigurationProperties(ZeusWebProperties.class)
public class ZeusNoRepeatSubmitAutoConfiguration {

    /**
     * 装配 @NoRepeatSubmit 切面，依赖 Redis 原子写入完成短时间窗口内的重复提交判断。
     */
    @Bean
    @ConditionalOnBean({ObjectMapper.class, StringRedisTemplate.class})
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "zeus.web", name = {"enabled", "no-repeat-submit.enabled"}, havingValue = "true", matchIfMissing = true)
    public NoRepeatSubmitAspect noRepeatSubmitAspect(StringRedisTemplate stringRedisTemplate,
                                                     ObjectMapper objectMapper,
                                                     ZeusWebProperties properties) {
        return new NoRepeatSubmitAspect(stringRedisTemplate, objectMapper, properties);
    }
}
