package com.easy1auth.authorization.web.dto;

import java.util.*;

public record ConsentStart(String interactionId, String userPoolId, PublicStyle style, String clientId, String clientName,
                           List<String> scopes) {
}
