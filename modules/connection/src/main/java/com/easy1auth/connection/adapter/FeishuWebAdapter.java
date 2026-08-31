package com.easy1auth.connection.adapter;

import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.connection.constant.ErrorCodeConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;

/**
 * 飞书网页授权 SSO 适配器。
 *
 * <p>流程：authorize -> code -> OAuth v3 user_access_token -> user_info。
 * 注意：这与 connection 模块中企业身份源的飞书「通讯录目录同步」是完全不同的两套场景，
 * 本适配器只负责飞书账号 SSO 登录。</p>
 */
@Component
public class FeishuWebAdapter extends AbstractSocialIdentityAdapter {

    /** 飞书 OAuth 授权页地址（引导用户到飞书确认授权） */
    private static final String AUTHORIZE = "https://accounts.feishu.cn/open-apis/authen/v1/authorize";
    /** 飞书 OAuth v3 换取 user_access_token 的令牌端点 */
    private static final String USER_TOKEN = "https://accounts.feishu.cn/oauth/v3/token";
    /** 飞书用户信息端点（携带 user_access_token 调用） */
    private static final String USERINFO = "https://open.feishu.cn/open-apis/authen/v1/user_info";

    FeishuWebAdapter(ObjectMapper json) { super(json); }

    @Override public String type() { return "feishu_web"; }
    @Override public String displayName() { return "飞书网页授权"; }
    @Override public String defaultScope() { return ""; }
    @Override public boolean supportsPkce() { return false; }

    /** 拼装飞书授权 URL（response_type=code，飞书不支持 PKCE）。 */
    @Override
    public AuthorizeResult authorize(String clientId, URI redirectUri, String state, String scope, String pkceChallenge) {
        var url = AUTHORIZE + "?client_id=" + enc(clientId)
                + "&response_type=code"
                + "&redirect_uri=" + enc(redirectUri.toString())
                + "&state=" + enc(state);
        return new AuthorizeResult(URI.create(url));
    }

    /** 用授权码换取 user_access_token 并拉取飞书用户信息，subject 优先取 open_id。 */
    @Override
    public RemoteUserInfo exchangeAndFetch(String clientId, String clientSecret, String code, URI redirectUri, String pkceVerifier) {
        // OAuth v3 直接使用授权码与应用凭证换取 user_access_token。
        var userTokenReq = Map.of(
                "grant_type", "authorization_code",
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "redirect_uri", redirectUri.toString());
        var userTokenRes = postForm(USER_TOKEN, userTokenReq, null);
        if (((Number) userTokenRes.getOrDefault("code", -1)).intValue() != 0) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }
        String userAccessToken = string(userTokenRes.get("access_token"));
        if (userAccessToken == null) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }

        // 获取用户信息
        var user = getJson(USERINFO, userAccessToken);
        if (((Number) user.getOrDefault("code", -1)).intValue() != 0) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_USERINFO_FAILED);
        }
        @SuppressWarnings("unchecked")
        var data = (Map<String, Object>) user.getOrDefault("data", user);
        String subject = string(data.get("open_id"));
        if (subject == null) {
            subject = string(data.get("user_id"));
        }
        if (subject == null) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_USERINFO_FAILED);
        }
        String name = string(data.get("name"));
        String email = string(data.get("email"));
        String avatar = string(data.get("avatar_url"));
        return new RemoteUserInfo(subject, string(data.get("user_id")), name, email, false, avatar, user);
    }
}
