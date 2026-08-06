package com.easy1auth.authorization.web;

import com.easy1auth.customization.CustomizationService;
import com.easy1auth.customization.model.LoginStyleEntity;
import com.easy1auth.foundation.error.DomainException;
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
 * Keeps the OAuth request in the server-side session while the resource owner
 * is interacting with the auth portal. The browser only receives a short-lived
 * opaque interaction id for the consent continuation.
 */
@Service
public class AuthorizationInteractionService {
    public static final String TENANT = "EASY1AUTH_INTERACTION_TENANT";
    public static final String AUTH_REQUEST = "EASY1AUTH_INTERACTION_AUTH_REQUEST";
    public static final String AUTH_URI = "EASY1AUTH_INTERACTION_AUTH_URI";
    public static final String EXPIRES_AT = "EASY1AUTH_INTERACTION_EXPIRES_AT";
    public static final String CONSENT_REQUEST = "EASY1AUTH_INTERACTION_CONSENT_REQUEST";
    public static final String CONSENT_ID = "EASY1AUTH_INTERACTION_CONSENT_ID";
    public static final String CONSENT_ACTION = "EASY1AUTH_INTERACTION_CONSENT_ACTION";
    public static final String USED = "EASY1AUTH_INTERACTION_USED";
    public static final String LOGIN_ERROR = "EASY1AUTH_LOGIN_ERROR";
    private static final long INTERACTION_TTL_SECONDS = 900;

    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
    private final CustomizationService customization;
    private final JdbcClient db;

