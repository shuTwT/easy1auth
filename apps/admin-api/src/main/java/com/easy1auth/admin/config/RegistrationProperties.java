package com.easy1auth.admin.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("easy1auth.registration") public record RegistrationProperties(boolean exposeCode,String fromAddress){}
