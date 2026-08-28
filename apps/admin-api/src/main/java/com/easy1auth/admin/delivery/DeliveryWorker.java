package com.easy1auth.admin.delivery;

import com.easy1auth.audit.*;
import com.easy1auth.audit.model.DeliveryOutboxEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.easy1auth.admin.config.RegistrationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.HexFormat;

/**
 * 投递 worker（后台定时任务）。
 *
 * <p>周期性认领审计通知投递队列（outbox）中的待发送条目，按渠道发送邮件或带签名的
 * Webhook；发送成功则标记 sent，失败则记录失败原因。</p>
 */
@Component
public class DeliveryWorker {
    /** 审计投递服务，负责认领、标记与查询投递条目 */
    private final DeliveryService service;
    /** Spring 邮件发送器，用于邮件渠道 */
    private final JavaMailSender mail;
    /** JSON 序列化器，用于 Webhook 请求体与签名载荷 */
    private final ObjectMapper json;
    /** 注册配置，用于取发件人地址 */
    private final RegistrationProperties registration;
    /** 复用 HTTP 客户端：5 秒建连超时，不跟随重定向（Webhook 目标应为最终地址） */
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();

    DeliveryWorker(DeliveryService service, JavaMailSender mail, ObjectMapper json, RegistrationProperties registration) {
        this.service = service;
        this.mail = mail;
        this.json = json;
        this.registration = registration;
    }

    /** 按固定间隔（默认 2 秒）认领最多 20 条投递条目并逐条发送，逐条记录成败。 */
    @Scheduled(fixedDelayString = "${easy1auth.delivery.poll-delay:2000}")
    public void poll() {
        for (var item : service.claim(20)) {
            try {
                if ("email".equals(item.channel())) {
                    sendMail(item);
                } else {
                    sendWebhook(item);
                }
                service.sent(item.id());
            } catch (Exception ex) {
                service.failed(item.id(), ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }
    }

    /** 发送邮件：从投递条目的 payload 中取主题与正文，发往 destination 地址。 */
    private void sendMail(DeliveryOutboxEntity item) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(registration.fromAddress());
        m.setTo(item.destination());
        m.setSubject(String.valueOf(item.payload().get("subject")));
        m.setText(String.valueOf(item.payload().get("body")));
        mail.send(m);
    }

    /**
     * 发送 Webhook：先拒绝内网/回环等私网目标地址（防 SSRF），再携带时间戳、事件与
     * HMAC-SHA256 签名头 POST JSON 载荷，非 2xx 响应视为失败。
     */
    private void sendWebhook(DeliveryOutboxEntity item) throws Exception {
        URI uri = URI.create(item.destination());
        for (var a : java.net.InetAddress.getAllByName(uri.getHost())) {
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress()) {
                throw new IllegalArgumentException("private webhook destination");
            }
        }
        String body = json.writeValueAsString(item.payload()), timestamp = String.valueOf(Instant.now().getEpochSecond()), secret = service.webhookSecret(item);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + item.id() + "." + body).getBytes(StandardCharsets.UTF_8)));
        var response = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json").header("X-Easy1Auth-Event", item.eventType()).header("X-Easy1Auth-Delivery", item.id().toString()).header("X-Easy1Auth-Timestamp", timestamp).header("X-Easy1Auth-Signature", "sha256=" + signature).POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
    }
}
