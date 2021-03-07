package ru.gang.logdoc.flaps.impl;

import ch.qos.logback.classic.Level;
import ru.gang.logdoc.flaps.Leveler;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:07
 * logback-adapter ☭ sweat and blood
 */
public class EmptyLeveler implements Leveler {
    @Override
    public Byte apply(final Level level) {
        return (byte) LDLevel.LOG.ordinal();
    }
}
