package com.easy1auth.admin.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SendCodeResponse(String code, String challengeToken) {
}
