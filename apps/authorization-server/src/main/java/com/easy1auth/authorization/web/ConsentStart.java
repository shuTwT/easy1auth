package com.easy1auth.authorization.web;
import java.util.*;

public record ConsentStart(String interactionId, String tenantId, PublicStyle style, String clientId, String clientName, List<String> scopes) { }
