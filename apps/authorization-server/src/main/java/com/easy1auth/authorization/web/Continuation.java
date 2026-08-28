package com.easy1auth.authorization.web;
import java.util.*;

/** OAuth 授权流程的继续跳转上下文。 */
public record Continuation(String location, String clientId, String state, List<String> scopes) { }
