package com.easy1auth.adminaccess.dto;
/** 管理员成员统计视图。 */
public record MemberStats(long totalAdmins, long activeAdmins, long disabledAdmins, long mfaEnabledAdmins, long ownerCount) { }
