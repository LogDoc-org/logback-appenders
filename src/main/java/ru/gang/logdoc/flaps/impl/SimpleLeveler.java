package ru.gang.logdoc.flaps.impl;

import ch.qos.logback.classic.Level;
import ru.gang.logdoc.flaps.Leveler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:08
 * logback-adapter ☭ sweat and blood
 */
public class SimpleLeveler implements Leveler {
    private final Map<Level, Byte> matchMap;

    public SimpleLeveler() {
        matchMap = new HashMap<>(LDLevel.values().length);

        matchMap.put(Level.DEBUG, (byte) LDLevel.DEBUG.ordinal());
        matchMap.put(Level.ERROR, (byte) LDLevel.ERROR.ordinal());
        matchMap.put(Level.TRACE, (byte) LDLevel.LOG.ordinal());
        matchMap.put(Level.INFO, (byte) LDLevel.INFO.ordinal());
        matchMap.put(Level.WARN, (byte) LDLevel.WARN.ordinal());
        matchMap.put(Level.ALL, (byte) LDLevel.LOG.ordinal());
        matchMap.put(Level.OFF, (byte) LDLevel.LOG.ordinal());
    }

    @Override
    public Byte apply(final Level level) {
        return matchMap.get(level);
    }
}
