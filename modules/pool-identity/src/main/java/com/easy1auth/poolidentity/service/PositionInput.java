package com.easy1auth.poolidentity.service;
import java.util.UUID;
/** 岗位创建/更新入参。 */
public record PositionInput(String name, String code, String description, UUID departmentId, Integer level,
                            String sequence, Integer maxCount) { }
