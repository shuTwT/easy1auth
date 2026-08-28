package com.easy1auth.authorization.web;
import java.util.*;

/** 授权页公开登录样式及法律文档视图。 */
public record PublicStyle(String logo, String logoDark, String backgroundImage, String backgroundColor,
                              String primaryColor, String title, String subtitle, List<String> loginMethods,
                              List<String> socialProviders, List<SocialSourceSummary> socialSources,
                              boolean registrationEnabled, Map<String, Object> config,
                              String termsOfService, String privacyPolicy) { }
