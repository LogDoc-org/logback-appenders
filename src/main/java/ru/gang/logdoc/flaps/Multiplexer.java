package ru.gang.logdoc.flaps;

import java.util.List;
import java.util.function.Function;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:16
 * logback-adapter ☭ sweat and blood
 */
public interface Multiplexer extends Function<String, List<String>> {
}
