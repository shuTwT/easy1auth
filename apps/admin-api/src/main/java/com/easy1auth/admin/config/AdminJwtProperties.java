package com.easy1auth.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties("easy1auth.admin-jwt")
public record AdminJwtProperties(String issuer, String audience, String secret, Duration accessTtl, Duration refreshTtl) {}
