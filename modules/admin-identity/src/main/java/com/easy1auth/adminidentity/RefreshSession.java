package com.easy1auth.adminidentity;

import java.util.UUID;

public record RefreshSession(UUID id, AdminAccount account, String replacementToken) {
}
