package com.easy1auth.authorization.web;

import com.easy1auth.customization.CustomizationService;
import com.easy1auth.customization.model.LoginStyleEntity;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.social.SocialIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * 管理授权门户中的登录与 OAuth 授权交互状态。
 *
 * <p>原始 OAuth 请求和租户上下文始终保存在服务端 Session 中，浏览器只拿到短期、
 * 不透明的交互 ID。这样可以避免用户通过修改前端参数替换 {@code redirect_uri}、
 * {@code scope} 或 {@code state}，并保证登录、MFA 和授权确认始终绑定到同一租户。</p>
 */
@Service
public class AuthorizationInteractionService {
    /** Session 中当前授权交互所属租户的属性名。 */
    public static final String TENANT = "EASY1AUTH_INTERACTION_TENANT";
    /** Session 中保存的原始 OAuth 授权参数。 */
    public static final String AUTH_REQUEST = "EASY1AUTH_INTERACTION_AUTH_REQUEST";
    /** 发起授权请求时的原始 URI。 */
    public static final String AUTH_URI = "EASY1AUTH_INTERACTION_AUTH_URI";
    /** 当前交互的过期时间（毫秒时间戳）。 */
    public static final String EXPIRES_AT = "EASY1AUTH_INTERACTION_EXPIRES_AT";
    /** Session 中已规范化的待确认授权参数。 */
    public static final String CONSENT_REQUEST = "EASY1AUTH_INTERACTION_CONSENT_REQUEST";
    /** 展示授权确认页时生成的不透明交互 ID。 */
    public static final String CONSENT_ID = "EASY1AUTH_INTERACTION_CONSENT_ID";
    /** 用户在授权确认页选择的操作（approve 或 deny）。 */
    public static final String CONSENT_ACTION = "EASY1AUTH_INTERACTION_CONSENT_ACTION";
    /** 标记授权交互是否已经被 OAuth 流程消费。 */
    public static final String USED = "EASY1AUTH_INTERACTION_USED";
    /** Session 中最近一次登录错误的展示信息。 */
    public static final String LOGIN_ERROR = "EASY1AUTH_LOGIN_ERROR";
    /** 登录或授权交互在无操作时的有效期，单位为秒。 */
    private static final long INTERACTION_TTL_SECONDS = 900;

    /** 保存 Spring Security 识别到的原始请求，登录完成后用于继续跳转。 */
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
    /** 查询租户公开的登录页样式。 */
    private final CustomizationService customization;
    /** 查询 OAuth 客户端所属租户和展示名称。 */
    private final JdbcClient db;
    /** 查询社会化身份源详情，供登录页渲染按钮。 */
    private final SocialIdentityService socialIdentity;

    /** 创建授权交互服务。 */
    public AuthorizationInteractionService(CustomizationService customization, JdbcClient db, SocialIdentityService socialIdentity) {
        this.customization = customization;
        this.db = db;
        this.socialIdentity = socialIdentity;
    }

