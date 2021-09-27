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
    private final Map<Level, String> matchMap;

    public SimpleLeveler() {
        matchMap = new HashMap<>(LDLevel.values().length);

        matchMap.put(Level.DEBUG, LDLevel.DEBUG.name());
        matchMap.put(Level.ERROR, LDLevel.ERROR.name());
        matchMap.put(Level.TRACE, LDLevel.LOG.name());
        matchMap.put(Level.INFO, LDLevel.INFO.name());
        matchMap.put(Level.WARN, LDLevel.WARN.name());
        matchMap.put(Level.ALL, LDLevel.LOG.name());
        matchMap.put(Level.OFF, LDLevel.LOG.name());
    }

    @Override
    public String apply(final Level level) {
        return matchMap.get(level);
    }
}
