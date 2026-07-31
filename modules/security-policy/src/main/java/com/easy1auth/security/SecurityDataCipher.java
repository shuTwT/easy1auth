package com.easy1auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

@Component
public final class SecurityDataCipher {
    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    public SecurityDataCipher(@Value("${easy1auth.security.data-encryption-secret:}") String secret) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalStateException("SECURITY_DATA_ENCRYPTION_SECRET must contain at least 32 UTF-8 bytes");
        try {
            key = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

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
