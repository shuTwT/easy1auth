package com.easy1auth.adminidentity;
import java.time.*;
import java.util.*;

public record IssuedCode(String email, String code, Instant expiresAt) {
    }
