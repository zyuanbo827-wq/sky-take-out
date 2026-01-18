package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {

    /**
     * 配置RedisTemplate
     * @param redisConnectionFactory
     */
    @Bean
    public RedisTemplate setRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始配置RedisTemplate...");
        //设置RedisTemplate的序列化器
        RedisTemplate redisTemplate = new RedisTemplate();

        redisTemplate.setKeySerializer(new StringRedisSerializer());

        redisTemplate.setConnectionFactory(redisConnectionFactory);

        return redisTemplate;
    }
}
