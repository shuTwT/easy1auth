package com.easy1auth.poolidentity.service;
/** 用户组成员、管理员和子组数量统计。 */
public record Counts(long members, long admins, long children) { }
