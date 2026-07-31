package com.easy1auth.foundation.security;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * Production-only validation for operator supplied secret material.
 */
public final class SecretPolicy {
    private static final Set<String> PLACEHOLDERS = Set.of(
            "changeme", "change-me", "change_me", "replace-me", "replace_me",
            "example", "example-secret", "secret", "password", "easy1auth"
    );

    private SecretPolicy() {
    }

    public static String require(String name, String value, int minimumBytes) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        if (value.getBytes(StandardCharsets.UTF_8).length < minimumBytes) {
            throw new IllegalStateException(name + " must contain at least " + minimumBytes + " UTF-8 bytes");
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        if (PLACEHOLDERS.contains(normalized) || normalized.startsWith("example-")
                || normalized.startsWith("replace-") || normalized.startsWith("change-me")) {
            throw new IllegalStateException(name + " must not use an example or placeholder value");
        }
        return value;
    }
}
