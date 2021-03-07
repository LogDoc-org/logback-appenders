package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Sourcer;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 12:20
 * logback-adapter ☭ sweat and blood
 */
public class SourcerBoth implements Sourcer {
    private final String prefix, suffix;

    public SourcerBoth(final String prefix, final String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    @Override
    public String apply(final String s) {
        return prefix + s + suffix;
    }
}
