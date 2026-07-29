package com.easy1auth.adminidentity;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

final class TokenHash {
    private TokenHash() {}
    static String sha256(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
