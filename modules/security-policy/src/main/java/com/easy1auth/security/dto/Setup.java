package com.easy1auth.security.dto;

import java.util.List;

/** TOTP 设置结果，密钥和备用码仅在设置时返回。 */
public record Setup(String secret, String qrCodeUrl, List<String> backupCodes) {
}
