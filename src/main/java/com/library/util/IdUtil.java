package com.library.util;

import java.security.SecureRandom;
import java.util.UUID;

/** UUID v7：前 48 位是毫秒时间戳，保证按 id 字典序近似按时间排序。 */
public final class IdUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private IdUtil() {
    }

    public static String uuid7() {
        long timestamp = System.currentTimeMillis();
        long randA = RANDOM.nextInt(0x1000);                 // 12 位随机
        long msb = ((timestamp & 0xFFFFFFFFFFFFL) << 16)
                | 0x7000L                                    // version 7
                | randA;
        long lsb = RANDOM.nextLong();
        lsb = (lsb & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L; // variant 10
        return new UUID(msb, lsb).toString();
    }

    public static String newId() {
        return uuid7();
    }
}
