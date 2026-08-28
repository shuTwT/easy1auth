package com.easy1auth.security.service;

import java.util.List;

/** 多因素认证状态。 */
public record Status(boolean enabled, List<String> methods) {
}
