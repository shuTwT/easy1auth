package com.easy1auth.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.time.Instant;
import java.util.*;

/**
 * TOTP（基于时间的一次性密码）服务。
 *
 * <p>负责生成 Base32 编码的 TOTP 密钥、按 30 秒时间步长计算 6 位验证码，
 * 以及在校验时允许前后各一个时间步的容差（防止时钟偏移导致误判）。</p>
 */
@Component
public final class TotpService {
    /** Base32 编码字母表（RFC 4648，不含填充符） */
    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    /** 安全随机源（生成密钥） */
    private final SecureRandom random = new SecureRandom();

    /** 生成 20 字节随机数并编码为 32 字符 Base32 密钥（160 位熵）。 */
    public String secret() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        StringBuilder out = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte v : bytes) {
            buffer = (buffer << 8) | (v & 255);
            bits += 8;
            while (bits >= 5) {
                out.append(BASE32[(buffer >> (bits -= 5)) & 31]);
            }
        }
        if (bits > 0) {
            out.append(BASE32[(buffer << (5 - bits)) & 31]);
        }
        return out.toString();
    }

    /** 计算指定时刻对应的 TOTP 时间步长（Unix 秒数除以 30）。 */
    public long step(Instant now) {
        return now.getEpochSecond() / 30;
    }

    /**
     * 校验验证码：当前及前后各一个时间步内，且必须晚于上次使用的时间步。
     *
     * @param lastUsed 上次成功使用的时间步，用于防重放（可为 null）
     */
    public boolean verify(String secret, String code, Instant now, Long lastUsed) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long current = step(now);
        for (long s = current - 1; s <= current + 1; s++) {
            if ((lastUsed == null || s > lastUsed) && code(secret, s).equals(code)) {
                return true;
            }
        }
        return false;
    }

    /** 按 HmacSHA1 计算指定时间步的 6 位 TOTP 验证码。 */
    public String code(String secret, long step) {
        try {
            byte[] key = decode(secret);
            byte[] data = new byte[8];
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) step;
                step >>>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int o = hash[19] & 15;
            int binary = ((hash[o] & 127) << 24) | ((hash[o + 1] & 255) << 16) | ((hash[o + 2] & 255) << 8) | (hash[o + 3] & 255);
            return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** 将 Base32 密钥解码为字节数组（忽略非法字符与填充）。 */
    private static byte[] decode(String value) {
        int buffer = 0, bits = 0, pos = 0;
        byte[] out = new byte[value.length() * 5 / 8];
        for (char ch : value.toUpperCase(Locale.ROOT).toCharArray()) {
            int v = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(ch);
            if (v < 0) {
                continue;
            }
            buffer = (buffer << 5) | v;
            bits += 5;
            if (bits >= 8) {
                out[pos++] = (byte) (buffer >> (bits -= 8));
            }
        }
        return Arrays.copyOf(out, pos);
    }
}
