package com.easy1auth.customization.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
/** 登录样式草稿视图。 */
public record DraftView(Map<String, Object> config, List<String> socialProviderIds, LegalDocumentsInput legalDocuments,
                        Instant draftUpdatedAt, Instant publishedAt) { }
