package com.easy1auth.social.service;
/** 社交身份源创建/更新入参。 */
public record SocialIdentityInput(String name, String type, String mode, String clientId, String clientSecret,
                                  Boolean jitProvisioning, String status) { }
