package com.easy1auth.poolidentity.dto;
import java.util.UUID;
/** 用户组创建/更新入参。 */
public record GroupInput(String name, String description, String type, UUID parentId) { }
