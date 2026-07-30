package com.easy1auth.adminidentity;

import java.util.Locale;

final class AdminIdentityNormalizer {
    private AdminIdentityNormalizer() {
    }

    static NormalizedIdentity normalize(String username, String email) {
        return new NormalizedIdentity(normalizeUsername(username), normalizeEmail(email));
    }

    static String normalizeLogin(String login) {
        return login == null ? null : login.strip().toLowerCase(Locale.ROOT);
    }

    static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    private static String normalizeUsername(String username) {
        return username == null ? null : username.strip().toLowerCase(Locale.ROOT);
    }

    record NormalizedIdentity(String username, String email) {
    }
}
