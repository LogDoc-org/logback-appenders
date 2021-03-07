package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Fielder;

import java.util.Collections;
import java.util.Map;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:27
 * logback-adapter ☭ sweat and blood
 */
public class EmptyFielder implements Fielder {
    @Override
    public Map<String, String> apply(final String s) {
        return Collections.emptyMap();
    }
}
