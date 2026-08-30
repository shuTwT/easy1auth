package com.easy1auth.security.dto;

import java.util.List;

/** 多因素认证状态。 */
public record StatusView(boolean enabled, List<String> methods) {
}
