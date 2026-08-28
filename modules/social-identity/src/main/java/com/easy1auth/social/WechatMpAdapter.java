package com.easy1auth.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * 微信公众号网页授权（在微信内置浏览器中打开），scope=snsapi_userinfo 可获取用户信息。
 */
@Component
public class WechatMpAdapter extends WechatAdapter {

    /** 微信公众号网页授权页地址（在微信内置浏览器中打开） */
    private static final String AUTHORIZE = "https://open.weixin.qq.com/connect/oauth2/authorize";

    WechatMpAdapter(ObjectMapper json) { super(json); }

    @Override public String type() { return "wechat_mp"; }
    @Override public String displayName() { return "微信公众号"; }
    @Override public String defaultScope() { return "snsapi_userinfo"; }

    /** 拼装公众号网页授权 URL，scope 缺省时使用 snsapi_userinfo（可获取用户信息）。 */
    @Override
    public AuthorizeResult authorize(String clientId, URI redirectUri, String state, String scope, String pkceChallenge) {
        var url = AUTHORIZE + "?appid=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri.toString())
                + "&response_type=code"
                + "&scope=" + enc(scope == null ? defaultScope() : scope)
                + "&state=" + enc(state)
                + "#wechat_redirect";
        return new AuthorizeResult(URI.create(url));
    }
}
