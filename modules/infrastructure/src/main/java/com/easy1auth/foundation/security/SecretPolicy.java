package com.easy1auth.foundation.security;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * 生产环境密钥材料校验：用于校验运维提供的密钥是否安全可靠。
 *
 * <p>校验规则包括：必填、最小长度（UTF-8 字节数），以及拒绝常见占位符/示例值
 * （如 changeme、password 等），避免将弱密钥带入生产环境。</p>
 */
public final class SecretPolicy {
    /** 常见占位符/示例值黑名单（大小写不敏感，用于拦截弱密钥） */
    private static final Set<String> PLACEHOLDERS = Set.of(
            "changeme", "change-me", "change_me", "replace-me", "replace_me",
            "example", "example-secret", "secret", "password", "easy1auth"
    );

    private SecretPolicy() {
    }

    /**
     * 校验指定密钥：必填、长度达标且非占位符，通过后原样返回。
     *
     * @param name         密钥名称（用于错误提示）
     * @param value        待校验的密钥值
     * @param minimumBytes 要求的最小 UTF-8 字节数
     * @return 校验通过后的原始密钥值
     * @throws IllegalStateException 校验不通过时抛出
     */
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
