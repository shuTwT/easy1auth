package com.easy1auth.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * 微信开放平台网站应用扫码登录（qrconnect）。
 */
@Component
public class WechatQrAdapter extends WechatAdapter {

    private static final String AUTHORIZE = "https://open.weixin.qq.com/connect/qrconnect";

    WechatQrAdapter(ObjectMapper json) { super(json); }

    @Override public String type() { return "wechat_qr"; }
    @Override public String displayName() { return "微信扫码"; }

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
