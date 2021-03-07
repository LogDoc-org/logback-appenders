package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Sourcer;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:04
 * logback-adapter ☭ sweat and blood
 */
public class EmptySourcer implements Sourcer {
    @Override
    public String apply(final String s) {
        return "";
    }
}
