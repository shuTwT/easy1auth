package com.easy1auth.application;

import java.util.List;

/** OAuth2 应用创建/更新输入。 */
public record ApplicationInput(String name, String logo, String description, String type, List<String> redirectUris,
                               List<String> postLogoutRedirectUris, List<String> allowedGrantTypes,
                               List<String> scopes, Boolean requirePkce, Boolean requireConsent,
                               Integer accessTokenLifetime, Integer refreshTokenLifetime) {
}
