package com.easy1auth.foundation.id;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

/**
 * RFC 9562 UUIDv7 generator with millisecond ordering.
 */
public final class UuidV7 {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile long lastMillis;
    private static volatile int sequence;

    private UuidV7() {
    }

    public static UUID randomUuid() {
        return randomUuid(Clock.systemUTC());
    }

    static synchronized UUID randomUuid(Clock clock) {
        long millis = clock.millis();
        if (millis == lastMillis) sequence = (sequence + 1) & 0x0fff;
        else {
            lastMillis = millis;
            sequence = RANDOM.nextInt(0x1000);
        }
        long msb = (millis & 0xffffffffffffL) << 16;
        msb |= 0x7000L | sequence;
        long lsb = RANDOM.nextLong();
        lsb = (lsb & 0x3fffffffffffffffL) | 0x8000000000000000L;
        return new UUID(msb, lsb);
    }
}
