package com.easy1auth.poolidentity.service;
import java.util.List;
import com.easy1auth.poolidentity.PoolUserView;
/** 用户组成员视图。 */
public record GroupMembers(List<PoolUserView> members, List<PoolUserView> admins, int total) { }
