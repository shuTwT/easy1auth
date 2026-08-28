package com.easy1auth.oauth2;

import com.easy1auth.infrastructure.foundation.id.UuidV7;
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

/**
 * 租户签名密钥服务：为每个租户提供 RSA 签名密钥（用于 OIDC/JWT 签名）。
 *
 * <p>密钥首次按租户生成并持久化到 oauth2_signing_key 表：公钥以明文保存，
 * 私钥使用 AES-GCM 加密后保存；同一租户始终复用同一把激活密钥。
 * 生成前会锁定租户，保证并发场景下每个租户只存在一把激活密钥。</p>
 */
@Service
public class TenantSigningKeyService {
    /** oauth2_signing_key 表静态描述符 */
    private static final OAuthSigningKeyEntityTable KEY = OAuthSigningKeyEntityTable.$;
    /** jimmer SQL 客户端 */
    private final JSqlClient sql;
    /** 租户服务（生成密钥前锁定租户，保证并发安全） */
    private final TenantService tenants;
    /** JSON 序列化器（用于 JWK 的序列化与反序列化） */
    private final ObjectMapper json;
    /** 私钥加密密钥（由配置密钥经 SHA-256 派生，供 AES-GCM 使用） */
    private final byte[] encryptionKey;

    public TenantSigningKeyService(JSqlClient sql, TenantService tenants, ObjectMapper json, @Value("${easy1auth.oauth2.key-encryption-secret:}") String secret) {
        this.sql = sql;
        this.tenants = tenants;
        this.json = json;
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("OAUTH2_KEY_ENCRYPTION_SECRET must contain at least 32 UTF-8 bytes");
        }
        try {
            this.encryptionKey = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 返回指定租户的激活签名密钥；若尚未生成，则并发安全地生成并持久化。
     *
     * @param tenant 租户 ID
     * @return 租户用于 JWT 签名的 RSA 密钥
     */
    @Transactional
    public RSAKey active(UUID tenant) {
        var existing = find(tenant);
        if (existing != null) {
            return decode(existing);
        }
        tenants.lockForSecurityMaterial(tenant);
        existing = find(tenant);
        if (existing != null) {
            return decode(existing);
        }
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

    /** 查询指定租户的激活签名密钥实体，不存在时返回 null。 */
    private OAuthSigningKeyEntity find(UUID tenant) {
        return sql.createQuery(KEY).where(KEY.tenantId().eq(tenant), KEY.status().eq("active")).select(KEY).fetchOneOrNull();
    }

    /** 将实体中加密存储的私钥解密，还原为可用的 RSAKey。 */
    private RSAKey decode(OAuthSigningKeyEntity entity) {
        try {
            return RSAKey.parse(decrypt(entity.tenantId(), entity.keyId(), entity.encryptedPrivateJwk()));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot decrypt tenant signing key", ex);
        }
    }

    /** 使用 AES-GCM 加密 JWK 明文，输出「nonce + 密文」的 Base64 串（以租户与 keyId 作为附加认证数据）。 */
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

    /** 解密 {@link #encrypt} 生成的 Base64 串，还原 JWK 明文。 */
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
