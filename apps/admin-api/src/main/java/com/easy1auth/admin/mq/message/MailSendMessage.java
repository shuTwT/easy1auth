package com.easy1auth.admin.mq.message;

/** 请求异步发送一封纯文本邮件的消息。 */
public record MailSendMessage(String recipient, String subject, String body) {
}
