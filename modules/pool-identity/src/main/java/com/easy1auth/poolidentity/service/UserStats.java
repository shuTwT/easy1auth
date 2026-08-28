package com.easy1auth.poolidentity.service;
/** 用户状态统计视图。 */
public record UserStats(long totalUsers, long activeUsers, long disabledUsers, long lockedUsers) { }
