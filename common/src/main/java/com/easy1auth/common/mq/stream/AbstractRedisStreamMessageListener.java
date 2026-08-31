package com.easy1auth.common.mq.stream;

import cn.hutool.core.util.TypeUtil;
import com.easy1auth.common.mq.RedisMQTemplate;
import com.easy1auth.common.mq.interceptor.RedisMessageInterceptor;
import com.easy1auth.common.mq.message.AbstractRedisMessage;
import com.easy1auth.common.util.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.List;

public abstract class AbstractRedisStreamMessageListener<T extends AbstractRedisStreamMessage>
        implements StreamListener<String, ObjectRecord<String, String>> {
    /**
     * 消息类型
     */
    private final Class<T> messageType;
    /**
     * Redis Channel
     */
    private final String streamKey;

    /**
     * Redis 消费者分组，默认使用 spring.application.name 名字
     */
    @Value("${spring.application.name}")
    private String group;
    /**
     * RedisMQTemplate
     */
    private RedisMQTemplate redisMQTemplate;

    public AbstractRedisStreamMessageListener() {
        try {
            this.messageType = getMessageClass();
            this.streamKey = messageType.getDeclaredConstructor().newInstance().getStreamKey();
        }catch(NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e){
            throw new RuntimeException(e);
        }

    }



    public String getStreamKey() {
        return streamKey;
    }

    public String getGroup() {
        return group;
    }

    public void setRedisMQTemplate(RedisMQTemplate redisMQTemplate) {
        this.redisMQTemplate = redisMQTemplate;
    }

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        // 消费消息
        T messageObj = JsonUtils.parseObject(message.getValue(),messageType);
        try {
            consumeMessageBefore(messageObj);
            // 消费消息
            this.onMessage(messageObj);
            // ack 消息消费完成
            redisMQTemplate.getRedisTemplate().opsForStream().acknowledge(group,message);
        }finally {
            consumeMessageAfter(messageObj);
        }
    }

    /**
     * 处理消息
     * @param message
     */
    public abstract void onMessage(T message);

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
