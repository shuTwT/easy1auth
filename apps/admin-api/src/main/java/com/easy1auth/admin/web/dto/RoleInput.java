package com.easy1auth.admin.web.dto;

import java.util.List;

public record RoleInput(String name, String description, List<String> permissions) {
}