    /**
     * 捕获一次新的 OAuth 授权请求。
     *
     * <p>除了交给 Spring Security 保存原始请求外，还会把租户、请求 URI 和参数复制到
     * Session，并清理上一次交互残留的 consent 状态，防止不同授权请求相互串联。</p>
     */
    public void captureAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        requestCache.saveRequest(request, response);
        UUID tenant = tenantFromPath(request.getRequestURI());
        if (tenant == null) return;
        HttpSession session = request.getSession(true);
        session.setAttribute(TENANT, tenant.toString());
        session.setAttribute(AUTH_URI, request.getRequestURI());
        session.setAttribute(AUTH_REQUEST, copy(request.getParameterMap()));
        session.setAttribute(EXPIRES_AT, Instant.now().plusSeconds(INTERACTION_TTL_SECONDS).toEpochMilli());
        session.removeAttribute(USED);
        session.removeAttribute(CONSENT_REQUEST);
        session.removeAttribute(CONSENT_ID);
        session.removeAttribute(CONSENT_ACTION);
    }

    /**
     * 生成登录门户上下文。
     *
     * <p>正常情况下租户从 Session 读取；直接打开登录页时也允许从 {@code tenant}
     * 查询参数初始化租户。交互不存在或已过期时返回 expired 状态，不创建新的认证上下文。</p>
     */
    public Context loginContext(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        UUID tenant = sessionTenant(session);
        if (tenant == null && request.getParameter("tenant") != null) {
            tenant = parseTenant(request.getParameter("tenant"));
            if (tenant != null) {
                session = request.getSession(true);
                session.setAttribute(TENANT, tenant.toString());
                session.setAttribute(EXPIRES_AT, Instant.now().plusSeconds(INTERACTION_TTL_SECONDS).toEpochMilli());
            }
        }
        if (tenant == null || expired(session)) return Context.expired();
        LoginStyleEntity style = customization.publicStyle(tenant);
        boolean mfa = session != null && session.getAttribute("EASY1AUTH_MFA_CHALLENGE") != null;
        String error = session == null ? null : (String) session.getAttribute(LOGIN_ERROR);
        return new Context(mfa ? "mfa" : "login", tenant.toString(), publicStyle(style, tenant), error, csrfToken(request));
    }

    /**
     * 获取当前交互租户，并确保登录请求仍在有效期内。
     *
     * @throws DomainException 租户缺失或交互已过期时抛出 410 异常
     */
    public UUID requireTenant(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        UUID tenant = sessionTenant(session);
        if (tenant == null || expired(session)) throw new DomainException(ErrorCodeConstants.AUTH_INTERACTION_EXPIRED_LOGIN);
        return tenant;
    }

    /**
     * 校验并捕获授权确认请求。
     *
     * <p>浏览器传回的关键参数必须与首次保存的请求一致；后续流程只使用服务端保存的
     * canonical 请求。此处同时校验 OAuth 客户端是否属于当前租户，并生成新的 consent ID。</p>
     */
    public ConsentStart captureConsent(HttpServletRequest request, HttpServletResponse response) {
        UUID tenant = requireTenant(request);
        Map<String, String[]> initial = sessionMap(request.getSession(false), AUTH_REQUEST);
        Map<String, String[]> received = copy(request.getParameterMap());
        if (initial != null && !same(initial, received, "client_id", "redirect_uri", "response_type", "state"))
            throw new DomainException(ErrorCodeConstants.AUTH_INTERACTION_MISMATCH_REQUEST);
        if (initial != null && initial.containsKey("scope") && !Arrays.equals(initial.get("scope"), received.get("scope")))
            throw new DomainException(ErrorCodeConstants.AUTH_INTERACTION_MISMATCH_REQUEST);
        if (initial == null)
            throw new DomainException(ErrorCodeConstants.AUTH_INTERACTION_EXPIRED_REQUEST);
        // The saved request is the canonical request. Never persist a scope,
        // redirect URI, or state supplied only by the consent-page browser.
        Map<String, String[]> canonical = copy(initial);
        String clientId = first(canonical, "client_id");
        if (clientId == null)
            throw new DomainException(ErrorCodeConstants.AUTH_INTERACTION_EXPIRED_REQUEST);
        LoginStyleEntity style = customization.publicStyle(tenant);
        UUID clientTenant = clientTenant(clientId);
        if (!tenant.equals(clientTenant))
            throw new DomainException(ErrorCodeConstants.AUTH_INTERACTION_MISMATCH_CLIENT_TENANT);
        HttpSession session = request.getSession(false);
        String interactionId = UUID.randomUUID().toString();
        session.setAttribute(CONSENT_REQUEST, canonical);
        session.setAttribute(CONSENT_ID, interactionId);
        session.removeAttribute(CONSENT_ACTION);
        session.setAttribute(EXPIRES_AT, Instant.now().plusSeconds(INTERACTION_TTL_SECONDS).toEpochMilli());
        return new ConsentStart(interactionId, tenant.toString(), publicStyle(style, tenant), clientId,
                clientName(clientId), values(canonical, "scope"));
    }

    /**
     * 获取授权确认页上下文。
     *
     * <p>已过期、已消费、缺少客户端或缺少授权参数的交互统一返回 expired 状态，
     * 避免把 Session 内部状态暴露给前端。</p>
     */
    public ConsentContext consentContext(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        UUID tenant = sessionTenant(session);
        Map<String, String[]> consent = sessionMap(session, CONSENT_REQUEST);
        if (tenant == null || consent == null || expired(session) || Boolean.TRUE.equals(session.getAttribute(USED))) return ConsentContext.expired();
        String clientId = first(consent, "client_id");
        if (clientId == null) return ConsentContext.expired();
        LoginStyleEntity style = customization.publicStyle(tenant);
        return new ConsentContext("consent", tenant.toString(), publicStyle(style, tenant), clientId,
                clientName(clientId), values(consent, "scope"), csrfToken(request));
    }

    /**
     * 记录用户在授权确认页的决定，并返回 OAuth 继续地址。
     *
     * <p>决定绑定到服务端 Session；浏览器只能携带不透明 ID，不能通过修改 URL 更换
     * approve/deny 操作。</p>
     *
     * @throws DomainException 操作非法、交互缺失、已过期或已消费时抛出领域异常
     */
    public Continuation consumeConsent(HttpServletRequest request, String action) {
        if (!"approve".equals(action) && !"deny".equals(action))
            throw new DomainException(ErrorCodeConstants.AUTH_ACTION_INVALID);
        HttpSession session = request.getSession(false);
        UUID tenant = sessionTenant(session);
        Map<String, String[]> consent = sessionMap(session, CONSENT_REQUEST);
        String id = session == null ? null : (String) session.getAttribute(CONSENT_ID);
        if (tenant == null || consent == null || id == null || expired(session) || Boolean.TRUE.equals(session.getAttribute(USED)))
            throw new DomainException(ErrorCodeConstants.AUTH_INTERACTION_EXPIRED_PROCESSED);
        // Bind the decision to the server-side session. The browser only gets
        // an opaque interaction id; it cannot switch approve to deny by editing
        // the continuation URL.
        session.setAttribute(CONSENT_ACTION, action);
        session.removeAttribute(USED);
        String path = "/t/" + tenant + "/oauth2/authorize?interaction=" + id;
        return new Continuation(path);
    }

    /** 获取 OAuth 流程继续使用的、经过复制保护的授权参数。 */
    public Map<String, String[]> interactionParameters(HttpSession session) {
        Map<String, String[]> value = sessionMap(session, CONSENT_REQUEST);
        if (value == null || expired(session)) throw new DomainException(ErrorCodeConstants.AUTH_INTERACTION_EXPIRED_PROCESSED);
        return value;
    }

    /** 获取当前授权确认交互 ID；Session 不存在时返回 null。 */
    public String consentId(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(CONSENT_ID);
    }

    /** 清除当前 Session 中的登录错误提示。 */
    public void clearLoginError(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.removeAttribute(LOGIN_ERROR);
    }

    /** 在当前 Session 中记录通用登录失败提示，不暴露具体失败原因。 */
    public void setLoginError(HttpServletRequest request) {
        request.getSession(true).setAttribute(LOGIN_ERROR, "用户名或密码错误");
    }

    /** 查询启用中的 OAuth 客户端所属租户，用于租户边界校验。 */
    private UUID clientTenant(String clientId) {
        return db.sql("select tenant_id from oauth_application where client_id=:clientId and status='active'")
                .param("clientId", clientId).query(UUID.class).optional().orElse(null);
    }

    /** 查询启用中的 OAuth 客户端名称；查不到时回退为 client_id。 */
    private String clientName(String clientId) {
        return db.sql("select name from oauth_application where client_id=:clientId and status='active'")
                .param("clientId", clientId).query(String.class).optional().orElse(clientId);
    }

    /** 从 Session 读取并解析租户 ID。 */
    private static UUID sessionTenant(HttpSession session) {
        if (session == null) return null;
        return parseTenant((String) session.getAttribute(TENANT));
    }

    /** 判断 Session 中的交互是否缺失过期时间或已经超过有效期。 */
    private static boolean expired(HttpSession session) {
        if (session == null) return true;
        Object value = session.getAttribute(EXPIRES_AT);
        return !(value instanceof Number n) || n.longValue() < System.currentTimeMillis();
    }

    /** 安全解析租户 UUID；格式非法时返回 null。 */
    private static UUID parseTenant(String value) {
        try { return UUID.fromString(value); } catch (RuntimeException ex) { return null; }
    }

    /** 从形如 /t/{tenant}/... 的请求 URI 中提取租户 UUID。 */
    private static UUID tenantFromPath(String uri) {
        String[] parts = uri.split("/");
        return parts.length > 2 && "t".equals(parts[1]) ? parseTenant(parts[2]) : null;
    }

    /**
     * 复制请求参数并限制键和值的数量与长度。
     *
     * <p>复制数组可以避免调用方在 Session 外修改已保存的授权参数，也能限制异常大的
     * 参数映射进入 Session。</p>
     */
    private static Map<String, String[]> copy(Map<String, String[]> input) {
        Map<String, String[]> out = new LinkedHashMap<>();
        input.forEach((key, values) -> {
            if (key != null && key.length() <= 100 && values != null && values.length <= 20)
                out.put(key, values.clone());
        });
        return out;
    }

    /** 从 Session 读取参数映射，并返回防止外部修改的副本。 */
    private static Map<String, String[]> sessionMap(HttpSession session, String key) {
        Object value = session == null ? null : session.getAttribute(key);
        return value instanceof Map<?, ?> map ? castMap(map) : null;
    }

    /** 将未类型化的 Session Map 安全转换为字符串数组参数映射。 */
    private static Map<String, String[]> castMap(Map<?, ?> map) {
        Map<String, String[]> out = new LinkedHashMap<>();
        map.forEach((key, value) -> { if (key instanceof String k && value instanceof String[] v) out.put(k, v.clone()); });
        return out;
    }

    /** 获取参数的第一个值；参数不存在或没有值时返回 null。 */
    private static String first(Map<String, String[]> map, String key) {
        String[] values = map.get(key);
        return values == null || values.length == 0 ? null : values[0];
    }

    /** 获取参数的全部值；参数不存在时返回空列表。 */
    private static List<String> values(Map<String, String[]> map, String key) {
        String[] values = map.get(key);
        return values == null || values.length == 0 ? List.of() : Arrays.asList(values.clone());
    }

    /** 比较指定 OAuth 关键参数是否完全一致。 */
    private static boolean same(Map<String, String[]> left, Map<String, String[]> right, String... keys) {
        return Arrays.stream(keys).allMatch(key -> Arrays.equals(left.get(key), right.get(key)));
    }

    /** 将数据库中的登录样式转换为只包含公开字段的响应对象，并附带登录页需要的社会化身份源详情。 */
    private PublicStyle publicStyle(LoginStyleEntity style, UUID tenant) {
        List<String> providerIds = style.socialProviders();
        List<SocialSourceSummary> sources = List.of();
        if (providerIds != null && !providerIds.isEmpty()) {
            var active = socialIdentity.listActive(tenant);
            var byId = new LinkedHashMap<String, SocialIdentityService.SourceView>();
            for (var s : active) byId.put(s.id().toString(), s);
            var filtered = new ArrayList<SocialSourceSummary>();
            for (var id : providerIds) {
                var s = byId.get(id);
                if (s != null) filtered.add(new SocialSourceSummary(s.id().toString(), s.type(), s.name()));
            }
            sources = filtered;
        }
        var legal = customization.publishedLegalDocuments(tenant);
        return new PublicStyle(style.logo(), style.logoDark(), style.backgroundImage(), style.backgroundColor(),
                style.primaryColor(), style.title(), style.subtitle(), style.loginMethods(), style.socialProviders(),
                sources, style.registrationEnabled(), customization.publishedConfig(tenant),
                legal.termsOfService(), legal.privacyPolicy());
    }

    /** 从当前请求中读取 Spring Security 注入的 CSRF Token。 */
    private static String csrfToken(HttpServletRequest request) {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return csrf == null ? null : csrf.getToken();
    }

    /** 登录页或授权页公开展示的品牌样式。 */
    public record PublicStyle(String logo, String logoDark, String backgroundImage, String backgroundColor,
                              String primaryColor, String title, String subtitle, List<String> loginMethods,
                              List<String> socialProviders, List<SocialSourceSummary> socialSources,
                              boolean registrationEnabled, Map<String, Object> config,
                              String termsOfService, String privacyPolicy) { }

    /** 登录页渲染社交登录按钮所需的最小身份源信息。 */
    public record SocialSourceSummary(String id, String type, String name) { }

    /** 登录页上下文，包含当前状态、租户、样式、错误提示和 CSRF Token。 */
    public record Context(String status, String tenantId, PublicStyle style, String message, String csrfToken) {
        /** 创建一个表示交互已失效的登录上下文。 */
        static Context expired() { return new Context("expired", null, null, "登录请求已过期，请重新发起", null); }
    }

    /** 授权确认页初始化结果。 */
    public record ConsentStart(String interactionId, String tenantId, PublicStyle style, String clientId, String clientName, List<String> scopes) { }

    /** 授权确认页上下文，包含客户端信息、权限范围、样式和 CSRF Token。 */
    public record ConsentContext(String status, String tenantId, PublicStyle style, String clientId, String clientName, List<String> scopes, String csrfToken) {
        /** 创建一个表示授权交互已失效的上下文。 */
        static ConsentContext expired() { return new ConsentContext("expired", null, null, null, null, List.of(), null); }
    }

    /** OAuth 授权决定完成后返回给调用方的继续地址。 */
    public record Continuation(String location) { }
}
