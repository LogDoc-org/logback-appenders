package ru.gang.logdoc.flaps;

import ch.qos.logback.classic.Level;

import java.util.function.Function;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:06
 * logback-adapter ☭ sweat and blood
 */
public interface Leveler extends Function<Level, Byte> {
    enum LDLevel {DEBUG, INFO, LOG, WARN, ERROR, SEVERE}
}
