package com.easy1auth.admin.mq.producer;

import com.easy1auth.admin.mq.message.MailSendMessage;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class MailSendProducer {

    @Resource
    private ApplicationContext applicationContext;

    public void sendMailMessage(MailSendMessage message){
        applicationContext.publishEvent(new MailSendMessage("","",""));
    }
}
