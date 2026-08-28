package com.easy1auth.authorization.web;
import java.util.*;

public record Continuation(String location, String clientId, String state, List<String> scopes) { }
