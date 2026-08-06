package com.easy1auth.authorization.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/** Rehydrates the validated consent request from the session before SAS sees it. */
public class ConsentInteractionFilter extends OncePerRequestFilter {
    private final AuthorizationInteractionService interactions;

    public ConsentInteractionFilter(AuthorizationInteractionService interactions) {
        this.interactions = interactions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().endsWith("/oauth2/authorize") || request.getParameter("interaction") == null) {
            chain.doFilter(request, response);
            return;
        }
        var session = request.getSession(false);
        if (session == null || Boolean.TRUE.equals(session.getAttribute(AuthorizationInteractionService.USED))
                || !Objects.equals(interactions.consentId(session), request.getParameter("interaction"))
                || !sameTenant(session, request.getRequestURI())) {
            response.sendError(410, "授权请求已过期或已处理");
            return;
        }
        Map<String, String[]> params;
        try {
            params = interactions.interactionParameters(session);
        } catch (RuntimeException ex) {
            response.sendError(410, "授权请求已过期或已处理");
            return;
        }
        Map<String, String[]> rehydrated = new LinkedHashMap<>(params);
        String action = Objects.toString(session.getAttribute(AuthorizationInteractionService.CONSENT_ACTION), null);
        if ("approve".equals(action)) {
            // The requested scope set is server-owned; the portal cannot narrow
            // or widen it by posting arbitrary values.
            if (params.containsKey("scope")) rehydrated.put("scope", params.get("scope").clone());
        } else if ("deny".equals(action)) {
            rehydrated.remove("scope");
        } else {
            response.sendError(400, "授权操作无效");
            return;
        }
        rehydrated.remove("interaction");
        rehydrated.remove("consent_action");
        // Consume before entering SAS. Session requests are serialized by the
        // servlet container, so a replay cannot pass this gate twice.
        session.setAttribute(AuthorizationInteractionService.USED, true);
        session.removeAttribute(AuthorizationInteractionService.CONSENT_REQUEST);
        session.removeAttribute(AuthorizationInteractionService.CONSENT_ID);
        session.removeAttribute(AuthorizationInteractionService.CONSENT_ACTION);
        chain.doFilter(new ParameterRequest(request, rehydrated), response);
    }

    private static boolean sameTenant(jakarta.servlet.http.HttpSession session, String uri) {
        Object expected = session.getAttribute(AuthorizationInteractionService.TENANT);
        String[] parts = uri.split("/");
        return parts.length > 2 && "t".equals(parts[1]) && Objects.equals(Objects.toString(expected, null), parts[2]);
    }

    private static final class ParameterRequest extends HttpServletRequestWrapper {
        private final Map<String, String[]> parameters;

        private ParameterRequest(HttpServletRequest request, Map<String, String[]> parameters) {
            super(request);
            this.parameters = parameters;
        }

        @Override public String getParameter(String name) {
            String[] values = parameters.get(name);
            return values == null || values.length == 0 ? null : values[0];
        }
        @Override public String[] getParameterValues(String name) { return parameters.get(name); }
        @Override public Enumeration<String> getParameterNames() { return Collections.enumeration(parameters.keySet()); }
        @Override public Map<String, String[]> getParameterMap() { return Collections.unmodifiableMap(parameters); }
    }
}
