package com.easy1auth.admin.web.dto;

import java.util.List;
import java.util.Map;

public record PublicStyleView(String logo, String logoDark, String backgroundImage, String backgroundColor,
                              String primaryColor, String title, String subtitle, List<String> loginMethods,
                              List<String> socialProviders, Map<String, Object> config,
                              String termsOfService, String privacyPolicy) {
}
