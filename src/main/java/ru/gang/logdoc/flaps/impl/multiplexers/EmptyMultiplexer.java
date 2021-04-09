package ru.gang.logdoc.flaps.impl.multiplexers;

import ru.gang.logdoc.flaps.Multiplexer;

import java.util.Collections;
import java.util.List;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:19
 * logback-adapter ☭ sweat and blood
 */
public class EmptyMultiplexer implements Multiplexer {
    @Override
    public List<String> apply(final String s) {
        return Collections.singletonList(s);
    }
}
