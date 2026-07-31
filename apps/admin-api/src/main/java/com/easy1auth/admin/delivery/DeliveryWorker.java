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

@Component
public class DeliveryWorker {
    private final DeliveryService service;
    private final JavaMailSender mail;
    private final ObjectMapper json;
    private final RegistrationProperties registration;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();

    DeliveryWorker(DeliveryService service, JavaMailSender mail, ObjectMapper json, RegistrationProperties registration) {
        this.service = service;
        this.mail = mail;
        this.json = json;
        this.registration = registration;
    }

    @Scheduled(fixedDelayString = "${easy1auth.delivery.poll-delay:2000}")
    public void poll() {
        for (var item : service.claim(20))
            try {
                if ("email".equals(item.channel())) sendMail(item);
                else sendWebhook(item);
                service.sent(item.id());
            } catch (Exception ex) {
                service.failed(item.id(), ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
    }

    private void sendMail(DeliveryOutboxEntity item) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(registration.fromAddress());
        m.setTo(item.destination());
        m.setSubject(String.valueOf(item.payload().get("subject")));
        m.setText(String.valueOf(item.payload().get("body")));
        mail.send(m);
    }

    private void sendWebhook(DeliveryOutboxEntity item) throws Exception {
        URI uri = URI.create(item.destination());
        for (var a : java.net.InetAddress.getAllByName(uri.getHost()))
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress())
                throw new IllegalArgumentException("private webhook destination");
        String body = json.writeValueAsString(item.payload()), timestamp = String.valueOf(Instant.now().getEpochSecond()), secret = service.webhookSecret(item);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + item.id() + "." + body).getBytes(StandardCharsets.UTF_8)));
        var response = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json").header("X-Easy1Auth-Event", item.eventType()).header("X-Easy1Auth-Delivery", item.id().toString()).header("X-Easy1Auth-Timestamp", timestamp).header("X-Easy1Auth-Signature", "sha256=" + signature).POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IllegalStateException("HTTP " + response.statusCode());
    }
}
