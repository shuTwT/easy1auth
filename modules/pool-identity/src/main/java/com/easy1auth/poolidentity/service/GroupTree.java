package com.easy1auth.poolidentity.service;
import java.util.List;
import java.util.UUID;
/** 用户组树节点视图。 */
public record GroupTree(UUID id, String name, String description, String type, UUID parentId,
                        List<GroupTree> children, long memberCount, long adminCount) { }
