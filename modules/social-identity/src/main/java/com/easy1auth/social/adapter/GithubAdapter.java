package com.easy1auth.social.adapter;

import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.social.constant.ErrorCodeConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;

/**
 * Github OAuth2 适配器（非 OIDC，用户信息走 /user）。
 */
@Component
public class GithubAdapter extends AbstractSocialIdentityAdapter {

    /** GitHub OAuth 授权页地址 */
    private static final String AUTHORIZE = "https://github.com/login/oauth/authorize";
    /** GitHub 令牌交换端点 */
    private static final String TOKEN = "https://github.com/login/oauth/access_token";
    /** GitHub 用户信息端点（携带 Bearer access_token 调用） */
    private static final String USER = "https://api.github.com/user";

    GithubAdapter(ObjectMapper json) { super(json); }

    @Override public String type() { return "github"; }
    @Override public String displayName() { return "GitHub"; }
    @Override public String defaultScope() { return "read:user user:email"; }
    @Override public boolean supportsPkce() { return false; }

    /** 拼装 GitHub 授权 URL（scope 缺省时使用默认 read:user user:email）。 */
    @Override
    public AuthorizeResult authorize(String clientId, URI redirectUri, String state, String scope, String pkceChallenge) {
        var url = AUTHORIZE + "?response_type=code&client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri.toString())
                + "&scope=" + enc(scope == null ? defaultScope() : scope)
                + "&state=" + enc(state);
        return new AuthorizeResult(URI.create(url));
    }

    /** 用授权码换取 access_token 并拉取 GitHub 用户信息，名称缺失时回退为登录名。 */
    @Override
    public RemoteUserInfo exchangeAndFetch(String clientId, String clientSecret, String code, URI redirectUri, String pkceVerifier) {
        var form = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "redirect_uri", redirectUri.toString());
        var token = postForm(TOKEN, form, null);
        String accessToken = string(token.get("access_token"));
        if (accessToken == null) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }
        var user = getJson(USER, accessToken);
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
