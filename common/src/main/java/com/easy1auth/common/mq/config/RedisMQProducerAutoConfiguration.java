package com.easy1auth.common.mq.config;

import com.easy1auth.common.mq.RedisMQTemplate;
import com.easy1auth.common.mq.interceptor.RedisMessageInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@AutoConfiguration
public class RedisMQProducerAutoConfiguration {

    public RedisMQTemplate redisMQTemplate(StringRedisTemplate redisTemplate,
                                           List<RedisMessageInterceptor> interceptors){
        RedisMQTemplate redisMQTemplate = new RedisMQTemplate(redisTemplate);
        // 添加拦截器
        interceptors.forEach(redisMQTemplate::addInterceptor);
        return redisMQTemplate;
    }
}
