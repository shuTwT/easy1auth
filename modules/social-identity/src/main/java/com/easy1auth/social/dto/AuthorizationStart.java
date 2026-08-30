package com.easy1auth.social.dto;
/** 社交授权发起结果。 */
public record AuthorizationStart(String authorizeUrl, String state, int expiresIn) { }
