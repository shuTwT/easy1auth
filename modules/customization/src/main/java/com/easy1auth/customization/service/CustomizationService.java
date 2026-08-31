package com.easy1auth.customization.service;

import com.easy1auth.customization.constant.ErrorCodeConstants;
import com.easy1auth.customization.dto.*;
import com.easy1auth.customization.model.*;
import com.easy1auth.customization.repository.CustomizationRepository;
import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.common.foundation.id.UuidV7;
import com.easy1auth.common.foundation.error.ErrorCode;
import com.easy1auth.connection.service.SocialIdentityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.*;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

/**
 * 品牌定制服务：管理 pool_user 登录页的品牌样式（logo、配色、文案、自定义 CSS、
 * 登录方式等）、法律文档（服务条款与隐私政策）、自定义域名与消息模板。
 *
 * <p>品牌样式与法律文档采用"草稿 + 发布"模式：编辑内容保存在草稿中，
 * 调用 {@link #publish()} 后才会对用户可见的登录页生效。
 * 自定义域名支持 DNS 或文件两种所有权验证方式。</p>
 */
@Service
public class CustomizationService {
    /** 登录样式配置的 schema 版本号（当前为 1） */
    private static final int LOGIN_STYLE_SCHEMA_VERSION = 1;
    /** 品牌定制数据仓储 */
    private final CustomizationRepository repository;
    /** 社会化身份源服务（用于校验所选社交登录方式对应的身份源是否可用） */
    private final SocialIdentityService socialIdentity;
    /** 生成域名所有权验证令牌的随机源 */
    private final SecureRandom random = new SecureRandom();

    public CustomizationService(CustomizationRepository repository, SocialIdentityService socialIdentity) {
        this.repository = repository;
        this.socialIdentity = socialIdentity;
    }

