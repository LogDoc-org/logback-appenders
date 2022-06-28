package org.logdoc.flaps.impl;

import org.logdoc.flaps.Sourcer;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 12:22
 * logback-adapter ☭ sweat and blood
 */
public class PreSourcer implements Sourcer {
    private final String prefix;

    public PreSourcer(final String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String apply(final String s) {
        return prefix + s;
    }
}
