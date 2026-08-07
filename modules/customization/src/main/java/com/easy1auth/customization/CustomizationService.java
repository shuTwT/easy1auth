package com.easy1auth.customization;

import com.easy1auth.customization.model.*;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.id.UuidV7;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.*;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
public class CustomizationService {
    private static final BrandSettingEntityTable BRAND = BrandSettingEntityTable.$;
    private static final LoginStyleEntityTable STYLE = LoginStyleEntityTable.$;
    private static final CustomDomainEntityTable DOMAIN = CustomDomainEntityTable.$;
    private static final MessageTemplateEntityTable TEMPLATE = MessageTemplateEntityTable.$;
    private final JSqlClient sql;
    private final SecureRandom random = new SecureRandom();

    public CustomizationService(JSqlClient sql) {
        this.sql = sql;
    }

    @Transactional
    public BrandSettingEntity brand() {
        var e = sql.createQuery(BRAND).select(BRAND).fetchOneOrNull();
        if (e == null) {
            e = BrandSettingEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setSettings(Map.of()).setCompanyName(null).setLogo(null).setFavicon(null).setPrimaryColor("#0369A1").setSecondaryColor(null).setSupportEmail(null).setCopyrightText(null).setUpdatedAt(Instant.now()));
            sql.saveCommand(e).setMode(SaveMode.INSERT_IF_ABSENT).execute();
        }
        return e;
    }

    @Transactional
    public BrandSettingEntity updateBrand(Map<String, Object> settings) {
        rejectDangerous(settings);
        var old = brand();
        var e = BrandSettingEntityDraft.$.produce(d -> d.setId(old.id()).setSettings(settings == null ? Map.of() : settings).setCompanyName(old.companyName()).setLogo(old.logo()).setFavicon(old.favicon()).setPrimaryColor(old.primaryColor()).setSecondaryColor(old.secondaryColor()).setSupportEmail(old.supportEmail()).setCopyrightText(old.copyrightText()).setUpdatedAt(Instant.now()));
        sql.saveCommand(e).setMode(SaveMode.UPSERT).execute();
        return e;
    }

    @Transactional
    public LoginStyleEntity style() {
        var e = sql.createQuery(STYLE).select(STYLE).fetchOneOrNull();
        if (e == null) {
            Instant now = Instant.now();
            e = LoginStyleEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setLogo(null).setLogoDark(null).setBackgroundImage(null).setBackgroundColor("#f5f7fa").setPrimaryColor("#0369A1").setTitle("Easy1Auth").setSubtitle("企业级身份管理平台").setCustomCss(null).setLoginMethods(List.of("password")).setSocialProviders(List.of()).setCreatedAt(now).setUpdatedAt(now));
            sql.saveCommand(e).setMode(SaveMode.INSERT_IF_ABSENT).execute();
        }
        return e;
    }

    @Transactional
    public LoginStyleEntity publicStyle(UUID tenant) {
        var e = sql.createQuery(STYLE).where(STYLE.tenantId().eq(tenant)).select(STYLE).fetchOneOrNull();
        if (e == null) {
            Instant now = Instant.now();
            e = LoginStyleEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant).setLogo(null).setLogoDark(null).setBackgroundImage(null).setBackgroundColor("#f5f7fa").setPrimaryColor("#0369A1").setTitle("Easy1Auth").setSubtitle("企业级身份管理平台").setCustomCss(null).setLoginMethods(List.of("password")).setSocialProviders(List.of()).setCreatedAt(now).setUpdatedAt(now));
            sql.saveCommand(e).setMode(SaveMode.INSERT_IF_ABSENT).execute();
        }
        return e;
    }

    @Transactional
    public LoginStyleEntity updateStyle(StyleInput in) {
        var old = style();
        String bg = color(in.backgroundColor(), old.backgroundColor()), primary = color(in.primaryColor(), old.primaryColor());
        url(in.logo());
        url(in.logoDark());
        url(in.backgroundImage());
        var e = LoginStyleEntityDraft.$.produce(d -> d.setId(old.id()).setLogo(pick(in.logo(), old.logo())).setLogoDark(pick(in.logoDark(), old.logoDark())).setBackgroundImage(pick(in.backgroundImage(), old.backgroundImage())).setBackgroundColor(bg).setPrimaryColor(primary).setTitle(shortText(in.title(), old.title(), 200)).setSubtitle(shortText(in.subtitle(), old.subtitle(), 500)).setCustomCss(in.customCss()).setLoginMethods(methods(in.loginMethods())).setSocialProviders(in.socialProviders() == null ? old.socialProviders() : in.socialProviders().stream().distinct().toList()).setCreatedAt(old.createdAt()).setUpdatedAt(Instant.now()));
        sql.saveCommand(e).setMode(SaveMode.UPSERT).execute();
        return e;
    }

    @Transactional(readOnly = true)
    public List<CustomDomainEntity> domains() {
        return sql.createQuery(DOMAIN).orderBy(DOMAIN.createdAt().desc()).select(DOMAIN).execute();
    }

    @Transactional
    public CustomDomainEntity addDomain(String value, String method) {
        String domain = normalizeDomain(value);
        String m = method == null ? "dns" : method;
        if (!Set.of("dns", "file").contains(m)) throw invalid(ErrorCodeConstants.DOMAIN_METHOD_INVALID);
        byte[] b = new byte[24];
        random.nextBytes(b);
        Instant now = Instant.now();
        var e = CustomDomainEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setDomain(domain).setStatus("pending").setVerificationMethod(m).setVerificationToken(Base64.getUrlEncoder().withoutPadding().encodeToString(b)).setVerifiedAt(null).setCreatedAt(now).setUpdatedAt(now));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        return e;
    }

    @Transactional
    public void deleteDomain(UUID id) {
        if (!sql.createQuery(DOMAIN).where(DOMAIN.id().eq(id)).select(DOMAIN.id()).exists() || sql.deleteById(CustomDomainEntity.class, id).getTotalAffectedRowCount() != 1)
            throw new DomainException(ErrorCodeConstants.DOMAIN_NOT_FOUND);
    }

    public void verificationUnavailable() {
        throw new DomainException(ErrorCodeConstants.DOMAIN_VERIFICATION_NOT_AVAILABLE);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplateEntity> templates(String type) {
        return sql.createQuery(TEMPLATE).whereIf(type != null && !type.isBlank(), () -> TEMPLATE.type().eq(type)).orderBy(TEMPLATE.type(), TEMPLATE.code()).select(TEMPLATE).execute();
    }

    @Transactional
    public MessageTemplateEntity saveTemplate(UUID id, TemplateInput in) {
        if (in == null || !Set.of("email", "sms").contains(in.type()) || in.code() == null || !in.code().matches("[a-z0-9_]{2,100}") || in.name() == null || in.name().isBlank() || in.content() == null || in.content().isBlank())
            throw invalid(ErrorCodeConstants.MESSAGE_TEMPLATE_INVALID);
        validateVariables(in.content(), in.variables());
        Instant now = Instant.now();
        if (id == null) {
            var created = MessageTemplateEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setType(in.type()).setCode(in.code()).setName(in.name().strip()).setSubject(in.subject()).setContent(in.content()).setVariables(in.variables() == null ? Map.of() : in.variables()).setDefaultTemplate(Boolean.TRUE.equals(in.isDefault())).setStatus(in.status() == null ? "active" : in.status()).setCreatedAt(now).setUpdatedAt(now));
            sql.saveCommand(created).setMode(SaveMode.INSERT_ONLY).execute();
            return created;
        }
        template(id);
        int updated = sql.createUpdate(TEMPLATE).set(TEMPLATE.type(), in.type()).set(TEMPLATE.code(), in.code()).set(TEMPLATE.name(), in.name().strip()).set(TEMPLATE.subject(), in.subject()).set(TEMPLATE.content(), in.content()).set(TEMPLATE.variables(), in.variables() == null ? Map.of() : in.variables()).set(TEMPLATE.defaultTemplate(), Boolean.TRUE.equals(in.isDefault())).set(TEMPLATE.status(), in.status() == null ? "active" : in.status()).set(TEMPLATE.updatedAt(), now).where(TEMPLATE.id().eq(id)).execute();
        if (updated != 1) throw templateMissing();
        return template(id);
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        template(id);
        if (sql.deleteById(MessageTemplateEntity.class, id).getTotalAffectedRowCount() != 1)
            throw new DomainException(ErrorCodeConstants.MESSAGE_TEMPLATE_NOT_FOUND);
    }

    private MessageTemplateEntity template(UUID id) {
        return sql.createQuery(TEMPLATE).where(TEMPLATE.id().eq(id)).select(TEMPLATE).fetchOptional().orElseThrow(this::templateMissing);
    }

    private DomainException templateMissing() {
        return new DomainException(ErrorCodeConstants.MESSAGE_TEMPLATE_NOT_FOUND);
    }

    private static void rejectDangerous(Map<String, Object> m) {
        if (m == null) return;
        if (m.toString().toLowerCase(Locale.ROOT).contains("<script") || m.toString().toLowerCase(Locale.ROOT).contains("javascript:"))
            throw invalid(ErrorCodeConstants.BRAND_CONTENT_UNSAFE);
    }

    private static void validateVariables(String content, Map<String, String> vars) {
        var allowed = vars == null ? Set.<String>of() : vars.keySet();
        var matcher = java.util.regex.Pattern.compile("\\{\\{([A-Za-z][A-Za-z0-9_]*)}}").matcher(content);
        while (matcher.find()) if (!allowed.contains(matcher.group(1)))
            throw invalid(ErrorCodeConstants.TEMPLATE_VARIABLE_UNKNOWN);
    }

    private static String normalizeDomain(String value) {
        try {
            String d = IDN.toASCII(Objects.toString(value, "").strip().toLowerCase(Locale.ROOT));
            if (d.length() > 253 || !d.matches("(?=.{1,253}$)([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}"))
                throw new IllegalArgumentException();
            return d;
        } catch (RuntimeException ex) {
            throw invalid(ErrorCodeConstants.DOMAIN_INVALID);
        }
    }

    private static void url(String v) {
        if (v == null || v.isBlank()) return;
        try {
            URI u = URI.create(v);
            if (!"https".equalsIgnoreCase(u.getScheme()) || u.getHost() == null) throw new IllegalArgumentException();
        } catch (RuntimeException ex) {
            throw invalid(ErrorCodeConstants.ASSET_URL_INVALID);
        }
    }

    private static String color(String value, String old) {
        String v = value == null ? old : value;
        if (!v.matches("#[0-9A-Fa-f]{6}")) throw invalid(ErrorCodeConstants.COLOR_INVALID);
        return v;
    }

    private static String pick(String v, String old) {
        return v == null ? old : v;
    }

    private static String shortText(String v, String old, int max) {
        String x = v == null ? old : v.strip();
        if (x.isEmpty() || x.length() > max) throw invalid(ErrorCodeConstants.STYLE_TEXT_INVALID);
        return x;
    }

    private static List<String> methods(List<String> v) {
        if (v == null) return List.of("password");
        var out = v.stream().filter(Set.of("password", "email", "oidc")::contains).distinct().toList();
        if (out.isEmpty()) throw invalid(ErrorCodeConstants.LOGIN_METHOD_INVALID);
        return out;
    }

    private static DomainException invalid(com.easy1auth.foundation.error.ErrorCode errorCode) {
        return new DomainException(errorCode);
    }

    public record StyleInput(String logo, String logoDark, String backgroundImage, String backgroundColor,
                             String primaryColor, String title, String subtitle, String customCss,
                             List<String> loginMethods, List<String> socialProviders) {
    }

    public record TemplateInput(String type, String code, String name, String subject, String content,
                                Map<String, String> variables, Boolean isDefault, String status) {
    }
}