    /** 查询（或自动创建）法律文档设置记录，返回当前生效的服务条款与隐私政策实体。 */
    @Transactional
    public LegalDocumentSettingEntity legalDocuments() {
        var e = repository.legalDocuments().orElse(null);
        if (e == null) {
            e = LegalDocumentSettingEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTermsOfService(null).setPrivacyPolicy(null).setDraftTermsOfService(null).setDraftPrivacyPolicy(null).setPublishedTermsOfService(null).setPublishedPrivacyPolicy(null).setUpdatedAt(Instant.now()));
            repository.insertLegal(e);
        }
        return e;
    }

    /** 查询当前草稿视图：登录样式草稿配置 + 法律文档草稿 + 草稿更新时间。 */
    @Transactional
    public DraftView draft() {
        var style = style();
        var legal = legalDocuments();
        Map<String, Object> config = config(style.draftConfig(), style);
        return new DraftView(config, style.draftSocialProviders(),
                new LegalDocumentsInput(firstNonNull(legal.draftTermsOfService(), legal.termsOfService()), firstNonNull(legal.draftPrivacyPolicy(), legal.privacyPolicy())),
                style.draftUpdatedAt(), style.publishedAt());
    }

    /** 更新草稿：合并输入配置、法律文档与社交身份源到现有草稿中，仅改草稿不影响已发布内容。 */
    @Transactional
    public DraftView updateDraft(DraftInput input) {
        Map<String, Object> nextConfig = normalizeConfig(input == null ? null : input.config());
        LegalDocumentsInput nextLegal = normalizeLegal(input == null ? null : input.legalDocuments());
        var oldStyle = style();
        var oldLegal = legalDocuments();
        List<String> nextSocialProviders = socialProviders(input == null || input.socialProviderIds() == null
                ? oldStyle.draftSocialProviders() : input.socialProviderIds());
        requireSocialProviders(nextConfig, nextSocialProviders);
        Instant now = Instant.now();
        var nextStyle = LoginStyleEntityDraft.$.produce(d -> d
                .setId(oldStyle.id())
                .setLogo(oldStyle.logo()).setLogoDark(oldStyle.logoDark()).setBackgroundImage(oldStyle.backgroundImage())
                .setBackgroundColor(oldStyle.backgroundColor()).setPrimaryColor(oldStyle.primaryColor())
                .setTitle(oldStyle.title()).setSubtitle(oldStyle.subtitle()).setCustomCss(oldStyle.customCss())
                .setLoginMethods(oldStyle.loginMethods()).setSocialProviders(oldStyle.socialProviders()).setDraftSocialProviders(nextSocialProviders)
                .setDraftConfig(nextConfig).setPublishedConfig(oldStyle.publishedConfig())
                .setRegistrationEnabled(oldStyle.registrationEnabled()).setDraftUpdatedAt(now)
                .setPublishedAt(oldStyle.publishedAt()).setCreatedAt(oldStyle.createdAt()).setUpdatedAt(now));
        var nextLegalEntity = LegalDocumentSettingEntityDraft.$.produce(d -> d
                .setId(oldLegal.id()).setTermsOfService(oldLegal.termsOfService()).setPrivacyPolicy(oldLegal.privacyPolicy())
                .setDraftTermsOfService(nextLegal.termsOfService()).setDraftPrivacyPolicy(nextLegal.privacyPolicy())
                .setPublishedTermsOfService(oldLegal.publishedTermsOfService()).setPublishedPrivacyPolicy(oldLegal.publishedPrivacyPolicy())
                .setUpdatedAt(now));
        repository.upsertStyle(nextStyle);
        repository.upsertLegal(nextLegalEntity);
        return new DraftView(nextConfig, nextSocialProviders, nextLegal, now, oldStyle.publishedAt());
    }

    /** 发布草稿：将当前草稿配置与法律文档设为已发布版本，对用户可见的登录页生效。 */
    @Transactional
    public DraftView publish() {
        var oldStyle = style();
        var oldLegal = legalDocuments();
        Map<String, Object> nextConfig = config(oldStyle.draftConfig(), oldStyle);
        LegalDocumentsInput nextLegal = normalizeLegal(new LegalDocumentsInput(
                firstNonNull(oldLegal.draftTermsOfService(), oldLegal.termsOfService()),
                firstNonNull(oldLegal.draftPrivacyPolicy(), oldLegal.privacyPolicy())));
        List<String> nextSocialProviders = socialProviders(oldStyle.draftSocialProviders());
        requireSocialProviders(nextConfig, nextSocialProviders);
        LegacyStyle legacy = legacyStyle(nextConfig);
        Instant now = Instant.now();
        var nextStyle = LoginStyleEntityDraft.$.produce(d -> d
                .setId(oldStyle.id()).setLogo(legacy.logo()).setLogoDark(legacy.logoDark()).setBackgroundImage(legacy.backgroundImage())
                .setBackgroundColor(legacy.backgroundColor()).setPrimaryColor(legacy.primaryColor()).setTitle(legacy.title())
                .setSubtitle(legacy.subtitle()).setCustomCss(legacy.customCss()).setLoginMethods(legacy.loginMethods())
                .setSocialProviders(nextSocialProviders).setDraftSocialProviders(nextSocialProviders).setDraftConfig(nextConfig).setPublishedConfig(nextConfig)
                .setRegistrationEnabled(legacy.registrationEnabled()).setDraftUpdatedAt(now).setPublishedAt(now)
                .setCreatedAt(oldStyle.createdAt()).setUpdatedAt(now));
        var nextLegalEntity = LegalDocumentSettingEntityDraft.$.produce(d -> d
                .setId(oldLegal.id()).setTermsOfService(nextLegal.termsOfService()).setPrivacyPolicy(nextLegal.privacyPolicy())
                .setDraftTermsOfService(nextLegal.termsOfService()).setDraftPrivacyPolicy(nextLegal.privacyPolicy())
                .setPublishedTermsOfService(nextLegal.termsOfService()).setPublishedPrivacyPolicy(nextLegal.privacyPolicy())
                .setUpdatedAt(now));
        repository.upsertStyle(nextStyle);
        repository.upsertLegal(nextLegalEntity);
        return new DraftView(nextConfig, nextSocialProviders, nextLegal, now, now);
    }

    /** 重置草稿为默认配置（默认样式 + 空法律文档 + 清空社交身份源）。 */
    @Transactional
    public DraftView resetDraft() {
        return updateDraft(new DraftInput(defaultConfig(), List.of(), new LegalDocumentsInput(null, null)));
    }

    /** 查询（或自动初始化）当前租户的登录样式实体；首次访问时写入默认样式。 */
    @Transactional
    public LoginStyleEntity style() {
        var e = repository.style().orElse(null);
        if (e == null) {
            Instant now = Instant.now();
            Map<String, Object> defaults = defaultConfig();
            e = LoginStyleEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setLogo(null).setLogoDark(null).setBackgroundImage(null).setBackgroundColor("#f5f7fa").setPrimaryColor("#0369A1").setTitle("Easy1Auth").setSubtitle("企业级身份管理平台").setCustomCss(null).setLoginMethods(List.of("password")).setSocialProviders(List.of()).setDraftSocialProviders(List.of()).setDraftConfig(defaults).setPublishedConfig(defaults).setRegistrationEnabled(true).setDraftUpdatedAt(now).setPublishedAt(now).setCreatedAt(now).setUpdatedAt(now));
            repository.insertStyle(e);
        }
        return e;
    }

    /** 查询指定租户的登录样式（面向 pool_user 登录页的公开访问；不存在则初始化默认样式）。 */
    @Transactional
    public LoginStyleEntity publicStyle(UUID tenant) {
        var e = repository.style(tenant).orElse(null);
        if (e == null) {
            Instant now = Instant.now();
            Map<String, Object> defaults = defaultConfig();
            e = LoginStyleEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant).setLogo(null).setLogoDark(null).setBackgroundImage(null).setBackgroundColor("#f5f7fa").setPrimaryColor("#0369A1").setTitle("Easy1Auth").setSubtitle("企业级身份管理平台").setCustomCss(null).setLoginMethods(List.of("password")).setSocialProviders(List.of()).setDraftSocialProviders(List.of()).setDraftConfig(defaults).setPublishedConfig(defaults).setRegistrationEnabled(true).setDraftUpdatedAt(now).setPublishedAt(now).setCreatedAt(now).setUpdatedAt(now));
            repository.insertStyle(e);
        }
        return e;
    }

    /** 查询指定租户已发布的登录样式配置（供登录页渲染使用；未配置时返回默认配置）。 */
    @Transactional(readOnly = true)
    public Map<String, Object> publishedConfig(UUID tenant) {
        var style = repository.style(tenant).orElse(null);
        if (style == null) {
            return defaultConfig();
        }
        return config(style.publishedConfig(), style);
    }

    /** 查询指定租户已发布的法律文档（服务条款与隐私政策，供登录 / 注册页展示）。 */
    @Transactional(readOnly = true)
    public PublishedLegalDocuments publishedLegalDocuments(UUID tenant) {
        var legal = repository.legalDocuments(tenant).orElse(null);
        if (legal == null) {
            return new PublishedLegalDocuments(null, null);
        }
        return new PublishedLegalDocuments(firstNonNull(legal.publishedTermsOfService(), legal.termsOfService()), firstNonNull(legal.publishedPrivacyPolicy(), legal.privacyPolicy()));
    }

    /** 查询全部自定义域名（按创建时间倒序）。 */
    @Transactional(readOnly = true)
    public List<CustomDomainEntity> domains() {
        return repository.domains();
    }

    /**
     * 添加自定义域名并进入待验证状态，生成用于所有权验证的随机令牌。
     *
     * @param method 验证方式：dns（DNS 记录）/ file（上传验证文件）
     */
    @Transactional
    public CustomDomainEntity addDomain(String value, String method) {
        String domain = normalizeDomain(value);
        String m = method == null ? "dns" : method;
        if (!Set.of("dns", "file").contains(m)) {
            throw invalid(ErrorCodeConstants.DOMAIN_METHOD_INVALID);
        }
        byte[] b = new byte[24];
        random.nextBytes(b);
        Instant now = Instant.now();
        var e = CustomDomainEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setDomain(domain).setStatus("pending").setVerificationMethod(m).setVerificationToken(Base64.getUrlEncoder().withoutPadding().encodeToString(b)).setVerifiedAt(null).setCreatedAt(now).setUpdatedAt(now));
        repository.insertDomain(e);
        return e;
    }

    /** 删除自定义域名；域名不存在时抛出领域异常。 */
    @Transactional
    public void deleteDomain(UUID id) {
        if (!repository.deleteDomain(id)) {
            throw new DomainException(ErrorCodeConstants.DOMAIN_NOT_FOUND);
        }
    }

    /** 抛出"阶段 6 不提供域名所有权验证或证书托管"的领域异常。 */
    public void verificationUnavailable() {
        throw new DomainException(ErrorCodeConstants.DOMAIN_VERIFICATION_NOT_AVAILABLE);
    }

    /** 查询消息模板列表，可按类型（email / sms）过滤，按类型与编码排序。 */
    @Transactional(readOnly = true)
    public List<MessageTemplateEntity> templates(String type) {
        return repository.templates(type);
    }

    /** 新建（id 为空）或更新（id 非空）消息模板，校验模板参数与变量声明合法性。 */
    @Transactional
    public MessageTemplateEntity saveTemplate(UUID id, MessageTemplateInput in) {
        if (in == null || !Set.of("email", "sms").contains(in.type()) || in.code() == null || !in.code().matches("[a-z0-9_]{2,100}") || in.name() == null || in.name().isBlank() || in.content() == null || in.content().isBlank()) {
            throw invalid(ErrorCodeConstants.MESSAGE_TEMPLATE_INVALID);
        }
        validateVariables(in.content(), in.variables());
        Instant now = Instant.now();
        if (id == null) {
            var created = MessageTemplateEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setType(in.type()).setCode(in.code()).setName(in.name().strip()).setSubject(in.subject()).setContent(in.content()).setVariables(in.variables() == null ? Map.of() : in.variables()).setDefaultTemplate(Boolean.TRUE.equals(in.isDefault())).setStatus(in.status() == null ? "active" : in.status()).setCreatedAt(now).setUpdatedAt(now));
            repository.insertTemplate(created);
            return created;
        }
        template(id);
        int updated = repository.updateTemplate(id, in.type(), in.code(), in.name().strip(), in.subject(), in.content(), in.variables() == null ? Map.of() : in.variables(), Boolean.TRUE.equals(in.isDefault()), in.status() == null ? "active" : in.status());
        if (updated != 1) {
            throw templateMissing();
        }
        return template(id);
    }

    /** 读取草稿 / 已发布的样式配置，为空时由旧版字段兼容生成。 */
    private static Map<String, Object> config(Map<String, Object> value, LoginStyleEntity legacy) {
        return normalizeConfig(value == null || value.isEmpty() ? legacyConfig(legacy) : value, false);
    }

    private static Map<String, Object> defaultConfig() {
        Map<String, Object> background = new LinkedHashMap<>();
        background.put("mode", "solid");
        background.put("color", "#f5f7fa");
        background.put("imageUrl", null);
        background.put("overlayColor", null);
        background.put("overlayOpacity", 0);
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("width", 480);
        card.put("radius", 16);
        card.put("shadow", true);
        card.put("position", "center");
        Map<String, Object> global = new LinkedHashMap<>();
        global.put("title", "Easy1Auth");
        global.put("subtitle", "企业级身份管理平台");
        global.put("logoUrl", null);
        global.put("logoDarkUrl", null);
        global.put("background", background);
        global.put("primaryColor", "#0369A1");
        global.put("language", "zh-CN");
        global.put("card", card);
        global.put("customCss", null);
        Map<String, Object> standard = new LinkedHashMap<>();
        standard.put("enabled", true);
        standard.put("methods", List.of("password", "email"));
        standard.put("registrationEnabled", true);
        standard.put("termsRequired", false);
        Map<String, Object> qr = new LinkedHashMap<>();
        qr.put("enabled", false);
        qr.put("title", "扫码登录");
        qr.put("subtitle", "使用手机扫码继续");
        qr.put("iconUrl", null);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", LOGIN_STYLE_SCHEMA_VERSION);
        root.put("global", global);
        root.put("standard", standard);
        root.put("qr", qr);
        return root;
    }

    private static Map<String, Object> legacyConfig(LoginStyleEntity style) {
        Map<String, Object> root = defaultConfig();
        Map<String, Object> global = object(root, "global");
        global.put("title", style.title());
        global.put("subtitle", style.subtitle());
        global.put("logoUrl", style.logo());
        global.put("logoDarkUrl", style.logoDark());
        global.put("primaryColor", style.primaryColor());
        global.put("customCss", style.customCss());
        Map<String, Object> background = object(global, "background");
        background.put("mode", style.backgroundImage() == null ? "solid" : "image");
        background.put("color", style.backgroundColor());
        background.put("imageUrl", style.backgroundImage());
        Map<String, Object> standard = object(root, "standard");
        standard.put("methods", normalizeMethods(style.loginMethods()));
        standard.put("registrationEnabled", style.registrationEnabled());
        return root;
    }

    private static Map<String, Object> normalizeConfig(Map<String, Object> input) {
        return normalizeConfig(input, true);
    }

    /** 规范化样式配置：与默认值合并并校验取值范围；strictCss=true 时不安全 CSS 直接报错，否则置空。 */
    private static Map<String, Object> normalizeConfig(Map<String, Object> input, boolean strictCss) {
        Map<String, Object> root = defaultConfig();
        if (input != null) {
            if (input.get("global") instanceof Map<?, ?> value) {
                object(root, "global").putAll(stringMap(value));
            }
            if (input.get("standard") instanceof Map<?, ?> value) {
                object(root, "standard").putAll(stringMap(value));
            }
            if (input.get("qr") instanceof Map<?, ?> value) {
                object(root, "qr").putAll(stringMap(value));
            }
            if (input.containsKey("schemaVersion")) {
                root.put("schemaVersion", 1);
            }
        }
        Map<String, Object> global = object(root, "global");
        global.put("title", text(global.get("title"), "Easy1Auth", 200));
        global.put("subtitle", text(global.get("subtitle"), "企业级身份管理平台", 500));
        global.put("logoUrl", httpsUrl(global.get("logoUrl")));
        global.put("logoDarkUrl", httpsUrl(global.get("logoDarkUrl")));
        global.put("primaryColor", sixColor(global.get("primaryColor"), "#0369A1"));
        global.put("language", "zh-CN");
        global.put("customCss", css(global.get("customCss"), strictCss));
        Map<String, Object> background = object(global, "background");
        String mode = Set.of("solid", "image").contains(Objects.toString(background.get("mode"), "solid")) ? Objects.toString(background.get("mode"), "solid") : "solid";
        background.put("mode", mode);
        background.put("color", sixColor(background.get("color"), "#f5f7fa"));
        background.put("imageUrl", httpsUrl(background.get("imageUrl")));
        background.put("overlayColor", background.get("overlayColor") == null ? null : sixColor(background.get("overlayColor"), "#000000"));
        background.put("overlayOpacity", number(background.get("overlayOpacity"), 0, 0, 1));
        Map<String, Object> card = object(global, "card");
        card.put("width", number(card.get("width"), 480, 320, 720));
        card.put("radius", number(card.get("radius"), 16, 0, 32));
        card.put("shadow", Boolean.TRUE.equals(card.get("shadow")));
        card.put("position", Set.of("left", "center", "right").contains(Objects.toString(card.get("position"), "center")) ? Objects.toString(card.get("position"), "center") : "center");
        Map<String, Object> standard = object(root, "standard");
        standard.put("enabled", !Boolean.FALSE.equals(standard.get("enabled")));
        standard.put("methods", normalizeMethods(standard.get("methods")));
        standard.put("registrationEnabled", !Boolean.FALSE.equals(standard.get("registrationEnabled")));
        standard.put("termsRequired", Boolean.TRUE.equals(standard.get("termsRequired")));
        Map<String, Object> qr = object(root, "qr");
        qr.put("enabled", Boolean.TRUE.equals(qr.get("enabled")));
        qr.put("title", text(qr.get("title"), "扫码登录", 200));
        qr.put("subtitle", text(qr.get("subtitle"), "使用手机扫码继续", 500));
        qr.put("iconUrl", httpsUrl(qr.get("iconUrl")));
        root.put("schemaVersion", LOGIN_STYLE_SCHEMA_VERSION);
        return root;
    }

    /** 从规范化配置中提取关键项，回写为旧版样式字段（发布时使用）。 */
    private static LegacyStyle legacyStyle(Map<String, Object> config) {
        Map<String, Object> global = object(config, "global");
        Map<String, Object> background = object(global, "background");
        Map<String, Object> standard = object(config, "standard");
        return new LegacyStyle(
                nullableString(global.get("logoUrl")), nullableString(global.get("logoDarkUrl")), nullableString(background.get("imageUrl")),
                Objects.toString(background.get("color"), "#f5f7fa"), Objects.toString(global.get("primaryColor"), "#0369A1"),
                Objects.toString(global.get("title"), "Easy1Auth"), Objects.toString(global.get("subtitle"), "企业级身份管理平台"),
                nullableString(global.get("customCss")), normalizeMethods(standard.get("methods")),
                !Boolean.FALSE.equals(standard.get("registrationEnabled")));
    }

    private static LegalDocumentsInput normalizeLegal(LegalDocumentsInput input) {
        String terms = documentText(input == null ? null : input.termsOfService());
        String privacy = documentText(input == null ? null : input.privacyPolicy());
        rejectDangerous(terms, privacy);
        return new LegalDocumentsInput(terms, privacy);
    }

    /** 获取父 Map 中的嵌套子 Map，不存在时创建并放回。 */
    private static Map<String, Object> object(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = stringMap(map);
            parent.put(key, result);
            return result;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        parent.put(key, result);
        return result;
    }

    private static Map<String, Object> stringMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> { if (key instanceof String name) result.put(name, value); });
        return result;
    }

    private static String text(Object value, String fallback, int max) {
        String result = Objects.toString(value, fallback).strip();
        if (result.isEmpty() || result.length() > max) {
            throw invalid(ErrorCodeConstants.STYLE_TEXT_INVALID);
        }
        return result;
    }

    private static String nullableString(Object value) {
        return value == null || Objects.toString(value).isBlank() ? null : Objects.toString(value);
    }

    private static String httpsUrl(Object value) {
        String url = nullableString(value);
        if (url != null) {
            CustomizationService.url(url);
        }
        return url;
    }

    private static String sixColor(Object value, String fallback) {
        return color(value == null ? fallback : Objects.toString(value), fallback);
    }

    private static double number(Object value, double fallback, double min, double max) {
        double result = value instanceof Number n ? n.doubleValue() : fallback;
        if (result < min || result > max) {
            throw invalid(ErrorCodeConstants.STYLE_CONFIG_INVALID);
        }
        return result;
    }

    /** 校验自定义 CSS：包含脚本、javascript:、@import 等不安全内容时按 strict 决定报错或置空。 */
    private static String css(Object value, boolean strict) {
        String result = nullableString(value);
        if (result == null) {
            return null;
        }
        String normalized = result.toLowerCase(Locale.ROOT);
        boolean unsafe = normalized.contains("<script") || normalized.contains("javascript:") || normalized.contains("@import") || result.contains("{") || result.contains("}");
        if (unsafe) {
            if (strict) {
                throw invalid(ErrorCodeConstants.STYLE_CONFIG_INVALID);
            }
            return null;
        }
        return result;
    }

    private static List<String> normalizeMethods(Object value) {
        if (!(value instanceof Collection<?> values)) {
            return List.of("password", "email");
        }
        List<String> result = values.stream().map(Objects::toString).map(method -> "oidc".equals(method) ? "social" : method)
                .filter(Set.of("password", "email", "social")::contains).distinct().toList();
        return result.isEmpty() ? List.of("password") : result;
    }

    /** 校验并规范化社交身份源 ID：须为合法 UUID 且均为启用中的身份源。 */
    private List<String> socialProviders(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String value : values) {
            try {
                ids.add(UUID.fromString(Objects.requireNonNull(value).trim()).toString());
            } catch (RuntimeException ex) {
                throw invalid(ErrorCodeConstants.SOCIAL_PROVIDER_INVALID);
            }
        }
        Set<String> activeIds = socialIdentity.listActive().stream().map(source -> source.id().toString()).collect(java.util.stream.Collectors.toSet());
        if (!activeIds.containsAll(ids)) {
            throw invalid(ErrorCodeConstants.SOCIAL_PROVIDER_INVALID);
        }
        return List.copyOf(ids);
    }

    private static void requireSocialProviders(Map<String, Object> config, List<String> providers) {
        Object standardValue = config.get("standard");
        if (!(standardValue instanceof Map<?, ?> standard)) {
            return;
        }
        Object methodsValue = standard.get("methods");
        if (methodsValue instanceof Collection<?> methods && methods.stream().map(Objects::toString).anyMatch("social"::equals) && providers.isEmpty()) {
            throw invalid(ErrorCodeConstants.SOCIAL_PROVIDER_INVALID);
        }
    }

    private static String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    /** 删除消息模板；模板不存在时抛出领域异常。 */
    @Transactional
    public void deleteTemplate(UUID id) {
        template(id);
        if (!repository.deleteTemplate(id)) {
            throw new DomainException(ErrorCodeConstants.MESSAGE_TEMPLATE_NOT_FOUND);
        }
    }

    private MessageTemplateEntity template(UUID id) {
        return repository.template(id).orElseThrow(this::templateMissing);
    }

    private DomainException templateMissing() {
        return new DomainException(ErrorCodeConstants.MESSAGE_TEMPLATE_NOT_FOUND);
    }

    private static void rejectDangerous(String... values) {
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String normalized = value.toLowerCase(Locale.ROOT);
            if (normalized.contains("<script") || normalized.contains("javascript:")) {
                throw invalid(ErrorCodeConstants.LEGAL_DOCUMENT_CONTENT_UNSAFE);
            }
        }
    }

    private static String documentText(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void validateVariables(String content, Map<String, String> vars) {
        var allowed = vars == null ? Set.<String>of() : vars.keySet();
        var matcher = java.util.regex.Pattern.compile("\\{\\{([A-Za-z][A-Za-z0-9_]*)}}").matcher(content);
        while (matcher.find()) {
            if (!allowed.contains(matcher.group(1))) {
                throw invalid(ErrorCodeConstants.TEMPLATE_VARIABLE_UNKNOWN);
            }
        }
    }

    private static String normalizeDomain(String value) {
        try {
            String d = IDN.toASCII(Objects.toString(value, "").strip().toLowerCase(Locale.ROOT));
            if (d.length() > 253 || !d.matches("(?=.{1,253}$)([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}")) {
                throw new IllegalArgumentException();
            }
            return d;
        } catch (RuntimeException ex) {
            throw invalid(ErrorCodeConstants.DOMAIN_INVALID);
        }
    }

    private static void url(String v) {
        if (v == null || v.isBlank()) {
            return;
        }
        try {
            URI u = URI.create(v);
            if (!"https".equalsIgnoreCase(u.getScheme()) || u.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException ex) {
            throw invalid(ErrorCodeConstants.ASSET_URL_INVALID);
        }
    }

    private static String color(String value, String old) {
        String v = value == null ? old : value;
        if (!v.matches("#[0-9A-Fa-f]{6}")) {
            throw invalid(ErrorCodeConstants.COLOR_INVALID);
        }
        return v;
    }

    private static DomainException invalid(ErrorCode errorCode) {
        return new DomainException(errorCode);
    }

    

    

    

    

    

    
}
