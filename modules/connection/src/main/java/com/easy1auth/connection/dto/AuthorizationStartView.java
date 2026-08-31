package com.easy1auth.connection.dto;
/** 社交授权发起结果。 */
public record AuthorizationStartView(String authorizeUrl, String state, int expiresIn) { }
