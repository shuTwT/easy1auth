package com.easy1auth.authorization.web.dto;

public record Context(String status, String tenantId, PublicStyle style, String message, String csrfToken) {
        /** 创建一个表示交互已失效的登录上下文。 */
        static Context expired() { return new Context("expired", null, null, "登录请求已过期，请重新发起", null); }
    }
