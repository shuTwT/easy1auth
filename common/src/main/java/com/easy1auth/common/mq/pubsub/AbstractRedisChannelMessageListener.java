package com.easy1auth.common.mq.pubsub;

import cn.hutool.core.util.TypeUtil;
import com.easy1auth.common.mq.RedisMQTemplate;
import com.easy1auth.common.mq.interceptor.RedisMessageInterceptor;
import com.easy1auth.common.mq.message.AbstractRedisMessage;
import com.easy1auth.common.util.JsonUtils;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.List;

public abstract class AbstractRedisChannelMessageListener <T extends AbstractRedisChannelMessage> implements MessageListener {

    /**
     * 消息类型
     */
    private final Class<T> messageType;
    /**
     * Redis Channel
     */
    private final String channel;
    /**
     * RedisMQTemplate
     */
    private RedisMQTemplate redisMQTemplate;

    protected AbstractRedisChannelMessageListener() {
        try {
            this.messageType = getMessageClass();
            this.channel = messageType.getDeclaredConstructor().newInstance().getChannel();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }

    public void setRedisMQTemplate(RedisMQTemplate redisMQTemplate) {
        this.redisMQTemplate = redisMQTemplate;
    }

    @Override
    public final void onMessage(Message message, byte[] bytes) {
        T messageObj = JsonUtils.parseObject(message.getBody(), messageType);
        try {
            consumeMessageBefore(messageObj);
            // 消费消息
            this.onMessage(messageObj);
        } finally {
            consumeMessageAfter(messageObj);
        }
    }

    /**
     * 处理消息
     *
     * @param message 消息
     */
    public abstract void onMessage(T message);

    /**
     * 通过解析类上的泛型，获得消息类型
     *
     * @return 消息类型
     */
    @SuppressWarnings("unchecked")
    private Class<T> getMessageClass() {
        Type type = TypeUtil.getTypeArgument(getClass(), 0);
        if (type == null) {
            throw new IllegalStateException(String.format("类型(%s) 需要设置消息类型", getClass().getName()));
        }
        return (Class<T>) type;
    }

    private void consumeMessageBefore(AbstractRedisMessage message) {
        assert redisMQTemplate != null;
        List<RedisMessageInterceptor> interceptors = redisMQTemplate.getInterceptors();
        // 正序
        interceptors.forEach(interceptor -> interceptor.consumeMessageBefore(message));
    }

    private void consumeMessageAfter(AbstractRedisMessage message) {
        assert redisMQTemplate != null;
        List<RedisMessageInterceptor> interceptors = redisMQTemplate.getInterceptors();
        // 倒序
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            interceptors.get(i).consumeMessageAfter(message);
        }
    }
}
