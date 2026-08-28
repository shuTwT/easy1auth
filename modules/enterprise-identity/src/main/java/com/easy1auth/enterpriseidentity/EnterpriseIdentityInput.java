package com.easy1auth.enterpriseidentity;

/**
 * 企业身份源新建/更新入参。
 */
public record EnterpriseIdentityInput(String name, String appId, String appSecret, String verificationToken,
                                      String encryptKey, String status) {
}
