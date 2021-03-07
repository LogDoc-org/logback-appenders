package ru.gang.logdoc.flaps;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:26
 * logback-adapter ☭ sweat and blood
 */
public interface Fielder extends Function<String, Map<String, String>> {
}
