package com.easy1auth.authorization.web.dto;

import java.util.*;

public record ConsentStart(String interactionId, String tenantId, PublicStyle style, String clientId, String clientName,
                           List<String> scopes) {
}
