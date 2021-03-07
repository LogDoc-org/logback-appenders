package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Fielder;
import ru.gang.logdoc.model.Field;
import ru.gang.logdoc.model.StaticPosFields;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:36
 * logback-adapter ☭ sweat and blood
 */
public class PostFielder implements Fielder {
    private final StaticPosFields staticSuffix;

    private final BiFunction<String, Integer, Boolean> matcher;

    public PostFielder(final StaticPosFields staticSuffix) {
        this.staticSuffix = staticSuffix;

        matcher = staticSuffix.getSeparator().length() > 1
                ? (log, pos) -> {
            int e = pos;
            for (int j = 0; j < staticSuffix.getSeparator().length(); j++, e++)
                if (log.charAt(e) != staticSuffix.getSeparator().charAt(j))
                    return false;
            return true;
        }
                : (log, pos) -> staticSuffix.getSeparator().charAt(0) == log.charAt(pos);
    }

    @Override
    public Map<String, String> apply(final String event) {
        if (!event.endsWith(staticSuffix.getCloseMark()))
            return Collections.emptyMap();

        final int openIdx = event.lastIndexOf(staticSuffix.getOpenMark());

        if (openIdx <= 0 || openIdx == event.length() - 1)
            return Collections.emptyMap();

        final String values = event.substring(openIdx + staticSuffix.getOpenMark().length(), event.length() - staticSuffix.getCloseMark().length());

        return extractFields(new HashMap<>(), values);
    }

    private Map<String, String> extractFields(final Map<String, String> map, final String values) {
        if (values.trim().isEmpty())
            return map;

        int from = 0;
        final Iterator<Field> iterator = staticSuffix.getFields().iterator();

        for (int i = 0; i < values.length(); i++) {
            if (Character.isISOControl(values.charAt(i)))
                return map;

            if ((i + 1) == values.length() || matcher.apply(values, i)) {
                map.put(iterator.next().getFieldName(), values.substring(from, Math.max(from, i - 1)));

                i += staticSuffix.getSeparator().length() - 1;
                from = i + 1;
            }
        }

        return map;
    }
}
