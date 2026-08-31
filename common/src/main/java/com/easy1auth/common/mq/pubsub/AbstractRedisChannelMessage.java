package com.easy1auth.common.mq.pubsub;

import com.easy1auth.common.mq.message.AbstractRedisMessage;
import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class AbstractRedisChannelMessage extends AbstractRedisMessage {

    /**
     * 获得 Redis Channel，默认使用类名
     * 避免序列化
     *
     * @return Channel
     */
    @JsonIgnore
    public String getChannel(){
        return getClass().getSimpleName();
    }
}
