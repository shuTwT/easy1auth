package com.easy1auth.authorization.web.dto;

import java.util.*;

public record ConsentContext(String status, String tenantId, PublicStyle style, String clientId, String clientName,
                             List<String> scopes, String csrfToken) {
    /**
     * 创建一个表示授权交互已失效的上下文。
     */
    public static ConsentContext expired() {
        return new ConsentContext("expired", null, null, null, null, List.of(), null);
    }
}
