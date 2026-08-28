package com.easy1auth.social;

import java.net.URI;
import java.util.Map;

/**
 * 社会化身份源适配器：每个预定义社交厂商一个实现，端点写死在常量内。
 *
 * <p>适配器只负责厂商协议相关的两件事：拼授权 URL、换 token 并拉取用户信息。
 * state/nonce 生成、事务存取和身份绑定等厂商无关逻辑由 {@link SocialIdentityService} 统一处理。</p>
 */
public interface SocialIdentityAdapter {

    /** 厂商类型标识，如 "wechat_qr" / "wechat_mp" / "github" / "gitee" / "feishu_web"。 */
    String type();

    /** 展示名称，如 "微信扫码"。 */
    String displayName();

    /** 默认 scope 参数（部分厂商用，如 github 用 "read:user"）。 */
    String defaultScope();

    /** 是否使用 PKCE（S256）。微信/Github/Gitee 不支持，飞书网页授权不用。 */
    boolean supportsPkce();

    /**
     * 拼接授权 URL。
     *
     * @param clientId    身份源配置的 client_id / app_id
     * @param redirectUri 回调地址
     * @param state       service 层生成的随机 state
     * @param scope       scope 参数
     * @param pkceChallenge PKCE code_challenge（仅 supportsPkce() 时非空）
     * @return 完整授权 URL
     */
    AuthorizeResult authorize(String clientId, URI redirectUri, String state, String scope, String pkceChallenge);

    /**
     * 用授权码换取 access_token 并拉取用户信息。
     *
     * @param clientId    身份源配置的 client_id / app_id
     * @param clientSecret 解密后的 client_secret / app_secret
     * @param code        回调带回的授权码
     * @param redirectUri 回调地址
     * @param pkceVerifier PKCE code_verifier（仅 supportsPkce() 时非空）
     * @return 标准化的远程用户信息
     */
    RemoteUserInfo exchangeAndFetch(String clientId, String clientSecret, String code, URI redirectUri, String pkceVerifier);

    /**
     * 授权 URL 拼接结果。
     *
     * @param authorizeUrl 引导用户跳转到社交厂商的完整授权 URL
     */
    record AuthorizeResult(URI authorizeUrl) { }

    /**
     * 适配器产出的标准化远程用户信息，由 service 层据此完成账户确认与绑定。
     *
     * @param subject       外部账号唯一标识（openid / unionid / id 等），用于建立绑定关系
     * @param username      外部平台登录名（可为 null）
     * @param name          用户显示名称（可为 null）
     * @param email         邮箱（可为 null）
     * @param emailVerified 邮箱是否已由平台验证
     * @param avatar        头像 URL（可为 null）
     * @param rawClaims     平台返回的原始用户信息，原样透传保存
     */
    record RemoteUserInfo(String subject, String username, String name, String email, boolean emailVerified,
                          String avatar, Map<String, Object> rawClaims) { }
}
