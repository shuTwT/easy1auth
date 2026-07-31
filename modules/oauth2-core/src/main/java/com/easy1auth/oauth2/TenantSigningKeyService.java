package com.easy1auth.oauth2;

import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.oauth2.model.*;
import com.easy1auth.tenant.TenantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.RSAKey;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.*;
import java.time.Instant;
import java.util.*;

@Service
public class TenantSigningKeyService {
    private static final OAuthSigningKeyEntityTable KEY = OAuthSigningKeyEntityTable.$;
    private final JSqlClient sql;
    private final TenantService tenants;
    private final ObjectMapper json;
    private final byte[] encryptionKey;

    public TenantSigningKeyService(JSqlClient sql, TenantService tenants, ObjectMapper json, @Value("${easy1auth.oauth2.key-encryption-secret:}") String secret) {
        this.sql = sql;
        this.tenants = tenants;
        this.json = json;
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalStateException("OAUTH2_KEY_ENCRYPTION_SECRET must contain at least 32 UTF-8 bytes");
        try {
            this.encryptionKey = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Transactional
    public RSAKey active(UUID tenant) {
        var existing = find(tenant);
        if (existing != null) return decode(existing);
        tenants.lockForSecurityMaterial(tenant);
        existing = find(tenant);
        if (existing != null) return decode(existing);
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            String keyId = UuidV7.randomUuid().toString();
            RSAKey rsa = new RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey((RSAPrivateKey) pair.getPrivate()).keyID(keyId).algorithm(com.nimbusds.jose.JWSAlgorithm.RS256).keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE).build();
            var entity = OAuthSigningKeyEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant).setKeyId(keyId).setAlgorithm("RS256").setPublicJwk(rsa.toPublicJWK().toJSONObject()).setEncryptedPrivateJwk(encrypt(tenant, keyId, rsa.toJSONString())).setStatus("active").setCreatedAt(Instant.now()).setExpiresAt(null));
            sql.saveCommand(entity).setMode(SaveMode.INSERT_ONLY).execute();
            return rsa;
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot generate tenant signing key", ex);
        }
    }

    private OAuthSigningKeyEntity find(UUID tenant) {
        return sql.createQuery(KEY).where(KEY.tenantId().eq(tenant), KEY.status().eq("active")).select(KEY).fetchOneOrNull();
    }

    private RSAKey decode(OAuthSigningKeyEntity entity) {
        try {
            return RSAKey.parse(decrypt(entity.tenantId(), entity.keyId(), entity.encryptedPrivateJwk()));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot decrypt tenant signing key", ex);
        }
    }

    private String encrypt(UUID tenant, String keyId, String value) {
        try {
            byte[] nonce = new byte[12];
            SecureRandom.getInstanceStrong().nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD((tenant + ":" + keyId).getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String decrypt(UUID tenant, String keyId, String value) throws GeneralSecurityException {
        byte[] all = Base64.getDecoder().decode(value);
        ByteBuffer buffer = ByteBuffer.wrap(all);
        byte[] nonce = new byte[12];
        buffer.get(nonce);
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, nonce));
        cipher.updateAAD((tenant + ":" + keyId).getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }
}
