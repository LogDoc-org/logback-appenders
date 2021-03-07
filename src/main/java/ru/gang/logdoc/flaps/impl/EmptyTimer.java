package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Timer;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 12:59
 * logback-adapter ☭ sweat and blood
 */
public class EmptyTimer implements Timer {
    @Override
    public String apply(Long aLong) {
        return "";
    }
}
