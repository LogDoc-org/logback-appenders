package ru.gang.logdoc.utils;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class Tools {
    public static final SecureRandom rnd;

    static {
        rnd = new SecureRandom();
        rnd.setSeed(System.currentTimeMillis());
    }

    public static final DateTimeFormatter logTimeFormat = DateTimeFormatter.ofPattern("yyMMddHHmmssSSS");
    public static final byte[] header = new byte[] {(byte) 6, (byte) 3};

    public static String notNull(final Object o, final String def) {
        if (o == null)
            return def == null ? "" : def.trim();

        if (o instanceof String)
            return ((String) o).trim();

        return String.valueOf(o).trim();
    }

    public static String notNull(final Object o) {
        return notNull(o, "");
    }

    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(final Object o) {
        if (o == null)
            return true;

        if (o.getClass().isArray())
            return Array.getLength(o) == 0;

        if (o instanceof Collection)
            return ((Collection) o).isEmpty();

        if (o instanceof Map)
            return ((Map) o).isEmpty();

        if (o.getClass().isEnum())
            return false;

        return notNull(o).isEmpty();
    }

    public static byte[] token() {
        return asBytes(generateUuid());
    }

    private static byte[] asBytes(final UUID uuid) {
        final ByteBuffer bb = ByteBuffer.wrap(new byte[16]);

        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        return bb.array();
    }

    private static long tol(final byte[] buf, final int shift) {
        return (toi(buf, shift) << 32) + ((toi(buf, shift + 4) << 32) >>> 32);
    }

    private static long toi(final byte[] buf, int shift) {
        return (buf[shift] << 24)
                + ((buf[++shift] & 0xFF) << 16)
                + ((buf[++shift] & 0xFF) << 8)
                + (buf[++shift] & 0xFF);
    }

    public static UUID generateUuid() {
        final byte[] buffer = new byte[16];
        rnd.nextBytes(buffer);

        return generateUuid(buffer);
    }

    private static UUID generateUuid(final byte[] buffer) {
        long r1, r2;

        r1 = tol(buffer, 0);
        r2 = tol(buffer, 1);

        r1 &= ~0xF000L;
        r1 |= 4 << 12;
        r2 = ((r2 << 2) >>> 2);
        r2 |= (2L << 62);

        return new UUID(r1, r2);
    }
}
