package com.easy1auth.common.mq.interceptor;

import com.easy1auth.common.mq.message.AbstractRedisMessage;

/**
 * redis消息拦截器
 */
public interface RedisMessageInterceptor {
    default void sendMessageBefore(AbstractRedisMessage message) {
    }

    default void sendMessageAfter(AbstractRedisMessage message) {
    }

    default void consumeMessageBefore(AbstractRedisMessage message) {
    }

    default void consumeMessageAfter(AbstractRedisMessage message) {
    }
}
