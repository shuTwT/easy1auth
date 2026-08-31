package com.easy1auth.common.mq.stream;

import com.easy1auth.common.mq.message.AbstractRedisMessage;
import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class AbstractRedisStreamMessage extends AbstractRedisMessage {

    /**
     * 获得 Redis Stream Key，默认使用类名
     *
     * @return Channel
     */
    @JsonIgnore // 避免序列化
    public String getStreamKey() {
        return getClass().getSimpleName();
    }
}
