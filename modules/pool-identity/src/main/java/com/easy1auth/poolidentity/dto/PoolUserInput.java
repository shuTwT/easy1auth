package com.easy1auth.poolidentity.dto;
import java.util.Map;
/** 用户创建/更新入参。 */
public record PoolUserInput(String username, String email, String password, String phone, String name, String avatar,
                            String status, String department, String position, Map<String, Object> customAttributes) { }
