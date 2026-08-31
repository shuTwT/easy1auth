package com.easy1auth.admin.mq.consumer;

import com.easy1auth.admin.config.RegistrationProperties;
import com.easy1auth.admin.mq.message.MailSendMessage;
import jakarta.annotation.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class MailSendConsumer {

    @Resource
    private JavaMailSender mailSender;
    @Resource
    private RegistrationProperties registration;

    public void onMessage(MailSendMessage message){
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(registration.fromAddress());
        mail.setTo(message.recipient());
        mail.setSubject(message.subject());
        mail.setText(message.body());
        mailSender.send(mail);
    }
}
