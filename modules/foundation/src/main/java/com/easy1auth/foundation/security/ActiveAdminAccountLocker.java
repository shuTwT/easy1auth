package com.easy1auth.foundation.security;

import java.util.UUID;

public interface ActiveAdminAccountLocker {
    void lockActive(UUID accountId);
}
