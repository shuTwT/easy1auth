package com.easy1auth.adminidentity.dto;
import java.time.*;

public record IssuedCode(String email, String code, Instant expiresAt) {
    }
