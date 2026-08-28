package com.easy1auth.customization;
import java.util.Map;
/** 消息模板新建/更新入参。 */
public record TemplateInput(String type, String code, String name, String subject, String content,
                            Map<String, String> variables, Boolean isDefault, String status) { }
