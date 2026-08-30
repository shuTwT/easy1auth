package com.easy1auth.social.adapter;

import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.social.constant.ErrorCodeConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;

/**
 * Gitee OAuth2 适配器（类 Github，用户信息走 /api/v5/user）。
 */
@Component
public class GiteeAdapter extends AbstractSocialIdentityAdapter {

    /** Gitee OAuth 授权页地址 */
    private static final String AUTHORIZE = "https://gitee.com/oauth2/authorize";
    /** Gitee 令牌交换端点 */
    private static final String TOKEN = "https://gitee.com/oauth2/token";
    /** Gitee 用户信息端点（access_token 以 query 参数传入） */
    private static final String USER = "https://gitee.com/api/v5/user";

    GiteeAdapter(ObjectMapper json) { super(json); }

    @Override public String type() { return "gitee"; }
    @Override public String displayName() { return "Gitee"; }
    @Override public String defaultScope() { return "user_info"; }
    @Override public boolean supportsPkce() { return false; }

    /** 拼装 Gitee 授权 URL（scope 缺省时使用默认 user_info）。 */
    @Override
    public AuthorizeResult authorize(String clientId, URI redirectUri, String state, String scope, String pkceChallenge) {
        var url = AUTHORIZE + "?response_type=code&client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri.toString())
                + "&scope=" + enc(scope == null ? defaultScope() : scope)
                + "&state=" + enc(state);
        return new AuthorizeResult(URI.create(url));
    }

    /** 用授权码换取 access_token 并拉取 Gitee 用户信息，名称缺失时回退为登录名。 */
    @Override
    public RemoteUserInfo exchangeAndFetch(String clientId, String clientSecret, String code, URI redirectUri, String pkceVerifier) {
        var form = Map.of(
                "grant_type", "authorization_code",
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "redirect_uri", redirectUri.toString());
        var token = postForm(TOKEN, form, null);
        String accessToken = string(token.get("access_token"));
        if (accessToken == null) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }
        var user = getJson(USER + "?access_token=" + enc(accessToken), null);
        String id = string(user.get("id"));
        if (id == null) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_USERINFO_FAILED);
        }
        String email = string(user.get("email"));
        String name = string(user.get("name"));
        String login = string(user.get("login"));
        if (name == null) {
            name = login;
        }
        return new RemoteUserInfo(id, login, name, email, false, string(user.get("avatar_url")), user);
    }
}
