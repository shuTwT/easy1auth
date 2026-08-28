package com.easy1auth.enterpriseidentity;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 飞书 URL 验证回调响应。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeishuEventResponse(String challenge) {
}
