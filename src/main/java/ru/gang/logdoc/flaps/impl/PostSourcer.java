package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Sourcer;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 12:22
 * logback-adapter ☭ sweat and blood
 */
public class PostSourcer implements Sourcer {
    private final String suffix;

    public PostSourcer(final String suffix) {
        this.suffix = suffix;
    }

    @Override
    public String apply(final String s) {
        return s + suffix;
    }
}
