package ru.gang.logdoc.flaps.impl;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ru.gang.logdoc.flaps.Multiplexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:16
 * logback-adapter ☭ sweat and blood
 */
public class SimpleMultiplexer implements Multiplexer {
    @Override
    public List<String> apply(final String s) {
        final String[] array = s.split("\\r?\\n");
        final List<String> list = new ArrayList<>(array.length);
        Collections.addAll(list, array);
        return list;
    }
}
