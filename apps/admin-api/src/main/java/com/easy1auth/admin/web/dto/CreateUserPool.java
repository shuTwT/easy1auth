package com.easy1auth.admin.web.dto;

import java.util.UUID;

public record CreateUserPool(String name, long packageId, UUID administratorAccountId) {
}
