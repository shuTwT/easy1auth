package com.easy1auth.admin.web.dto;

import java.util.List;

public record ImportResponse(int success, int failed, int total, List<ImportError> errors,
                             List<ImportedUser> importedUsers) {
}
