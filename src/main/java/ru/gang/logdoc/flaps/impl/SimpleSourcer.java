package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Sourcer;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 12:46
 * logback-adapter ☭ sweat and blood
 */
public class SimpleSourcer implements Sourcer {
    @Override
    public String apply(final String s) {
        return s;
    }
}
