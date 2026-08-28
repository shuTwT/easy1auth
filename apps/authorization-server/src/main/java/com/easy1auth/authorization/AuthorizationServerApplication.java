package com.easy1auth.authorization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 授权服务器应用入口。
 *
 * <p>以 {@code com.easy1auth} 为扫描根启动 Spring Boot 应用，承载 OAuth2 / OIDC
 * 授权端点与 pool_user 登录门户。授权服务器默认监听 18850 端口。</p>
 */
@SpringBootApplication(scanBasePackages = "com.easy1auth")
public class AuthorizationServerApplication {
    /** 应用启动入口。 */
    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServerApplication.class, args);
    }
}
