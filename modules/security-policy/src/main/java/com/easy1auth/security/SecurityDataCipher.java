package com.easy1auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

/**
 * 安全数据加密器：基于配置密钥对敏感材料做 AES-256-GCM 加密与解密。
 *
 * <p>每次加密生成随机 12 字节 IV，并将 IV 与密文一起 Base64 编码输出；
 * 支持传入 AAD（附加认证数据，如主体上下文）防止密文被跨场景重放。</p>
 */
@Component
public final class SecurityDataCipher {
    /** 由配置密钥经 SHA-256 派生的 32 字节 AES 密钥 */
    private final byte[] key;
    /** 安全随机源（生成 IV） */
    private final SecureRandom random = new SecureRandom();

    /** 构造器：配置密钥至少 32 个 UTF-8 字节，否则拒绝启动。 */
    public SecurityDataCipher(@Value("${easy1auth.security.data-encryption-secret:}") String secret) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("SECURITY_DATA_ENCRYPTION_SECRET must contain at least 32 UTF-8 bytes");
        }
        try {
            key = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** 用 AES-GCM 加密明文，返回「12 字节 IV + 密文」的 Base64 字符串。 */
    public String encrypt(String aad, String value) {
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            c.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            byte[] out = c.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(12 + out.length).put(iv).put(out).array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** 解密 {@link #encrypt} 产出的密文（AAD 需与加密时一致，否则认证失败）。 */
    public String decrypt(String aad, String value) {
        try {
            ByteBuffer b = ByteBuffer.wrap(Base64.getDecoder().decode(value));
            byte[] iv = new byte[12];
            b.get(iv);
            byte[] in = new byte[b.remaining()];
            b.get(in);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            c.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            return new String(c.doFinal(in), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot decrypt security material", ex);
        }
    }
}
