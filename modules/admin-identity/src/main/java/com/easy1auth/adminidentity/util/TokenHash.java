package com.easy1auth.adminidentity.util;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

/**
 * 令牌哈希工具。
 *
 * <p>对令牌（刷新令牌、注册码）取 SHA-256 摘要并转为十六进制字符串，
 * 用于落库存储，避免明文令牌泄露。</p>
 */
public final class TokenHash {
    private TokenHash() {
    }

    /** 计算给定令牌的 SHA-256 十六进制摘要。 */
    public static String sha256(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
