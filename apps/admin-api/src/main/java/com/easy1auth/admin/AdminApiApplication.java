package com.easy1auth.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 管理端 API 应用入口。
 *
 * <p>以 {@code com.easy1auth} 为扫描根启动 Spring Boot 应用，并开启定时调度
 * （支撑投递 worker 等后台任务）。管理 API 默认监听 18848 端口。</p>
 */
@SpringBootApplication(scanBasePackages = "com.easy1auth")
@EnableScheduling
public class AdminApiApplication {
    /** 应用启动入口。 */
    public static void main(String[] args) {
        SpringApplication.run(AdminApiApplication.class, args);
    }
}
