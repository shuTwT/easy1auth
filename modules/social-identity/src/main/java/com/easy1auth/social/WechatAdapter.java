package com.easy1auth.social;

import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Map;

/**
 * 微信 OAuth2 适配器基类：扫码（qrconnect）与公众号网页授权（oauth2/authorize）共用
 * token/userinfo 端点，仅 authorize 端点不同。
 *
 * <p>微信 access_token 换取返回 openid（非邮箱），userinfo 需 scope=snsapi_userinfo 才能拿到。
 * 公众号网页授权用 snsapi_userinfo 可静默拿 openid + userinfo；snsapi_base 只能拿 openid。</p>
 */
abstract class WechatAdapter extends AbstractSocialIdentityAdapter {

    /** 微信 sns OAuth 令牌交换端点（appid/secret 走 query 参数） */
    static final String TOKEN = "https://api.weixin.qq.com/sns/oauth2/access_token";
    /** 微信用户信息端点（需携带 access_token 与 openid） */
    static final String USERINFO = "https://api.weixin.qq.com/sns/userinfo";

    WechatAdapter(ObjectMapper json) { super(json); }

    @Override public boolean supportsPkce() { return false; }
    @Override public String defaultScope() { return "snsapi_login"; }

    /** 用授权码换取微信 access_token 并拉取用户信息，subject 优先取 unionid 实现跨应用去重。 */
    @Override
    public RemoteUserInfo exchangeAndFetch(String clientId, String clientSecret, String code, URI redirectUri, String pkceVerifier) {
        // 微信换 token 用 query 参数而非 Basic Auth，且 secret 直接放 query。
        var form = Map.of(
                "appid", clientId,
                "secret", clientSecret,
                "code", code,
                "grant_type", "authorization_code");
        var token = postForm(TOKEN, form, null);
        if (token.get("errcode") != null && ((Number) token.get("errcode")).intValue() != 0) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }
        String accessToken = string(token.get("access_token"));
        String openid = string(token.get("openid"));
        if (accessToken == null || openid == null) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }
        // snsapi_login / snsapi_userinfo 可拉用户信息；snsapi_base 只有 openid。
        var user = getJson(USERINFO + "?access_token=" + enc(accessToken) + "&openid=" + enc(openid), null);
        if (user.get("errcode") != null && ((Number) user.get("errcode")).intValue() != 0) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_USERINFO_FAILED);
        }
        String unionid = string(user.get("unionid"));
        String subject = unionid != null ? unionid : openid; // 优先 unionid 跨应用去重
        String nickname = string(user.get("nickname"));
        String avatar = string(user.get("headimgurl"));
        return new RemoteUserInfo(subject, openid, nickname, null, false, avatar, user);
    }
}
