package com.easy1auth.system.dto;
import java.time.*;

public record IssuedCode(String email, String code, Instant expiresAt) {
    }
