package com.easy1auth.admin.web;

import com.easy1auth.admin.annotation.ManagementRouteClassification;
import com.easy1auth.admin.constant.ManagementRouteKind;
import com.easy1auth.admin.web.dto.SystemInitializationInput;
import com.easy1auth.admin.web.dto.SystemInitializationStatus;
import com.easy1auth.framework.web.response.ApiResponse;
import com.easy1auth.system.service.SystemConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 首次访问控制台时使用的公开系统初始化接口。 */
@RestController
@RequestMapping("/api/system/initialization")
public class SystemInitializationController {
    /** 系统配置服务。 */
    private final SystemConfigService systemConfig;
    /** 初始化编排服务。 */
    private final SystemInitializationService initialization;

    SystemInitializationController(SystemConfigService systemConfig,
                                   SystemInitializationService initialization) {
        this.systemConfig = systemConfig;
        this.initialization = initialization;
    }

    /** 查询系统是否已完成初始化。 */
    @ManagementRouteClassification(ManagementRouteKind.PUBLIC)
    @GetMapping("/status")
    public ApiResponse<SystemInitializationStatus> status() {
        return ApiResponse.ok(new SystemInitializationStatus(systemConfig.isInitialized()));
    }

    /** 创建首个系统管理员并完成系统初始化。 */
    @ManagementRouteClassification(ManagementRouteKind.PUBLIC)
    @PostMapping
    public ApiResponse<Void> initialize(@RequestBody SystemInitializationInput input) {
        String account = input == null ? null : input.account();
        String password = input == null ? null : input.password();
        initialization.initialize(account, password);
        return ApiResponse.ok(null, "系统初始化完成");
    }
}
