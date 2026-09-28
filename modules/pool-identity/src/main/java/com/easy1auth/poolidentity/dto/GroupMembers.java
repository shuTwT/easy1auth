package com.easy1auth.poolidentity.dto;

import java.util.List;

/** 用户组成员视图。 */
public record GroupMembers(List<PoolUserView> members, int total) {
}
