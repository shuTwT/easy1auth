package com.easy1auth.common.foundation.id;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

/**
 * RFC 9562 UUIDv7 生成器：生成毫秒级时间有序的 UUID。
 *
 * <p>UUIDv7 按时间排序，适合作为数据库主键以减少索引碎片；
 * 同一毫秒内通过递增序列号保证唯一性。实现线程安全。</p>
 */
public final class UuidV7 {
    /** 随机数源（用于序列号与随机位） */
    private static final SecureRandom RANDOM = new SecureRandom();
    /** 上次生成时的时间戳（毫秒），用于判断是否处于同一毫秒 */
    private static volatile long lastMillis;
    /** 当前毫秒内的递增序列号（0-4095，循环使用） */
    private static volatile int sequence;

    private UuidV7() {
    }

    /** 生成一个基于系统时钟的 UUIDv7。 */
    public static UUID randomUuid() {
        return randomUuid(Clock.systemUTC());
    }

    /** 基于指定时钟生成 UUIDv7（包内可见，便于测试注入时钟）。 */
    static synchronized UUID randomUuid(Clock clock) {
        long millis = clock.millis();
        if (millis == lastMillis) {
            sequence = (sequence + 1) & 0x0fff;
        } else {
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