    public AuthorizationInteractionService(CustomizationService customization, JdbcClient db) {
        this.customization = customization;
        this.db = db;
    }

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
        return new Context(mfa ? "mfa" : "login", tenant.toString(), publicStyle(style), error, csrfToken(request));
    }

    public UUID requireTenant(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        UUID tenant = sessionTenant(session);
        if (tenant == null || expired(session)) throw new DomainException("AUTH_INTERACTION_EXPIRED", "登录请求已过期，请重新发起授权", 410);
        return tenant;
    }

    public ConsentStart captureConsent(HttpServletRequest request, HttpServletResponse response) {
        UUID tenant = requireTenant(request);
        Map<String, String[]> initial = sessionMap(request.getSession(false), AUTH_REQUEST);
        Map<String, String[]> received = copy(request.getParameterMap());
        if (initial != null && !same(initial, received, "client_id", "redirect_uri", "response_type", "state"))
            throw new DomainException("AUTH_INTERACTION_MISMATCH", "授权请求已失效，请重新发起", 409);
        if (initial != null && initial.containsKey("scope") && !Arrays.equals(initial.get("scope"), received.get("scope")))
            throw new DomainException("AUTH_INTERACTION_MISMATCH", "授权请求已失效，请重新发起", 409);
        if (initial == null)
            throw new DomainException("AUTH_INTERACTION_EXPIRED", "授权请求已过期，请重新发起", 410);
        // The saved request is the canonical request. Never persist a scope,
        // redirect URI, or state supplied only by the consent-page browser.
        Map<String, String[]> canonical = copy(initial);
        String clientId = first(canonical, "client_id");
        if (clientId == null)
            throw new DomainException("AUTH_INTERACTION_EXPIRED", "授权请求已过期，请重新发起", 410);
        LoginStyleEntity style = customization.publicStyle(tenant);
        UUID clientTenant = clientTenant(clientId);
        if (!tenant.equals(clientTenant))
            throw new DomainException("AUTH_INTERACTION_MISMATCH", "授权客户端与租户不匹配", 400);
        HttpSession session = request.getSession(false);
        String interactionId = UUID.randomUUID().toString();
        session.setAttribute(CONSENT_REQUEST, canonical);
        session.setAttribute(CONSENT_ID, interactionId);
        session.removeAttribute(CONSENT_ACTION);
        session.setAttribute(EXPIRES_AT, Instant.now().plusSeconds(INTERACTION_TTL_SECONDS).toEpochMilli());
        return new ConsentStart(interactionId, tenant.toString(), publicStyle(style), clientId,
                clientName(clientId), values(canonical, "scope"));
    }

    public ConsentContext consentContext(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        UUID tenant = sessionTenant(session);
        Map<String, String[]> consent = sessionMap(session, CONSENT_REQUEST);
        if (tenant == null || consent == null || expired(session) || Boolean.TRUE.equals(session.getAttribute(USED))) return ConsentContext.expired();
        String clientId = first(consent, "client_id");
        if (clientId == null) return ConsentContext.expired();
        LoginStyleEntity style = customization.publicStyle(tenant);
        return new ConsentContext("consent", tenant.toString(), publicStyle(style), clientId,
                clientName(clientId), values(consent, "scope"), csrfToken(request));
    }

    public Continuation consumeConsent(HttpServletRequest request, String action) {
        if (!"approve".equals(action) && !"deny".equals(action))
            throw new DomainException("AUTH_ACTION_INVALID", "授权操作无效", 400);
        HttpSession session = request.getSession(false);
        UUID tenant = sessionTenant(session);
        Map<String, String[]> consent = sessionMap(session, CONSENT_REQUEST);
        String id = session == null ? null : (String) session.getAttribute(CONSENT_ID);
        if (tenant == null || consent == null || id == null || expired(session) || Boolean.TRUE.equals(session.getAttribute(USED)))
            throw new DomainException("AUTH_INTERACTION_EXPIRED", "授权请求已过期或已处理", 410);
        // Bind the decision to the server-side session. The browser only gets
        // an opaque interaction id; it cannot switch approve to deny by editing
        // the continuation URL.
        session.setAttribute(CONSENT_ACTION, action);
        session.removeAttribute(USED);
        String path = "/t/" + tenant + "/oauth2/authorize?interaction=" + id;
        return new Continuation(path);
    }

    public Map<String, String[]> interactionParameters(HttpSession session) {
        Map<String, String[]> value = sessionMap(session, CONSENT_REQUEST);
        if (value == null || expired(session)) throw new DomainException("AUTH_INTERACTION_EXPIRED", "授权请求已过期或已处理", 410);
        return value;
    }

    public String consentId(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(CONSENT_ID);
    }

    public void clearLoginError(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.removeAttribute(LOGIN_ERROR);
    }

    public void setLoginError(HttpServletRequest request) {
        request.getSession(true).setAttribute(LOGIN_ERROR, "用户名或密码错误");
    }

    private UUID clientTenant(String clientId) {
        return db.sql("select tenant_id from oauth_application where client_id=:clientId and status='active'")
                .param("clientId", clientId).query(UUID.class).optional().orElse(null);
    }

    private String clientName(String clientId) {
        return db.sql("select name from oauth_application where client_id=:clientId and status='active'")
                .param("clientId", clientId).query(String.class).optional().orElse(clientId);
    }

    private static UUID sessionTenant(HttpSession session) {
        if (session == null) return null;
        return parseTenant((String) session.getAttribute(TENANT));
    }

    private static boolean expired(HttpSession session) {
        if (session == null) return true;
        Object value = session.getAttribute(EXPIRES_AT);
        return !(value instanceof Number n) || n.longValue() < System.currentTimeMillis();
    }

    private static UUID parseTenant(String value) {
        try { return UUID.fromString(value); } catch (RuntimeException ex) { return null; }
    }

    private static UUID tenantFromPath(String uri) {
        String[] parts = uri.split("/");
        return parts.length > 2 && "t".equals(parts[1]) ? parseTenant(parts[2]) : null;
    }

    private static Map<String, String[]> copy(Map<String, String[]> input) {
        Map<String, String[]> out = new LinkedHashMap<>();
        input.forEach((key, values) -> {
            if (key != null && key.length() <= 100 && values != null && values.length <= 20)
                out.put(key, values.clone());
        });
        return out;
    }

    private static Map<String, String[]> sessionMap(HttpSession session, String key) {
        Object value = session == null ? null : session.getAttribute(key);
        return value instanceof Map<?, ?> map ? castMap(map) : null;
    }

    private static Map<String, String[]> castMap(Map<?, ?> map) {
        Map<String, String[]> out = new LinkedHashMap<>();
        map.forEach((key, value) -> { if (key instanceof String k && value instanceof String[] v) out.put(k, v.clone()); });
        return out;
    }

    private static String first(Map<String, String[]> map, String key) {
        String[] values = map.get(key);
        return values == null || values.length == 0 ? null : values[0];
    }

    private static List<String> values(Map<String, String[]> map, String key) {
        String[] values = map.get(key);
        return values == null || values.length == 0 ? List.of() : Arrays.asList(values.clone());
    }

    private static boolean same(Map<String, String[]> left, Map<String, String[]> right, String... keys) {
        return Arrays.stream(keys).allMatch(key -> Arrays.equals(left.get(key), right.get(key)));
    }

    private static PublicStyle publicStyle(LoginStyleEntity style) {
        return new PublicStyle(style.logo(), style.logoDark(), style.backgroundImage(), style.backgroundColor(), style.primaryColor(), style.title(), style.subtitle(), style.loginMethods(), style.socialProviders());
    }

    private static String csrfToken(HttpServletRequest request) {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return csrf == null ? null : csrf.getToken();
    }

    public record PublicStyle(String logo, String logoDark, String backgroundImage, String backgroundColor,
                              String primaryColor, String title, String subtitle, List<String> loginMethods,
                              List<String> socialProviders) { }
    public record Context(String status, String tenantId, PublicStyle style, String message, String csrfToken) {
        static Context expired() { return new Context("expired", null, null, "登录请求已过期，请重新发起", null); }
    }
    public record ConsentStart(String interactionId, String tenantId, PublicStyle style, String clientId, String clientName, List<String> scopes) { }
    public record ConsentContext(String status, String tenantId, PublicStyle style, String clientId, String clientName, List<String> scopes, String csrfToken) {
        static ConsentContext expired() { return new ConsentContext("expired", null, null, null, null, List.of(), null); }
    }
    public record Continuation(String location) { }
}
