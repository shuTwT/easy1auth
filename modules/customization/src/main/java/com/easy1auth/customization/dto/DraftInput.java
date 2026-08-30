package com.easy1auth.customization.dto;

import java.util.List;
import java.util.Map;
/** 登录样式草稿更新入参。 */
public record DraftInput(Map<String, Object> config, List<String> socialProviderIds, LegalDocumentsInput legalDocuments) { }
