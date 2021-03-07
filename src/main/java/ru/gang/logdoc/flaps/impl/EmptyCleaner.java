package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Cleaner;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:53
 * logback-adapter ☭ sweat and blood
 */
public class EmptyCleaner implements Cleaner {
    @Override
    public String apply(final String s) {
        return s;
    }
}
