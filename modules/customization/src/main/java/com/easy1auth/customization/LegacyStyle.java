package com.easy1auth.customization;
import java.util.List;
/** 旧版登录样式字段视图。 */
public record LegacyStyle(String logo, String logoDark, String backgroundImage, String backgroundColor,
                   String primaryColor, String title, String subtitle, String customCss,
                   List<String> loginMethods, boolean registrationEnabled) { }
